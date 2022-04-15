package page;

import java.util.ArrayList;
import java.util.HashMap;
import iconst.IConst;
import iconst.NodeCellTyp;
import iconst.PageTyp;

// bookTab has 1024 ptrs. each to different pageTab
// pageTab has 1024 ptrs. each to different page
// INTVAL page has 1024 ints
// other pages for non-int data values
// stackTab has 2 stacks: operands (nodes) and operators
// operand stack has Page of Lists; each list has 1024 AddrNodes
// operator stack has 256 Pages of bytes

public class Store implements IConst {

	private PageTab bookTab[];
	private PageTab stackTab;
	private AllocFree afInt;
	private AllocFree afFloat;
	private AllocFree afString;
	private AllocFree afLong;
	private AllocFree afByte;
	private AllocFree afNode;
	private AllocFree afList;
	private AllocFree afMap;
	private int bookLen;
	private int firstFree;
	private int currBookIdx;
	// int bookLen;
	// int firstFree;
	// afInt.firstBookIdx .. afString.firstBookIdx
	// nextIdx, prevIdx: several linked lists
	
	// PageTab:
	// int pageTabLen;
	// int firstFreeIdx;
	// int firstPageIdx;
	// int firstDensIdx;  // dense
	// nextIdx, prevIdx: 3 linked lists
	
	// Page:
	// int valcount;
	// int freeidx;  
	
	public Store() {
		PageTab pgtab;
		stackTab = new PageTab();
		bookTab = new PageTab[INTPGLEN];
		for (int i=0; i < INTPGLEN; i++) {
			bookTab[i] = null;
		}
		pgtab = new PageTab(PageTyp.INTVAL);
		bookTab[0] = pgtab;
		afInt = new AllocFree(PageTyp.INTVAL, this);
		afFloat = new AllocFree(PageTyp.FLOAT, this);
		afString = new AllocFree(PageTyp.STRING, this);
		afLong = new AllocFree(PageTyp.LONG, this);
		afByte = new AllocFree(PageTyp.BYTE, this);
		afNode = new AllocFree(PageTyp.NODE, this);
		afList = new AllocFree(PageTyp.LIST, this);
		afMap = new AllocFree(PageTyp.MAP, this);
		afInt.setFirst(0);
		firstFree = -1;
		bookLen = 1;
		currBookIdx = 0;
		pgtab.setCurrPageIdx(0);
	}
	
	public PageTab getPageTab(int idx) {
		return bookTab[idx];
	}
	
	public void setPageTab(int idx, PageTab pgtab) {
		bookTab[idx] = pgtab;
	}
	
	public Page getPage(int addr) {
		PageTab pgtab;
		Page page;
		int pageidx, pgtabidx;
		
		addr = addr >>> 12;
		pageidx = addr & 0x3FF;
		pgtabidx = addr >>> 10;
		pgtab = getPageTab(pgtabidx);
		page = pgtab.getPage(pageidx);
		return page;
	}
	
	public Page getPageZero(int pgidx) {
		PageTab pgtab = bookTab[0];
		return pgtab.getPage(pgidx);
	}
	
	public Page getIdxToPage(int idx) {
		PageTab pgtab;
		Page page;
		int pageidx, pgtabidx;
		
		if (idx < 0) {
			return null;
		}
		pageidx = idx & 0x3FF;
		pgtabidx = idx >>> 10;
		pgtab = getPageTab(pgtabidx);
		if (pgtab == null) {
			return null;
		}
		page = pgtab.getPage(pageidx);
		return page;
	}

	public int getElemIdx(int addr) {
		return (addr & 0xFFF);
	}
	
	public int getPageIdx(int addr) {
		return ((addr >>> 12) & 0x3FF);
	}
	
	public int getBookIdx(int addr) {
		return (addr >>> 22);
	}
	
	public boolean isLocAddr(int addr) {
		return (getBookIdx(addr) == 0);
	}
	
	public Page getCurrPage() {
		PageTab pgtab = bookTab[currBookIdx];
		int pageIdx = pgtab.getCurrPageIdx();
		Page page = pgtab.getPage(pageIdx);
		return page;
	}
	
	public int getCurrBookIdx() {
		return currBookIdx;
	}
	
	public void setCurrBookIdx(int idx) {
		currBookIdx = idx;
	}
	
	public Node getNode(int addr) {
		Page page;
		int idx;
		Node rtnval;
		
		page = getPage(addr);
		idx = getElemIdx(addr);
		rtnval = page.getNode(idx);
		return rtnval;
	}
	
	public AllocFree getAllocFree(PageTyp pgtyp) {
		switch (pgtyp) {
		case INTVAL: return afInt;
		case FLOAT: return afFloat;
		case STRING: return afString;
		case LONG: return afLong;
		case BYTE: return afByte;
		case NODE: return afNode;
		case LIST: return afList;
		case MAP: return afMap;
		default: return null;
		}
	}
	
	public AddrNode newAddrNode(int header, int addr) {
		// reuse nodes at end of stack
		AddrNode node;
		if (stackTab.isMaxStkIdx()) {
			node = new AddrNode(header, addr);
		}
		else {
			node = stackTab.getUpperNode();
			node.setHeader(header);
			node.setAddr(addr);
		}
		return node;
	}

