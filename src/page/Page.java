package page;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import iconst.IConst;
import iconst.PageTyp;

public class Page implements IConst {

	private PageTyp pgtyp;
	private int valcount;  // no. of used cells, including free cells
	private int freeidx;  // points to first free cell
	private int cellcount;  // total no. of cells in page, fixed value
	private byte bytes[];
	private short words[];
	private int intvals[];
	private double doubles[];
	private long longs[];
	private String strings[];
	private List<?> lists[];
	private HashMap<?, ?> maps[];
	private boolean gcbits[];  // will be used for garbage collection (future)
	//
	private int nextIdx;  // index to PageTab
	private int prevIdx;  // index to PageTab
	
	public Page(PageTyp pgtyp) {
		this.pgtyp = pgtyp;
		valcount = 0;
		freeidx = -1;
		nextIdx = -1;
		prevIdx = -1;
		switch (pgtyp) {
		case NODE:
			cellcount = WRDPGLEN;
			words = new short[cellcount];
			break;
		case INTVAL:
			cellcount = INTPGLEN;
			intvals = new int[cellcount];
			break;
		case BYTE:  // used for operator stack
			cellcount = BYTPGLEN;
			bytes = new byte[cellcount + BYTHDRLEN];
			return;
		case LONG:
			cellcount = DBLPGLEN;
			longs = new long[cellcount];
			break;
		case FLOAT:
			cellcount = DBLPGLEN;
			doubles = new double[cellcount];
			break;
		case STRING:
			cellcount = INTPGLEN;
			strings = new String[cellcount];
			break;
		case LIST:
			cellcount = INTPGLEN;
			lists = new List<?>[cellcount];
			break;
		case MAP:
			cellcount = INTPGLEN;
			maps = new HashMap<?, ?>[cellcount];
			break;
		case KWD:
			return;
		}
		gcbits = new boolean[cellcount];
		for (int i=0; i < cellcount; i++) {
			gcbits[i] = false;
		}
	}
	
	public void out(String msg) {
		if (debug) {
		//if (true) {
			System.out.println(msg);
		}
	}
	
	public PageTyp getPageTyp() {
		return pgtyp;
	}
	
	public int getValCount() {
		return valcount;
	}
	
	public void incValCount() {
		valcount++;
	}
	
	public void setValCount(int val) {
		valcount = val;
	}
	
	public int getIdxVal(int idx) {
		return (idx & 0xFFF);
	}
	
	public boolean isMarked(int idx) {
		return gcbits[idx];
	}
	
	public void setMarked(int idx, boolean flag) {
		gcbits[idx] = flag;
	}
	
	public int getFirstFree() {
		return freeidx;
	}
	
	public void setFirstFree(int idx) {
		freeidx = idx;
	}
	
	public byte getByte(int idx) {
		return bytes[idx];
	}
	
	public void setByte(int idx, int val) {
		bytes[idx] = (byte) val;
	}
	
	public int getWord(int idx) {
		return words[idx];
	}
	
	public void setWord(int idx, int val) {
		words[idx] = (short) val;
	}
	
	public int getIntVal(int idx) {
		return intvals[idx];
	}
	
	public void setIntVal(int idx, int val) {
		intvals[idx] = val;
	}
	
	public long getLong(int idx) {
		return longs[idx];
	}
	
	public void setLong(int idx, long val) {
		longs[idx] = val;
	}
	
	public double getFloat(int idx) {
		return doubles[idx];
	}
	
	public void setFloat(int idx, double val) {
		doubles[idx] = val;
	}
	
	public String getString(int idx) {
		return strings[idx];
	}
	
	public void setString(int idx, String str) {
		strings[idx] = str;
	}
	
	public List<?> getList(int idx) {
		List<?> list;
		list = lists[idx];
		return list;
	}
	
	public void setList(int idx, List<?> list) {
		lists[idx] = list;
	}
	
