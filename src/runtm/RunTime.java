package runtm;

import iconst.IConst;
import iconst.RunConst;
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

public class RunTime implements IConst, RunConst {

	private Store store;
	private ScanSrc scanSrc;
	private SynChk synChk;
	private RunScanner rscan;
	private RunCall rcall;
	private RunFlowCtrl rfc;
	private RunPushPop pp;
	private RunOperators runop;
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
	private boolean afterStmtKwd;
	private boolean isWhileUntil;
	private boolean isForContinue;
	private boolean isNakedKwd;
	private int lastErrCode;
	private int utKeyValIdx;
	private boolean isBadUtPair;
	private int runidx;
	private static final char SP = ' ';
	public HashMap<String, Integer> glbFunMap;
	public HashMap<String, Integer> glbLocVarMap;
	public ArrayList<Integer> glbFunList;
	public ArrayList<Integer> glbLocVarList;
	public ArrayList<String> glbFuncNames;
	public ArrayList<String> utKeyValList;

	public RunTime(Store store, ScanSrc scanSrc, SynChk synChk) {
		this.store = store;
		this.scanSrc = scanSrc;
		this.synChk = synChk;
		pp = new RunPushPop(store, this);
		runop = new RunOperators(store, this, pp);
		rcall = new RunCall(store, this, pp);
		rfc = new RunFlowCtrl(store, this, pp, runop);
		locBaseIdx = 0;
		varCountIdx = 0;
		stmtCount = 0;
		locDepth = 0;
		lastErrCode = 0;
		popMultiFreeCount = 0;
		utKeyValIdx = 0;
		isTgtExpr = false;
		isCalcExpr = false;
		isExprLoop = false;
		afterStmtKwd = false;
		isWhileUntil = false;
		isForContinue = false;
		isNakedKwd = true;
		glbFunMap = new HashMap<String, Integer>();
		glbLocVarMap = new HashMap<String, Integer>();
		glbFunList = new ArrayList<Integer>();
		glbLocVarList = new ArrayList<Integer>();
		glbFuncNames = new ArrayList<String>();
		utKeyValList = new ArrayList<String>();
	}
	
	public void setRscan(RunScanner rscan) {
		this.rscan = rscan;
		rcall.setRscan(rscan);
		rfc.setRscan(rscan);
	}

	public void out(String msg) {
		rscan.out(msg);
	}
	
	public void omsg(String msg) {  
		rscan.omsg(msg);
	}
	
	public void omsgz(String msg) {  
		rscan.omsgz(msg);
	}
	
	public void oprn(String msg) {  
		rscan.oprn(msg);
	}
	
	public void oerr(String msg) {  
		rscan.omsg(msg);
	}
	
	public int getLastErrCode() {
		return lastErrCode;
	}
	
	public boolean isBadUpFlag() {
		return isBadUtPair;
	}
	
	public int getLocBaseIdx() {
		return locBaseIdx;
	}
	
	public int getCurrZstmt() {
		return currZstmt;
	}
	
	public int getLocDepth() {
		return locDepth;
	}
	
	public void setLocDepth(int locd) {
		locDepth = locd;
	}
	
	public void setLocBaseIdx(int idx) {
		locBaseIdx = idx;
		pp.setLocBaseIdx(idx);
	}
	
	public void setVarCountIdx(int idx) {
		varCountIdx = idx;
	}
	
	public void setExprLoop(boolean flag) {
		isExprLoop = flag;
	}
	
	public boolean getAfterStmtKwd() {
		return afterStmtKwd;
	}
	
	public void setAfterStmtKwd(boolean flag) {
		afterStmtKwd = flag;
	}
	
	public void setForContinue(boolean flag) {
		isForContinue = flag;
	}
	
	public boolean getForContinue() {
		return isForContinue;
	}
	
	public boolean getWhileUntil() {
		return isWhileUntil;
	}
	
	public void setWhileUntil(boolean flag) {
		isWhileUntil = flag;
	}
	
	public void setNegInt(boolean flag) {
		isNegInt = flag;
	}
	
	public void setLastErrCode(int errCode) {
		lastErrCode = errCode;
	}
	
	public int getPopMultiFreeCount() {
		return popMultiFreeCount;
	}
	
	public void setPopMultiFreeCount(int count) {
		popMultiFreeCount = count;
	}
	
	public void incPopMultiFreeCount() {
		popMultiFreeCount++;
	}
	
