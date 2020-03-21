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
	@SuppressWarnings("unused")
	private SynChkExpr synExpr;
	private boolean isZpar;
	
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
	
	public int chkDo(int rightp) {
		Page page;
		int idx;
		Node node;
		int downp;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int savep;

		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		savep = node.getRightp();
		if (savep <= 0) {
			savep = 0;
		}
		if (!node.isOpenPar()) {  // never lands here
			oerr(rightp, "Do block error (in chkDo): body lacks parentheses");
			return -1;
		}
		rightp = node.getDownp();
		if (rightp <= 0) {
			return savep;  // OK
		}
		while (rightp > 0) {
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			kwtyp = node.getKeywordTyp();
			celltyp = node.getDownCellTyp();
			out("rightp = " + rightp + ", idx = " + idx + 
				", kwd = " + kwtyp + ", celtyp = " + celltyp);
			if (!node.isOpenPar()) {
				oerr(rightp, "Do block error (in chkDo): body lacks semicolon(s)");
				return -1;
			}
			out("Here is (");
			downp = node.getDownp();
			if (!doStmt(downp)) {
				return -1;
			}
			out("Here is )");
			if (isZpar) {
				out("Zparen found");
			}
			rightp = node.getRightp();
		}
		return savep; // OK
	}
	
	private boolean doStmt(int rightp) {
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
		case IF: return doIfStmt(rightp);
		case WHILE: return doWhileStmt(rightp);
		case FOR: return doForStmt(rightp);
		case TRY: return doTryStmt(rightp);
		case DEL: return doDelStmt(rightp);
		case PRINT: return doPrintStmt(rightp);
		case CALL: return doCallStmt(rightp);
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
			return doSetOpStmt(rightp, kwtyp);
		case ZPAREN:
			isZpar = true;
			return true;
		default:
			if (celltyp == NodeCellTyp.ID) {
				return doIdStmt(rightp);
			}
			oerr(rightp, "Invalid keyword: " + kwtyp.toString() +
				" encountered at beginning of statement");
			return false;
		}
	}
	
	private boolean doSetStmt(int rightp) {
		return true;
	}
	
	private boolean doSetOpStmt(int rightp, KeywordTyp kwtyp) {
		return true;
	}
	
	private boolean doIfStmt(int rightp) {
		return true;
	}
	
	private boolean doWhileStmt(int rightp) {
		return true;
	}
	
	private boolean doForStmt(int rightp) {
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
	
	private boolean doCallStmt(int rightp) {
		return true;
	}
	
	private boolean doDotStmt(int rightp) {
		return true;
	}
	
	private boolean doIdStmt(int rightp) {
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
	
	/*
				currPhaseNo = getPhaseNo(kwtyp);
				phaseDesc = getPhaseDesc(currPhaseNo);
				if (currPhaseNo < phaseNo) {
					oerr(rightp, "Top-level " + phaseDesc + 
						" encountered unexpectedly");
					return -1;
				}
				isZparen = (kwtyp == KeywordTyp.ZPAREN);
				rightq = rightp;
				rightp = node.getRightp();
				if (rightp <= 0 && !isZparen) {
					oerr(rightq, "Null pointer encountered unexpectedly " +
						"after " + kwtyp);
					rightq = -1;
				}
				else if (rightp <= 0) {
					rightp = rightq;  // isZparen is true
				}
				else {
					isZparen = false;
				}
				// rightp > 0 inside following switch
				switch (currPhaseNo) {
				case 1:
					rightq = chkImportStmt(rightp, kwtyp);
					if (rightq == -1) {
						oerr(initp, "Keyword 'import' followed by " +
							"no module names or invalid text");
					}
					break;
				case 2:
					if (currPhaseNo == phaseNo) {
						oerr(rightp, "Multiple gdefun statements encountered");
						rightq = 0;
					}
					else {
						rightq = chkGlbDefStmt(rightp);
					}
					break;
				case 3:
					rightq = chkDefunStmt(rightp);
					break;
				case 4:
					rightq = chkClassStmt(rightp, kwtyp);
					break;
				default:
					if (currPhaseNo <= ABPHASE) {
						oerr(rightq, "Keyword 'abdefun' encountered at " +
							"top level unexpectedly");
						rightq = -1;
					}
				}
				if (isZparen) {
					rightp = node.getRightp();
				}
				if (rightq > 0) {
					rightp = rightq;
				}
				else {
					return -1;
				}
			
			if (node.isOpenPar()) {
				out("Here is ()");
			}
			rightp = node.getRightp();
	 */
}
