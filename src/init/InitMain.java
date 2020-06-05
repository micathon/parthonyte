package init;

import iconst.IConst;
import iconst.PageTyp;
import iconst.TokenTyp;
import page.Store;
import page.Page;
import page.AddrNode;
import page.Node;
import scansrc.ScanSrc;
import synchk.SynChk;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

// 0. Enables user to alloc/free different types of data values:
//    - int, long, double, string, node, list, map
//    Allows testing of alloc/free logic

// 1. Calls Lexical Scanner (if given file name w/o ext)
// 2. Builds program tree:
// 3. Syntax checker
// 4. *Code execution

// * not yet implemented

public class InitMain implements IConst {
	
	public Store store;
	private int pageIdx = 0;
	private static final boolean isSilent = false;

	public InitMain() {
		store = new Store();
	}
	
	public void runInit(String fileName, boolean isUnitTest, boolean isMain) {
		BufferedReader br = new BufferedReader(new
			InputStreamReader(System.in));
		String inbuf;
		
		if (isMain) {
			doMasterFile(fileName);
		}
		else if (fileName.length() > 0) {
			doSrcFile(fileName, isUnitTest, isMain);
		}
		else {
			System.out.println("Type h for help");
			for (;;) {
				inbuf = "";
				System.out.print(DEFPROMPT);
				try {
					inbuf = br.readLine();
				}
				catch (IOException exc) {
					System.out.println("Error: IOException!");
				}
				inbuf = inbuf.trim();
				if (inbuf.length() == 0) {
					continue;
				}
				if (!doCmd(inbuf)) {
					break;
				}
			}
			System.out.println("\nbye");
		}
	}
	
	private void doMasterFile(String mainFileName) {
		String fileName;
		BufferedReader fbr;
		boolean isFail;
		boolean isGlbFail = false;
		
		try {
			fbr = new BufferedReader(new FileReader(mainFileName));
			while ((fileName = fbr.readLine()) != null) {
				fileName = fileName.trim();
				if (fileName.equals("")) {
					continue;
				}
				omsg("Unit Test: " + fileName);
				fileName = "../dat/test/" + fileName + ".test";
				isFail = doSrcFile(fileName, true, true);
				isGlbFail = isGlbFail || isFail;
			}
			showUnitTestVal(isGlbFail);
		} catch (IOException exc) {
			System.out.println("I/O Error: " + exc);
		}
	}
	
	private boolean doSrcFile(String fileName, boolean isUnitTest, boolean isMain) {
		String inbuf;
		BufferedReader fbr;
		ScanSrc scanSrc;
		SynChk synchk;
		boolean fatalErr = false;
		boolean rtnval = true;

		scanSrc = new ScanSrc(store);
		synchk = new SynChk(scanSrc, store);
		scanSrc.setSynChk(synchk);
		synchk.isUnitTest = isUnitTest;
		if (isUnitTest) {
			synchk.initUnitTestFlags();
		}
		try {
			fbr = new BufferedReader(new FileReader(fileName));
			while ((inbuf = fbr.readLine()) != null) {
				if (!scanSrc.scanCodeBuf(inbuf)) {
					fatalErr = true;
					break;
				}
			}
			if (scanSrc.inCmtBlk) {
				scanSrc.putErr(TokenTyp.ERRINCMTEOF);
			}
			if (isUnitTest) {
				rtnval = synchk.showUnitTestVal();
			}
			else {
				scanSrc.scanSummary(fatalErr);
			}
		} catch (IOException exc) {
			System.out.println("I/O Error: " + exc);
		}
		return rtnval;
	}
	
	private void omsg(String msg) {
		if (!isSilent) {
			System.out.println(msg);
		}
	}
	
	private void showUnitTestVal(boolean isFail) {
		if (isFail) {
			omsg("Main unit test failed!");
		}
		else {
			omsg("Main unit test passed OK");
		}
		omsg("");
	}
	
	private boolean doCmd(String inbuf) {
		String arg = getArg(inbuf);
		char ch;
		
		inbuf = getRest(inbuf, arg);
		if (arg.length() != 1) {
			return true;
		}
		ch = arg.charAt(0);
		switch (ch) {
		case 'q': 
			return quit(inbuf);
		case 'h':
			help(inbuf);
			break;
		case 'a':
			add(inbuf);  // use alloc func for a piece of data
			break;
		case 'd':
			delete(inbuf);  // use free func
			break;
		case 's':
			show(inbuf);
			break;
		case 'p':
			page(inbuf);
			break;
		}
		return true;
	}
	
