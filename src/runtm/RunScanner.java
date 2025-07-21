package runtm;

import iconst.IConst;
import iconst.RunConst;
import iconst.KeywordTyp;
import iconst.NodeCellTyp;
import page.Node;
import page.AddrNode;
import page.Store;
import page.Page;
import java.util.ArrayList;
import scansrc.ScanSrc;
import synchk.SynChk;

// Setup Code Execution

public class RunScanner implements IConst, RunConst {

	private Store store;
	private ScanSrc scanSrc;
	private SynChk synChk;
	private static final boolean isSilent = false;
	private int rootNodep;
	private RunTime rt;
	private String scopeFuncName;
	private int defunCount;
	private int count;
	private int glbVarListIdx;
	private ArrayList<Integer> gvarList;
	private int lastRightp;
	private int lastErrCode;
	private boolean isSyntaxError;
	private int stmtCount;
	private int runidx;
	private boolean isRunTest;
	private boolean isBadUtPair;

	public RunScanner(Store store, ScanSrc scanSrc, SynChk synChk, int rootNodep) {
		this.store = store;
		this.scanSrc = scanSrc;
		this.synChk = synChk;
		this.rootNodep = rootNodep;
		rt = new RunTime(store, scanSrc, synChk);
		gvarList = new ArrayList<Integer>();
		lastRightp = 0;
		lastErrCode = 0;
		isSyntaxError = false;
		defunCount = 0;
		count = 0;
		stmtCount = 0;
	}

	public boolean run(int runidx) {
		boolean rtnval;
		
		omsg("RunTime.run: rootNodep = " + rootNodep);
		rt.setRscan(this);
		this.runidx = runidx;
		isBadUtPair = false;
		rtnval = runRoot(rootNodep);
		if (!rtnval) {
			oprn(getRTorSyntax() + " error detected!");
		}
		return rtnval;
	}
	
	public void out(String msg) {
		if (debug) {
			System.out.println(msg);
		}
	}
	
	public void omsg(String msg) {  
		if (isrtbug) {
			System.out.println(msg);
		}
	}
	
	public void oprn(String msg) {  
		System.out.println(msg);
	}
	
	private boolean runRoot(int rightp) {
		int downp;
		Node node;
		int lineno;
		boolean rtnval;
		
		node = store.getNode(rightp);
		rightp = node.getRightp();
		node = store.getNode(rightp);
		downp = node.getDownp();
		// scan decls. of glb/loc vars., 
		// scan names of func decls., including gdefun
		rtnval = scanTopBlock(downp);
		if (!rtnval) {
			omsg("err: scanTopBlock");
			return false;
		}
		// replace downp of glb/loc var refs. w/ var idx nos.
		// must scan all do-blocks
		rtnval = scopeTopBlock(downp);
		if (!rtnval) {
			omsg("err: scopeTopBlock");
			lineno = store.lookupLineNo(lastRightp);
			if (lineno != 0) {
				oprn("Error on line number: " + lineno);
			}
			handleErrToken(lastErrCode);
			isSyntaxError = true;
			return false;
		}
		// run prog. using do-block of gdefun stmt.
		rtnval = rt.runTopBlock(downp, runidx);
		isBadUtPair = rt.isBadUpFlag();
		omsg("runRoot: runTopBlock = " + rtnval);
		return rtnval;
	}
	