	public int allocInt(int val) {
		afInt.setInt(val);
		return afInt.alloc();
	}
	
	public boolean freeInt(Page page, int idx) {
		return afInt.free(page, idx);
	}
	
	public int allocLong(long val) {
		afLong.setLong(val);
		return afLong.alloc();
	}
	
	public boolean freeLong(Page page, int idx) {
		return afLong.free(page, idx);
	}
	
	public int allocFloat(double val) {
		afFloat.setFloat(val);
		return afFloat.alloc();
	}
	
	public boolean freeFloat(Page page, int idx) {
		return afFloat.free(page, idx);
	}
	
	public int allocString(String str) {
		afString.setStr(str);
		return afString.alloc();
	}
	
	public boolean freeString(Page page, int idx) {
		return afString.free(page, idx);
	}
	
	public int allocByte(boolean flag) {
		afByte.setByte(flag);
		return afByte.alloc();
	}
	
	public boolean freeByte(Page page, int idx) {
		return afByte.free(page, idx);
	}
	
	public int allocNode(Node node) {
		afNode.setNode(node);
		return afNode.alloc();
	}
	
	public boolean freeNode(Page page, int idx) {
		return afNode.free(page, idx);
	}
	
	public int allocList(ArrayList<AddrNode> list) {
		afList.setList(list);
		return afList.alloc();
	}
	
	public boolean freeList(Page page, int idx) {
		return afList.free(page, idx);
	}
	
	public int allocMap(HashMap<String, AddrNode> map) {
		afMap.setMap(map);
		return afMap.alloc();
	}
	
	public boolean freeMap(Page page, int idx) {
		return afMap.free(page, idx);
	}

	public int getAddr(int i, int j, int k) {
		int n;
		n = (i << 10) | j;
		n = (n << 12) | k;
		return n;
	}
	
	public long packHdrAddr(int header, int addr) {
		return stackTab.packHdrAddr(header, addr);
	}
	
	public boolean isNodeStkEmpty() {
		return stackTab.isNodeStkEmpty();
	}
	
	public boolean isOpStkEmpty() {
		return stackTab.isOpStkEmpty();
	}
	
	public AddrNode topNode() {
		return stackTab.topNode();
	}
	
	public AddrNode popNode() {
		return stackTab.popNode();
	}
	
	public boolean pushNode(AddrNode node) {
		return stackTab.pushNode(node);
	}

	public AddrNode fetchNode(int stkidx) {
		return stackTab.fetchNode(stkidx);
	}
	
	public AddrNode fetchRelNode(int depth) {
		return stackTab.fetchRelNode(depth);
	}
	
	public void writeNode(int stkidx, int val, PageTyp pgtyp) {
		stackTab.writeNode(stkidx, val, pgtyp);
	}
	
	public void writeRelNode(int depth, int val, PageTyp pgtyp) {
		stackTab.writeRelNode(depth, val, pgtyp);
	}
	
	public boolean swapNodes() {
		return stackTab.swapNodes();
	}
	
	public int getStkIdx() {
		return stackTab.getStkIdx();
	}
	
	public void putStkIdx(int stkidx) {
		stackTab.putStkIdx(stkidx);
	}
	
	public void initSpareStkIdx() {
		stackTab.initSpareStkIdx();
	}
	
	public AddrNode popSpare() {
		return stackTab.popSpare();
	}
	
	public AddrNode fetchSpare() {
		return stackTab.fetchSpare();
	}
	
	public long topLong() {
		return stackTab.topLong();
	}
	
	public long popLong() {
		return stackTab.popLong();
	}
	
	public boolean pushLong(long val) {
		return stackTab.pushLong(val);
	}

	public byte topByte() {
		return stackTab.topByte();
	}
	
	public byte popByte() {
		return stackTab.popByte();
	}
	
	public boolean pushByte(byte byteval) {
		return stackTab.pushByte(byteval);
	}
	
	public boolean appendNodep(int nodep, int lineno) {
		return stackTab.appendNodep(nodep, lineno);
	}
	
	public int lookupLineNo(int matchp) {
		return stackTab.lookupLineNo(matchp);
	}
	
	public Node getSubNode(Node node) {
		int downp;
		int idx;
		Page page;
		
		if (node.getDownCellTyp() != NodeCellTyp.PTR) {
			return null;
		}
		downp = node.getDownp();
		page = getPage(downp);
		idx = getElemIdx(downp);
		node = page.getNode(idx);
		return node;
	}

	public String getVarName(int downp) {
		Page page;
		int idx;

		page = getPage(downp);
		idx = getElemIdx(downp);
		return page.getString(idx);
	}

	public void setVarName(int downp, String s) {
		Page page;
		int idx;

		page = getPage(downp);
		idx = getElemIdx(downp);
		page.setString(idx, s);
	}

	public void pokeNode(int rightp, int ival) {
		Page page;
		int idx;
		Node node;
		
		page = getPage(rightp);
		idx = getElemIdx(rightp);
		node = page.getNode(idx);
		node.setDownp(ival);
		page.setNode(idx, node);
	}
	
	public int getBookLen() {
		return bookLen;
	}
	