	private String getArg(String buf) {
		int n = buf.indexOf(' ');
		if (n < 0) {
			return buf;
		}
		return buf.substring(0, n);
	}
	
	private String getRest(String buf, String arg) {
		int n = arg.length();
		buf = buf.substring(n);
		return buf.trim();
	}
	
	private String getKeyVal(String buf) {
		int n = buf.indexOf(',');
		if (n < 0) {
			return buf;
		}
		return buf.substring(0, n);
	}
	
	private String getRestKeyVal(String buf) {
		int n = buf.indexOf(',');
		if (n < 0) {
			return "";
		}
		buf = buf.substring(n + 1);
		return buf.trim();
	}
	
	private int getInt(String buf) {
		int n;
		try {
			n = Integer.parseInt(buf);
		}
		catch (NumberFormatException exc) {
			n = -1;
		}
		return n;
	}
	
	private boolean quit(String buf) {
		return (buf.length() > 0);
	}
	
	private void help(String buf) {
		if (buf.length() > 0) {
			return;
		}
		System.out.println("h - display this list of commands");
		System.out.println("a x - add x");
		System.out.println("d i - delete i-th item");
		System.out.println("s i - show i-th item");
		System.out.println("s i j - show i-th list at j");
		System.out.println("s i a - show i-th map at a");
		System.out.println("p - print current page");
		System.out.println("p i - print i-th page");
		System.out.println("q - quit");
		System.out.println("");
		System.out.println("x values:");
		System.out.println("99 - integer");
		System.out.println("3.14 - float");
		System.out.println("\"abc\" - string");
		System.out.println("[a b c 1 2 3] - list w/ strings/ints");
		System.out.println("{a x, b y, c 3, d 4} - map w/ strings/ints");
		System.out.println("(3 5 7) - node");
		System.out.println("");
	}
	
	private void add(String buf) {
		char ch;
		if (buf.length() == 0) {
			return;
		}
		ch = buf.charAt(0);
		switch (ch) {
		case '"':
			addString(buf);
			break;
		case '(':
			addNode(buf);
			break;
		case '[':
			addList(buf);
			break;
		case '{':
			addMap(buf);
			break;
		default:
			addNum(buf);
		}
	}
	
	private void delete(String buf) {
		Page page = store.getPageZero(pageIdx);
		PageTyp pgtyp = page.getPageTyp();
		boolean success = false;
		int n = getInt(buf);

		if (buf.length() == 0) {
			return;
		}
		if (n < 0) {
			System.out.println("Error: invalid delete arg = " + buf);
			return;
		}
		System.out.println("You are trying to delete i-th element,");
		System.out.println("where i = " + n);
		switch (pgtyp) {
		case INTVAL:
			success = page.freeInt(n);
			break;
		case LONG:
			success = page.freeLong(n);
			break;
		case DOUBLE:
			success = page.freeDouble(n);
			break;
		case STRING:
			success = page.freeString(n);
			break;
		case NODE:
			success = page.freeNode(n);
			break;
		case LIST:
			success = page.freeList(n);
			break;
		case MAP:
			success = page.freeMap(n);
			break;
		case BYTE:
			success = false;
			break;
		}
		if (success) {
			System.out.println("Element was deleted OK.");
		}
		else {
			System.out.println("Element could not be deleted.");
		}
	}

	private void show(String buf) {
		int i;
		int j = -1;
		String arg;
		String save = buf;
		boolean valid = true;
		
		if (buf.length() == 0) {
			return;
		}
		arg = getArg(buf);
		i = getInt(arg);
		if (i < 0) {
			valid = false;
		}
		else {
			buf = getRest(buf, arg);
			if (buf.length() > 0) {
				j = getInt(buf);
			}
		}
		if (!valid) {
			System.out.println("Error: invalid show arg = " + save);
			return;
		}
		if (buf.length() == 0) {
			System.out.println("You are trying to show i-th element,");
			System.out.println("where i = " + i);
			showElem(i);
			return;
		}
		if (j < 0) {
			System.out.println("You are trying to show key/val of i-th map,");
			System.out.println("where (i, key) = " + i + ", " + buf);
			showMapElem(i, buf);
			return;
		}
		System.out.println("You are trying to show j-th element of i-th list,");
		System.out.println("where (i, j) = " + i + ", " + j);
		showListElem(i, j);
	}
	
