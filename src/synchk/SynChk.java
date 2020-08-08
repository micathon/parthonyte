package synchk;

import iconst.KeywordTyp;
import iconst.NodeCellTyp;
import page.Node;
import page.Page;
import page.Store;
import scansrc.ScanSrc;

public class SynChk {
	
	public SynChkStmt synStmt;
	public SynChkExpr synExpr;
	private ScanSrc scan;
	private Store store;
	public boolean isUnitTest; 
	public boolean isUnitTestFail; 
	public boolean isBrkZeroFail;  
	public boolean isBrkFound;     
	public int unitTestIdx;    
	public double brkval;
	public int moduleval;
	private static final int ABPHASE = 100;

	public SynChk(ScanSrc scan, Store store) {
		this.scan = scan;
		this.store = store;
		this.synStmt = new SynChkStmt(this, scan, store);
		this.synExpr = new SynChkExpr(this, scan, store);
		synStmt.init();
		synExpr.init();
	}
	
	public void out(String msg) {
		scan.out(msg);
	}
	
	public void oprn(String msg) {
		scan.omsg(msg);
	}
	
	public void oerr(int nodep, String msg) {
		oerrmod(nodep, msg, 0.0, 3);
	}
	
	public void oerrd(int nodep, String msg, double currbrk) {
		oerrmod(nodep, msg, currbrk, 3);
	}
	
	public void oerrmod(int nodep, String msg, double currbrk, int modno) {
		int lineno;
		String preLineNoStr;
		
		lineno = store.lookupLineNo(nodep);
		//oprn("oerrd: lineno = " + lineno);
		if (!isUnitTest) { }
		else if (brkval == 0.0) {
			isBrkZeroFail = true;
		}
		else if (isFloatEq(brkval, currbrk) && (lineno > 0) && 
			(modno == moduleval)) 
		{
			oprn("oerrd: brkval found = " + brkval + " | " + msg);
			isBrkFound = true;
			return;
		}
		else {
			//oprn("oerrd: brkval tried = " + brkval + " | " + msg);
			return;
		}
		if (lineno > 0) {
			preLineNoStr = "Line " + lineno + " - ";
			msg = preLineNoStr + msg;
		}
		scan.omsg("Syntax Error:");
		scan.omsg(msg);
	}
	
	public void initUnitTestFlags() {
		isBrkFound = false;
		isBrkZeroFail = false;
		isUnitTestFail = false;
		brkval = 0.0;
		unitTestIdx = 0;
	}
	
	public void endBlkUnitTest() {
		if (!isBrkFound && (brkval != 0.0)) {
			scan.omsg("Break not found: " + brkval);
		}
		else {
			isBrkFound = true;
		}
		isUnitTestFail = isUnitTestFail || isBrkZeroFail || !isBrkFound;
		isBrkFound = false;
		isBrkZeroFail = false;
		brkval = 0.0;
		++unitTestIdx;
		scan.omsg("Break " + unitTestIdx);
		scan.omsg("");
	}
	
	public boolean showUnitTestVal() {
		if (isUnitTestFail) {
			scan.omsg("Unit test failed!");
		}
		else {
			scan.omsg("Unit test passed OK");
		}
		scan.omsg("");
		return isUnitTestFail;
	}
	
	public int getNodeCount() {
		int tokenCount = 0;
		int listCount = 0;
		int count;
		int rightp, downp;
		Node node;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;

		rightp = scan.getRootNodep();
		while (rightp != 0) {
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			celltyp = node.getDownCellTyp();
			out("rightp = " + rightp +
				", kwd = " + kwtyp + ", celtyp = " + celltyp);
			if (kwtyp == KeywordTyp.DO) {
				out("Here is open par!");
				listCount++;
				downp = node.getDownp();
				out("root downp = " + downp + ", rightp = " +
					node.getRightp());
				count = doListCounts(downp, true);
				tokenCount += count & 0xFFFF;
				listCount += count >>> 16;
				out("Here is close par!");
			}
			rightp = node.getRightp();
			tokenCount++;
		}
		return tokenCount + (listCount << 16);
	}

	private int doListCounts(int rightp, boolean isTopLevel) {
		int tokenCount = 0;
		int listCount = 0;
		int count;
		Node node;
		int downp;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;

		while (rightp != 0) {
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			celltyp = node.getDownCellTyp();
			out("rightp = " + rightp + 
				", kwd = " + kwtyp + ", celtyp = " + celltyp);
			if (isTopLevel && node.isOpenPar()) {
				out("Here is (");
				listCount++;
				downp = node.getDownp();
				out("doLstCts: recurse, downp = " + downp);
				count = doListCounts(downp, false);
				tokenCount += count & 0xFFFF;
				listCount += count >>> 16;
				out("Here is )");
			}
			rightp = node.getRightp();
			tokenCount++;
		}
		return tokenCount + (listCount << 16);
	}
	
	public boolean isValidSrc() {
		int stmtCount = 0;
		int count;
		int rightp, downp;
		Node node;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		boolean isNone;
		boolean isDo;
		int stmtNo = 0;
		int doBlkCount = 0;

		out("Top of isValidSrc");
		brkval = 0.0;
		rightp = scan.getRootNodep();
		if (rightp == 0) {
			return false;
		}
		while (rightp != 0) {
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			isNone = (kwtyp == KeywordTyp.NULL);
			isDo = (kwtyp == KeywordTyp.DO);
			if (isNone && (stmtNo != 0)) {
				return false;
			}
			if (isDo && (stmtNo != 1)) {
				return false;
			}
			if (!isNone && !isDo) {
				return false;
			}
			celltyp = node.getDownCellTyp();
			out("rightp = " + rightp + 
				", kwd = " + kwtyp + ", celtyp = " + celltyp);
			if (isDo) {
				doBlkCount++;
				if (doBlkCount > 1) {
					return false;
				}
				out("Here is open par!");
				downp = node.getDownp();
				out("root downp = " + downp + ", rightp = " +
					node.getRightp());
				if (downp > 0) {
					count = doStmtCounts(downp);
				}
				else {
					return false;
				}
				if (count < 0) {
					return false;
				}
				stmtCount += count;
				out("Here is close par!");
			}
			rightp = node.getRightp();
			stmtNo++;
		}
		out("Stmt count = " + stmtCount);
		return (stmtCount >= 0);
	}

