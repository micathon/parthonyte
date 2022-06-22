package runtm;

import iconst.IConst;
import iconst.KeywordTyp;
import iconst.PageTyp;
import iconst.RunConst;
import page.AddrNode;
import page.Page;
import page.Store;

class RunPushPop implements IConst, RunConst {
	
	private Store store;
	private RunTime rt;
	private int locBaseIdx;
	
	public RunPushPop(Store store, RunTime rt) {
		this.store = store;
		this.rt = rt;
		locBaseIdx = rt.getLocBaseIdx();
	}
	
	private void omsg(String s) {
		rt.omsg(s);
	}
	
	private void setNegInt(boolean flag) {
		rt.setNegInt(flag);
	}
	
	private void setLastErrCode(int errCode) {
		rt.setLastErrCode(errCode);
	}
	
	public void setLocBaseIdx(int idx) {
		locBaseIdx = idx;
	}
	
	public KeywordTyp popKwd() {
		KeywordTyp kwtyp;
		int ival;
		
		ival = (int)store.popByte();
		kwtyp = KeywordTyp.values[ival];
		return kwtyp;
	}
	
	public int stripIntSign(int val) {
		if (val == 0x80000000) {
			return 0;
		}
		val = (val < 0) ? -val : val;
		return val;
	}
	
	public int packIntSign(boolean isNeg, int val) {
		if (isNeg && (val == 0)) {
			return 0x80000000;
		}
		val = isNeg ? -val : val;
		return val;
	}
	
	public int popIdxVal() {
		AddrNode addrNode;
		int rtnval = -1;
		
		addrNode = store.popNode();
		if (addrNode != null) {
			rtnval = addrNode.getAddr();
		}
		return rtnval;
	}
	
	public int getIntOffStk(int stkidx) {
		AddrNode addrNode;
		int rtnval;
		
		addrNode = store.fetchNode(stkidx);
		rtnval = addrNode.getAddr();
		omsg("getIntOffStk: stkidx = " + stkidx + ", rtn = " + rtnval);
		return rtnval;
	}
	
	public int popIntStk() {
		AddrNode addrNode;
		int locVarTyp;
		int varidx;
		int rtnval;
		boolean ptrFlag;

		addrNode = store.popNode(); 
		if (addrNode == null){
			return rt.STKUNDERFLOW;
		}
		rtnval = store.getStkIdx();
		ptrFlag = addrNode.isPtr();
		locVarTyp = addrNode.getHdrLocVarTyp();
		omsg("popIntStk: ptrFlag = " + ptrFlag);
		if (ptrFlag && addrNode.getHdrNonVar()) {
			return addrNode.getAddr();
		}
		switch (locVarTyp) {
		case NONVAR: 
			omsg("popIntStk: nonvar, rtn = " + rtnval);
			return rtnval;
		case LOCVAR:
		case GLBVAR:
			varidx = addrNode.getAddr();
			if (addrNode.getHdrLocVar()) {
				varidx += locBaseIdx;
			}
			omsg("popIntStk: varidx, rtn = " + rtnval);
			return varidx;
		default: return BADINTVAL;
		}
	}
	
	public boolean isNullKwd(AddrNode addrNode) {
		PageTyp pgtyp;
		int addr;
		boolean rtnval;
		
		addr = addrNode.getAddr();
		pgtyp = addrNode.getHdrPgTyp(); 
		rtnval = (pgtyp == PageTyp.KWD) && (addr == KeywordTyp.NULL.ordinal());
		return rtnval;
	}
	
	public int popInt(boolean isKwd) {
		// assume set stmt. only handles integer vars/values
		// isKwd: if null popped, then not an error
		AddrNode addrNode;
		
		addrNode = store.popNode();
		return popIntRtn(addrNode, isKwd);
	}
	
	public int popIntFromNode(AddrNode addrNode) {
		return popIntRtn(addrNode, false);
	}
	
