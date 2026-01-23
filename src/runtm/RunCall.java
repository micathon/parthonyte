package runtm;

import iconst.IConst;
import iconst.KeywordTyp;
import iconst.PageTyp;
import iconst.RunConst;
import page.AddrNode;
import page.Node;
import page.Page;
import page.Store;

public class RunCall implements IConst, RunConst {

	private Store store;
	private RunTime rt;
	private RunScanner rscan;
	private RunPushPop pp;
	
	public RunCall(Store store, RunTime rt, RunPushPop pp) {
		this.store = store;
		this.rt = rt;
		this.pp = pp;
	}
	
	public void setRscan(RunScanner rscan) {
		this.rscan = rscan;
	}

	private void omsg(String msg) {
		rt.omsg(msg);
	}
	
	private void omsgz(String msg) {
		rt.omsgz(msg);
	}
	
	private void oprn(String msg) {
		System.out.println(msg);
	}

	private int runAltZcallStmt(int parmCount, int lbidx) {
		AddrNode addrNode;
		PageTyp pgtyp;
		Integer val;
		int rtnval;
		int i;

		for (i = 0; i < parmCount; i++) {
			addrNode = store.fetchSpare();
			if (addrNode == null) {
				return STKOVERFLOW;
			}
			pgtyp = addrNode.getHdrPgTyp();
			val = pp.nodeToIntVal(addrNode, lbidx);
			if (val == null) {
				omsgz("runAltZcallStmt: rtn = " + rt.getLastErrCode()); 
				omsg(", i = " + i);
				return rt.getLastErrCode();
			}
			omsgz("runAltZcallStmt: i/pc = " + i + "/" + parmCount);
			omsgz(", val = " + val);
			omsg(", pgtyp = " + pgtyp);
			rtnval = pp.storeLocGlbInt(i, val, pgtyp, false);
			if (rtnval < 0) {
				return rtnval;
			}
		}
		return 0;
	}
	
	public int getCountOfPrintSpares(KeywordTyp kwtyp) {
		AddrNode addrNode;
		PageTyp pgtyp;
		int addr;
		int count = 0;
		
		store.initSpareStkIdx();
		while (true) {
			addrNode = store.popSpare();
			if (addrNode == null) {
				return STKUNDERFLOW;
			}
			addr = addrNode.getAddr();
			pgtyp = addrNode.getHdrPgTyp();
			omsgz("getCountOfPrintSpares: addr = " + addr); 
			omsg(", pgtyp = " + pgtyp);
			if ((addr == kwtyp.ordinal()) && (pgtyp == PageTyp.KWD)) {
				break;
			}
			count++;
		}
		addrNode = store.fetchSpare();  // pop the PRINTLN
		if (addrNode == null) { 
			return STKOVERFLOW; 
		}
		return count;
	}
	
	private int getCountOfSpares(KeywordTyp kwtyp) {
		AddrNode addrNode;
		PageTyp pgtyp;
		int addr;
		int count = 0;
		boolean isZkwd;
		
		store.initSpareStkIdx();
		while (true) {
			addrNode = store.popSpare();
			if (addrNode == null) {
				return STKUNDERFLOW;
			}
			if (!addrNode.isInzValid()) {
				return NOVARINZ;
			}
			addrNode.setInz();
			addr = addrNode.getAddr();
			pgtyp = addrNode.getHdrPgTyp();
			isZkwd = 
				(addr == KeywordTyp.ZCALL.ordinal()) ||
				(addr == KeywordTyp.ZPROC.ordinal());
			omsg("getCountOfSpares: addr = " + addr + ", pgtyp = " + pgtyp);
			if (isZkwd && (pgtyp == PageTyp.KWD)) {
				break;
			}
			count++;
		}
		addrNode = store.fetchSpare();  // pop the PRINTLN/ZCALL
		if (addrNode == null) { 
			return STKOVERFLOW; 
		}
		return count;
	}
	
	private int getReturnpSpares() {
		AddrNode addrNode;
		int addr;
	
		addrNode = store.fetchSpare();  // pop the returnp
		if (addrNode == null) {
			return STKOVERFLOW; 
		}
		addr = addrNode.getAddr();
		if (addr < 0) {
			addr = -addr;
		}
		omsg("getReturnpSpares: rtn addr = " + addr);
		return addr;
	}
			
	private int getFuncIdxSpares() {
		AddrNode addrNode;
		int addr;
	
		addrNode = store.fetchSpare();  // pop the func var
		if (addrNode == null) {
			return STKOVERFLOW; 
		}
		addr = addrNode.getAddr();
		omsg("getFuncIdxSpares: rtn addr = " + addr);
		return addr;
	}
			
