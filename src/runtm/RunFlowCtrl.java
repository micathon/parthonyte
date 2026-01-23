package runtm;

import iconst.IConst;
import iconst.KeywordTyp;
import iconst.PageTyp;
import iconst.RunConst;
import page.AddrNode;
import page.Node;
import page.Page;
import page.Store;

public class RunFlowCtrl implements IConst, RunConst {

	private Store store;
	private RunTime rt;
	private RunOperators runop;
	private RunScanner rscan;
	private RunPushPop pp;
	
	public RunFlowCtrl(Store store, RunTime rt, RunPushPop pp,
		RunOperators runop) 
	{
		this.store = store;
		this.rt = rt;
		this.pp = pp;
		this.runop = runop;
	}
	
	public void setRscan(RunScanner rscan) {
		this.rscan = rscan;
	}

	private void omsg(String msg) {
		rt.omsg(msg);
	}
	
	private void omsgz(String msg) {
		rt.omsgz(msg);
	}
	
	private void oprn(String msg) {
		System.out.println(msg);
	}

	public int handleLogicalKwd(KeywordTyp kwtop, int rightp) {
		Node node;
		AddrNode addrNode;
		PageTyp pgtyp;
		int ival, jval, kval;
		KeywordTyp kwtyp;
		boolean isShortCircuit;
		
		isShortCircuit = false;
		omsg("hlogkw: top, rightp = " + rightp);
		addrNode = store.topNode();
		pgtyp = addrNode.getHdrPgTyp();
		ival = addrNode.getAddr();
		omsg("hlogkw: pgtyp = " + pgtyp + ", addr = " + ival);
		if (pgtyp == PageTyp.KWD) {
			ival = pp.topIntVal();  // = -1
			jval = 0;
			omsg("hlogkw: KWD ival = " + ival);
		}
		else if (kwtop == KeywordTyp.QUEST) { 
			rightp = logicalQuestKwd(rightp);
			return rightp;
		}
		else if (kwtop == KeywordTyp.CASE) { 
			rightp = logicalCaseKwd(rightp);
			return rightp;
		}
		else {
			addrNode = store.popNode();
			if (addrNode == null) {
				return STKUNDERFLOW;
			}
			pgtyp = addrNode.getHdrPgTyp();
			omsg("hlogkw: pgtyp = " + pgtyp);
			if (pgtyp != PageTyp.BOOLEAN) {
				return BADOPTYP; 
			}
			jval = pp.nodeToIntVal(addrNode, rt.getLocBaseIdx());  
			ival = pp.topIntVal();  // = 0 or 1
			omsg("hlogkw: top ival = " + ival + ", jval = " + jval);
		}
		switch (kwtop) {
		case AND:
			omsg("hlogkw: AND");
			kval = 1;
			if (ival >= 0) {
				isShortCircuit = 
					((ival == 0) || (jval == 0));
				kval = isShortCircuit ? 0 : 1;
			}
			if (store.popNode() == null) {
				return STKUNDERFLOW;
			}
			else if (!pp.pushKwdVal(kval)) {
				return STKOVERFLOW;
			}
			break;
		case OR:
			omsg("hlogkw: OR, ival = " + ival);
			kval = 0;
			if (ival >= 0) {
				isShortCircuit = 
					((ival == 1) || (jval == 1));
				kval = isShortCircuit ? 1 : 0;
				omsgz("hlogkw: OR ival = " + ival); 
				omsg(", jval = " + jval + ", kval = " + kval);
			}
			if (store.popNode() == null) {
				return STKUNDERFLOW;
			}
			else if (!pp.pushKwdVal(kval)) {
				return STKOVERFLOW;
			}
			break;
		default:
			break;
		} 
		if (isShortCircuit) {
			// skip over calling getRightp until zero:
			while (rightp > 0) {
				node = store.getNode(rightp);
				rightp = node.getRightp();
			}
		}
		else if (rightp == 0) {
			return EXIT;  // got to end, no short circuit
		}
		return rightp;
	}		