	public int popIntRtn(AddrNode addrNode, boolean isKwd) {
		// assume set stmt. only handles integer vars/values
		// isKwd: if null popped, then not an error
		PageTyp pgtyp;
		int locVarTyp;
		int addr, varidx;
		int rtnval;
		
		if (addrNode == null) {
			return STKUNDERFLOW;
		}
		addr = addrNode.getAddr();
		pgtyp = addrNode.getHdrPgTyp(); 
		if (pgtyp != PageTyp.KWD) { }
		else if (!isKwd || (addr != KeywordTyp.NULL.ordinal())) {
			return KWDPOPPED;
		}
		else {
			return NULLPOPPED;  // not an error
		}
		locVarTyp = addrNode.getHdrLocVarTyp();
		switch (locVarTyp) {
		case NONVAR: 
			omsg("popIntRtn: nonvar, addr = " + addr);
			rtnval = addr;
			break;
		case LOCVAR:
		case GLBVAR:
			varidx = addr;
			if (addrNode.getHdrLocVar()) {
				varidx += locBaseIdx;
			}
			addrNode = store.fetchNode(varidx);
			pgtyp = addrNode.getHdrPgTyp(); 
			if (pgtyp != PageTyp.INTVAL) {
				return BADPOP;
			}
			rtnval = addrNode.getAddr();
			break;
		default: 
			return BADPOP;
		}
		setNegInt(rtnval < 0);
		rtnval = stripIntSign(rtnval);
		return rtnval;
	}
	
	public Integer popIObjFromNode(AddrNode addrNode) {
		// replaces popInt* functions
		PageTyp pgtyp;
		int locVarTyp;
		int addr, varidx;
		boolean flag;
		int rtnval;
		
		if (addrNode == null) {
			return setErrCode(STKUNDERFLOW);
		}
		addr = addrNode.getAddr();
		flag = addrNode.isPtr();  // debug
		omsg("popIObjFN: isPtr = " + flag);
		pgtyp = addrNode.getHdrPgTyp(); 
		if (pgtyp == PageTyp.KWD) { 
			return setErrCode(KWDPOPPED);
		}
		omsg("popIObjFN: pgtyp = " + pgtyp);
		locVarTyp = addrNode.getHdrLocVarTyp();
		switch (locVarTyp) {
		case NONVAR: 
			omsg("popIObjFN: nonvar, addr = " + addr);
			rtnval = addr;
			break;
		case LOCVAR:
		case GLBVAR:
			varidx = addr;
			if (addrNode.getHdrLocVar()) {
				varidx += locBaseIdx;
			}
			omsg("popIObjFN: varidx = " + varidx + 
				", locBaseIdx = " + locBaseIdx);
			addrNode = store.fetchNode(varidx);
			//pgtyp = addrNode.getHdrPgTyp(); 
			rtnval = addrNode.getAddr();
			break;
		default: 
			return setErrCode(BADPOP);
		}
		return rtnval;
	}
	
	public Integer setErrCode(int errCode) {
		setLastErrCode(errCode);
		return null;
	}
	