	public int pushZcallStmt(Node node) {
		KeywordTyp kwtyp;
		int rightp;
		
		omsg("pushZcallStmt: top");
		kwtyp = KeywordTyp.ZPROC;
		if (!pp.pushOp(KeywordTyp.ZCALL) || 
			!pp.pushOpAsNode(kwtyp) || !pp.pushInt(rt.getCurrZstmt())) 
		{
			return STKOVERFLOW;
		}
		rightp = rt.handleLeafToken(node);
		return rightp;
	}
	
	public int runZcallStmt() {
		// - popSpare returnp (go back here)
		// - popSpare func ref. (idx in func list)
		// - get ptr to func def parm list
		// - set locBaseIdx
		// - push parms, loc vars
		// - push varCount
		// - push old locBaseIdx
		// - push returnp
		// - push old local depth
		// - go to func body: return ptr to first zstmt
		int downp;
		int funcidx;
		int upNodep;
		int funcp;
		int firstp;
		int returnp;
		int currLocBase;
		Node node = null;
		Node upNode;
		KeywordTyp kwtyp = KeywordTyp.ZCALL;
		String funcName;
		String varName;
		int varCount = 0;  // includes parms, loc vars
		int parmCount;
		int parmFixedCount;
		int i, j, k;
		int rtnval;
		
		omsg("runZcallStmt: top");
		// EDBF: enable detection of bottom of current function in stack
		//   push an extra DO:
		if (!pp.pushOp(KeywordTyp.DO) || !pp.pushOp(KeywordTyp.DO)) {
			return STKOVERFLOW;
		}
		currLocBase = rt.getLocBaseIdx();
		k = getCountOfSpares(kwtyp);
		if (k < 0) {
			return k;
		}
		parmCount = k - 2;
		returnp = getReturnpSpares();
		if (returnp < 0) {
			return STKUNDERFLOW;
		}
		funcidx = getFuncIdxSpares();
		if (funcidx < 0) {
			return STKUNDERFLOW;
		}
		omsg("Zcall: funcidx = " + funcidx);
		upNodep = rt.glbFunList.get(funcidx);
		upNode = store.getNode(upNodep);
		funcp = upNode.getDownp();
		firstp = upNode.getRightp();
		node = store.getNode(firstp);
		firstp = node.getDownp();  // return firstp
		node = store.getNode(funcp);
		downp = node.getDownp();  // (_f_ x y)
		node = store.getNode(downp);
		downp = node.getDownp();
		funcName = store.getVarName(downp);
		varName = rscan.getFunVar(funcName);
		parmFixedCount = rt.glbLocVarMap.get(varName);  
		if (parmFixedCount != parmCount) {
			return BADPARMCT;
		}
		i = rt.glbLocVarMap.get(funcName);
		rt.setLocBaseIdx(store.getStkIdx() - parmCount);
		omsgz("runZcallStmt: lbidx = " + rt.getLocBaseIdx()); 
		omsg(", parmCount = " + parmCount);
		while (true) {
			if (varCount < parmCount) {
				varCount++;
				continue;
			}
			j = rt.glbLocVarList.get(i + varCount);
			if (j < 0) {
				break;
			}
			omsg("runZcallStmt: stkidx = " + store.getStkIdx());
			if (!pp.pushVal(varCount, PageTyp.INTVAL, LOCVAR)) {
				return STKOVERFLOW;
			}
			varCount++;
		}
		rtnval = runAltZcallStmt(parmCount, currLocBase);
		if (rtnval < 0) {
			omsg("Zcall: rtnval = " + rtnval);
			return rtnval;
		}
		if (!pp.pushInt(varCount)) {
			return STKOVERFLOW;
		}
		if (!pp.pushInt(currLocBase)) {
			return STKOVERFLOW;
		}
		if (!pp.pushInt(returnp)) {
			return STKOVERFLOW;
		}
		omsg("Zcall: locDepth = " + rt.getLocDepth());
		if (!pp.pushInt(rt.getLocDepth())) {
			return STKOVERFLOW;
		}
		// EDBF:
		if (!pp.pushOpAsNode(KeywordTyp.ZNULL)  
			//|| !pushOpAsNode(KeywordTyp.ZSTMT) 
			//|| !pushOpAsNode(KeywordTyp.NULL) 
		) { 
			return STKOVERFLOW;
		}
		omsg("Zcall: btm, firstp = " + firstp);
		return firstp;
	}
	
