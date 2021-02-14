package runtm;

import iconst.IConst;
import iconst.TokenTyp;
import iconst.KeywordTyp;
import iconst.BifTyp;
import iconst.NodeCellTyp;
import iconst.SysFnTyp;
import page.Node;
import page.AddrNode;
import page.Store;
import page.Page;
import scansrc.ScanSrc;
import synchk.SynChk;
import java.util.HashMap;
import java.util.ArrayList;

// Code Execution

public class RunTime implements IConst {

	private Store store;
	private ScanSrc scanSrc;
	private SynChk synChk;
	private static final boolean isSilent = false;
	private int rootNodep;
	private HashMap<String, Integer> glbFunMap;
	private HashMap<String, Integer> glbLocVarMap;
	private ArrayList<Integer> glbLocVarList;
	private ArrayList<AddrNode> locVarList;
	private String scopeFuncName;
	private int defunCount;
	private int count;

	public RunTime(Store store, ScanSrc scanSrc, SynChk synChk,	int rootNodep) {
		this.store = store;
		this.scanSrc = scanSrc;
		this.synChk = synChk;
		this.rootNodep = rootNodep;
		glbFunMap = new HashMap<String, Integer>();
		glbLocVarMap = new HashMap<String, Integer>();
		glbLocVarList = new ArrayList<Integer>();
		locVarList = new ArrayList<AddrNode>();
		defunCount = 0;
		count = 0;
	}

	public boolean run() {
		boolean rtnval;
		omsg("RunTime.run: rootNodep = " + rootNodep);
		rtnval = runRoot(rootNodep);
		if (!rtnval) {
			omsg("Runtime error detected!");
		}
		return rtnval;
	}
	
	public void out(String msg) {
		if (debug) {
			System.out.println(msg);
		}
	}
	
	public void omsg(String msg) {  
		if (!isSilent) {
			System.out.println(msg);
		}
	}
	
	private boolean runRoot(int rightp) {
		int downp;
		Node node;
		boolean rtnval;
		
		node = store.getNode(rightp);
		rightp = node.getRightp();
		node = store.getNode(rightp);
		downp = node.getDownp();
		rtnval = scanTopBlock(downp);
		if (!rtnval) {
			return false;
		}
		rtnval = scopeTopBlock(downp);
		if (!rtnval) {
			return false;
		}
		rtnval = runTopBlock(downp);
		return rtnval;
	}
	
