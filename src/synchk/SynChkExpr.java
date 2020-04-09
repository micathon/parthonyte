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
		if (kwtyp != KeywordTyp.ZPAREN) {
			oerr(rightp, "Internal expression error: expecting ZPAREN, " +
				kwtyp + " found");
			return false;
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
		case NOT:
		case NOTBITZ:
			return doUnaryOp(rightp);
		case MINUS:
			return doMinusOp(rightp);
		case QUEST:
			return doQuestOp(rightp);
		case MPY:
		case ADD:
		case STRDO:
		case STRCAT:
		case AND:
		case OR:
		case XOR:
		case ANDBITZ:
		case ORBITZ:
		case XORBITZ:
			return doMultiOp(rightp);
		case DIV:
		case IDIV:
		case MOD:
		case SHL:
		case SHR:
		case SHRU:
		case GE:
		case LE:
		case GT:
		case LT:
		case EQ:
		case NE:
		case IS:
		case IN:
			return doBinaryOp(rightp);
		case TUPLE:
			return doTuple(rightp);
		case ZPAREN:
		case ZSTMT:
			oerr(rightp, "Error: ZPAREN/ZSTMT encountered in expression");
			return false;
		default:
			oerr(rightp, "Invalid keyword: " + kwtyp +
				" encountered at beginning of expression");
			return false;
		}
	}
	
	private boolean doKwdConst(int rightp) {
		Page page;
		int idx;
		Node node;
		KeywordTyp kwtyp;

		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		switch (kwtyp) {
		case TRUE:
		case FALSE:
		case NULL:
			return true;
		default:
			oerr(rightp, "Invalid keyword: " + kwtyp +
				" encountered in middle of expression");
			return false;
		}
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
	
	public int getExprCount(int rightp) {
		Page page;
		int idx;
		Node node;
		int count = 0;
		boolean isValid = true;
		
		while (rightp > 0) {
			count++;
			if (!doExpr(rightp)) {
				isValid = false;
			}
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			rightp = node.getRightp();
		}
		if (!isValid) {
			return -1;
		}
		return count;
	}
	
	private boolean doUnaryOp(int rightp) {
		Page page;
		int idx;
		Node node;
		KeywordTyp kwtyp;
		int count;
		
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		count = getExprCount(rightp);
		if (count < 0) { 
			oerr(rightp, "Unary operator " + kwtyp +
				" has invalid argument(s)");
			return false;
		}
		if (count != 1) {
			oerr(rightp, "Unary operator " + kwtyp + 
				" has wrong no. of operands");
			return false;
		}
		return true;
	}
	
	private boolean doMinusOp(int rightp) {
		int count;
		
		count = getExprCount(rightp);
		if (count < 0) { 
			oerr(rightp, "MINUS operator has invalid argument(s)");
			return false;
		}
		if ((count != 1) && (count != 2)) {
			oerr(rightp, "MINUS operator has wrong no. of operands");
			return false;
		}
		return true;
	}
	
	private boolean doQuestOp(int rightp) {
		int count;
		
		count = getExprCount(rightp);
		if (count < 0) { 
			oerr(rightp, "QUEST operator has invalid argument(s)");
			return false;
		}
		if (count != 3) {
			oerr(rightp, "QUEST operator has wrong no. of operands");
			return false;
		}
		return true;
	}
	
	private boolean doMultiOp(int rightp) {
		Page page;
		int idx;
		Node node;
		KeywordTyp kwtyp;
		int count;
		
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		count = getExprCount(rightp);
		if (count < 0) { 
			oerr(rightp, "Multi operator " + kwtyp +
				" has invalid argument(s)");
			return false;
		}
		if (count < 2) {
			oerr(rightp, "Multi operator " + kwtyp + 
				" has wrong no. of operands");
			return false;
		}
		return true;
	}
	
	private boolean doBinaryOp(int rightp) {
		Page page;
		int idx;
		Node node;
		KeywordTyp kwtyp;
		int count;
		
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		count = getExprCount(rightp);
		if (count < 0) { 
			oerr(rightp, "Binary operator " + kwtyp +
				" has invalid argument(s)");
			return false;
		}
		if (count != 2) {
			oerr(rightp, "Binary operator " + kwtyp + 
				" has wrong no. of operands");
			return false;
		}
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
	
	private boolean dox(int rightp) {
		return true;
	}
	
	private boolean dox(int rightp) {
		return true;
	}
*/	
	private boolean doTuple(int rightp) {
		out("TUPLE keyword encountered at beginning of expression");
		return true;
	}
	
}
