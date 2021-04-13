package runtm;

import iconst.IConst;
import iconst.KeywordTyp;
import iconst.NodeCellTyp;
import iconst.TokenTyp;
import iconst.PageTyp;
import page.Node;
import page.AddrNode;
import page.Store;
import page.Page;
import scansrc.ScanSrc;
import synchk.SynChk;
import java.util.HashMap;
import java.util.ArrayList;

// Code Execution

public class RunTime implements IConst {

	private Store store;
	private ScanSrc scanSrc;
	private SynChk synChk;
	private RunScanner rscan;
	private int locBaseIdx;
	private int varCountIdx;
	private int stmtCount;
	private int currZstmt;
	private int stkZstmt;
	private boolean isTgtExpr;
	private boolean isNegInt;
	private static final char SP = ' ';
	private static final int EXIT = -1;
	private static final int NEGADDR = -2;
	private static final int STKOVERFLOW = -3;
	private static final int STKUNDERFLOW = -4;
	private static final int BADSTMT = -5;
	private static final int BADOP = -6;
	private static final int BADCELLTYP = -7;
	private static final int BADALLOC = -8;
	private static final int BADPOP = -9;
	private static final int ZERODIV = -10;
	private static final int KWDPOPPED = -11;
	private static final int NULLPOPPED = -12;
	private static final int BADSTORE = -13;
	private static final int NONVAR = 0; // same as AddrNode
	private static final int LOCVAR = 1; //
	private static final int FLDVAR = 2; //
	private static final int GLBVAR = 3; //
	public HashMap<String, Integer> glbFunMap;
	public HashMap<String, Integer> glbLocVarMap;
	public ArrayList<Integer> glbFunList;
	public ArrayList<Integer> glbLocVarList;

	public RunTime(Store store, ScanSrc scanSrc, SynChk synChk) {
		this.store = store;
		this.scanSrc = scanSrc;
		this.synChk = synChk;
		locBaseIdx = 0;
		varCountIdx = 0;
		stmtCount = 0;
		isTgtExpr = false;
		glbFunMap = new HashMap<String, Integer>();
		glbLocVarMap = new HashMap<String, Integer>();
		glbFunList = new ArrayList<Integer>();
		glbLocVarList = new ArrayList<Integer>();
	}
	
	public void setRscan(RunScanner rscan) {
		this.rscan = rscan;
	}

	public void out(String msg) {
		rscan.out(msg);
	}
	
	public void omsg(String msg) {  
		rscan.omsg(msg);
	}
	
	public void oprn(String msg) {  
		rscan.oprn(msg);
	}
	
	public void oerr(String msg) {  
		rscan.omsg(msg);
	}
	
	public boolean runTopBlock(int rightp) {
		// process top-level stmts.
		Node node;
		int downp;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int phaseNo = 0;

		while (rightp != 0) {
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			celltyp = node.getDownCellTyp();
			out("rightp = " + rightp +  
				", kwd = " + kwtyp + ", celtyp = " + celltyp);
			if (kwtyp != KeywordTyp.ZSTMT) {
				return false;
			}
			if (node.isOpenPar()) {
				out("Here is (");
				downp = node.getDownp();
				phaseNo = runTopStmt(downp, phaseNo);
				if (phaseNo < 0) {
					return false;
				}
				out("Here is )");
			}
			else {
				return false;
			}
			rightp = node.getRightp();
			if (phaseNo < 0) {
				return false;
			}
		}
		return true;
	}
	