	public int logicalQuestKwd(int rightp) {
		Node node;
		AddrNode addrNode;
		int ival, jval;

		addrNode = store.popNode();
		if (addrNode == null) {
			return STKUNDERFLOW;
		}
		ival = pp.topIntVal(); 
		omsg("logicalQuestKwd: top, ival = " + ival);
		if (ival < 0) {
			if (store.popNode() == null) {  // pop -1
				return STKUNDERFLOW;
			}
			if (addrNode.getHdrPgTyp() != PageTyp.BOOLEAN) {
				return BADOPTYP; 
			}
			jval = pp.nodeToIntVal(addrNode, rt.getLocBaseIdx());
			omsg("logicalQuestKwd: jval = " + jval);
			if (jval == 0) { }
			else if (!pp.pushKwdVal(0)) {
				return STKOVERFLOW;
			}
			else {
				return rightp;
			}
			node = store.getNode(rightp);
			rightp = node.getRightp();
			if (rightp <= 0) {
				return GENERR;
			}
			omsg("logicalQuestKwd: rightp = " + rightp);
			return rightp;
		}
		else if (ival == 0) {
			if (store.popNode() == null) {  
				return STKUNDERFLOW;
			}
			if (!store.pushNode(addrNode)) {
				return STKOVERFLOW;
			}
			node = store.getNode(rightp);
			rightp = node.getRightp();
			return rightp;
		}
		else {
			omsg("logicalQuestKwd: bad ival = " + ival);
			return GENERR;
		}
	}

	private int logicalCaseKwd(int rightp) {
		Node node;
		AddrNode addrNode;
		KeywordTyp kwtyp;
		KeywordTyp kwtop;
		boolean isCqCase;
		boolean isCqTrue;
		boolean isSwixCase;
		int ival, jval;
		int rtnval;
		int stkidx;
		PageTyp pgtyp;
		
		omsg("logicalCaseKwd: top");
		kwtop = popKwd();
		kwtyp = topKwd();
		omsg("logicalCaseKwd: kwtyp = " + kwtyp);
		isCqCase = (kwtyp == KeywordTyp.CQUEST);
		isSwixCase = (kwtyp == KeywordTyp.SWIX);
		pushOp(kwtop);
		if (!isCqCase && !isSwixCase) {
			return rightp;
		}
		if (isSwixCase) {
			popKwd();
			addrNode = store.popNode();
			popVal();
			store.swapNodes();
			store.pushNode(addrNode);
			rtnval = runop.runEqExpr();
			if (rtnval < 0) {
				return rtnval;
			}
		}
		stkidx = pp.popIntStk();
		if (stkidx < 0) {
			return stkidx;
		}
		addrNode = store.fetchNode(stkidx);
		pgtyp = addrNode.getHdrPgTyp();
		if (pgtyp != PageTyp.BOOLEAN) { 
			omsg("logicalCaseKwd: BADOPTYP");
			return BADOPTYP;
		}
		ival = addrNode.getAddr();
		omsg("logicalCaseKwd: ival = " + ival);
		if (ival == 1) {
			return rightp;
		}
		kwtyp = popKwd();
		if (kwtyp == KeywordTyp.SWIX) {
			pushOp(kwtyp);
			addrNode = store.popNode();
			if (pp.topIntVal() == 0) {
				// no cases were true:
				omsg("logicalCaseKwd: top = 0 (swix)");
				popKwd();
				popVal();
				popVal();
				pushAddr(0);
				return 0;
			}
			rightp = popVal();
			store.pushNode(addrNode);
			return rightp;
		}
		popVal();
		if (pp.topIntVal() == 0) {
			// no cases were true:
			omsg("logicalCaseKwd: top = 0");
			popKwd();
			popVal();
			popVal();
			pushAddr(0);
			return 0;
		}
		rightp = popVal();
		return rightp;
	}

	public int pushWhileStmt(Node node, int rightp) {
		KeywordTyp kwtyp;

		omsg("(2) pushWhileStmt: top");
		rt.setAfterStmtKwd(true);
		kwtyp = KeywordTyp.WHILE;
		if (!pushOp(kwtyp) || !pushAddr(rightp)) {
			return STKOVERFLOW;
		}
		rightp = node.getRightp();
		return rightp;
	}
	
	public int pushForStmt(Node node, int rightp) {
		KeywordTyp kwtyp;

		omsg("(2) pushForStmt: top");
		rt.setAfterStmtKwd(true);
		kwtyp = KeywordTyp.FOR;
		if (!pushOp(kwtyp)) { // || !pushAddr(rightp)) {
			return STKOVERFLOW;
		}
		rightp = node.getRightp();
		node = store.getNode(rightp);
		// now we're at do #1
		if (rt.getForContinue()) {
			rightp = node.getRightp();
			// debug:
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			omsg("pushForStmt: kwtyp = " + kwtyp);
			rt.setForContinue(false);
		}
		return rightp;
	}
	
