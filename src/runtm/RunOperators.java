package runtm;

import iconst.IConst;
import iconst.KeywordTyp;
import iconst.PageTyp;
import iconst.RunConst;
import page.AddrNode;
import page.Page;
import page.Store;

public class RunOperators implements IConst, RunConst {

	private Store store;
	private RunPushPop pp;
	
	public RunOperators(Store store, RunPushPop pp) {
		this.store = store;
		this.pp = pp;
	}
	
	private void omsg(String msg) {
		if (debug) {
			System.out.println(msg);
		}
	}
	
	public int handleExprKwdRtn(KeywordTyp kwtyp) {
		switch (kwtyp) {
		case ADD: return runAddExpr();
		case MPY: return runMpyExpr();
		case MINUS: return runMinusExpr();
		case DIV: return runDivExpr();
		default:
			omsg("handleExprKwdRtn: kwtyp = " + kwtyp);
			return NEGBASEVAL - kwtyp.ordinal();
		}
	}

	private int runAddExpr() {
		long sum = 0L;
		AddrNode addrNode;
		Page page;
		int addr;
		int idx;
		double fsum = 0.0;
		double dval = 0.0;
		long longval = 0L;
		int stkidx;
		boolean isFloat;
		boolean isLong;
		boolean isResFloat = false;
		boolean isResLong = false;
		boolean isNewFloat;
		int rtnval;
		
		while (true) {
			stkidx = popIntStk();
			if (stkidx < 0) {
				return stkidx;
			}
			addrNode = store.fetchNode(stkidx);
			if (isNullKwd(addrNode)) {
				break;
			}
			addr = addrNode.getAddr();
			isFloat = (addrNode.getHdrPgTyp() == PageTyp.FLOAT);
			isLong = (addrNode.getHdrPgTyp() == PageTyp.LONG);
			isNewFloat = isFloat && !isResFloat;
			isResFloat = isResFloat || isFloat;
			isResLong = isResLong || isLong;
			isResLong = isResLong && !isResFloat;
			if (isFloat) {
				page = store.getPage(addr);
				idx = store.getElemIdx(addr);
				dval = page.getFloat(idx);
			}
			else if (isLong) {
				page = store.getPage(addr);
				idx = store.getElemIdx(addr);
				longval = page.getLong(idx);
			}
			else {
				longval = getIntOffStk(stkidx);
			}
			if (isNewFloat) {
				fsum = sum + dval;
			}
			else if (isFloat) {
				fsum += dval;
			}
			else if (isResFloat) {
				fsum += longval;
			}
			else {
				sum += longval;
			}
		}
		if (isResFloat) {
			rtnval = pushFloat(fsum);
		}
		else if (isResLong) {
			rtnval = pushLong(sum);
		}
		else { 
			rtnval = pushIntStk((int)sum) ? 0 : STKOVERFLOW;
		}
		return rtnval;
	}
	
	private int runMpyExpr() {
		long product = 1;
		double fproduct = 1.0;
		long longval = 0L;
		AddrNode addrNode;
		Page page;
		int addr;
		int idx;
		double dval = 0.0;
		int stkidx;
		boolean isFloat;
		boolean isLong;
		boolean isResFloat = false;
		boolean isResLong = false;
		boolean isNewFloat;
		int rtnval;

		while (true) {
			stkidx = popIntStk();
			if (stkidx < 0) {
				return stkidx;
			}
			addrNode = store.fetchNode(stkidx);
			if (isNullKwd(addrNode)) {
				break;
			}
			addr = addrNode.getAddr();
			omsg("runMpyExpr: stkidx = " + stkidx +
				", addr = " + addr);
			isFloat = (addrNode.getHdrPgTyp() == PageTyp.FLOAT);
			isLong = (addrNode.getHdrPgTyp() == PageTyp.LONG);
			isNewFloat = isFloat && !isResFloat;
			isResFloat = isResFloat || isFloat;
			isResLong = isResLong || isLong;
			isResLong = isResLong && !isResFloat;
			if (isFloat) {
				page = store.getPage(addr);
				idx = store.getElemIdx(addr);
				dval = page.getFloat(idx);
			}
			else if (isLong) {
				page = store.getPage(addr);
				idx = store.getElemIdx(addr);
				longval = page.getLong(idx);
			}
			else {
				longval = getIntOffStk(stkidx);
			}
			if (isNewFloat) {
				fproduct = product * dval;
			}
			else if (isFloat) {
				fproduct *= dval;
			}
			else if (isResFloat) {
				fproduct *= longval;
			}
			else {
				product *= longval;
			}
		}
		if (isResFloat) {
			rtnval = pushFloat(fproduct);
		}
		else if (isResLong) {
			rtnval = pushLong(product);
		}
		else { 
			rtnval = pushIntStk((int)product) ? 0 : STKOVERFLOW;
		}
		return rtnval;
	}
	
