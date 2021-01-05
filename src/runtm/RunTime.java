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

// Code Execution

public class RunTime implements IConst {

	private Store store;
	private ScanSrc scanSrc;
	private SynChk synChk;
	private static final boolean isSilent = false;
	private int rootNodep;
	private HashMap<String, Integer> glbPubVarMap;
	private int count;

	public RunTime(Store store, ScanSrc scanSrc, SynChk synChk,	int rootNodep) {
		this.store = store;
		this.scanSrc = scanSrc;
		this.synChk = synChk;
		this.rootNodep = rootNodep;
		glbPubVarMap = new HashMap<String, Integer>();
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
		if (rtnval) {
			rtnval = runTopBlock(downp);
		}
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
	
	private int scanGlbDefStmt(int rightp) {
		Node node;
		Node firstNode;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int downp;
		int savep = rightp;
		String varName;
		int varidx = 0;
		int stmtCount = 0;
		boolean rtnval;

		omsg("Keyword gdefun detected.");
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
				node = store.getNode(rightp);
				celltyp = node.getDownCellTyp();
				if (celltyp != NodeCellTyp.ID) {
					return -1;
				}
				downp = node.getDownp();
				varName = store.getVarName(downp);
				glbPubVarMap.put(varName, varidx++);
				rightp = node.getRightp();
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
		rightp = node.getDownp();
		while (rightp > 0) {
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp != KeywordTyp.ZSTMT) {
				return -1;
			}
			downp = node.getDownp();
			rtnval = scanStmt(downp);
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
		NodeCellTyp celltyp;
		int downp;
		int savep = rightp;
		String varName;
		int varidx = 0;
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
			rightp = node.getRightp();
			while (rightp > 0) {
				node = store.getNode(rightp);
				celltyp = node.getDownCellTyp();
				if (celltyp != NodeCellTyp.ID) {
					return -1;
				}
				//downp = node.getDownp();
				//varName = store.getVarName(downp);
				//glbPubVarMap.put(varName, varidx++);
				rightp = node.getRightp();
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
	
	private boolean scanStmt(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		switch (kwtyp) {
		case SET: return scanSetStmt(node);
		case PRINTLN: return scanPrintlnStmt(node);
		default: return false;
		}
	}
	
	private boolean runStmt(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		switch (kwtyp) {
		case SET: return runSetStmt(node);
		case PRINTLN: return runPrintlnStmt(node);
		default: return false;
		}
	}
	
	private boolean scanSetStmt(Node node) {
		int rightp;
		boolean rtnval;
		
		count++;
		rightp = node.getRightp();
		if (rightp <= 0) {
			return false;
		}
		node = store.getNode(rightp);
		rtnval = scanLocVar(node);
		return rtnval;
	}
	
	private boolean scanPrintlnStmt(Node node) {
		int rightp;
		boolean rtnval;

		rightp = node.getRightp();
		while (rightp > 0) {
			node = store.getNode(rightp);
			rtnval = scanLocVar(node);
			if (!rtnval) {
				return false;
			}
			rightp = node.getRightp();
		}
		return true;
	}
	
	private boolean scanLocVar(Node node) {
		int downp;
		NodeCellTyp celltyp;
		String varName;
		Integer value;
		int varidx;
		
		celltyp = node.getDownCellTyp();
		if (celltyp != NodeCellTyp.ID) {
			return false;
		}
		downp = node.getDownp();
		varName = store.getVarName(downp);
		value = glbPubVarMap.get(varName);
		if (value == null) {
			return false;
		}
		varidx = (int)value;
		node.setDownCellTyp(NodeCellTyp.LOCVAR.ordinal());
		node.setDownp(varidx);
		omsg("LocVar = " + varidx);
		return true;
	}
	
	private int scanDefunStmt(int rightp) {
		return rightp;
	}
	
	private int scanClassStmt(int rightp, KeywordTyp kwtyp) {
		return rightp;
	}
	
	private boolean runSetStmt(Node node) {
		int rightp;
		boolean rtnval;
		
		count++;
		rightp = node.getRightp();
		if (rightp <= 0) {
			return false;
		}
		node = store.getNode(rightp);
		//rtnval = scanLocVar(node);
		rtnval = true;
		return rtnval;
	}
	
	private boolean runPrintlnStmt(Node node) {
		int rightp;
		boolean rtnval;

		rightp = node.getRightp();
		while (rightp > 0) {
			node = store.getNode(rightp);
			//rtnval = scanLocVar(node);
			rtnval = true;
			if (!rtnval) {
				return false;
			}
			rightp = node.getRightp();
		}
		return true;
	}
	
}