	public int runForStmt() {  
		// end of for loop header reached
		// loop control flag on stack
		int rightp;
		int stkidx;
		int ival;
		Node node;
		AddrNode addrNode;
		PageTyp pgtyp;
		
		omsg("runForStmt: top");
		stkidx = pp.popIntStk();
		if (stkidx < 0) {
			return stkidx;
		}
		addrNode = store.fetchNode(stkidx);
		pgtyp = addrNode.getHdrPgTyp();
		if (pgtyp != PageTyp.BOOLEAN) { 
			omsg("handleDoToken: BADOPTYP");
			return BADOPTYP;
		}
		ival = addrNode.getAddr();
		if (ival == 0) {
			popVal(); // 2nd zstmt in header
			rightp = popVal(); // zstmt of for
			popVal(); // ZSTMT
			node = store.getNode(rightp);
			rightp = node.getRightp();
			popKwd(); // QUEST
			popKwd(); // FOR
			return rightp;
		}
		popVal(); // 2nd zstmt in header
		rightp = popVal(); // zstmt of for  
		node = store.getNode(rightp);
		popKwd(); // QUEST
		if (!pushAddr(rightp)) { // zstmt of for
			return STKOVERFLOW; 
		}
		rightp = node.getDownp();
		node = store.getNode(rightp);
		rightp = node.getRightp();
		node = store.getNode(rightp);
		rightp = node.getRightp();
		node = store.getNode(rightp);
		rightp = node.getRightp();
		node = store.getNode(rightp);
		rightp = node.getDownp();
		return rightp; 
	}

	public int handleDoToken(Node node, int rightp) {
		KeywordTyp kwtyp;
		AddrNode addrNode;
		PageTyp pgtyp;
		int stkidx;
		int ival;
		int rtnval;
		boolean isWhile;
		boolean isCase;
		
		kwtyp = topKwd();
		isWhile = (kwtyp == KeywordTyp.WHILE);
		isCase = (kwtyp == KeywordTyp.CASE);
		rt.setWhileUntil(false);
		switch (kwtyp) {
		case IF:
		case ELIF:
		case CASE:
		case WHILE:
			omsg("handleDoToken: afterStmtKwd = " + rt.getAfterStmtKwd());
			if (isWhile && rt.getAfterStmtKwd()) {
				rt.setWhileUntil(true);
				popKwd();
				pushOp(KeywordTyp.UNTIL);
				ival = 1;
				break;
			}
			if (isCase) {
				popKwd();
				rtnval = runop.runEqExpr();
				if (rtnval < 0) {
					return rtnval;
				}
			}
			stkidx = pp.popIntStk();
			if (stkidx < 0) {
				return stkidx;
			}
			addrNode = store.fetchNode(stkidx);
			pgtyp = addrNode.getHdrPgTyp();
			if (pgtyp != PageTyp.BOOLEAN) { 
				omsg("handleDoToken: BADOPTYP");
				return BADOPTYP;
			}
			ival = addrNode.getAddr();
			if (isWhile) {
				omsg("(3) handleDoToken: ival = " + ival);
			}
			break;
		case ELSE:
			ival = 1;
			break;
		case FOR:
			omsg("handleDoToken: FOR");
			ival = 1;
			break;
		default:
			return BADDOSTMT;
		}
		// if ival = 1 then do block is executed
		// else (ival = 0):
		omsg("(3) handleDoToken: ival = " + ival);
		omsg("handleDoToken: bool as int = " + ival);
		if (ival == 1) { }
		//else if (isWhile && !afterStmtKwd) {
		else if (isWhile) {
			rightp = popVal();  // points to while stmt
			omsg("handleDoToken: WHILE LOOP EXIT");
			return 0;
		}
		else {
			rightp = node.getRightp();
			return rightp;
		}
		if (isCase) {
			//popKwd();
		}
		rightp = node.getDownp();
		if (!pushAddr(rightp)) {
			return STKOVERFLOW;
		}
		if (!pushOp(KeywordTyp.DO)) {
			return STKOVERFLOW;
		}
		return 0;
	}

	public int runDoStmt() {
		int rightp;
		omsg("runDoStmt: top");
		if (rt.getWhileUntil()) {
			omsg("runDoStmt: isWhileUntil");
		}
		rightp = popVal(); 
		return rightp;
	}
	
	public boolean isJumpKwd(KeywordTyp kwtyp) {
		switch (kwtyp) {
		case ZCALL:
		case RETURN:
		case BREAK:
		case CONTINUE:
		case QUEST:
		case ZQUEST:
		case DO:
			omsg("isJumpKwd: kwtyp = " + kwtyp);
			return true;
		default:
			return false;
		}
	}
	