	public String popStrFromNode(AddrNode addrNode) {
		PageTyp pgtyp;
		Page page;
		int idx;
		int locVarTyp;
		int addr, varidx;
		int rtnval;
		String s;
		String err = "";
		boolean isImmed = true;
		double dval;
		long longval;
		
		if (addrNode == null) {
			return err;
		}
		addr = addrNode.getAddr();
		pgtyp = addrNode.getHdrPgTyp(); 
		if (pgtyp == PageTyp.KWD) { 
			return err;
		}
		locVarTyp = addrNode.getHdrLocVarTyp();
		switch (locVarTyp) {
		case NONVAR: 
			omsg("popIntRtn: nonvar, addr = " + addr);
			isImmed = (pgtyp == PageTyp.INTVAL);
			rtnval = addr;
			break;
		case LOCVAR:
		case GLBVAR:
			varidx = addr;
			if (addrNode.getHdrLocVar()) {
				varidx += locBaseIdx;
			}
			addrNode = store.fetchNode(varidx);
			pgtyp = addrNode.getHdrPgTyp(); 
			//pgtyp = PageTyp.FLOAT;
			isImmed = (pgtyp == PageTyp.INTVAL);
			rtnval = addrNode.getAddr();
			break;
		default: 
			return err;
		}
		if (isImmed) {
			//oprn("popStrFromNode: rtnval = " + rtnval);
			s = "" + rtnval;
			return s;
		}
		page = store.getPage(rtnval);
		idx = store.getElemIdx(rtnval);
		switch (pgtyp) {
		case LONG:
			longval = page.getLong(idx);
			omsg("popStrFromNode: longval = " + longval);
			s = "" + longval;
			break;
		case FLOAT:
			dval = page.getFloat(idx);
			omsg("popStrFromNode: dval = " + dval);
			s = "" + dval;
			break;
		case STRING:
			s = page.getString(idx);
			break;
		default:
			return err;
		}
		return s;
	}
	
	public int popVal() {
		AddrNode node;
		int val;
		
		node = store.popNode();
		if (node == null) {
			return NEGBASEVAL;
		}
		val = node.getAddr();
		return val;
	}
	
	public int storeInt(AddrNode node) {
		// assume set stmt. only handles integer vars/values
		AddrNode addrNode;
		int locVarTyp;
		int addr;
		int rtnval;
		int val;
		PageTyp pgtyp;
		boolean isGlb = true;
		
		val = node.getAddr();
		pgtyp = node.getHdrPgTyp();
		addrNode = store.popNode();
		if (addrNode == null) {
			return STKUNDERFLOW;
		}
		addr = addrNode.getAddr();
		/*
		pgtyp = addrNode.getHdrPgTyp(); 
		if (pgtyp != PageTyp.INTVAL) { 
			return BADSTORE;  
		}
		*/
		locVarTyp = addrNode.getHdrLocVarTyp();
		switch (locVarTyp) {
		case NONVAR: 
			return KWDPOPPED;
		case LOCVAR:
			isGlb = false;
		case GLBVAR:
			rtnval = storeLocGlbInt(addr, val, pgtyp, isGlb);
			if (rtnval < 0) {
				return rtnval;
			}
			break;
		default: return BADPOP;
		}
		return 0;
	}

	public int storeLocGlbInt(int varidx, int val, PageTyp pgtyp,
		boolean isGlb) 
	{
		omsg("storeLocGlbInt: varidx = " + varidx + ", val = " +
			val + ", pgtyp = " + pgtyp + ", lbidx = " + locBaseIdx);
		if (!isGlb) {
			varidx += locBaseIdx;
		}
		/*
		addrNode = store.fetchNode(varidx);
		addr = addrNode.getAddr();
		omsg("storeLocGlbInt: varidx = " + varidx + ", addr = " + addr);
		pgtyp = addrNode.getHdrPgTyp(); 
		*/
		store.writeNode(varidx, val, pgtyp);
		return 0;
	}
	
	public boolean pushVal(int val, PageTyp pgtyp, int locVarTyp) {
		AddrNode addrNode;
		addrNode = store.newAddrNode(0, val);
		//addrNode = new AddrNode(0, val);
		addrNode.setHdrPgTyp(pgtyp);
		if (pgtyp == PageTyp.KWD) {
			omsg("pushVal: pushing KWD!!!");
		}
		addrNode.setHdrLocVarTyp(locVarTyp);
		addrNode.setPtr();
		if (!store.pushNode(addrNode)) {
			return false;
		}
		return true;
	}
	
	public boolean pushAddr(int rightp) {
		AddrNode addrNode;
		addrNode = store.newAddrNode(0, rightp);
		if (!store.pushNode(addrNode)) {
			return false;
		}
		return true;
	}
	
