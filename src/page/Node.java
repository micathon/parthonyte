package page;

import iconst.NodeCellTyp;
import iconst.KeywordTyp;

public class Node {
	
	private short header;
	private int downp;
	private int rightp;
	
	public Node(int header, int downp, int rightp) {
		this.header = (short) header;
		this.downp = downp;
		this.rightp = rightp;
	}
	
	public int getHeader() {
		return header;
	}
	
	public void setHeader(int header) {
		this.header = (short) header;
	}

	public int getDownp() {
		return downp;
	}
	
	public void setDownp(int downp) {
		this.downp = downp;
	}

	public int getRightp() {
		return rightp;
	}

	public void setRightp(int rightp) {
		this.rightp = rightp;
		/* if (rightp < 0) {  
			System.out.println("setRightp: rightp = " + rightp);
			System.exit(0);
		} */
	}
	
	public boolean isOpenPar() {
		return (header < 0);
	}
	
	public void setOpenPar(boolean flag) {
		int openPar = flag ? 0x8000 : 0;
		header = (short)((header & 0x7FFF) | openPar);
	}

	public KeywordTyp getKeywordTyp() {
		int idx = header >>> 8;
		idx &= 0x7F;
		return getKwIdxTyp(idx);
	}

	public KeywordTyp getKwIdxTyp(int idx) {
		KeywordTyp rtnval = KeywordTyp.values[idx];
		return rtnval;
	}
	
	public void setKeywordTyp(KeywordTyp kwtyp) {
		setKwIdxTyp(kwtyp.ordinal());
	}
	
	public void setKwIdxTyp(int idx) {
		idx <<= 8;
		header = (short)((header & 0x80FF) | idx);
	}
	
	public NodeCellTyp getDownCellTyp() {
		int idx = header >>> 4;
		idx &= 0xF;
		return getCellIdxTyp(idx);
	}

	public NodeCellTyp getRightCellTyp() {
		int idx = header &0xF;
		return getCellIdxTyp(idx);
	}

	public NodeCellTyp getCellIdxTyp(int idx) {
		NodeCellTyp rtnval = NodeCellTyp.values[idx];
		return rtnval;
	}
	
	public void setDownCellTyp(int idx) {
		idx <<= 4;
		header = (short)((header & 0xFF0F) | idx);
	}

	public void setRightCellTyp(int idx) {
		header = (short)((header & 0xFFF0) | idx);
	}

}
