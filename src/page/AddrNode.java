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
	
	public int getHdrLocVarTyp() {
		short locVarTyp = (short)((header & (short)0x0030) >>> 4);
		return (int)locVarTyp;
	}

	public void setHdrLocVarTyp(int locVarTyp) {
		int mask = 0xFFCF;
		header &= mask;
		locVarTyp &= 0x3;
		header |= (locVarTyp << 4);
	}
	
	public void setHdrNonVar() {
		setHdrLocVarTyp(0);
	}
	
	public void setHdrLocVar() {
		setHdrLocVarTyp(1);
	}
	
	public void setHdrFldVar() {
		setHdrLocVarTyp(2);
	}
	
	public void setHdrGlbVar() {
		setHdrLocVarTyp(3);
	}
	
	public boolean getHdrNonVar() {
		return (getHdrLocVarTyp() == 0);
	}
	
	public boolean getHdrLocVar() {
		return (getHdrLocVarTyp() == 1);
	}
	
	public boolean getHdrFldVar() {
		return (getHdrLocVarTyp() == 2);
	}
	
	public boolean getHdrGlbVar() {
		return (getHdrLocVarTyp() == 3);
	}
	
}
