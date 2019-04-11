package synchk;

import iconst.KeywordTyp;
import iconst.NodeCellTyp;
import page.Node;
import page.Page;
import page.Store;
import scancoop.ScanCoop;

public class SynChk {
	
	private ScanCoop scan;
	private Store store;
	private boolean isZparen;

	public SynChk(ScanCoop scan, Store store) {
		this.scan = scan;
		this.store = store;
	}
	
	private void out(String msg) {
		scan.out(msg);
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
	
	public boolean isValidCoop() {
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

		out("Top of isValidCoop");
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
		boolean isGlbDef = false;
		int currPhaseNo = phaseNo;
		int rightq;

		while (rightp != 0) {
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
				if (currPhaseNo < phaseNo) {
					return -1;
				}
				rightq = rightp;
				rightp = node.getRightp();
				switch (currPhaseNo) {
				case 1:
					rightq = chkImportStmt(rightp, kwtyp);
					break;
				case 2:
					if (isGlbDef) {
						rightq = 0;
					}
					else {
						rightq = chkGlbDefStmt(rightp);
						isGlbDef = true;
					}
					break;
				case 3:
					rightq = chkDefunStmt(rightp);
					break;
				case 4:
					rightq = chkClassStmt(rightp, kwtyp);
					break;
				default:
					if (currPhaseNo <= 0) {
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
		case ZPAREN:
			return 9999;
		default:
			return -1;
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
		int currp;
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
				return 0;
			}
			else if (hasColonCase && isColonList(node)) {
				out("celltyp = PTR, (: a b)");
			}
			else {
				currp = node.getDownp();
				page = store.getPage(currp);
				idx = store.getElemIdx(currp);
				subNode = page.getNode(idx);
				kwtyp = subNode.getKeywordTyp();
				if (kwtyp != KeywordTyp.AS) {
					return 0;
				}
				currp = subNode.getRightp();
				if (currp <= 0) {
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
					return 0;
				}
				currp = subNode.getRightp();
				if (currp <= 0) {
					return 0;
				}
				page = store.getPage(currp);
				idx = store.getElemIdx(currp);
				subNode = page.getNode(idx);
				celltyp = subNode.getDownCellTyp();
				if (celltyp != NodeCellTyp.ID) {
					return 0;
				}
				currp = subNode.getRightp();
				if (currp > 0) {
					return 0;
				}
			}
			rightp = node.getRightp();
		}
		if (modCount <= 0) {
			return 0;
		}
		return savep;
	}
	
	private boolean isColonList(Node node) {
		Page page;
		int idx;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int rightp;
		int count = 0;
		
		rightp = node.getDownp();		
		if (rightp <= 0) {
			return false;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.DOT) {
			return false;
		}
		rightp = node.getRightp();
		while (rightp > 0) {
			count++;
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			celltyp = node.getDownCellTyp();
			if (celltyp != NodeCellTyp.ID) {
				return false;
			}
			rightp = node.getRightp();
		}
		if (count < 2) {
			return false;
		}
		return true;
	}
	
	private boolean isRelModList(Node node) {
		Page page;
		int idx;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int rightp;
		int count = 0;
		
		rightp = node.getDownp();		
		if (rightp <= 0) {
			return false;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.DOT) {
			return false;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
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
				return false;
			}
			rightp = node.getRightp();
		}
		if (count <= 0) {
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

		if (rightp <= 0) {
			return -1;
		}
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
			return -1;
		}
		if (rightp <= 0) {
			return -1;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.IMPORT) {
			return -1;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			return -1;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		if (kwtyp == KeywordTyp.ALL) { 
			rightp = node.getRightp();
			if (rightp > 0) {
				return -1;
			}
			else {
				return savep;
			}
		}
		return chkImportKwd(rightp, false);
	}
	
	private int chkGlbDefStmt(int rightp) {
		Page page;
		int idx;
		Node node, parNode;
		KeywordTyp kwtyp;
		int savep = rightp;
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
				break;
			}
			else {
				return -1;
			}
			phaseNo = getGdefunPhase(kwtyp);
			if (phaseNo <= oldPhaseNo) {
				return -1;
			}
			switch (phaseNo) {
			case 1:
			case 2:
				rightp = node.getRightp();
				if (chkVarList(rightp) < 0) {
					return -1;
				}
				break;
			default:
				return -1;
			}
			oldPhaseNo = phaseNo;
			rightp = parNode.getRightp();
		}
		out("gdefun: chk DO");
		rightp = chkDoBlock(rightp);
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
		case DECOR:
			return 3;
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
					return -1;
				}
				rightp = node.getRightp();
				if (rightp > 0) {
					return -1;
				}
				if (count < 1) {
					return -1;
				}
				return count + 1;
			}
			celltyp = node.getDownCellTyp();
			if (celltyp != NodeCellTyp.ID) {
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
			if (node != null) {
				kwtyp = node.getKeywordTyp();
				subRightp = node.getRightp();
			}
			else {
				kwtyp = KeywordTyp.NULL;
				subRightp = 0;
			}
			phaseNo = getParmPhase(kwtyp);
			if (phaseNo < oldPhaseNo) {
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
					return false;
				}
				break;
			default:
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
		if (!isConstExpr(rightp)) {
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
		int subRightp;
		int count = 0;
		
		while (rightp > 0) {
			count++;
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			parNode = page.getNode(idx);
			node = store.getSubNode(parNode);
			if (node != null) {
				kwtyp = node.getKeywordTyp();
				subRightp = node.getRightp();
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
					return false;
				}
				break;
			case DOT:
				if (chkDotCall(subRightp) < 2) {
					return false;
				}
				break;
			default:
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
		
		if (rightp <= 0) {
			out("doBlock (): fail 0");
			return -1;
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.DO) {
			out("doBlock (): fail 1");
			return -1;
		}
		if (!node.isOpenPar()) {
			out("doBlock (): fail 1.5");
			return -1;
		}
		rightp = node.getRightp();
		if (rightp > 0) {
			out("doBlock (): fail 2");
			return -1;
		}
		rightp = node.getDownp();
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		if (!node.isOpenPar()) {
			out("doBlock (): fail 3");
			return -1;
		}
		return 0;
	}
	
	private int chkDefunStmt(int rightp) {
		return doDefunStmt(rightp, false);
	}
	
	@SuppressWarnings("unused")
	private int chkAbDefunStmt(int rightp) {
		return doDefunStmt(rightp, true);
	}
	
	private int doDefunStmt(int rightp, boolean isAbDef) {
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
			else if (isAbDef) {
				return -1;
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
				if (isAbDef) {
					return -1;
				}
				if (chkVarList(rightp) < 0) {
					return -1;
				}
				break;
			case 3:
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
		if (isAbDef) {
			return savep;
		}
		out("gdefun: chk DO");
		rightp = chkDoBlock(rightp);
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
		rightp = chkDoDefBlock(rightp);
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
	
	private int chkDoDefBlock(int rightp) {
		Page page;
		int idx;
		Node node, subNode;
		KeywordTyp kwtyp;
		int downp, downq;
		int phaseNo;
		
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
			if (node.isOpenPar()) {
				downp = node.getDownp();
				page = store.getPage(downp);
				idx = store.getElemIdx(downp);
				subNode = page.getNode(idx);
				kwtyp = subNode.getKeywordTyp();
				if (kwtyp == KeywordTyp.ZPAREN) {
					break;
				}
				phaseNo = getPhaseNo(kwtyp);
				if (phaseNo != 3) {
					//out("phaseNo = " + phaseNo + ", kwtyp = " + kwtyp);
					out("doDefBlock (): fail 3");
					return -1;
				}
				else {
					out("doDefBlock (): OK - 3");
				}
				downp = subNode.getRightp();
				downq = chkDefunStmt(downp);
				if (downq <= 0) {
					out("doDefBlock (): fail 4");
					return -1;
				}
			}
			else {
				out("doDefBlock (): fail 5");
				return -1;
			}
			rightp = node.getRightp();
		}
		return 0; // OK
	}
	
}