	public void setBookLen(int idx) {
		bookLen = idx;
	}
	
	public int getFirstFree() {
		return firstFree;
	}
	
	public void setFirstFree(int idx) {
		firstFree = idx;
	}
	
	public String printStkIdxs() {
		return stackTab.printStkIdxs();
	}
	
	@SuppressWarnings("unused")
	private void omsg(String msg) {
		stackTab.omsg(msg);
	}
	
}

class PageTab implements IConst {
	
	private Page pageTab[];
	private Page nodepg, nodelstpg;
	private int nodeStkLstIdx;
	private int nodeStkIdx;
	private int opStkPgIdx;
	private int opStkIdx;
	private int nodeLstIdx, nodeMastIdx;
	private int spareStkLstIdx;
	private int spareStkIdx;
	private int maxStkIdx;
	private PageTyp pgtyp;
	private boolean isFree;
	private int nextIdx;  // index to bookTab
	private int firstFreeIdx;
	private int firstPageIdx;
	private int firstDensIdx;
	private int currPageIdx;
	private int pageTabLen;
	
	public PageTab(PageTyp pgtyp) {
		Page page;
		pageTab = new Page[INTPGLEN];
		for (int i=0; i < INTPGLEN; i++) {
			pageTab[i] = null;
		}
		page = new Page(pgtyp);
		pageTab[0] = page;
		this.pgtyp = pgtyp;
		pageTabLen = 1;
		isFree = true;
	}
	
	public PageTab() {
		Page page;
		ArrayList<AddrNode> list = initStkLst(NODESTKLEN);
		ArrayList<Integer> nodeplist = initNodepLst(NODESTKLEN);

		pageTab = new Page[OPSTKLEN];
		for (int i=0; i < OPSTKLEN; i++) {
			pageTab[i] = null;
		}
		opStkPgIdx = 0;
		opStkIdx = 0;
		page = new Page(PageTyp.BYTE);
		pageTab[opStkPgIdx] = page;

		nodeStkLstIdx = 0;
		nodeStkIdx = 0;
		nodepg = new Page(PageTyp.LIST);
		nodepg.setList(0, list);
		for (int i=1; i < INTPGLEN; i++) {
			nodepg.setList(i, null);
		}

		maxStkIdx = 0;
		nodeMastIdx = 0;
		nodeLstIdx = 0;
		nodelstpg = new Page(PageTyp.LIST);
		nodelstpg.setList(0, nodeplist);
		for (int i=1; i < INTPGLEN; i++) {
			nodelstpg.setList(i, null);
		}
	}
	
	public Page getPage(int idx) {
		return pageTab[idx];
	}
	
	public void setPage(int idx, Page page) {
		pageTab[idx] = page;
	}
	
	public int getCount() {
		return pageTabLen;
	}
	
	public void setCount(int n) {
		pageTabLen = n;
	}
	
	public boolean getFree() {
		return isFree;
	}
	
	public void setFree(boolean flag) {
		isFree = flag;
	}
	
	public PageTyp getPageTyp() {
		return pgtyp;
	}
	
	public int getCurrPageIdx() {
		return currPageIdx;
	}
	
	public void setCurrPageIdx(int idx) {
		currPageIdx = idx;
	}
	
	public int getFirstPageIdx() {
		return firstPageIdx;
	}
	
	public void setFirstPageIdx(int idx) {
		firstPageIdx = idx;
	}
	
	public int getFirstFreeIdx() {
		return firstFreeIdx;
	}
	
	public void setFirstFreeIdx(int idx) {
		firstFreeIdx = idx;
	}
	
	public int getFirstDensIdx() {
		return firstDensIdx;
	}
	
	public void setFirstDensIdx(int idx) {
		firstDensIdx = idx;
	}
	
	public int getNextBookIdx() {
		return nextIdx;
	}
	
	public void setNextBookIdx(int idx) {
		nextIdx = idx;
	}
	
	private ArrayList<AddrNode> initStkLst(int len) {
		ArrayList<AddrNode> list = new ArrayList<AddrNode>(); 
		AddrNode node;

		list.clear();
		for (int i=0; i < len; i++) {
			node = new AddrNode(0, 0);
			list.add(node);
		}
		return list;
	}
	
	private ArrayList<Integer> initNodepLst(int len) {
		ArrayList<Integer> list = new ArrayList<Integer>(); 

		list.clear();
		for (int i=0; i < len; i++) {
			list.add(0);
		}
		return list;
	}
	
	public String printStkIdxs() {
		return "(" + opStkPgIdx + ',' + opStkIdx + " - " + 
			nodeStkLstIdx + ',' + nodeStkIdx  + ')';
	}
	
	public long packHdrAddr(int header, int addr) {
		long h;
		long rtnval;

		h = header & 0xFFFF;
		rtnval = (h << 32) | (addr & 0xFFFFFFFF);
		return rtnval;
	}
	
	public boolean isNodeStkEmpty() {
		return (nodeStkIdx == 0 && nodeStkLstIdx == 0);
	}
	
	public boolean isOpStkEmpty() {
		return (opStkIdx == 0 && opStkPgIdx == 0);
	}
	
