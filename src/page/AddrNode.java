package page;

import iconst.PageTyp;

public class AddrNode {
	
	private short header;
	private int addr;
	
	public AddrNode(int header, int addr) {
		this.header = (short) header;
		this.addr = addr;
	}
	
	public int getHeader() {
		return header;
	}
	
	public void setHeader(int header) {
		this.header = (short) header;
	}

	public int getAddr() {
		return addr;
	}
	
	public void setAddr(int addr) {
		this.addr = addr;
	}
	
	public boolean isInt() {
		PageTyp pgtyp = PageTyp.INTVAL;
		return ((header & 0xF) == pgtyp.ordinal());
	}
	
	public void setHdrPgTyp(PageTyp pgtyp) {
		short mask = (short)(pgtyp.ordinal());
		header = (short)((header & 0xFFF0) | mask);
	}

}