	public boolean isLogicalKwd(KeywordTyp kwtyp) {
		switch (kwtyp) {
		case AND:
		case OR:
		case QUEST:
		case CASE:
			return true;
		default:
			return false;
		}
	}

	public boolean isQuestKwd(KeywordTyp kwtyp) {
		switch (kwtyp) {
		case QUEST:
		case CQUEST:
		case SWIX:
		case CASE:
			return true;
		default:
			return false;
		}
	}

	public boolean isBranchKwd(KeywordTyp kwtyp) {
		switch (kwtyp) {
		case IF:
		case ELIF:
		case ELSE:
			return true;
		default:
			return false;
		}
	}
	
	private boolean isLoopKwd(KeywordTyp kwtyp) {
		switch (kwtyp) {
		case WHILE:
		case FOR:
			return true;
		default:
			return false;
		}
	}
	
	public boolean isNoSwapKwd(KeywordTyp kwtyp) {
		switch (kwtyp) {
		case CASE:
			return true;
		default:
			return false;
		}
	}

	public int runBoolStmt() {
		omsg("runBoolStmt: top");
		return 0; 
	}
	
	public int runSwitchStmt() {
		omsg("runSwitchStmt: top");
		popVal();  // ZSTMT?
		return 0; 
	}
	
	public int pushBrkStmt(Node node) {
		int rightp;
		KeywordTyp kwtyp = KeywordTyp.BREAK;
		
		omsg("pushBrkStmt: top");
		if (!pushOp(kwtyp)) {
			return STKOVERFLOW;
		}
		rightp = node.getRightp();
		if (rightp > 0) {  // naked brk kwd expected
			return BADBRKSTMT;
		}
		if (!isBrkInLoop()) {
			return BADBRKSTMT;
		}
		rightp = 0;
		return rightp;
	}
	
	public int pushContinueStmt(Node node) {
		int rightp;
		KeywordTyp kwtyp = KeywordTyp.CONTINUE;
		
		omsg("pushContinueStmt: top");
		if (!pushOp(kwtyp)) {
			return STKOVERFLOW;
		}
		rightp = node.getRightp();
		if (rightp > 0) {  // naked continue kwd expected
			return BADBRKSTMT;
		}
		if (!isBrkInLoop()) {
			return BADBRKSTMT;
		}
		rightp = 0;
		return rightp;
	}
	
	public int runContinueStmt() {
		KeywordTyp kwtyp;
		int addr;
		int rightp;
		Node node;
		
		omsg("runContinueStmt: top");
		while (true) {
			kwtyp = popKwd();
			while (kwtyp == KeywordTyp.DO) { 
				kwtyp = popKwd();
			}
			switch (kwtyp) {
			case IF:
			case ELIF:
			case ELSE:
				popVal(); // addr
				popVal(); // ZSTMT
				break;
			case SWITCH:
			case CASE:
				popVal(); // switch control expr.
				popVal(); // addr
				popVal(); // ZSTMT
				break;
			case WHILE:
				popVal(); // 
				popVal(); // ZSTMT
				addr = popVal();
				popVal(); 
				popVal(); // ZSTMT
				rightp = addr;
				// debug:
				node = store.getNode(addr);
				kwtyp = node.getKeywordTyp();
				omsg("runContinueStmt, while kwtyp = " + kwtyp);
				return rightp;
			case FOR:
				rt.setForContinue(true);
				popVal(); // 
				popVal(); // ZSTMT
				addr = popVal();
				popVal(); // ZSTMT
				rightp = addr;
				// debug:
				node = store.getNode(rightp);
				kwtyp = node.getKeywordTyp();
				omsg("runContinueStmt, for kwtyp = " + kwtyp);
				return rightp;
			default:
				return BADBRKSTMT;
			}
		}
	}
	
	public int runBrkStmt() {
		KeywordTyp kwtyp;
		int addr;
		int rightp;
		Node node;
		
		while (true) {
			kwtyp = popKwd();
			while (kwtyp == KeywordTyp.DO) { 
				kwtyp = popKwd();
			}
			switch (kwtyp) {
			case IF:
			case ELIF:
			case ELSE:
				popVal(); // addr
				popVal(); // ZSTMT
				break;
			case SWITCH:
			case CASE:
				popVal(); // switch control expr.
				popVal(); // addr
				popVal(); // ZSTMT
				break;
			case WHILE:
			case FOR:
				popVal(); // 
				popVal(); // ZSTMT
				addr = popVal();
				if (kwtyp == KeywordTyp.WHILE) {
				  popVal(); // addr again 
				}
				popVal(); // ZSTMT
				node = store.getNode(addr);
				rightp = node.getRightp();
				return rightp;
			default:
				return BADBRKSTMT;
			}
		}
	}
	