	private boolean scanTopBlock(int rightp) {
		// process top-level stmts.
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
				return false;
			}
			if (node.isOpenPar()) {
				out("Here is (");
				downp = node.getDownp();
				phaseNo = scanTopStmt(downp, phaseNo);
				if (phaseNo < 0) {
					return false;
				}
				out("Here is )");
			}
			else {
				return false;
			}
			rightp = node.getRightp();
			if (phaseNo < 0) {
				return false;
			}
		}
		return true;
	}
	
	private boolean scopeTopBlock(int rightp) {
		// process top-level stmts.
		// scope = scan + code
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
				return false;
			}
			if (node.isOpenPar()) {
				out("Here is (");
				downp = node.getDownp();
				phaseNo = scopeTopStmt(downp, phaseNo);
				if (phaseNo < 0) {
					return false;
				}
				out("Here is )");
			}
			else {
				return false;
			}
			rightp = node.getRightp();
			if (phaseNo < 0) {
				return false;
			}
		}
		return true;
	}
	
	private boolean runTopBlock(int rightp) {
		// process top-level stmts.
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
				return false;
			}
			if (node.isOpenPar()) {
				out("Here is (");
				downp = node.getDownp();
				phaseNo = runTopStmt(downp, phaseNo);
				if (phaseNo < 0) {
					return false;
				}
				out("Here is )");
			}
			else {
				return false;
			}
			rightp = node.getRightp();
			if (phaseNo < 0) {
				return false;
			}
		}
		return true;
	}
	
	private int scanTopStmt(int rightp, int phaseNo) {
		// process top-level statement
		// return phase no. of current stmt.: 0=quest, 1=import,
		//   gdefun, functions, 4=classes
		// return -1 on error
		Node node;
		KeywordTyp kwtyp = null;
		NodeCellTyp celltyp;
		boolean first = true;
		int currPhaseNo = phaseNo;
		int rightq;

		while (rightp > 0) {
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			celltyp = node.getDownCellTyp();
			out("rightp = " + rightp +  
					", kwd = " + kwtyp + ", celtyp = " + celltyp);
			if (first) {
				// at keyword token, beginning of top-level stmt.
				out("Statement kwd = " + kwtyp);
				currPhaseNo = synChk.getPhaseNo(kwtyp);
				rightq = rightp;
				rightp = node.getRightp();
				if (rightp <= 0) {
					return -1;
				}
				// rightp > 0 inside following switch
				switch (currPhaseNo) {
				case 0:
					return -1;
				case 1:
					rightq = scanImportStmt(rightp, kwtyp);
					break;
				case 2:
					rightq = scanGlbDefStmt(rightp);
					break;
				case 3:
					rightq = scanDefunStmt(rightp);
					break;
				case 4:
					rightq = scanClassStmt(rightp, kwtyp);
					break;
				default:
					rightq = -1;
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
	
	private int scopeTopStmt(int rightp, int phaseNo) {
		// process top-level statement
		// return phase no. of current stmt.: 0=quest, 1=import,
		//   gdefun, functions, 4=classes
		// return -1 on error
		Node node;
		KeywordTyp kwtyp = null;
		NodeCellTyp celltyp;
		boolean first = true;
		int currPhaseNo = phaseNo;
		int rightq;

		while (rightp > 0) {
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			celltyp = node.getDownCellTyp();
			out("rightp = " + rightp +  
					", kwd = " + kwtyp + ", celtyp = " + celltyp);
			if (first) {
				// at keyword token, beginning of top-level stmt.
				out("Statement kwd = " + kwtyp);
				currPhaseNo = synChk.getPhaseNo(kwtyp);
				rightq = rightp;
				rightp = node.getRightp();
				if (rightp <= 0) {
					return -1;
				}
				// rightp > 0 inside following switch
				switch (currPhaseNo) {
				case 0:
					return -1;
				case 1:
					rightq = scopeImportStmt(rightp, kwtyp);
					break;
				case 2:
					rightq = scopeGlbDefStmt(rightp);
					break;
				case 3:
					rightq = scopeDefunStmt(rightp);
					break;
				case 4:
					rightq = scopeClassStmt(rightp, kwtyp);
					break;
				default:
					rightq = -1;
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
	
	private int runTopStmt(int rightp, int phaseNo) {
		// process top-level statement
		// return phase no. of current stmt.: 0=quest, 1=import,
		//   gdefun, functions, 4=classes
		// return -1 on error
		Node node;
		KeywordTyp kwtyp = null;
		NodeCellTyp celltyp;
		boolean first = true;
		int currPhaseNo = phaseNo;
		int rightq;

		while (rightp > 0) {
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			celltyp = node.getDownCellTyp();
			out("rightp = " + rightp +  
					", kwd = " + kwtyp + ", celtyp = " + celltyp);
			if (first) {
				// at keyword token, beginning of top-level stmt.
				out("Statement kwd = " + kwtyp);
				currPhaseNo = synChk.getPhaseNo(kwtyp);
				rightq = rightp;
				rightp = node.getRightp();
				if (rightp <= 0) {
					return -1;
				}
				// rightp > 0 inside following switch
				switch (currPhaseNo) {
				case 0:
					return -1;
				case 1:
				case 3:
				case 4:
					rightq = rightp;
					break;
				case 2:
					rightq = runGlbDefStmt(rightp);
					break;
				default:
					rightq = -1;
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
	
	private int scanImportStmt(int rightp, KeywordTyp kwtyp) {
		return rightp;
	}
	
	private int scopeImportStmt(int rightp, KeywordTyp kwtyp) {
		return rightp;
	}
	
	private int scanGlbDefStmt(int rightp) {
		Node node;
		Node firstNode;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int downp;
		int savep = rightp;
		String varName;
		int varidx = 0;
		int idx;
		Page page;

		omsg("Keyword gdefun detected.");
		glbFunMap.put(getGdefunWord(), defunCount++);
		node = store.getNode(rightp);
		firstNode = node;
		kwtyp = node.getKeywordTyp();
		if (kwtyp == KeywordTyp.ZPAREN) {
			rightp = node.getDownp();
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp != KeywordTyp.VAR) {
				return -1;
			}
			rightp = node.getRightp();
			while (rightp > 0) {
				page = store.getPage(rightp);
				idx = store.getElemIdx(rightp);
				node = page.getNode(idx);
				celltyp = node.getDownCellTyp();
				if (celltyp != NodeCellTyp.ID) {
					return -1;
				}
				downp = node.getDownp();
				varName = getGdefunWord() + ' ';
				varName += store.getVarName(downp);
				glbLocVarMap.put(varName, varidx++);
				glbLocVarList.add(rightp);
				rightp = node.getRightp();
				node.setRightp(downp);
				node.setDownp(0);
				node.setRightCell(true);
				page.setNode(idx, node);
			}
			rightp = firstNode.getRightp();
			omsg("Global public var count = " + varidx);
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp == KeywordTyp.ZPAREN) {  // (ivar ...)
				rightp = node.getRightp();
				if (rightp <= 0) {
					return -1;
				}
				node = store.getNode(rightp);
			}
			kwtyp = node.getKeywordTyp();
		}
		if (kwtyp != KeywordTyp.DO) {
			omsg("Missing DO");
			return -1;
		}
		return savep;
	}
	
	private int scopeGlbDefStmt(int rightp) {
		Node node;
		Node firstNode;
		KeywordTyp kwtyp;
		int downp;
		int savep = rightp;
		int stmtCount = 0;
		boolean rtnval;

		omsg("Keyword gdefun detected.");
		scopeFuncName = getGdefunWord();
		node = store.getNode(rightp);
		firstNode = node;
		kwtyp = node.getKeywordTyp();
		if (kwtyp == KeywordTyp.ZPAREN) {
			rightp = node.getDownp();
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp != KeywordTyp.VAR) {
				return -1;
			}
			rightp = firstNode.getRightp();
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp == KeywordTyp.ZPAREN) {  // (ivar ...)
				rightp = node.getRightp();
				if (rightp <= 0) {
					return -1;
				}
				node = store.getNode(rightp);
			}
			kwtyp = node.getKeywordTyp();
		}
		if (kwtyp != KeywordTyp.DO) {
			omsg("Missing DO");
			return -1;
		}
		rightp = node.getDownp();
		while (rightp > 0) {
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp != KeywordTyp.ZSTMT) {
				return -1;
			}
			omsg("Stmt count = " + stmtCount);
			downp = node.getDownp();
			rtnval = scopeStmt(downp);
			if (!rtnval) {
				return -1;
			}
			stmtCount++;
			rightp = node.getRightp();
		} 
		omsg("Stmt count = " + stmtCount + ", set count = " + count);
		return savep;
	}
	
	private int runGlbDefStmt(int rightp) {
		Node node;
		Node firstNode;
		KeywordTyp kwtyp;
		int downp;
		int savep = rightp;
		int stmtCount = 0;
		boolean rtnval;

		omsg("Keyword gdefun detected again.");
		node = store.getNode(rightp);
		firstNode = node;
		kwtyp = node.getKeywordTyp();
		if (kwtyp == KeywordTyp.ZPAREN) {
			rightp = node.getDownp();
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp != KeywordTyp.VAR) {
				return -1;
			}
			rightp = firstNode.getRightp();
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp == KeywordTyp.ZPAREN) {  // (ivar ...)
				rightp = node.getRightp();
				if (rightp <= 0) {
					return -1;
				}
				node = store.getNode(rightp);
			}
			kwtyp = node.getKeywordTyp();
		}
		if (kwtyp != KeywordTyp.DO) {
			omsg("Missing DO");
			return -1;
		}
		rightp = node.getDownp();
		while (rightp > 0) {
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp != KeywordTyp.ZSTMT) {
				return -1;
			}
			downp = node.getDownp();
			rtnval = runStmt(downp);
			if (!rtnval) {
				return -1;
			}
			stmtCount++;
			rightp = node.getRightp();
		} 
		omsg("Stmt count = " + stmtCount + ", set count = " + count);
		return savep;
	}
	
	private boolean scopeStmt(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		switch (kwtyp) {
		case SET: return scopeSetStmt(node);
		case PRINTLN: return scopePrintlnStmt(node);
		case ZCALL: return scopeZcallStmt(node);
		default: return false;
		}
	}
	
	private boolean runStmt(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		int rtnval;
		
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		switch (kwtyp) {
		case SET: 
			rtnval = runSetStmt(node);
			break;
		case PRINTLN: 
			rtnval = runPrintlnStmt(node);
			break;
		case ZCALL:
			rtnval = runZcallStmt(node);
			break;
		default: return false;
		}
		if (rtnval < 0) {
			omsg("runStmt: err code = " + rtnval);
		}
		return (rtnval >= 0);
	}
	
	private boolean scopeSetStmt(Node node) {
		int rightp;
		boolean rtnval;
		
		count++;
		rightp = node.getRightp();
		if (rightp <= 0) {
			return false;
		}
		node = store.getNode(rightp);
		rtnval = scopeLocVar(rightp);
		return rtnval;
	}
	
	private boolean scopePrintlnStmt(Node node) {
		int rightp;
		boolean rtnval;

		rightp = node.getRightp();
		while (rightp > 0) {
			node = store.getNode(rightp);
			rtnval = scopeLocVar(rightp);
			if (!rtnval) {
				return false;
			}
			rightp = node.getRightp();
		}
		return true;
	}
	
	private boolean scopeZcallStmt(Node node) {
		return true;
	}
	
	private boolean scopeLocVar(int rightp) {
		int downp;
		Page page;
		int idx;
		Node node;
		NodeCellTyp celltyp;
		String varName;
		Integer value;
		int varidx;
		
		node = store.getNode(rightp);
		celltyp = node.getDownCellTyp();
		if (celltyp != NodeCellTyp.ID) {
			return false;
		}
		downp = node.getDownp();
		varName = scopeFuncName + ' ';
		varName += store.getVarName(downp);
		value = glbLocVarMap.get(varName);
		if (value == null) {
			return false;
		}
		varidx = (int)value;
		node.setDownCellTyp(NodeCellTyp.LOCVAR.ordinal());
		node.setDownp(varidx);
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		page.setNode(idx, node);
		omsg("LocVar = " + varidx);
		return true;
	}
	
	private int scanDefunStmt(int rightp) {
		Node node;
		Node upNode;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int downp;
		int savep = rightp;
		String funcName;
		String varName;
		int varidx = 0;
		int idx;
		Page page;

		omsg("Keyword defun detected.");
		node = store.getNode(rightp);
		upNode = node;
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.ZPAREN) {
			return -1;
		}
		rightp = node.getDownp();
		node = store.getNode(rightp);
		celltyp = node.getDownCellTyp();
		if (celltyp != NodeCellTyp.FUNC) {
			return -1;
		}
		downp = node.getDownp();
		funcName = store.getVarName(downp);
		glbFunMap.put(funcName, defunCount++);
		node = upNode;
		rightp = node.getRightp();
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp == KeywordTyp.ZPAREN) {
			upNode = node;
			rightp = node.getDownp();
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp != KeywordTyp.VAR) {
				return -1;
			}
			rightp = node.getRightp();
			while (rightp > 0) {
				page = store.getPage(rightp);
				idx = store.getElemIdx(rightp);
				node = page.getNode(idx);
				celltyp = node.getDownCellTyp();
				if (celltyp != NodeCellTyp.ID) {
					return -1;
				}
				downp = node.getDownp();
				varName = funcName + ' ';
				varName += store.getVarName(downp);
				glbLocVarMap.put(varName, varidx++);
				glbLocVarList.add(rightp);
				rightp = node.getRightp();
				node.setRightp(downp);
				node.setDownp(0);
				node.setRightCell(true);
				page.setNode(idx, node);
			}
			node = upNode;
			rightp = node.getRightp();
			omsg("Global/local var count = " + varidx);
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp == KeywordTyp.ZPAREN) {  // (gvar ...)
				rightp = node.getRightp();
				if (rightp <= 0) {
					return -1;
				}
				node = store.getNode(rightp);
			}
			kwtyp = node.getKeywordTyp();
		}
		if (kwtyp != KeywordTyp.DO) {
			omsg("Missing DO");
			return -1;
		}
		return savep;
	}
	
	private int scopeDefunStmt(int rightp) {
		Node node;
		Node upNode;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int downp;
		int savep = rightp;
		int stmtCount = 0;
		boolean rtnval;

		omsg("Keyword (scope) defun detected.");
		node = store.getNode(rightp);
		upNode = node;
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.ZPAREN) {
			return -1;
		}
		rightp = node.getDownp();
		node = store.getNode(rightp);
		celltyp = node.getDownCellTyp();
		if (celltyp != NodeCellTyp.FUNC) {
			return -1;
		}
		downp = node.getDownp();
		scopeFuncName = store.getVarName(downp);
		node = upNode;
		rightp = node.getRightp();
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp == KeywordTyp.ZPAREN) {
			upNode = node;
			rightp = node.getDownp();
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp != KeywordTyp.VAR) {
				return -1;
			}
			node = upNode;
			rightp = node.getRightp();
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp == KeywordTyp.ZPAREN) {  // (gvar ...)
				rightp = node.getRightp();
				if (rightp <= 0) {
					return -1;
				}
				node = store.getNode(rightp);
			}
			kwtyp = node.getKeywordTyp();
		}
		if (kwtyp != KeywordTyp.DO) {
			omsg("Missing DO");
			return -1;
		}
		rightp = node.getDownp();
		while (rightp > 0) {
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp != KeywordTyp.ZSTMT) {
				return -1;
			}
			omsg("Stmt count = " + stmtCount);
			downp = node.getDownp();
			rtnval = scopeStmt(downp);
			if (!rtnval) {
				return -1;
			}
			stmtCount++;
			rightp = node.getRightp();
		} 
		omsg("Stmt count = " + stmtCount + ", set count = " + count);
		return savep;
	}
	
	private int scanClassStmt(int rightp, KeywordTyp kwtyp) {
		return rightp;
	}
	
	private int scopeClassStmt(int rightp, KeywordTyp kwtyp) {
		return rightp;
	}
	
	private int runSetStmt(Node node) {
		int rightp;
		Page page;
		int idx;
		Node valnode;
		NodeCellTyp celltyp;
		int varidx;
		int value;
		
		count++;
		omsg("runSetStmt: top");
		rightp = node.getRightp();
		if (rightp <= 0) {
			return -1;
		}
		node = store.getNode(rightp);
		celltyp = node.getDownCellTyp();
		if (celltyp != NodeCellTyp.LOCVAR) {
			return -2;
		}
		varidx = node.getDownp();
		rightp = node.getRightp();
		if (rightp <= 0) {
			return -3;
		}
		valnode = store.getNode(rightp);
		celltyp = valnode.getDownCellTyp();
		if (celltyp != NodeCellTyp.INT) {
			return -4;
		}
		value = valnode.getDownp();
		rightp = glbLocVarList.get(varidx);
		node = store.getNode(rightp);
		node.setDownCellTyp(NodeCellTyp.INT.ordinal());
		node.setDownp(value);
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		page.setNode(idx, node);
		omsg("set stmt: value = " + value);
		return 0;
	}
	
	private int runPrintlnStmt(Node node) {
		int rightp;
		NodeCellTyp celltyp;
		Node varnode;
		int varidx;
		int varp, namep;
		int value;
		String varname;
		String msg = "";

		rightp = node.getRightp();
		while (rightp > 0) {
			node = store.getNode(rightp);
			celltyp = node.getDownCellTyp();
			if (celltyp != NodeCellTyp.LOCVAR) {
				return -51;
			}
			varidx = node.getDownp();
			varp = glbLocVarList.get(varidx);
			varnode = store.getNode(varp);
			namep = varnode.getRightp();
			varname = store.getVarName(namep);
			celltyp = varnode.getDownCellTyp();
			if (celltyp != NodeCellTyp.INT) {
				return -52;
			}
			value = varnode.getDownp();
			msg = msg + varname + " = " + value + "; ";
			rightp = node.getRightp();
		}
		omsg(msg);
		return 0;
	}
	
	private int runZcallStmt(Node node) {
		omsg("runZcallStmt");
		return 0;
	}
	
	private String getGdefunWord() {
		return "gdefun";
	}
	
}