	public boolean runTopBlock(int rightp, int runidx) {
		// process top-level stmts.:
		// do ( stmt-1; .. stmt-n; )
		Node node;
		int downp;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int phaseNo = 0;
		boolean rtnval;

		this.runidx = runidx;
		isBadUtPair = false;
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
		//rtnval = !isBadUtPair;
		rtnval = true;
		return rtnval;
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
				case 2: // only called once
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
		KeywordTyp kwtyp;
		int savep = rightp;

		omsg("Keyword gdefun detected again.");
		rightp = rscan.scopeGlbVarLists(rightp);
		if (rightp <= 0) {
			omsg("runGlbDefStmt: bad var list");
			return -1;
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.DO) {
			omsg("Missing DO");
			return -1;
		}
		if (rcall.runGlbCall() < 0) {
			handleErrToken(STKOVERFLOW);
			return -1;
		}
		if (!pushInt(currZstmt)) {
			return STKOVERFLOW;
		}
		if (!pushInt(locDepth)) {
			return STKOVERFLOW;
		}
		// currently only called once, in body of gdefun stmt.
		rightp = handleDoBlock(node);
		omsg("Stmt count = " + stmtCount);
		if (rightp < EXIT) {
			doRunTimeError(rightp);
			return -1;
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
	
	public String convertErrToken(int rightp) {
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
		case BADINCDECSTMT: return "Malformed INC/DEC statement";
		case BADPARMCT: return "Mismatched parameter count";
		case RTNISEMPTY: return "Return stmt. lacks value";
		case BADUTSTMT: return "Malformed unit test stmt.";
		case BADTYPE: return "Unexpected data type";
		case BADFREE: return "Memory free failure";
		case BADOPTYP: return "Invalid operand type";
		case BADDOSTMT: return "Unexpected DO encountered";
		case NOVARINZ: return "Variable not initialized";
		case BADGVAR: return "Invalid attempt to modify global var.";
		case BADFORSTMT: return "Malformed for stmt";
		case BADBRKSTMT: return "Error in break or continue stmt";
		case FNCALLNORTNVAL : return "Function call has no return value";
		case GENERR: return "General runtime error";
		default: return "Error code = " + (-rightp);
		}
	}
	
	private int handleDoBlock(Node node) {	
		// currently only called once, in body of gdefun stmt.
		AddrNode zeroNode;
		KeywordTyp kwtyp;
		int rightp;

		// return 0 if done (anywhere in this fn)
		// Note that a rightp value of zero means points to nothing,
		//   everywhere in the xyn project
		rightp = node.getDownp();
		if (rightp <= 0) {
			return 0;
		}
		pushOp(KeywordTyp.NULL);
		zeroNode = new AddrNode(0, 0);
		store.pushNode(zeroNode);
		while (rightp > 0) {
			// handle stmt.
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
			rightp = pushStmt(node, rightp);  // scan stmt., push it
			do {
				isExprLoop = false;
				// handle mult. exprs.
				rightp = handleExprToken(rightp, false);
				if (rightp >= 0) {
					return BADOP;
				}
				if (rightp == EXIT) {
					return 0;
				}
				if (rightp > NEGBASEVAL) {  // error
					return rightp;
				}
				// run stmt. using kwd. (encoded in -ve rightp)
				rightp = -(rightp - NEGBASEVAL);
				kwtyp = KeywordTyp.values[rightp];
				omsg("handleDoBlock: btm2, kwtyp = " + kwtyp);
				rightp = handleStmtKwd(kwtyp);
				omsg("handleDoBlock: btm2, rightp = " + rightp);
			} while (isExprLoop);  // keep going if return stmt...
			// unless depth counter is zero, ends up here:
			while (rightp == 0) {
				rightp = handleBtmZeroAddr(node);
				if (rightp == EXIT) {
					return 0;
				}
			}
		} 
		return rightp;  // always -ve
	}
	
	private int handleBtmZeroAddr(Node node) {
		KeywordTyp kwtyp;
		int rightp = 0;
		
		kwtyp = topKwd();
		omsg("handleBtmZeroAddr: top, kwtyp = " + kwtyp);
		if (rfc.isBranchKwd(kwtyp)) {
			popKwd();
			rightp = popVal();
			popVal();
			node = store.getNode(rightp);
			rightp = node.getRightp();
		}
		else if (kwtyp == KeywordTyp.SWITCH) {
			popKwd();
			popVal();
			rightp = popVal();
			popVal();
			node = store.getNode(rightp);
			rightp = node.getRightp();
		}
		else if (kwtyp == KeywordTyp.WHILE) {
			omsg("(4) handleBtmZeroAddr: WHILE");
			popKwd();
			rightp = popVal();
			popVal();
			popVal();
		}
		else if (kwtyp == KeywordTyp.UNTIL) {
			omsg("handleBtmZeroAddr: UNTIL");
			rightp = rfc.doBtmUntilLoop();
		}
		else if (kwtyp == KeywordTyp.FOR) {
			omsg("handleBtmZeroAddr: FOR");
			rightp = popVal(); // zstmt of for
			if (!pushAddr(rightp)) {
				return STKOVERFLOW;
			}
			omsg("handleBtmZeroAddr: FOR, ZF = " + rightp);
			node = store.getNode(rightp);
			rightp = node.getDownp();
			node = store.getNode(rightp);
			rightp = node.getRightp();
			node = store.getNode(rightp);
			rightp = node.getRightp();
			node = store.getNode(rightp);
			rightp = node.getDownp();
		}
		else if (kwtyp == KeywordTyp.ZQUEST) {
			// end of for loop header reached
			// loop control flag on stack
			rightp = rfc.runForStmt();
		}
		else {
			rightp = rcall.runRtnStmt(false);
		}
		omsg("handleBtmZeroAddr: btm, rightp = " + rightp);
		return rightp;
	}

	public int handleExprToken(int rightp, boolean isSingle) {	
		// handle single/mult. expr(s).
		KeywordTyp kwtyp;
		KeywordTyp kwtop;
		Node node;
		int oldLocDepth;
		String numstr = "";
		boolean isShortCircSkip = false;
		boolean found = false;
		
		// currently isSingle on single expr. end of push set/return
		omsg("exprtok: top, rightp = " + rightp);
		while (rightp >= 0) {
			while ((rightp <= 0) && !found) {  
				// rightp = 0, not found yet
				if (store.isOpStkEmpty()) {
					omsg("exprtok: top of while, empty op stk");
					return STKUNDERFLOW;
				}
				found = isSingle && (locDepth <= 0);
				kwtyp = popKwd();
				if (kwtyp == KeywordTyp.NULL && store.isOpStkEmpty()) {
					return EXIT;
				}
				numstr = getWhileDoKwd(kwtyp);
				omsg(numstr + "exprtok: kwtyp popped = " + kwtyp);
				if (kwtyp == KeywordTyp.ZCALL && locDepth > 0) {  
					omsg("exprtok: function call, locDepth = " + locDepth);
				}
				rightp = handleExprKwd(kwtyp);
				omsg("exprtok: kwtyp = " + kwtyp + ", rightp = " + rightp);
				if (rightp < 0) {
					return rightp;  // err if > NEGBASEVAL,
					// else stmt. kwd. encoded in -ve rightp
				}
				locDepth--;
			}
			// rightp > 0 or found
			if (found) {  // isSingle and depth at bottom
				break;
			}
			// start here upon rightp > 0
			omsg("exprtok: node -> rightp = " + rightp);
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			omsg("exprtok: btm kwtyp = " + kwtyp + ", rightp = " + rightp);
			if (kwtyp == KeywordTyp.ZSTMT) {
				return STMTINEXPR;
			}
			oldLocDepth = locDepth;
			//omsg("exprtok: locDepth = " + locDepth);
			// handling expr.
			kwtop = topKwd();
			numstr = getWhileDoKwd(kwtop);
			omsg(numstr + "exprtok: kwtop = " + kwtop);
			if (rfc.isLogicalKwd(kwtop)) {  
				rightp = rfc.handleLogicalKwd(kwtop, rightp);
				omsg("exprtok: hlogkw-> rightp = " + rightp);
				if (rightp > 0) {
					node = store.getNode(rightp); 
				}
			}
			if (rightp > 0) {
				rightp = pushExprOrLeaf(node, rightp);
				found = isSingle && (locDepth <= 0);
			}
			//omsg("exprtok: locDepth (2) = " + locDepth);
			if ((rightp == 0) && rfc.isLogicalKwd(kwtop) &&
				!rfc.isQuestKwd(kwtop) && !isShortCircSkip &&
				(locDepth <= oldLocDepth))
			{  
				rightp = rfc.handleLogicalKwd(kwtop, rightp);
				omsg("exprtok: (2) hlogkw-> rightp = " + rightp);
			}
			if (rightp == EXIT) {
				rightp = 0;
				isShortCircSkip = true;
			}
		} 
		return rightp;
	}
	
	private int pushExprOrLeaf(Node node, int rightp) {
		KeywordTyp kwtyp;

		isNakedKwd = false;
		kwtyp = node.getKeywordTyp();
		omsg("pushExprOrLeaf: kwtyp = " + kwtyp);
		if (kwtyp == KeywordTyp.ZPAREN) {
			locDepth++;
			currZexpr = rightp;
			rightp = pushExpr(node);
		}
		else if (kwtyp == KeywordTyp.DO) {
			rightp = rfc.handleDoToken(node, rightp);
		}
		else if (kwtyp == KeywordTyp.CASE) {
			rightp = rfc.handleCaseKwd(node, rightp);
		}
		else if (rfc.isKwdSkipped(kwtyp)) {
			rightp = rfc.handleSkipKwd(node, rightp);
		}
		else {
			rightp = handleLeafToken(node);
		}
		return rightp;
	}

	private String getWhileDoKwd(KeywordTyp kwtyp) {
		String numstr;
		numstr = "";
		if ((kwtyp == KeywordTyp.FOR) || (kwtyp == KeywordTyp.DO)) {
			numstr = "(1) ";
		}
		return numstr;
	}
	
	public int handleLeafToken(Node node) {
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
		long longval;
		double dval;
		String sval;

		afterStmtKwd = false;
		varidx = node.getDownp();
		celltyp = node.getDownCellTyp();
		rightp = node.getRightp();
		omsgz("htok: celltyp = " + celltyp + ", downp = " + varidx);
		omsg(", rightp = " + rightp);
		if (isQuote) {
			switch (celltyp) {
			case ID:
			case LOCVAR:
				if (!pushVarQuote(varidx)) {
					return STKOVERFLOW;
				}
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
		case BOOLEAN:
			ival = varidx;
			omsg("htok: push BOOL = " + ival);
			if (!pushBoolStk(ival)) {
				return STKOVERFLOW;
			}
			break;
		case LONG:	
			downp = node.getDownp();
			page = store.getPage(downp);
			idx = store.getElemIdx(downp);
			longval = page.getLong(idx);
			rtnval = pushLong(longval);
			omsg("htok: long = " + longval);
			if (rtnval < 0) {
				return rtnval;
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
		
		rightp = runop.handleExprKwdRtn(kwtyp);
		if (rightp < 0) {
			return rightp;
		}
		if (rfc.isNoSwapKwd(kwtyp)) {
			return rightp;
		}
		if (!store.swapNodes()) {
			return STKUNDERFLOW;
		}
		rightp = popVal();
		omsg("handleExprKwd: rightp = " + rightp);
		return rightp;
	}
	
	private int handleStmtKwd(KeywordTyp kwtyp) {
		int rightp;
		int temp;
		AddrNode addrNode;
		Node node;
		
		rightp = handleStmtKwdRtn(kwtyp);
		if ((rightp < 0) || rfc.isJumpKwd(kwtyp)) {
			return rightp;
		}
		addrNode = store.popNode();
		store.popNode();
		rightp = addrNode.getAddr();
		temp = rightp;
		node = store.getNode(rightp);
		rightp = node.getRightp();
		return rightp;
	}
	
	private int handleStmtKwdRtn(KeywordTyp kwtyp) {
		afterStmtKwd = true;
		switch (kwtyp) {
		case ADDSET:
		case MINUSSET: 
		case MPYSET: 
		case DIVSET: 
		case ANDBSET:
		case ORBSET:
		case XORBSET:
		case IDIVSET:
		case MODSET:
		case SHLSET:
		case SHRSET:
		case SHRUSET:
		case SET: 
			return runop.runSetStmt(kwtyp);
		case INCINT:
		case DECINT:
			return runop.runIncDecStmt(kwtyp);
		case PRINTLN: return runPrintlnStmt(kwtyp);
		case ZCALL: return rcall.runZcallStmt();
		case RETURN: return rcall.runRtnStmt(!isNakedKwd);
		case BREAK:
			return rfc.runBrkStmt();
		case CONTINUE:
			return rfc.runContinueStmt(); 
		case UTPUSH: return runUtPushStmt();
		case UTSCAN: return runUtScanStmt();
		case QUEST: return rfc.runBoolStmt();
		case DO: return rfc.runDoStmt();
		case IF: 
		case ELIF: 
		case ELSE: 
		case CASE:
		case WHILE:
		case FOR:
		case ZQUEST:
		case TUPLE:
			return 0;
		case SWITCH:
			return rfc.runSwitchStmt(); // don't need, just return 0?
		case UNTIL:
			oprn("Keyword: UNTIL detected.");
			return BADOP;
		default:
			oprn("handleStmtKwdRtn: kwtyp = " + kwtyp);
			return BADOP;
		}
	}
	
	private void doRunTimeError(int errCode) {
		int rightp;
		Node node;
		int lineno = 0;
		int varidx;
		int downp;
		String msg;
		String fileName;
		String funcName;
		boolean foundZeroLines = false;
		
		omsg("doRunTimeError: errCode = " + errCode);
		rightp = popUntilZstmt();
		if (rightp < 0) {
			errCode = rightp;
		}
		else if (rightp > 0) {
			lineno = store.lookupLineNo(rightp);
		}
		else {  
			errCode = NEGADDR;
		}
		if (lineno == 0) {
			oprn("Line number of error: unknown");
			foundZeroLines = true;
		}
		else {
			oprn("Error on line number: " + lineno);
		}
		handleErrToken(errCode);
		// output rest of stack trace:
		//   call popUntilBase in loop
		fileName = scanSrc.getSrcFileName();
		while (true) {
			rightp = popUntilBase();
			if (rightp <= 0) {
				break;
			}
			// find name of func:
			node = store.getNode(rightp);
			downp = node.getDownp();
			node = store.getNode(downp);
			varidx = node.getDownp();
			funcName = glbFuncNames.get(varidx);
			msg = getStkTrcLine(funcName, fileName, lineno);
			oprn(msg);
			lineno = store.lookupLineNo(rightp);
			if (lineno == 0) {
				foundZeroLines = true;
			}
		}
		if (rightp != STKUNDERFLOW) {
			handleErrToken(rightp);
			return;
		}
		funcName = rscan.getGdefunKwd();
		msg = getStkTrcLine(funcName, fileName, lineno);
		oprn(msg);
		if (foundZeroLines) {
			oprn("Note: If line number is zero, it could not " +
				"be determined.");
		}
	}
	
	private String getStkTrcLine(String funcName, String fileName,
		int lineno) 
	{
		String rtnval;
		rtnval = "at " + funcName + "(" + fileName +
			":" + lineno + ")";
		return rtnval;
	}
	
	private int pushStmt(Node node, int savep) {
		// scan stmt.
		KeywordTyp kwtyp;
		int rightp, rightq;
		
		omsg(":: pushStmt: stkidx = " + store.getStkIdx());
		//rightq = node.getRightp();
		rightq = savep;
		rightp = node.getDownp();
		if (rightp <= 0) {
			return NEGADDR;
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		//if (isJumpKwd(kwtyp)) { }
		if (kwtyp == KeywordTyp.DO) { }
		else if (kwtyp == KeywordTyp.ZCALL) { 
			omsg("pushStmt: kwd = zcall");
		}
		//else if (kwtyp == KeywordTyp.RETURN) { }
		else if (!pushOpAsNode(KeywordTyp.ZSTMT) || !pushAddr(rightq)) 
		{
			return STKOVERFLOW;
		}
		isNakedKwd = true;
		switch (kwtyp) {
		case ADDSET:
		case MINUSSET: 
		case MPYSET: 
		case DIVSET: 
		case ANDBSET: 
		case ORBSET: 
		case XORBSET:
		case IDIVSET:
		case MODSET:
		case SHLSET:
		case SHRSET:
		case SHRUSET:
		case SET: 
			rightp = pushSetStmt(node, kwtyp);
			break;
		case INCINT:
		case DECINT:
			rightp = pushIncDecStmt(node, kwtyp);
			break;
		case ZQUEST: 
			//rightp = pushBoolForStmt(node);
			//break;
		case QUEST: 
			rightp = pushBoolStmt(node);
			break;
		case PRINTLN: 
			rightp = pushPrintlnStmt(node);
			break;
		case ZCALL:
			rightp = rcall.pushZcallStmt(node);  // prepare to scan call
			break;
		case RETURN:
			rightp = rcall.pushRtnStmt(node);
			break;
		case BREAK:
			rightp = rfc.pushBrkStmt(node);
			break;
		case CONTINUE:
			rightp = rfc.pushContinueStmt(node);
			break;
		case UTPUSH:
		case UTSCAN:
			rightp = pushUtPushStmt(node, kwtyp);
			break;
		case IF:
			rightp = rfc.pushIfStmt(node);
			break;
		case WHILE:
			rightp = rfc.pushWhileStmt(node, savep);
			break;
		case FOR:
			rightp = rfc.pushForStmt(node, savep);
			break;
		case SWITCH:
			rightp = rfc.pushSwitchStmt(node);
			break;
		case TUPLE:
			rightp = pushTupleStmt(node);
			break;
		default: return BADSTMT;
		}
		isForContinue = false;
		return rightp;
	}
	
	private int pushExpr(Node node) {
		KeywordTyp kwtyp;
		KeywordTyp nullkwd;
		int ival;
		int rightp;
		
		afterStmtKwd = false;
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
		case XOR:
		case ANDBITZ:
		case ORBITZ:
		case XORBITZ:
			nullkwd = KeywordTyp.NULL; 
			if (!pushOp(kwtyp) || !pushOpAsNode(nullkwd)) {
				return STKOVERFLOW;
			}
			break;
		case MINUS:
		case DIV:
		case IDIV:
		case MOD:
		case SHL:
		case SHR:
		case SHRU:
		case EQ:
		case NE:
		case LT:
		case LE:
		case GE:
		case GT:
		case NOT:
		case NOTBITZ:
		case CQUEST:
		case SWIX:
			if (!pushOp(kwtyp)) {
				return STKOVERFLOW;
			}
			break;
		case AND:
		case OR:
		case QUEST:
			//nullkwd = KeywordTyp.NULL; 
			//if (!pushOp(kwtyp) || !pushOpAsNode(nullkwd)) {
			//ival = (kwtyp == KeywordTyp.AND) ? 1 : 0;
			if (!pushOp(kwtyp) || !pushKwdVal(-1)) {  
				return STKOVERFLOW;
			}
			break;
		case CASE:
			rightp = topIntVal();
			omsg("pushExpr: CASE top = " + rightp);
			//if (!pushOp(kwtyp)) {  
			if (!pushOp(kwtyp) || !pushKwdVal(-1)) {  
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
		default: 
			return BADOP;
		}
		rightp = node.getRightp();
		return rightp;
	}
	
	private int pushSetStmt(Node node, KeywordTyp kwtyp) {
		int rightp;
		
		omsg("pushSetStmt: top");
		//kwtyp = KeywordTyp.SET;
		if (!pushOp(kwtyp)) {
			return STKOVERFLOW;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {  // naked set kwd.
			return BADSETSTMT;
		}
		node = store.getNode(rightp);
		rightp = handleLeafTokenQuote(node);  // handle target expr.
		if (rightp <= 0) {
			return BADSETSTMT;
		}
		rightp = handleExprToken(rightp, true);  // handle expr.
		return rightp;
	}
	
	private int pushIncDecStmt(Node node, KeywordTyp kwtyp) {
		int rightp;
		
		omsg("pushIncDecStmt: top");
		if (!pushOp(kwtyp)) {
			return STKOVERFLOW;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {  // naked inc/dec kwd.
			return BADINCDECSTMT;
		}
		node = store.getNode(rightp);
		rightp = handleLeafTokenQuote(node);  // handle target expr.
		if (rightp != 0) {
			return BADINCDECSTMT;
		}
		return 0;
	}
	
	private int pushBoolStmt(Node node) {
		int rightp;
		KeywordTyp kwtyp = KeywordTyp.ZQUEST;
		
		omsg("pushBoolStmt: top");
		rightp = popVal(); 
		popVal(); // ZSTMT
		if (!pushOp(kwtyp) || !pushAddr(rightp)) {
			return STKOVERFLOW;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {  // naked quest kwd.
			return BADFORSTMT;
		}
		rightp = handleExprToken(rightp, true);  // handle expr.
		return rightp;
	}
	
	private int pushBoolForStmt(Node node) {
		int rightp;
		
		omsg("pushBoolForStmt: top");
		if (!pushOp(KeywordTyp.ZQUEST)) {
			return STKOVERFLOW;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {  // naked zquest kwd.
			return BADFORSTMT;
		}
		rightp = handleExprToken(rightp, true);  // handle expr.
		return rightp;
	}
	
	private int pushTupleStmt(Node node) {
		int rightp;
		KeywordTyp kwtyp = KeywordTyp.TUPLE;
		
		omsg("pushTupleStmt: top");
		if (!pushOp(kwtyp)) {
			return STKOVERFLOW;
		}
		rightp = node.getRightp();
		if (rightp > 0) {  // naked tuple kwd expected
			return GENERR;
		}
		rightp = 0;
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
		int count;
		int rtnval;
		String msg = "";
		String s;

		count = rcall.getCountOfPrintSpares(kwtyp);
		omsg("runPrintlnStmt: count of spares = " + count);
		count = 0;
		while (true) {
			addrNode = store.fetchSpare();
			if (addrNode == null) {
				break;
			}
			s = popStrFromNode(addrNode);
			if (s.length() > 0) {
				msg = msg + s + SP;
				//msg = msg + s + "_";
			}
			else if (lastErrCode != 0) {
				return lastErrCode;
			}
			omsg("runPrintlnStmt: msg = " + msg);
			count++;
		}
		if (count > 0) {
			oprn(msg);
		}
		rtnval = popUntilKwd(kwtyp);
		return rtnval;
	}
	
	private int runUtPushStmt() {
		AddrNode addrNode;
		String skey;
		String sval;
		int addr;
		KeywordTyp kwtyp = KeywordTyp.UTPUSH; 
		Page page;
		int idx;
		int len;
		int rtnval;

		omsg("runUtPushStmt: top");
		addrNode = store.popNode();
		if (addrNode == null) {
			return STKUNDERFLOW;
		}
		sval = popStrFromNode(addrNode);
		omsg("runUtPushStmt: sval = " + sval);
		len = sval.length();
		sval = sval.trim();
		if (sval.equals("") && (len > 0)) {
			sval = " ";
		}
		addrNode = store.popNode();
		if (addrNode == null) {
			return STKUNDERFLOW;
		}
		addr = addrNode.getAddr();
		page = store.getPage(addr);
		idx = store.getElemIdx(addr);
		skey = page.getString(idx);
		skey = skey.trim();
		utKeyValList.add(skey);
		utKeyValList.add(sval);
		omsg("runUtPushStmt: skey = " + skey + ", sval = " + sval);
		//oprn(ditto...); 
		rtnval = popUntilKwd(kwtyp);
		omsg("runUtPushStmt: btm, rtnval = " + rtnval);
		return rtnval;
	}
	
	private int runUtScanStmt() {
		AddrNode addrNode;
		String skey;
		String sval;
		String lstkey;
		String lstval;
		int addr;
		KeywordTyp kwtyp = KeywordTyp.UTSCAN; 
		Page page;
		int idx;
		int len;
		int rtnval;
		boolean isBadPair;
		boolean isBadUtKey;
		boolean isBadUtVal;
		int pairIdx;

		addrNode = store.popNode();
		if (addrNode == null) {
			return STKUNDERFLOW;
		}
		addr = addrNode.getAddr();
		page = store.getPage(addr);
		idx = store.getElemIdx(addr);
		sval = page.getString(idx);
		len = sval.length();
		sval = sval.trim();
		if (sval.equals("") && (len > 0)) {
			sval = " ";
		}
		addrNode = store.popNode();
		if (addrNode == null) {
			return STKUNDERFLOW;
		}
		addr = addrNode.getAddr();
		page = store.getPage(addr);
		idx = store.getElemIdx(addr);
		skey = page.getString(idx);
		skey = skey.trim();
		pairIdx = (utKeyValIdx / 2) + 1;
		lstkey = utKeyValList.get(utKeyValIdx);
		isBadUtKey = !skey.equals(lstkey);
		utKeyValIdx++;
		lstval = utKeyValList.get(utKeyValIdx);
		isBadUtVal = !sval.equals(lstval);
		utKeyValIdx++;
		isBadPair = (isBadUtKey || isBadUtVal);
		if (isBadPair) {
			oprn("UT Error: run index = " + runidx);
			oprn("UT Error: pair index = " + pairIdx);
			//oprn("UT Error: pair utKeyValIdx = " + utKeyValIdx);
		}
		if (isBadUtKey) {
			oprn("UT Error: bad key = " + lstkey);
			oprn("UT Error: correct key = " + skey);
			if (!isBadUtVal) {
				oprn("UT Error: value = " + sval);
			}
		}
		if (isBadUtVal) {
			if (!isBadUtKey) {
				oprn("UT Error: key = " + skey);
				//oprn("UT Error: lstkey = " + lstkey);
			}
			oprn("UT Error: bad value = " + lstval);
			oprn("UT Error: correct value = " + sval);
		}
		isBadUtPair = isBadUtPair || isBadPair;
		rtnval = popUntilKwd(kwtyp);
		return rtnval;
	}
	
	private int pushUtPushStmt(Node node, KeywordTyp kwtyp) {
		int rightp;
		
		omsg("pushUtPushStmt: top, kwtyp = " + kwtyp);
		if (!pushOp(kwtyp) || !pushOpAsNode(kwtyp)) {
			return STKOVERFLOW;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			return BADUTSTMT;  
		}
		node = store.getNode(rightp);
		rightp = handleLeafToken(node);  // handle 1st expr.
		if (rightp <= 0) {
			return BADUTSTMT;
		}
		rightp = handleExprToken(rightp, true);  // handle 2nd expr.
		omsg("pushUtPushStmt: btm, rightp = " + rightp);
		return rightp;
	}
	
	public AddrNode getVarNode(AddrNode node) {
		return pp.getVarNode(node);
	}

	public int popUntilZstmt() {
		return pp.popUntilZstmt();
	}
	
	public int popUntilBase() {
		return pp.popUntilBase();
	}
	
	private int popUntilKwd(KeywordTyp kwtyp) {
		return pp.popUntilKwd(kwtyp);
	}
	
	public int popMulti(int varCount) {
		return pp.popMulti(varCount);
	}
	
	private KeywordTyp popKwd() {
		return pp.popKwd();
	}
	
	private KeywordTyp topKwd() {
		return pp.topKwd();
	}
	
	private KeywordTyp pickKwd(int idx) {
		return pp.pickKwd(idx);
	}
	
	public int stripIntSign(int val) {
		return pp.stripIntSign(val);
	}
	
	public int packIntSign(boolean isNeg, int val) {
		return pp.packIntSign(isNeg, val);
	}
	
	private int popIdxVal() {
		return pp.popIdxVal();
	}
	
	private int popAbsVal() {
		return pp.popAbsVal();
	}
	
	private int getIntOffStk(int stkidx) {
		return pp.getIntOffStk(stkidx);
	}
	
	private int topIntVal() {
		return pp.topIntVal();
	}
	
	private int popIntStk() {
		return pp.popIntStk();
	}

	private boolean isNullKwd(AddrNode addrNode) {
		return pp.isNullKwd(addrNode);
	}
	
	private int popInt(boolean isKwd) {
		return pp.popInt(isKwd);
	}
	
	private int popIntFromNode(AddrNode addrNode) {
		return pp.popIntFromNode(addrNode);
	}
	
	private int popIntRtn(AddrNode addrNode, boolean isKwd) {
		return pp.popIntRtn(addrNode, isKwd);
	}
	
	private Integer nodeToIntVal(AddrNode addrNode, int lbidx) {
		return pp.nodeToIntVal(addrNode, lbidx);
	}
	
	private Integer setErrCode(int errCode) {
		lastErrCode = errCode;
		return null;
	}
	
	private String popStrFromNode(AddrNode addrNode) {
		return pp.popStrFromNode(addrNode);
	}
	
	private int popVal() {
		return pp.popVal();
	}

	public boolean popSafeVal() {
		return pp.popSafeVal();
	}
	
	private int storeLocGlbInt(int varidx, int val, PageTyp pgtyp,
		boolean isGlb) 
	{
		return pp.storeLocGlbInt(varidx, val, pgtyp, isGlb);
	}
	
	private boolean pushVal(int val, PageTyp pgtyp, int locVarTyp) {
		return pp.pushVal(val, pgtyp, locVarTyp);
	}
	
	private boolean pushKwdVal(int ival) {
		return pp.pushKwdVal(ival);
	}
	
	private boolean pushAddr(int rightp) {
		return pp.pushAddr(rightp);
	}
	
	private int pushLong(long val) {
		return pp.pushLong(val);
	}
	
	private int pushFloat(double val) {
		return pp.pushFloat(val);
	}
	
	private int pushString(String val) {
		return pp.pushString(val);
	}
	
	private boolean pushInt(int val) {
		return pp.pushInt(val);
	}
	
	private boolean pushIntStk(int val) {
		return pp.pushIntStk(val);
	}
	
	private boolean pushIntVar(int val, int locVarTyp, boolean ptrFlag) {
		return pp.pushIntVar(val, locVarTyp, ptrFlag);
	}
	
	private boolean pushBoolStk(int val) {
		return pp.pushBoolStk(val);
	}
	
	private int pushIntMulti(int val, int varCount) {
		return pp.pushIntMulti(val, varCount);
	}
	
	private int pushFuncRtnVal(int val, AddrNode srcNode,
		boolean isDelayPops, int varCount) 
	{
		return pp.pushFuncRtnVal(val, srcNode, isDelayPops, varCount);
	}
	
	private int pushNonImmed(int addr, PageTyp pgtyp, int varCount) {
		return pp.pushNonImmed(addr, pgtyp, varCount);
	}
	
	private boolean isNonImmedLocVar(AddrNode addrNode) {
		return pp.isNonImmedLocVar(addrNode);
	}
	
	private boolean pushPtrVar(int val, int locVarTyp, PageTyp pgtyp) {
		return pp.pushPtrVar(val, locVarTyp, pgtyp);
	}
	
	private boolean isImmedTyp(PageTyp pgtyp) {
		return (pgtyp == PageTyp.INTVAL);
	}
	
	private boolean pushOp(KeywordTyp kwtyp) {
		return pp.pushOp(kwtyp);
	}
	
	private boolean pushOpAsNode(KeywordTyp kwtyp) {
		return pp.pushOpAsNode(kwtyp);
	}
	
	private boolean pushVar(int varidx) {
		return pp.pushVar(varidx);
	}
	
	private boolean pushVarQuote(int varidx) {
		return pp.pushVarQuote(varidx);
	}
	
	private boolean freeTarget(AddrNode node, 
		boolean isChkTgt, int addr) 
	{
		return pp.freeTarget(node, isChkTgt, addr);
	}
}