	public int pushLong(long val) {
		AddrNode addrNode;
		int addr;
		long longval;
		Page page;
		int idx;
		
		addr = store.allocLong(val);
		if (addr < 0) {
			return BADALLOC;
		}
		//## debug code:
		page = store.getPage(addr);
		idx = store.getElemIdx(addr);
		longval = page.getLong(idx);
		omsg("pushLong: longval = " + longval + ", addr = " + addr);

		addrNode = store.newAddrNode(0, addr);
		addrNode.setHdrPgTyp(PageTyp.LONG);
		if (!store.pushNode(addrNode)) {
			return STKOVERFLOW;
		}
		return 0;
	}
	
	public int pushFloat(double val) {
		AddrNode addrNode;
		int addr;
		double dval;
		Page page;
		int idx;
		
		addr = store.allocFloat(val);
		if (addr < 0) {
			return BADALLOC;
		}
		//## debug code:
		page = store.getPage(addr);
		idx = store.getElemIdx(addr);
		dval = page.getFloat(idx);
		omsg("pushFloat: dval = " + dval + ", addr = " + addr);

		addrNode = store.newAddrNode(0, addr);
		addrNode.setHdrPgTyp(PageTyp.FLOAT);
		if (!store.pushNode(addrNode)) {
			return STKOVERFLOW;
		}
		return 0;
	}
	
	public int pushString(String val) {
		AddrNode addrNode;
		int addr;
		
		addr = store.allocString(val);
		if (addr < 0) {
			return BADALLOC;
		}
		addrNode = store.newAddrNode(0, addr);
		addrNode.setHdrPgTyp(PageTyp.STRING);
		if (!store.pushNode(addrNode)) {
			return STKOVERFLOW;
		}
		return 0;
	}
	
	public boolean pushInt(int val) {
		boolean rtnval;
		rtnval = pushIntVar(val, NONVAR, true);
		return rtnval;
	}
	
	public boolean pushIntStk(int val) {
		boolean rtnval;
		rtnval = pushIntVar(val, NONVAR, false);
		return rtnval;
	}
	
	public boolean pushIntVar(int val, int locVarTyp, boolean ptrFlag) {
		AddrNode addrNode;
		addrNode = store.newAddrNode(0, val);
		addrNode.setHdrPgTyp(PageTyp.INTVAL);
		addrNode.setHdrLocVarTyp(locVarTyp);
		if (ptrFlag) {
			addrNode.setPtr();
		}
		if (!store.pushNode(addrNode)) {
			return false;
		}
		return true;
	}
	
	public int pushIntMulti(int val, int varCount) {
		int rtnval;
		boolean flag;
		
		omsg("pushIntMulti: popMulti, string");
		rtnval = popMulti(varCount);
		if (rtnval < 0) {
			return rtnval;
		}
		flag = pushIntVar(val, NONVAR, false);
		return flag ? 0 : STKOVERFLOW;
	}
	
	public int pushFuncRtnVal(int val, AddrNode srcNode,
		boolean isDelayPops, int varCount) 
	{
		PageTyp pgtyp;
		int locVarTyp;
		int rtnval;
		boolean flag;
		
		pgtyp = srcNode.getHdrPgTyp();
		omsg("pushFuncRtnVal: pgtyp = " + pgtyp);
		if (isImmedTyp(pgtyp)) {
			//rtnval = pushIntMulti(val, varCount);
			flag = pushIntStk(val);
			return flag ? 0 : STKOVERFLOW;
		}
		locVarTyp = srcNode.getHdrLocVarTyp();
		if (locVarTyp == NONVAR) {
			rtnval = pushNonImmed(val, pgtyp, -1);
			return rtnval;
		}
		if (locVarTyp == GLBVAR) {
			omsg("pushFuncRtnVal: GLBVAR, val = " + val);
			rtnval = pushNonImmed(val, pgtyp, -1);
			return rtnval;
		}
		if (locVarTyp != LOCVAR || !isDelayPops) {
			return GENERR;
		}
		rtnval = pushNonImmed(val, pgtyp, varCount);
		return rtnval;
	}
	