	@SuppressWarnings("unchecked")
	public AddrNode getUpperNode() {
		AddrNode node;
		ArrayList<AddrNode> list;
		int topStkIdx = getStkIdx();
		int topIdx, topLstIdx;
		
		topIdx = topStkIdx % NODESTKLEN;
		topLstIdx = topStkIdx / NODESTKLEN;
		list = (ArrayList<AddrNode>) nodepg.getList(topLstIdx);
		node = list.get(topIdx);
		return node;
	}
	
	@SuppressWarnings("unchecked")
	public AddrNode topNode() {
		int idx;
		AddrNode node;
		ArrayList<AddrNode> list;
		
		if (nodeStkIdx > 0) {
			list = (ArrayList<AddrNode>) nodepg.getList(nodeStkLstIdx);
			node = list.get(nodeStkIdx - 1);
		}
		else if (nodeStkLstIdx > 0) {
			list = (ArrayList<AddrNode>) nodepg.getList(nodeStkLstIdx - 1);
			idx = NODESTKLEN - 1;
			node = list.get(idx);
		}
		else {
			node = null;
		}
		return node;
	}
	
	@SuppressWarnings("unchecked")
	public AddrNode popNode() {
		AddrNode node;
		ArrayList<AddrNode> list;
		
		if (nodeStkIdx > 0) {
			list = (ArrayList<AddrNode>) nodepg.getList(nodeStkLstIdx);
			node = list.get(--nodeStkIdx);
			omsg("Popped " + node.getAddr());
		}
		else if (nodeStkLstIdx > 0) {
			list = (ArrayList<AddrNode>) nodepg.getList(--nodeStkLstIdx);
			nodeStkIdx = NODESTKLEN - 1;
			node = list.get(nodeStkIdx);
			omsg("Popped " + node.getAddr());
		}
		else {
			node = null;
		}
		return node;
	}
	
	@SuppressWarnings("unchecked")
	public boolean pushNode(AddrNode node) {
		int header = node.getHeader();
		int addr = node.getAddr();
		AddrNode addrNode;
		ArrayList<AddrNode> list;
		int pval;
		boolean isKwd;
		String s = "";

		if (nodeStkIdx < NODESTKLEN) { 
			list = (ArrayList<AddrNode>) nodepg.getList(nodeStkLstIdx);
		}
		else if (nodeStkLstIdx < INTPGLEN - 1) {
			list = (ArrayList<AddrNode>) nodepg.getList(++nodeStkLstIdx);
			if (list == null) {
				list = initStkLst(NODESTKLEN);
				nodepg.setList(nodeStkLstIdx, list);
			}
			nodeStkIdx = 0;
		}
		else {
			return false;
		}
		addrNode = list.get(nodeStkIdx++);
		addrNode.setHeader(header);
		addrNode.setAddr(addr);
		pval = addrNode.getAddr();
		isKwd = (addrNode.getHdrPgTyp() == PageTyp.KWD);
		if (isKwd) {
			s = " KWD, stkidx = " + getStkIdx();
		}
		omsg("Pushed " + pval + s);
		return true;
	}

	@SuppressWarnings("unchecked")
	public AddrNode fetchNode(int stkidx) {
		AddrNode node;
		ArrayList<AddrNode> list;
		int myStkIdx, myStkLstIdx;
		
		myStkIdx = stkidx % NODESTKLEN;
		myStkLstIdx = stkidx / NODESTKLEN;
		list = (ArrayList<AddrNode>) nodepg.getList(myStkLstIdx);
		node = list.get(myStkIdx);
		return node;
	}
	
	public AddrNode fetchRelNode(int depthIdx) {
		int stkidx;
		stkidx = getStkIdx() - depthIdx - 1;
		if (stkidx < 0) {
			return null;
		}
		return fetchNode(stkidx);
	}
	
	@SuppressWarnings("unchecked")
	public void writeNode(int stkidx, int val, PageTyp pgtyp) {
		AddrNode node;
		ArrayList<AddrNode> list;
		int myStkIdx, myStkLstIdx;
		
		myStkIdx = stkidx % NODESTKLEN;
		myStkLstIdx = stkidx / NODESTKLEN;
		list = (ArrayList<AddrNode>) nodepg.getList(myStkLstIdx);
		node = list.get(myStkIdx);
		node.setAddr(val);
		node.setValue();
		node.setHdrPgTyp(pgtyp);
	}
	
	public void writeRelNode(int depthIdx, int val, PageTyp pgtyp) {
		int stkidx;
		stkidx = getStkIdx() - depthIdx - 1;
		if (stkidx < 0) {
			return;
		}
		writeNode(stkidx, val, pgtyp);
	}
	
	public boolean swapNodes() {
		AddrNode topNode, node;
		int header, addr;
		
		if (getStkIdx() < 2) {
			return false;
		}
		topNode = popNode();
		node = popNode();
		header = node.getHeader();
		addr = node.getAddr();
		// Warning: using newAddrNode caused error
		//node = newAddrNode(header, addr); // bad!
		node = new AddrNode(header, addr);
		pushNode(topNode);
		pushNode(node);
		return true;
	}
	