	private void page(String buf) {
		int n;
		if (buf.length() == 0) {
			System.out.println("You are trying to print current page.");
			outPage(pageIdx);
			return;
		}
		n = getInt(buf);
		if (n < 0 || n >= INTPGLEN) {
			System.out.println("Error: invalid page arg = " + buf);
			return;
		}
		System.out.println("You are trying to print i-th page,");
		System.out.println("where i = " + n);
		outPage(n);
	}
	
	private void outPage(int pgidx) {
		Page page;
		int count;
		
		page = store.getPageZero(pgidx);
		if (page == null) {
			System.out.println("Error: page idx out of bounds.");
			return;
		}
		pageIdx = pgidx;
		count = page.getValCount();
		if (count <= 0) {
			System.out.println("Page is empty.");
			return;
		}
		for (int i=0; i < count; i++) {
			System.out.print("" + i + ". ");
			showElem(i);
		}
	}
	
	private void showElem(int idx) {
		Page page = store.getPageZero(pageIdx);
		PageTyp pgtyp = page.getPageTyp();
		int count;
		
		count = page.getValCount();
		if (idx >= count) {
			System.out.println("Error: index out of bounds.");
			return;
		}
		if (pgtyp == PageTyp.MAP) {
			showMap(idx);
			return;
		}
		showListElem(idx, -1);
	}
	
	@SuppressWarnings("unchecked")
	private void showListElem(int idx, int altidx) {
		Page page = store.getPageZero(pageIdx);
		PageTyp pgtyp = page.getPageTyp();
		int n, j, count;
		long lval;
		double x;
		String s;
		Node node;
		ArrayList<AddrNode> list;
		AddrNode addrnode;
		int addr;
		Page pgdat;

		if (altidx >= 0 && pgtyp != PageTyp.LIST) { 
			System.out.println("Error: curr page is non-list.");
			return;
		}
		count = page.getValCount();
		if (idx >= count) {
			System.out.println("Error: index out of bounds.");
			return;
		}
		switch (pgtyp) {
		case INTVAL: 
			n = page.getIntVal(idx);
			System.out.println("" + n);
			break;
		case LONG:
			lval = page.getLong(idx);
			System.out.println("" + lval);
			break;
		case DOUBLE:
			x = page.getDouble(idx);
			System.out.println("" + x);
			break;
		case STRING:
			s = page.getString(idx);
			System.out.println("\"" + s + "\"");
			break;
		case NODE:
			node = page.getNode(idx);
			System.out.println("(" + 
				node.getHeader() + " " +
				node.getDownp() + " " +
				node.getRightp() + ")");
			break;
		case MAP:
		case BYTE:
			break;
		case LIST:
			list = (ArrayList<AddrNode>) page.getList(idx);
			System.out.print("[");
			count = list.size();
			for (int i=0; i < count; i++) {
				if (i > 0 && altidx < 0) {
					System.out.print(" ");
				}
				if (altidx >= 0 && altidx != i) {
					continue;
				}
				addrnode = list.get(i);
				addr = addrnode.getAddr();
				if (page.isFreeList(idx)) {
					System.out.print("" + addr);
					break;
				}
				pgdat = store.getPage(addr);
				j = store.getElemIdx(addr);
				if (addrnode.isInt()) {
					n = pgdat.getIntVal(j);
					System.out.print("" + n);
				}
				else {
					s = pgdat.getString(j);
					System.out.print(s);
				}
			}
			System.out.println("]");
			break;
		}
	}
	
