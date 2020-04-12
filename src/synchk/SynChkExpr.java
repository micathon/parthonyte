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

		rightp = doParenExpr(rightp, true);
		switch (rightp) {
		case 0: return true;
		case -1: return false;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
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
		case JIST:
		case TUPLE:
			return doListOp(rightp);
		case QUOTE:
			return doQuoteOp(rightp);
		case DICT:
			return doDictOp(rightp);
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
	
	private int doParenExpr(int rightp, boolean isTopLevel) {
		Page page;
		int idx;
		Node node;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		boolean isValid;
		int rightq;

		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		celltyp = node.getDownCellTyp();
		out("rightp = " + rightp + ", idx = " + idx + 
				", kwd = " + kwtyp + ", celtyp = " + celltyp +
				", top level = " + isTopLevel);
		out("Expression kwd = " + kwtyp);
		out("Expression celltyp = " + celltyp);
		if (isTopLevel && !node.isOpenPar()) {
			switch (celltyp) {
			case KWD: 
				isValid = doKwdConst(rightp);
				break;
			case ID: 
				isValid = doIdentifier(rightp);
				break;
			case INT:
			case LONG:
				isValid = doIntConst(rightp);
				break;
			case DOUBLE: 
				isValid = doFloatConst(rightp);
				break;
			case STRING: 
				isValid = doStrLit(rightp);
				break;
			case NULL: 
				oerr(rightp, "Invalid token encountered in expression");
				return -1;
			default:
				oerr(rightp, "Invalid cell type: " + celltyp.toString() +
					" encountered in expression");
				return -1;
			}
			if (isValid) {
				return 0;
			}
			return -1;
		}
		if (!node.isOpenPar()) {
			oerr(rightp, "Error in parenthesized expression: isOpenPar failure");
			return -1;
		}
		if (kwtyp != KeywordTyp.ZPAREN) {
			oerr(rightp, "Internal expression error: expecting ZPAREN, " +
				kwtyp + " found");
			return -1;
		}
		rightq = rightp;
		rightp = node.getDownp();
		if (rightp <= 0) {
			oerr(rightq, "Error in parenthesized expression: null pointer");
			return -1;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		out("rightp = " + rightp + ", idx = " + idx + 
				", kwd = " + kwtyp);
		out("Paren Expr kwd = " + kwtyp);
		return rightp;
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
		int rightq;
		int count;
		
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		rightq = node.getRightp();
		count = getExprCount(rightq);
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
		Page page;
		int idx;
		Node node;
		int rightq;
		int count;
		
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		rightq = node.getRightp();
		out("MinusOp: rightp, q = " + rightp + ", " + rightq);
		count = getExprCount(rightq);
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
		Page page;
		int idx;
		Node node;
		int rightq;
		int count;
		
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		rightq = node.getRightp();
		count = getExprCount(rightq);
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
		int rightq;
		int count;
		
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		rightq = node.getRightp();
		count = getExprCount(rightq);
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
		int rightq;
		int count;
		
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		rightq = node.getRightp();
		count = getExprCount(rightq);
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

	private boolean doListOp(int rightp) {
		return doListOpRtn(rightp, true);
	}

	private boolean doQuoteOp(int rightp) {
		return doListOpRtn(rightp, false);
	}

	private boolean doListOpRtn(int rightp, boolean isZero) {
		Page page;
		int idx;
		Node node;
		KeywordTyp kwtyp;
		int rightq;
		int count;
		
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		rightq = node.getRightp();
		count = getExprCount(rightq);
		if (count < 0) { 
			oerr(rightp, "List operator " + kwtyp +
				" has invalid argument(s)");
			return false;
		}
		if (isZero) {
			return true;
		}
		if (count == 0) {
			oerr(rightp, "List operator " + kwtyp +
				" has no arguments");
			return false;
		}
		return true;
	}
	
	private boolean doDictOp(int rightp) {
		Page page;
		int idx;
		Node node;
		int savep = rightp;
		boolean isValid = true;
		
		while (true) {
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			rightp = node.getRightp();
			if (rightp <= 0) {
				break;
			}
			if (!doDictPair(rightp)) {
				isValid = false;
			}
		} 
		if (!isValid) {
			oerr(savep, "Invalid dict. expression");
		}
		return isValid;
	}

	private boolean doDictPair(int rightp) {
		Page page;
		int idx;
		Node node;
		KeywordTyp kwtyp;

		out("DictPair top: rightp = " + rightp);
		rightp = doParenExpr(rightp, false);
		if (rightp <= 0) {
			return false;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.DOT) {
			oerr(rightp, "Error in dict. pair: expecting DOT, " +
				kwtyp + " found");
			return false;
		}
		rightp = node.getRightp();
		if (getExprCount(rightp) != 2) {
			oerr(rightp, "Error in dict. pair: expression count not = 2");
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
*/	
	
}