	public int getStkIdx() {
		int rtnval;
		rtnval = (nodeStkLstIdx << 10) + nodeStkIdx;
		return rtnval;
	}
	
	public void putStkIdx(int stkidx) {
		nodeStkIdx = stkidx % NODESTKLEN;
		nodeStkLstIdx = stkidx / NODESTKLEN;
	}
	
	public void initSpareStkIdx() {
		spareStkLstIdx = nodeStkLstIdx;
		spareStkIdx = nodeStkIdx;
	}
	
	public int getSpareStkIdx() {
		int rtnval;
		rtnval = (spareStkLstIdx << 10) + spareStkIdx;
		return rtnval;
	}
	
	public boolean isMaxStkIdx() {
		int stkIdx = getStkIdx();
		boolean flag = stkIdx > maxStkIdx;
		
		if (flag) {
			maxStkIdx = stkIdx;
		}
		return flag;
	}
	
	@SuppressWarnings("unchecked")
	public AddrNode popSpare() {
		AddrNode node;
		ArrayList<AddrNode> list;
		int stkidx = getSpareStkIdx();
		
		if (spareStkIdx > 0) {
			list = (ArrayList<AddrNode>) nodepg.getList(spareStkLstIdx);
			node = list.get(--spareStkIdx);
		}
		else if (spareStkLstIdx > 0) {
			list = (ArrayList<AddrNode>) nodepg.getList(--spareStkLstIdx);
			spareStkIdx = NODESTKLEN - 1;
			node = list.get(spareStkIdx);
		}
		else {
			node = null;
		}
		omsg("PopSpare: " + node.getAddr() + ", stkidx = " + stkidx);
		return node;
	}
	
	@SuppressWarnings("unchecked")
	public AddrNode fetchSpare() {
		AddrNode addrNode;
		ArrayList<AddrNode> list;

		if (getSpareStkIdx() >= getStkIdx()) {
			return null;
		}
		if (spareStkIdx < NODESTKLEN) { 
			list = (ArrayList<AddrNode>) nodepg.getList(spareStkLstIdx);
		}
		else if (spareStkLstIdx < INTPGLEN - 1) {
			list = (ArrayList<AddrNode>) nodepg.getList(++spareStkLstIdx);
			if (list == null) {
				list = initStkLst(NODESTKLEN);
				nodepg.setList(spareStkLstIdx, list);
			}
			spareStkIdx = 0;
		}
		else {
			return null;
		}
		addrNode = list.get(spareStkIdx++);
		return addrNode;
	}
	
