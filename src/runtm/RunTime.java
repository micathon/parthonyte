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
	private int popMultiFreeCount;
	private int currZstmt;
	private int currZexpr;
	private int locDepth;
	private boolean isTgtExpr;
	private boolean isCalcExpr;
	private boolean isExprLoop;
	private boolean isNegInt;
	private int lastErrCode;
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
	private static final int BADINTVAL = -14;
	private static final int STMTINEXPR = -15;
	private static final int BADZSTMT = -16;
	private static final int BADSETSTMT = -17;
	private static final int BADPARMCT = -18;
	private static final int RTNISEMPTY = -19;
	private static final int BADTYPE = -20;
	private static final int GENERR = -99;
	private static final int NEGBASEVAL = -1000;
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
		locDepth = 0;
		lastErrCode = 0;
		popMultiFreeCount = 0;
		isTgtExpr = false;
		isCalcExpr = false;
		isExprLoop = false;
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
		if (!pushInt(currZstmt)) {
			return STKOVERFLOW;
		}
		if (!pushInt(locDepth)) {
			return STKOVERFLOW;
		}
		rightp = handleDoBlock(node);
		omsg("Stmt count = " + stmtCount);
		if (rightp < EXIT) {
			handleErrToken(rightp);
		}
		if (rightp == 0) {
			omsg("handleDoBlock rtn = 0");  // done
		}
		omsg("runGlbDefStmt: popFreeCount = " + popMultiFreeCount);
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
		case BADINTVAL: return "Integer data expected after pop operation";
		case STMTINEXPR: return "Statement encountered in expression";
		case BADZSTMT: return "Expression encountered at stmt. level";
		case BADSETSTMT: return "Malformed SET statement";
		case BADPARMCT: return "Mismatched parameter count";
		case RTNISEMPTY: return "Return stmt. lacks value";
		case BADTYPE: return "Unexpected data type";
		case GENERR: return "General runtime error";
		default: return "Error code = " + (-rightp);
		}
	}
	
	private int handleDoBlock(Node node) {	
		KeywordTyp kwtyp;
		int rightp;

		rightp = node.getDownp();
		if (rightp <= 0) {
			return 0;
		}
		pushOp(KeywordTyp.NULL);
		while (rightp > 0) {
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			omsg("doblock: kwtyp = " + kwtyp + ", rightp = " + rightp);
			if (kwtyp != KeywordTyp.ZSTMT) {
				return BADZSTMT;
			}
			stmtCount++;
			omsg("doblock: stmtCount = " + stmtCount);
			currZstmt = rightp;
			locDepth = 0;
			rightp = pushStmt(node);
			do {
				isExprLoop = false;
				rightp = handleExprToken(rightp, false);
				if (rightp >= 0) {
					return BADOP;
				}
				if (rightp == EXIT) {
					return 0;
				}
				if (rightp > NEGBASEVAL) {
					return rightp;
				}
				rightp = -(rightp - NEGBASEVAL);
				kwtyp = KeywordTyp.values[rightp];
				omsg("handleDoBlock: btm2, kwtyp = " + kwtyp);
				rightp = handleStmtKwd(kwtyp);
				omsg("handleDoBlock: btm2, rightp = " + rightp);
			} while (isExprLoop);
			while (rightp == 0) {
				rightp = runRtnStmt(false);
				omsg("handleDoBlock: btm, rightp = " + rightp);
				if (rightp == EXIT) {
					return 0;
				}
			}
		} 
		return rightp;
	}
	
	private int handleExprToken(int rightp, boolean isSingle) {	
		KeywordTyp kwtyp;
		Node node;
		boolean found = false;
		
		omsg("exprtok: top, rightp = " + rightp);
		while (rightp >= 0) {
			while ((rightp <= 0) && !found) {
				if (store.isOpStkEmpty()) {
					omsg("exprtok: top of while, empty op stk");
					return STKUNDERFLOW;
				}
				found = isSingle && (locDepth <= 0);
				kwtyp = popKwd();
				if (kwtyp == KeywordTyp.NULL && store.isOpStkEmpty()) {
					return EXIT;
				}
				omsg("exprtok: kwtyp popped = " + kwtyp);
				if (kwtyp == KeywordTyp.ZCALL && locDepth > 0) {  
					omsg("exprtok: function call, locDepth = " + locDepth);
				}
				rightp = handleExprKwd(kwtyp);
				omsg("exprtok: kwtyp = " + kwtyp + ", rightp = " + rightp);
				if (rightp < 0) {
					return rightp;  // err if > NEGBASEVAL
				}
				locDepth--;
			}
			if (found) {
				break;
			}
			omsg("exprtok: node -> rightp = " + rightp);
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			omsg("exprtok: btm kwtyp = " + kwtyp + ", rightp = " + rightp);
			if (kwtyp == KeywordTyp.ZSTMT) {
				return STMTINEXPR;
			}
			if (kwtyp == KeywordTyp.ZPAREN) {
				locDepth++;
				currZexpr = rightp;
				rightp = pushExpr(node);
			}
			else {
				rightp = handleLeafToken(node);
				found = isSingle && (locDepth <= 0);
			}
		} 
		return rightp;
	}
	
	private int handleLeafToken(Node node) {
		return handleLeafTokenRtn(node, false);
	}
	
	private int handleLeafTokenQuote(Node node) {
		return handleLeafTokenRtn(node, true);
	}
	
	private int handleLeafTokenRtn(Node node, boolean isQuote) {
		NodeCellTyp celltyp;
		Page page;
		int idx, varidx;
		int downp;
		int rightp;
		int ival, rtnval;
		double dval;
		String sval;

		varidx = node.getDownp();
		celltyp = node.getDownCellTyp();
		omsg("htok: celltyp = " + celltyp + ", downp = " + varidx);
		if (isQuote) {
			switch (celltyp) {
			case ID:
			case LOCVAR:
				if (!pushVarQuote(varidx)) {
					return STKOVERFLOW;
				}
				rightp = node.getRightp();
				return rightp;
			default:
				return BADCELLTYP;
			}
		}
		switch (celltyp) {
		case ID:
		case LOCVAR:
			if (!pushVar(varidx)) {
				return STKOVERFLOW;
			}
			break;
		case FUNC:
			if (!pushInt(varidx)) {
				return STKOVERFLOW;
			}
			break;
		case INT:
			ival = varidx;
			omsg("htok: push INT = " + ival);
			if (!pushIntStk(ival)) {
				return STKOVERFLOW;
			}
			break;
		case FLOAT:	
			downp = node.getDownp();
			page = store.getPage(downp);
			idx = store.getElemIdx(downp);
			dval = page.getFloat(idx);
			rtnval = pushFloat(dval);
			omsg("htok: float = " + dval);
			if (rtnval < 0) {
				return rtnval;
			}
			break;
		case STRING:	
			downp = node.getDownp();
			page = store.getPage(downp);
			idx = store.getElemIdx(downp);
			sval = page.getString(idx);
			rtnval = pushString(sval);
			omsg("htok: string = " + sval);
			if (rtnval < 0) {
				return rtnval;
			}
			break;
		default: return BADCELLTYP;
		}
		rightp = node.getRightp();
		return rightp;
	}
	
	private int handleExprKwd(KeywordTyp kwtyp) {
		int rightp;
		AddrNode addrNode;
		
		rightp = handleExprKwdRtn(kwtyp);
		if (rightp < 0) {
			return rightp;
		}
		if (!store.swapNodes()) {
			return STKUNDERFLOW;
		}
		addrNode = store.popNode();
		rightp = addrNode.getAddr();
		return rightp;
	}
	
	private int handleExprKwdRtn(KeywordTyp kwtyp) {
		switch (kwtyp) {
		case ADD: return runAddExpr();
		case MPY: return runMpyExpr();
		case MINUS: return runMinusExpr();
		case DIV: return runDivExpr();
		case ZCALL:
			break;
		default:
			//
		}
		return NEGBASEVAL - kwtyp.ordinal();
	}

	private int handleStmtKwd(KeywordTyp kwtyp) {
		int rightp;
		AddrNode addrNode;
		
		rightp = handleStmtKwdRtn(kwtyp);
		if ((rightp < 0) || isJumpKwd(kwtyp)) {
			return rightp;
		}
		addrNode = store.popNode();
		rightp = addrNode.getAddr();
		return rightp;
	}
	
	private int handleStmtKwdRtn(KeywordTyp kwtyp) {
		switch (kwtyp) {
		case SET: return runSetStmt();
		case PRINTLN: return runPrintlnStmt(kwtyp);
		case ZCALL: return runZcallStmt();
		case RETURN: return runRtnStmt(true);
		default:
			return BADOP;
		}
	}
	
	private boolean isJumpKwd(KeywordTyp kwtyp) {
		omsg("isJumpKwd: kwtyp = " + kwtyp);
		switch (kwtyp) {
		case ZCALL:
		case RETURN:
			return true;
		default:
			return false;
		}
	}
	
	private int pushStmt(Node node) {
		KeywordTyp kwtyp;
		int rightp, rightq;
		
		rightq = node.getRightp();
		rightp = node.getDownp();
		if (rightp <= 0) {
			return NEGADDR;
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (isJumpKwd(kwtyp)) { }
		else if (!pushAddr(rightq)) {
			return STKOVERFLOW;
		}
		switch (kwtyp) {
		case SET: 
			rightp = pushSetStmt(node);
			break;
		case PRINTLN: 
			rightp = pushPrintlnStmt(node);
			break;
		case ZCALL:
			rightp = pushZcallStmt(node);
			break;
		case RETURN:
			rightp = pushRtnStmt(node);
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
		omsg("pushExpr: kwtyp = " + kwtyp + ", rightp = " + rightp);
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
		case ZCALL:
			if (popVal() < 0) {
				return STKUNDERFLOW;
			}
			if (!pushOp(kwtyp) || !pushOpAsNode(kwtyp) ||
				!pushInt(currZexpr)) 
			{
				return STKOVERFLOW;
			}
			rightp = handleLeafToken(node);
			return rightp;
		default: return BADOP;
		}
		rightp = node.getRightp();
		return rightp;
	}
	
	private int runAddExpr() {
		int val = 0;
		int sum = 0;
		AddrNode addrNode;
		Page page;
		int addr;
		int idx;
		double fsum = 0.0;
		double dval = 0.0;
		int stkidx;
		boolean isFloat;
		boolean isResFloat = false;
		int rtnval;
		
		while (true) {
			stkidx = popIntStk();
			if (stkidx < 0) {
				return stkidx;
			}
			addrNode = store.fetchNode(stkidx);
			if (isNullKwd(addrNode)) {
				break;
			}
			addr = addrNode.getAddr();
			isFloat = (addrNode.getHdrPgTyp() == PageTyp.FLOAT);
			isResFloat = isResFloat || isFloat;
			if (isFloat) {
				page = store.getPage(addr);
				idx = store.getElemIdx(addr);
				dval = page.getFloat(idx);
			}
			else {
				val = getIntOffStk(stkidx);
			}
			if (!isResFloat) {
				sum += val;
				fsum = sum;
			}
			else if (isFloat) {
				fsum += dval;
			}
			else {
				fsum += val;
			}
		}
		if (!isResFloat) { 
			rtnval = pushIntStk(sum) ? 0 : STKOVERFLOW;
			return rtnval;
		}
		rtnval = pushFloat(fsum);
		return rtnval;
	}
	
	private int runMpyExpr() {
		int product = 1;
		double fproduct = 1.0;
		int val = 0;
		AddrNode addrNode;
		Page page;
		int addr;
		int idx;
		double dval = 0.0;
		int stkidx;
		boolean isFloat;
		boolean isResFloat = false;
		int rtnval;

		while (true) {
			stkidx = popIntStk();
			if (stkidx < 0) {
				return stkidx;
			}
			addrNode = store.fetchNode(stkidx);
			if (isNullKwd(addrNode)) {
				break;
			}
			addr = addrNode.getAddr();
			isFloat = (addrNode.getHdrPgTyp() == PageTyp.FLOAT);
			isResFloat = isResFloat || isFloat;
			if (isFloat) {
				page = store.getPage(addr);
				idx = store.getElemIdx(addr);
				dval = page.getFloat(idx);
			}
			else {
				val = getIntOffStk(stkidx);
			}
			if (!isResFloat) {
				product *= val;
				fproduct = product;
			}
			else if (isFloat) {
				fproduct *= dval;
			}
			else {
				fproduct *= val;
			}
		}
		if (!isResFloat) { 
			rtnval = pushIntStk(product) ? 0 : STKOVERFLOW;
			return rtnval;
		}
		rtnval = pushFloat(fproduct);
		return rtnval;
	}
	
	private int runDivExpr() {
		AddrNode addrNode;
		Page page;
		int addr;
		int idx;
		double denom;
		double base;
		double quotient;
		int stkidx;
		boolean isFloat;
		int rtnval;
		
		omsg("runDivExpr: top");
		stkidx = popIntStk();
		if (stkidx < 0) {
			return stkidx;
		}
		addrNode = store.fetchNode(stkidx);
		addr = addrNode.getAddr();
		isFloat = (addrNode.getHdrPgTyp() == PageTyp.FLOAT);
		if (isFloat) {
			page = store.getPage(addr);
			idx = store.getElemIdx(addr);
			denom = page.getFloat(idx);
		}
		else {
			denom = getIntOffStk(stkidx);
		}
		if (denom == 0.0) {
			return ZERODIV;
		}
		stkidx = popIntStk();
		if (stkidx < 0) {
			return stkidx;
		}
		addrNode = store.fetchNode(stkidx);
		addr = addrNode.getAddr();
		isFloat = (addrNode.getHdrPgTyp() == PageTyp.FLOAT);
		if (isFloat) {
			page = store.getPage(addr);
			idx = store.getElemIdx(addr);
			base = page.getFloat(idx);
		}
		else {
			base = getIntOffStk(stkidx);
		}
		quotient = base / denom;
		rtnval = pushFloat(quotient);
		return rtnval;
	}
	
	private int runMinusExpr() {
		AddrNode addrNode;
		Page page;
		int addr;
		int idx;
		int delta = 0;
		int base = 0;
		int diff = 0;
		int stkidx;
		int rtnval;
		boolean isDeltaFloat;
		boolean isBaseFloat;
		boolean isInt;
		double fdelta = 0.0;
		double fbase = 0.0;
		double fdiff = 0.0;
		
		omsg("runMinusExpr: top");
		stkidx = popIntStk();
		if (stkidx < 0) {
			return stkidx;
		}
		addrNode = store.fetchNode(stkidx);
		addr = addrNode.getAddr();
		isDeltaFloat = (addrNode.getHdrPgTyp() == PageTyp.FLOAT);
		if (isDeltaFloat) {
			page = store.getPage(addr);
			idx = store.getElemIdx(addr);
			fdelta = page.getFloat(idx);
		}
		else {
			delta = getIntOffStk(stkidx);
		}
		stkidx = popIntStk();
		if (stkidx < 0) {
			return stkidx;
		}
		addrNode = store.fetchNode(stkidx);
		addr = addrNode.getAddr();
		isBaseFloat = (addrNode.getHdrPgTyp() == PageTyp.FLOAT);
		if (isBaseFloat) {
			page = store.getPage(addr);
			idx = store.getElemIdx(addr);
			fbase = page.getFloat(idx);
		}
		else {
			base = getIntOffStk(stkidx);
		}
		isInt = !isDeltaFloat && !isBaseFloat;
		if (isInt) {
			diff = base - delta;
		}
		else if (isDeltaFloat && isBaseFloat) {
			fdiff = fbase - fdelta;
		}
		else if (isDeltaFloat) {
			fdiff = base - fdelta;
		}
		else {
			fdiff = fbase - delta;
		}
		omsg("runMinusExpr: base, delta, diff = " + base +
				" " + delta + " " + diff);
		omsg("runMinusExpr: fbase, fdelta, fdiff = " + fbase +
				" " + fdelta + " " + fdiff);
		if (isInt) { 
			rtnval = pushIntStk(diff) ? 0 : STKOVERFLOW;
			return rtnval;
		}
		rtnval = pushFloat(fdiff);
		return rtnval;
	}
	
	private int runSetStmt() {
		int val;
		int stkidx;
		AddrNode addrNode;
		PageTyp pgtyp;
		
		omsg("runSetStmt: top");
		stkidx = popIntStk();
		if (stkidx < 0) {
			return stkidx;
		}
		//val = getIntOffStk(stkidx);
		addrNode = store.fetchNode(stkidx);
		val = addrNode.getAddr();
		pgtyp = addrNode.getHdrPgTyp();
		//oprn("runSetStmt: pgtyp of expr = " + pgtyp);
		omsg("set stmt: value = " + val + ", stkidx = " + stkidx);
		return storeInt(addrNode);
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
		if (rightp <= 0) {
			return BADSETSTMT;
		}
		node = store.getNode(rightp);
		rightp = handleLeafTokenQuote(node);
		if (rightp <= 0) {
			return BADSETSTMT;
		}
		rightp = handleExprToken(rightp, true);
		return rightp;
	}
	
	private int pushPrintlnStmt(Node node) {
		int rightp;
		KeywordTyp kwtyp;

		omsg("pushPrintlnStmt: top");
		kwtyp = KeywordTyp.PRINTLN;
		if (!pushOp(kwtyp) || !pushOpAsNode(kwtyp)) {
			return STKOVERFLOW;
		}
		rightp = node.getRightp();
		return rightp;
	}
	
	private int runPrintlnStmt(KeywordTyp kwtyp) {
		AddrNode addrNode;
		int val;
		int count;
		int rtnval;
		String msg = "";
		String s;
		PageTyp pgtyp;

		count = getCountOfSpares(kwtyp);
		omsg("runPrintlnStmt: count of spares = " + count);
		count = 0;
		while (true) {
			addrNode = store.fetchSpare();
			if (addrNode == null) {
				break;
			}
			/*
			val = popIntFromNode(addrNode);
			if (val < 0) {
				omsg("runPrintlnStmt: rtn = " + val + ", count = " + count);
				return val;
			}
			val = packIntSign(isNegInt, val);
			*/
			s = popStrFromNode(addrNode);
			if (s.length() > 0) {
				msg = msg + s + SP;
			}
			//msg = msg + val + SP;
			omsg("runPrintlnStmt: msg = " + msg);
			count++;
		}
		if (count > 0) {
			oprn(msg);
		}
		rtnval = popUntilKwd(kwtyp);
		return rtnval;
	}
	
	private int popUntilKwd(KeywordTyp kwtyp) {
		AddrNode addrNode;
		PageTyp pgtyp;
		int addr;
		int count;
		
		count = 0;
		do {
			count++;
			addrNode = store.popNode();
			if (addrNode == null) {
				return STKUNDERFLOW;
			}
			addr = addrNode.getAddr();
			pgtyp = addrNode.getHdrPgTyp();
		} while (
			!(addr == kwtyp.ordinal() && (
			pgtyp == PageTyp.KWD))
		);
		omsg("popUntilKwd: btm, count = " + count);
		return 0;
	}
	
	private int runAltZcallStmt(int parmCount) {
		AddrNode addrNode;
		PageTyp pgtyp;
		Integer val;
		int rtnval;
		int i;

		for (i = 0; i < parmCount; i++) {
			addrNode = store.fetchSpare();
			if (addrNode == null) {
				return STKOVERFLOW;
			}
			pgtyp = addrNode.getHdrPgTyp();
			val = popIObjFromNode(addrNode);
			if (val == null) {
				omsg("runAltZcallStmt: rtn = " + lastErrCode + 
					", i = " + i);
				return lastErrCode;
			}
			omsg("runAltZcallStmt: i/pc = " + i + "/" + parmCount +
				", val = " + val +
				", pgtyp = " + pgtyp);
			rtnval = storeLocGlbInt(i, val, pgtyp, false);
			if (rtnval < 0) {
				return rtnval;
			}
		}
		return 0;
	}
	
	private int getCountOfSpares(KeywordTyp kwtyp) {
		AddrNode addrNode;
		PageTyp pgtyp;
		int addr;
		int count = 0;
		
		store.initSpareStkIdx();
		while (true) {
			addrNode = store.popSpare();
			if (addrNode == null) {
				return STKUNDERFLOW;
			}
			addr = addrNode.getAddr();
			pgtyp = addrNode.getHdrPgTyp();
			omsg("getCountOfSpares: addr = " + addr + ", pgtyp = " + pgtyp);
			if ((addr == kwtyp.ordinal()) && (pgtyp == PageTyp.KWD)) {
				break;
			}
			count++;
		}
		addrNode = store.fetchSpare();  // pop the PRINTLN/ZCALL
		if (addrNode == null) { 
			return STKOVERFLOW; 
		}
		return count;
	}
	
	private int getReturnpSpares() {
		AddrNode addrNode;
		int addr;
	
		addrNode = store.fetchSpare();  // pop the returnp
		if (addrNode == null) {
			return STKOVERFLOW; 
		}
		addr = addrNode.getAddr();
		omsg("getReturnpSpares: rtn addr = " + addr);
		return addr;
	}
			
	private int getFuncIdxSpares() {
		AddrNode addrNode;
		int addr;
	
		addrNode = store.fetchSpare();  // pop the func var
		if (addrNode == null) {
			return STKOVERFLOW; 
		}
		addr = addrNode.getAddr();
		omsg("getFuncIdxSpares: rtn addr = " + addr);
		return addr;
	}
			
	private int pushZcallStmt(Node node) {
		KeywordTyp kwtyp;
		int rightp;
		
		omsg("pushZcallStmt: top");
		kwtyp = KeywordTyp.ZCALL;
		if (!pushOp(kwtyp) || !pushOpAsNode(kwtyp) || !pushInt(currZstmt)) {
			return STKOVERFLOW;
		}
		rightp = handleLeafToken(node);
		return rightp;
	}
	
	private int runZcallStmt() {
		// - popSpare returnp (go back here)
		// - popSpare func ref. (idx in func list)
		// - get ptr to func def parm list
		// - set locBaseIdx
		// - push parms, loc vars
		// - push varCount
		// - push old locBaseIdx
		// - push returnp
		// - push old local depth
		// - go to func body: return ptr to first zstmt
		int downp;
		int funcidx;
		int upNodep;
		int funcp;
		int firstp;
		int returnp;
		int currLocBase;
		Node node = null;
		Node upNode;
		KeywordTyp kwtyp = KeywordTyp.ZCALL;
		String funcName;
		String varName;
		int varCount = 0;  // includes parms, loc vars
		int parmCount;
		int parmFixedCount;
		int i, j;
		int rtnval;
		
		omsg("runZcallStmt: top");
		currLocBase = locBaseIdx;
		parmCount = getCountOfSpares(kwtyp) - 2;
		returnp = getReturnpSpares();
		if (returnp < 0) {
			return STKUNDERFLOW;
		}
		funcidx = getFuncIdxSpares();
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
		varName = rscan.getFunVar(funcName);
		parmFixedCount = glbLocVarMap.get(varName);  
		if (parmFixedCount != parmCount) {
			return BADPARMCT;
		}
		i = glbLocVarMap.get(funcName);
		locBaseIdx = store.getStkIdx() - parmCount;
		while (true) {
			if (varCount < parmCount) {
				varCount++;
				continue;
			}
			j = glbLocVarList.get(i + varCount);
			if (j < 0) {
				break;
			}
			omsg("runZcallStmt: stkidx = " + store.getStkIdx());
			if (!pushVal(varCount, PageTyp.INTVAL, LOCVAR)) {
				return STKOVERFLOW;
			}
			varCount++;
		}
		rtnval = runAltZcallStmt(parmCount);
		if (rtnval < 0) {
			omsg("Zcall: rtnval = " + rtnval);
			return rtnval;
		}
		if (!pushInt(varCount)) {
			return STKOVERFLOW;
		}
		if (!pushInt(currLocBase)) {
			return STKOVERFLOW;
		}
		if (!pushInt(returnp)) {
			return STKOVERFLOW;
		}
		omsg("Zcall: locDepth = " + locDepth);
		if (!pushInt(locDepth)) {
			return STKOVERFLOW;
		}
		omsg("Zcall: btm, firstp = " + firstp);
		return firstp;
	}
	
	private int runRtnStmt(boolean isExpr) {
		// - pop return value if any
		// - pop local depth
		// - pop returnp (go back here)
		// - if returnp=0 then done
		// - pop calling locBaseIdx
		// - pop varCount
		// - pop loc vars, parms
		// - pop until ZCALL, inclusive
		// - push return value if any
		// - set local depth
		// - return getRightp of returnp
		int rightp;
		int currLocBase;
		int varCount;
		int funcAddr = 0;
		int i;
		int rtnval;
		Integer val;
		boolean isDelayPops = false;
		AddrNode funcReturns = null;
		Node node;
		
		omsg("runRtnStmt: top");
		if (isExpr) {
			funcReturns = store.popNode(); 
			if (funcReturns == null) {
				return STKUNDERFLOW;
			}
			val = popIObjFromNode(funcReturns);
			if (val == null) {
				omsg("runRtnStmt: isExpr, rtn = " + lastErrCode); 
				return lastErrCode;
			}
			funcAddr = val;
			isExprLoop = true;
			omsg("runRtnStmt: funcAddr = " + funcAddr);
		}
		locDepth = popVal(); // locBaseIdx
		omsg("runRtnStmt: locDepth = " + locDepth);
		if (locDepth == NEGBASEVAL) {
			return STKUNDERFLOW;
		}
		rightp = popIdxVal(); // currZstmt/Zexpr
		omsg("runRtnStmt: returnp = " + rightp);
		if (rightp < 0) {
			return STKUNDERFLOW;
		}
		if (rightp == 0) {
			omsg("runRtnStmt: done");
			return EXIT; // done
		}
		omsg("runRtnStmt: top2");
		currLocBase = popVal(); // locBaseIdx
		if (currLocBase < 0) {
			return STKUNDERFLOW;
		}
		omsg("runRtnStmt: top3");
		varCount = popVal(); // varCount
		if (varCount < 0) {
			return STKUNDERFLOW;
		}
		omsg("runRtnStmt: varCount = " + varCount);
		isDelayPops = isNonImmedLocVar(funcReturns);
		if (!isDelayPops) {
			omsg("runRtnStmt: popMulti");
			rtnval = popMulti(varCount);
			if (rtnval < 0) {
				return rtnval;
			}
		}
		// push func rtnval if locDepth > 0:
		if (locDepth <= 0) { 
			omsg("runRtnStmt: locDepth = " + locDepth);
			isExprLoop = false;
		}
		else if (funcReturns == null) { 
			return GENERR;
		}
		else {  
			rtnval = pushFuncRtnVal(funcAddr, funcReturns, isDelayPops,
				varCount);
			if (rtnval < 0) {
				return rtnval;
			}
		}
		locDepth--;
		locBaseIdx = currLocBase;
		node = store.getNode(rightp);
		rightp = node.getRightp();
		omsg("runRtnStmt: rtnval = " + rightp);
		return rightp;
	}
	
	private int popMulti(int varCount) {
		KeywordTyp kwtyp = KeywordTyp.ZCALL;
		AddrNode node;
		PageTyp pgtyp;
		Page page;
		int idx;
		int addr;
		int i;
		int flagCount = 0;
		boolean flag;
		int rtnval;
		
		if (varCount < 0) {
			return 0;
		}
		for (i = 0; i < varCount; i++) {
			node = store.popNode();
			if (node == null) {
				return STKUNDERFLOW;
			}
			flag = false;
			addr = node.getAddr();
			page = store.getPage(addr);
			idx = store.getElemIdx(addr);
			pgtyp = node.getHdrPgTyp();
			omsg("popm: i = " + i + ", addr = " + addr + 
				", pgtyp = " + pgtyp);
			switch (pgtyp) {
			case FLOAT:
				flag = page.freeFloat(idx);
				//flag = store.freeFloat(addr);
				break;
			case STRING:
				flag = page.freeString(idx);
				//flag = store.freeString(addr);
				break;
			default:
				continue;
			}
			if (flag && (popMultiFreeCount >= 0)) {
				++popMultiFreeCount;
				++flagCount;
			}
			else {
				popMultiFreeCount = -1;
			}
		}
		omsg("popMulti: flagCount = " + flagCount);
		rtnval = popUntilKwd(kwtyp);
		return rtnval;
	}
	
	private int pushRtnStmt(Node node) {
		int rightp;
		KeywordTyp kwtyp;
		
		omsg("pushSetStmt: top");
		kwtyp = KeywordTyp.RETURN;
		if (!pushOp(kwtyp)) {
			return STKOVERFLOW;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			return RTNISEMPTY;  
		}
		node = store.getNode(rightp);
		rightp = handleExprToken(rightp, true);
		return rightp;
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
		val = (val < 0) ? -val : val;
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
	
	private int getIntOffStk(int stkidx) {
		AddrNode addrNode;
		int rtnval;
		
		addrNode = store.fetchNode(stkidx);
		rtnval = addrNode.getAddr();
		omsg("getIntOffStk: stkidx = " + stkidx + ", rtn = " + rtnval);
		return rtnval;
	}
	
	private int popIntStk() {
		AddrNode addrNode;
		int locVarTyp;
		int varidx;
		int rtnval;
		boolean ptrFlag;

		addrNode = store.popNode(); 
		if (addrNode == null){
			return STKUNDERFLOW;
		}
		rtnval = store.getStkIdx();
		ptrFlag = addrNode.isPtr();
		locVarTyp = addrNode.getHdrLocVarTyp();
		omsg("popIntStk: ptrFlag = " + ptrFlag);
		if (ptrFlag && addrNode.getHdrNonVar()) {
			return addrNode.getAddr();
		}
		switch (locVarTyp) {
		case NONVAR: 
			omsg("popIntStk: nonvar, rtn = " + rtnval);
			return rtnval;
		case LOCVAR:
		case GLBVAR:
			varidx = addrNode.getAddr();
			if (addrNode.getHdrLocVar()) {
				varidx += locBaseIdx;
			}
			omsg("popIntStk: varidx, rtn = " + rtnval);
			return varidx;
		default: return BADINTVAL;
		}
	}
	
	private boolean isNullKwd(AddrNode addrNode) {
		PageTyp pgtyp;
		int addr;
		boolean rtnval;
		
		addr = addrNode.getAddr();
		pgtyp = addrNode.getHdrPgTyp(); 
		rtnval = (pgtyp == PageTyp.KWD) && (addr == KeywordTyp.NULL.ordinal());
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
			omsg("popIntRtn: nonvar, addr = " + addr);
			rtnval = addr;
			break;
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
			break;
		default: 
			return BADPOP;
		}
		isNegInt = (rtnval < 0);
		rtnval = stripIntSign(rtnval);
		return rtnval;
	}
	
	private Integer popIObjFromNode(AddrNode addrNode) {
		// replaces popInt* functions
		PageTyp pgtyp;
		int locVarTyp;
		int addr, varidx;
		boolean flag;
		int rtnval;
		
		if (addrNode == null) {
			return setErrCode(STKUNDERFLOW);
		}
		addr = addrNode.getAddr();
		flag = addrNode.isPtr();  // debug
		omsg("popIObjFN: isPtr = " + flag);
		pgtyp = addrNode.getHdrPgTyp(); 
		if (pgtyp == PageTyp.KWD) { 
			return setErrCode(KWDPOPPED);
		}
		omsg("popIObjFN: pgtyp = " + pgtyp);
		locVarTyp = addrNode.getHdrLocVarTyp();
		switch (locVarTyp) {
		case NONVAR: 
			omsg("popIObjFN: nonvar, addr = " + addr);
			rtnval = addr;
			break;
		case LOCVAR:
		case GLBVAR:
			varidx = addr;
			if (addrNode.getHdrLocVar()) {
				varidx += locBaseIdx;
			}
			omsg("popIObjFN: varidx = " + varidx + 
				", locBaseIdx = " + locBaseIdx);
			addrNode = store.fetchNode(varidx);
			//pgtyp = addrNode.getHdrPgTyp(); 
			rtnval = addrNode.getAddr();
			break;
		default: 
			return setErrCode(BADPOP);
		}
		return rtnval;
	}
	
	private Integer setErrCode(int errCode) {
		lastErrCode = errCode;
		return null;
	}
	
	private String popStrFromNode(AddrNode addrNode) {
		PageTyp pgtyp;
		Page page;
		int idx;
		int locVarTyp;
		int addr, varidx;
		int rtnval;
		String s;
		String err = "";
		boolean isImmed = true;
		double dval;
		
		if (addrNode == null) {
			return err;
		}
		addr = addrNode.getAddr();
		pgtyp = addrNode.getHdrPgTyp(); 
		if (pgtyp == PageTyp.KWD) { 
			return err;
		}
		locVarTyp = addrNode.getHdrLocVarTyp();
		switch (locVarTyp) {
		case NONVAR: 
			omsg("popIntRtn: nonvar, addr = " + addr);
			isImmed = (pgtyp == PageTyp.INTVAL);
			rtnval = addr;
			break;
		case LOCVAR:
		case GLBVAR:
			varidx = addr;
			if (addrNode.getHdrLocVar()) {
				varidx += locBaseIdx;
			}
			addrNode = store.fetchNode(varidx);
			pgtyp = addrNode.getHdrPgTyp(); 
			//pgtyp = PageTyp.FLOAT;
			isImmed = (pgtyp == PageTyp.INTVAL);
			rtnval = addrNode.getAddr();
			break;
		default: 
			return err;
		}
		if (isImmed) {
			//oprn("popStrFromNode: rtnval = " + rtnval);
			s = "" + rtnval;
			return s;
		}
		page = store.getPage(rtnval);
		idx = store.getElemIdx(rtnval);
		switch (pgtyp) {
		case FLOAT:
			dval = page.getFloat(idx);
			omsg("popStrFromNode: dval = " + dval);
			s = "" + dval;
			break;
		case STRING:
			s = page.getString(idx);
			break;
		default:
			return err;
		}
		return s;
	}
	
	private int popVal() {
		AddrNode node;
		int val;
		
		node = store.popNode();
		if (node == null) {
			return NEGBASEVAL;
		}
		val = node.getAddr();
		return val;
	}
	
	private int storeInt(AddrNode node) {
		// assume set stmt. only handles integer vars/values
		AddrNode addrNode;
		int locVarTyp;
		int addr;
		int rtnval;
		int val;
		PageTyp pgtyp;
		boolean isGlb = true;
		
		val = node.getAddr();
		pgtyp = node.getHdrPgTyp();
		addrNode = store.popNode();
		if (addrNode == null) {
			return STKUNDERFLOW;
		}
		addr = addrNode.getAddr();
		/*
		pgtyp = addrNode.getHdrPgTyp(); 
		if (pgtyp != PageTyp.INTVAL) { 
			return BADSTORE;  
		}
		*/
		locVarTyp = addrNode.getHdrLocVarTyp();
		switch (locVarTyp) {
		case NONVAR: 
			return KWDPOPPED;
		case LOCVAR:
			isGlb = false;
		case GLBVAR:
			rtnval = storeLocGlbInt(addr, val, pgtyp, isGlb);
			if (rtnval < 0) {
				return rtnval;
			}
			break;
		default: return BADPOP;
		}
		return 0;
	}

	private int storeLocGlbInt(int varidx, int val, PageTyp pgtyp,
		boolean isGlb) 
	{
		omsg("storeLocGlbInt: varidx = " + varidx + ", val = " +
			val + ", pgtyp = " + pgtyp);
		if (!isGlb) {
			varidx += locBaseIdx;
		}
		/*
		addrNode = store.fetchNode(varidx);
		addr = addrNode.getAddr();
		omsg("storeLocGlbInt: varidx = " + varidx + ", addr = " + addr);
		pgtyp = addrNode.getHdrPgTyp(); 
		*/
		store.writeNode(varidx, val, pgtyp);
		return 0;
	}
	
	private boolean pushVal(int val, PageTyp pgtyp, int locVarTyp) {
		AddrNode addrNode;
		addrNode = store.newAddrNode(0, val);
		//addrNode = new AddrNode(0, val);
		addrNode.setHdrPgTyp(pgtyp);
		if (pgtyp == PageTyp.KWD) {
			omsg("pushVal: pushing KWD!!!");
		}
		addrNode.setHdrLocVarTyp(locVarTyp);
		addrNode.setPtr();
		if (!store.pushNode(addrNode)) {
			return false;
		}
		return true;
	}
	
	private boolean pushAddr(int rightp) {
		AddrNode addrNode;
		addrNode = store.newAddrNode(0, rightp);
		if (!store.pushNode(addrNode)) {
			return false;
		}
		return true;
	}
	
	private int pushFloat(double val) {
		AddrNode addrNode;
		int addr;
		double dval;
		Page page;
		int idx;
		
		addr = store.allocFloat(val);
		if (addr < 0) {
			return BADALLOC;
		}
		//## debug code:
		page = store.getPage(addr);
		idx = store.getElemIdx(addr);
		dval = page.getFloat(idx);
		omsg("pushFloat: dval = " + dval + ", addr = " + addr);

		addrNode = store.newAddrNode(0, addr);
		addrNode.setHdrPgTyp(PageTyp.FLOAT);
		if (!store.pushNode(addrNode)) {
			return STKOVERFLOW;
		}
		return 0;
	}
	
	private int pushString(String val) {
		AddrNode addrNode;
		int addr;
		
		addr = store.allocString(val);
		if (addr < 0) {
			return BADALLOC;
		}
		addrNode = store.newAddrNode(0, addr);
		addrNode.setHdrPgTyp(PageTyp.STRING);
		if (!store.pushNode(addrNode)) {
			return STKOVERFLOW;
		}
		return 0;
	}
	
	private boolean pushInt(int val) {
		boolean rtnval;
		rtnval = pushIntVar(val, NONVAR, true);
		return rtnval;
	}
	
	private boolean pushIntStk(int val) {
		boolean rtnval;
		rtnval = pushIntVar(val, NONVAR, false);
		return rtnval;
	}
	
	private boolean pushIntVar(int val, int locVarTyp, boolean ptrFlag) {
		AddrNode addrNode;
		addrNode = store.newAddrNode(0, val);
		addrNode.setHdrPgTyp(PageTyp.INTVAL);
		addrNode.setHdrLocVarTyp(locVarTyp);
		if (ptrFlag) {
			addrNode.setPtr();
		}
		if (!store.pushNode(addrNode)) {
			return false;
		}
		return true;
	}
	
	private int pushIntMulti(int val, int varCount) {
		int rtnval;
		boolean flag;
		
		omsg("pushIntMulti: popMulti, string");
		rtnval = popMulti(varCount);
		if (rtnval < 0) {
			return rtnval;
		}
		flag = pushIntVar(val, NONVAR, false);
		return flag ? 0 : STKOVERFLOW;
	}
	
	private int pushFuncRtnVal(int val, AddrNode srcNode,
		boolean isDelayPops, int varCount) 
	{
		PageTyp pgtyp;
		int locVarTyp;
		int rtnval;
		boolean flag;
		
		pgtyp = srcNode.getHdrPgTyp();
		omsg("pushFuncRtnVal: pgtyp = " + pgtyp);
		if (isImmedTyp(pgtyp)) {
			//rtnval = pushIntMulti(val, varCount);
			flag = pushIntStk(val);
			return flag ? 0 : STKOVERFLOW;
		}
		locVarTyp = srcNode.getHdrLocVarTyp();
		if (locVarTyp == NONVAR) {
			rtnval = pushNonImmed(val, pgtyp, -1);
			return rtnval;
		}
		if (locVarTyp == GLBVAR) {
			omsg("pushFuncRtnVal: GLBVAR, val = " + val);
			rtnval = pushNonImmed(val, pgtyp, -1);
			return rtnval;
		}
		if (locVarTyp != LOCVAR || !isDelayPops) {
			return GENERR;
		}
		rtnval = pushNonImmed(val, pgtyp, varCount);
		return rtnval;
	}
	
	private int pushNonImmed(int addr, PageTyp pgtyp, int varCount) {
		Page page;
		int idx;
		double dval;
		String sval;
		int rtnval;

		page = store.getPage(addr);
		idx = store.getElemIdx(addr);
		if (pgtyp == PageTyp.FLOAT) {
			dval = page.getFloat(idx);
			omsg("pushNonImmed: popMulti, float");
			rtnval = popMulti(varCount);
			if (rtnval < 0) {
				return rtnval;
			}
			rtnval = pushFloat(dval);
			return rtnval;
		}
		else if (pgtyp == PageTyp.STRING) {
			sval = page.getString(idx);
			omsg("pushNonImmed: popMulti, string");
			rtnval = popMulti(varCount);
			if (rtnval < 0) {
				return rtnval;
			}
			rtnval = pushString(sval);
			return rtnval;
		}
		else {
			return BADTYPE;
		}
	}
	
	private boolean isNonImmedLocVar(AddrNode addrNode) {
		PageTyp pgtyp;
		int locVarTyp;
		boolean rtnval;
		
		if (addrNode == null) {
			return false;
		}
		pgtyp = addrNode.getHdrPgTyp();
		locVarTyp = addrNode.getHdrLocVarTyp();
		rtnval = (!isImmedTyp(pgtyp)) && (locVarTyp == LOCVAR);
		return rtnval;
	}
	
	private boolean pushPtrVar(int val, int locVarTyp, PageTyp pgtyp) {
		AddrNode addrNode;
		addrNode = store.newAddrNode(0, val);
		addrNode.setHdrPgTyp(pgtyp);
		addrNode.setHdrLocVarTyp(locVarTyp);
		addrNode.setPtr();
		if (!store.pushNode(addrNode)) {
			return false;
		}
		return true;
	}
	
	private boolean isImmedTyp(PageTyp pgtyp) {
		return (pgtyp == PageTyp.INTVAL);
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
		
		addrNode = store.newAddrNode(0, val);
		addrNode.setHdrPgTyp(PageTyp.KWD);
		omsg("pushOpAsNode: --------------------------");
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
			stkidx = locBaseIdx + varidx;
		}
		else {
			varidx = -1 - varidx;
			locVarTyp = GLBVAR;
			stkidx = varidx;
		}
		addrNode = store.fetchNode(stkidx);
		pgtyp = addrNode.getHdrPgTyp();
		//oprn("pushVar: pgtyp = " + pgtyp);
		if (pgtyp == PageTyp.KWD) {
			omsg("pushVar: KWD stkidx = " + stkidx);
		}
		if (pushVal(varidx, pgtyp, locVarTyp)) {
			omsg("pushVar: varidx = " + varidx + ", pgtyp = " + 
				pgtyp + ", locvartyp = " + locVarTyp);
			return true;
		}
		else {
			return false;
		}
	}
	
	private boolean pushVarQuote(int varidx) {
		boolean isLocal;
		int stkidx;
		int locVarTyp;
		PageTyp pgtyp;
		AddrNode addrNode;
		boolean rtnval = false;
		
		omsg("pushVarQuote: top");
		isLocal = (varidx >= 0);
		if (isLocal) {
			locVarTyp = LOCVAR;
			stkidx = locBaseIdx + varidx;
		}
		else {
			varidx = -1 - varidx;
			locVarTyp = GLBVAR;
			stkidx = varidx;
		}
		addrNode = store.fetchNode(stkidx);
		pgtyp = addrNode.getHdrPgTyp();
		switch (pgtyp) {
		case INTVAL:
			omsg("pushVarQuote: call pushIntVar");
			rtnval = pushIntVar(varidx, locVarTyp, true);
			break;
		case FLOAT:
		case STRING:
			omsg("pushVarQuote: call pushPtrVar");
			rtnval = pushPtrVar(varidx, locVarTyp, pgtyp);
			break;
		default:
			omsg("pushVarQuote: default, pgtyp = " + pgtyp);
			return false;
		}
		omsg("pushVarQuote: rtnval = " + rtnval);
		return rtnval;
	}

	private int fetchInt(AddrNode node) {
		int varidx;
		varidx = node.getAddr();
		varidx += locBaseIdx;
		node = store.fetchNode(varidx);
		return node.getAddr();
	}
	
	private AddrNode fetchStkNode(int varidx) {
		int stkidx;
		AddrNode node;
		
		stkidx = locBaseIdx + varidx;
		node = store.fetchNode(stkidx);
		return node;
	}
	
	private String getGdefunWord() {
		return "gdefun";
	}
	
	private boolean isGdefun(String s) {
		return s.equals("gdefun");
	}
	
}