	public int pushNonImmed(int addr, PageTyp pgtyp, int varCount) {
		Page page;
		int idx;
		long longval;
		double dval;
		String sval;
		int rtnval;

		page = store.getPage(addr);
		idx = store.getElemIdx(addr);
		if (pgtyp == PageTyp.LONG) {
			longval = page.getLong(idx);
			omsg("pushNonImmed: popMulti, long");
			rtnval = popMulti(varCount);
			if (rtnval < 0) {
				return rtnval;
			}
			rtnval = pushLong(longval);
			return rtnval;
		}
		else if (pgtyp == PageTyp.FLOAT) {
			dval = page.getFloat(idx);
			omsg("pushNonImmed: popMulti, float");
			rtnval = popMulti(varCount);
			if (rtnval < 0) {
				return rtnval;
			}
			rtnval = pushFloat(dval);
			return rtnval;
		}
		else if (pgtyp == PageTyp.STRING) {
			sval = page.getString(idx);
			omsg("pushNonImmed: popMulti, string");
			rtnval = popMulti(varCount);
			if (rtnval < 0) {
				return rtnval;
			}
			rtnval = pushString(sval);
			return rtnval;
		}
		else {
			return BADTYPE;
		}
	}
	
	public boolean isNonImmedLocVar(AddrNode addrNode) {
		PageTyp pgtyp;
		int locVarTyp;
		boolean rtnval;
		
		if (addrNode == null) {
			return false;
		}
		pgtyp = addrNode.getHdrPgTyp();
		locVarTyp = addrNode.getHdrLocVarTyp();
		rtnval = (!isImmedTyp(pgtyp)) && (locVarTyp == LOCVAR);
		return rtnval;
	}
	
	public boolean pushPtrVar(int val, int locVarTyp, PageTyp pgtyp) {
		AddrNode addrNode;
		addrNode = store.newAddrNode(0, val);
		addrNode.setHdrPgTyp(pgtyp);
		addrNode.setHdrLocVarTyp(locVarTyp);
		addrNode.setPtr();
		if (!store.pushNode(addrNode)) {
			return false;
		}
		return true;
	}
	
	public boolean isImmedTyp(PageTyp pgtyp) {
		return (pgtyp == PageTyp.INTVAL);
	}
	
	public boolean pushOp(KeywordTyp kwtyp) {
		byte byt = (byte)(kwtyp.ordinal());
		if (!store.pushByte(byt)) {
			return false;
		}
		return true;
	}
	
	public boolean pushOpAsNode(KeywordTyp kwtyp) {
		int val = kwtyp.ordinal();
		AddrNode addrNode;
		
		addrNode = store.newAddrNode(0, val);
		addrNode.setHdrPgTyp(PageTyp.KWD);
		omsg("pushOpAsNode: --------------------------");
		if (!store.pushNode(addrNode)) {
			return false;
		}
		return true;
	}
	