	private int runTopStmt(int rightp, int phaseNo) {
		// process top-level statement
		// return phase no. of current stmt.: 0=quest, 1=import,
		//   gdefun, functions, 4=classes
		// return -1 on error
		Node node;
		KeywordTyp kwtyp = null;
		NodeCellTyp celltyp;
		boolean first = true;
		int currPhaseNo = phaseNo;
		int rightq;

		while (rightp > 0) {
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			celltyp = node.getDownCellTyp();
			out("rightp = " + rightp +  
					", kwd = " + kwtyp + ", celtyp = " + celltyp);
			if (first) {
				// at keyword token, beginning of top-level stmt.
				out("Statement kwd = " + kwtyp);
				currPhaseNo = synChk.getPhaseNo(kwtyp);
				rightq = rightp;
				rightp = node.getRightp();
				if (rightp <= 0) {
					return -1;
				}
				// rightp > 0 inside following switch
				switch (currPhaseNo) {
				case 0:
					return -1;
				case 1:
				case 3:
				case 4:
					rightq = rightp;
					break;
				case 2:
					rightq = runGlbDefStmt(rightp);
					break;
				default:
					rightq = -1;
				}
				if (rightq > 0) {
					rightp = rightq;
				}
				else {
					return -1;
				}
			}
			rightp = node.getRightp();
			first = false;
		}
		return currPhaseNo;
	}
	
