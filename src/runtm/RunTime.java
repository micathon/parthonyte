package runtm;

import iconst.IConst;
import iconst.KeywordTyp;
import iconst.NodeCellTyp;
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
	private RunScanner rscan;
	public HashMap<String, Integer> glbFunMap;
	public HashMap<String, Integer> glbLocVarMap;
	public ArrayList<Integer> glbFunList;
	public ArrayList<Integer> glbLocVarList;

	public RunTime(Store store, ScanSrc scanSrc, SynChk synChk) {
		this.store = store;
		this.scanSrc = scanSrc;
		this.synChk = synChk;
		glbFunMap = new HashMap<String, Integer>();
		glbLocVarMap = new HashMap<String, Integer>();
		glbFunList = new ArrayList<Integer>();
		glbLocVarList = new ArrayList<Integer>();
	}
	
	public void setRscan(RunScanner rscan) {
		this.rscan = rscan;
	}

	public void out(String msg) {
		rscan.out(msg);
	}
	
	public void omsg(String msg) {  
		rscan.omsg(msg);
	}
	
	public boolean runTopBlock(int rightp) {
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
		omsg("Stmt count = " + stmtCount);
		return savep;
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
	
	private int runSetStmt(Node node) {
		int rightp;
		Page page;
		int idx;
		Node valnode;
		NodeCellTyp celltyp;
		int varidx;
		int value;
		
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
		if (varidx < 0) {
			varidx = -1 - varidx;
		}
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
			if (varidx < 0) {
				varidx = -1 - varidx;
			}
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
		int downp;
		Page page;
		int idx;
		NodeCellTyp celltyp;
		String funcName;
		Integer value;
		int varidx;
		int i, j, k;
		
		omsg("runZcallStmt: top");
		varidx = node.getDownp() - 1;
		downp = glbFunList.get(varidx);
		node = store.getNode(downp);
		downp = node.getDownp();
		node = store.getNode(downp);
		downp = node.getDownp();
		node = store.getNode(downp);
		downp = node.getDownp();
		funcName = store.getVarName(downp);
		omsg("runZcall: FunVar = " + varidx + ", Fun = " + funcName);
		i = glbLocVarMap.get(funcName);
		k = i;
		j = 0;
		while (true) {
			j = glbLocVarList.get(i);
			if (j < 0) {
				break;
			}
			i++;
		}
		i -= k;
		omsg("runZcall: LocVar count = " + i);
		return 0;
	}
	
	private String getGdefunWord() {
		return "gdefun";
	}
	
	private boolean isGdefun(String s) {
		return s.equals("gdefun");
	}
	
}