	private boolean scanTopBlock(int rightp) {
		// process top-level stmts.:
		// do ( stmt-1; .. stmt-n; )
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
					out("scopeTopBlock: -ve phaseNo");
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
		// scan process saves local var. names/idx nos., and node ptrs.
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
				case 1:  // stub
					rightq = scanImportStmt(rightp, kwtyp);
					break;
				case 2:
					rightq = scanGlbDefStmt(rightp);
					break;
				case 3:
					rightq = scanDefunStmt(rightp);
					break;
				case 4:  // stub
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
				case 1:  // stub
					rightq = scopeImportStmt(rightp, kwtyp);
					break;
				case 2:
					rightq = scopeGlbDefStmt(rightp);
					break;
				case 3:
					rightq = scopeDefunStmt(rightp);
					break;
				case 4:  // stub
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
	
	private int scanImportStmt(int rightp, KeywordTyp kwtyp) {
		return rightp;
	}
	
	private int scopeImportStmt(int rightp, KeywordTyp kwtyp) {
		return rightp;
	}
	
	private int scanGlbDefStmt(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		String fname;
		char prefix;
		int savep = rightp;
		int upperp;

		omsg("Keyword gdefun detected.");
		fname = getGdefunWord();
		rt.glbFunMap.put(fname, defunCount);
		rt.glbFunList.add(0);
		rt.glbFuncNames.add(fname);
		defunCount++;
		glbVarListIdx = 0;
		upperp = rightp;
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp == KeywordTyp.ZPAREN) {
			rightp = node.getDownp();
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			switch (kwtyp) {
			case VAR:
				prefix = 'g';
				break;
			case IVAR:
				prefix = 'i';
				break;
			default:
				return -1;
			}
			rightp = node.getRightp();
			rightp = scanGlbVarList(prefix + fname, upperp, rightp);
			if (rightp <= 0) {
				return -1;
			}
			node = store.getNode(rightp);
			rightp = node.getRightp();
			if (rightp <= 0) {
				omsg("Missing DO after var/ivar");
				return -1;
			}
			upperp = rightp;
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp != KeywordTyp.ZPAREN) { }
			else if (prefix == 'i') {
				omsg("scanGlbDefStmt: open paren found " + 
					"after (ivar ...)");
				return -1;
			}
			else {
				omsg("scanGlbDefStmt: do ivar after var");
				rightp = node.getDownp();
				node = store.getNode(rightp);
				kwtyp = node.getKeywordTyp();
				if (kwtyp != KeywordTyp.IVAR) {
					return -1;
				}
				rightp = node.getRightp();
				rightp = scanGlbVarList('i' + fname, upperp, rightp);
				if (rightp <= 0) {
					return -1;
				}
				node = store.getNode(rightp);
				rightp = node.getRightp();
				if (rightp <= 0) {
					omsg("Missing DO");
					return -1;
				}
				node = store.getNode(rightp);
				kwtyp = node.getKeywordTyp();
			}
		}
		if (kwtyp != KeywordTyp.DO) {
			omsg("Expecting DO, found kwytp = " + kwtyp);
			return -1;
		}
		return savep;
	}
	
	private int scanGlbVarList(String fname, int upperp, int rightp) {
		Node node;
		NodeCellTyp celltyp;
		int downp;
		String varName;
		int varidx;
		int idx;
		Page page;

		varidx = glbVarListIdx;
		while (rightp > 0) {
			// scan decls. of global/local vars.
			// save dict. w/ var names and var nos.
			// save list w/ var decl. node ptrs.
			page = store.getPage(rightp);
			idx = store.getElemIdx(rightp);
			node = page.getNode(idx);
			celltyp = node.getDownCellTyp();
			if (celltyp != NodeCellTyp.ID) {
				omsg("scanGlbVarList: bad celltyp = " + celltyp);
				return -1;
			}
			downp = node.getDownp();
			varName = fname + ' ';
			varName += store.getVarName(downp);
			rt.glbLocVarMap.put(varName, varidx);
			rt.glbLocVarList.add(rightp);
			varidx++;
			rightp = node.getRightp();
			node.setRightp(downp);
			node.setDownp(0);
			node.setRightCell(true);
			page.setNode(idx, node);
			omsg("scanGlbVarList: var = " + varName + 
				", idx = " + varidx);
			omsg("Global public var/ivar count = " + varidx);
		}
		glbVarListIdx = varidx;
		rt.glbLocVarList.add(-1);
		return upperp;
	}
	
	private int scopeGlbDefStmt(int rightp) {
		// scope operation:
		// replace downp of glb/loc var refs. w/ var idx nos.
		// (they were pointing to var names)
		Node node;
		Node upNode;
		KeywordTyp kwtyp, kwalt;
		int downp;
		int savep = rightp;
		int stmtCount = 0;
		boolean rtnval;

		omsg("Keyword gdefun detected.");
		scopeFuncName = "";
		rightp = scopeGlbVarLists(rightp);
		if (rightp <= 0) {
			omsg("scopeGlbDefStmt: bad var list");
			return -1;
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.DO) {
			omsg("Missing DO");
			return -1;
		}
		rightp = node.getDownp();
		while (rightp > 0) {
			// for all stmts. in do-block, perform scope oper.
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp != KeywordTyp.ZSTMT) {
				return -1;
			}
			omsg("Stmt count = " + stmtCount);
			downp = node.getDownp();
			rtnval = scopeStmt(downp);
			if (!rtnval) {
				omsg("scopeGlbDefStmt: err scopeStmt");
				return -1;
			}
			stmtCount++;
			rightp = node.getRightp();
		} 
		omsg("Stmt count = " + stmtCount + ", set count = " + count);
		return savep;
	}
	
	public int scopeGlbVarLists(int rightp) {
		Node node;
		Node upNode;
		KeywordTyp kwtyp, kwalt;
		
		node = store.getNode(rightp);
		upNode = node;
		kwtyp = node.getKeywordTyp();
		if (kwtyp == KeywordTyp.ZPAREN) {
			rightp = node.getDownp();
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp != KeywordTyp.VAR && 
				kwtyp != KeywordTyp.IVAR) 
			{
				omsg("scopeGlbDefStmt: expecting var/ivar, " +
					"found kwd = " + kwtyp);
				return -1;
			}
			rightp = upNode.getRightp();
			if (rightp <= 0) {
				omsg("scopeGlbDefStmt: naked var/ivar list");
				return -1;
			}
			node = store.getNode(rightp);
			upNode = node;
			kwtyp = node.getKeywordTyp();
			if (kwtyp == KeywordTyp.ZPAREN) {  // (ivar ...)
				rightp = node.getDownp();
				node = store.getNode(rightp);
				kwalt = node.getKeywordTyp();
				if (kwalt != KeywordTyp.IVAR || 
					kwtyp == KeywordTyp.IVAR) 
				{
					omsg("scopeGlbDefStmt: 2 invalid var lists");
					return -1;
				}
				rightp = upNode.getRightp();
				if (rightp <= 0) {
					omsg("scopeGlbDefStmt: var OK, naked ivar list");
					return -1;
				}
			}
		}
		return rightp;
	}
	
	private boolean scopeStmt(int rightp) {
		Node node;
		KeywordTyp kwtyp;
		
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		switch (kwtyp) {
		case SET: 
		case ADDSET:
		case MINUSSET: 
		case MPYSET: 
		case DIVSET: 
		case ANDBSET: 
		case ORBSET: 
		case XORBSET: 
			return scopeSetStmt(node);
		case INCINT:
		case DECINT:
			return scopeIncDecStmt(node);
		case QUEST:
			return scopeBoolStmt(node);
		case PRINTLN: return scopePrintlnStmt(node);
		case ZCALL: return scopeZcallStmt(rightp, false);
		case RETURN: return scopeRtnStmt(node);
		case UTPUSH: return scopeUtPushStmt(node);
		case UTSCAN: return scopeUtScanStmt(node);
		case IF: return scopeIfStmt(node);
		case WHILE: return scopeWhileStmt(node);
		case FOR: return scopeForStmt(node);
		case BREAK:
		case CONTINUE:
			return true;
		default: 
			omsg("scopeStmt: invalid kwtyp = " + kwtyp);
			return false;
		}
	}
	
	private boolean scopeSetStmt(Node node) {
		int rightp;
		
		count++;
		omsg("scopeSetStmt: top");
		rightp = node.getRightp();
		if (rightp <= 0) {
			return false;
		}
		node = store.getNode(rightp);
		// perform scope oper. on single var. ref.
		if (!scopeLocVarRtn(rightp, true)) {
			omsg("scopeSetStmt: scopeLocVar fail");
			return false;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			return false;
		}
		// perform scope oper. on single expr.
		omsg("scopeSetStmt: call scopeExpr");
		return scopeExpr(rightp);
	}
	
	private boolean scopeIncDecStmt(Node node) {
		int rightp;
		
		count++;
		omsg("scopeIncDecStmt: top");
		rightp = node.getRightp();
		if (rightp <= 0) {
			return false;
		}
		node = store.getNode(rightp);
		// perform scope oper. on single var. ref.
		if (!scopeLocVarRtn(rightp, true)) {
			omsg("scopeIncDecStmt: scopeLocVar fail");
			return false;
		}
		rightp = node.getRightp();
		if (rightp > 0) {
			return false;
		}
		return true;
	}
	
	private boolean scopeBoolStmt(Node node) {
		int rightp;
		
		count++;
		omsg("scopeBoolStmt: top");
		rightp = node.getRightp();
		if (rightp <= 0) {
			return false;
		}
		// perform scope oper. on single expr.
		omsg("scopeBoolStmt: call scopeExpr");
		return scopeExpr(rightp);
	}
	
	private boolean scopePrintlnStmt(Node node) {
		// perform scope oper. on multiple exprs.
		int rightp;
		boolean rtnval;

		rightp = node.getRightp();
		while (rightp > 0) {
			node = store.getNode(rightp);
			rtnval = scopeExpr(rightp);
			if (!rtnval) {
				return false;
			}
			rightp = node.getRightp();
		}
		return true;
	}
	
	private boolean scopeMultiStmt(Node node, int count) {
		// perform scope oper. on multiple exprs.
		int rightp;
		int i;
		boolean rtnval;

		rightp = node.getRightp();
		for (i = 0; i < count; i++) {
			node = store.getNode(rightp);
			rtnval = scopeExpr(rightp);
			if (!rtnval) {
				return false;
			}
			rightp = node.getRightp();
		}
		return true;
	}
	
	private boolean scopeRtnStmt(Node node) {
		// perform scope oper. on single expr.
		// does not fail if no expr. found
		int rightp;
		boolean rtnval;
		
		rightp = node.getRightp();
		if (rightp <= 0) {
			return true;
		}
		rtnval = scopeExpr(rightp);
		if (!rtnval) {
			omsg("scopeRtnStmt: expr fail");
		}
		return rtnval;
	}
	
	private boolean scopeZcallStmt(int rightp, boolean isExpr) {
		// scope a func. call
		// perform scope oper. on zero or more args.
		int downp;
		Node node;
		Page page;
		int idx;
		NodeCellTyp celltyp;
		String varName;
		Integer value;
		int varidx;
		boolean rtnval;
		
		node = store.getNode(rightp);
		celltyp = node.getDownCellTyp();
		if (celltyp != NodeCellTyp.FUNC) {
			return false;
		}
		downp = node.getDownp();
		varName = store.getVarName(downp);
		value = rt.glbFunMap.get(varName);
		if (value == null) {
			return false;
		}
		varidx = (int)value;
		// replace downp of func. ref. w/ func. idx no.
		node.setDownp(varidx);
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		page.setNode(idx, node);
		omsg("FunVar = " + varidx);
		if (isExpr) {
			return true;
		}
		rtnval = scopePrintlnStmt(node);
		return rtnval;
	}
	
	private boolean scopeIfStmt(Node node) {
		int rightp;
		KeywordTyp kwtyp;
		boolean isElse = false;
		boolean rtnval;
		
		omsg("scopeIfStmt: top");
		do {
			rightp = node.getRightp();
			if (rightp <= 0) {
				return false;
			}
			node = store.getNode(rightp);
			if (!isElse) {
				rtnval = scopeExpr(rightp);
				if (!rtnval) {
					return false;
				}
				rightp = node.getRightp();
				if (rightp <= 0) {
					return false;
				}
				node = store.getNode(rightp);
			}
			kwtyp = node.getKeywordTyp();
			if (kwtyp != KeywordTyp.DO) {
				omsg("Missing DO");
				return false;
			}
			rtnval = scopeDoBlock(node);
			if (!rtnval) {
				return false;
			}
			rightp = node.getRightp();
			if (rightp <= 0) {
				return true;
			}
			if (isElse) {
				return false;
			}
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			isElse = (kwtyp == KeywordTyp.ELSE);
		} while ((kwtyp == KeywordTyp.ELIF) || isElse);
		return false;
	}
	
	private boolean scopeWhileStmt(Node node) {
		int rightp;
		KeywordTyp kwtyp;
		boolean rtnval;
		
		omsg("scopeWhileStmt: top");
		rightp = node.getRightp();
		if (rightp <= 0) {
			return false;
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp == KeywordTyp.DO) {
			if (!scopeDoBlock(node)) {
				return false;
			}
			rightp = node.getRightp();
			if (rightp <= 0) {
				return false;
			}
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp != KeywordTyp.UNTIL) {
				omsg("Missing UNTIL");
				return false;
			}
			rightp = node.getRightp();
			if (rightp <= 0) {
				return false;
			}
			rtnval = scopeExpr(rightp);
			return rtnval;
		}
		rtnval = scopeExpr(rightp);
		if (!rtnval) {
			return false;
		}
		rightp = node.getRightp();
		if (rightp <= 0) {
			return false;
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.DO) {
			omsg("Missing DO");
			return false;
		}
		rtnval = scopeDoBlock(node);
		return rtnval;
	}
	
	private boolean scopeForStmt(Node node) {
		int rightp;
		int firstp;
		int midp;
		int exprp;
		int thirdp;
		int savep;
		int downp;
		KeywordTyp kwtyp;
		Page page;
		Node midNode;
		int idx;
		boolean rtnval;
		
		omsg("scopeForStmt: top");
		rightp = node.getRightp();
		if (rightp <= 0) {
			return false;
		}
		firstp = rightp;
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		omsg("scopeForStmt: kwtyp = " + kwtyp);
		if (kwtyp != KeywordTyp.DO) {
			omsg("Missing DO before header");
			return false;
		}
		rtnval = scopeDoBlock(node);
		if (!rtnval) {
			omsg("scopeForStmt: header scope failed");
			return false;
		}
		savep = node.getRightp();
		// handle massage of header here:
		// insert do node in between both do nodes
		// middle do node points to 3rd stmt in header
		// 3rd stmt in header points to 2nd stmt in header
		// 2nd stmt in header points to null
		
		midp = scanSrc.addDoNode(firstp);
		midNode = store.getNode(midp);
		downp = node.getDownp();
		node = store.getNode(downp); // 1st stmt in header (doesn't change)
		exprp = node.getRightp();
		node = store.getNode(exprp); // 2nd (expr) stmt in header
		thirdp = node.getRightp();
		node.setRightp(0); 
		page = store.getPage(exprp);
		idx = store.getElemIdx(exprp);
		page.setNode(idx, node);
		
		node = store.getNode(thirdp); // 3rd stmt in header
		midNode.setDownp(thirdp);
		midNode.setRightp(savep);
		page = store.getPage(midp);
		idx = store.getElemIdx(midp);
		page.setNode(idx, midNode);

		node.setRightp(exprp);
		page = store.getPage(thirdp);
		idx = store.getElemIdx(thirdp);
		page.setNode(idx, node); 
		rightp = savep;
		if (rightp <= 0) {
			return false;
		}
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.DO) {
			omsg("Missing DO");
			return false;
		}
		rtnval = scopeDoBlock(node);
		return rtnval;
	}
	
	private boolean scopeUtPushStmt(Node node) {
		boolean rtnval;
		
		count++;
		omsg("scopeUtPushStmt: top");
		rtnval = scopeMultiStmt(node, 2);
		if (!rtnval) {
			omsg("scopeUtPushStmt: fail");
		}
		return rtnval;
	}
	
	private boolean scopeUtScanStmt(Node node) {
		boolean rtnval;
		
		count++;
		omsg("scopeUtScanStmt: top");
		rtnval = scopeMultiStmt(node, 2);
		if (!rtnval) {
			omsg("scopeUtScanStmt: fail");
		}
		return rtnval;
	}
	
	private boolean scopeLocVar(int rightp) {
		return scopeLocVarRtn(rightp, false);
	}
	
	private boolean scopeLocVarRtn(int rightp, boolean isTgt) {
		// replace downp of glb/loc var ref. w/ var idx no.
		// (it was pointing to var name)
		// glb var idx nos. are -ve
		int downp;
		Node node;
		NodeCellTyp celltyp;
		boolean isGlb;
		String varName, name;
		String gname;
		Integer value;
		int varidx;
		
		node = store.getNode(rightp);
		celltyp = node.getDownCellTyp();
		if (celltyp != NodeCellTyp.ID) {
			return false;
		}
		downp = node.getDownp();
		gname = getGdefunWord();
		isGlb = scopeFuncName.equals("");
		if (isGlb) {
			name = 'g' + gname;
		}
		else {
			name = scopeFuncName;
		}
		varName = store.getVarName(downp);
		name = name + ' ' + varName;
		omsg("scopeLocVar: name = " + name);
		value = rt.glbLocVarMap.get(name);
		if (value != null) {
			varidx = (int)value;
			if (isGlb) {
				varidx = -1 - varidx;
			}
			doScopeLocVar(node, rightp, varidx);
			return true;
		}
		else if (isGlb) {
			name = 'i' + gname + ' ' + varName;
			value = rt.glbLocVarMap.get(name);
			if (value == null) {
				omsg("scopeLocVar: glb var fail");
				return false;
			}
			varidx = (int)value;
			doScopeLocVar(node, rightp, varidx);
			return true;
		}
		name = 'g' + gname + ' ' + varName;
		value = rt.glbLocVarMap.get(name);
		if (value != null) {
			varidx = (int)value;
			omsg("scopeLocVar: varidx = " + varidx);
			// check if varidx not in gvar list...
			if (isTgt && !gvarList.contains(varidx)) {
				// error: attempt to modify unlisted glbvar
				lastErrCode = BADGVAR;
				lastRightp = rightp;
				return false;
			}
			varidx = -1 - varidx;
			doScopeLocVar(node, rightp, varidx);
			return true;
		}
		name = 'i' + gname + ' ' + varName;
		value = rt.glbLocVarMap.get(name);
		if (value == null) {
			return false;
		}
		varidx = (int)value;
		doScopeLocVar(node, rightp, varidx);
		return true;
	}
	
	private void doScopeLocVar(Node node, int rightp, int varidx) {
		Page page;
		int idx;
	
		node.setDownCellTyp(NodeCellTyp.LOCVAR.ordinal());
		node.setDownp(varidx);
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		page.setNode(idx, node);
		omsg("LocVar = " + varidx);
	}
	
	private boolean scopeExpr(int rightp) {
		// call scopeLocVar for all lower level IDs
		Node node;
		NodeCellTyp celltyp;
		boolean rtnval = true;

		node = store.getNode(rightp);
		celltyp = node.getDownCellTyp();
		if (celltyp == NodeCellTyp.ID) {  // scope var. ref.
			return scopeLocVar(rightp);
		}
		if (celltyp == NodeCellTyp.FUNC) {  // scope fun. call
			return scopeZcallStmt(rightp, true);
		}
		if (celltyp != NodeCellTyp.PTR) {
			return true;
		}
		rightp = node.getDownp();
		while (rtnval && (rightp > 0)) {
			// handle lower level exprs. recursively
			rtnval = rtnval && scopeExpr(rightp);
			node = store.getNode(rightp);
			rightp = node.getRightp();
		}
		return rtnval;
	}
	
	private int scanDefunStmt(int rightp) {
		Node node;
		Node upNode, funcNameNode;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int downp;
		int savep = rightp;
		int funcp;
		String funcName;
		String varName;
		int varidx = 0;
		int idx;
		int glbLocIdx;
		Page page;
		boolean gvarFound = false;

		omsg("Keyword defun detected.");
		node = store.getNode(rightp);
		upNode = node;
		kwtyp = node.getKeywordTyp();
		if (kwtyp != KeywordTyp.ZPAREN) {
			return -1;
		}
		funcp = rightp;
		rightp = node.getDownp();
		node = store.getNode(rightp);
		celltyp = node.getDownCellTyp();
		if (celltyp != NodeCellTyp.FUNC) {
			return -1;
		}
		funcNameNode = node;
		downp = node.getDownp();
		funcName = store.getVarName(downp);
		rt.glbFunMap.put(funcName, defunCount);
		glbLocIdx = rt.glbLocVarList.size();
		rt.glbLocVarMap.put(funcName, glbLocIdx);
		rt.glbFuncNames.add(funcName);
		rightp = node.getRightp();
		while (rightp > 0) {
			// scan var decls. of parm. list
			rightp = scanParmVarList(rightp, varidx, funcName);
			varidx++;
		}
		if (rightp < 0) {
			return rightp;
		}
		// call to put used if parm count value needed later:
		varName = getFunVar(funcName);
		rt.glbLocVarMap.put(varName, varidx);
		node = upNode;
		rightp = node.getRightp();
		node = store.getNode(rightp);
		kwtyp = node.getKeywordTyp();
		if (kwtyp == KeywordTyp.ZPAREN) {
			upNode = node;
			rightp = node.getDownp();
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if ((kwtyp != KeywordTyp.VAR) && 
				(kwtyp != KeywordTyp.GVAR)) 
			{
				return -1;
			}
			if (kwtyp == KeywordTyp.VAR) {
				rightp = node.getRightp();
				while (rightp > 0) {
					// scan var decls. of local vars.
					rightp = scanParmVarList(rightp, varidx, funcName);
					varidx++;
				}
				if (rightp < 0) {
					return rightp;
				}
				node = upNode;
				rightp = node.getRightp();
				omsg("Global/local var count = " + varidx);
				node = store.getNode(rightp);
				kwtyp = node.getKeywordTyp();
				if (kwtyp == KeywordTyp.ZPAREN) {  // (gvar ...) 
					omsg("scanDefunStmt: var, go into gvar list");
					upNode = node;
					rightp = node.getDownp();
					node = store.getNode(rightp);
					kwtyp = node.getKeywordTyp();
					if (kwtyp != KeywordTyp.GVAR) {
						return -1;
					}
					gvarFound = true;
				}
			}
			else {
				gvarFound = true;
			}
			if (gvarFound) {
				gvarList.clear();
				rightp = node.getRightp();
				while (rightp > 0) {
					// scan var decls. of gvar list
					rightp = scanGvarList(rightp);
				}
				if (rightp < 0) {
					return rightp;
				}
				node = upNode;
				rightp = node.getRightp();
				if (rightp <= 0) {
					return -1;
				}
				node = store.getNode(rightp);
				kwtyp = node.getKeywordTyp();
			}
		}
		rt.glbLocVarList.add(-1);
		if (kwtyp != KeywordTyp.DO) {
			omsg("Missing DO");
			return -1;
		}
		if (node.getRightp() > 0) {
			omsg("Post DO: unexpected rightp found");
			return -1;
		}
		// rightp points to do-block
		// funcp points to zparen of (f x y)
		downp = funcNameNode.getDownp();
		varName = store.getVarName(downp);
		omsg("Post DO: varName = " + varName);
		upNode = new Node(0, funcp, rightp);
		rightp = store.allocNode(upNode);
		rt.glbFunList.add(rightp);
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		celltyp = NodeCellTyp.PTR;
		upNode.setDownCellTyp(celltyp.ordinal());
		upNode.setRightCell(true);
		page.setNode(idx, upNode);
		defunCount++;
		return savep;
	}
	
	private int scanParmVarList(int rightp, int varidx, String funcName) {
		// scan decl. of local var
		// save dict. w/ var name and var no.
		// save list w/ var decl. node ptr.
		Node node;
		NodeCellTyp celltyp;
		int downp;
		String varName;
		int idx;
		Page page;

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
		rt.glbLocVarMap.put(varName, varidx);
		rt.glbLocVarList.add(rightp);
		rightp = node.getRightp();
		node.setRightp(downp);
		node.setDownp(0);
		node.setRightCell(true);
		page.setNode(idx, node);
		return rightp;
	}
	
	private int scanGvarList(int rightp) {
		// scan decl. in gvar list
		Node node;
		NodeCellTyp celltyp;
		int downp;
		String funcName;
		String varName;
		int varidx;
		Integer value;
		int idx;
		Page page;

		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		celltyp = node.getDownCellTyp();
		if (celltyp != NodeCellTyp.ID) {
			return -1;
		}
		downp = node.getDownp();
		funcName = getGdefunKwd();
		varName = funcName + ' ';
		varName += store.getVarName(downp);
		value = rt.glbLocVarMap.get(varName);
		if (value == null) {
			return -1;
		}
		varidx = (int)value;
		gvarList.add(varidx);
		rightp = node.getRightp();
		return rightp;
	}
	
	private int scopeDefunStmt(int rightp) {
		// scope operation:
		// replace downp of glb/loc var refs. w/ var idx nos.
		// (they were pointing to var names)
		Node node;
		Node upNode;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		int downp;
		int savep = rightp;
		boolean gvarFound = false;
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
			if ((kwtyp != KeywordTyp.VAR) && 
				(kwtyp != KeywordTyp.GVAR)) 
			{
				return -1;
			}
			if (kwtyp == KeywordTyp.VAR) {
				node = upNode;
				rightp = node.getRightp();
				node = store.getNode(rightp);
				kwtyp = node.getKeywordTyp();
				if (kwtyp == KeywordTyp.ZPAREN) {  // (gvar ...)
					upNode = node;
					rightp = node.getDownp();
					node = store.getNode(rightp);
					kwtyp = node.getKeywordTyp();
					if (kwtyp != KeywordTyp.GVAR) {
						return -1;
					}
					gvarFound = true;
				}
			}
			else {
				gvarFound = true;
			}
			if (gvarFound) {
				node = upNode;
				rightp = node.getRightp();
				if (rightp <= 0) {
					return -1;
				}
				node = store.getNode(rightp);
				kwtyp = node.getKeywordTyp();
			}
		}
		if (kwtyp != KeywordTyp.DO) {
			omsg("Missing DO");
			return -1;
		}
		rtnval = scopeDoBlock(node);
		if (!rtnval) {
			return -1;
		}
		omsg("Stmt count = " + stmtCount + ", set count = " + count);
		return savep;
	}
	
	private boolean scopeDoBlock(Node node) {
		KeywordTyp kwtyp;
		int downp;
		int rightp;
		boolean rtnval;

		rightp = node.getDownp();
		while (rightp > 0) {
			// for all stmts. in do-block, perform scope oper.
			node = store.getNode(rightp);
			kwtyp = node.getKeywordTyp();
			if (kwtyp != KeywordTyp.ZSTMT) {
				return false;
			}
			omsg("Stmt count = " + stmtCount);
			downp = node.getDownp();
			rtnval = scopeStmt(downp);
			if (!rtnval) {
				return false;
			}
			incStmtCount();
			rightp = node.getRightp();
		} 
		return true;
	}
	
	private void incStmtCount() {
		stmtCount++;
	}
	
	private int scanClassStmt(int rightp, KeywordTyp kwtyp) {
		return rightp;
	}
	
	private int scopeClassStmt(int rightp, KeywordTyp kwtyp) {
		return rightp;
	}
	
	public boolean isBadUpFlag() {
		return isBadUtPair;
	}
	
	public boolean getSyntaxErrorFlag() {
		return isSyntaxError;
	}
	
	public String getRTorSyntax() {
		if (isSyntaxError) {
			return "Syntax";
		}
		else {
			return "Runtime";
		}
	}
	
	private void handleErrToken(int rightp) {
		if (rightp == 0) {
			return;
		}
		oprn("Syntax Error: " + rt.convertErrToken(rightp));
	}
	
	private String getGdefunWord() {
		return "defun";
	}
	
	public String getGdefunKwd() {
		return "gdefun";
	}
	
	public String getFunVar(String funcName) {
		String varName;
		varName = funcName + " var";
		return varName;
	}
	
}