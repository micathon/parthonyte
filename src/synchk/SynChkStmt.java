package synchk;

import iconst.KeywordTyp;
import iconst.NodeCellTyp;
import page.Node;
import page.Store;
import scansrc.ScanSrc;

public class SynChkStmt {

	private ScanSrc scan;
	private Store store;
	private SynChk synChk;
	private SynChkExpr synExpr;
	
	public SynChkStmt(SynChk synChk, ScanSrc scan, Store store) {
		this.synChk = synChk;
		this.scan = scan;
		this.store = store;
	}
	
	public void init() {
		this.synExpr = synChk.synExpr;
	}

	private void out(String msg) {
		scan.out(msg);
	}
	
	private void oprn(String msg) {
		scan.oprn(msg);
	}
	
	@SuppressWarnings("unused")
	private void omsg(String msg) {
		scan.omsg(msg);
	}
	
	@SuppressWarnings("unused")
	private void oerr(int nodep, String msg) {
		synChk.oerr(nodep, msg);
	}
	
	private void oerrd(int nodep, String msg, double bval) {
		synChk.oerrmod(nodep, msg, bval, 2);
	}
	
	public boolean doStmt(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;

		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		celltyp = node.getDownCellTyp();
		out("rightp = " + rightp +  
				", kwd = " + kwtyp + ", celtyp = " + celltyp);
		out("Statement kwd = " + kwtyp);
		switch (kwtyp) {
		case SET: return doSetStmt(rightp);
		case INCINT:
		case DECINT:
			return doIncDecStmt(rightp);
		case IF: return doIfStmt(rightp);
		case WHILE: return doWhileStmt(rightp);
		case FOR: return doForStmt(rightp);
		case TRY: return doTryStmt(rightp);
		case SWITCH: return doSwitchStmt(rightp);
		case DEL: return doDelStmt(rightp);
		case PRINT: return doPrintStmt(rightp, false);
		case PRINTLN: return doPrintlnStmt(rightp);
		case ECHO: return doEchoStmt(rightp);
		case UTPUSH: return doUtPushStmt(rightp);
		case UTSCAN: return doUtScanStmt(rightp);
		case CALL: return doCallStmt(rightp);
		case ZCALL: return doCallFunStmt(rightp);
		case QUEST: return doBoolStmt(rightp);
		case DOT: return doDotStmt(rightp);
		case RAISE: return doRaiseStmt(rightp);
		case CONTINUE: return doContinueStmt(rightp);
		case BREAK: return doBreakStmt(rightp);
		case RETURN: return doReturnStmt(rightp);
		case ADDSET:
		case MINUSSET:
		case MPYSET:
		case DIVSET:
		case IDIVSET:
		case MODSET:
		case SHLSET:
		case SHRSET:
		case SHRUSET:
		case ANDBSET:
		case XORBSET:
		case ORBSET:
			return doSetOpStmt(rightp);
		default:
			oerrd(rightp, "Invalid keyword: " + kwtyp.toString() +
				" encountered at beginning of statement", 10.1);
			return false;
		}
	}
	
	public boolean doLoopStmt(int rightp) {
		// one of 3 stmts. in for-loop header
		Node node;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;

		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		celltyp = node.getDownCellTyp();
		out("rightp = " + rightp +  
				", kwd = " + kwtyp + ", celtyp = " + celltyp);
		out("Loop Statement kwd = " + kwtyp);
		switch (kwtyp) {
		case SET: return doSetStmt(rightp);
		case INCINT:
		case DECINT:
			return doIncDecStmt(rightp);
		case QUEST: return doBoolStmt(rightp);
		case ADDSET:
		case MINUSSET:
		case MPYSET:
		case DIVSET:
		case IDIVSET:
		case MODSET:
		case SHLSET:
		case SHRSET:
		case SHRUSET:
		case ANDBSET:
		case XORBSET:
		case ORBSET:
			return doSetOpStmt(rightp);
		default:
			return false;
		}
	}
	