	public HashMap<?, ?> getMap(int idx) {
		HashMap<?, ?> map;
		map = maps[idx];
		return map;
	}
	
	public void setMap(int idx, HashMap<?, ?> map) {
		maps[idx] = map;
	}
	
	public int getDataNode(int idx) {  // downp
		int wrdidx = idx * NODESIZ;
		return getIntNode(wrdidx + 1);
	}
	
	public void setDataNode(int idx, int val) {
		int wrdidx = idx * NODESIZ;
		setIntNode(wrdidx + 1, val);
	}
	
	public int getPtrNode(int idx) {  // rightp
		int wrdidx = idx * NODESIZ;
		return getIntNode(wrdidx + 3);
	}
	
	public void setPtrNode(int idx, int val) {
		int wrdidx = idx * NODESIZ;
		setIntNode(wrdidx + 3, val);
	}
	
	public int getIntNode(int idx) {
		return (words[idx] << 16) | (words[idx + 1] & 0xFFFF);
	}
	
	public void setIntNode(int idx, int val) {
		words[idx] = (short)(val >>> 16);
		words[idx + 1] = (short)(val & 0xFFFF);
	}
	
	public Node getNode(int idx) {
		int header, downp, rightp;
		int wrdidx = idx * NODESIZ;
		Node node;

		header = words[wrdidx];
		downp = getIntNode(wrdidx + 1);
		rightp = getIntNode(wrdidx + 3);
		node = new Node(header, downp, rightp);
		return node;
	}
	
	public void setNode(int idx, Node node) {
		int wrdidx = idx * NODESIZ;
		setWord(wrdidx, node.getHeader());
		setIntNode(wrdidx + 1, node.getDownp());
		setIntNode(wrdidx + 3, node.getRightp());
	}
	
	public int allocNode(Node node) {
		int rtnval;
		int nextidx;

		if ((freeidx < 0) && (valcount >= NODECOUNT)) {
			return -1;
		}
		if (freeidx < 0) {
			setNode(valcount, node);
			out("allocNode: rtns valcount = " + valcount);
			return valcount++;
		}
		nextidx = getPtrNode(freeidx);
		setNode(freeidx, node);
		rtnval = freeidx;
		freeidx = nextidx;
		out("allocNode: rtns freeidx was = " + rtnval);
		return rtnval;
	}
	
	public int allocByte(int val) {
		int rtnval;
		int nextidx;

		if ((freeidx < 0) && (valcount >= BYTPGLEN)) {
			return -1;
		}
		if (freeidx < 0) {
			setByte(valcount, val);
			return valcount++;
		}
		nextidx = getByte(freeidx);
		setByte(freeidx, val);
		rtnval = freeidx;
		freeidx = nextidx;
		valcount = 0;
		return rtnval;
	}
	
	public int allocInt(int val) {
		int rtnval;
		int nextidx;

		if ((freeidx < 0) && (valcount >= INTPGLEN)) {
			return -1;
		}
		if (freeidx < 0) {
			setIntVal(valcount, val);
			return valcount++;
		}
		nextidx = getIntVal(freeidx);
		nextidx = freeValToIdx(nextidx);
		setIntVal(freeidx, val);
		rtnval = freeidx;
		freeidx = nextidx;
		valcount = 0;
		return rtnval;
	}
	
	public int freeNum(int idx) {
		int freeVal;
		
		if (idx < 0 || idx >= cellcount) {
			return RESERR;  
		}
		if (valcount <= 0) { 
			return RESERR;
		}
		if (idx >= valcount) {
			return RESERR;  
		}
		if (freeidx < 0) {
			if (idx < (valcount - 1)) {
				freeidx = idx;
				freeVal = idxToFreeVal(-1);
				out("freeNum: pgtyp = " + pgtyp);
				setNumVal(idx, freeVal);
			}
			else if (valcount == 1) {
				valcount = 0;
				return RESFREE;
			}
			else {
				valcount--;
			}
		}
		else {
			freeVal = idxToFreeVal(freeidx);
			setNumVal(idx, freeVal);
			freeidx = idx;
			// snip out and iter if top of valcount:
			if (idx >= (valcount - 1)) {
				return shrinkFreeList(idx);
			}
		}
		return RESOK;
	}
	