	private int runDivExpr() {
		AddrNode addrNode;
		Page page;
		int addr;
		int idx;
		double denom;
		double base;
		double quotient;
		int stkidx;
		boolean isFloat;
		boolean isLong;
		int rtnval;
		
		omsg("runDivExpr: top");
		stkidx = popIntStk();
		if (stkidx < 0) {
			return stkidx;
		}
		addrNode = store.fetchNode(stkidx);
		addr = addrNode.getAddr();
		isFloat = (addrNode.getHdrPgTyp() == PageTyp.FLOAT);
		isLong = (addrNode.getHdrPgTyp() == PageTyp.LONG);
		if (isFloat) {
			page = store.getPage(addr);
			idx = store.getElemIdx(addr);
			denom = page.getFloat(idx);
		}
		else if (isLong) {
			page = store.getPage(addr);
			idx = store.getElemIdx(addr);
			denom = page.getLong(idx);
		}
		else {
			denom = getIntOffStk(stkidx);
		}
		if (denom == 0.0) {
			return ZERODIV;
		}
		stkidx = popIntStk();
		if (stkidx < 0) {
			return stkidx;
		}
		addrNode = store.fetchNode(stkidx);
		addr = addrNode.getAddr();
		isFloat = (addrNode.getHdrPgTyp() == PageTyp.FLOAT);
		isLong = (addrNode.getHdrPgTyp() == PageTyp.LONG);
		if (isFloat) {
			page = store.getPage(addr);
			idx = store.getElemIdx(addr);
			base = page.getFloat(idx);
		}
		else if (isLong) {
			page = store.getPage(addr);
			idx = store.getElemIdx(addr);
			base = page.getLong(idx);
		}
		else {
			base = getIntOffStk(stkidx);
		}
		quotient = base / denom;
		rtnval = pushFloat(quotient);
		return rtnval;
	}
	
	private int runMinusExpr() {
		AddrNode addrNode;
		Page page;
		int addr;
		int idx;
		long delta = 0;
		long base = 0;
		long diff = 0;
		int stkidx;
		int rtnval;
		boolean isDeltaFloat;
		boolean isBaseFloat;
		boolean isDeltaLong;
		boolean isBaseLong;
		boolean isResFloat;
		boolean isResLong;
		boolean isInt;
		double fdelta = 0.0;
		double fbase = 0.0;
		double fdiff = 0.0;
		
		omsg("runMinusExpr: top");
		stkidx = popIntStk();
		if (stkidx < 0) {
			return stkidx;
		}
		addrNode = store.fetchNode(stkidx);
		addr = addrNode.getAddr();
		isDeltaFloat = (addrNode.getHdrPgTyp() == PageTyp.FLOAT);
		isDeltaLong = (addrNode.getHdrPgTyp() == PageTyp.LONG);
		if (isDeltaFloat) {
			page = store.getPage(addr);
			idx = store.getElemIdx(addr);
			fdelta = page.getFloat(idx);
		}
		else if (isDeltaLong) {
			page = store.getPage(addr);
			idx = store.getElemIdx(addr);
			delta = page.getLong(idx);
		}
		else {
			delta = getIntOffStk(stkidx);
		}
		stkidx = popIntStk();
		if (stkidx < 0) {
			return stkidx;
		}
		addrNode = store.fetchNode(stkidx);
		addr = addrNode.getAddr();
		isBaseFloat = (addrNode.getHdrPgTyp() == PageTyp.FLOAT);
		isBaseLong = (addrNode.getHdrPgTyp() == PageTyp.LONG);
		if (isBaseFloat) {
			page = store.getPage(addr);
			idx = store.getElemIdx(addr);
			fbase = page.getFloat(idx);
		}
		else if (isBaseLong) {
			page = store.getPage(addr);
			idx = store.getElemIdx(addr);
			base = page.getLong(idx);
		}
		else {
			base = getIntOffStk(stkidx);
		}
		isInt = !isDeltaFloat && !isBaseFloat;
		if (isInt) {
			diff = base - delta;
		}
		else if (isDeltaFloat && isBaseFloat) {
			fdiff = fbase - fdelta;
		}
		else if (isDeltaFloat) {
			fdiff = base - fdelta;
		}
		else {
			fdiff = fbase - delta;
		}
		omsg("runMinusExpr: base, delta, diff = " + base +
				" " + delta + " " + diff);
		omsg("runMinusExpr: fbase, fdelta, fdiff = " + fbase +
				" " + fdelta + " " + fdiff);
		isResFloat = isDeltaFloat || isBaseFloat;
		isResLong = isDeltaLong || isBaseLong;
		if (isResFloat) {
			rtnval = pushFloat(fdiff);
		}
		else if (isResLong) {
			rtnval = pushLong(diff);
		}
		else { 
			rtnval = pushIntStk((int)diff) ? 0 : STKOVERFLOW;
		}
		return rtnval;
	}
	
	private int getIntOffStk(int stkidx) {
		return pp.getIntOffStk(stkidx);
	}
	
	private int popIntStk() {
		return pp.popIntStk();
	}
	
	private boolean isNullKwd(AddrNode addrNode) {
		return pp.isNullKwd(addrNode);
	}
	
	private int pushLong(long val) {
		return pp.pushLong(val);
	}
	
	private int pushFloat(double val) {
		return pp.pushFloat(val);
	}
	
	private int pushString(String val) {
		return pp.pushString(val);
	}
	
	private boolean pushInt(int val) {
		return pp.pushInt(val);
	}
	
	private boolean pushIntStk(int val) {
		return pp.pushIntStk(val);
	}
	
	private boolean pushIntVar(int val, int locVarTyp, boolean ptrFlag) {
		return pp.pushIntVar(val, locVarTyp, ptrFlag);
	}
	
	private boolean pushBoolStk(int val) {
		return pp.pushBoolStk(val);
	}
	
}