	private boolean doIfStmt(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		String msg = "Error in if stmt.: ";
		boolean isElse = false;
		int savep = rightp;

		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, msg + "no body", 30.1);
			return false;
		}
		node = store.getNode(rightp);
		while (rightp > 0) {
			if (isElse) { }
			else if (!synExpr.doExpr(rightp)) {
				oerrd(savep, msg + "invalid expression", 30.2);
				return false;
			}
			else {
				node = store.getNode(rightp);
				rightp = node.getRightp();
			}
			if (rightp <= 0) {
				oerrd(savep, msg + "no do-block", 30.3);
				return false;
			}
			rightp = synChk.chkStmtDoBlock(rightp);
			if (rightp < 0) {
				oerrd(savep, "Error in if stmt.", 30.4);
				return false;
			}
			else if (rightp == 0) {
				return true;
			}
			savep = rightp;
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp == KeywordTyp.ELSE) {
				isElse = true;
			}
			else if (kwtyp != KeywordTyp.ELIF) {
				oerrd(savep, msg + "invalid keyword " + kwtyp + " found",
					30.5);
				return false;
			}
			rightp = node.getRightp();
		}
		oerrd(savep, msg + "dangling ELSE or ELIF", 30.6);
		return false;
	}
	
	private boolean doSetOpStmt(int rightp) {
		Node node;
		String msg = "Error in asst. stmt.: ";
		int savep = rightp;
		
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, msg + "no args.", 40.1);
			return false;
		}
		if (!synExpr.doTargetExpr(rightp)) {
			oerrd(savep, msg + "invalid target expr.", 40.2);
			return false;
		}
		return doSetStmtTail(rightp, savep, msg);
	}
	
	private boolean doSetStmtTail(int rightp, int savep, String msg) {
		Node node;
		
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, msg + "missing result expr.", 50.1);
			return false;
		}
		if (!synExpr.doExpr(rightp)) {
			oerrd(savep, msg + "invalid result expr.", 50.2);
			return false;
		}
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp > 0) {
			oerrd(savep, msg + "too many args.", 50.3);
			return false;
		}
		return true;
	}
	
	private boolean doSetStmt(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		String msg = "Error in SET stmt.: ";
		int savep = rightp;
		
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, msg + "no args.", 60.1);
			return false;
		}
		node = store.getNode(rightp);
		celltyp = node.getDownCellTyp();
		if (celltyp == NodeCellTyp.ID) {
			return doSetOpStmt(savep);
		}
		rightp = synExpr.parenExprRtn(rightp, node); 
		if (rightp <= 0) {
			oerrd(savep, msg + "invalid parenthetical arg.", 60.2);
			return false;
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp == KeywordTyp.TUPLE) {
			return doSetTuple(savep);
		}
		return doSetOpStmt(savep);
	}
	
	private boolean doSetTuple(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		String msg = "Error in tuple asst. stmt.: ";
		int savep = rightp;
		int rightq;
		int count = 0;
		
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, msg + "no args.", 70.1);
			return false;
		}
		node = store.getNode(rightp);
		rightq = rightp;
		rightp = synExpr.parenExprRtn(rightp, node); 
		if (rightp <= 0) {
			oerrd(savep, msg + "no tuple arg. in parentheses", 70.2);
			return false;
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.TUPLE) {
			oerrd(savep, msg + "expecting TUPLE keyword, " + kwtyp + " found",
				70.3);
			return false;
		}
		rightp = node.getRightp();
		while (rightp > 0) {
			count++;
			if (!synExpr.doTargetExpr(rightp)) {
				oerrd(savep, msg + "invalid target expr.", 70.4);
				return false;
			}
			node = store.getNode(rightp);
			rightp = node.getRightp();
		}
		if (count == 0) {
			oerrd(savep, msg + "no target exprs.", 70.5);
			return false;
		}
		return doSetStmtTail(rightq, savep, msg);
	}
	
	private boolean doIncDecStmt(int rightp) {
		Node node;
		NodeCellTyp celltyp;
		String msg = "Error in INC/DEC stmt.: ";
		int savep = rightp;
		
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, msg + "no args.", 80.1);
			return false;
		}
		node = store.getNode(rightp);
		celltyp = node.getDownCellTyp();
		if (celltyp != NodeCellTyp.ID) {
			oerrd(savep, msg + "no identifier found", 80.2);
			return false;
		}
		rightp = node.getRightp();
		if (rightp > 0) {
			oerrd(savep, msg + "too many args.", 80.3);
			return false;
		}
		return true;
	}
	
	private boolean doWhileStmt(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		String msg = "Error in while stmt.: ";
		int savep = rightp;

		oprn("doWhileStmt: chk syntax at top");
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, msg + "no body", 90.1);
			return false;
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.DO) {
			if (!synExpr.doExpr(rightp)) {
				oerrd(savep, msg + "invalid expression", 90.2);
				return false;
			}
			rightp = node.getRightp();
			if (rightp <= 0) {
				oerrd(savep, msg + "no do-block", 90.3);
				return false;
			}
			rightp = synChk.chkDoBlock(rightp);
			if (rightp < 0) {
				oerrd(savep, "Error in while stmt.", 90.4);
				return false;
			}
			return true;
		}
		rightp = synChk.chkStmtDoBlock(rightp);
		if (rightp < 0) {
			oerrd(savep, "Error in while stmt.", 90.45);
			return false;
		}
		if (rightp == 0) {
			oerrd(savep, msg + "expecting UNTIL, invalid text found",
				90.5);
			return false;
		}
		savep = rightp;
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.UNTIL) {
			oerrd(savep, msg + "expecting UNTIL, " + kwtyp + " found",
				90.6);
			return false;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, msg + "missing expression", 90.7);
			return false;
		}
		if (!synExpr.doExpr(rightp)) {
			oerrd(savep, msg + "invalid expression after UNTIL", 90.8);
			return false;
		}
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp > 0) {
			oerrd(savep, msg + "invalid text after expression", 90.9);
			return false;
		}
		oprn("doWhileStmt: chk syntax of while-until");
		return true;
	}
	
	private boolean doForStmt(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		String msg = "Error in for stmt.: ";
		int savep = rightp;

		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, msg + "no body", 100.1);
			return false;
		}
		node = store.getNode(rightp);
		celltyp = node.getDownCellTyp();
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.DO) {
			if (celltyp != NodeCellTyp.ID) {
				oerrd(rightp, msg + "identifier not found", 100.2);
				return false;
			}
			rightp = node.getRightp();
			if (rightp <= 0) {
				oerrd(savep, msg + "dangling single identifier", 100.3);
				return false;
			}
			node = store.getNode(rightp);
			celltyp = node.getDownCellTyp();
			kwtyp = node.getKeywordTyp();
			if (celltyp == NodeCellTyp.ID) {
				rightp = node.getRightp();
				node = store.getNode(rightp);
				kwtyp = node.getKeywordTyp();
			}
			if (kwtyp != KeywordTyp.IN) {
				oerrd(savep, msg + "expecting IN, but " + kwtyp + " found", 
					100.4);
				return false;
			}
			rightp = node.getRightp();
			if (rightp <= 0) {
				oerrd(savep, msg + "dangling IN keyword", 100.5);
				return false;
			}
			if (!synExpr.doExpr(rightp)) {
				oerrd(savep, msg + "invalid expression after IN", 100.6);
				return false;
			}
			node = store.getNode(rightp);
			rightp = node.getRightp();
			if (rightp <= 0) {
				oerrd(savep, msg + "missing do-block", 100.7);
				return false;
			}
		}
		else {
			rightp = chkLoopDo(rightp);
			if (rightp > 0) { }
			else if (rightp == -2) {
				oerrd(savep, msg + "invalid stmt. in header", 100.85);
				return false;
			}
			else if (rightp == -99) {
				oerrd(savep, msg + "header with 3 stmts. is dangling", 
					100.87);
				return false;
			}
			else {
				oerrd(savep, msg + "general error in header", 100.8);
				return false;
			}
		}
		rightp = synChk.chkDoBlock(rightp);
		if (rightp < 0) {
			oerrd(savep, "Error in do-block of for stmt.", 100.9);
			return false;
		}
		return true;
	}
	
	private int chkLoopDo(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		int i;
		int downp;
		int rightq;
		int rtnval = -99;
		
		node = store.getNode(rightp);
		rightq = node.getRightp();
		if (rightq > 0) { 
			rtnval = rightq;
		}
		rightp = node.getDownp();
		if (rightp <= 0) {
			return -1;  // never
		}
		node = store.getNode(rightp);
		if (!node.isOpenPar()) {  // never lands here
			return -1;
		}
		rightq = node.getDownp();
		if (rightq <= 0) {
			return -1;  // never
		}
		node = store.getNode(rightq);
		kwtyp = node.getKeywordTyp();
		if (kwtyp == KeywordTyp.TUPLE) { 
			return -1; 
		}
		// header has 3 loop control stmts.
		for (i = 0; i < 3; i++) {
			if (rightp <= 0) {
				return -1;
			}
			node = store.getNode(rightp);
			if (!node.isOpenPar()) {  // may never happen
				return -1;
			}
			downp = node.getDownp();
			if (downp <= 0) {
				return -1;
			}
			if (!doLoopStmt(downp)) {
				return -2;  // invalid loop control stmt. found
			}
			rightp = node.getRightp();
		}
		if (rightp > 0) {
			return -1;
		}
		return rtnval;
	}
	
	private boolean doBoolStmt(int rightp) {
		Node node;
		int savep = rightp;

		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp <= 0) {
			//oerrd(savep, "Missing BOOL expression", 120.1);
			return true;
		}
		node = store.getNode(rightp);
		if (!synExpr.doExpr(rightp)) {
			oerrd(savep, "Invalid BOOL expression", 120.2);
			return false;
		}
		rightp = node.getRightp();
		if (rightp > 0) {
			oerrd(savep, "Invalid text after BOOL expression", 120.3);
			return false;
		}
		return true;
	}
	
	private boolean doSwitchStmt(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		NodeCellTyp ctyp = NodeCellTyp.NULL;
		String msg = "Error in switch stmt.: ";
		int rightq;
		int savep = rightp;

		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, msg + "no body", 130.05);
			return false;
		}
		node = store.getNode(rightp);
		if (!synExpr.doExpr(rightp)) {
			oerrd(savep, msg + "invalid switch expression", 130.1);
			return false;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, msg + "dangling switch expression", 130.15);
			return false;
		}
		while (true) {
			savep = rightp;
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp == KeywordTyp.ELSE) {
				break;
			}
			if (kwtyp != KeywordTyp.CASE) {
				oerrd(rightp, msg + "expecting CASE, but " + kwtyp + " found",
					130.2);
				return false;
			}
			rightp = node.getRightp();
			if (rightp <= 0) {
				oerrd(savep, msg + "dangling CASE", 130.25);
				return false;
			}
			rightq = synExpr.chkTuple(rightp);
			if (rightq < 0) {
				node = store.getNode(rightp);
				celltyp = node.getDownCellTyp();
				switch (celltyp) {
				case ID:
				case INT:
				case STRING:
					if (ctyp == NodeCellTyp.NULL) {
						ctyp = celltyp;
					}
					else if (celltyp != ctyp) {
						oerrd(rightp, msg + "expecting case " + ctyp +
								" but " + celltyp + " found", 130.3);
						return false;
					}
					break;
				default:
					oerrd(rightp, msg + "invalid case = " + celltyp, 130.35);
					return false;
				}
				rightp = node.getRightp();
			}
			else if (rightq == 0) {
				oerrd(savep, msg + "dangling TUPLE", 130.4);
				return false;
			}
			else {
				rightp = rightq;
			}
			if (rightp <= 0) {
				oerrd(savep, msg + "case DO not found", 130.45);
				return false;
			}
			rightp = synChk.chkStmtDoBlock(rightp);
			if (rightp < 0) {
				oerrd(savep, "Error in switch stmt.", 130.5);
				return false;
			}
			else if (rightp == 0) {
				return true;
			}
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, msg + "dangling ELSE", 130.6);
			return false;
		}
		rightp = synChk.chkDoBlock(rightp);
		if (rightp < 0) {
			oerrd(savep, "Error in ELSE block", 130.7);
			return false;
		}
		return true;
	}
	
	private boolean doTryStmt(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		String errmsg = "Error in try stmt.";
		String msg = errmsg + ": ";
		boolean isExcept = false;
		boolean isElse;
		int savep = rightp;

		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, msg + "no body", 140.0);
			return false;
		}
		rightp = synChk.chkStmtDoBlock(rightp);
		if (rightp < 0) {
			oerrd(savep, errmsg, 140.05);
			return false;
		}
		else if (rightp == 0) {
			oerrd(savep, msg + "no except clauses or eotry block", 
				140.1);
			return false;
		}
		while (true) {
			savep = rightp;
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if ((kwtyp == KeywordTyp.ELSE) || (kwtyp == KeywordTyp.EOTRY)) {
				break;
			}
			if (kwtyp != KeywordTyp.EXCEPT) {
				oerrd(savep, msg + "expecting EXCEPT, but " + kwtyp + " found",
					140.2);
				return false;
			}
			isExcept = true;
			rightp = node.getRightp();
			if (rightp <= 0) {
				oerrd(savep, msg + "dangling EXCEPT", 140.25);
				return false;
			}
			node = store.getNode(rightp);
			celltyp = node.getDownCellTyp();
			if (celltyp != NodeCellTyp.ID) {
				oerrd(savep, msg + "expecting identifier, but " + 
					celltyp + " found", 140.3);
				return false;
			}
			rightp = node.getRightp();
			if (rightp <= 0) {
				oerrd(savep, msg + "dangling identifier", 140.35);
				return false;
			}
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp == KeywordTyp.AS) {
				rightp = node.getRightp();
				if (rightp <= 0) {
					oerrd(savep, msg + "dangling AS", 140.4);
					return false;
				}
				node = store.getNode(rightp);
				celltyp = node.getDownCellTyp();
				if (celltyp != NodeCellTyp.ID) {
					oerrd(savep, msg + "expecting identifier after AS, but " + 
						celltyp + " found", 140.45);
					return false;
				}
				rightp = node.getRightp();
				if (rightp <= 0) {
					oerrd(savep, msg + "except clause missing do-block", 
						140.5);
					return false;
				}
			}
			rightp = synChk.chkStmtDoBlock(rightp);
			if (rightp < 0) {
				oerrd(savep, errmsg, 140.55);
				return false;
			}
			else if (rightp == 0) {
				return true;
			}
		}
		isElse = (kwtyp == KeywordTyp.ELSE);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, msg + "dangling " + kwtyp, 140.6);
			return false;
		}
		if (!isExcept && isElse) {
			oerrd(savep, msg + "unexpected ELSE block w/o any except clauses",
				140.65);
			return false;
		}
		savep = rightp;
		node = store.getNode(rightp);
		rightp = synChk.chkStmtDoBlock(rightp);
		if (rightp < 0) {
			oerrd(savep, msg + "invalid mid do-block", 140.7);
			return false;
		}
		else if (rightp == 0) {
			return true;
		}
		if (!isElse) {
			oerrd(savep, msg + "invalid text after EOTRY block", 140.75);
			return false;
		}
		savep = rightp;
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.EOTRY) {
			oerrd(savep, msg + "expecting EOTRY, but " + kwtyp + " found",
				140.8);
			return false;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, msg + "dangling EOTRY after ELSE block", 140.85);
			return false;
		}
		rightp = synChk.chkStmtDoBlock(rightp);
		if (rightp < 0) {
			oerrd(savep, msg + "invalid final do-block", 140.9);
			return false;
		}
		else if (rightp == 0) {
			return true;
		}
		oerrd(savep, msg + "invalid text after ELSE/EOTRY block", 140.95);
		return false;
	}
	
	private boolean doDelStmt(int rightp) {
		boolean rtnval = synExpr.doUnaryOp(rightp);
		return rtnval;
	}
	
	private boolean doPrintlnStmt(int rightp) {
		int errcode = synExpr.doListOpRtn(rightp, true, "stmt.");
		
		switch (errcode) {
		case -1:
			oerrd(rightp, "Error in println stmt.: " +
				"has invalid argument(s)", 160.1);
			return false;
		}
		return true;
	}

	private boolean doPrintStmt(int rightp, boolean isEcho) {
		String keyword;
		String msg;
		int errcode = synExpr.doListOpRtn(rightp, false, "stmt.");
		
		if (isEcho) {
			keyword = "echo";
		}
		else {
			keyword = "print";
		}
		msg = "Error in " + keyword + " stmt.: ";
		switch (errcode) {
		case -1:
			oerrd(rightp, msg +
				"has invalid argument(s)", 170.1);
			return false;
		case -2:
			oerrd(rightp, msg +
				"has no arguments", 170.2);
			return false;
		}
		return true;
	}

	private boolean doEchoStmt(int rightp) {
		return doPrintStmt(rightp, true);
	}
	
	private boolean doUtPushStmt(int rightp) {
		boolean rtnval = synExpr.doBinaryOp(rightp);
		return rtnval;
	}
	
	private boolean doUtScanStmt(int rightp) {
		boolean rtnval = synExpr.doBinaryOp(rightp);
		return rtnval;
	}
	
	private boolean doCallStmt(int rightp) {
		boolean rtnval = synExpr.doCallOp(rightp);
		return rtnval;
	}
	
	private boolean doCallFunStmt(int rightp) {
		boolean rtnval = synExpr.doZcallOp(rightp);
		return rtnval;
	}
	
	private boolean doDotStmt(int rightp) {
		boolean rtnval = synExpr.doDotOp(rightp, false, false);
		return rtnval;
	}
	
	private boolean doContinueStmt(int rightp) {
		boolean rtnval = synExpr.doZeroOp(rightp);
		return rtnval;
	}
	
	private boolean doBreakStmt(int rightp) {
		boolean rtnval = synExpr.doZeroOp(rightp);
		return rtnval;
	}
	
	private boolean doReturnStmt(int rightp) {
		int errcode = synExpr.doOptArgOp(rightp);
		
		switch (errcode) {
		case -1:
			oerrd(rightp, "Error in return stmt.: " +
				"has invalid argument", 200.1);
			return false;
		case -2:
			oerrd(rightp, "Error in return stmt.: " +
				"has more than one argument", 200.2);
			return false;
		}
		return true;
	}
	
	private boolean doRaiseStmt(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		int savep = rightp;
		
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		rightp = node.getRightp();
		if (rightp <= 0) {
			return true;  // raise;
		}
		if (!synExpr.doExpr(rightp)) {
			oerrd(savep, "Raise stmt. followed by invalid expr.", 210.1);
			return false;
		}
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp <= 0) {
			return true;  // raise <expr>;
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.FROM) {
			oerrd(savep, "Expecting FROM in raise stmt., " + kwtyp + " found",
				210.2);
			return false;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, "Dangling FROM in raise stmt.", 210.3);
			return false;
		}
		if (!synExpr.doExpr(rightp)) {
			oerrd(savep, "Raise stmt. followed by invalid 2nd expr.", 210.4);
			return false;
		}
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp > 0) {
			oerrd(savep, "Raise stmt. followed by invalid text", 210.5);
			return false;
		}
		return true;
	}
	
}
