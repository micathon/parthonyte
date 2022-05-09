package synchk;

import iconst.KeywordTyp;
import iconst.NodeCellTyp;
import iconst.BifTyp;
import page.Node;
import page.Store;
import scansrc.ScanSrc;

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
	
	@SuppressWarnings("unused")
	private void omsg(String msg) {
		scan.omsg(msg);
	}
	
	@SuppressWarnings("unused")
	private void oerr(int nodep, String msg) {
		synChk.oerr(nodep, msg);
	}
	
	private void oerrd(int nodep, String msg, double bval) {
		synChk.oerrmod(nodep, msg, bval, 1);
	}
	
	public boolean doExpr(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		BifTyp biftyp;
		int downp;

		rightp = doParenExpr(rightp, true);
		switch (rightp) {
		case 0: 
			return true;
		case -1: 
			return false;
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
			return doLambdaOp(rightp);
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
				oerrd(rightp, "Error in parentheses: null keyword & identifier node",
					10.1);
				return false;
			}
			if (celltyp != NodeCellTyp.KWD) {
				oerrd(rightp, "Error in parentheses: null keyword, node cell-type not KWD",
					10.2);
				return false;
			}
			downp = node.getDownp();
			biftyp = scan.getInt2Bif(downp);
			if (biftyp != BifTyp.NULL) {
				return doBifTyp(biftyp, rightp);
			}
			oerrd(rightp, "Internal error: null keyword & null built-in func. node",
				10.3);
			return false;
		case ZPAREN:
		case ZSTMT:
			oerrd(rightp, "Error: ZPAREN/ZSTMT encountered in expression",
				10.4);
			return false;
		default:
			oerrd(rightp, "Invalid keyword: " + kwtyp +
				" encountered at beginning of expression", 10.5);
			return false;
		}
	}
	
	private int doParenExpr(int rightp, boolean isTopLevel) {
		Node node;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		boolean isValid;

		out("doParenExpr: top");
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
			case FLOAT: 
				isValid = doFloatConst(rightp);
				break;
			case STRING: 
				isValid = doStrLit(rightp);
				break;
			case NULL: 
				oerrd(rightp, "Invalid token encountered in expression",
					20.1);
				return -1;
			default:
				oerrd(rightp, "Invalid cell type: " + celltyp.toString() +
					" encountered in expression", 20.2);
				return -1;
			}
			if (isValid) {
				return 0;
			}
			out("doParenExpr: !isValid");
			return -1;
		}
		return parenExprRtn(rightp, node);
	}
		
	public int parenExprRtn(int rightp, Node node) {
		int rightq;
		KeywordTyp kwtyp = node.getKeywordTyp();
		
		out("parenExprRtn: top");
		if (!node.isOpenPar()) {
			oerrd(rightp, "Error: unexpected token while scanning for " +
				"parenthesized expression", 30.1);
			return -1;
		}
		if (kwtyp != KeywordTyp.ZPAREN) {
			oerrd(rightp, "Internal expression error: expecting ZPAREN, " +
				kwtyp + " found", 30.2);
			return -1;
		}
		rightq = rightp;
		rightp = node.getDownp();
		if (rightp <= 0) {
			oerrd(rightq, "Error in parenthesized expression: null pointer",
				30.3);
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
			oerrd(rightp, "Invalid keyword: " + kwtyp +
				" encountered in middle of expression", 40.1);
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
			oerrd(rightp, "Error: literal expected, parenthesis found",
				80.1);
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
		case FLOAT: 
			isValid = doFloatConst(rightp);
			break;
		case STRING: 
			isValid = doStrLit(rightp);
			break;
		case NULL: 
			oerrd(rightp, "Error: literal expected, NULL found",
				80.2);
			return false;
		default:
			oerrd(rightp, "Error: cell type: " + celltyp +
				" encountered, expecting literal", 80.3);
			return false;
		}
		if (!isValid) {
			oerrd(rightp, "Error parsing literal", 80.4);
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
	
	public boolean doUnaryOp(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		int rightq;
		int count;
		
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		rightq = node.getRightp();
		count = getExprCount(rightq);
		if (count < 0) { 
			oerrd(rightp, "Unary operator " + kwtyp +
				" has invalid argument(s)", 100.1);
			return false;
		}
		if (count != 1) {
			oerrd(rightp, "Unary operator " + kwtyp + 
				" has wrong no. of operands", 100.2);
			return false;
		}
		return true;
	}
	
	public int doOptArgOp(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		int rightq;
		int count;
		
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		rightq = node.getRightp();
		count = getExprCount(rightq);
		if (count < 0) { 
			oerrd(rightp, "Operator (having optional arg.) " + kwtyp +
				" has invalid operand", 110.1);
			return -1;
		}
		if (count > 1) {
			oerrd(rightp, "Operator (having optional arg.) " + kwtyp + 
				" has more than one operand", 110.2);
			return -2;
		}
		return 0;
	}
	
	public boolean doZeroOp(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		int rightq;
		
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		rightq = node.getRightp();
		if (rightq > 0) { 
			oerrd(rightp, "Keyword " + kwtyp + " followed by invalid text",
				120.1);
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
			oerrd(rightp, "MINUS operator has invalid argument(s)",
				130.1);
			return false;
		}
		if ((count != 1) && (count != 2)) {
			oerrd(rightp, "MINUS operator has wrong no. of operands",
				130.2);
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
			oerrd(rightp, "QUEST operator has invalid argument(s)",
				140.1);
			return false;
		}
		if (count != 3) {
			oerrd(rightp, "QUEST operator has wrong no. of operands",
				140.2);
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
			oerrd(rightp, "Multi operator " + kwtyp +
				" has invalid argument(s)", 150.1);
			return false;
		}
		if (count < 2) {
			oerrd(rightp, "Multi operator " + kwtyp + 
				" has wrong no. of operands", 150.2);
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
			oerrd(rightp, "Binary operator " + kwtyp +
				" has invalid argument(s)", 160.1);
			return false;
		}
		if (count != 2) {
			oerrd(rightp, "Binary operator " + kwtyp + 
				" has wrong no. of operands", 160.2);
			return false;
		}
		return true;
	}

	private boolean doListOp(int rightp) {
		boolean isOK;
		isOK = (doListOpRtn(rightp, true, "operator") >= 0);
		return isOK;
	}

	private boolean doQuoteOp(int rightp) {
		boolean isOK;
		isOK = (doListOpRtn(rightp, false, "operator") >= 0);
		return isOK;
	}

	public int doListOpRtn(int rightp, boolean isZero, String opstmt) {
		Node node;
		KeywordTyp kwtyp;
		int rightq;
		int count;
		
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		rightq = node.getRightp();
		count = getExprCount(rightq);
		if (count < 0) { 
			oerrd(rightp, "List " + opstmt + " " + kwtyp + " has invalid argument(s)",
				180.1);
			return -1;
		}
		if (isZero) {
			return 0;
		}
		if (count == 0) {
			oerrd(rightp, "List " + opstmt + " " + kwtyp + " has no arguments",
				180.2);
			return -2;
		}
		return 0;
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
			// no node after tuple, a parenthesized list
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
			oerrd(savep, "Invalid dict. expression", 200.1);
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
			oerrd(rightp, "Error in dict. pair: expecting DOT, " +
				kwtyp + " found", 210.1);
			return false;
		}
		rightp = node.getRightp();
		if (getExprCount(rightp) != 2) {
			oerrd(rightp, "Error in dict. pair: expression count not = 2",
				210.2);
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
			oerrd(rightp, msg + "invalid parenthesized arg. or non-identifier",
				220.1);
			return false;
		}
		if (!doSliceOp(rightp) && !doDotOp(rightp, true, false)) {
			oerrd(rightp, msg + "neither identifier, dot, or slice targets found",
				220.2);
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
			oerrd(savep, "Error in SLICE expr.: no args.",
				230.1);
			return false;
		}
		if (!doExpr(rightp)) {
			oerrd(savep, "Error in list-obj. arg. of SLICE expr.",
				230.2);
			return false;
		}
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, "Error in SLICE expr.: no idx. args.",
				230.3);
			return false;
		}
		if (!doExpr(rightp)) {
			oerrd(savep, "Error in idx. arg. of SLICE expr.",
				230.4);
			return false;
		}
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp <= 0) {
			return true;
		}
		if (!isKwdMatch(rightp, KeywordTyp.ALL) && !doExpr(rightp)) {
			oerrd(savep, "Error in 2nd idx. arg. of SLICE expr.",
				230.5);
			return false;
		}
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp > 0) {
			oerrd(savep, "Error in SLICE expr.: too many args.",
				230.6);
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
			else {
				isCurrIdent = false;
				rightq = parenExprRtn(rightp, node);
				if (rightq <= 0) {
					oerrd(savep, msg + "invalid parenthesized arg. or non-identifier",
						250.1);
					return false;
				}
				if (!doZcallOp(rightq)) {
					oerrd(savep, msg + "invalid function call", 250.2);
					return false;
				}
			}
			rightp = node.getRightp();
		}
		if (count < 2) {
			oerrd(savep, msg + "less than 2 args. encountered", 250.3);
			return false;
		}
		if (isAnyEnd) { }
		else if (isEndName && !isCurrIdent) {
			oerrd(savep, "Error in target expr.: " +
				"final arg. of DOT operator must be an identifier", 250.4);
			return false;
		}
		else if (!isEndName && isCurrIdent) {
			oerrd(savep, "Error in DOT stmt.: " +
				"final arg. of DOT operator must be a function call", 250.5);
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
					oerrd(savep, "Error in expr. arg of CALL expr.",
						260.1);
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
			oerrd(savep, "Error in CALL expr.: no args.", 260.2);
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
					oerrd(savep, "Expecting identifier in function call, " +
						"invalid text found", 270.1);
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
			oerrd(savep, "Invalid expression found in function call",
				280.1);
			return false;
		}
		if (!isValidExpr) {
			oerrd(savep, "Error in keyword arg.: " + msg,
				280.2);
			return false;
		}
		if (!isValidKwdArg && found) {
			oerrd(savep, 
				"Error in function call: keyword arg. followed by normal arg.",
				280.3);
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
			oerrd(savep, "VENUM expr has no arg(s)", 300.1);
			return false;
		}
		isValid = (synChk.chkEnumStmt(rightp, true) > 0);
		return isValid;
	}
	
	private boolean doLambdaOp(int rightp) {
		Node node;
		Node subNode;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		boolean first = true;
		boolean isTuple = false;
		int savep = rightp;
		int rightq;
		
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, "LAMBDA expr. has no args.", 310.1);
			return false;
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.ZPAREN) {
			oerrd(savep, "LAMBDA expr. has no id list", 310.2);
			return false;
		}
		rightq = node.getDownp();
		while (rightq > 0) {
			if (isTuple) {
				oerrd(savep, "LAMBDA expr. has non-empty TUPLE expr. " +
					"instead of id list", 310.3);
				return false;
			}
			subNode = store.getNode(rightq);
			kwtyp = subNode.getKeywordTyp();
			celltyp = subNode.getDownCellTyp();
			if (first && (kwtyp == KeywordTyp.TUPLE)) {
				isTuple = true;
			}
			else if ((celltyp != NodeCellTyp.FUNC) && (celltyp != NodeCellTyp.ID)) {
				oerrd(savep, "LAMBDA expr. has invalid id list",
					310.4);
				return false;
			}
			first = false;
			rightq = subNode.getRightp();
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, "LAMBDA expr. has no expr. arg. or DO block",
				310.5);
			return false;
		}
		node = store.getNode(rightp);
		rightq = node.getRightp();
		kwtyp = node.getKeywordTyp();
		if (kwtyp == KeywordTyp.DO) { }
		else if (!doExpr(rightp)) {
			oerrd(savep, "LAMBDA expr. has invalid expr. arg.", 310.6);
			return false;
		}
		else if (rightq > 0) {
			oerrd(savep, "LAMBDA expr. has invalid text after expr. arg.",
				310.7);
			return false;
		}
		else {
			return true;
		}
		if (synChk.chkDoBlock(rightp) < 0) {
			oerrd(savep, "Error in LAMBDA expr. DO block", 310.8);
			return false;
		}
		return true;
	}
	
	private boolean doCastOp(int rightp) {
		Node node;
		int savep = rightp;
		
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, "CAST expr. has no args.", 320.1);
			return false;
		}
		node = store.getNode(rightp);
		if (!doLiteralExpr(rightp, true)) {
			oerrd(savep, "CAST expr. has invalid literal", 320.2);
			return false;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, "CAST expr. has no expr. arg.", 320.3);
			return false;
		}
		out("doCastOp: 1st rightp = " + rightp);
		if (!doExpr(rightp)) {
			oerrd(savep, "CAST expr. has invalid expr. arg.", 320.4);
			return false;
		}
		node = store.getNode(rightp);
		rightp = node.getRightp();
		out("doCastOp: 2nd rightp = " + rightp);
		if (rightp > 0) {
			oerrd(savep, "CAST expr. has too many args.", 320.5);
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
			oerrd(rightp, "Binary built-in function " + biftyp +
				" has invalid argument(s)", 340.1);
			return false;
		}
		if (count != 2) {
			oerrd(rightp, "Binary built-in function " + biftyp + 
				" has wrong no. of arguments", 340.2);
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
			oerrd(rightp, "Unary built-in function " + biftyp +
				" has invalid argument(s)", 350.1);
			return false;
		}
		if (count != 1) {
			oerrd(rightp, "Unary built-in function " + biftyp + 
				" has wrong no. of arguments", 350.2);
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
			oerrd(savep, "CRPATH function has no arguments", 360.1);
			return false;
		}
		node = store.getNode(rightp);
		downp = node.getDownp();
		if (downp < 0) {
			oerrd(savep, "CRPATH function has negative depth", 360.2);
			return false;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, "CRPATH function has no bit-string argument",
				360.3);
			return false;
		}
		node = store.getNode(rightp);
		downp = node.getDownp();
		if (downp < 0) {
			oerrd(savep, "CRPATH function has negative bit-string",
				360.4);
			return false;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(savep, "CRPATH function has no expression argument",
				360.5);
			return false;
		}
		if (!doExpr(rightp)) {
			oerrd(savep, "CRPATH function has invalid expression argument",
				360.6);
			return false;
		}
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp > 0) {
			oerrd(savep, "CRPATH function has too many arguments",
				360.7);
			return false;
		}
		return true;
	}
	
}
