package synchk;

import iconst.KeywordTyp;
import iconst.NodeCellTyp;
import iconst.BifTyp;
import page.Node;
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
		Node node;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		BifTyp biftyp;
		int downp;

		rightp = doParenExpr(rightp, true);
		switch (rightp) {
		case 0: return true;
		case -1: return false;
		}
		node = store.getNode(rightp);
		celltyp = node.getDownCellTyp();
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
		case VENUM:
			return doVenumOp(rightp);
		case LAMBDA:
			return true;  // not yet implemented
		case CAST:
			return doCastOp(rightp);
		case SLICE:
			return doSliceOp(rightp);
		case DOT:
			return doDotOp(rightp, false, true);
		case ZCALL:
			return doZcallOp(rightp);
		case CALL:
			return doCallOp(rightp);
		case NULL:
			if (celltyp == NodeCellTyp.ID) {
				oerr(rightp, "Error in parentheses: null keyword & identifier node");
				return false;
			}
			if (celltyp != NodeCellTyp.KWD) {
				oerr(rightp, "Error in parentheses: null keyword, node cell-type not KWD");
				return false;
			}
			downp = node.getDownp();
			biftyp = scan.getInt2Bif(downp);
			if (biftyp != BifTyp.NULL) {
				return doBifTyp(biftyp, rightp);
			}
			oerr(rightp, "Internal error: null keyword & null built-in func. node");
			return false;
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
		Node node;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		boolean isValid;

		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		celltyp = node.getDownCellTyp();
		out("rightp = " + rightp + 
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
		return parenExprRtn(rightp, node);
	}
		
	public int parenExprRtn(int rightp, Node node) {
		int rightq;
		KeywordTyp kwtyp = node.getKeywordTyp();
		
		if (!node.isOpenPar()) {
			oerr(rightp, "Error: unexpected token while scanning for " +
				"parenthesized expression");
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
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		out("rightp = " + rightp + ", kwd = " + kwtyp);
		out("Paren Expr kwd = " + kwtyp);
		return rightp;
	}
	
	private boolean doKwdConst(int rightp) {
		Node node;
		KeywordTyp kwtyp;

		node = store.getNode(rightp);
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
		out("doIntConst: rightp = " + rightp);
		return true;
	}
	
	private boolean doFloatConst(int rightp) {
		return true;
	}
	
	private boolean doStrLit(int rightp) {
		return true;
	}
	
	public boolean doLiteralExpr(int rightp, boolean isIdentifier) {
		Node node;
		NodeCellTyp celltyp;
		boolean isValid;

		node = store.getNode(rightp);
		celltyp = node.getDownCellTyp();
		if (node.isOpenPar()) {
			oerr(rightp, "Error: literal expected, parenthesis found");
			return false;
		}
		switch (celltyp) {
		case ID: 
			isValid = isIdentifier && doIdentifier(rightp);
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
			oerr(rightp, "Error: literal expected, NULL found");
			return false;
		default:
			oerr(rightp, "Error: cell type: " + celltyp +
				" encountered, expecting literal");
			return false;
		}
		if (!isValid) {
			oerr(rightp, "Error parsing literal");
		}
		return isValid;
	}
		
	public int getExprCount(int rightp) {
		Node node;
		int count = 0;
		boolean isValid = true;
		
		while (rightp > 0) {
			count++;
			if (!doExpr(rightp)) {
				isValid = false;
			}
			node = store.getNode(rightp);
			rightp = node.getRightp();
		}
		if (!isValid) {
			return -1;
		}
		return count;
	}
	
	private boolean doUnaryOp(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		int rightq;
		int count;
		
		node = store.getNode(rightp);
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
		Node node;
		int rightq;
		int count;
		
		node = store.getNode(rightp);
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
		Node node;
		int rightq;
		int count;
		
		node = store.getNode(rightp);
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
		Node node;
		KeywordTyp kwtyp;
		int rightq;
		int count;
		
		node = store.getNode(rightp);
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
		Node node;
		KeywordTyp kwtyp;
		int rightq;
		int count;
		
		node = store.getNode(rightp);
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
		Node node;
		KeywordTyp kwtyp;
		int rightq;
		int count;
		
		node = store.getNode(rightp);
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
	
	public int chkTuple(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		int rtnval;

		if (rightp <= 0) {
			return -1;
		}
		node = store.getNode(rightp);
		if (!node.isOpenPar()) {
			return -1;
		}
		rtnval = node.getRightp();
		rightp = node.getDownp();
		if (rightp <= 0) {
			return -1;
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.TUPLE) {
			return -1;
		}
		if (!doListOp(rightp)) {
			return -1;
		}
		if (rtnval < 0) {
			return 0;
		}
		return rtnval;
	}
	
	private boolean doDictOp(int rightp) {
		Node node;
		int savep = rightp;
		boolean isValid = true;
		
		while (true) {
			node = store.getNode(rightp);
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
		Node node;
		KeywordTyp kwtyp;

		out("DictPair top: rightp = " + rightp);
		rightp = doParenExpr(rightp, false);
		if (rightp <= 0) {
			return false;
		}
		node = store.getNode(rightp);
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
	
	public boolean doTargetExpr(int rightp) {
		Node node;
		NodeCellTyp celltyp;
		String msg = "Error in target expr.: ";

		node = store.getNode(rightp);
		celltyp = node.getDownCellTyp();
		if (celltyp == NodeCellTyp.ID) {
			return true;
		}
		rightp = parenExprRtn(rightp, node);
		if (rightp <= 0) {
			oerr(rightp, msg + "invalid parenthesized arg. or non-identifier");
			return false;
		}
		if (!doSliceOp(rightp) && !doDotOp(rightp, true, false)) {
			oerr(rightp, msg + "neither identifier, dot, or slice targets found");
			return false;
		}
		return true;
	}

	private boolean doSliceOp(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		int savep = rightp;

		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.SLICE) {
			return false;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerr(savep, "Error in SLICE expr.: no args.");
			return false;
		}
		if (!doExpr(rightp)) {
			oerr(savep, "Error in list-obj. arg. of SLICE expr.");
			return false;
		}
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerr(savep, "Error in SLICE expr.: no idx. args.");
			return false;
		}
		if (!doExpr(rightp)) {
			oerr(savep, "Error in idx. arg. of SLICE expr.");
			return false;
		}
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp <= 0) {
			return true;
		}
		if (!isKwdMatch(rightp, KeywordTyp.ALL) && !doExpr(rightp)) {
			oerr(savep, "Error in 2nd idx. arg. of SLICE expr.");
			return false;
		}
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp > 0) {
			oerr(savep, "Error in SLICE expr.: too many args.");
			return false;
		}
		return true;
	}
	
	public boolean isKwdMatch(int rightp, KeywordTyp kwtyp) {
		Node node;
		NodeCellTyp celltyp;

		node = store.getNode(rightp);
		celltyp = node.getDownCellTyp();
		if (celltyp != NodeCellTyp.KWD) {
			return false;
		}
		return (kwtyp == node.getKeywordTyp());
	}
	
	public boolean doDotOp(int rightp, boolean isEndName, boolean isAnyEnd) {
		Node node;
		NodeCellTyp celltyp;
		KeywordTyp kwtyp;
		int savep = rightp;
		int rightq;
		String msg = "Error in DOT expr.: ";
		boolean isCurrIdent = false;
		int count = 0;
		
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.DOT) {
			return false;
		}
		rightp = node.getRightp();
		while (rightp > 0) {
			count++;
			node = store.getNode(rightp);
			celltyp = node.getDownCellTyp();
			if (celltyp == NodeCellTyp.ID) {
				isCurrIdent = true;
			}
			else if (count == 1) {
				oerr(savep, msg + "expecting identifier after DOT, invalid text found");
				return false;
			}
			else {
				isCurrIdent = false;
				rightq = parenExprRtn(rightp, node);
				if (rightq <= 0) {
					oerr(savep, msg + "invalid parenthesized arg. or non-identifier");
					return false;
				}
				if (!doZcallOp(rightq)) {
					oerr(savep, msg + "invalid function call");
					return false;
				}
			}
			rightp = node.getRightp();
		}
		if (count < 2) {
			oerr(savep, msg + "less than 2 args. encountered");
			return false;
		}
		if (isAnyEnd) { }
		else if (isEndName && !isCurrIdent) {
			oerr(savep, "Error in target expr.: " +
				"final arg. of DOT operator must be an identifier");
			return false;
		}
		else if (!isEndName && isCurrIdent) {
			oerr(savep, "Error in DOT stmt.: " +
				"final arg. of DOT operator must be a function call");
			return false;
		}
		return true;
	}

	public boolean doCallOp(int rightp) {
		Node node;
		int savep = rightp;
		String msg;
		boolean first = true;
		boolean found = false;
		boolean isValidExpr = true;
		boolean isValidKwdArg;
		
		while (true) {
			node = store.getNode(rightp);
			rightp = node.getRightp();
			if (rightp <= 0) {
				break;
			}
			if (first) {
				if (!doExpr(rightp)) {
					oerr(savep, "Error in expr. arg of CALL expr.");
					return false;
				}
				node = store.getNode(rightp);
				rightp = node.getRightp();
				if (rightp <= 0) {
					return true;
				}
			}
			first = false;
			msg = doKeywordArg(rightp);
			isValidKwdArg = (msg == "");
			out("doKwdArg : " + msg);
			if (!isValidKwdArg) {
				isValidExpr = doExpr(rightp);
			}
			if (!chkZcallFlags(savep, msg, isValidExpr, isValidKwdArg, found)) {
				return false;
			}
			if (isValidKwdArg) {
				found = true;
			}
		}
		if (first) {
			oerr(savep, "Error in CALL expr.: no args.");
			return false;
		}
		return true;
	}
	
	public boolean doZcallOp(int rightp) {
		Node node;
		NodeCellTyp celltyp;
		int savep = rightp;
		String msg;
		boolean first = true;
		boolean found = false;
		boolean isValidExpr = true;
		boolean isValidKwdArg;
		
		while (true) {
			node = store.getNode(rightp);
			if (first) {
				celltyp = node.getDownCellTyp();
				if ((celltyp != NodeCellTyp.FUNC) && 
					(celltyp != NodeCellTyp.ID)) 
				{
					oerr(savep, "Expecting identifier in function call, " +
						"invalid text found");
					return false;
				}
			}
			first = false;
			rightp = node.getRightp();
			if (rightp <= 0) {
				break;
			}
			msg = doKeywordArg(rightp);
			isValidKwdArg = (msg == "");
			out("doKwdArg : " + msg);
			if (!isValidKwdArg) {
				isValidExpr = doExpr(rightp);
			}
			if (!chkZcallFlags(savep, msg, isValidExpr, isValidKwdArg, found)) {
				return false;
			}
			if (isValidKwdArg) {
				found = true;
			}
		} 
		return true;
	}
	
	private boolean chkZcallFlags(int savep, String msg, 
		boolean isValidExpr, boolean isValidKwdArg, boolean found) 
	{
		if (!isValidExpr && !found) {
			oerr(savep, "Invalid expression found in function call");
			return false;
		}
		if (!isValidExpr) {
			oerr(savep, "Error in keyword arg.: " + msg);
			return false;
		}
		if (!isValidKwdArg && found) {
			oerr(savep, "Error in function call: keyword arg. followed by normal arg.");
			return false;
		}
		return true;
	}
	
	private String doKeywordArg(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;

		out("doKeywordArg top: rightp = " + rightp);
		rightp = doParenExpr(rightp, true);
		if (rightp <= 0) {
			return "invalid or not found parenthesized expr.";
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.SET) {
			return "missing SET keyword";
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			return "no args. found after SET";
		}
		node = store.getNode(rightp);
		celltyp = node.getDownCellTyp();
		if (celltyp != NodeCellTyp.ID) {
			return "missing identifier";
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			return "missing expression value";
		}
		if (!doExpr(rightp)) {
			return "invalid expression value";
		}
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp > 0) {
			return "expression value followed by invalid text";
		}
		out("doKeywordArg OK");
		return "";
	}

	private boolean doVenumOp(int rightp) {
		Node node;
		int savep = rightp;
		boolean isValid = true;
		
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerr(savep, "VENUM expr has no arg(s)");
			return false;
		}
		isValid = (synChk.chkEnumStmt(rightp, true) > 0);
		return isValid;
	}
	
	private boolean doCastOp(int rightp) {
		Node node;
		int savep = rightp;
		
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerr(savep, "CAST expr. has no args.");
			return false;
		}
		node = store.getNode(rightp);
		if (!doLiteralExpr(rightp, true)) {
			oerr(savep, "CAST expr. has invalid literal");
			return false;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerr(savep, "CAST expr. has no expr. arg.");
			return false;
		}
		out("doCastOp: 1st rightp = " + rightp);
		if (!doExpr(rightp)) {
			oerr(savep, "CAST expr. has invalid expr. arg.");
			return false;
		}
		node = store.getNode(rightp);
		rightp = node.getRightp();
		out("doCastOp: 2nd rightp = " + rightp);
		if (rightp > 0) {
			oerr(savep, "CAST expr. has too many args.");
			return false;
		}
		return true;
	}
	
	private boolean doBifTyp(BifTyp biftyp, int rightp) {
		switch (biftyp) {
		case CRPATH:
			return doCrPath(rightp);
		case CAR:
		case CDR:
			return doUnaryBif(biftyp, rightp);
		case RPLACA:
		case RPLACD:
			return doBinaryBif(biftyp, rightp);
		default:
			return false;
		}
	}
	
	private boolean doBinaryBif(BifTyp biftyp, int rightp) {
		Node node;
		int rightq;
		int count;
		
		node = store.getNode(rightp);
		rightq = node.getRightp();
		count = getExprCount(rightq);
		if (count < 0) { 
			oerr(rightp, "Binary built-in function " + biftyp +
				" has invalid argument(s)");
			return false;
		}
		if (count != 2) {
			oerr(rightp, "Binary built-in function " + biftyp + 
				" has wrong no. of arguments");
			return false;
		}
		return true;
	}
	
	private boolean doUnaryBif(BifTyp biftyp, int rightp) {
		Node node;
		int rightq;
		int count;
		
		node = store.getNode(rightp);
		rightq = node.getRightp();
		count = getExprCount(rightq);
		if (count < 0) { 
			oerr(rightp, "Unary built-in function " + biftyp +
				" has invalid argument(s)");
			return false;
		}
		if (count != 1) {
			oerr(rightp, "Unary built-in function " + biftyp + 
				" has wrong no. of arguments");
			return false;
		}
		return true;
	}
	
	private boolean doCrPath(int rightp) {
		// (crpath n b expr): n = depth, b = bits
		Node node;
		int savep = rightp;
		int downp;
		
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerr(savep, "CRPATH function has no arguments");
			return false;
		}
		node = store.getNode(rightp);
		downp = node.getDownp();
		if (downp < 0) {
			oerr(savep, "CRPATH function has negative depth");
			return false;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerr(savep, "CRPATH function has no bit-string argument");
			return false;
		}
		node = store.getNode(rightp);
		downp = node.getDownp();
		if (downp < 0) {
			oerr(savep, "CRPATH function has negative bit-string");
			return false;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerr(savep, "CRPATH function has no expression argument");
			return false;
		}
		if (!doExpr(rightp)) {
			oerr(savep, "CRPATH function has invalid expression argument");
			return false;
		}
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp > 0) {
			oerr(savep, "CRPATH function has too many arguments");
			return false;
		}
		return true;
	}
	
}