	private int runGlbDefStmt(int rightp) {
		Node node;
		Node firstNode;
		KeywordTyp kwtyp;
		int savep = rightp;

		omsg("Keyword gdefun detected again.");
		node = store.getNode(rightp);
		firstNode = node;
		kwtyp = node.getKeywordTyp();
		if (kwtyp == KeywordTyp.ZPAREN) {
			rightp = node.getDownp();
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp != KeywordTyp.VAR) {
				return -1;
			}
			rightp = firstNode.getRightp();
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp == KeywordTyp.ZPAREN) {  // (ivar ...)
				rightp = node.getRightp();
				if (rightp <= 0) {
					return -1;
				}
				node = store.getNode(rightp);
			}
			kwtyp = node.getKeywordTyp();
		}
		if (kwtyp != KeywordTyp.DO) {
			omsg("Missing DO");
			return -1;
		}
		if (runGlbCall() < 0) {
			handleErrToken(STKOVERFLOW);
			return -1;
		}
		rightp = node.getDownp();
		if (rightp <= 0) {
			return savep;
		}
		rightp = handleToken(rightp);
		omsg("Stmt count = " + stmtCount);
		if (rightp < EXIT) {
			handleErrToken(rightp);
		}
		if (rightp == 0) {
			omsg("handleToken rtn = 0");  // done
		}
		omsg("runGlbDefStmt: btm");
		return savep;
	}
	
	private void handleErrToken(int rightp) {
		oprn("Runtime Error: " + convertErrToken(rightp));
	}
	
	private String convertErrToken(int rightp) {
		switch (rightp) {
		case NEGADDR: return "Nonpositive pointer encountered";
		case STKOVERFLOW: return "Stack overflow";
		case STKUNDERFLOW: return "Stack underflow";
		case BADSTMT: return "Unsupported stmt. type";
		case BADOP: return "Unsupported operator";
		case BADCELLTYP: return "Unsupported var/const type";
		case BADALLOC: return "Memory allocation failure";
		case BADPOP: return "Error in pop operation";
		case ZERODIV: return "Divide by zero";
		case KWDPOPPED: return "Keyword popped unexpectedly";
		default: return "Error code = " + (-rightp);
		}
	}
	
	private int handleToken(int rightp) {	
		KeywordTyp kwtyp;
		Node node;
		AddrNode addrNode;
		NodeCellTyp celltyp;
		Page page;
		int idx, varidx;
		int downp;
		int ival, rtnval;
		double dval;
		
		while (rightp >= 0) {
			while (rightp <= 0) {
				if (store.isOpStkEmpty()) {
					omsg("htok: top of while, empty op stk");
					return 0;  // done
				}
				kwtyp = popKwd();
				omsg("htok: kwtyp popped = " + kwtyp);
				rightp = handleKwd(kwtyp);
				if (!stmtClnStk(kwtyp)) {
					omsg("stmtClnStk fail! rightp = " + rightp);
					return STKUNDERFLOW;
				}
				if (kwtyp != KeywordTyp.RETURN) { }
				else if (rightp == EXIT) {
					omsg("isRtnExit = Y");
					store.popNode();
					continue;
				}
				else if (rightp == 0) {  // done
					return 0;
				}
				if (rightp < 0) {
					return rightp;
				}
				else if (rightp > 0) {
					break;
				}
				if (store.isNodeStkEmpty()) {
					return STKUNDERFLOW;
				}
				addrNode = store.popNode();
				rightp = addrNode.getAddr();
				if (rightp == 0) {
					omsg("htok: btm of while, rightp = zero");
				}
			}
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			omsg("htok: kwtyp = " + kwtyp);
			if (kwtyp == KeywordTyp.ZSTMT) {
				stmtCount++;
				omsg("htok: stmtCount = " + stmtCount);
				currZstmt = rightp;
				rightp = pushStmt(node);
			}
			else if (kwtyp == KeywordTyp.ZPAREN) {
				rightp = pushExpr(node);
			}
			else {
				varidx = node.getDownp();
				celltyp = node.getDownCellTyp();
				omsg("htok: celltyp = " + celltyp + ", downp = " + varidx);
				switch (celltyp) {
				case ID:
				case LOCVAR:
					if (!pushVar(varidx)) {
						return STKOVERFLOW;
					}
					omsg("htok: ID var = " + varidx);
					break;
				case FUNC:
					if (!pushInt(varidx)) {
						return STKOVERFLOW;
					}
					break;
				case INT:
					ival = varidx;
					if (!pushInt(ival)) {
						return STKOVERFLOW;
					}
					break;
				case DOUBLE:	
					downp = node.getDownp();
					page = store.getPage(downp);
					idx = store.getElemIdx(downp);
					dval = page.getDouble(idx);
					rtnval = pushFloat(dval);
					if (rtnval < 0) {
						return rtnval;
					}
					break;
				default: return BADCELLTYP;
				}
				rightp = node.getRightp();
			}
		} 
		return rightp;
	}
	
	private int pushStmt(Node node) {
		KeywordTyp kwtyp;
		int rightp;
		
		rightp = node.getRightp();
		if (!pushAddr(rightp)) {
			return STKOVERFLOW;
		}
		stkZstmt = store.getStkIdx();
		rightp = node.getDownp();
		if (rightp <= 0) {
			return NEGADDR;
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		switch (kwtyp) {
		case SET: 
			rightp = pushSetStmt(node);
			break;
		case PRINTLN: 
			rightp = pushPrintlnStmt(node);
			break;
		case ZCALL:
			omsg("pushStmt: ZCALL, currZstmt = " + currZstmt);
			rightp = pushZcallStmt(rightp);
			break;
		default: return BADSTMT;
		}
		return rightp;
	}
	
	private int pushExpr(Node node) {
		KeywordTyp kwtyp;
		KeywordTyp nullkwd;
		int rightp;
		
		rightp = node.getRightp();
		if (!pushAddr(rightp)) {
			return STKOVERFLOW;
		}
		rightp = node.getDownp();
		if (rightp <= 0) {
			return NEGADDR;
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		switch (kwtyp) {
		case ADD:
		case MPY:
			nullkwd = KeywordTyp.NULL; 
			if (!pushOp(kwtyp) || !pushOpAsNode(nullkwd)) {
				return STKOVERFLOW;
			}
			break;
		case MINUS:
		case DIV:
			if (!pushOp(kwtyp)) {
				return STKOVERFLOW;
			}
			break;
		default: return BADOP;
		}
		rightp = node.getRightp();
		return rightp;
	}
	
	private int runAddExpr() {
		int n;
		int val;
		int sum = 0;

		val = popInt(true);
		while (val != NULLPOPPED) {
			if (val < 0) {
				return val;
			}
			n = packIntSign(isNegInt, val);
			sum += n;
			val = popInt(true);
		}
		if (!pushInt(sum)) {
			return STKOVERFLOW;
		}
		return 0;
	}
	
	private int runMpyExpr() {
		int n;
		int val;
		int product = 1;

		val = popInt(true);
		while (val != NULLPOPPED) {
			if (val < 0) {
				return val;
			}
			n = packIntSign(isNegInt, val);
			product *= n;
			val = popInt(true);
		}
		if (!pushInt(product)) {
			return STKOVERFLOW;
		}
		return 0;
	}
	
	private int runDivExpr() {
		int m, n;
		int val;
		int ival;
		
		val = popInt(false);
		if (val < 0) {
			return val;
		}
		n = packIntSign(isNegInt, val);
		if (n == 0) {
			return ZERODIV;
		}
		val = popInt(false);
		if (val < 0) {
			return val;
		}
		m = packIntSign(isNegInt, val);
		ival = m / n;
		if (!pushInt(ival)) {
			return STKOVERFLOW;
		}
		return 0;
	}
	
	private int runMinusExpr() {
		int m, n;
		int val;
		int ival;
		
		val = popInt(false);
		if (val < 0) {
			return val;
		}
		n = packIntSign(isNegInt, val);
		val = popInt(false);
		if (val < 0) {
			return val;
		}
		m = packIntSign(isNegInt, val);
		ival = m - n;
		if (!pushInt(ival)) {
			return STKOVERFLOW;
		}
		return 0;
	}
	
	private int pushSetStmt(Node node) {
		int rightp;
		KeywordTyp kwtyp;
		
		omsg("pushSetStmt: top");
		kwtyp = KeywordTyp.SET;
		if (!pushOp(kwtyp)) {
			return STKOVERFLOW;
		}
		rightp = node.getRightp();
		return rightp;
	}
	
	private int runSetStmt() {
		int n;
		int val;
		
		omsg("runSetStmt: top");
		val = popInt(false);
		if (val < 0) {
			return val;
		}
		n = packIntSign(isNegInt, val);
		omsg("set stmt: value = " + n);
		return storeInt(n);
	}
	
	private int pushPrintlnStmt(Node node) {
		int rightp;
		KeywordTyp kwtyp;

		kwtyp = KeywordTyp.PRINTLN;
		if (!pushOp(kwtyp) || !pushOpAsNode(kwtyp)) {
			return STKOVERFLOW;
		}
		rightp = node.getRightp();
		return rightp;
	}
	
	private int runPrintlnStmt() {
		AddrNode addrNode;
		PageTyp pgtyp;
		int val;
		int addr;
		int count = 0;
		String msg = "";

		store.initSpareStkIdx();
		do {
			addrNode = store.popSpare();
			if (addrNode == null) {
				return STKUNDERFLOW;
			}
			addr = addrNode.getAddr();
			pgtyp = addrNode.getHdrPgTyp();
			omsg("runPrintlnStmt: addr = " + addr + ", pgtyp = " + pgtyp);
		} while (
			!(addr == KeywordTyp.PRINTLN.ordinal() && (
			pgtyp == PageTyp.KWD))
		);
		addrNode = store.fetchSpare();  // pop the PRINTLN
		if (addrNode == null) { 
			return STKOVERFLOW; 
		}
		while (true) {
			addrNode = store.fetchSpare();
			if (addrNode == null) {
				break;
			}
			addr = addrNode.getAddr();
			pgtyp = addrNode.getHdrPgTyp();
			//if (pgtyp != PageTyp.INTVAL) {
			//return BADPOP;
			val = popIntFromNode(addrNode);
			if (val < 0) {
				omsg("runPrintlnStmt: rtn = " + val);
				return val;
			}
			val = packIntSign(isNegInt, val);
			msg = msg + val + SP;
			count++;
		}
		if (count > 0) {
			oprn(msg);
		}
		do {
			addrNode = store.popNode();
			if (addrNode == null) {
				return STKUNDERFLOW;
			}
			addr = addrNode.getAddr();
			pgtyp = addrNode.getHdrPgTyp();
		} while (
			!(addr == KeywordTyp.PRINTLN.ordinal() && (
			pgtyp == PageTyp.KWD))
		);
		return 0;
	}
			
	private int pushZcallStmt(int rightp) {
		// assume no args.
		// after handling args., may need to call pushOpAsNode
		
		//Node node;
		KeywordTyp kwtyp;
		
		omsg("pushZcallStmt: top");
		//node = store.getNode(rightp);
		kwtyp = KeywordTyp.ZCALL;
		if (!pushOp(kwtyp)) {
			return STKOVERFLOW;
		}
		//rightp = node.getRightp();
		return rightp;
	}
	
	private int runZcallStmt() {
		// - pop func ref. (idx in func list)
		// - get ptr to func def parm list
		// - set locBaseIdx
		// - push loc vars
		// - set varCountIdx
		// - push varCount
		// - push currZstmt
		// - push RETURN
		// - go to func body: return ptr to first zstmt
		int downp;
		int funcidx;
		int upNodep;
		int funcp;
		int firstp;
		Node node = null;
		Node upNode;
		String funcName;
		int varCount = 0;
		int i, j;
		
		omsg("runZcallStmt: top");
		funcidx = popIdxVal();
		if (funcidx < 0) {
			return STKUNDERFLOW;
		}
		omsg("Zcall: funcidx = " + funcidx);
		upNodep = glbFunList.get(funcidx);
		upNode = store.getNode(upNodep);
		funcp = upNode.getDownp();
		firstp = upNode.getRightp();
		node = store.getNode(firstp);
		firstp = node.getDownp();  // return firstp
		node = store.getNode(funcp);
		downp = node.getDownp();  // (_f_ x y)
		node = store.getNode(downp);
		downp = node.getDownp();
		funcName = store.getVarName(downp);
		i = glbLocVarMap.get(funcName);
		// old currZstmt at top of stack
		locBaseIdx = store.getStkIdx();
		while (true) {
			j = glbLocVarList.get(i + varCount);
			if (j < 0) {
				break;
			}
			if (!pushVal(varCount, PageTyp.INTVAL, LOCVAR)) {
				return STKOVERFLOW;
			}
			varCount++;
		}
		varCountIdx = store.getStkIdx();
		if (!pushInt(varCount)) {
			return STKOVERFLOW;
		}
		if (!pushInt(currZstmt)) {
			return STKOVERFLOW;
		}
		if (!pushOp(KeywordTyp.RETURN)) {
			return STKOVERFLOW;
		}
		return firstp;
	}
	
	private int runRtnStmt() {
		// - already popped RETURN
		// - pop currZstmt
		// - pop varCount
		// - pop loc vars
		// - if currZstmt then done
		// - pop old currZstmt
		// - varCount = top
		// - set locBaseIdx, varCountIdx
		// - push old currZstmt
		// - return getRightp of currZstmt
		int rightp;
		int zstmt;
		int varCount;
		int i;
		Node node;
		AddrNode addrNode;
		
		rightp = popIdxVal(); // currZstmt
		if (rightp < 0) {
			return STKUNDERFLOW;
		}
		varCount = popVal(); // varCount
		if (varCount < 0) {
			return STKUNDERFLOW;
		}
		for (i = 0; i < varCount; i++) {
			if (popVal() < 0) {
				return STKUNDERFLOW;
			}
		}
		if (rightp == 0) {
			omsg("runRtnStmt: done");
			return 0; // done
		}
		zstmt = popVal();
		if (zstmt < 0) {
			return STKUNDERFLOW;
		}
		varCountIdx = store.getStkIdx() - 1;
		addrNode = store.topNode();
		if (addrNode == null) {
			return STKUNDERFLOW;
		}
		varCount = addrNode.getAddr();
		if (varCount < 0) {
			return NEGADDR;
		}
		locBaseIdx = varCountIdx - varCount;
		if (!pushInt(zstmt)) {
			return STKOVERFLOW;
		}
		node = store.getNode(rightp);
		rightp = node.getRightp();
		omsg("runRtnStmt: rtnval = " + rightp);
		if (rightp == 0) {
			return EXIT;
		}
		return rightp;  // never returns zero
	}
	
	private int runGlbCall() {
		int i, j;
		
		i = 0;
		locBaseIdx = store.getStkIdx();
		omsg("runGlbCall: locBaseIdx = " + locBaseIdx);
		while (true) {
			j = glbLocVarList.get(i);
			if (j < 0) {
				break;
			}
			if (!pushVal(i, PageTyp.INTVAL, GLBVAR)) {
				return STKOVERFLOW;
			}
			i++;
		}
		varCountIdx = store.getStkIdx();
		if (!pushInt(i)) {
			return STKOVERFLOW;
		}
		if (!pushInt(0)) {
			return STKOVERFLOW;
		}
		return 0;
	}
	
	private int handleKwd(KeywordTyp kwtyp) {
		switch (kwtyp) {
		case SET: return runSetStmt();
		case PRINTLN: return runPrintlnStmt();
		case ZCALL: return runZcallStmt();
		case RETURN: return runRtnStmt();
		case ADD: return runAddExpr();
		case MPY: return runMpyExpr();
		case MINUS: return runMinusExpr();
		case DIV: return runDivExpr();
		default:
			return BADOP;
		}
	}
	
	private boolean stmtClnStk(KeywordTyp kwtyp) {
		switch (kwtyp) {
		case ZCALL:
		case RETURN:
			return true;
		default:
			return (stkZstmt == store.getStkIdx());
		}
	}
	
	private KeywordTyp popKwd() {
		KeywordTyp kwtyp;
		int ival;
		
		ival = (int)store.popByte();
		kwtyp = KeywordTyp.values[ival];
		return kwtyp;
	}
	
	public int stripIntSign(int val) {
		if (val == 0x80000000) {
			return 0;
		}
		return val;
	}
	
	public int packIntSign(boolean isNeg, int val) {
		if (isNeg && (val == 0)) {
			return 0x80000000;
		}
		val = isNeg ? -val : val;
		return val;
	}
	
	private int popIdxVal() {
		AddrNode addrNode;
		int rtnval = -1;
		
		addrNode = store.popNode();
		if (addrNode != null) {
			rtnval = addrNode.getAddr();
		}
		return rtnval;
	}
	
	private int popInt(boolean isKwd) {
		// assume set stmt. only handles integer vars/values
		// isKwd: if null popped, then not an error
		AddrNode addrNode;
		
		addrNode = store.popNode();
		return popIntRtn(addrNode, isKwd);
	}
	
	private int popIntFromNode(AddrNode addrNode) {
		return popIntRtn(addrNode, false);
	}
	
	private int popIntRtn(AddrNode addrNode, boolean isKwd) {
		// assume set stmt. only handles integer vars/values
		// isKwd: if null popped, then not an error
		PageTyp pgtyp;
		int locVarTyp;
		int addr, varidx;
		int rtnval;
		
		if (addrNode == null) {
			return STKUNDERFLOW;
		}
		addr = addrNode.getAddr();
		pgtyp = addrNode.getHdrPgTyp(); 
		if (pgtyp != PageTyp.KWD) { }
		else if (!isKwd || (addr != KeywordTyp.NULL.ordinal())) {
			return KWDPOPPED;
		}
		else {
			return NULLPOPPED;  // not an error
		}
		locVarTyp = addrNode.getHdrLocVarTyp();
		switch (locVarTyp) {
		case NONVAR: 
			return addr;
		case LOCVAR:
		case GLBVAR:
			varidx = addr;
			if (addrNode.getHdrLocVar()) {
				varidx += locBaseIdx;
			}
			addrNode = store.fetchNode(varidx);
			pgtyp = addrNode.getHdrPgTyp(); 
			if (pgtyp != PageTyp.INTVAL) {
				return BADPOP;
			}
			rtnval = addrNode.getAddr();
			isNegInt = (rtnval < 0);
			rtnval = stripIntSign(rtnval);
			break;
		default: rtnval = BADPOP;
		}
		return rtnval;
	}
	
	private int popVal() {
		AddrNode node;
		int val;
		
		node = store.popNode();
		if (node == null) {
			return -1;
		}
		val = node.getAddr();
		return val;
	}
	
	private int storeInt(int ival) {
		// assume set stmt. only handles integer vars/values
		AddrNode addrNode;
		PageTyp pgtyp;
		int locVarTyp;
		int addr, varidx;
		
		addrNode = store.popNode();
		if (addrNode == null) {
			return STKUNDERFLOW;
		}
		addr = addrNode.getAddr();
		pgtyp = addrNode.getHdrPgTyp(); 
		if (pgtyp != PageTyp.INTVAL) { 
			return BADSTORE;  
		}
		locVarTyp = addrNode.getHdrLocVarTyp();
		switch (locVarTyp) {
		case NONVAR: 
			return KWDPOPPED;
		case LOCVAR:
		case GLBVAR:
			varidx = addr;
			omsg("storeInt: varidx = " + varidx);
			//-----------if (addrNode.getHdrLocVar()) { }
			varidx += locBaseIdx;
			addrNode = store.fetchNode(varidx);
			pgtyp = addrNode.getHdrPgTyp(); 
			if (pgtyp != PageTyp.INTVAL) {
				return BADPOP;
			}
			//--------------rightp = addrNode.getAddr();
			store.writeNode(varidx, ival);
			break;
		default: return BADPOP;
		}
		return 0;
	}
	
	private boolean pushVal(int val, PageTyp pgtyp, int locVarTyp) {
		AddrNode addrNode;
		addrNode = new AddrNode(0, val);
		addrNode.setHdrPgTyp(pgtyp);
		addrNode.setHdrLocVarTyp(locVarTyp);
		if (!store.pushNode(addrNode)) {
			return false;
		}
		return true;
	}
	
	private boolean pushAddr(int rightp) {
		AddrNode addrNode;
		addrNode = new AddrNode(0, rightp);
		if (!store.pushNode(addrNode)) {
			return false;
		}
		return true;
	}
	
	private int pushFloat(double val) {
		AddrNode addrNode;
		int addr;
		
		addr = store.allocDouble(val);
		if (addr < 0) {
			return BADALLOC;
		}
		addrNode = new AddrNode(0, addr);
		addrNode.setHdrPgTyp(PageTyp.DOUBLE);
		if (!store.pushNode(addrNode)) {
			return STKOVERFLOW;
		}
		return 0;
	}
	
	private boolean pushInt(int val) {
		AddrNode addrNode;
		addrNode = new AddrNode(0, val);
		addrNode.setHdrPgTyp(PageTyp.INTVAL);
		if (!store.pushNode(addrNode)) {
			return false;
		}
		return true;
	}
	
	private boolean pushOp(KeywordTyp kwtyp) {
		byte byt = (byte)(kwtyp.ordinal());
		if (!store.pushByte(byt)) {
			return false;
		}
		return true;
	}
	
	private boolean pushOpAsNode(KeywordTyp kwtyp) {
		int val = kwtyp.ordinal();
		AddrNode addrNode;
		
		addrNode = new AddrNode(0, val);
		addrNode.setHdrPgTyp(PageTyp.KWD);
		if (!store.pushNode(addrNode)) {
			return false;
		}
		return true;
	}
	
	private boolean pushVar(int varidx) {
		boolean isLocal;
		int stkidx;
		int locVarTyp;
		PageTyp pgtyp;
		AddrNode addrNode;
		
		isLocal = (varidx >= 0);
		if (isLocal) {
			locVarTyp = LOCVAR;
		}
		else {
			varidx = -1 - varidx;
			locVarTyp = GLBVAR;
		}
		stkidx = locBaseIdx + varidx;
		addrNode = store.fetchNode(stkidx);
		pgtyp = addrNode.getHdrPgTyp();
		if (!pushVal(varidx, pgtyp, locVarTyp)) {
			return false;
		}
		return true;
	}
	
	private String getGdefunWord() {
		return "gdefun";
	}
	
	private boolean isGdefun(String s) {
		return s.equals("gdefun");
	}
	
}
