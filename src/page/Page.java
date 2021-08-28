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
	
	public Page(PageTyp pgtyp) {
		this.pgtyp = pgtyp;
		valcount = 0;
		freeidx = -1;
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
			bytes = new byte[cellcount];
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
	
	public PageTyp getPageTyp() {
		return pgtyp;
	}
	
	public int getValCount() {
		return valcount;
	}
	
	public void incValCount() {
		valcount++;
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
		int validx;

		if ((freeidx < 0) && (valcount >= NODECOUNT)) {
			return -1;
		}
		if (freeidx < 0) {
			validx = valcount++; 
			setNode(validx , node);
			return validx;
		}
		nextidx = getPtrNode(freeidx);
		setNode(freeidx, node);
		rtnval = freeidx;
		freeidx = nextidx;
		return rtnval;
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
		setIntVal(freeidx, val);
		rtnval = freeidx;
		freeidx = nextidx;
		valcount = 0;
		return rtnval;
	}
	
	public boolean isFreeInt(int idx) {
		if (idx < 0 || idx >= INTPGLEN) {
			return false;  // error
		}
		if (valcount <= 0) { 
			setIntVal(idx, freeidx);
			freeidx = idx;
		}
		else if (freeidx < 0) {
			if (idx == (valcount - 1)) {
				valcount--;
			}
			else if (idx >= valcount) {
				return false;  // error
			}
			else {
				freeidx = idx;
				valcount = 0;
			}
			setIntVal(idx, -1);
		}
		else if (idx >= valcount) {
			return false;  // error
		}
		else {
			valcount = 0;
			setIntVal(idx, freeidx);
			freeidx = idx;
		}
		return true;
	}
	
	public boolean freeInt(int idx) {
		int i = freeidx;
		if (valcount <= 0 || idx >= valcount) {
			return false;
		}
		while (i >= 0) {
			if (i == idx) {
				return false;  // already in free list
			}
			i = getIntVal(i);
		}
		setIntVal(idx, freeidx);
		freeidx = idx;
		return true;
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
		setLong(freeidx, val);
		rtnval = freeidx;
		freeidx = nextidx;
		return rtnval;
	}
	
	public boolean isFreeLong(int idx) {
		if (idx < 0 || idx >= DBLPGLEN) {
			return false;  // error
		}
		if (valcount <= 0) { 
			setLong(idx, freeidx);
			freeidx = idx;
		}
		else if (freeidx < 0) {
			if (idx == (valcount - 1)) {
				valcount--;
			}
			else if (idx >= valcount) {
				return false;  // error
			}
			else {
				freeidx = idx;
				valcount = 0;
			}
			setLong(idx, -1);
		}
		else if (idx >= valcount) {
			return false;  // error
		}
		else {
			valcount = 0;
			setLong(idx, freeidx);
			freeidx = idx;
		}
		return true;
	}
	
	public boolean freeLong(int idx) {
		int i = freeidx;
		if (valcount <= 0 || idx >= valcount) {
			return false;
		}
		while (i >= 0) {
			if (i == idx) {
				return false;
			}
			i = (int) getLong(i);
		}
		setLong(idx, freeidx);
		freeidx = idx;
		return true;
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
		setFloat(freeidx, val);
		rtnval = freeidx;
		freeidx = nextidx;
		return rtnval;
	}
	
	public boolean isFreeFloat(int idx) {
		if (idx < 0 || idx >= DBLPGLEN) {
			return false;  // error
		}
		if (valcount <= 0) { 
			setFloat(idx, freeidx);
			freeidx = idx;
		}
		else if (freeidx < 0) {
			if (idx == (valcount - 1)) {
				valcount--;
			}
			else if (idx >= valcount) {
				return false;  // error
			}
			else {
				freeidx = idx;
				valcount = 0;
			}
			setFloat(idx, -1);
		}
		else if (idx >= valcount) {
			return false;  // error
		}
		else {
			valcount = 0;
			setFloat(idx, freeidx);
			freeidx = idx;
		}
		return true;
	}
	
	public boolean freeFloat(int idx) {
		int i = freeidx;
		if (valcount <= 0 || idx >= valcount) {
			return false;
		}
		while (i >= 0) {
			if (i == idx) {
				return false;
			}
			i = (int) getFloat(i);
		}
		setFloat(idx, freeidx);
		freeidx = idx;
		return true;
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
		setString(freeidx, val);
		rtnval = freeidx;
		freeidx = nextidx;
		return rtnval;
	}
	
	public boolean isFreeString(int idx) {
		if (idx < 0 || idx >= INTPGLEN) {
			return false;  // error
		}
		if (valcount <= 0) { 
			setString(idx, "" + freeidx);
			freeidx = idx;
		}
		else if (freeidx < 0) {
			if (idx == (valcount - 1)) {
				valcount--;
			}
			else if (idx >= valcount) {
				return false;  // error
			}
			else {
				freeidx = idx;
				valcount = 0;
			}
			setString(idx, "-1");
		}
		else if (idx >= valcount) {
			return false;  // error
		}
		else {
			valcount = 0;
			setString(idx, "" + freeidx);
			freeidx = idx;
		}
		return true;
	}
	
	public boolean freeString(int idx) {
		int i = freeidx;
		if (valcount <= 0 || idx >= valcount) {
			return false;
		}
		while (i >= 0) {
			if (i == idx) {
				return false;
			}
			try {
				i = Integer.parseInt(getString(i));
			}
			catch (NumberFormatException exc) {
				return false;
			}
		}
		setString(idx, "" + freeidx);
		freeidx = idx;
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public int allocList(ArrayList<AddrNode> arrlist) {
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
	public boolean freeList(int idx) {
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
	
	@SuppressWarnings("unchecked")
	public boolean freeMap(int idx) {
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
	
}
