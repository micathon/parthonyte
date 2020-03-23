package synchk;

import iconst.KeywordTyp;
import iconst.NodeCellTyp;
import page.Node;
import page.Page;
import page.Store;
import scansrc.ScanSrc;
import synchk.SynChk;
import synchk.SynChkStmt;

public class SynChkExpr {

	private ScanSrc scan;
	private Store store;
	private SynChk synChk;
	@SuppressWarnings("unused")
	private SynChkStmt synStmt;
	
	public SynChkExpr(SynChk synChk, ScanSrc scan, Store store) {
		this.synChk = synChk;
		this.scan = scan;
		this.store = store;
	}
	
	public void init() {
		this.synStmt = synChk.synStmt;
	}

	private void out(String msg) {
		scan.out(msg);
	}
	
	private void oerr(int nodep, String msg) {
		synChk.oerr(nodep, msg);
	}
	
	public boolean doExpr(int rightp) {
		Page page;
		int idx;
		Node node;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int rightq;

		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		celltyp = node.getDownCellTyp();
		out("rightp = " + rightp + ", idx = " + idx + 
				", kwd = " + kwtyp + ", celtyp = " + celltyp);
		out("Expression kwd = " + kwtyp);
		out("Expression celltyp = " + celltyp);
		if (!node.isOpenPar()) {
			switch (celltyp) {
			case KWD: return doKwdConst(rightp);
			case ID: return doIdentifier(rightp);
			case INT:
			case LONG:
				return doIntConst(rightp);
			case DOUBLE: return doFloatConst(rightp);
			case STRING: return doStrLit(rightp);
			case NULL: 
				oerr(rightp, "Invalid token encountered in expression");
				return false;
			default:
				oerr(rightp, "Invalid cell type: " + celltyp.toString() +
					" encountered in expression");
				return false;
			}
		}
		rightq = rightp;
		rightp = node.getDownp();
		if (rightp <= 0) {
			oerr(rightq, "Error in parenthesized expression: null pointer");
			return false;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		celltyp = node.getDownCellTyp();
		out("rightp = " + rightp + ", idx = " + idx + 
				", kwd = " + kwtyp + ", celtyp = " + celltyp);
		out("Expression kwd = " + kwtyp);
		out("Expression celltyp = " + celltyp);
		switch (kwtyp) {
		case MINUS:
		case NOTBITZ:
		case NOT:
			return doUnaryOp(rightp);
		case ZPAREN:
			oerr(rightp, "Error: ZPAREN encountered in expression");
			return false;
		default:
			oerr(rightp, "Invalid keyword: " + kwtyp.toString() +
				" encountered at beginning of expression");
			return false;
		}
	}
	
	private boolean doKwdConst(int rightp) {
		return true;
	}
	
	private boolean doIdentifier(int rightp) {
		return true;
	}
	
	private boolean doIntConst(int rightp) {
		return true;
	}
	
	private boolean doFloatConst(int rightp) {
		return true;
	}
	
	private boolean doStrLit(int rightp) {
		return true;
	}
	
	private boolean doUnaryOp(int rightp) {
		return true;
	}
/*	
	private boolean dox(int rightp) {
		return true;
	}
	
	private boolean dox(int rightp) {
		return true;
	}
	
	private boolean dox(int rightp) {
		return true;
	}
	
	private boolean dox(int rightp) {
		return true;
	}
	
	private boolean dox(int rightp) {
		return true;
	}
	
	private boolean dox(int rightp) {
		return true;
	}
*/
	
}
