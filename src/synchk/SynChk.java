package synchk;

import iconst.KeywordTyp;
import iconst.NodeCellTyp;
import page.Node;
import page.Page;
import page.Store;
import scansrc.ScanSrc;

public class SynChk {
	
	private ScanSrc scan;
	private Store store;
	private boolean isZparen;
	private static final int ABPHASE = 100;

	public SynChk(ScanSrc scan, Store store) {
		this.scan = scan;
		this.store = store;
	}
	
	private void out(String msg) {
		scan.out(msg);
	}
	
	private void oerr(int nodep, String msg) {
		int lineno;
		String preLineNoStr;
		
		lineno = store.lookupLineNo(nodep);
		if (lineno > 0) {
			preLineNoStr = "Line " + lineno + " - ";
			msg = preLineNoStr + msg;
		}
		scan.omsg("Syntax Error:");
		scan.omsg(msg);
	}
	
	public int getNodeCount() {
		int tokenCount = 0;
		int listCount = 0;
		int count;
		int rightp, downp;
		Node node;
		Page page;
		int idx;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;

		rightp = scan.getRootNodep();
		while (rightp != 0) {
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			kwtyp = node.getKeywordTyp();
			celltyp = node.getDownCellTyp();
			out("rightp = " + rightp + ", idx = " + idx + 
				", kwd = " + kwtyp + ", celtyp = " + celltyp);
			if (node.isOpenPar()) {
				out("Here is open par!");
				listCount++;
				downp = node.getDownp();
				out("root downp = " + downp + ", rightp = " +
					node.getRightp());
				count = doListCounts(downp);
				tokenCount += count & 0xFFFF;
				listCount += count >>> 16;
				out("Here is close par!");
			}
			rightp = node.getRightp();
			tokenCount++;
		}
		return tokenCount + (listCount << 16);
	}