	@SuppressWarnings("unchecked")
	private void showMapElem(int idx, String key) {
		Page page = store.getPageZero(pageIdx);
		PageTyp pgtyp = page.getPageTyp();
		HashMap<String, AddrNode> map;
		AddrNode addrnode;
		Page pgdat;
		int count;
		int addr;
		int j, n;
		String s;
		
		if (pgtyp != PageTyp.MAP) {
			System.out.println("Error: curr page is non-map.");
			return;
		}
		count = page.getValCount();
		if (idx >= count) {
			System.out.println("Error: index out of bounds.");
			return;
		}
		map = (HashMap<String, AddrNode>) page.getMap(idx);
		addrnode = map.get(key);
		if (addrnode == null) {
			System.out.println("Error: invalid key");
			return;
		}
		addr = addrnode.getAddr();
		if (key.length() == 0) {
			System.out.println("" + addr);
			return;
		}
		pgdat = store.getPage(addr);
		j = store.getElemIdx(addr);
		if (addrnode.isInt()) {
			n = pgdat.getIntVal(j);
			System.out.println("" + n);
		}
		else {
			s = pgdat.getString(j);
			System.out.println(s);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void showMap(int idx) {
		Page page = store.getPageZero(pageIdx);
		HashMap<String, AddrNode> map;
		AddrNode addrnode;
		Page pgdat;
		int addr;
		int j, n;
		int i = 0;
		String key, s;
		boolean isNullKey;

		map = (HashMap<String, AddrNode>) page.getMap(idx);
		Set<Entry<String, AddrNode>> entrySet = map.entrySet();
	    Iterator<Entry<String, AddrNode>> it = entrySet.iterator();
	    System.out.print("{");
	    while(it.hasNext()){
	    	if (i > 0) {
	    		System.out.print(", ");
	    	}
	    	Map.Entry<String, AddrNode> me = 
	    		(Map.Entry<String, AddrNode>) it.next();
	    	key = (String) me.getKey();
	    	if (key.length() == 0) {
	    	    System.out.print("\"\"");
	    	    isNullKey = true;
	    	}
	    	else {
	    		System.out.print(key);
	    		isNullKey = false;
	    	}
	    	System.out.print(": ");
	    	addrnode = (AddrNode) me.getValue();
			addr = addrnode.getAddr();
			if (isNullKey) {
				System.out.print("" + addr);
				i++;
				continue;
			}
			pgdat = store.getPage(addr);
			j = store.getElemIdx(addr);
			if (addrnode.isInt()) {
				n = pgdat.getIntVal(j);
				System.out.print("" + n);
			}
			else {
				s = pgdat.getString(j);
				System.out.print(s);
			}
			i++;
	    }
	    System.out.println("}");
	}

	private void addNum(String buf) {
		int n = 0;
		double x = 0.0;
		boolean valid = true;
		boolean hasDecPt = (buf.indexOf('.') >= 0);
		int addr = 0;
		
		if (hasDecPt) {
			try {
				x = Double.parseDouble(buf);
			}
			catch (NumberFormatException exc) {
				valid = false;
			}
		}
		else {
			try {
				n = Integer.parseInt(buf);
			}
			catch (NumberFormatException exc) {
				valid = false;
			}
		}
		if (!valid) {
			System.out.println("Error: invalid numeric arg = " + buf);
			return;
		}
		if (hasDecPt) {
			System.out.println("You are trying to add float x,");
			System.out.println("where x = " + x);
			addr = store.allocDouble(x);
		}
		else if (n < 0) {
			// user enters +ve: int; enters -ve: long
			System.out.println("You are trying to add long n,");
			System.out.println("where n = " + -n);
			addr = store.allocLong(n);
		}
		else {
			System.out.println("You are trying to add int n,");
			System.out.println("where n = " + n);
			addr = store.allocInt(n);
		}
		outAddr(addr);
	}
	
	private void addString(String buf) {
		int len;
		int addr;
		
		buf = buf.substring(1);
		len = buf.length();
		if (len == 0 || buf.charAt(len - 1) != '"') {
			System.out.println("Error: string not terminated with quote.");
			return;
		}
		buf = buf.substring(0, len - 1);
		if (buf.indexOf('"') >= 0) {
			System.out.println("Error: string has quote in middle.");
			return;
		}
		System.out.println("You are trying to add string s,");
		System.out.println("where s = [" + buf + "]");
		addr = store.allocString(buf);
		outAddr(addr);
	}
	
	private void addNode(String buf) {
		String arg;
		int n;
		int hdr = 0;
		int downp = 0;
		int rightp = 0;
		Node node;
		int addr;
		int len = buf.length();
		
		if (buf.charAt(len - 1) != ')') {
			System.out.println("Error: node not terminated with close paren.");
			return;
		}
		buf = buf.substring(1, len - 1);
		buf = buf.trim();
		for (int i=0; i < 3; i++) {
			arg = getArg(buf);
			if (arg.length() == 0) {
				System.out.println("Error: node has too few ints.");
				return;
			}
			n = getInt(arg);
			if (n < 0) {
				System.out.println("Error: node has invalid/negative int.");
				return;
			}
			switch (i) {
			case 0:
				hdr = n;
				break;
			case 1:
				downp = n;
				break;
			case 2:
				rightp = n;
				break;
			}
			buf = getRest(buf, arg);
		}
		if (buf.length() > 0) {
			System.out.println("Error: node has too many ints.");
			return;
		}
		System.out.println("You are trying to add node:");
		System.out.println("hdr = " + hdr);
		System.out.println("downp = " + downp);
		System.out.println("rightp = " + rightp);
		node = new Node(hdr, downp, rightp);
		addr = store.allocNode(node);
		outAddr(addr);
	}
	
	private void addList(String buf) {
		int numCount = 0;
		int strCount = 0;
		int n = 0;
		boolean isNum;
		String arg;
		ArrayList<AddrNode> list = new ArrayList<AddrNode>();
		int addr;
		PageTyp pgtyp;
		AddrNode node;
		int len = buf.length();
		
		if (buf.charAt(len - 1) != ']') {
			System.out.println("Error: list not terminated with close bracket.");
			return;
		}
		buf = buf.substring(1, len - 1);
		buf = buf.trim();
		if (buf.indexOf(']') >= 0) {
			System.out.println("Error: list has ']' in middle.");
			return;
		}
		while (buf.length() > 0) {
			arg = getArg(buf);
			isNum = true;
			try {
				n = Integer.parseInt(arg);
			}
			catch (NumberFormatException exc) {
				isNum = false;
			}
			if (isNum) {
				numCount++;
				pgtyp = PageTyp.INTVAL;
				addr = store.allocInt(n);
			}
			else {
				strCount++;
				pgtyp = PageTyp.STRING;
				addr = store.allocString(arg);
			}
			node = new AddrNode(0, addr);
			node.setHdrPgTyp(pgtyp);
			list.add(node);
			buf = getRest(buf, arg);
		}
		System.out.println("You are trying to add list:");
		System.out.println("No. of ints = " + numCount);
		System.out.println("No. of strings = " + strCount);
		if (numCount > 0) {
			System.out.println("Final int = " + n);
		}
		addr = store.allocList(list);
		outAddr(addr);
	}
	
	private void addMap(String buf) {
		int numCount = 0;
		int strCount = 0;
		int n = 0;
		boolean isNum;
		String arg, keyval, key;
		HashMap<String, AddrNode> map = new HashMap<String, AddrNode>();
		int addr;
		PageTyp pgtyp;
		AddrNode node;
		int len = buf.length();
		
		if (buf.charAt(len - 1) != '}') {
			System.out.println("Error: map not terminated with close brace.");
			return;
		}
		buf = buf.substring(1, len - 1);
		buf = buf.trim();
		if (buf.indexOf('}') >= 0) {
			System.out.println("Error: map has '}' in middle.");
			return;
		}
		while (buf.length() > 0) {
			keyval = getKeyVal(buf);
			key = getArg(keyval);
			arg = getRest(keyval, key);
			isNum = true;
			try {
				n = Integer.parseInt(arg);
			}
			catch (NumberFormatException exc) {
				isNum = false;
			}
			if (isNum) {
				numCount++;
				pgtyp = PageTyp.INTVAL;
				addr = store.allocInt(n);
			}
			else {
				strCount++;
				pgtyp = PageTyp.STRING;
				addr = store.allocString(arg);
			}
			node = new AddrNode(0, addr);
			node.setHdrPgTyp(pgtyp);
			map.put(key, node);
			buf = getRestKeyVal(buf);
		}
		System.out.println("You are trying to add map:");
		System.out.println("No. of ints = " + numCount);
		System.out.println("No. of strings = " + strCount);
		if (numCount > 0) {
			System.out.println("Final int = " + n);
		}
		addr = store.allocMap(map);
		outAddr(addr);
	}
	
	private void outAddr(int addr) {
		int elemIdx;
		if (!store.isLocAddr(addr)) {
			outNonLocal();
			return;
		}
		pageIdx = store.getPageIdx(addr);
		elemIdx = store.getElemIdx(addr);
		System.out.println("(page, elem) = " + pageIdx + ", " + elemIdx);
	}
	
	private void outNonLocal() {
		System.out.println("Error: non-local addr encountered.");
	}

}