	public boolean pushVar(int varidx) {
		boolean isLocal;
		int stkidx;
		int locVarTyp;
		PageTyp pgtyp;
		AddrNode addrNode;
		
		isLocal = (varidx >= 0);
		if (isLocal) {
			locVarTyp = LOCVAR;
			stkidx = locBaseIdx + varidx;
		}
		else {
			varidx = -1 - varidx;
			locVarTyp = GLBVAR;
			stkidx = varidx;
			omsg("pushVar: in varidx < 0"); 
		}
		addrNode = store.fetchNode(stkidx);
		pgtyp = addrNode.getHdrPgTyp();
		//oprn("pushVar: pgtyp = " + pgtyp);
		if (pgtyp == PageTyp.KWD) {
			omsg("pushVar: KWD stkidx = " + stkidx);
		}
		if (pushVal(varidx, pgtyp, locVarTyp)) {
			omsg("pushVar: varidx = " + varidx + ", pgtyp = " + 
				pgtyp + ", locvartyp = " + locVarTyp);
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean pushVarQuote(int varidx) {
		boolean isLocal;
		int stkidx;
		int locVarTyp;
		PageTyp pgtyp;
		AddrNode addrNode;
		boolean rtnval = false;
		
		omsg("pushVarQuote: top");
		isLocal = (varidx >= 0);
		if (isLocal) {
			locVarTyp = LOCVAR;
			stkidx = locBaseIdx + varidx;
		}
		else {
			varidx = -1 - varidx;
			locVarTyp = GLBVAR;
			stkidx = varidx;
		}
		addrNode = store.fetchNode(stkidx);
		pgtyp = addrNode.getHdrPgTyp();
		switch (pgtyp) {
		case INTVAL:
			omsg("pushVarQuote: call pushIntVar");
			rtnval = pushIntVar(varidx, locVarTyp, true);
			break;
		case LONG:
		case FLOAT:
		case STRING:
			omsg("pushVarQuote: call pushPtrVar");
			rtnval = pushPtrVar(varidx, locVarTyp, pgtyp);
			break;
		default:
			omsg("pushVarQuote: default, pgtyp = " + pgtyp);
			return false;
		}
		omsg("pushVarQuote: rtnval = " + rtnval);
		return rtnval;
	}

	public int popMulti(int varCount) {
		KeywordTyp kwtyp = KeywordTyp.ZCALL;
		AddrNode node;
		PageTyp pgtyp;
		Page page;
		int idx;
		int addr;
		int i;
		int flagCount = 0;
		boolean flag;
		int rtnval;
		
		if (varCount < 0) {
			return 0;
		}
		for (i = 0; i < varCount; i++) {
			node = store.popNode();
			if (node == null) {
				return STKUNDERFLOW;
			}
			flag = false;
			addr = node.getAddr();
			page = store.getPage(addr);
			idx = store.getElemIdx(addr);
			pgtyp = node.getHdrPgTyp();
			omsg("popm: i = " + i + ", addr = " + addr + 
				", pgtyp = " + pgtyp);
			switch (pgtyp) {
			case LONG:
				omsg("popm: freeLong");
				flag = store.freeLong(page, idx);
				break;
			case FLOAT:
				//flag = page.freeFloat(idx);
				omsg("popm: freeFloat");
				flag = store.freeFloat(page, idx);
				break;
			case STRING:
				//flag = page.freeString(idx);
				omsg("popm: freeString");
				flag = store.freeString(page, idx);
				break;
			default:
				continue;
			}
			if (flag && (rt.getPopMultiFreeCount() >= 0)) {
				rt.incPopMultiFreeCount();
				++flagCount;
			}
			else {
				rt.setPopMultiFreeCount(-1);
			}
		}
		omsg("popMulti: flagCount = " + flagCount);
		rtnval = popUntilKwd(kwtyp);
		return rtnval;
	}
	
	public int popUntilKwd(KeywordTyp kwtyp) {
		AddrNode addrNode;
		PageTyp pgtyp;
		int addr;
		int count;
		
		count = 0;
		do {
			count++;
			addrNode = store.popNode();
			if (addrNode == null) {
				return STKUNDERFLOW;
			}
			addr = addrNode.getAddr();
			pgtyp = addrNode.getHdrPgTyp();
		} while (
			!(addr == kwtyp.ordinal() && (
			pgtyp == PageTyp.KWD))
		);
		omsg("popUntilKwd: btm, count = " + count);
		return 0;
	}
	
}
/*
private int fetchInt(AddrNode node) {
	int varidx;
	varidx = node.getAddr();
	varidx += locBaseIdx;
	node = store.fetchNode(varidx);
	return node.getAddr();
}

private AddrNode fetchStkNode(int varidx) {
	int stkidx;
	AddrNode node;
	
	stkidx = locBaseIdx + varidx;
	node = store.fetchNode(stkidx);
	return node;
}

private String getGdefunWord() {
	return "gdefun";
}

private boolean isGdefun(String s) {
	return s.equals("gdefun");
}
*/	