	@SuppressWarnings("unchecked")
	public boolean appendNodep(int nodep, int lineno) {
		ArrayList<Integer> list;

		if (nodeLstIdx < NODESTKLEN) { 
			list = (ArrayList<Integer>) nodelstpg.getList(nodeMastIdx);
		}
		else if (nodeMastIdx < INTPGLEN - 1) {
			list = (ArrayList<Integer>) nodelstpg.getList(++nodeMastIdx);
			if (list == null) {
				list = initNodepLst(NODESTKLEN);
				nodelstpg.setList(nodeMastIdx, list);
			}
			nodeLstIdx = 0;
		}
		else {
			return false;
		}
		list.set(nodeLstIdx++, nodep);
		list.set(nodeLstIdx++, lineno);
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public int lookupLineNo(int matchp) {
		ArrayList<Integer> list;
		int mastIdx, idx;
		int maxLstIdx;
		int nodep, lineno;
		
		for (mastIdx = 0; mastIdx <= nodeMastIdx; mastIdx++) {
			list = (ArrayList<Integer>) nodelstpg.getList(mastIdx);
			if (mastIdx < nodeMastIdx) {
				maxLstIdx = NODESTKLEN;
			}
			else {
				maxLstIdx = nodeLstIdx;
			}
			for (idx = 0; idx < maxLstIdx; idx += 2) {
				nodep = list.get(idx);
				lineno = list.get(idx + 1);
				if (matchp == nodep) {
					return lineno;
				}
			}
		}
		return 0;
	}

	@SuppressWarnings("unchecked")
	public long topLong() {
		int header, addr;
		int idx;
		long rtnval;
		AddrNode node;
		ArrayList<AddrNode> list;
		
		if (nodeStkIdx > 0) {
			list = (ArrayList<AddrNode>) nodepg.getList(nodeStkLstIdx);
			node = list.get(nodeStkIdx - 1);
		}
		else if (nodeStkLstIdx > 0) {
			list = (ArrayList<AddrNode>) nodepg.getList(nodeStkLstIdx - 1);
			idx = NODESTKLEN - 1;
			node = list.get(idx);
		}
		else {
			return -1;
		}
		header = node.getHeader();
		addr = node.getAddr();
		rtnval = packHdrAddr(header, addr);
		return rtnval;
	}
	
	@SuppressWarnings("unchecked")
	public long popLong() {
		int header, addr;
		long rtnval;
		AddrNode node;
		ArrayList<AddrNode> list;
		
		list = (ArrayList<AddrNode>) nodepg.getList(nodeStkLstIdx);
		if (nodeStkIdx > 0) {
			node = list.get(--nodeStkIdx);
		}
		else if (nodeStkLstIdx > 0) {
			list = (ArrayList<AddrNode>) nodepg.getList(--nodeStkLstIdx);
			nodeStkIdx = NODESTKLEN - 1;
			node = list.get(nodeStkIdx);
		}
		else {
			return -1;
		}
		header = node.getHeader();
		addr = node.getAddr();
		rtnval = packHdrAddr(header, addr);
		return rtnval;
	}
	
	@SuppressWarnings("unchecked")
	public boolean pushLong(long val) {
		int header = (int)(val >>> 32);
		int addr = (int) val;
		AddrNode addrNode;
		ArrayList<AddrNode> list;

		if (nodeStkIdx < NODESTKLEN) { 
			list = (ArrayList<AddrNode>) nodepg.getList(nodeStkLstIdx);
		}
		else if (nodeStkLstIdx < INTPGLEN - 1) {
			list = (ArrayList<AddrNode>) nodepg.getList(++nodeStkLstIdx);
			if (list == null) {
				list = initStkLst(NODESTKLEN);
				nodepg.setList(nodeStkLstIdx, list);
			}
			nodeStkIdx = 0;
		}
		else {
			return false;
		}
		addrNode = list.get(nodeStkIdx++);
		addrNode.setHeader(header);
		addrNode.setAddr(addr);
		return true;
	}

	public byte topByte() {
		byte byteval;
		int idx;
		Page page = pageTab[opStkPgIdx];
		
		if (opStkIdx > 0) {
			byteval = page.getByte(opStkIdx - 1);
		}
		else if (opStkPgIdx > 0) {
			page = pageTab[opStkPgIdx - 1];
			idx = BYTPGLEN - 1;
			byteval = page.getByte(idx);
		}
		else {
			byteval = 0;
		}
		return byteval;
	}
	
	public byte popByte() {
		byte byteval;
		Page page = pageTab[opStkPgIdx];
		
		if (opStkIdx > 0) {
			byteval = page.getByte(--opStkIdx);
		}
		else if (opStkPgIdx > 0) {
			page = pageTab[--opStkPgIdx];
			opStkIdx = BYTPGLEN - 1;
			byteval = page.getByte(opStkIdx);
		}
		else {
			byteval = 0;
		}
		return byteval;
	}
	
	public boolean pushByte(byte byteval) {
		Page page;
		
		if (opStkIdx < BYTPGLEN) {
			page = pageTab[opStkPgIdx];
			page.setByte(opStkIdx++, byteval);
		}
		else if (opStkPgIdx < OPSTKLEN - 1) {
			page = pageTab[++opStkPgIdx];
			if (page == null) {
				page = new Page(PageTyp.BYTE);
				pageTab[opStkPgIdx] = page;
			}
			opStkIdx = 0;
			page.setByte(opStkIdx++, byteval);
		}
		else {
			return false;
		}
		return true;
	}

	@SuppressWarnings("unused")
	private void out(boolean flag, String msg) {
		if (flag) {
			System.out.println(msg);
		}
	}
	
	public void omsg(String msg) {
		if (isrtbug) {
			System.out.println(msg);
		}
	}
	
}

class DataRec {
	
	public int intVal;
	public long longVal;
	public double floatVal;
	public byte byteVal;
	public String strVal;
	public Node nodeVal;
	public ArrayList<AddrNode> listVal;
	public HashMap<String, AddrNode> mapVal;
	
	public DataRec(){}
	
}

class AllocFree implements IConst {
	
	private PageTyp pageTyp;
	private DataRec datarec;
	private Store store;
	private int firstBookIdx;
	private int currPageIdx;
	private PageTab currPgTab;
	private int currBookIdx;
	
	public AllocFree(PageTyp pgtyp, Store store) {
		pageTyp = pgtyp;
		datarec = new DataRec();
		this.store = store;
		firstBookIdx = -1;
		currBookIdx = -1;
	}
	// int bookLen;
	// int firstFree;
	// afInt.firstBookIdx .. afString.firstBookIdx
	// nextIdx, prevIdx: several linked lists
	
	// PageTab:
	// int pageTabLen;
	// int firstFreeIdx;
	// int firstPageIdx;
	// int firstDensIdx;  // dense
	// nextIdx, prevIdx: 3 linked lists
	
	// Page:
	// int valcount;
	// int freeidx;  
	