	private int doListCounts(int rightp) {
		int tokenCount = 0;
		int listCount = 0;
		int count;
		Page page;
		int idx;
		Node node;
		int downp;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;

		while (rightp != 0) {
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			kwtyp = node.getKeywordTyp();
			celltyp = node.getDownCellTyp();
			out("rightp = " + rightp + ", idx = " + idx + 
				", kwd = " + kwtyp + ", celtyp = " + celltyp);
			if (node.isOpenPar()) {
				out("Here is (");
				listCount++;
				downp = node.getDownp();
				count = doListCounts(downp);
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
		Page page;
		int idx;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		boolean isNone;
		boolean isDo;
		int stmtNo = 0;
		int doBlkCount = 0;

		out("Top of isValidSrc");
		rightp = scan.getRootNodep();
		if (rightp == 0) {
			return false;
		}
		while (rightp != 0) {
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
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
			out("rightp = " + rightp + ", idx = " + idx + 
				", kwd = " + kwtyp + ", celtyp = " + celltyp);
			if (node.isOpenPar()) {
				doBlkCount++;
				if (doBlkCount > 1) {
					return false;
				}
				out("Here is open par!");
				downp = node.getDownp();
				out("root downp = " + downp + ", rightp = " +
					node.getRightp());
				count = doStmtCounts(downp);
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
		Page page;
		int idx;
		Node node;
		int downp;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int phaseNo = 0;

		while (rightp != 0) {
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			kwtyp = node.getKeywordTyp();
			celltyp = node.getDownCellTyp();
			out("rightp = " + rightp + ", idx = " + idx + 
				", kwd = " + kwtyp + ", celtyp = " + celltyp);
			if (node.isOpenPar()) {
				out("Here is (");
				downp = node.getDownp();
				phaseNo = doStmt(downp, phaseNo);
				if (phaseNo < 0) {
					return -1;
				}
				out("Here is )");
			}
			rightp = node.getRightp();
			if (isZparen) {
				out("Zparen found");
				continue;
			}
			if (phaseNo < 0) {
				return -1;
			}
			stmtCount++;
		}
		return stmtCount;
	}
	
	private int doStmt(int rightp, int phaseNo) {
		Page page;
		int idx;
		Node node;
		KeywordTyp kwtyp = null;
		NodeCellTyp celltyp;
		boolean first = true;
		boolean isZparen;
		int currPhaseNo = phaseNo;
		int rightq;
		int initp = rightp;
		String phaseDesc;

		while (rightp > 0) {
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			kwtyp = node.getKeywordTyp();
			celltyp = node.getDownCellTyp();
			out("rightp = " + rightp + ", idx = " + idx + 
					", kwd = " + kwtyp + ", celtyp = " + celltyp);
			if (first) {
				out("Statement kwd = " + kwtyp);
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
				if (rightq > 0) {
					rightp = rightq;
				}
				else {
					return -1;
				}
			}
			if (node.isOpenPar()) {
				out("Here is ()");
			}
			rightp = node.getRightp();
			first = false;
		}
		isZparen = (kwtyp == KeywordTyp.ZPAREN);
		return currPhaseNo;
	}
	
	private int getPhaseNo(KeywordTyp kwtyp) {
		switch (kwtyp) {
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
		case ZPAREN:
			return 9999;
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
	
	private int chkImportStmt(int rightp, KeywordTyp kwtyp) {
		if (kwtyp == KeywordTyp.IMPORT) {
			return chkImportKwd(rightp, true);
		}
		return chkFromKwd(rightp);
	}
	
	private int chkImportKwd(int rightp, boolean hasColonCase) {
		Page page;
		int idx;
		Node node, subNode;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int currp, ascurrp;
		int modCount = 0;
		int savep = rightp;

		while (rightp > 0) {
			modCount++;
			out("modCount = " + modCount);
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			kwtyp = node.getKeywordTyp();
			celltyp = node.getDownCellTyp();
			if (celltyp == NodeCellTyp.ID) {
				out("celltyp = ID");
			}
			else if (celltyp != NodeCellTyp.PTR) {
				oerr(rightp, "Keyword 'import' followed by invalid text: " +
					kwtyp.toString());
				return 0;
			}
			else if (hasColonCase && isColonListQuiet(node)) {
				out("celltyp = PTR, (: a b)");
			}
			else {
				currp = node.getDownp();
				page = store.getPage(currp);
				idx = store.getElemIdx(currp);
				subNode = page.getNode(idx);
				kwtyp = subNode.getKeywordTyp();
				if (kwtyp != KeywordTyp.AS) {
					oerr(currp, "Keyword 'import', expecting 'as', but was " +
						"followed by invalid text: " + kwtyp.toString());
					return 0;
				}
				ascurrp = currp;
				currp = subNode.getRightp();
				if (currp <= 0) {
					oerr(ascurrp, "Keyword 'import', then 'as' found, " +
						"not followed by module names");
					return 0;
				}
				page = store.getPage(currp);
				idx = store.getElemIdx(currp);
				subNode = page.getNode(idx);
				celltyp = subNode.getDownCellTyp();
				if (celltyp == NodeCellTyp.ID) { }
				else if (hasColonCase && celltyp == NodeCellTyp.PTR && 
					isColonList(subNode)) { } 
				else {
					oerr(ascurrp, "Keyword 'import', then 'as' found, " +
						"followed by invalid text");
					return 0;
				}
				currp = subNode.getRightp();
				if (currp <= 0) {
					oerr(ascurrp, "Keyword 'import', then 'as' clause found, " +
						"followed by invalid text");
					return 0;
				}
				page = store.getPage(currp);
				idx = store.getElemIdx(currp);
				subNode = page.getNode(idx);
				celltyp = subNode.getDownCellTyp();
				if (celltyp != NodeCellTyp.ID) {
					oerr(ascurrp, "Keyword 'import', then 'as' clause found, " +
						"not followed by identifier");
					return 0;
				}
				currp = subNode.getRightp();
				if (currp > 0) {
					oerr(ascurrp, "Keyword 'import', then 'as' clause found, " +
						"then identifier found, followed by invalid text");
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
		Page page;
		int idx;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int rightp;
		int colonp;
		int count = 0;
		
		rightp = node.getDownp();		
		if (rightp <= 0) {
			return false;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		if (kwtyp == KeywordTyp.DOT) { }
		else if (verbose) {
			oerr(rightp, "Expecting colon operator, instead found: " +
				kwtyp.toString());
			return false;
		}
		else {
			return false;
		}
		colonp = rightp;
		rightp = node.getRightp();
		while (rightp > 0) {
			count++;
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			celltyp = node.getDownCellTyp();
			if (celltyp == NodeCellTyp.ID) { }
			else if (verbose) {
				oerr(rightp, "Expecting identifier after colon operator, " +
					"invalid text found");
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
		oerr(colonp, "Expecting multiple identifiers after colon operator, " +
				"less than 2 found");
		return false;
	}
	
	private boolean isRelModList(Node node) {
		Page page;
		int idx;
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
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.DOT) {
			oerr(rightp, "Keyword 'from' needs dot operator");
			return false;
		}
		rightq = rightp;
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerr(rightq, "Keyword 'from', dot operator, followed by null pointer");
			return false;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		celltyp = node.getDownCellTyp();
		if (celltyp == NodeCellTyp.INT) {
			count++;
			rightp = node.getRightp();
		}
		while (rightp > 0) {
			count++;
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			celltyp = node.getDownCellTyp();
			if (celltyp != NodeCellTyp.ID) {
				oerr(rightp, "Keyword 'from', dot operator, " +
					"expecting list of identifiers");
				return false;
			}
			rightq = rightp;
			rightp = node.getRightp();
		}
		if (count <= 0) {
			oerr(rightq, "Keyword 'from', dot operator, no arguments found");
			return false;
		}
		return true;
	}
	
	private int chkFromKwd(int rightp) {
		Page page;
		int idx;
		Node node;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int savep = rightp;
		int rightq;

		if (rightp <= 0) {
			return -1;
		}
		rightq = rightp;
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		celltyp = node.getDownCellTyp();
		if (celltyp == NodeCellTyp.ID) {
			rightp = node.getRightp();
		}
		else if (isRelModList(node)) {
			rightp = node.getRightp();
		}
		else {
			oerr(rightq, "Keyword 'from' followed by incorrect text");
			return -1;
		}
		if (rightp <= 0) {
			oerr(rightq, "Keyword 'from' followed by null pointer");
			return -1;
		}
		rightq = rightp;
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.IMPORT) {
			oerr(rightp, "Keyword: 'from' missing 'import' clause");
			return -1;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			oerr(rightq, "Keyword 'import' in 'from' statement " +
				"encountered w/o list of modules");
			return -1;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		if (kwtyp == KeywordTyp.ALL) { 
			rightp = node.getRightp();
			if (rightp > 0) {
				oerr(rightp, "Keyword 'all' in 'from' statement followed by " +
					"unexpected text");
				return -1;
			}
			else {
				return savep;
			}
		}
		rightq = rightp;
		savep = chkImportKwd(rightp, false);
		if (savep <= 0) {
			oerr(rightq, "Keyword 'import' in 'from' statement " +
				"followed by incorrect text");
		}
		return savep;
	}
	
	private int chkGlbDefStmt(int rightp) {
		Page page;
		int idx;
		Node node, parNode;
		KeywordTyp kwtyp;
		int savep = rightp;
		int rightq = 0;
		int phaseNo = 0;
		int oldPhaseNo = 0;

		while (rightp > 0) {
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			parNode = page.getNode(idx);
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
				oerr(rightp, "Invalid keyword: " + kwtyp + " found in gdefun stmt.");
				return -1;
			}
			phaseNo = getGdefunPhase(kwtyp);
			if (phaseNo <= oldPhaseNo) {
				oerr(rightp, "Error in gdefun stmt.: keyword " + kwtyp +
					" encountered unexpectedly");
				return -1;
			}
			switch (phaseNo) {
			case 1:
			case 2:
				rightq = rightp;
				rightp = node.getRightp();
				if (chkVarList(rightp) < 0) {
					oerr(rightq, "Error in gdefun stmt.: invalid var list");
					return -1;
				}
				break;
			default:
				oerr(rightp, "Invalid keyword: (" + kwtyp + "...) found in gdefun stmt.");
				return -1;
			}
			oldPhaseNo = phaseNo;
			rightq = rightp;
			rightp = parNode.getRightp();
		}
		out("gdefun: chk DO");
		rightp = chkDoBlock(rightq);
		if (rightp != 0) {
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
		case CALL:
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
	
	private int chkVarList(int rightp) {
		Page page;
		int idx;
		Node node;
		NodeCellTyp celltyp;
		int count = 0;
		
		while (rightp > 0) {
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
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
		Page page;
		int idx;
		Node node, subNode;
		NodeCellTyp celltyp;
		int subRightp;
		int count = 0;
		
		while (rightp > 0) {
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			subNode = store.getSubNode(node);
			if (subNode != null) {
				subRightp = node.getDownp();
				if (chkVarList(subRightp) < 1) {
					oerr(rightp, "Invalid dot-call: terminating identifier list " +
						"has no identifiers");
					return -1;
				}
				rightp = node.getRightp();
				if (rightp > 0) {
					oerr(rightp, "Invalid dot-call: terminating identifier list " +
						"followed by unexpected text");
					return -1;
				}
				if (count < 1) {
					oerr(rightp, "Invalid dot-call: terminating identifier list " +
						"not preceded by identifier(s)");
					return -1;
				}
				return count + 1;
			}
			celltyp = node.getDownCellTyp();
			if (celltyp != NodeCellTyp.ID) {
				oerr(rightp, "Invalid dot-call: expecting identifier");
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
		Page page;
		int idx;
		Node node, parNode;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int subRightp;
		int phaseNo = 0;
		int oldPhaseNo = 0;
		int count = 0;
		
		while (rightp > 0) {
			count++;
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			parNode = page.getNode(idx);
			node = store.getSubNode(parNode);
			celltyp = parNode.getDownCellTyp();
			if (node != null) {
				kwtyp = node.getKeywordTyp();
				subRightp = node.getRightp();
			}
			else if (celltyp != NodeCellTyp.ID) {
				oerr(rightp, "Keyword or constant encountered, " +
					"identifier expected");
				return false;
			}
			else {
				kwtyp = KeywordTyp.NULL;
				subRightp = 0;
			}
			phaseNo = getParmPhase(kwtyp);
			if (phaseNo < 0) {
				oerr(rightp, "Invalid keyword: " + kwtyp.toString() +
					" encountered in parameter list");
				return false;
			}
			if (phaseNo < oldPhaseNo) {
				oerr(rightp, "Parameter list error: " +
					kwtyp.toString() + " encountered unexpectedly");
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
					oerr(rightp, "Invalid star-type parameter encountered " +
						"in parameter list");
					return false;
				}
				break;
			default:
				// dup. cond'n, case already handled after getParmPhase() call
				oerr(rightp, "Invalid keyword: " + kwtyp.toString() +
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
		Page page;
		int idx;
		Node node;
		NodeCellTyp celltyp;
		
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
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
		Page page;
		int idx;
		Node node;
		NodeCellTyp celltyp, inCellTyp;
		
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		celltyp = node.getDownCellTyp();
		if (isConstPair) {
			inCellTyp = NodeCellTyp.FUNC;
		}
		else {
			inCellTyp = NodeCellTyp.ID;
		}
		if (celltyp != inCellTyp) {
			oerr(rightp, "Error in default parameter: identifier not found");
			out("chkDefParm (): celltyp = " + celltyp);
			out("chkDefParm (): fail 0");
			return false;
		}
		rightp = node.getRightp();
		if (!isConstExpr(rightp)) {
			oerr(rightp, "Error in default parameter: constant expr. not found");
			out("chkDefParm (): fail 1");
			return false;
		}
		return true;
	}
	
	private boolean isConstExpr(int rightp) {
		Page page;
		int idx;
		Node node;
		NodeCellTyp celltyp;
		KeywordTyp kwtyp;
		
		if (rightp <= 0) {
			return false;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
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
	
	private boolean chkDecorList(int rightp) {
		Page page;
		int idx;
		Node node, parNode;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int subRightp;
		int count = 0;
		int n;
		
		while (rightp > 0) {
			count++;
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			parNode = page.getNode(idx);
			node = store.getSubNode(parNode);
			celltyp = parNode.getDownCellTyp();
			if (node != null) {
				kwtyp = node.getKeywordTyp();
				subRightp = node.getRightp();
			}
			else if (celltyp != NodeCellTyp.ID) {
				oerr(rightp, "Keyword or constant encountered, " +
					"identifier expected");
				return false;
			}
			else {
				kwtyp = KeywordTyp.NULL;
				subRightp = 0;
			}
			switch (kwtyp) {
			case NULL:
				break;
			case CALL:
				subRightp = parNode.getDownp();
				if (chkVarList(subRightp) < 2) {
					oerr(rightp, "Decor error: " +
						"under 2 identifiers encountered in list");
					return false;
				}
				break;
			case DOT:
				n = chkDotCall(subRightp);
				if (n >= 2) {
					break;
				}
				if (n >= 0) {
					oerr(rightp, "Decor error: " +
						"under 2 identifiers encountered in dot-list");
					// more error messages in chkDotCall
				}
				return false;
			default:
				oerr(rightp, "Invalid keyword: " + kwtyp.toString() +
					" encountered in decor list");
				return false;
			}
			rightp = parNode.getRightp();
			out("decor # " + count);
		}
		return true;
	}
	
	private int chkDoBlock(int rightp) {
		Page page;
		int idx;
		Node node;
		KeywordTyp kwtyp;
		int rightq = 0;
		
		if (rightp <= 0) {
			oerr(0, "Internal error: do-block handler was passed zero address");
			out("doBlock (): fail 0");
			return -1;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.DO) {
			oerr(rightp, "Internal error: do-block handler called, no DO keyword");
			out("doBlock (): fail 1");
			return -1;
		}
		rightq = node.getRightp();
		if (rightq > 0) {
			oerr(rightp, "Unexpected/invalid text encountered after " +
				"do-block/keyword of defun stmt.");
			out("doBlock (): fail 1.5");
			return -1;
		}
		if (!node.isOpenPar()) {  // never lands here
			oerr(rightp, "Do block error: body lacks parentheses");
			out("doBlock (): fail 2");
			return -1;
		}
		rightp = node.getDownp();
		if (rightp <= 0) {
			return 0; // OK
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		return 0; // OK
	}
	
	private int chkDefunStmt(int rightp) {
		Page page;
		int idx;
		Node node, parNode;
		KeywordTyp kwtyp;
		int savep = rightp;
		int rightq = 0;
		int phaseNo = 0;
		int oldPhaseNo = 0;
		boolean isParms = false;

		while (rightp > 0) {
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			parNode = page.getNode(idx);
			node = store.getSubNode(parNode);
			if (node != null) {
				kwtyp = node.getKeywordTyp();
			}
			else if (parNode.getKeywordTyp() == KeywordTyp.DO) {
				rightq = rightp;
				break;
			}
			else {
				oerr(rightp, "Invalid text encountered in defun stmt. header");
				out("chkDefunStmt (): fail 0");
				return -1;
			}
			rightp = node.getRightp();
			phaseNo = getDefunPhase(kwtyp);
			if (phaseNo < 0) {
				oerr(rightp, "Invalid keyword " + kwtyp.toString() + 
					" encountered in defun stmt.");
				out("chkDefunStmt (): fail 0.5");
				return -1;
			}
			if (phaseNo <= oldPhaseNo) {
				oerr(rightp, "Defun statement header error: " +
					kwtyp.toString() + " encountered unexpectedly");
				out("chkDefunStmt (): fail 1");
				return -1;
			}
			if (phaseNo > 1 && !isParms) {
				oerr(rightp, "Defun statement header error: " +
					"missing function name/parameters");
				out("chkDefunStmt (): fail 2");
				return -1;
			}
			switch (phaseNo) {
			case 1:
				if (!chkParmList(rightp)) {
					oerr(rightp, "Error in parm list of defun stmt.");
					out("chkDefunStmt (): fail 3");
					return -1;
				}
				isParms = true;
				break;
			case 2:
			case 3:
				if (chkVarList(rightp) < 0) {
					oerr(rightp, "Error in var list of defun stmt.: " +
						"invalid text found when processing identifier list");
					out("chkDefunStmt (): fail 4");
					return -1;
				}
				break;
			case 4:
				if (!chkDecorList(rightp)) {
					oerr(rightp, "Error in decor list of defun stmt.");
					out("chkDefunStmt (): fail 5");
					return -1;
				}
				break;
			default:
				oerr(rightp, "Invalid keyword encountered in " +
					"defun stmt. header");
				out("chkDefunStmt (): fail 6");
				return -1;
			}
			oldPhaseNo = phaseNo;
			rightq = rightp;
			rightp = parNode.getRightp();
		}
		if (!isParms) {
			oerr(rightq, "Defun statement header error: " +
				"missing function name/parameters, post-loop");
			out("chkDefunStmt (): fail 7");
			return -1;
		}
		out("defun: chk DO");
		rightp = chkDoBlock(rightq);
		if (rightp < 0) {
			oerr(rightq, "Error in do-block of defun stmt.");
			out("chkDefunStmt (): fail 8");
			return -1;
		}
		return savep;
	}
	
	private int chkAbDefunStmt(int rightp) {
		Page page;
		int idx;
		Node node, parNode;
		KeywordTyp kwtyp;
		int savep = rightp;
		int phaseNo = 0;
		int oldPhaseNo = 0;
		boolean isParms = false;

		while (rightp > 0) {
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			parNode = page.getNode(idx);
			node = store.getSubNode(parNode);
			if (node != null) {
				kwtyp = node.getKeywordTyp();
			}
			else if (parNode.getKeywordTyp() == KeywordTyp.DO) {
				break;
			}
			else {
				return -1;
			}
			rightp = node.getRightp();
			phaseNo = getDefunPhase(kwtyp);
			if (phaseNo <= oldPhaseNo) {
				return -1;
			}
			if (phaseNo > 1 && !isParms) {
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
				return -1;
			case 4:
				if (!chkDecorList(rightp)) {
					return -1;
				}
				break;
			default:
				return -1;
			}
			oldPhaseNo = phaseNo;
			rightp = parNode.getRightp();
		}
		if (!isParms) {
			return -1;
		}
		out("abdefun: end loop");
		//rightp = chkDoBlock(rightp);
		if (rightp != 0) {
			return -1;
		}
		return savep;
	}
	
	private int chkClassStmt(int rightp, KeywordTyp kwtyp) {
		Page page;
		int idx;
		Node node, parNode;
		int savep = rightp;
		int phaseNo = 0;
		int oldPhaseNo = 0;
		boolean isNameFound = false;
		boolean isDotOK = true;
		boolean isAbCls = (kwtyp == KeywordTyp.ABCLASS);

		switch (kwtyp) {
		case SCOOL:
		case ISCOOL:
			return chkScoolStmt(rightp);
		case ENUM:
		case IENUM:
			return chkEnumStmt(rightp);
		default:
			break;
		}
		while (rightp > 0) {
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			parNode = page.getNode(idx);
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
				out("chkClassStmt (): fail 0");
				return -1;
			}
			phaseNo = getClassPhase(kwtyp, oldPhaseNo);
			if (phaseNo <= oldPhaseNo) {
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
				isDotOK = isValidDoesList(rightp);
				break;
			case 4:
			case 5:
				rightp = node.getRightp();
				if (chkVarList(rightp) < 0) {
					out("chkClassStmt (): fail 2");
					return -1;
				}
				break;
			default:
				out("chkClassStmt (): fail 3");
				return -1;
			}
			if (!isNameFound && (phaseNo > 1)) {
				out("chkClassStmt (): fail 4");
				oerr(rightp, "Class definition: Name of class not found");
				return -1;
			}
			if (!isDotOK) {
				out("chkClassStmt (): fail 5");
				return -1;
			}
			oldPhaseNo = phaseNo;
			rightp = parNode.getRightp();
		}
		out("class: chk DO");
		rightp = chkDoDefBlock(rightp, isAbCls);
		if (rightp != 0) {
			out("chkClassStmt (): fail 6");
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
		Page page;
		int idx;
		Node node;
		Node parNode;
		int savep = rightp;
		int phaseNo = 0;
		int oldPhaseNo = 0;
		boolean isNameFound = false;
		KeywordTyp kwtyp = null;

		while (rightp > 0) {
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			parNode = page.getNode(idx);
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
				out("chkScoolStmt (): fail 0");
				return -1;
			}
			phaseNo = getScoolPhase(kwtyp, oldPhaseNo);
			if (phaseNo <= oldPhaseNo) {
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
				if (!isValidDoesList(rightp)) {
					out("chkScoolStmt (): fail 2");
					return -1;
				}
				break;
			case 3:
				// const
				rightp = node.getRightp();
				if (!isValidConstList(rightp)) {
					out("chkScoolStmt (): fail 3");
					return -1;
				}
				break;
			default:
				break;
			}
			if (!isNameFound && (phaseNo > 1)) {
				out("chkScoolStmt (): fail 4");
				return -1;
			}
			oldPhaseNo = phaseNo;
			rightp = parNode.getRightp();
		}
		out("scool: chk DO");
		rightp = chkScoolDoBlock(rightp);
		if (rightp != 0) {
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
	
	private boolean isValidConstList(int rightp) {
		Page page;
		int idx;
		Node node;
		int downp;
		int count = 0;
		
		while (rightp > 0) {
			count++;
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			downp = node.getDownp();
			if (downp == 0 || !isValidConstPair(downp)) {
				if (downp == 0) {
					out("isValidConstList (): zero downp!");
				}
				out("isValidConstList (): count = " + count);
				out("isValidConstList (): fail 1");
				return false;
			}
			rightp = node.getRightp();
		}
		out("isValidConstList (): bottom");
		return count > 0;
	}
			
	private int chkEnumStmt(int rightp) {
		Page page;
		int idx;
		Node node, subNode;
		int savep = rightp;
		int count = 0;
		int enumTyp = -1;  // 0: id, 1: int, 2: char
		int etyp;
		
		if (rightp == 0) {
			return -1;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		if (node.getDownCellTyp() != NodeCellTyp.ID) {
			return -1;
		} 
		rightp = node.getRightp();
		while (rightp > 0) {
			count++;
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			subNode = store.getSubNode(node);
			if (subNode != null) {
				etyp = getEnumPair(subNode);
			}
			else {
				etyp = getEnumTyp(node);
			}
			if (etyp < 0) {
				return -1;
			}
			if (enumTyp < 0) {
				enumTyp = etyp;
			}
			else if (etyp != enumTyp) {
				return -1;
			}
			rightp = node.getRightp();
		}
		if (count == 0) {
			return -1;
		}
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
	
	private int getEnumPair(Node node) {
		Page page;
		int idx;
		int enumTyp, etyp;
		int rightp;
		
		if (node.getKeywordTyp() != KeywordTyp.DOT) {
			return -1;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			out("getEnumPair (): fail 0");
			return -1;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		enumTyp = getEnumTyp(node);
		rightp = node.getRightp();
		if (rightp <= 0) {
			out("getEnumPair (): fail 0.5");
			return -1;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		etyp = getEnumTyp(node);
		rightp = node.getRightp();
		if (rightp > 0) {
			out("getEnumPair (): fail 1");
			return -1;
		}
		if (etyp != enumTyp) {
			out("getEnumPair (): fail 2");
			return -1;
		}
		out("getEnumPair() = " + enumTyp);
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
	
	private boolean isValidDoesList(int rightp) {
		Page page;
		int idx;
		Node node, subNode;
		KeywordTyp kwtyp;
		int subRightp;
		int count = 0;
		boolean isValid;

		while (rightp > 0) {
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			subNode = store.getSubNode(node);
			if (node.getDownCellTyp() == NodeCellTyp.ID) {} 
			else if (subNode == null) {
				out("isValidDoesList (): fail 0");
				return false;
			}
			else {
				kwtyp = subNode.getKeywordTyp();
				if (kwtyp != KeywordTyp.DOT) {
					out("isValidDoesList (): fail 1");
					return false;
				}
				subRightp = subNode.getRightp();
				if (!isValidDotList(subRightp)) {
					out("isValidDoesList (): fail 2");
					return false;
				}
			}
			count++;
			rightp = node.getRightp();
		}
		isValid = (count >= 1);
		if (!isValid) {
			out("isValidDoesList (): fail 3");
		}
		return isValid;
	}
	
	private int chkDoDefBlock(int rightp, boolean isAbCls) {
		Page page;
		int idx;
		Node node, subNode;
		KeywordTyp kwtyp;
		int downp, downq;
		int phaseNo;
		boolean abfound;
		
		if (rightp <= 0) {
			out("doDefBlock (): fail 0");
			return -1;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.DO) {
			out("doDefBlock (): fail 1");
			return -1;
		}
		if (!node.isOpenPar()) {
			out("doDefBlock (): fail 1.5");
			return -1;
		}
		rightp = node.getRightp();
		if (rightp > 0) {
			out("doDefBlock (): fail 2");
			return -1;
		}
		rightp = node.getDownp();
		while (rightp > 0) {
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			if (!node.isOpenPar()) {
				out("doDefBlock (): fail 5");
				return -1;
			}
			downp = node.getDownp();
			page = store.getPage(downp);
			idx = store.getElemIdx(downp);
			subNode = page.getNode(idx);
			kwtyp = subNode.getKeywordTyp();
			if (kwtyp == KeywordTyp.ZPAREN) {
				break;
			}
			phaseNo = getPhaseNo(kwtyp);
			switch (phaseNo) {
			case 3:
				abfound = false;
				break;
			case ABPHASE:
				abfound = true;
				break;
			default:
				out("doDefBlock (): fail 3");
				return -1;
			}
			out("doDefBlock (): OK - 3");
			downp = subNode.getRightp();
			if (abfound && isAbCls) {
				downq = chkAbDefunStmt(downp);
			}
			else if (abfound) {
				out("doDefBlock (): fail 4");
				return -1;
			}
			else {
				downq = chkDefunStmt(downp);
			}
			if (downq <= 0) {
				out("doDefBlock (): fail 5");
				return -1;
			}
			rightp = node.getRightp();
		}
		return 0; // OK
	}
	
	private int chkScoolDoBlock(int rightp) {
		Page page;
		int idx;
		Node node, subNode;
		KeywordTyp kwtyp;
		int downp, downq;
		
		if (rightp <= 0) {
			out("doScoolBlock (): fail 0");
			return -1;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.DO) {
			out("doScoolBlock (): fail 1");
			return -1;
		}
		if (!node.isOpenPar()) {
			out("doScoolBlock (): fail 1.5");
			return -1;
		}
		rightp = node.getRightp();
		if (rightp > 0) {
			out("doScoolBlock (): fail 2");
			return -1;
		}
		rightp = node.getDownp();
		while (rightp > 0) {
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			if (!node.isOpenPar()) {
				out("doScoolBlock (): fail 3");
				return -1;
			}
			downp = node.getDownp();
			page = store.getPage(downp);
			idx = store.getElemIdx(downp);
			subNode = page.getNode(idx);
			kwtyp = subNode.getKeywordTyp();
			if (kwtyp == KeywordTyp.ZPAREN) {
				break;
			}
			downp = subNode.getRightp();
			if (downp == 0) {
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
				out("doScoolBlock (): fail 5");
				return -1;
			}
			out("doScoolBlock (): OK - 5");
			if (downq <= 0) {
				out("doScoolBlock (): fail 6");
				return -1;
			}
			rightp = node.getRightp();
		}
		return 0; // OK
	}
	
}
