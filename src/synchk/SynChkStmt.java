package synchk;

import iconst.KeywordTyp;
import iconst.NodeCellTyp;
import page.Node;
import page.Page;
import page.Store;
import scansrc.ScanSrc;
import synchk.SynChk;
import synchk.SynChkExpr;

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
	
	private void oerr(int nodep, String msg) {
		synChk.oerr(nodep, msg);
	}
	
	public boolean doStmt(int rightp) {
		Page page;
		int idx;
		Node node;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;

		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		celltyp = node.getDownCellTyp();
		out("rightp = " + rightp + ", idx = " + idx + 
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
		case DEL: return doDelStmt(rightp);
		case PRINT: return doPrintStmt(rightp);
		case ECHO: return doEchoStmt(rightp);
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
		case ANDSET:
		case XORSET:
		case ORSET:
			return doSetOpStmt(rightp);
		default:
			oerr(rightp, "Invalid keyword: " + kwtyp.toString() +
				" encountered at beginning of statement");
			return false;
		}
	}
	
	public boolean doLoopStmt(int rightp) {
		Page page;
		int idx;
		Node node;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;

		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		celltyp = node.getDownCellTyp();
		out("rightp = " + rightp + ", idx = " + idx + 
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
		case ANDSET:
		case XORSET:
		case ORSET:
			return doSetOpStmt(rightp);
		default:
			return false;
		}
	}
	
	private boolean doIfStmt(int rightp) {
		Page page;
		int idx;
		Node node;
		KeywordTyp kwtyp;
		String msg = "Error in if stmt.: ";
		boolean isElse = false;
		int savep = rightp;

		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerr(savep, msg + "no body");
			return false;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		while (rightp > 0) {
			if (isElse) { }
			else if (!synExpr.doExpr(rightp)) {
				oerr(rightp, msg + "invalid expression");
				return false;
			}
			else {
				page = store.getPage(rightp);
				idx = store.getElemIdx(rightp);
				node = page.getNode(idx);
				rightp = node.getRightp();
			}
			if (rightp <= 0) {
				oerr(savep, msg + "no do-block");
				return false;
			}
			rightp = synChk.chkStmtDoBlock(rightp);
			if (rightp < 0) {
				oerr(savep, "Error in if stmt.");
				return false;
			}
			else if (rightp == 0) {
				return true;
			}
			savep = rightp;
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			kwtyp = node.getKeywordTyp();
			if (kwtyp == KeywordTyp.ELSE) {
				isElse = true;
			}
			else if (kwtyp != KeywordTyp.ELIF) {
				oerr(savep, msg + "invalid keyword " + kwtyp + " found");
				return false;
			}
			rightp = node.getRightp();
		}
		oerr(savep, msg + "dangling ELSE or ELIF");
		return false;
	}
	
	private boolean doSetOpStmt(int rightp) {
		Page page;
		int idx;
		Node node;
		String msg = "Error in asst. stmt.: ";
		int savep = rightp;
		
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerr(savep, msg + "no args.");
			return false;
		}
		if (!synExpr.doTargetExpr(rightp)) {
			oerr(savep, msg + "invalid target expr.");
			return false;
		}
		return doSetStmtTail(rightp, savep, msg);
	}
	
	private boolean doSetStmtTail(int rightp, int savep, String msg) {
		Page page;
		int idx;
		Node node;
		
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerr(savep, msg + "missing result expr.");
			return false;
		}
		if (!synExpr.doExpr(rightp)) {
			oerr(savep, msg + "invalid result expr.");
			return false;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		rightp = node.getRightp();
		if (rightp > 0) {
			oerr(savep, msg + "too many args.");
			return false;
		}
		return true;
	}
	
	private boolean doSetStmt(int rightp) {
		Page page;
		int idx;
		Node node;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		String msg = "Error in SET stmt.: ";
		int savep = rightp;
		
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerr(savep, msg + "no args.");
			return false;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		celltyp = node.getDownCellTyp();
		if (celltyp == NodeCellTyp.ID) {
			return doSetOpStmt(savep);
		}
		rightp = synExpr.parenExprRtn(rightp, node); 
		if (rightp <= 0) {
			oerr(savep, msg + "invalid parenthetical arg.");
			return false;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		if (kwtyp == KeywordTyp.TUPLE) {
			return doSetTuple(savep);
		}
		return doSetOpStmt(savep);
	}
	
	private boolean doSetTuple(int rightp) {
		Page page;
		int idx;
		Node node;
		KeywordTyp kwtyp;
		String msg = "Error in tuple asst. stmt.: ";
		int savep = rightp;
		int rightq;
		int count = 0;
		
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerr(savep, msg + "no args.");
			return false;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		rightq = rightp;
		rightp = synExpr.parenExprRtn(rightp, node); 
		if (rightp <= 0) {
			oerr(savep, msg + "no tuple arg. in parentheses");
			return false;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.TUPLE) {
			oerr(savep, msg + "expecting TUPLE keyword, " + kwtyp + " found");
			return false;
		}
		rightp = node.getRightp();
		while (rightp > 0) {
			count++;
			if (!synExpr.doTargetExpr(rightp)) {
				oerr(savep, msg + "invalid target expr.");
				return false;
			}
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			rightp = node.getRightp();
		}
		if (count == 0) {
			oerr(savep, msg + "no target exprs.");
			return false;
		}
		return doSetStmtTail(rightq, savep, msg);
	}
	
	private boolean doIncDecStmt(int rightp) {
		Page page;
		int idx;
		Node node;
		NodeCellTyp celltyp;
		String msg = "Error in INC/DEC stmt.: ";
		int savep = rightp;
		
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerr(savep, msg + "no args.");
			return false;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		celltyp = node.getDownCellTyp();
		if (celltyp != NodeCellTyp.ID) {
			oerr(savep, msg + "no identifier found");
			return false;
		}
		rightp = node.getRightp();
		if (rightp > 0) {
			oerr(savep, msg + "too many args.");
			return false;
		}
		return true;
	}
	
	private boolean doWhileStmt(int rightp) {
		Page page;
		int idx;
		Node node;
		KeywordTyp kwtyp;
		String msg = "Error in while stmt.: ";
		int savep = rightp;

		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerr(savep, msg + "no body");
			return false;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.DO) {
			if (!synExpr.doExpr(rightp)) {
				oerr(rightp, msg + "invalid expression");
				return false;
			}
			rightp = node.getRightp();
			if (rightp <= 0) {
				oerr(savep, msg + "no do-block");
				return false;
			}
			rightp = synChk.chkDoBlock(rightp);
			if (rightp < 0) {
				oerr(savep, "Error in while stmt.");
				return false;
			}
			return true;
		}
		rightp = synChk.chkStmtDoBlock(rightp);
		if (rightp < 0) {
			oerr(savep, "Error in while stmt.");
			return false;
		}
		if (rightp == 0) {
			oerr(savep, msg + "expecting UNTIL, invalid text found");
			return false;
		}
		savep = rightp;
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.UNTIL) {
			oerr(savep, msg + "expecting UNTIL, " + kwtyp + " found");
			return false;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerr(savep, msg + "missing expression");
			return false;
		}
		if (!synExpr.doExpr(rightp)) {
			oerr(savep, msg + "invalid expression after UNTIL");
			return false;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		rightp = node.getRightp();
		if (rightp > 0) {
			oerr(savep, msg + "invalid text after expression");
			return false;
		}
		return true;
	}
	
	private boolean doForStmt(int rightp) {
		Page page;
		int idx;
		Node node;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		String msg = "Error in for stmt.: ";
		int savep = rightp;

		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerr(savep, msg + "no body");
			return false;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		celltyp = node.getDownCellTyp();
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.DO) {
			if (celltyp != NodeCellTyp.ID) {
				oerr(rightp, msg + "identifier not found");
				return false;
			}
			rightp = node.getRightp();
			if (rightp <= 0) {
				oerr(savep, msg + "dangling single identifier");
				return false;
			}
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			celltyp = node.getDownCellTyp();
			kwtyp = node.getKeywordTyp();
			if (celltyp == NodeCellTyp.ID) {
				rightp = node.getRightp();
				page = store.getPage(rightp);
				idx = store.getElemIdx(rightp);
				node = page.getNode(idx);
				kwtyp = node.getKeywordTyp();
			}
			if (kwtyp != KeywordTyp.IN) {
				oerr(savep, msg + "expecting IN, but " + kwtyp + " found");
				return false;
			}
			rightp = node.getRightp();
			if (rightp <= 0) {
				oerr(savep, msg + "dangling IN keyword");
				return false;
			}
			if (!synExpr.doExpr(rightp)) {
				oerr(savep, msg + "invalid expression after IN");
				return false;
			}
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			rightp = node.getRightp();
			if (rightp <= 0) {
				oerr(savep, msg + "missing do-block");
				return false;
			}
		}
		else {
			rightp = chkLoopDo(rightp);
			if (rightp <= 0) {
				oerr(savep, "Error in for stmt. header");
				return false;
			}
		}
		rightp = synChk.chkDoBlock(rightp);
		if (rightp < 0) {
			oerr(savep, "Error in for stmt.");
			return false;
		}
		return true;
	}
	
	private int chkLoopDo(int rightp) {
		Page page;
		int idx;
		Node node;
		KeywordTyp kwtyp;
		int i;
		int downp;
		int rightq;
		int rtnval = -1;
		
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		rightq = node.getRightp();
		if (rightq > 0) { 
			rtnval = rightq;
		}
		rightp = node.getDownp();
		if (rightp <= 0) {
			return -1;  // never
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		if (!node.isOpenPar()) {  // never lands here
			return -1;
		}
		rightq = node.getDownp();
		if (rightq <= 0) {
			return -1;  // never
		}
		page = store.getPage(rightq);
		idx = store.getElemIdx(rightq);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		if (kwtyp == KeywordTyp.TUPLE) { 
			return -1; 
		}
		for (i = 0; i < 3; i++) {
			if (rightp <= 0) {
				return -1;
			}
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			if (!node.isOpenPar()) {  // may never happen
				return -1;
			}
			downp = node.getDownp();
			if ((downp <= 0) || !doLoopStmt(downp)) {
				return -1;
			}
			rightp = node.getRightp();
		}
		if (rightp > 0) {
			return -1;
		}
		return rtnval;
	}
	
	private boolean doBoolStmt(int rightp) {
		Page page;
		int idx;
		Node node;
		int savep = rightp;

		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		rightp = node.getRightp();
		if (rightp <= 0) {
			return true;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		if (!synExpr.doExpr(rightp)) {
			oerr(savep, "Invalid BOOL expression");
			return false;
		}
		rightp = node.getRightp();
		if (rightp > 0) {
			oerr(savep, "Invalid text after BOOL expression");
			return false;
		}
		return true;
	}
	
	private boolean doTryStmt(int rightp) {
		return true;
	}
	
	private boolean doDelStmt(int rightp) {
		return true;
	}
	
	private boolean doPrintStmt(int rightp) {
		return true;
	}
	
	private boolean doEchoStmt(int rightp) {
		return true;
	}
	
	private boolean doCallStmt(int rightp) {
		return true;
	}
	
	private boolean doCallFunStmt(int rightp) {
		return true;
	}
	
	private boolean doDotStmt(int rightp) {
		return true;
	}
	
	private boolean doRaiseStmt(int rightp) {
		return true;
	}
	
	private boolean doContinueStmt(int rightp) {
		return true;
	}
	
	private boolean doBreakStmt(int rightp) {
		return true;
	}
	
	private boolean doReturnStmt(int rightp) {
		return true;
	}
	
}
