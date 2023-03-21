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
	private RunTime rt;
	private RunPushPop pp;
	
	public RunOperators(Store store, RunTime rt, RunPushPop pp) {
		this.store = store;
		this.rt = rt;
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
		case XOR: return runXorExpr();
		case NOT:
		case NOTBITZ:
			return runUnaryExpr(kwtyp);
		case ANDBITZ: 
		case ORBITZ: 
		case XORBITZ: 
			return runBitwiseExpr(kwtyp);
		case AND:
		case OR:
			return runLogicalExpr(kwtyp);
		case QUEST:
			return runQuestExpr(kwtyp);
		default:
			omsg("handleExprKwdRtn: kwtyp = " + kwtyp);
			return NEGBASEVAL - kwtyp.ordinal();
		}
	}

	public int runSetStmt(KeywordTyp kwtyp) {
		int stkidx;
		AddrNode srcNode;
		AddrNode destNode;
		PageTyp pgtyp;
		Page page;
		int idx;
		int addr;
		long longval = 0;
		double dval = 0.0;
		String sval = "";
		boolean isLong = false;
		boolean isDup = true;
		
		omsg("runSetStmt: top");
		if (kwtyp != KeywordTyp.SET) {
			return runOpSetStmt(kwtyp);
		}
		srcNode = store.popNode(); 
		if (srcNode == null){
			return STKUNDERFLOW;
		}
		if (!srcNode.getHdrNonVar()) {
			srcNode = pp.getVarNode(srcNode);
		}
		addr = srcNode.getAddr();
		if (srcNode.isInt()) {
			page = null;
			idx = 0;
		}
		else {
			page = store.getPage(addr);
			idx = store.getElemIdx(addr);
		}
		pgtyp = srcNode.getHdrPgTyp();
		destNode = store.popNode();
		if (destNode == null) {
			return STKUNDERFLOW;
		}
		if (destNode.getHdrNonVar()) {
			omsg("runSetStmt: BADSET, addr = " + addr);
			return BADSETSTMT; 
		}
		stkidx = destNode.getAddr();
		if (destNode.getHdrLocVar()) {
			stkidx += rt.getLocBaseIdx();
		}
		destNode = pp.getVarNode(destNode);
		if (!pp.freeTarget(destNode, true, addr)) {
			return BADFREE; 
		}
		switch (pgtyp) {
		case LONG:
			longval = page.getLong(idx);
			omsg("runSetStmt: longval = " + longval);
			isLong = true;
			break;
		case FLOAT:
			dval = page.getFloat(idx);
			omsg("runSetStmt: dval = " + dval);
			break;
		case STRING:
			sval = page.getString(idx);
			omsg("runSetStmt: sval = " + sval);
			isDup = false;
			break;
		default:
			isDup = false;
		}
		if (!isDup) { }
		else if (isLong) {
			addr = store.allocLong(longval);
		}
		else {
			addr = store.allocFloat(dval);
		}
		if (isDup && (addr < 0)) {
			return BADALLOC;
		}
		store.writeNode(stkidx, addr, pgtyp);
		omsg("runSetStmt: stk = " + stkidx + ", addr = " + addr +
			", pgtyp = " + pgtyp);
		return 0;
	}
	
	public int runOpSetStmt(KeywordTyp kwtyp) {
		int stkidx;
		AddrNode srcNode;
		AddrNode destNode;
		PageTyp pgtyp;
		PageTyp pgtypdest;
		Page page;
		Page pagedest;
		int idx;
		int addr;
		int addrdest;
		int ival = 0;
		long longval = 0;
		double dval = 0.0;
		String sval = "";
		long longvaldest = 0;
		double dvaldest = 0.0;
		String svaldest;
		boolean isLong = false;
		boolean isFloat = false;
		boolean isStrExpr = false;
		
		omsg("runOpSetStmt: top");
		srcNode = store.popNode(); 
		if (srcNode == null){
			return STKUNDERFLOW;
		}
		if (!srcNode.getHdrNonVar()) {
			srcNode = pp.getVarNode(srcNode);
		}
		addr = srcNode.getAddr();
		if (srcNode.isInt()) {
			page = null;
			idx = 0;
		}
		else {
			page = store.getPage(addr);
			idx = store.getElemIdx(addr);
		}
		pgtyp = srcNode.getHdrPgTyp();
		destNode = store.popNode();
		if (destNode == null) {
			return STKUNDERFLOW;
		}
		if (destNode.getHdrNonVar()) {
			return BADSETSTMT; 
		}
		pgtypdest = destNode.getHdrPgTyp();
		stkidx = destNode.getAddr();
		if (destNode.getHdrLocVar()) {
			stkidx += rt.getLocBaseIdx();
		}
		destNode = pp.getVarNode(destNode);
		switch (pgtyp) {
		case LONG:
			longval = page.getLong(idx);
			omsg("runOpSetStmt: longval = " + longval);
			if ((kwtyp == KeywordTyp.DIVSET) && (longval == 0)) {
				return ZERODIV;
			}
			dval = longval;
			isLong = true;
			break;
		case FLOAT:
			dval = page.getFloat(idx);
			omsg("runOpSetStmt: dval = " + dval);
			if ((kwtyp == KeywordTyp.DIVSET) && (dval == 0.0)) {
				return ZERODIV;
			}
			isFloat = true;
			break;
		case STRING:
			sval = page.getString(idx);
			omsg("runOpSetStmt: sval = " + sval);
			isStrExpr = true;
			break;
		default:
			ival = addr;
			if ((kwtyp == KeywordTyp.DIVSET) && (ival == 0)) {
				return ZERODIV;
			}
			longval = ival;
			dval = ival;
		}
		addrdest = destNode.getAddr();
		omsg("runOpSetStmt: addrdest = " + addrdest +
			", pgtypdest = " + pgtypdest);
		switch (pgtypdest) {
		case LONG:
			if (isStrExpr) {
				return BADSETSTMT;
			}
			pagedest = store.getPage(addrdest);
			idx = store.getElemIdx(addrdest);
			longvaldest = pagedest.getLong(idx);
			dvaldest = longvaldest;
			isLong = true;
			break;
		case FLOAT:
			if (isStrExpr) {
				return BADSETSTMT;
			}
			pagedest = store.getPage(addrdest);
			idx = store.getElemIdx(addrdest);
			dvaldest = pagedest.getFloat(idx);
			omsg("runOpSetStmt: dvaldest = " + dvaldest +
					", pgtypdest = " + pgtypdest);
			isFloat = true;
			break;
		case STRING:
			pagedest = store.getPage(addrdest);
			idx = store.getElemIdx(addrdest);
			svaldest = pagedest.getString(idx);
			if (kwtyp != KeywordTyp.ADDSET) {
				return BADSETSTMT;
			}
			switch (pgtyp) {
			case LONG: sval = "" + longval; break;
			case FLOAT: sval = "" + dval; break;
			case STRING: break;
			default: 
				sval = "" + ival;
			}
			svaldest = svaldest + sval;
			addr = store.allocString(svaldest);
			store.writeNode(stkidx, addr, pgtypdest);
			return 0;
		default:
			longvaldest = addrdest;
			dvaldest = addrdest;
		}
		if (isFloat) {
			switch (kwtyp) {
			case ADDSET: dvaldest += dval; break;
			case MINUSSET: dvaldest -= dval; break;
			case MPYSET: dvaldest *= dval; break;
			case DIVSET: dvaldest /= dval; break;
			default:
				return BADSETSTMT;
			}
			if (!pp.freeTarget(destNode, true, addr)) {
				return BADFREE; 
			}
			pgtypdest = PageTyp.FLOAT;
			omsg("runOpSetStmt (2): dvaldest = " + dvaldest +
				", pgtypdest = " + pgtypdest);
			addr = store.allocFloat(dvaldest);
			store.writeNode(stkidx, addr, pgtypdest);
			return 0;
		}
		switch (kwtyp) {
		case ADDSET: longvaldest += longval; break;
		case MINUSSET: longvaldest -= longval; break; 
		case MPYSET: longvaldest *= longval; break; 
		case DIVSET: longvaldest /= longval; break; 
		case ANDBSET: longvaldest &= longval; break;
		case ORBSET: longvaldest |= longval; break;
		case XORBSET: longvaldest ^= longval; break;
		default:
			return BADSETSTMT;
		}
		if (isLong) {
			if (!pp.freeTarget(destNode, true, addr)) {
				return BADFREE; 
			}
			addr = store.allocLong(longvaldest);
			pgtypdest = PageTyp.LONG;
		}
		else {
			addr = (int) longvaldest;
			pgtypdest = PageTyp.INTVAL;
		}
		store.writeNode(stkidx, addr, pgtypdest);
		omsg("runOpSetStmt: stk = " + stkidx + ", addr = " + addr +
			", pgtyp = " + pgtypdest);
		return 0;
	}
	
	private int runAddExpr() {
		long sum = 0L;
		AddrNode addrNode;
		PageTyp pgtyp;
		Page page;
		int addr = -1;
		int idx;
		double fsum = 0.0;
		double dval = 0.0;
		long longval = 0L;
		int stkidx;
		String str = "";
		String s;
		boolean isFloat;
		boolean isLong;
		boolean isResFloat = false;
		boolean isResLong = false;
		boolean isNewFloat;
		boolean isConcat = false;
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
			pgtyp = addrNode.getHdrPgTyp();
			if ((pgtyp == PageTyp.STRING) && (addr == -1)) {
				isConcat = true;
			}
			if (!isConcat && !isNumeric(pgtyp)) {
				return BADOPTYP;
			}
			if (isConcat) {
				s = popStrFromNode(addrNode);
				str = str + s;
				continue;
			}
			addr = addrNode.getAddr();
			isFloat = (pgtyp == PageTyp.FLOAT);
			isLong = (pgtyp == PageTyp.LONG);
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
		if (isConcat) {
			rtnval = pushString(str);
		}
		else if (isResFloat) {
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
		PageTyp pgtyp;
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
			pgtyp = addrNode.getHdrPgTyp();
			omsg("runMpyExpr: stkidx = " + stkidx +
				", addr = " + addr);
			if (!isNumeric(pgtyp)) {
				return BADOPTYP;
			}
			isFloat = (pgtyp == PageTyp.FLOAT);
			isLong = (pgtyp == PageTyp.LONG);
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
		PageTyp pgtyp;
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
		pgtyp = addrNode.getHdrPgTyp();
		if (!isNumeric(pgtyp)) {
			return BADOPTYP;
		}
		addr = addrNode.getAddr();
		isFloat = (pgtyp == PageTyp.FLOAT);
		isLong = (pgtyp == PageTyp.LONG);
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
		pgtyp = addrNode.getHdrPgTyp();
		if (!isNumeric(pgtyp)) {
			return BADOPTYP;
		}
		addr = addrNode.getAddr();
		isFloat = (pgtyp == PageTyp.FLOAT);
		isLong = (pgtyp == PageTyp.LONG);
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
		PageTyp pgtyp;
		Page page;
		int addr;
		int idx;
		long delta = 0;
		long base = 0;
		long diff = 0;
		int stkidx;
		int rtnval;
		boolean isUnary;
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
		addrNode = store.topNode();
		addrNode = store.fetchNode(stkidx);
		pgtyp = addrNode.getHdrPgTyp();
		if (!isNumeric(pgtyp)) {
			return BADOPTYP;
		}
		addr = addrNode.getAddr();
		isDeltaFloat = (pgtyp == PageTyp.FLOAT);
		isDeltaLong = (pgtyp == PageTyp.LONG);
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
		if (!isNullKwd(addrNode)) { }
		else if (isDeltaFloat) {
			fdiff = -fdelta;
			rtnval = pushFloat(fdiff);
			return rtnval;
		}
		else if (isDeltaLong) {
			diff = -delta;
			rtnval = pushLong(diff);
			return rtnval;
		}
		else {
			diff = -delta;
			rtnval = pushIntStk((int)diff) ? 0 : STKOVERFLOW;
			return rtnval;
		}
		pgtyp = addrNode.getHdrPgTyp();
		if (!isNumeric(pgtyp)) {
			return BADOPTYP;
		}
		addr = addrNode.getAddr();
		isBaseFloat = (pgtyp == PageTyp.FLOAT);
		isBaseLong = (pgtyp == PageTyp.LONG);
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
	
	private int runLogicalExpr(KeywordTyp kwtyp) {
		AddrNode node;
		AddrNode subnode;
		int addr;
		
		omsg("runLogicalExpr: kwtyp = " + kwtyp);
		node = store.popNode();
		if (node == null) {
			return STKUNDERFLOW;
		}
		subnode = store.topNode();
		if (subnode != null) {
			addr = subnode.getAddr();
			if (subnode.getHdrPgTyp() == PageTyp.KWD) {
				if ((addr == 0) || (addr == 1)) {
					store.popNode();
				}
			}
		}
		node.setHdrPgTyp(PageTyp.BOOLEAN);
		if (!store.pushNode(node)) {
			return STKOVERFLOW;
		}
		return 0;
	}
	
	private int runQuestExpr(KeywordTyp kwtyp) {
		AddrNode node;
		omsg("runQuestExpr: kwtyp = " + kwtyp);
		return 0;
	}

	private int runXorExpr() {
		AddrNode addrNode;
		PageTyp pgtyp;
		int addr;
		int stkidx;
		boolean initFlag = false;
		boolean currFlag;
		int ival;
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
			pgtyp = addrNode.getHdrPgTyp();
			omsg("runXorExpr: stkidx = " + stkidx +
				", addr = " + addr);
			if (pgtyp != PageTyp.BOOLEAN) {
				return BADOPTYP;
			}
			currFlag = (addr == 1);
			initFlag = initFlag ^ currFlag;
		}
		ival = initFlag ? 1 : 0;
		currFlag = pushBoolStk(ival);
		rtnval = currFlag ? 0 : STKOVERFLOW;
		return rtnval;
	}
	
	private int runBitwiseExpr(KeywordTyp kwtyp) {
		long res = 0L;
		AddrNode addrNode;
		PageTyp pgtyp;
		Page page;
		int addr;
		int idx;
		long longval = 0L;
		int stkidx;
		boolean isLong;
		boolean isResLong = false;
		int rtnval;
		
		if (kwtyp == KeywordTyp.ANDBITZ) {
			res = 0xFFFFFFFFFFFFFFFFL;
		}
		while (true) {
			stkidx = popIntStk();
			if (stkidx < 0) {
				return stkidx;
			}
			addrNode = store.fetchNode(stkidx);
			if (isNullKwd(addrNode)) {
				break;
			}
			pgtyp = addrNode.getHdrPgTyp();
			if (!isIntLong(pgtyp)) {
				return BADOPTYP;
			}
			addr = addrNode.getAddr();
			isLong = (pgtyp == PageTyp.LONG);
			isResLong = isResLong || isLong;
			if (isLong) {
				page = store.getPage(addr);
				idx = store.getElemIdx(addr);
				longval = page.getLong(idx);
			}
			else {
				longval = getIntOffStk(stkidx);
			}
			switch (kwtyp) {
			case ANDBITZ:
				res &= longval;
				break;
			case ORBITZ:
				res |= longval;
				break;
			case XORBITZ:
				res ^= longval;
				break;
			default:
				return BADOPTYP;
			}
		}
		if (isResLong) {
			rtnval = pushLong(res);
		}
		else { 
			rtnval = pushIntStk((int)res) ? 0 : STKOVERFLOW;
		}
		return rtnval;
	}
	
	private int runUnaryExpr(KeywordTyp kwtyp) {
		AddrNode addrNode;
		PageTyp pgtyp;
		Page page;
		int addr;
		int idx;
		long delta = 0;
		int stkidx;
		int rtnval;
		boolean isDeltaLong;
		
		omsg("runUnaryExpr: top");
		stkidx = popIntStk();
		if (stkidx < 0) {
			return stkidx;
		}
		addrNode = store.fetchNode(stkidx);
		pgtyp = addrNode.getHdrPgTyp();
		if ((kwtyp == KeywordTyp.NOT) && 
			(pgtyp != PageTyp.BOOLEAN)) 
		{ 
			return BADOPTYP;
		}
		if ((kwtyp == KeywordTyp.NOTBITZ) &&
			!isIntLong(pgtyp)) 
		{
			return BADOPTYP;
		}
		addr = addrNode.getAddr();
		isDeltaLong = (pgtyp == PageTyp.LONG);
		if (isDeltaLong) {
			page = store.getPage(addr);
			idx = store.getElemIdx(addr);
			delta = page.getLong(idx);
		}
		else {
			delta = getIntOffStk(stkidx);
		}
		switch (kwtyp) {
		case NOT:
			delta = 1 - delta;
			break;
		case NOTBITZ:
			delta = ~delta;
			break;
		default:
			return BADOPTYP;
		}
		if (isDeltaLong) {
			rtnval = pushLong(delta);
		}
		else if (pgtyp == PageTyp.INTVAL) { 
			rtnval = pushIntStk((int)delta) ? 0 : STKOVERFLOW;
		}
		else if (pgtyp == PageTyp.BOOLEAN) {
			rtnval = pushBoolStk((int)delta) ? 0 : STKOVERFLOW;
		}
		else {
			return BADOPTYP;
		}
		return rtnval;
	}
	
	private boolean isNumNode(AddrNode node) {
		PageTyp pgtyp;
		boolean flag;
		
		pgtyp = node.getHdrPgTyp();
		flag = isNumeric(pgtyp);
		return flag;
	}
	
	private boolean isNumeric(PageTyp pgtyp) {
		boolean flag;
		flag = (pgtyp == PageTyp.INTVAL) ||
			(pgtyp == PageTyp.LONG) ||
			(pgtyp == PageTyp.FLOAT);
		return flag;
	}
	
	private boolean isIntLong(PageTyp pgtyp) {
		boolean flag;
		flag = (pgtyp == PageTyp.INTVAL) ||
			(pgtyp == PageTyp.LONG);
		return flag;
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
	
	private String popStrFromNode(AddrNode addrNode) {
		return pp.popStrFromNode(addrNode);
	}
	
}
