package runtm;

import iconst.IConst;
import iconst.TokenTyp;
import iconst.KeywordTyp;
import iconst.BifTyp;
import iconst.NodeCellTyp;
import iconst.SysFnTyp;
import page.Node;
import page.AddrNode;
import page.Store;
import page.Page;

// Code Execution

public class RunTime implements IConst {

	private Store store;
	private static final boolean isSilent = false;
	private int rootNodep;

	public RunTime(Store store, int rootNodep) {
		this.store = store;
		this.rootNodep = rootNodep;
	}

	public boolean run() {
		omsg("RunTime.run: rootNodep = " + rootNodep);
		return true;
	}
	
	public void out(String msg) {
		if (debug) {
			System.out.println(msg);
		}
	}
	
	public void omsg(String msg) {  
		if (!isSilent) {
			System.out.println(msg);
		}
	}
	
}