	public int alloc() {
		// use singly linked list
		Page page;
		PageTab pgtab;
		PageTyp pgtyp;
		boolean isFirstIter = true;
		int addr;
		int bookLen;
		int firstFree;
		
		while (true) {
			if (!isFirstIter) {
				currPgTab = store.getPageTab(currBookIdx);
				currPageIdx = currPgTab.getFirstPageIdx();
			}
			if (firstBookIdx >= 0) {
				currPgTab.setFree(false);
				page = currPgTab.getPage(currPageIdx);
				addr = pageAlloc(page);
				if (addr >= 0) {
					pgtyp = page.getPageTyp();
					out("alloc: addr = " + addr + ", pgtyp = " +
						pgtyp);
					return addr;
				}
				addr = allocInner();
				if (addr >= 0) {
					out("alloc: post Inner, addr = " + addr);
					return addr;
				}
			}
			if (isFirstIter) {
				isFirstIter = false;
				currBookIdx = firstBookIdx;
			}
			else {
				currBookIdx = currPgTab.getNextBookIdx();
			}
			if (currBookIdx >= 0) {
				continue;
			}
			bookLen = store.getBookLen();
			firstFree = store.getFirstFree();
			if ((bookLen >= INTPGLEN) && (firstFree < 0)) {
				return -1;  // out of memory
			}
			if (firstFree < 0) {
				currBookIdx = bookLen;
				bookLen++;
				store.setBookLen(bookLen);
				pgtab = store.getPageTab(currBookIdx);
				if (pgtab == null) {
					pgtab = new PageTab(pageTyp);
					store.setPageTab(currBookIdx, pgtab);
				}
			}
			else {
				currBookIdx = firstFree;
				currPgTab = store.getPageTab(firstFree);
				firstFree = currPgTab.getNextBookIdx();
				currPgTab.setFirstFreeIdx(firstFree);
			}
			if (firstBookIdx < 0) {
				firstBookIdx = currBookIdx;
			}
		}
	}
	
	public int allocInner() {
		// untested
		PageTab pgtab;
		Page page, pg;
		int addr;
		int nextIdx;
		int prevIdx;
		int firstFree;
		int pgidx;
		int len;
		
		out("allocInner: top");
		pgtab = store.getPageTab(currBookIdx);
		while (true) {
			page = pgtab.getPage(currPageIdx);
			if (!page.isFullPage()) {
				break;
			}
			nextIdx = page.getNext();
			// handle full page:
			// append page to full list:
			pgidx = pgtab.getFirstDensIdx();
			page.setNext(pgidx);
			pgtab.setFirstDensIdx(currPageIdx);
			pg = pgtab.getPage(pgidx);
			pg.setPrev(currPageIdx);
			// remove page from page list:
			prevIdx = page.getPrev();
			if (prevIdx < 0) {
				pgtab.setFirstPageIdx(nextIdx);
			}
			else {
				pg = pgtab.getPage(prevIdx);
				pg.setNext(nextIdx);
			}
			page.setPrev(-1);
			if (nextIdx >= 0) {
				currPageIdx = nextIdx;
				page = pgtab.getPage(currPageIdx);
				page.setPrev(prevIdx);
				continue;
			}
			firstFree = pgtab.getFirstFreeIdx();
			if (firstFree >= 0) {
				currPageIdx = firstFree;
				// remove page from free list:
				page = pgtab.getPage(currPageIdx);
				nextIdx = page.getNext();
				pgtab.setFirstFreeIdx(nextIdx);
				if (nextIdx < 0) {
					break;
				}
				pg = pgtab.getPage(nextIdx);
				pg.setPrev(-1);
				break;
			}
			if (pgtab.getCount() >= INTPGLEN) {
				return -1;  // curr PageTab is full
			}
			currPageIdx = pgtab.getCount();
			len = currPageIdx + 1;
			pgtab.setCount(len);
			page = pgtab.getPage(currPageIdx);
			if (page == null) {
				page = new Page(pageTyp);
			}
			// append curr pg to page list:
			pgidx = pgtab.getFirstPageIdx();
			page.setNext(pgidx);
			pgtab.setFirstPageIdx(currPageIdx);
			pg = pgtab.getPage(pgidx);
			pg.setPrev(currPageIdx);
			page.setPrev(-1);
			break;
		}
		page = pgtab.getPage(currPageIdx);
		addr = pageAlloc(page);
		return addr;  // never -ve
	}

	private int pageAlloc(Page page) {
		int rtnval;
		int idx;

		idx = pageAllocRtn(page);
		if (idx < 0) {
			return -1;
		}
		rtnval = store.getAddr(currBookIdx, currPageIdx, idx);
		return rtnval;
	}
	
	private int pageAllocRtn(Page page) {
		switch (pageTyp) {
		case INTVAL: return page.allocInt(datarec.intVal);
		case FLOAT: return page.allocFloat(datarec.floatVal);
		case STRING: return page.allocString(datarec.strVal);
		case LONG: return page.allocLong(datarec.longVal);
		case LIST: return page.allocList(datarec.listVal);
		case MAP: return page.allocMap(datarec.mapVal);
		case NODE: return page.allocNode(datarec.nodeVal);
		case BYTE: return page.allocByte(datarec.byteVal);
		default: return -1;
		}
	}
	
	private int pageFree(Page page, int idx) {
		switch (pageTyp) {
		case INTVAL: 
		case FLOAT:
		case STRING: 
		case LONG:
		case LIST:
		case MAP:
		case NODE:
		case BYTE:
			return page.freeNum(idx);
		default: return RESERR;
		}
	}
	