	private int doStmtCounts(int rightp) {
		int stmtCount = 0;
		Node node;
		int downp;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int phaseNo = 0;

		while (rightp != 0) {
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			celltyp = node.getDownCellTyp();
			out("rightp = " + rightp +  
				", kwd = " + kwtyp + ", celtyp = " + celltyp);
			if (kwtyp != KeywordTyp.ZSTMT) {
				return -1;
			}
			if (node.isOpenPar()) {
				out("Here is (");
				downp = node.getDownp();
				phaseNo = doStmt(downp, phaseNo);
				if (phaseNo < 0) {
					return -1;
				}
				out("Here is )");
			}
			else {
				return -1;
			}
			rightp = node.getRightp();
			if (phaseNo < 0) {
				return -1;
			}
			stmtCount++;
		}
		return stmtCount;
	}
	
	private int doStmt(int rightp, int phaseNo) {
		Node node;
		KeywordTyp kwtyp = null;
		NodeCellTyp celltyp;
		boolean first = true;
		int currPhaseNo = phaseNo;
		int rightq;
		int initp = rightp;
		double currbrk;
		String phaseDesc;

		while (rightp > 0) {
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			celltyp = node.getDownCellTyp();
			out("rightp = " + rightp +  
					", kwd = " + kwtyp + ", celtyp = " + celltyp);
			if (first) {
				out("Statement kwd = " + kwtyp);
				currPhaseNo = getPhaseNo(kwtyp);
				phaseDesc = getPhaseDesc(currPhaseNo);
				if ((currPhaseNo < phaseNo) && (currPhaseNo != 0)) {
					oerrd(rightp, "Top-level " + phaseDesc + 
						" encountered unexpectedly", 10.1);
					return -1;
				}
				rightq = rightp;
				rightp = node.getRightp();
				if (rightp <= 0) {
					oerrd(rightq, "Null pointer encountered unexpectedly " +
						"after " + kwtyp, 10.2);
					rightq = -1;
				}
				// rightp > 0 inside following switch
				switch (currPhaseNo) {
				case 0:
					if (!isUnitTest) {
						oerrd(initp, kwtyp + " operator (unit test) is invalid",
							10.3);
						return -1;
					}
					currbrk = getMethBrkPair(rightp);
					if (currbrk > 0) {
						brkval = currbrk;
					}
					else {
						moduleval = -(int)currbrk;
						//oprn("doStmt: currbrk = " + currbrk + ", moduleval = "+moduleval);
					}
					break;
				case 1:
					rightq = chkImportStmt(rightp, kwtyp);
					if (rightq == -1) {
						oerrd(initp, "Keyword " + kwtyp + " followed by " +
							"no module names or invalid text", 10.4);
					}
					break;
				case 2:
					if (currPhaseNo == phaseNo) {
						oerrd(rightp, "Multiple gdefun statements encountered",
							10.5);
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
						oerrd(rightq, "Keyword 'abdefun' encountered at " +
							"top level unexpectedly", 10.6);
						rightq = -1;
					}
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
	
	private int getPhaseNo(KeywordTyp kwtyp) {
		switch (kwtyp) {
		case QUEST:
			return 0;
		case IMPORT:
		case FROM:
			return 1;
		case GDEFUN:
			return 2;
		case DEFUN:
		case IDEFUN:
			return 3;
		case CLASS:
		case ICLASS:
		case ABCLASS:
		case SCOOL:
		case ISCOOL:
		case ENUM:
		case IENUM:
			return 4;
		case ABDEFUN:
			return ABPHASE;
		default:
			return -1;
		}
	}
	
	private String getPhaseDesc(int phaseNo) {
		switch (phaseNo) {
		case 1: return "module import(s)";
		case 2: return "global(s)";
		case 3: return "global function definition(s)";
		case 4: return "class definition(s)";
		default: return "post-class definitions phase";
		}
	}
	
	private double getMethBrkPair(int rightp) {
		Page page;
		int idx;
		int downp;
		Node node;
		NodeCellTyp celltyp;
		double rtnval = 0.0;

		node = store.getNode(rightp);
		celltyp = node.getDownCellTyp();
		if (celltyp != NodeCellTyp.DOUBLE) {
			return rtnval;
		}
		downp = node.getDownp();
		page = store.getPage(downp);
		idx = store.getElemIdx(downp);
		rtnval = page.getDouble(idx);
		return rtnval;
	}
	
	private boolean isFloatEq(double x, double y) {
		double eps = 0.000001;
		boolean rtnval;
		
		rtnval = (Math.abs(x - y) < eps);
		return rtnval;
	}
	
	private int chkImportStmt(int rightp, KeywordTyp kwtyp) {
		if (kwtyp == KeywordTyp.IMPORT) {
			return chkImportKwd(rightp, true);
		}
		return chkFromKwd(rightp);
	}
	
	private int chkImportKwd(int rightp, boolean hasColonCase) {
		Node node, subNode;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int currp, ascurrp;
		int modCount = 0;
		int savep = rightp;

		while (rightp > 0) {
			modCount++;
			out("modCount = " + modCount);
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			celltyp = node.getDownCellTyp();
			if (celltyp == NodeCellTyp.ID) {
				out("celltyp = ID");
			}
			else if (celltyp != NodeCellTyp.PTR) {
				oerrd(rightp, "Keyword 'import' followed by invalid text: " +
					kwtyp.toString(), 40.1);
				return 0;
			}
			else if (hasColonCase && isColonListQuiet(node)) {
				out("celltyp = PTR, (: a b)");
			}
			else {
				currp = node.getDownp();
				subNode = store.getNode(currp);
				kwtyp = subNode.getKeywordTyp();
				if (kwtyp != KeywordTyp.AS) {
					oerrd(currp, "Keyword 'import', expecting 'as', but was " +
						"followed by invalid text: " + kwtyp.toString(), 40.2);
					return 0;
				}
				ascurrp = currp;
				currp = subNode.getRightp();
				if (currp <= 0) {
					oerrd(ascurrp, "Keyword 'import', then 'as' found, " +
						"not followed by module names", 40.3);
					return 0;
				}
				subNode = store.getNode(currp);
				celltyp = subNode.getDownCellTyp();
				if (celltyp == NodeCellTyp.ID) { }
				else if (!(hasColonCase && celltyp == NodeCellTyp.PTR)) { }
				else if (!isColonList(subNode)) { 
					//oerrd(ascurrp, "Keyword 'import', then 'as' found, " +
					//	"followed by invalid text", 40.4);
					return 0;
				}
				currp = subNode.getRightp();
				if (currp <= 0) {
					oerrd(ascurrp, "Keyword 'import', then 'as' clause found, " +
						"followed by invalid text", 40.5);
					return 0;
				}
				subNode = store.getNode(currp);
				celltyp = subNode.getDownCellTyp();
				if (celltyp != NodeCellTyp.ID) {
					oerrd(ascurrp, "Keyword 'import', then 'as' clause found, " +
						"not followed by identifier", 40.6);
					return 0;
				}
				currp = subNode.getRightp();
				if (currp > 0) {
					oerrd(ascurrp, "Keyword 'import', then 'as' clause found, " +
						"then identifier found, followed by invalid text", 40.7);
					return 0;
				}
			}
			rightp = node.getRightp();
		}
		if (modCount <= 0) {
			return -1;
		}
		return savep;
	}
	
	private boolean isColonList(Node node) {
		return isColonListRtn(node, true);
	}
	
	private boolean isColonListQuiet(Node node) {
		return isColonListRtn(node, false);
	}
	
	private boolean isColonListRtn(Node node, boolean verbose) {
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int rightp;
		int colonp;
		int count = 0;
		
		rightp = node.getDownp();		
		if (rightp <= 0) {
			return false;
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp == KeywordTyp.DOT) { }
		else if (verbose) {
			oerrd(rightp, "Expecting colon operator, instead found: " +
				kwtyp.toString(), 50.1);
			return false;
		}
		else {
			return false;
		}
		colonp = rightp;
		rightp = node.getRightp();
		while (rightp > 0) {
			count++;
			node = store.getNode(rightp);
			celltyp = node.getDownCellTyp();
			if (celltyp == NodeCellTyp.ID) { }
			else if (verbose) {
				oerrd(rightp, "Expecting identifier after colon operator, " +
					"invalid text found", 50.2);
				return false;
			}
			else {
				return false;
			}
			rightp = node.getRightp();
		}
		if (count >= 2) {
			return true;
		}
		if (!verbose) {
			return false;
		}
		oerrd(colonp, "Expecting multiple identifiers after colon operator, " +
				"less than 2 found", 50.3);
		return false;
	}
	
	private boolean isRelModList(Node node) {
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int rightp, rightq;
		int count = 0;
		
		celltyp = node.getDownCellTyp();
		if (celltyp != NodeCellTyp.PTR) {
			return false;
		}
		rightp = node.getDownp();		
		if (rightp <= 0) {
			return false;
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.DOT) {
			oerrd(rightp, "Keyword 'from' needs dot operator", 60.1);
			return false;
		}
		rightq = rightp;
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(rightq, "Keyword 'from', dot operator, followed by null pointer",
				60.2);
			return false;
		}
		node = store.getNode(rightp);
		celltyp = node.getDownCellTyp();
		if (celltyp == NodeCellTyp.INT) {
			count++;
			rightp = node.getRightp();
		}
		while (rightp > 0) {
			count++;
			node = store.getNode(rightp);
			celltyp = node.getDownCellTyp();
			if (celltyp != NodeCellTyp.ID) {
				oerrd(rightp, "Keyword 'from', dot operator, " +
					"expecting list of identifiers", 60.3);
				return false;
			}
			rightq = rightp;
			rightp = node.getRightp();
		}
		if (count <= 0) {
			oerrd(rightq, "Keyword 'from', dot operator, no arguments found", 60.4);
			return false;
		}
		return true;
	}
	
	private int chkFromKwd(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int savep = rightp;
		int rightq;

		if (rightp <= 0) {
			return -1;
		}
		rightq = rightp;
		node = store.getNode(rightp);
		celltyp = node.getDownCellTyp();
		if (celltyp == NodeCellTyp.ID) {
			rightp = node.getRightp();
		}
		else if (isRelModList(node)) {
			rightp = node.getRightp();
		}
		else {
			oerrd(rightq, "Keyword 'from' followed by incorrect text", 70.1);
			return -1;
		}
		if (rightp <= 0) {
			oerrd(rightq, "Keyword 'from' followed by null pointer", 70.2);
			return -1;
		}
		rightq = rightp;
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.IMPORT) {
			oerrd(rightp, "Keyword: 'from' missing 'import' clause", 70.3);
			return -1;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerrd(rightq, "Keyword 'import' in 'from' statement " +
				"encountered w/o list of modules", 70.4);
			return -1;
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp == KeywordTyp.ALL) { 
			rightp = node.getRightp();
			if (rightp > 0) {
				oerrd(rightp, "Keyword 'all' in 'from' statement followed by " +
					"unexpected text", 70.5);
				return -1;
			}
			else {
				return savep;
			}
		}
		rightq = rightp;
		savep = chkImportKwd(rightp, false);
		if (savep <= 0) {
			oerrd(rightq, "Keyword 'import' in 'from' statement " +
				"followed by incorrect text", 70.6);
		}
		return savep;
	}
	
	private int chkGlbDefStmt(int rightp) {
		Node node, parNode;
		KeywordTyp kwtyp;
		int savep = rightp;
		int rightq = 0;
		int phaseNo = 0;
		int oldPhaseNo = 0;

		while (rightp > 0) {
			parNode = store.getNode(rightp);
			node = store.getSubNode(parNode);
			if (node != null) {
				kwtyp = node.getKeywordTyp();
			}
			else if (parNode.getKeywordTyp() == KeywordTyp.DO) {
				rightq = rightp;
				break;
			}
			else {
				kwtyp = parNode.getKeywordTyp();
				oerrd(rightp, "Invalid keyword: " + kwtyp + " found in gdefun stmt.",
					80.1);
				return -1;
			}
			phaseNo = getGdefunPhase(kwtyp);
			if (phaseNo <= oldPhaseNo) {
				oerrd(savep, "Error in gdefun stmt.: keyword " + kwtyp +
					" encountered unexpectedly", 80.2);
				return -1;
			}
			switch (phaseNo) {
			case 1:
			case 2:
				rightq = rightp;
				rightp = node.getRightp();
				if (chkVarList(rightp) < 0) {
					oerrd(savep, "Error in gdefun stmt.: invalid var list", 80.3);
					return -1;
				}
				break;
			default:
				oerrd(rightp, "Invalid keyword: (" + kwtyp + "...) found in gdefun stmt.",
					80.4);
				return -1;
			}
			oldPhaseNo = phaseNo;
			rightq = rightp;
			rightp = parNode.getRightp();
		}
		out("gdefun: chk DO");
		rightp = chkDoBlock(rightq);
		if (rightp != 0) {
			oerrd(savep, "Error in gdefun stmt. DO block", 80.5);
			return -1;
		}
		return savep;
	}
	
	private int getGdefunPhase(KeywordTyp kwtyp) {
		switch (kwtyp) {
		case VAR:
			return 1;
		case IVAR:
			return 2;
		default:
			return -1;
		}
	}
	
	private int getDefunPhase(KeywordTyp kwtyp) {
		switch (kwtyp) {
		case ZCALL:
			return 1;
		case VAR:
			return 2;
		case GVAR:
			return 3;
		case DECOR:
			return 4;
		default:
			return -1;
		}
	}
	
	private int getParmPhase(KeywordTyp kwtyp) {
		switch (kwtyp) {
		case NULL:
			return 1;
		case SET:
			return 2;
		case MPY:
			return 3;
		case DBLSTAR:
			return 4;
		default:
			return -1;
		}
	}
	
	private int chkDefunStmt(int rightp) {
		Node node, parNode;
		KeywordTyp kwtyp;
		int savep = rightp;
		int rightq = 0;
		int phaseNo = 0;
		int oldPhaseNo = 0;
		boolean isParms = false;

		while (rightp > 0) {
			parNode = store.getNode(rightp);
			node = store.getSubNode(parNode);
			if (node != null) {
				kwtyp = node.getKeywordTyp();
			}
			else if (parNode.getKeywordTyp() == KeywordTyp.DO) {
				rightq = rightp;
				break;
			}
			else {
				oerrd(rightp, "Invalid text encountered in defun stmt. header",
					100.1);
				out("chkDefunStmt (): fail 0");
				return -1;
			}
			rightp = node.getRightp();
			phaseNo = getDefunPhase(kwtyp);
			if (phaseNo < 0) {
				oerrd(savep, "Invalid keyword " + kwtyp.toString() + 
					" encountered in defun stmt.", 100.2);
				out("chkDefunStmt (): fail 0.5");
				return -1;
			}
			if (phaseNo <= oldPhaseNo) {
				oerrd(savep, "Defun statement header error: " +
					kwtyp.toString() + " encountered unexpectedly", 100.3);
				out("chkDefunStmt (): fail 1");
				return -1;
			}
			if (phaseNo > 1 && !isParms) {
				oerrd(savep, "Defun statement header error: " +
					"missing function name/parameters", 100.4);
				out("chkDefunStmt (): fail 2");
				return -1;
			}
			switch (phaseNo) {
			case 1:
				if (!chkParmList(rightp)) {
					oerrd(savep, "Error in parm list of defun stmt.", 100.5);
					out("chkDefunStmt (): fail 3");
					return -1;
				}
				isParms = true;
				break;
			case 2:
			case 3:
				if (chkVarList(rightp) < 0) {
					oerrd(savep, "Error in var list of defun stmt.: " +
						"invalid text found when processing identifier list", 100.6);
					out("chkDefunStmt (): fail 4");
					return -1;
				}
				break;
			case 4:
				if (!chkDecorList(rightp)) {
					oerrd(savep, "Error in decor list of defun stmt.", 100.65);
					out("chkDefunStmt (): fail 5");
					return -1;
				}
				break;
			default:
				oerrd(savep, "Invalid keyword encountered in " +
					"defun stmt. header", 100.68);
				out("chkDefunStmt (): fail 6");
				return -1;
			}
			oldPhaseNo = phaseNo;
			rightq = rightp;
			rightp = parNode.getRightp();
		}
		if (!isParms) {
			oerrd(rightq, "Defun statement header error: " +
				"missing function name/parameters, post-loop", 100.7);
			out("chkDefunStmt (): fail 7");
			return -1;
		}
		out("defun: chk DO");
		rightp = chkDoBlock(rightq);
		if (rightp < 0) {
			oerrd(rightq, "Error in do-block of defun stmt.", 100.8);
			out("chkDefunStmt (): fail 8");
			return -1;
		}
		return savep;
	}
	
	private int chkAbDefunStmt(int rightp) {
		Node node, parNode;
		KeywordTyp kwtyp;
		int savep = rightp;
		int rightq = 0;
		int phaseNo = 0;
		int oldPhaseNo = 0;
		boolean isParms = false;

		out("chkAbDefunStmt() - top");
		while (rightp > 0) {
			parNode = store.getNode(rightp);
			node = store.getSubNode(parNode);
			if (node != null) {
				kwtyp = node.getKeywordTyp();
			}
			else if (parNode.getKeywordTyp() == KeywordTyp.DO) {
				oerrd(rightp, "Error: DO keyword found in abdefun stmt.", 110.1);
				// not yet unit tested
				break;
			}
			else {
				oerrd(rightp, "Invalid text encountered in abdefun stmt. header",
					110.2);
				return -1;
			}
			rightq = rightp;
			rightp = node.getRightp();
			phaseNo = getDefunPhase(kwtyp);
			if (phaseNo < 0) {
				oerr(rightp, "Invalid keyword " + kwtyp + 
					" encountered in abdefun stmt.");
				return -1;
			}
			if (phaseNo <= oldPhaseNo) {
				oerr(rightp, "Abdefun statement header error: " +
					kwtyp + " encountered unexpectedly");
				return -1;
			}
			switch (phaseNo) {
			case 1:
				if (!chkParmList(rightp)) {
					return -1;
				}
				isParms = true;
				break;
			case 2:
			case 3:
				oerr(rightp, "Error: abdefun stmt. invalid phase = " + kwtyp);
				return -1;
			case 4:
				if (!chkDecorList(rightp)) {
					return -1;
				}
				break;
			default:
				oerr(rightp, "Error: abdefun stmt. unknown phase = " + kwtyp);
				return -1;
			}
			oldPhaseNo = phaseNo;
			rightp = parNode.getRightp();
		}
		if (!isParms) {
			oerr(rightq, "Abdefun statement header error: " +
				"missing function name/parameters, post-loop");
			return -1;
		}
		out("abdefun: end loop");
		if (rightp > 0) {
			//oerr(rightp, "Error: DO keyword #2 found in abdefun stmt.");
			return -1;
		}
		return savep;
	}
	
	private boolean chkDecorList(int rightp) {
		Node node, parNode;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int savep = rightp;
		int subRightp;
		int count = 0;
		int n;
		
		while (rightp > 0) {
			count++;
			parNode = store.getNode(rightp);
			node = store.getSubNode(parNode);
			celltyp = parNode.getDownCellTyp();
			if (node != null) {
				kwtyp = node.getKeywordTyp();
				subRightp = node.getRightp();
			}
			else if (celltyp != NodeCellTyp.ID) {
				oerrd(rightp, "Keyword or constant encountered in decor list, " +
					"identifier expected", 120.1);
				return false;
			}
			else {
				kwtyp = KeywordTyp.NULL;
				subRightp = 0;
			}
			switch (kwtyp) {
			case NULL:
				break;
			case ZCALL:
				subRightp = parNode.getDownp();
				if (chkVarList(subRightp) < 2) {
					oerrd(rightp, "Decor error: " +
						"under 2 identifiers encountered in list", 120.2);
					return false;
				}
				break;
			case DOT:
				n = chkDotCall(subRightp);
				if (n >= 2) {
					break;
				}
				if (n >= 0) {
					oerrd(rightp, "Decor error: " +
						"under 2 identifiers encountered in dot-list", 120.3);
					// more error messages in chkDotCall
				}
				return false;
			default:
				oerrd(savep, "Invalid keyword: " + kwtyp.toString() +
					" encountered in decor list", 120.4);
				return false;
			}
			rightp = parNode.getRightp();
			out("decor # " + count);
		}
		return true;
	}
	
	private int chkVarList(int rightp) {
		Node node;
		NodeCellTyp celltyp;
		int count = 0;
		
		while (rightp > 0) {
			node = store.getNode(rightp);
			celltyp = node.getDownCellTyp();
			if (celltyp != NodeCellTyp.FUNC &&
				celltyp != NodeCellTyp.ID)
			{
				out("var (): fail 1");
				return -1;
			}
			count++;
			out("var (): count = " + count);
			rightp = node.getRightp();
		}
		return count;
	}
	
	private int chkDotCall(int rightp) {
		Node node, subNode;
		NodeCellTyp celltyp;
		int savep = rightp;
		int subRightp;
		int count = 0;
		
		while (rightp > 0) {
			node = store.getNode(rightp);
			subNode = store.getSubNode(node);
			if (subNode != null) {
				subRightp = node.getDownp();
				if (chkVarList(subRightp) < 1) {
					oerrd(rightp, "Invalid dot-call: terminating identifier list " +
						"has no identifiers", 140.1);
					return -1;
				}
				rightp = node.getRightp();
				if (rightp > 0) {
					oerrd(rightp, "Invalid dot-call: terminating identifier list " +
						"followed by unexpected text", 140.2);
					return -1;
				}
				if (count < 1) {
					oerrd(savep, "Invalid dot-call: terminating identifier list " +
						"not preceded by identifier(s)", 140.3);
					return -1;
				}
				return count + 1;
			}
			celltyp = node.getDownCellTyp();
			if (celltyp != NodeCellTyp.ID) {
				oerrd(rightp, "Invalid dot-call: expecting identifier", 140.4);
				out("dotCall: fail 1");
				return -1;
			}
			count++;
			out("dotCall: count = " + count);
			rightp = node.getRightp();
		}
		return count;
	}
	
	private boolean chkParmList(int rightp) {
		Node node, parNode;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int subRightp;
		int phaseNo = 0;
		int oldPhaseNo = 0;
		int count = 0;
		
		while (rightp > 0) {
			count++;
			parNode = store.getNode(rightp);
			node = store.getSubNode(parNode);
			celltyp = parNode.getDownCellTyp();
			if (node != null) {
				kwtyp = node.getKeywordTyp();
				subRightp = node.getRightp();
			}
			else if (celltyp != NodeCellTyp.ID) {
				oerrd(rightp, "Keyword or constant encountered in " +
					"parameter list, identifier expected", 150.1);
				return false;
			}
			else {
				kwtyp = KeywordTyp.NULL;
				subRightp = 0;
			}
			phaseNo = getParmPhase(kwtyp);
			if (phaseNo < 0) {
				oerrd(rightp, "Invalid keyword: " + kwtyp +
					" encountered in parameter list", 150.2);
				return false;
			}
			if (phaseNo < oldPhaseNo) {
				oerrd(rightp, "Parameter list error: " + kwtyp +
					" encountered unexpectedly", 150.3);
				return false;
			}
			switch (phaseNo) {
			case 1:
				break;
			case 2:
				if (!chkDefParm(subRightp)) {
					return false;
				}
				break;
			case 3:
			case 4:
				if (!chkStarParm(subRightp)) {
					oerrd(rightp, "Invalid star-type parameter encountered " +
						"in parameter list", 150.4);
					return false;
				}
				break;
			default:
				// dup. cond'n, case already handled after getParmPhase() call
				oerr(rightp, "Invalid keyword: " + kwtyp +
					" encountered in parameter list");
				return false;
			}
			oldPhaseNo = phaseNo;
			rightp = parNode.getRightp();
			out("parm # " + count);
		}
		return true;
	}
	
	private boolean chkStarParm(int rightp) {
		Node node;
		NodeCellTyp celltyp;
		
		node = store.getNode(rightp);
		celltyp = node.getDownCellTyp();
		if (celltyp != NodeCellTyp.ID) {
			return false;
		}
		rightp = node.getRightp();
		if (rightp > 0) {
			return false;
		}
		return true;
	}
	
	private boolean chkDefParm(int rightp) {
		return chkDefParmRtn(rightp, false);
	}

	private boolean isValidConstPair(int rightp) {
		return chkDefParmRtn(rightp, true);
	}

	private boolean chkDefParmRtn(int rightp, boolean isConstPair) {
		Node node;
		NodeCellTyp celltyp, inCellTyp;
		
		node = store.getNode(rightp);
		celltyp = node.getDownCellTyp();
		if (isConstPair) {
			inCellTyp = NodeCellTyp.FUNC;
		}
		else {
			inCellTyp = NodeCellTyp.ID;
		}
		if (celltyp != inCellTyp) {
			oerrd(rightp, "Error in default parameter (or const. pair):" +
				" identifier not found", 170.1);
			out("chkDefParm (): celltyp = " + celltyp);
			out("chkDefParm (): fail 0");
			return false;
		}
		rightp = node.getRightp();
		if (!isConstExpr(rightp)) {
			oerrd(rightp, "Error in default parameter (or const. pair):" +
				" constant expr. not found", 170.2);
			out("chkDefParm (): fail 1");
			return false;
		}
		return true;
	}
	
	private boolean isConstExpr(int rightp) {
		Node node;
		NodeCellTyp celltyp;
		KeywordTyp kwtyp;
		
		if (rightp <= 0) {
			return false;
		}
		node = store.getNode(rightp);
		rightp = node.getRightp();
		if (rightp > 0) {
			return false;
		}
		celltyp = node.getDownCellTyp();
		if (celltyp == NodeCellTyp.KWD) {
			kwtyp = node.getKeywordTyp();
			switch (kwtyp) {
			case NULL:
			case TRUE:
			case FALSE:
				return true;
			default:
				return false;
			}
		}
		switch (celltyp) {
		case INT:
		case LONG:
		case DOUBLE:
		case STRING:
			return true;
		default:
			return false;
		}
	}
	
	public int chkDo(int rightp) {
		Node node;
		int downp;

		while (rightp > 0) {
			node = store.getNode(rightp);
			if (!node.isOpenPar()) {  // may never happen
				oerr(rightp, "Do block error (in chkDo): isOpenPar failure");
				return -1;
			}
			out("Here is (");
			downp = node.getDownp();
			if ((downp <= 0) || !synStmt.doStmt(downp)) {
				return -1;
			}
			out("Here is )");
			rightp = node.getRightp();
		}
		return 0; // OK
	}
	
	public int chkDoBlock(int rightp) {
		return chkDoBlockRtn(rightp, true);
	}
		
	public int chkStmtDoBlock(int rightp) {
		return chkDoBlockRtn(rightp, false);
	}
		
	private int chkDoBlockRtn(int rightp, boolean isFinal) {
		Node node;
		KeywordTyp kwtyp;
		int savep = rightp;
		int rightq = 0;
		int rtnval = 0;
		
		if (rightp <= 0) {
			oerr(0, "Internal error: do-block handler was passed zero address");
			out("doBlock (): fail 0");
			return -1;
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.DO) {
			oerr(rightp, "Error: do-block handler called, no DO keyword");
			out("doBlock (): fail 1");
			return -1;
		}
		rightq = node.getRightp();
		if (rightq <= 0) { }
		else if (isFinal) {
			oerr(rightp, "Unexpected/invalid text encountered after " +
				"do-block containing stmts.");
			out("doBlock (): fail 1.5");
			return -1;
		}
		else {
			rtnval = rightq;
		}
		rightp = node.getDownp();
		if (rightp <= 0) {
			oerr(savep, "Do block mising body");
			return -1;
		}
		node = store.getNode(rightp);
		if (!node.isOpenPar()) {  // never lands here
			oerr(rightp, "Do block error: body lacks parentheses");
			out("doBlock (): fail 2");
			return -1;
		}
		rightq = rightp;
		rightp = node.getDownp();
		node = store.getNode(rightp);
		rightp = node.getRightp();
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.TUPLE) { }
		else if (rightp <= 0) {
			return rtnval;  // 0, isFinal
		}
		else {
			return -1;  // only naked tuple allowed
		}
		if (chkDo(rightq) < 0) {
			return -1;
		}
		return rtnval;  // 0, isFinal
	}
	
	private int chkClassStmt(int rightp, KeywordTyp kwtyp) {
		Node node, parNode;
		int savep = rightp;
		int rightq = 0;
		int phaseNo = 0;
		int oldPhaseNo = 0;
		KeywordTyp curkwtyp;
		boolean isNameFound = false;
		boolean isDotOK = true;
		boolean isAbCls = (kwtyp == KeywordTyp.ABCLASS);

		switch (kwtyp) {
		case SCOOL:
		case ISCOOL:
			return chkScoolStmt(rightp);
		case ENUM:
		case IENUM:
			return chkEnumStmt(rightp, false);
		default:
			break;
		}
		while (rightp > 0) {
			rightq = rightp;
			parNode = store.getNode(rightp);
			curkwtyp = parNode.getKeywordTyp();
			node = store.getSubNode(parNode);
			if (node != null) {
				kwtyp = node.getKeywordTyp();
			}
			else if (parNode.getKeywordTyp() == KeywordTyp.DO) {
				break;
			}
			else if (parNode.getDownCellTyp() == NodeCellTyp.ID) {
				kwtyp = KeywordTyp.NULL;
			}
			else {
				oerr(rightp, "Error in class def, unexpected keyword found: " +
					curkwtyp);
				out("chkClassStmt (): fail 0");
				return -1;
			}
			phaseNo = getClassPhase(kwtyp, oldPhaseNo);
			if (phaseNo < 0) {
				oerr(rightp, "Malformed class def");
				return -1;
			}
			if (phaseNo <= oldPhaseNo) {
				oerr(rightp, "Malformed (mixed up) class def");
				out("chkClassStmt (): fail 1");
				return -1;
			}
			switch (phaseNo) {
			case 1:
				isNameFound = true;
				break;
			case 2:
				// base class
				if (node != null) {
					rightp = node.getRightp();
				}
				isDotOK = (kwtyp == KeywordTyp.NULL) || 
					isValidDotList(rightp);
				break;
			case 3:
				// does
				rightp = node.getRightp();
				isDotOK = isValidDoesList(rightp, rightq);
				break;
			case 4:
			case 5:
				rightp = node.getRightp();
				if (chkVarList(rightp) < 0) {
					oerr(rightq, "Error in class def: malformed var list");
					out("chkClassStmt (): fail 2");
					return -1;
				}
				break;
			default:
				// never happens because getClassPhase will catch it
				oerr(rightp, "Error in class def: system error");
				out("chkClassStmt (): fail 3");
				return -1;
			}
			if (!isNameFound && (phaseNo > 1)) {
				oerr(rightp, "Class definition: Name of class not found");
				out("chkClassStmt (): fail 4");
				return -1;
			}
			if (isDotOK) { }
			else if (phaseNo == 2) {
				//out("chkClassStmt (): fail 5");
				oerr(rightq, "Error in class def: base class has invalid dot list");
				return -1;
			}
			else {
				oerr(rightq, "Error in class def: does list is invalid");
				return -1;
			}
			oldPhaseNo = phaseNo;
			rightp = parNode.getRightp();
		}
		out("class: chk DO");
		if (rightp <= 0) {
			oerr(rightq, "Missing DO in class def");
			out("chkClassStmt (): fail 6");
			return -1;
		}
		rightp = chkDoDefBlock(rightp, isAbCls);
		if (rightp != 0) {
			oerr(rightq, "Error in DO block in class def");
			out("chkClassStmt (): fail 7");
			return -1;
		}
		return savep;
	}
	
	private int getClassPhase(KeywordTyp kwtyp, int phaseNo) {
		switch (kwtyp) {
		case NULL:
			if (phaseNo == 0) {
				return 1;
			}
			else if (phaseNo == 1) {
				return 2;
			}
			else {
				out("getClassPhase (): fail 0");
				return -1;
			}
		case DOT:
			return 2;
		case DOES:
			return 3;
		case VAR:
			return 4;
		case IVAR:
			return 5;
		default:
			out("getClassPhase (): fail 1");
			return -1;
		}
	}
	
	private int chkScoolStmt(int rightp) {
		Node node;
		Node parNode;
		int savep = rightp;
		int rightq = 0;
		int phaseNo = 0;
		int oldPhaseNo = 0;
		boolean isNameFound = false;
		KeywordTyp kwtyp = null;

		while (rightp > 0) {
			rightq = rightp;
			parNode = store.getNode(rightp);
			node = store.getSubNode(parNode);
			if (node != null) {
				kwtyp = node.getKeywordTyp();
			}
			else if (parNode.getKeywordTyp() == KeywordTyp.DO) {
				break;
			}
			else if (parNode.getDownCellTyp() == NodeCellTyp.ID) {
				kwtyp = KeywordTyp.NULL;
			}
			else {
				oerr(rightp, "Error in scool def: unexpected keyword encountered");
				out("chkScoolStmt (): fail 0");
				return -1;
			}
			phaseNo = getScoolPhase(kwtyp, oldPhaseNo);
			if (phaseNo < 0) {
				oerr(rightp, "Malformed scool def with invalid keyword: " + kwtyp);
				return -1;
			}
			if (phaseNo <= oldPhaseNo) {
				oerr(rightp, "Malformed (mixed up) scool def");
				out("chkScoolStmt (): fail 1");
				return -1;
			}
			switch (phaseNo) {
			case 1:
				isNameFound = true;
				break;
			case 2:
				// does
				rightp = node.getRightp();
				if (!isValidDoesList(rightp, rightq)) {
					out("chkScoolStmt (): fail 2");
					return -1;
				}
				break;
			case 3:
				// const
				rightp = node.getRightp();
				if (!isValidConstList(rightp, rightq)) {
					out("chkScoolStmt (): fail 3");
					return -1;
				}
				break;
			default:
				break;
			}
			if (!isNameFound && (phaseNo > 1)) {
				oerr(rightp, "Error in scool def: missing scool name");
				out("chkScoolStmt (): fail 4");
				return -1;
			}
			oldPhaseNo = phaseNo;
			rightp = parNode.getRightp();
		}
		out("scool: chk DO");
		if (rightp <= 0) {
			oerr(rightq, "Missing DO in scool def");
			return -1;
		}
		rightp = chkScoolDoBlock(rightp);
		if (rightp != 0) {
			oerr(rightq, "Error in DO block in scool def");
			out("chkScoolStmt (): fail 5");
			return -1;
		}
		return savep;
	}
	
	private int getScoolPhase(KeywordTyp kwtyp, int phaseNo) {
		switch (kwtyp) {
		case NULL:
			return 1;
		case DOES:
			return 2;
		case CONST:
			return 3;
		default:
			out("getScoolPhase (): fail 1");
			return -1;
		}
	}
	
	private boolean isValidConstList(int rightp, int rightq) {
		Node node, subnode;
		int downp;
		int count = 0;
		boolean isValid;
		
		while (rightp > 0) {
			count++;
			node = store.getNode(rightp);
			subnode = store.getSubNode(node);
			downp = node.getDownp();
			if (subnode == null || !isValidConstPair(downp)) {
				if (subnode == null) {
					oerr(rightp, "Error in const list: missing parens");
					out("isValidConstList (): null subnode!");
				}
				else {
					oerr(rightp, "Error in const list: invalid const pair");
				}
				out("isValidConstList (): count = " + count);
				out("isValidConstList (): fail 1");
				return false;
			}
			rightp = node.getRightp();
		}
		out("isValidConstList (): bottom");
		isValid = (count >= 1);
		if (!isValid) {
			oerr(rightq, "Error in const list: no const pairs found");
			out("isValidConstList (): fail 2");
		}
		return isValid;
	}
			
	public int chkEnumStmt(int rightp, boolean isExpr) {
		Node node, subNode;
		int savep = rightp;
		int rightq;
		int count = 0;
		int enumTyp = -1;  // 0: id, 1: int, 2: char
		int etyp;
		
		if (rightp == 0) {
			return -1;
		}
		rightq = rightp;
		node = store.getNode(rightp);
		if (node.getDownCellTyp() != NodeCellTyp.ID) {
			oerr(rightp, "Error in enum def: missing enum name");
			return -1;
		} 
		rightp = node.getRightp();
		while (rightp > 0) {
			count++;
			node = store.getNode(rightp);
			subNode = store.getSubNode(node);
			if (subNode != null) {
				etyp = getEnumPair(rightp, isExpr);
			}
			else {
				etyp = getEnumTyp(node);
			}
			if (etyp < 0) {
				oerr(rightp, "Error in enum def: invalid enum id/val/pair found");
				return -1;
			}
			if (enumTyp < 0) {
				enumTyp = etyp;
			}
			else if (etyp != enumTyp) {
				oerr(rightp, "Error in enum def: mixed id/val/pairs found");
				return -1;
			}
			rightp = node.getRightp();
		}
		if ((count == 0) && !isExpr) {
			oerr(rightq, "Error in enum def: no id/val/pairs found");
			return -1;
		}
		out("chkEnumStmt: count = " + count);
		return savep;
	}
	
	private int getEnumTyp(Node node) {
		int len;
		
		switch (node.getDownCellTyp()) {
		case ID: return 0;
		case INT: return 1;
		case STRING:
			len = getStrNodeLen(node);
			if (len != 1) {
				return -1;
			}
			return 2;
		default:
			return -1;
		}
	}
	
	private int getEnumPair(int rightp, boolean isExpr) {
		Node node, parNode;
		int rightq = 0;
		int enumTyp, etyp;
		
		if (rightp <= 0) {
			return -1;
		}
		parNode = store.getNode(rightp);
		node = store.getSubNode(parNode);
		if (node.getKeywordTyp() != KeywordTyp.DOT) {
			oerr(rightp, "Invalid enum pair: missing DOT operator");
			return -1;
		}
		rightq = rightp;
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerr(rightq, "Invalid enum pair: DOT operator has no operands");
			out("getEnumPair (): fail 0");
			return -1;
		}
		node = store.getNode(rightp);
		enumTyp = getEnumTyp(node);
		rightq = rightp;
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerr(rightq, "Invalid enum pair: DOT operator has only single operand");
			out("getEnumPair (): fail 0.5");
			return -1;
		}
		node = store.getNode(rightp);
		etyp = getEnumTyp(node);
		rightp = node.getRightp();
		if (rightp > 0) {
			oerr(rightq, "Invalid enum pair: DOT operator has more than 2 operands");
			out("getEnumPair (): fail 1");
			return -1;
		}
		if (etyp != enumTyp) {
			oerr(rightq, "Invalid enum pair: DOT operator has mixed operands");
			out("getEnumPair (): fail 2");
			return -1;
		}
		out("getEnumPair() = " + enumTyp);
		if ((enumTyp == 0) && !isExpr) {
			oerr(rightq, "Invalid enum pair: expecting constants, identifiers found");
			out("getEnumPair (): fail 2");
			return -1;
		}
		return enumTyp;
	}
	
	private int getStrNodeLen(Node node) {
		Page page;
		int idx;
		int downp;
		String str;
		int len;

		if (node.getDownCellTyp() != NodeCellTyp.STRING) {
			return -1;
		}
		downp = node.getDownp();
		page = store.getPage(downp);
		idx = store.getElemIdx(downp);
		str = page.getString(idx);
		len = str.length();
		return len;
	}
	
	private boolean isValidDotList(int rightp) {
		return (chkVarList(rightp) >= 2);
	}
	
	private boolean isValidDoesList(int rightp, int rightq) {
		Node node, subNode;
		KeywordTyp kwtyp;
		int subRightp;
		int count = 0;
		boolean isValid;

		while (rightp > 0) {
			node = store.getNode(rightp);
			subNode = store.getSubNode(node);
			if (node.getDownCellTyp() == NodeCellTyp.ID) {} 
			else if (subNode == null) {
				oerr(rightp, "Error in does list: expecting identifier or dot list");
				out("isValidDoesList (): fail 0");
				return false;
			}
			else {
				kwtyp = subNode.getKeywordTyp();
				if (kwtyp != KeywordTyp.DOT) {
					oerr(rightp, "Error in does list: expecting dot operator, " +
						kwtyp + " found");
					out("isValidDoesList (): fail 1");
					return false;
				}
				subRightp = subNode.getRightp();
				if (!isValidDotList(subRightp)) {
					oerr(rightp, "Error in does list: malformed dot list");
					out("isValidDoesList (): fail 2");
					return false;
				}
			}
			count++;
			rightp = node.getRightp();
		}
		isValid = (count >= 1);
		if (!isValid) {
			oerr(rightq, "Error in does list: no args found (identifiers/dot-lists)");
			out("isValidDoesList (): fail 3");
		}
		return isValid;
	}
	
	private int chkDoDefBlock(int rightp, boolean isAbCls) {
		Node node, subNode;
		KeywordTyp kwtyp;
		int downp, downq;
		int phaseNo;
		boolean abfound;
		
		if (rightp <= 0) {
			out("doDefBlock (): fail 0");
			return -1;
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.DO) {
			oerr(rightp, "Error in class def DO block, instead of DO, " +
				kwtyp + " encountered");
			out("doDefBlock (): fail 1");
			return -1;
		}
		rightp = node.getDownp();
		while (rightp > 0) {
			node = store.getNode(rightp);
			if (!node.isOpenPar()) {
				oerr(rightp, "Class def DO block system error: isOpenPar failure");
				out("doDefBlock (): fail 5");
				return -1;
			}
			downp = node.getDownp();
			subNode = store.getNode(downp);
			kwtyp = subNode.getKeywordTyp();
			phaseNo = getPhaseNo(kwtyp);
			switch (phaseNo) {
			case 3:
				abfound = false;
				break;
			case ABPHASE:
				abfound = true;
				break;
			default:
				oerr(rightp, "Expecting func def, " + kwtyp + " found");
				out("doDefBlock (): fail 3");
				return -1;
			}
			out("doDefBlock (): OK - 3");
			downp = subNode.getRightp();
			if (abfound && isAbCls) {
				downq = chkAbDefunStmt(downp);
			}
			else if (abfound) {
				oerr(downp, "Error: ABDEFUN found in DO block of class def");
				out("doDefBlock (): fail 4");
				return -1;
			}
			else {
				downq = chkDefunStmt(downp);
			}
			if (downq <= 0) {
				oerr(downp, "Error in func def");
				out("doDefBlock (): fail 5");
				return -1;
			}
			rightp = node.getRightp();
		}
		return 0; // OK
	}
	
	private int chkScoolDoBlock(int rightp) {
		Node node, subNode;
		KeywordTyp kwtyp;
		int downp, downq;
		
		if (rightp <= 0) {
			out("doScoolBlock (): fail 0");
			return -1;
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.DO) {
			oerr(rightp, "System error in scool do block: missing DO keyword");
			out("doScoolBlock (): fail 1");
			return -1;
		}
		rightp = node.getDownp();
		while (rightp > 0) {
			node = store.getNode(rightp);
			if (!node.isOpenPar()) {
				oerr(rightp, "System error in scool do block: isOpenPar failure");
				out("doScoolBlock (): fail 3");
				return -1;
			}
			downp = node.getDownp();
			subNode = store.getNode(downp);
			kwtyp = subNode.getKeywordTyp();
			downp = subNode.getRightp();
			if (downp == 0) {
				oerr(rightp, "Error in scool def: " + kwtyp +
					" missing open paren");
				out("doScoolBlock (): fail 4");
				return -1;
			}
			switch (kwtyp) {
			case ABDEFUN:
				downq = chkAbDefunStmt(downp);
				break;
			case DEFIMP:
				downq = chkDefunStmt(downp);
				break;
			default:
				oerr(downp, "Error in scool def: unexpected keyword " +
					kwtyp + " found");
				out("doScoolBlock (): fail 5");
				return -1;
			}
			out("doScoolBlock (): OK - 5");
			if (downq <= 0) {
				oerr(downp, "Error in scool def: invalid abdefun/defimp def");
				out("doScoolBlock (): fail 6");
				return -1;
			}
			rightp = node.getRightp();
		}
		return 0; // OK
	}
	
}