	public int runRtnStmt(boolean isExpr) {
		// - pop return value, pop null if none
		// - pop local depth
		// - pop returnp (go back here)
		// - if returnp=0 then done
		// - pop calling locBaseIdx
		// - pop varCount
		// - pop loc vars, parms
		// - pop until ZCALL, inclusive
		// - push return value if any
		// - set local depth
		// - return getRightp of returnp
		int rightp;
		int currLocBase;
		int varCount;
		int funcAddr = 0;
		int i;
		int rtnval;
		Integer val;
		int locDepth = rt.getLocDepth();
		boolean isDelayPops = false;
		AddrNode funcReturns = null;
		Node node;
		KeywordTyp kwtyp, kwd;
		
		omsg("runRtnStmt: top");
		rtnval = pp.popUntilDoRepeats();
		if (rtnval < 0) {
			return rtnval;
		}
		if (isExpr) {
			funcReturns = store.popNode(); 
			if (funcReturns == null) {
				return STKUNDERFLOW;
			}
			val = pp.nodeToIntVal(funcReturns, rt.getLocBaseIdx());
			if (val == null) {
				omsg("runRtnStmt: isExpr, rtn = " + rt.getLastErrCode()); 
				return rt.getLastErrCode();
 			}
			funcAddr = val;
			rt.setExprLoop(true);
			omsg("runRtnStmt: funcAddr = " + funcAddr);
			if (!pp.popSafeVal() || !pp.popSafeVal()) {
				return STKUNDERFLOW;
			}
		}
		//popVal(); // EDBF: pop NULL
		//popVal(); // EDBF: pop ZSTMT
		rtnval = pp.popUntilKwd(KeywordTyp.ZNULL);
		if (rtnval < 0) {
			return rtnval;
		}
		rt.setLocDepth(pp.popVal());
		omsg("runRtnStmt: locDepth = " + locDepth);
		if (locDepth == NEGBASEVAL) {
			return STKUNDERFLOW;
		}
		rightp = pp.popAbsVal(); // currZstmt/Zexpr
		if (rightp == -1) {
			return STKUNDERFLOW;
		}
		if (rightp == 0) {
			omsg("runRtnStmt: done");
			return EXIT; // done
		}
		omsg("runRtnStmt: top2");
		currLocBase = pp.popVal(); // locBaseIdx
		if (currLocBase < 0) {
			return STKUNDERFLOW;
		}
		omsg("runRtnStmt: top3");
		varCount = pp.popVal(); // varCount
		if (varCount < 0) {
			return STKUNDERFLOW;
		}
		omsg("runRtnStmt: varCount = " + varCount);
		isDelayPops = pp.isNonImmedLocVar(funcReturns);
		if (!isDelayPops) {
			omsg("runRtnStmt: popMulti");
			rtnval = pp.popMulti(varCount);
			if (rtnval < 0) {
				return rtnval;
			}
		}
		// push func rtnval if locDepth > 0:
		if (locDepth < 0) {
			return GENERR;  // not needed (just be safe)
		}
		if (locDepth == 0) { 
			omsg("runRtnStmt: locDepth = zero");
			rt.setExprLoop(false);
			if (isDelayPops) {
				omsg("runRtnStmt: zero LocD, popMulti");
				rtnval = pp.popMulti(varCount);
				if (rtnval < 0) {
					return rtnval;
				}
			}
			/*if (isExpr) { store.popNode(); }*/
		}
		else if (funcReturns == null) {
			omsg("runRtnStmt: funrtn is null, locDepth = " + locDepth);
			return FNCALLNORTNVAL;
		}
		else {  
			rtnval = pp.pushFuncRtnVal(funcAddr, funcReturns, isDelayPops,
				varCount);
			if (rtnval < 0) {
				return rtnval;
			}
			locDepth--;
			rt.setLocDepth(locDepth);
		}
		rt.setLocBaseIdx(currLocBase);
		omsg("runRtnStmt: btm, locDepth = " + locDepth);
		omsg("runRtnStmt: btm, rightp = " + rightp);
		node = store.getNode(rightp);
		rightp = node.getRightp();
		return rightp;
	}
	
	public int pushRtnStmt(Node node) {
		int rightp;
		KeywordTyp kwtyp;
		
		omsg("pushRtnStmt: top");
		kwtyp = KeywordTyp.RETURN;
		if (!pp.pushOp(kwtyp)) {
			return STKOVERFLOW;
		}
		rightp = node.getRightp();
		if (rightp == 0) { 
			omsg("pushRtnStmt: no expr");
			//rightp = runRtnStmt(false);
			return 0;
		}
		// (!pushOpAsNode(KeywordTyp.NULL)) 
		node = store.getNode(rightp);
		rightp = rt.handleExprToken(rightp, true);
		return rightp;
	}

	public int runGlbCall() {
		int i, j;
		int len;
		
		i = 0;
		len = rt.glbLocVarList.size();
		rt.setLocBaseIdx(store.getStkIdx());
		omsg("runGlbCall: locBaseIdx = " + rt.getLocBaseIdx());
		while (len > 0) {
			j = rt.glbLocVarList.get(i);
			if (j < 0) {
				break;
			}
			if (!pp.pushVal(i, PageTyp.INTVAL, GLBVAR)) {
				return STKOVERFLOW;
			}
			i++;
		}
		rt.setVarCountIdx(store.getStkIdx());
		if (!pp.pushInt(i)) {
			return STKOVERFLOW;
		}
		if (!pp.pushInt(0)) {
			return STKOVERFLOW;
		}
		return 0;
	}
	
}