	public boolean free(Page page, int idx) {
		// call pageFree(page, idx)
		// error out on failure
		// if result = RESFREE then keep going
		PageTab pgtab;
		Page pg;
		int nextIdx;
		int prevIdx;
		int pgidx;
		int pageLen;
		int rtnval;
		
		rtnval = pageFree(page, idx);
		if (rtnval == RESERR) {
			return false;
		}
		if (rtnval == RESOK) {
			return true;
		}
		// otherwise RESFREE
		pgtab = store.getPageTab(currBookIdx);
		nextIdx = page.getNext();
		// handle empty page:
		// append page to empty list:
		pgidx = pgtab.getFirstFreeIdx();
		if (pgidx >= 0) {
			pg = pgtab.getPage(pgidx);
			pg.setPrev(currPageIdx);
		}
		page.setNext(pgidx);
		pgtab.setFirstFreeIdx(currPageIdx);
		// remove page from page list:
		prevIdx = page.getPrev();
		if (prevIdx < 0) {
			pgtab.setFirstPageIdx(nextIdx);
		}
		else {
			pg = pgtab.getPage(prevIdx);
			pg.setNext(nextIdx);
		}
		if (nextIdx >= 0) {
			pg = pgtab.getPage(nextIdx);
			pg.setPrev(prevIdx);
		}
		page.setPrev(-1);
		pageLen = pgtab.getCount();
		if (currPageIdx < (pageLen - 1)) {
			return true;
		}
		while (true) {
			// top of pageLen in pgtab record
			// shrink pgtab
			pageLen--;
			pgtab.setCount(pageLen);
			if (pageLen <= 0) {
				break;
			}
			page = pgtab.getPage(pageLen - 1);
			if (page.getValCount() > 0) {
				// page not empty at top of pageLen
				return true;
			}
			// snip out of empty list
			nextIdx = page.getNext();
			prevIdx = page.getPrev();
			if (nextIdx >= 0) { 
				pg = pgtab.getPage(nextIdx);
				pg.setPrev(prevIdx);
			}
			if (prevIdx >= 0) {
				pg = pgtab.getPage(prevIdx);
				pg.setNext(nextIdx);
			}
			else {
				pgtab.setFirstFreeIdx(nextIdx);
			}
		}
		return freeEmptyPgTab(pgtab);
	}
		
	private boolean freeEmptyPgTab(PageTab pgtab) {	
		// empty pgtab record
		// snip out of pgtab list:
		PageTab currPgTab;
		int nextIdx;
		int bookIdx;
		int firstFree;
		int bookLen;
		int saveIdx;
		
		pgtab.setFree(true);
		nextIdx = pgtab.getNextBookIdx();
		currPgTab = null;
		bookIdx = firstBookIdx;
		while (bookIdx != currBookIdx) {
			currPgTab = store.getPageTab(bookIdx);
			bookIdx = currPgTab.getNextBookIdx();
		}
		if (currPgTab == null) {
			firstBookIdx = nextIdx;
		}
		else {
			currPgTab.setNextBookIdx(nextIdx);
		}
		// insert in free pgtab list:
		firstFree = store.getFirstFree();
		pgtab.setNextBookIdx(firstFree);
		store.setFirstFree(currBookIdx);
		bookLen = store.getBookLen();
		if (currBookIdx < (bookLen - 1)) {
			currBookIdx = getFirstAtEnd(nextIdx);
			return true;
		}
		// shrink bookTab, since top of bookLen:
		while (true) {
			bookLen--;
			store.setBookLen(bookLen);
			if (bookLen <= 0) {
				currBookIdx = getFirstAtEnd(nextIdx);
				return true;
			}
			bookIdx = bookLen - 1;
			pgtab = store.getPageTab(bookIdx);
			if (!pgtab.getFree() || (pgtab.getPageTyp() != pageTyp)) {
				currBookIdx = getFirstAtEnd(nextIdx);
				return true;
			}
			// snip currBookIdx out of free list:
			saveIdx = nextIdx;
			nextIdx = pgtab.getNextBookIdx();
			currPgTab = null;
			bookIdx = store.getFirstFree();
			while (bookIdx != currBookIdx) {
				currPgTab = store.getPageTab(bookIdx);
				bookIdx = currPgTab.getNextBookIdx();
			}
			if (currPgTab == null) {
				store.setFirstFree(nextIdx);
			}
			else {
				currPgTab.setNextBookIdx(nextIdx);
			}
			nextIdx = saveIdx;
		}
	}
	
	private int getFirstAtEnd(int nextIdx) {
		if (nextIdx < 0) {
			nextIdx = firstBookIdx;
		}
		return nextIdx;
	}
	
	public void setFirst(int firstidx) {
		firstBookIdx = firstidx;
	}
	
	public int getFirst() {
		return firstBookIdx;
	}
	
	public void setLong(long longVal) {
		datarec.longVal = longVal;
	}

	public void setFloat(double floatVal) {
		datarec.floatVal = floatVal;
	}

	public void setByte(boolean boolVal) {
		datarec.byteVal = (byte)(boolVal ? 1 : 0);
	}

	public void setStr(String strVal) {
		datarec.strVal = strVal;
	}

	public void setNode(Node nodeVal) {
		datarec.nodeVal = nodeVal;
	}

	public void setInt(int intVal) {
		datarec.intVal = intVal;
	}

	public void setList(ArrayList<AddrNode> listVal) {
		datarec.listVal = listVal;
	}

	public void setMap(HashMap<String, AddrNode> mapVal) {
		datarec.mapVal = mapVal;
	}

	public void out(String msg) {
		if (debug) {
		//if (true) {
			System.out.println(msg);
		}
	}

}