	private int shrinkFreeList(int idx) {
		int i, j, k;
		
		if (idx < 0) {
			return RESERR;
		}
		j = -1;
		while (idx >= (valcount - 1)) {
			if (!inFreeRange(idx)) {
				return RESOK;
			}
			i = freeidx;
			while (i != idx && i >= 0) {
				j = i;
				i = getNumVal(i);
			}
			if (i == idx) {
				k = getNumVal(i);
				if (j >= 0) {
					setNumVal(j, idxToFreeVal(k));
				}
				else {
					freeidx = idxToFreeVal(k);
				}
				setNumVal(k, idxToFreeVal(-1));
			}
			else if (i < 0) {
				return RESOK;
			}
			else {
				return RESERR;
			}
			valcount--;
			if (valcount <= 0) {
				return RESFREE;
			}
			if (freeidx < 0) {
				return RESOK;
			}
			idx--;
		}
		return RESERR;
	}
	
	private int idxToFreeVal(int idx) {
		int rtnval = getLoFreeVal();
		if (idx == -1) {
			return rtnval;
		}
		rtnval += idx + 1;
		return rtnval;
	}
	
	private int freeValToIdx(int val) {
		int rtnval = getLoFreeVal();
		if (val == rtnval) {
			return -1;
		}
		rtnval = val - rtnval;
		rtnval--;
		return rtnval;
	}

	private int getLoFreeVal() {
		int rtnval = 0x80000001;
		return rtnval;
	}
	
	private int getHiFreeVal() {
		int rtnval;
		rtnval = getLoFreeVal() + cellcount;
		return rtnval;
	}
	
	private boolean inFreeRange(int val) {
		boolean rtnval;
		rtnval = (val >= getLoFreeVal() && val <= getHiFreeVal());
		return rtnval;
	}
	
	private void setNumVal(int idx, int val) {
		switch (pgtyp) {
		case INTVAL: 
			setIntVal(idx, val);
			break;
		case LONG: 
			setLong(idx, val);
			break;
		case FLOAT: 
			setFloat(idx, val);
			break;
		case STRING: 
			setString(idx, "" + val);
			break;
		case LIST: 
			setRawListVal(idx, val);
			break;
		case MAP: 
			setRawMapVal(idx, val);
			break;
		case NODE: 
			setPtrNode(idx, val);
			break;
		case BYTE: 
			setRawByteVal(idx, val);
			break;
		default:
			break;
		}
	}
	
	private int getNumVal(int idx) {
		idx = getRawVal(idx);
		if (idx == -1) {
			return -1;
		}
		return freeValToIdx(idx);
	}
	
	private int getRawVal(int idx) {
		switch (pgtyp) {
		case INTVAL: return getIntVal(idx);
		case LONG: return (int) getLong(idx);
		case FLOAT: return (int) getFloat(idx);
		case STRING:
			return getStrAsInt(idx);
		case LIST:
			return getRawListVal(idx);
		case MAP:
			return getRawMapVal(idx);
		case NODE:
			return getPtrNode(idx);
		case BYTE:
			return getRawByteVal(idx);
		default:
			break;
		}
		return -1;
	}
	
	private int getStrAsInt(int idx) {
		String buf;
		buf = getString(idx);
		try {
			idx = Integer.parseInt(buf);
		}
		catch (NumberFormatException exc) {
			return -1;
		}
		return idx;
	}
	