	private boolean isBrkInLoop() {
		KeywordTyp kwtyp;
		int i = 0;
		boolean isDoKwd = false;
		boolean wasDoKwd;
		
		while (true) {
			kwtyp = pp.pickKwd(i);
			if (kwtyp == KeywordTyp.ZNULL) {
				return false;
			}
			if (isLoopKwd(kwtyp)) {
				return true;
			}
			wasDoKwd = isDoKwd;
			isDoKwd = (kwtyp == KeywordTyp.DO);
			if (isDoKwd && wasDoKwd) {
				return false; // bottom of function
			}
			i++;
		}
	}
	
	public boolean isKwdSkipped(KeywordTyp kwtyp) {
		switch (kwtyp) {
		case ELIF:
		case ELSE:
		//case CASE:
			return true;
		default:
			return false;
		}
	}
	
	public int handleSkipKwd(Node node, int rightp) {
		KeywordTyp kwtyp;
		
		if (topKwd() == KeywordTyp.SWIX) { 
			pushAddr(0);
		}
		else {
			popKwd();
			kwtyp = node.getKeywordTyp();
			if (!pushOp(kwtyp)) {
				return STKOVERFLOW;
			}
		}
		rightp = node.getRightp();
		return rightp;
	}
	
	public int handleCaseKwd(Node node, int rightp) {
		// don't need?
		KeywordTyp kwtyp;
		KeywordTyp switchkwd;
		int addr1, addr2;

		omsg("handleCaseKwd: top");
		kwtyp = node.getKeywordTyp();
		if (!pushOp(kwtyp)) {
			return STKOVERFLOW;
		}
		rightp = node.getRightp();
		return rightp;
	}
	
	public int pushIfStmt(Node node) {
		int rightp;
		KeywordTyp kwtyp;

		omsg("pushIfStmt: top");
		kwtyp = KeywordTyp.IF;
		if (!pushOp(kwtyp)) {
			return STKOVERFLOW;
		}
		rightp = node.getRightp();
		return rightp;
	}
	
	public int doBtmUntilLoop() {
		int rightp;
		Node node;
		int stkidx;
		AddrNode addrNode;
		PageTyp pgtyp;
		int ival;
		
		rightp = popVal();
		node = store.getNode(rightp);
		rightp = node.getDownp();
		node = store.getNode(rightp);
		rightp = node.getRightp();
		node = store.getNode(rightp);
		rightp = node.getRightp();
		node = store.getNode(rightp);
		rightp = node.getRightp();
		rightp = rt.handleExprToken(rightp, true);
		stkidx = pp.popIntStk();
		if (stkidx < 0) {
			return stkidx;
		}
		addrNode = store.fetchNode(stkidx);
		pgtyp = addrNode.getHdrPgTyp();
		if (pgtyp != PageTyp.BOOLEAN) { 
			omsg("doBtmUntilLoop: BADOPTYP");
			return BADOPTYP;
		}
		ival = addrNode.getAddr();
		omsg("doBtmUntilLoop: ival = " + ival);
		popKwd();
		rightp = popVal();
		if (ival == 0) {
			popVal();
			popVal();
		}
		else {
			node = store.getNode(rightp);
			rightp = node.getRightp();
		}
		return rightp;
	}
	
	public int pushSwitchStmt(Node node) {
		int rightp;
		KeywordTyp kwtyp;

		omsg("pushSwitchStmt: top");
		kwtyp = KeywordTyp.SWITCH;
		if (!pushOp(kwtyp)) {
			return STKOVERFLOW;
		}
		rightp = node.getRightp();
		return rightp;
	}
	
	private KeywordTyp popKwd() {
		return pp.popKwd();
	}
	
	private KeywordTyp topKwd() {
		return pp.topKwd();
	}
	
	private int popVal() {
		return pp.popVal();
	}

	private boolean pushAddr(int rightp) {
		return pp.pushAddr(rightp);
	}
	
	private boolean pushOp(KeywordTyp kwtyp) {
		return pp.pushOp(kwtyp);
	}
	

}