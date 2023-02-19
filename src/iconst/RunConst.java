package iconst;

public interface RunConst {

	int EXIT = -1;
	int NEGADDR = -2;
	int STKOVERFLOW = -3;
	int STKUNDERFLOW = -4;
	int BADSTMT = -5;
	int BADOP = -6;
	int BADCELLTYP = -7;
	int BADALLOC = -8;
	int BADPOP = -9;
	int ZERODIV = -10;
	int KWDPOPPED = -11;
	int NULLPOPPED = -12;
	int BADSTORE = -13;
	int BADINTVAL = -14;
	int STMTINEXPR = -15;
	int BADZSTMT = -16;
	int BADSETSTMT = -17;
	int BADPARMCT = -18;
	int RTNISEMPTY = -19;
	int BADUTSTMT = -20;
	int BADTYPE = -21;
	int BADFREE = -22;
	int BADOPTYP = -23;
	int GENERR = -99;
	int NEGBASEVAL = -1000;
	int NONVAR = 0; // same as AddrNode
	int LOCVAR = 1; //
	int FLDVAR = 2; //
	int GLBVAR = 3; //
}