	@SuppressWarnings("unchecked")
	private int getRawListVal(int idx) {
		List<?> list;
		ArrayList<AddrNode> arrlist;
		AddrNode node;
		int rtnval;
		
		list = getList(idx);
		arrlist = (ArrayList<AddrNode>) list;
		node = arrlist.get(0);
		rtnval = node.getAddr();
		return rtnval;
	}
	
	@SuppressWarnings("unchecked")
	private void setRawListVal(int idx, int val) {
		List<?> list;
		ArrayList<AddrNode> arrlist;
		AddrNode node;
		
		node = new AddrNode(0, val);
		list = getList(idx);
		list.clear();
		arrlist = (ArrayList<AddrNode>) list;
		arrlist.add(node);
		setList(idx, arrlist);
	}

	@SuppressWarnings("unchecked")
	private int getRawMapVal(int idx) {
		HashMap<?, ?> map;
		HashMap<String, AddrNode> strmap;
		AddrNode node;
		int rtnval;
		
		map = getMap(idx);
		strmap = (HashMap<String, AddrNode>) map;
		node = strmap.get("");
		rtnval = node.getAddr();
		return rtnval;
	}
	
	@SuppressWarnings("unchecked")
	private void setRawMapVal(int idx, int val) {
		HashMap<?, ?> map;
		HashMap<String, AddrNode> strmap;
		AddrNode node;
		
		node = new AddrNode(0, val);
		map = getMap(idx);
		map.clear();
		strmap = (HashMap<String, AddrNode>) map;
		strmap.put("", node);
		setMap(idx, strmap);
	}
	
	private int getRawByteVal(int idx) {
		int rtnval;
		int hdridx;
		int hdrshift;
		int hdrval;

		rtnval = getByte(idx);
		hdridx = (idx / 16) + BYTPGLEN;
		hdrshift = (idx % 4) << 1;
		hdrval = getByte(hdridx + BYTPGLEN);
		hdrval = hdrval & (0x3 << hdrshift);
		hdrval >>>= hdrshift;
		rtnval |= hdrval << 8;
		return rtnval;
	}
	
	private void setRawByteVal(int idx, int val) {
		byte bytval;
		int hdrval;
		int hdridx;
		int hdrshift;
		
		bytval = (byte)(val & 0xFF);
		setByte(idx, bytval);
		hdrval = (val & 0x300) >> 8;  // top 2 bits: hdr bits
		hdridx = (idx / 16) + BYTPGLEN;
		hdrshift = (idx % 4) << 1;
		bytval = getByte(hdridx);
		bytval = (byte)(bytval & ~(0x3 << hdrshift));
		bytval |= hdrval << hdrshift;
		setByte(hdridx, bytval);
	}
	
	public int allocLong(long val) {
		int rtnval;
		int nextidx;

		if ((freeidx < 0) && (valcount >= DBLPGLEN)) {
			return -1;
		}
		if (freeidx < 0) {
			setLong(valcount, val);
			return valcount++;
		}
		nextidx = (int) getLong(freeidx);
		nextidx = freeValToIdx(nextidx);
		setLong(freeidx, val);
		rtnval = freeidx;
		freeidx = nextidx;
		return rtnval;
	}
	
	public int allocFloat(double val) {
		int rtnval;
		int nextidx;

		if ((freeidx < 0) && (valcount >= DBLPGLEN)) {
			return -1;
		}
		if (freeidx < 0) {
			setFloat(valcount, val);
			return valcount++;
		}
		nextidx = (int) getFloat(freeidx);
		nextidx = freeValToIdx(nextidx);
		setFloat(freeidx, val);
		rtnval = freeidx;
		freeidx = nextidx;
		return rtnval;
	}
	
	public int allocString(String val) {
		int rtnval;
		int nextidx;
		String buf;

		if ((freeidx < 0) && (valcount >= INTPGLEN)) {
			return -1;
		}
		if (freeidx < 0) {
			setString(valcount, val);
			return valcount++;
		}
		buf = getString(freeidx);
		try {
			nextidx = Integer.parseInt(buf);
		}
		catch (NumberFormatException exc) {
			return -1;
		}
		nextidx = freeValToIdx(nextidx);
		setString(freeidx, val);
		rtnval = freeidx;
		freeidx = nextidx;
		return rtnval;
	}
	
	@SuppressWarnings("unchecked")
	public int allocList(ArrayList<AddrNode> arrlist) {
		// not updated with freeValToIdx()
		int rtnval;
		int nextidx;
		ArrayList<AddrNode> list;
		AddrNode node;

		if ((freeidx < 0) && (valcount >= INTPGLEN)) {
			return -1;
		}
		if (freeidx < 0) {
			setList(valcount, arrlist);
			return valcount++;
		}
		list = (ArrayList<AddrNode>) getList(freeidx);
		node = list.get(0);
		nextidx = node.getAddr();
		setList(freeidx, arrlist);
		rtnval = freeidx;
		freeidx = nextidx;
		return rtnval;
	}
	
	@SuppressWarnings("unchecked")
	public boolean isFreeList(int idx) {
		List<?> list;
		ArrayList<AddrNode> arrlist;
		AddrNode node;
		int i = freeidx;

		if (valcount <= 0 || idx >= valcount) {
			return false;
		}
		while (i >= 0) {
			if (i == idx) {
				return true;
			}
			list = getList(i);
			arrlist = (ArrayList<AddrNode>) list;
			node = arrlist.get(0);
			i = node.getAddr();
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public int allocMap(HashMap<String, AddrNode> strmap) {
		// not updated with freeValToIdx()
		int rtnval;
		int nextidx;
		HashMap<String, AddrNode> map;
		AddrNode node;

		if ((freeidx < 0) && (valcount >= INTPGLEN)) {
			return -1;
		}
		if (freeidx < 0) {
			setMap(valcount, strmap);
			return valcount++;
		}
		map = (HashMap<String, AddrNode>) getMap(freeidx);
		node = map.get("");
		nextidx = node.getAddr();
		setMap(freeidx, strmap);
		rtnval = freeidx;
		freeidx = nextidx;
		return rtnval;
	}
	
	public boolean isAvailPage() {
		boolean isAvail = (freeidx >= 0) || (valcount < INTPGLEN);
		return isAvail;
	}
	
	public int getNext() {
		return nextIdx;
	}
	
	public int getPrev() {
		return prevIdx;
	}
	
	public void setNext(int idx) {
		nextIdx = idx;
	}
	
	public void setPrev(int idx) {
		prevIdx = idx;
	}
	
	public boolean isFullPage() {
		boolean isFull;
		isFull = 
			(valcount >= getMaxPageLen(pgtyp)) && 
			(freeidx < 0);
		return isFull;
	}
	
	private int getMaxPageLen(PageTyp pgtyp) {
		switch (pgtyp) {
		case FLOAT: return DBLPGLEN;
		case LONG: return DBLPGLEN;
		case INTVAL: return INTPGLEN;
		case STRING: return INTPGLEN;
		case LIST: return INTPGLEN;
		case MAP: return INTPGLEN;
		case BYTE: return BYTPGLEN;
		case NODE: return WRDPGLEN;
		default: return 0;
		}
	}
/*	
	public boolean freeLong(int idx) {
		int freeVal;
		
		if (idx < 0 || idx >= DBLPGLEN) {
			return false;  
		}
		if (valcount <= 0) { 
			return false;
		}
		if (freeidx < 0) {
			if (idx == (valcount - 1)) {
				valcount--;
			}
			else if (idx >= valcount) {
				return false;  
			}
			else {
				freeidx = idx;
			}
			freeVal = idxToFreeVal(-1);
			setLong(idx, freeVal);
		}
		else if (idx >= valcount) {
			return false;  
		}
		else {
			freeVal = idxToFreeVal(freeidx);
			setLong(idx, freeVal);
			freeidx = idx;
		}
		return true;
	}

	public boolean freeFloat(int idx) {
		int freeVal;
		
		if (idx < 0 || idx >= DBLPGLEN) {
			return false;  
		}
		if (valcount <= 0) { 
			return false;
		}
		if (freeidx < 0) {
			if (idx == (valcount - 1)) {
				valcount--;
			}
			else if (idx >= valcount) {
				return false;  
			}
			else {
				freeidx = idx;
			}
			freeVal = idxToFreeVal(-1);
			setFloat(idx, freeVal);
		}
		else if (idx >= valcount) {
			return false;  
		}
		else {
			freeVal = idxToFreeVal(freeidx);
			setFloat(idx, freeVal);
			freeidx = idx;
		}
		return true;
	}

	public boolean freeString(int idx) {
		int freeVal;
		
		if (idx < 0 || idx >= INTPGLEN) {
			return false;  
		}
		if (valcount <= 0) { 
			return false;
		}
		if (freeidx < 0) {
			if (idx == (valcount - 1)) {
				valcount--;
			}
			else if (idx >= valcount) {
				return false;  
			}
			else {
				freeidx = idx;
			}
			freeVal = idxToFreeVal(-1);
			setString(idx, "" + freeVal);
		}
		else if (idx >= valcount) {
			return false;  
		}
		else {
			freeVal = idxToFreeVal(freeidx);
			setString(idx, "" + freeVal);
			freeidx = idx;
		}
		return true;
	}

	public boolean freeList(int idx) {
		// not updated with idxToFreeVal()
		List<?> list;
		ArrayList<AddrNode> arrlist;
		AddrNode node;
		int i = freeidx;

		if (valcount <= 0 || idx >= valcount) {
			return false;
		}
		while (i >= 0) {
			if (i == idx) {
				return false;
			}
			list = getList(i);
			arrlist = (ArrayList<AddrNode>) list;
			node = arrlist.get(0);
			i = node.getAddr();
		}
		node = new AddrNode(0, freeidx);
		list = getList(idx);
		list.clear();
		arrlist = (ArrayList<AddrNode>) list;
		arrlist.add(node);
		setList(idx, arrlist);
		freeidx = idx;
		return true;
	}
	
	public boolean freeMap(int idx) {
		// not updated with idxToFreeVal()
		HashMap<?, ?> map;
		HashMap<String, AddrNode> strmap;
		AddrNode node;
		int i = freeidx;

		if (valcount <= 0 || idx >= valcount) {
			return false;
		}
		while (i >= 0) {
			if (i == idx) {
				return false;
			}
			map = getMap(i);
			strmap = (HashMap<String, AddrNode>) map;
			node = strmap.get("");
			i = node.getAddr();
		}
		node = new AddrNode(0, freeidx);
		map = getMap(idx);
		map.clear();
		strmap = (HashMap<String, AddrNode>) map;
		strmap.put("", node);
		setMap(idx, strmap);
		freeidx = idx;
		return true;
	}
	
	public boolean freeNode(int idx) {
		int i = freeidx;
		if (valcount <= 0 || idx >= valcount) {
			return false;
		}
		while (i >= 0) {
			if (i == idx) {
				return false;
			}
			i = getPtrNode(i);
		}
		setPtrNode(idx, freeidx);
		freeidx = idx;
		return true;
	}

	public boolean freeByte(int idx) {
		if (idx < 0 || idx >= BYTPGLEN) {
			return false;  
		}
		if (valcount <= 0) { 
			return false;
		}
		if (freeidx < 0) {
			if (idx == (valcount - 1)) {
				valcount--;
			}
			else if (idx >= valcount) {
				return false;  
			}
			else {
				freeidx = idx;
			}
			setByte(idx, -1);
		}
		else if (idx >= valcount) {
			return false;  
		}
		else {
			setByte(idx, freeidx);
			freeidx = idx;
		}
		return true;
	}
*/	
}
