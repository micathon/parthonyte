package scansrc;

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
import synchk.SynChk;

// Lexical Scanner

public class ScanSrc implements IConst {

	private SynChk synchk;
	private static final boolean isDetail = false;
	private static final boolean isVerbose = true;
	private Store store;
	private int currNodep, rootNodep;
	private int lineCount;
	private int colCount;
	private int tokTypLen, tokCatgLen;
	private TokenTyp allTokTyps[];
	private int errTokCounts[];
	//private int idcount = 0;
	private static final int BIFBYTNO = 1;
	private static final int SYSBYTNO = 2;
	private static final String TABSTR = "     "; // 5 blanks
	private static final String ERRSYMBUF = "[]'`,@";
	private static final char CMTLINECH = '#';
	private static final char OPENBRACECH = '{';
	private static final char CLOSEBRACECH = '}';
	private static final char QUOTECH = '"';
	private static final char OPENPARCH = '(';
	private static final char CLOSEPARCH = ')';
	private static final char SEMICOLONCH = ';';
	private static final char HYPHENCH = '-';
	private static final char UNDERSCORECH = '_';
	private static final char LONGCH = 'L';
	private static final char HEXCH = 'x';
	private static final char OCTCH = 'o';
	private static final char BINCH = 'b';
	private static final String ZEROBOXSTR = "BOX";
	private static final String[] CATGNAME = {
		"Identifiers",
		"Numeric Literals",
		"Punctuation",
		"Invalid Symbols"
	};
	public boolean inCmtBlk;
	private boolean inStrLit;
	private boolean isAllWhiteSp;
	private String strLitBuf;
	private boolean wasdo;
	private boolean wasparen;
	private boolean wassemicln;
	private boolean wasstmt;
	private boolean wasfor;
	private boolean isClean;
	private int dirtyLine;
	private int dirtyCol;
	private int fatalRtnCode = 0;
	
	public ScanSrc(Store store) {
		Node rootNode;
		lineCount = 0;
		allTokTyps = TokenTyp.values();
		tokTypLen = allTokTyps.length;
		errTokCounts = new int[tokTypLen];
		for (int i=0; i < tokTypLen; i++) {
			errTokCounts[i] = 0;
		}
		tokCatgLen = CATGNAME.length;
		inCmtBlk = false;
		inStrLit = false;
		isAllWhiteSp = false;
		strLitBuf = "";
		wasdo = false;
		wasparen = false;
		wassemicln = false;
		wasstmt = true;
		wasfor = false;
		isClean = true;
		dirtyLine = -1;
		dirtyCol = -1;
		this.store = store;
		rootNode = new Node(0, KeywordTyp.NULL.ordinal(), 0);
		rootNode.setKeywordTyp(KeywordTyp.NULL);
		rootNode.setDownCellTyp(NodeCellTyp.KWD.ordinal());
		rootNode.setRightCellTyp(NodeCellTyp.PTR.ordinal());
		rootNodep = store.allocNode(rootNode);
		currNodep = rootNodep;
	}
	
	public boolean scanCodeBuf(String inbuf) {
		char sp = ' ';
		char ch = sp;
		//char oldch = sp;
		String t, outbuf;
		String errstr = ERRSYMBUF;
		String token = "";
		String inBufSqr;
		String lineNoBuf;
		String tabstr = TABSTR;
		int colIdx = 0;
		int rtnval;
		boolean wasBackslash;
		boolean wasWhiteSp, inWhiteSp;
		boolean isAtEnd;
		
		lineCount++;
		colCount = 0;
		inWhiteSp = true;
		t = inbuf;
		inbuf = inbuf.trim();
		inBufSqr = "[" + inbuf + "]";
		outSumm(inBufSqr);
		lineNoBuf = padLineNo(lineCount);
		outbuf = lineNoBuf + sp + inBufSqr;
		if (lineCount == 0) {  // suppress
			outSumm(outbuf);  // make sure padLineNo works
			printDetl(tabstr);
			outDetl(inBufSqr);
		}
		if (inbuf.length() > 0) {
			printCmd("");
			outCmd(outbuf);
		}
		colIdx = getLeadingBlCount(t);
		for (int i=0; i < inbuf.length(); i++) {
			if (fatalRtnCode < 0) {
				return false;
			}
			wasBackslash = (ch == '\\');
			wasWhiteSp = getInWhiteSp(ch);
			isAtEnd = (i >= inbuf.length() - 1);
			colIdx++;
			colCount = colIdx;
			//oldch = ch;
			ch = inbuf.charAt(i);
			// non-functional '}' recommended to always be escaped with '\',
			//   in string lit or line comment,
			//   in case surrounding code is a block comment
			if (!inCmtBlk) { 
				if (inStrLit) { }
				else if (ch == CLOSEBRACECH) {
					putErr(TokenTyp.ERRCLOSEBRACE);
					incTokCount(TokenTyp.ERRCLOSEBRACE);
					ch = sp;
					continue;
				}
			}
			else if (ch != CLOSEBRACECH) { 
				continue;
			}
			else if (!wasBackslash) {
				putCmt(2);
				incTokCount(TokenTyp.CMTBLK);
				inCmtBlk = false;
				continue;
			}
			else {
				// '}' was escaped with '\'
				continue;
			}
			// not inCmtBlk
			if (!inStrLit) { }
			else if ((ch == '\\') && isAtEnd) {
				strLitBuf = token;
				isAllWhiteSp = true;
				continue;
			}
			else if (ch != QUOTECH) {
				token += ch;
				if (wasBackslash && (ch == '\\')) {
					// backslashes cancel
					wasBackslash = false;
					ch = sp;
				}
				continue;
			}
			else if (wasBackslash) {
				// quote char. is escaped
				token += ch;
				continue;
			}
			else {
				token += ch;
				doTokenRtn(TokenTyp.STRLIT, token);
				putStr(token);
				inStrLit = false;
				isAllWhiteSp = false;
				token = "";
				strLitBuf = "";
				continue;
			}
			// not inStrLit
			inWhiteSp = getInWhiteSp(ch);
			if (wasWhiteSp && !inWhiteSp) {
				if (ch == QUOTECH) {
					inStrLit = true;
					isAllWhiteSp = false;
					if (strLitBuf.length() == 0) {
						token = "" + ch;
					}
					else {
						token = strLitBuf;
					}
					continue;
				}
				else if (isAllWhiteSp) {
					isAllWhiteSp = false;
					strLitBuf = "";
					putErr(TokenTyp.ERRMULTISTRLITBADCHAR);
					incTokCount(TokenTyp.ERRMULTISTRLITBADCHAR);
				}
				token = "" + ch;
				continue;
			}
			else if (!inWhiteSp) {
				token += ch;
				continue;
			}
			else if (!wasWhiteSp) {
				// handle token, (inWhiteSp && !wasWhiteSp)
				out("main loop: doToken = " + token);
				doToken(token);
				token = "";
			}
			if (ch == OPENBRACECH) {
				inCmtBlk = true;
				inWhiteSp = true;
				if (token.length() > 0) {
					doToken(token);
					token = "";
				}
				putCmt(1);
				continue;
			}
			if (ch == CMTLINECH) {
				putCmt(3);
				incTokCount(TokenTyp.CMTLINE);
				break;
			}
			rtnval = 0;
			if (ch == OPENPARCH) {
				if (wasfor) {
					inWhiteSp = false;
					doToken("do");
					if (fatalRtnCode < 0) {
						return false;
					}
				}
				rtnval = putPar(1);
				incTokCount(TokenTyp.OPENPAR);
				outStructTok(ch);
			}
			else if (ch == CLOSEPARCH) {
				//out("ch = ), oldch = [" + oldch + ']');
				rtnval = putPar(2);
				incTokCount(TokenTyp.CLOSEPAR);
				outStructTok(ch);
			}
			else if (ch == SEMICOLONCH) {
				rtnval = putPar(3);
				incTokCount(TokenTyp.SEMICOLON);
				outStructTok(ch);
			}
			else if (errstr.indexOf(ch) >= 0) {
				rtnval = 0;
				putColErr(TokenTyp.ERRSYM, colIdx);
				incTokCount(TokenTyp.ERRSYM);
				inWhiteSp = true;
			}
			if (rtnval < 0) {
				putRtnCodeErr(rtnval);
			}
		}
		if (inStrLit) {
			if (!isAllWhiteSp) {
				putErr(TokenTyp.ERRCLOSEQUOTE);
				incTokCount(TokenTyp.ERRCLOSEQUOTE);
			}
			inStrLit = false;
		}
		else if (!inWhiteSp) {
			// handle token
			doToken(token);
		}
		return (fatalRtnCode >= 0);
	}
	
	private void doToken(String token) {
		TokenTyp toktyp = TokenTyp.OPERATOR;
		char ch;
		
		if (token.length() == 0) {
			return;
		}
		ch = token.charAt(0);
		if (ch == HYPHENCH && token.length() > 1) {
			ch = token.charAt(1);
			if (Character.isDigit(ch)) {
				toktyp = TokenTyp.DECIMAL;
			}
		}
		else if (ch == UNDERSCORECH) {
			toktyp = TokenTyp.IDENTIFIER;
		}
		else if (Character.isDigit(ch)) {
			toktyp = TokenTyp.DECIMAL;
		}
		else if (Character.isLetter(ch)) {
			toktyp = TokenTyp.IDENTIFIER;
		}
		switch (toktyp) {
		case IDENTIFIER:
			toktyp = getTokIdentifier(token);
			break;
		case DECIMAL:
			toktyp = getTokNumeric(token);
			break;
		case OPERATOR:
			toktyp = getTokOperator(token);
			break;
		default:
			return;
		}
		wasfor = (token.equals("for"));
		outSumm(token);
		incTokCount(toktyp);
		out("doToken: " + token);
	}
	
	private void incTokCount(TokenTyp toktyp) {
		doTokenRtn(toktyp, "");
	}
	
	private void doTokenRtn(TokenTyp toktyp, String token) {
		int ord = toktyp.ordinal();
		++errTokCounts[ord];
		if (token.length() == 0) {
			return;
		}
		outSumm(token);
	}
	
	public int getLeadingBlCount(String buf) {
		int count = 0;
		for (int i=0; i < buf.length(); i++) {
			if (buf.charAt(i) != ' ') {
				break;
			}
			count++;
		}
		return count;
	}
	
	private void outStructTok(char ch) {
		printSumm("" + ch + ' ');
	}
	
	private void printSumm(String msg) {
		outSummDetl(!isDetail, false, msg);
	}
	
	private void printDetl(String msg) {
		outSummDetl(isDetail, false, msg);
	}
	
	private void printCmd(String msg) {
		outSdRtn(isDetail, false, msg);
	}
	
	private void outSumm(String msg) {
		outSummDetl(!isDetail, true, msg);
	}
	
	private void outDetl(String msg) {
		outSummDetl(isDetail, true, msg);
	}
	
	private void outCmd(String msg) {
		outSdRtn(isDetail, true, msg);
	}
	
	private void outSummDetl(boolean show, boolean cr, String msg) {
		if (show && isVerbose) {
			outSdRtn(show, cr, msg);
		}
	}
	
	private void outSdRtn(boolean show, boolean cr, String msg) {
		if (show) {
			System.out.print(msg);
			if (cr) {
				System.out.println("");
			}
		}
	}
	
	public void out(String msg) {
		if (debug) {
			System.out.println(msg);
		}
	}
	
	public void omsg(String msg) {
		System.out.println(msg);
	}
	
	private String padLineNo(int lineNo) {
		String s = "000" + lineNo;
		int idx;
		
		if (lineNo > 9999) {
			return "" + lineNo;
		}
		idx = s.length() - 4;  // return 4-digit string
		s = s.substring(idx);
		return s;
	}
	
	public void scanSummary(boolean fatalErr) {
		TokenTyp toktyp;
		int catgIdx = -1;
		int idx;
		boolean tokensFound = false;
		int tokenCount, listCount, count;
		
		if (fatalErr) {
			out("Fatal error encountered!");
		}
		count = synchk.getNodeCount();
		tokenCount = count & 0xFFFF;
		listCount = count >>> 16;
		out("Nodes created = " + tokenCount);
		out("Lists created = " + listCount);
		outSumm("\nLines read = " + lineCount);
		outSumm("");
		if (tokCatgLen == 0) {
			return;  // not needed, no yellow
		}
		for (int i=0; i < tokTypLen; i++) {
			if (errTokCounts[i] <= 0) {
				continue;
			}
			tokensFound = true;
			toktyp = allTokTyps[i];
			idx = getTokCatgIdx(toktyp);
			if (idx > catgIdx) {
				catgIdx = idx;
				outSumm("Tokens found: " + getTokCatg(idx));
			}
			outSumm(
				"  Count of " + allTokTyps[i] + " = " +
				errTokCounts[i]
			);
		}
		if (tokensFound) {
			outSumm("");
		}
		if (isClean && synchk.isValidSrc()) {
			out("Src file is valid.");
		}
		else {
			if (!isClean) {
				out("Error detected during initial scan:");
				out("Line no. = " + dirtyLine);
				out("Column no. = " + dirtyCol);
				out("");
			}
			out("Src file is invalid!");
		}
	}
	
	public void setSynChk(SynChk synchk) {
		this.synchk = synchk;
	}
	
	public int getRootNodep() {
		return rootNodep;
	}
	
	private String getTokCatg(int catgIdx) {
		return CATGNAME[catgIdx];
	}
	
	private int getTokCatgIdx(TokenTyp toktyp) {
		int ord = toktyp.ordinal();

		if (ord <= TokenTyp.IDENTIFIER.ordinal()) {
			return 0;
		}
		if (ord <= TokenTyp.FLOAT.ordinal()) {
			return 1;
		}
		if (ord <= TokenTyp.OPERATOR.ordinal()) {
			return 2;
		}
		return 3;
	}
	
	private boolean getInWhiteSp(char ch) {
		boolean inWhiteSp;
		inWhiteSp = (ch == ' ') || (ch == '\t') || 
			(ch == OPENBRACECH) || (ch == CMTLINECH) ||
			(ch == OPENPARCH) || (ch == CLOSEPARCH) || (ch == SEMICOLONCH);
		return inWhiteSp;
	}
	
	private boolean isAllLowerCase(String token) {
		char ch;
		if (token.length() == 0) {
			return false;
		}
		for (int i=0; i < token.length(); i++) {
			ch = token.charAt(i);
			if (!Character.isLetter(ch)) {
				return false;
			}
		}
		return token.toLowerCase().equals(token);
	}
	
	private TokenTyp getTokIdentifier(String token) {
		char ch;
		int i = 0;
		boolean valid = true;
		TokenTyp toktyp = TokenTyp.ERRIDENTIFIER;
		
		while ((ch = token.charAt(i)) == UNDERSCORECH) {
			if (++i > token.length()) {
				valid = false;
				break;
			}
		}
		if (!valid || !Character.isLetter(ch)) {
			putTokErr(TokenTyp.ERRIDENTIFIER, token);
			return toktyp;
		}
		if (i == 2) {
			return getTokSysFunc(token);
		}
		if (i == 1) {
			return getValidTokId(token);
		}
		if (i > 2) {
			putTokErr(TokenTyp.ERRIDOVERSLASH, token);
			return toktyp;
		}
		// i = 0
		if (isAllLowerCase(token)) {
			return getTokLowerCase(token);
		}
		return getValidTokId(token);
	}
	
	private TokenTyp getTokLowerCase(String token) {
		KeywordTyp kwtyp = KeywordTyp.NULL;
		BifTyp cftyp;
		boolean isKeyword = true;
		boolean startsWithZed;
		int kwidx;
		String intok = token;
		int rtnCode = 0;
		TokenTyp toktyp = TokenTyp.IDENTIFIER;
		
		token = token.toUpperCase();
		startsWithZed = (token.charAt(0) == 'Z'); // znull,zparen,zstmt,zcall: internal use
		try {
			kwtyp = KeywordTyp.valueOf(token);
		} catch (IllegalArgumentException exc) {
			isKeyword = false;
		}
		if (isKeyword && startsWithZed) { 
			toktyp = TokenTyp.ERRZKEYWD;
			rtnCode = getNegErrCode(toktyp);
		}
		else if (isKeyword) {
			rtnCode = putKwd(token, kwtyp);
			toktyp = TokenTyp.KEYWORD;
		}
		else {
			cftyp = getBifTyp(token);
			if (cftyp != BifTyp.NULL) {
				putFun(token, cftyp);
				kwidx = cftyp.ordinal() | (BIFBYTNO << 8);
				rtnCode = addNode(NodeCellTyp.KWD, kwidx, 0.0, "");
				toktyp = TokenTyp.BLTINFUNC;
			}
			else {
				rtnCode = putIdent(intok);
			}
		}
		if (rtnCode < 0) {
			putRtnCodeErr(rtnCode);
		}
		return toktyp;
	}
	
	public BifTyp getBifTyp(String token) {
		BifTyp cftyp;
		try {
			cftyp = BifTyp.valueOf(token); 
		} catch (IllegalArgumentException exc) {
			cftyp = BifTyp.NULL;
		}
		return cftyp;
	}
	
	public BifTyp getInt2Bif(int kwidx) {
		BifTyp cftyp;
		int mask = (BIFBYTNO << 8);
		
		if ((kwidx & ~mask) == 0) {
			return BifTyp.NULL;
		}
		kwidx &= 0x00FF;
		cftyp = BifTyp.values[kwidx];
		return cftyp;
	}
	
	private boolean isIdentifierOk(String token) {
		// assumes token starts with optional underscore(s)
		//   followed by a letter
		String t = "-_";
		char ch;
		
		for (int i=0; i < token.length(); i++) {
			ch = token.charAt(i);
			if ((t.indexOf(ch) < 0) && 
				!Character.isLetter(ch) && !Character.isDigit(ch)) {
				return false;
			}
		}
		return true;
	}
	
	private TokenTyp getValidTokId(String token) {
		int rtnCode = 0;
		if (isIdentifierOk(token)) {
			rtnCode = putIdent(token);
			if (rtnCode < 0) {
				putRtnCodeErr(rtnCode);
			}
			return TokenTyp.IDENTIFIER;
		}
		putTokErr(TokenTyp.ERRIDENTIFIER, token);
		return TokenTyp.ERRIDENTIFIER;
	}
	
	private TokenTyp getTokSysFunc(String token) {
		if (isSysFuncOk(token)) {
			putSys(token);
			return TokenTyp.SYSFUNC;
		}
		putTokErr(TokenTyp.ERRSYSFUNC, token);
		return TokenTyp.ERRIDENTIFIER;
	}
	
	private boolean isSysFuncOk(String token) {
		SysFnTyp kwtyp;
		int kwidx;
		boolean isKeyword = true;
		int rtnCode = 0;
		
		token = token.toUpperCase();
		try {
			kwtyp = SysFnTyp.valueOf(token);
			kwidx = kwtyp.ordinal() | (SYSBYTNO << 8);
			rtnCode = addNode(NodeCellTyp.KWD, kwidx, 0.0, "");
		} catch (IllegalArgumentException exc) {
			isKeyword = false;
		}
		if (rtnCode < 0) {
			putRtnCodeErr(rtnCode);
		}
		return isKeyword;
	}
	
	private TokenTyp getTokOperator(String token) {
		KeywordTyp kwtyp = getOpKeyword(token);
		String keyword = kwtyp.toString();
		int rtnCode;

		if (kwtyp == KeywordTyp.NULL) {
			out("kwtyp is null!");  //##
			putTokErr(TokenTyp.ERROP, token);
			return TokenTyp.ERROP;
		}
		rtnCode = putKwdOp(keyword, token, kwtyp);
		if (rtnCode < 0) {
			putRtnCodeErr(rtnCode);
		}
		return TokenTyp.OPERATOR;
	}
	
	private TokenTyp getTokNumeric(String token) {
		TokenTyp toktyp;
		char ch = token.charAt(0);
		boolean isNeg = false;
		boolean isBox = false;
		String s = ZEROBOXSTR;
		String sign = "";
		int rtnval = 0;
		
		token = token.toUpperCase();
		if (ch == HYPHENCH) {
			isNeg = true;
			sign = "-";
			token = token.substring(1);
		}
		ch = token.charAt(0);
		if (ch == '0' && token.length() >= 2 &&
			s.indexOf(token.charAt(1)) >= 0)
		{
			isBox = true;
		}
		if (!isBox) {
			if (isValidNumTok(TokenTyp.DECIMAL, token)) {
				rtnval = putDec(token, isNeg);
				toktyp = TokenTyp.DECIMAL;
			}
			else if (isValidNumTok(TokenTyp.LONG, token)) {
				token = token.substring(0, token.length() - 1);
				rtnval = putLng(token, isNeg);
				toktyp = TokenTyp.LONG;
			}
			else if (isValidFloat(token)) {
				rtnval = putFlt(token, isNeg);
				toktyp = TokenTyp.FLOAT;
			}
			else {
				putTokErr(TokenTyp.ERRNUM, token);
				toktyp = TokenTyp.ERRNUM;
			}
			if (rtnval < 0) {
				putRtnCodeErr(rtnval);
			}
			return toktyp;
		}
		toktyp = TokenTyp.ERRNUM;
		token = token.toLowerCase();
		ch = token.charAt(1);
		token = token.substring(2);
		switch (ch) {
		case HEXCH:
			if (isValidNumTok(TokenTyp.HEXADECIMAL, token)) {
				rtnval = putHex(sign + token);
				toktyp = TokenTyp.HEXADECIMAL;
			}
			break;
		case OCTCH:
			if (isValidNumTok(TokenTyp.OCTAL, token)) {
				rtnval = putOct(sign + token);
				toktyp = TokenTyp.OCTAL;
			}
			break;
		case BINCH:
			if (isValidNumTok(TokenTyp.BINARY, token)) {
				rtnval = putBin(sign + token);
				toktyp = TokenTyp.BINARY;
			}
			break;
		}
		if (rtnval < 0) {
			putRtnCodeErr(rtnval);
		}
		if (toktyp == TokenTyp.ERRNUM) {
			putTokErr(toktyp, token);
		}
		return toktyp;
	}
	
	private boolean isValidNumTok(TokenTyp toktyp, String token) {
		boolean isdig;
		boolean ishex;
		boolean isoct;
		boolean isbin;
		boolean islong;
		char ch;
		
		token = token.toUpperCase();
		for (int i=0; i < token.length(); i++) {
			ch = token.charAt(i);
			isdig = Character.isDigit(ch);
			ishex = (ch >= 'A' && ch <= 'F');
			isoct = (ch >= '0' && ch <= '7');
			isbin = (ch == '0' || ch == '1');
			islong = (ch == LONGCH && (i == token.length() - 1));
			switch (toktyp) {
			case DECIMAL:
				if (!isdig) {
					return false;
				}
				break;
			case LONG:
				if (!isdig && !islong) {
					return false;
				}
				break;
			case HEXADECIMAL:
				if (!isdig && !ishex) {
					return false;
				}
				break;
			case OCTAL:
				if (!isoct) {
					return false;
				}
				break;
			case BINARY:
				if (!isbin) {
					return false;
				}
				break;
			default:
				return false;
			}
		}
		return true;
	}
	
	@SuppressWarnings("unused")
	private boolean isValidFloat(String token) {
		double t;
		try {
			t = Double.parseDouble(token);
		} catch (NumberFormatException exc) {
			return false;
		}
		return true;
	}
	
	private int putKwd(String token, KeywordTyp kwtyp) {
		String outbuf;
		String opstr;
		char sp = ' ';
		
		token = token.toLowerCase();
		opstr = getOpStr(kwtyp);
		if (opstr.length() == 0) {
			outbuf = TABSTR + "KWD" + sp + token;
			outDetl(outbuf);
			return addNode(NodeCellTyp.KWD, kwtyp.ordinal(), 0.0, "");
		}
		return putKwdOp(token, opstr, kwtyp);
	}

	private int putKwdOp(String kwstr, String opstr, KeywordTyp kwtyp) {
		String outbuf;
		char sp = ' ';
		
		outbuf = TABSTR + "OP " + sp + kwstr.toLowerCase() + sp + opstr;
		outDetl(outbuf);
		return addNode(NodeCellTyp.KWD, kwtyp.ordinal(), 0.0, "");
	}
	
	private void putFun(String token, BifTyp cftyp) {
		String outbuf;
		char sp = ' ';
		
		token = token.toLowerCase();
		outbuf = TABSTR + "FUN" + sp + token;
		outDetl(outbuf);
	}
	
	private void putSys(String token) {
		String outbuf;
		char sp = ' ';
		
		token = token.toLowerCase();
		outbuf = TABSTR + "SYS" + sp + token;
		outDetl(outbuf);
	}
	
	private int putIdent(String token) {
		String outbuf;
		char sp = ' ';
		
		outbuf = TABSTR + "ID " + sp + token;
		outDetl(outbuf);
		out("Ident added: " + token);
		return addNode(NodeCellTyp.ID, 0, 0.0, token);
	}
	
	private int putBin(String token) {
		return putNum(token, 2);
	}
	
	private int putOct(String token) {
		return putNum(token, 8);
	}
	
	private int putHex(String token) {
		return putNum(token, 16);
	}
	
	private int putDec(String token, boolean isNeg) {
		if (isNeg) {
			token = '-' + token;
		}
		return putNum(token, 10);
	}
	
	private int putLng(String token, boolean isNeg) {
		if (isNeg) {
			token = '-' + token;
		}
		return putNum(token, -1);
	}
	
	private int putNum(String token, int radix) {
		String outbuf;
		String radixStr;
		char sp = ' ';
		long val = 0;
		String sval;
		boolean isLong = false;
		
		switch (radix) {
		case 2:
			radixStr = "BIN";
			break;
		case 8:
			radixStr = "OCT";
			break;
		case 16:
			radixStr = "HEX";
			break;
		case -1:
			radixStr = "LNG";
			radix = 10;
			isLong = true;
			break;
		default:
			radixStr = "DEC";
			radix = 10;
		}
		try {
			val = Long.parseLong(token, radix);
			sval = "" + val;
		} catch (NumberFormatException exc) {
			sval = "Invalid";
		}
		outbuf = TABSTR + radixStr + sp + token + sp + sval;
		outDetl(outbuf);
		if (isLong) {
			return addNode(NodeCellTyp.LONG, val, 0.0, "");
		}
		else {
			return addNode(NodeCellTyp.INT, val, 0.0, "");
		}
	}

	private int putFlt(String token, boolean isNeg) {
		String outbuf;
		char sp = ' ';
		double val = 0.0;
		String sval;
		
		if (isNeg) {
			token = '-' + token;
		}
		try {
			val = Double.parseDouble(token);
			sval = "" + val;
		} catch (NumberFormatException exc) {
			sval = "Invalid";
		}
		outbuf = TABSTR + "FLT" + sp + token + sp + sval;
		outDetl(outbuf);
		return addNode(NodeCellTyp.DOUBLE, 0, val, "");
	}
	
	private int putStr(String token) {
		int i, j;
		String stripTok;
		String outbuf;
		char sp = ' ';
		
		token = token.trim();
		i = token.indexOf(QUOTECH);
		j = token.length() - 1;
		if (token.charAt(j) != QUOTECH) {
			j++;
		}
		stripTok = token.substring(i + 1, j);
		outbuf = TABSTR + "STR" + sp + token + sp + 
			'[' + stripTok + ']';
		outDetl(outbuf);
		return addNode(NodeCellTyp.STRING, 0, 0.0, stripTok);
	}
	
	private int addNode(NodeCellTyp celltyp, long val, double dval,
		String sval)
	{
		int nodep;
		Node node;
		Page page;
		int idx;
		KeywordTyp kwtyp;
		int rtnval = 0;
		
		nodep = addSimpleNode(celltyp, val, dval, sval);
		if (nodep < 0) {
			out("addNode: < 0");
			return nodep;
		}
		page = store.getPage(nodep);
		idx = store.getElemIdx(nodep);
		node = page.getNode(idx);
		kwtyp = node.getKeywordTyp();
		out("addNode: nodep = " + nodep + ", idx = " + idx +
			", kwd = " + kwtyp);
		return rtnval;
	}
	
	private int addSimpleNode(NodeCellTyp celltyp, long val, double dval,
		String sval)
	{
		// Handle non-structural tokens, not parentheses or semicolons
		Node node;
		Page page;
		int downp = 0;
		int rightp;
		KeywordTyp kwtyp;
		AddrNode addrNode;
		int idx;
		boolean isDoBlock = false;

		switch (celltyp) {
		case KWD:
			downp = (int) val;
			out("addSimp: KWD, downp = " + downp);
			isDoBlock = (downp == KeywordTyp.DO.ordinal());
			break;
		case INT:
			downp = (int) val;
			break;
		case LONG:
			downp = store.allocLong(val);
			break;
		case DOUBLE:
			downp = store.allocDouble(dval);
			break;
		case STRING:
		case ID:
			downp = store.allocString(sval);
			break;
		default:
			downp = 0;
		}
		wasstmt = true;
		wassemicln = false;
		if (wasparen) {
			wasparen = false;
			return addZparNode(celltyp, downp);
		}
		if (wasdo) {
			// do keyword must be followed by (
			wasdo = false;
			return getNegErrCode(TokenTyp.ERRDOMISSINGBLK);
		}
		page = store.getPage(currNodep);
		idx = store.getElemIdx(currNodep);
		//currNode = page.getNode(idx);
		//kwtyp = currNode.getKeywordTyp();
		kwtyp = KeywordTyp.ZNULL;
		node = new Node(0, downp, 0);  // node of new token
		if (celltyp == NodeCellTyp.KWD && downp < 256) { 
			kwtyp = KeywordTyp.values[downp];
		}
		node.setKeywordTyp(kwtyp);
		node.setDownCellTyp(celltyp.ordinal());
		node.setRightCellTyp(NodeCellTyp.PTR.ordinal());
		out("currNodep = " + currNodep + ", idx = " + idx);
		rightp = store.allocNode(node);
		out("rightp = " + rightp + ", celltyp = " + celltyp +
			", kwd = " + node.getKeywordTyp());
		page.setPtrNode(idx, rightp);
		if (isDoBlock) {
			// push do node
			out("addSimp: isDoBlk");
			wasdo = true;
			wassemicln = true;
			node.setOpenPar(true);
			node.setDownp(0);
			addrNode = new AddrNode(0, rightp);
			if (!store.pushNode(addrNode)) {
				return getNegErrCode(TokenTyp.ERRSTKOVRFLW);
			}
			if (!store.pushByte((byte) downp)) {
				return getNegErrCode(TokenTyp.ERRSTKOVRFLW);
			}
		}
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		page.setNode(idx, node);
		store.appendNodep(rightp, lineCount);  // store line no. of token
		currNodep = rightp;
		return currNodep;
	}
		
	private int addZparNode(NodeCellTyp celltyp, int downp) {
		// Insert zparen or zstmt node (after open paren)
		Node currNode, node;
		Page page;
		int idx;
		KeywordTyp kwtyp;
		AddrNode addrNode;
		int rightp;
		NodeCellTyp ctyp;
		boolean nullCellTyp;
		String varName;

		page = store.getPage(currNodep);
		idx = store.getElemIdx(currNodep);
		currNode = page.getNode(idx);
		store.appendNodep(currNodep, lineCount);
		node = new Node(0, 0, 0);
		rightp = store.allocNode(node);
		if (!isTopKwtyp(KeywordTyp.DO)) {
			kwtyp = KeywordTyp.ZPAREN;
			currNode.setRightp(rightp);
			out("addZpar: not DO, currNodep = " + currNodep);
		}
		else {
			kwtyp = KeywordTyp.ZSTMT;
			if (wasdo) {
				currNode.setDownp(rightp);
				out("addZpar: setDownp(p), p = " + rightp);
			}
			else {
				currNode.setRightp(rightp);
				out("addZpar: setRightp(p), p = " + rightp);
			}
			out("addZpar: currNodep = " + currNodep);
			wasdo = false;
		}
		page.setNode(idx, currNode);
		// push zparen or zstmt keyword and pointer to new node
		addrNode = new AddrNode(0, rightp);
		if (!store.pushNode(addrNode)) {
			return getNegErrCode(TokenTyp.ERRSTKOVRFLW);
		}
		if (!store.pushByte((byte) kwtyp.ordinal())) {
			return getNegErrCode(TokenTyp.ERRSTKOVRFLW);
		}
		node.setKeywordTyp(kwtyp);
		ctyp = NodeCellTyp.PTR;
		node.setDownCellTyp(ctyp.ordinal());
		node.setRightCellTyp(ctyp.ordinal());
		node.setOpenPar(true);
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		currNode = new Node(0, 0, 0);  // insert 2nd node containing token after (
		rightp = store.allocNode(currNode);
		store.appendNodep(rightp, lineCount);
		node.setDownp(rightp);
		page.setNode(idx, node);
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		if (celltyp == NodeCellTyp.KWD && downp < 256) {
			// prev token = open paren, curr token = kwd
			kwtyp = KeywordTyp.values[downp];
			currNode.setKeywordTyp(kwtyp);
			currNode.setDownCellTyp(celltyp.ordinal());
			currNode.setRightCellTyp(NodeCellTyp.PTR.ordinal());
			page.setNode(idx, currNode);
			out("List kwtyp = " + kwtyp + ", downp = " + downp);
			out("rightp = " + rightp + ", celltyp = " + celltyp);
			currNodep = rightp;
			return currNodep;
		}
		else if (celltyp == NodeCellTyp.KWD) {  // (built-in func x y z)
			// warning: sys funcs not handled!
			currNode.setKeywordTyp(KeywordTyp.NULL);
			currNode.setDownCellTyp(celltyp.ordinal());
			currNode.setRightCellTyp(NodeCellTyp.PTR.ordinal());
			currNode.setDownp(downp);
			page.setNode(idx, currNode);
			out("BIF kwtyp = " + kwtyp + ", downp = " + downp);
			out("rightp = " + rightp + ", celltyp = " + celltyp);
			currNodep = rightp;
			return currNodep;
		}
		if (celltyp == NodeCellTyp.ID) {  // (func x y z)
			varName = store.getVarName(downp);
			out("( varname = " + varName);
			if (isCrPathName(varName)) {  
				return genCrPathCall(varName, downp, rightp);
			}
			kwtyp = KeywordTyp.ZCALL;
			celltyp = NodeCellTyp.FUNC;
			currNode.setKeywordTyp(kwtyp);
			currNode.setDownCellTyp(celltyp.ordinal());
			currNode.setRightCellTyp(NodeCellTyp.PTR.ordinal());
			currNode.setDownp(downp);
			page.setNode(idx, currNode);
			out("Func kwtyp = " + kwtyp + ", downp = " + downp);
			out("rightp = " + rightp + ", celltyp = " + celltyp);
			currNodep = rightp;
			return currNodep;
		}
		nullCellTyp = (celltyp == NodeCellTyp.NULL);
		if (isConstCellTyp(celltyp) || nullCellTyp) {
			// handle () or (123 ...) or ("hello" ...)
			// insert tuple keyword node
			kwtyp = KeywordTyp.TUPLE;
			ctyp = NodeCellTyp.KWD;
			currNode.setKeywordTyp(kwtyp);
			currNode.setDownCellTyp(ctyp.ordinal());
			currNode.setRightCellTyp(NodeCellTyp.PTR.ordinal());
			page.setNode(idx, currNode);
			out("Const kwtyp = " + kwtyp + ", downp = " + downp);
			out("rightp = " + rightp + ", celltyp = " + celltyp);
			if (!nullCellTyp) {
				// insert node of numeric constant or string literal
				node = currNode;
				currNode = new Node(0, 0, 0);
				rightp = store.allocNode(currNode);
				node.setRightp(rightp);
				page.setNode(idx, node);
				page = store.getPage(rightp);
				idx = store.getElemIdx(rightp);
				kwtyp = KeywordTyp.ZNULL;
				currNode.setKeywordTyp(kwtyp);
				currNode.setDownCellTyp(celltyp.ordinal());
				currNode.setRightCellTyp(NodeCellTyp.PTR.ordinal());
				currNode.setDownp(downp);
				page.setNode(idx, currNode);
			}
			currNodep = rightp;
			return currNodep;
		}
		// note: (( is an error
		return getNegErrCode(TokenTyp.ERRPOSTPARTOK);
	}
	
	private int doAddZtuple() {
		// "do (" [;]... + )
		// or:
		// "curr (" + )
		int rtnval;
		
		// do => zstmt, [push zstmt] => tuple
		// or:
		// curr -> zparen, [push zparen]
		//
		// Notation: ->: rightp, =>: downp
		wasstmt = true;
		rtnval = addZparNode(NodeCellTyp.NULL, 0);
		if (rtnval < 0) {
			return rtnval;
		}
		out("doAddZtuple: rtnval = " + rtnval);
		return popZparStmt(true);
	}
	
	private int popZparStmt(boolean isTuple) {
		// pop zstmt or zparen
		AddrNode addrNode;
		int byteval;
		KeywordTyp kwtyp;
		boolean isZstmt;
		boolean isZparen;

		if (isTuple) { }
		else if (isTopKwtyp(KeywordTyp.DO)) { 
			// pop do
			byteval = store.popByte();
			addrNode = store.popNode();
			if (addrNode == null) {
				return getNegErrCode(TokenTyp.ERRSTKUNDFLW);
			}
			currNodep = addrNode.getAddr();
			out("popZparStmt: isDoBlk, currNodep = " + currNodep);
			return 0;
		}
		// pop zstmt or zparen
		byteval = store.popByte();
		addrNode = store.popNode();
		if (addrNode == null) {
			return getNegErrCode(TokenTyp.ERRSTKUNDFLW);
		}
		currNodep = addrNode.getAddr();
		kwtyp = KeywordTyp.values[byteval];
		out("popZparStmt: kwtyp = " + kwtyp);
		isZstmt = (kwtyp == KeywordTyp.ZSTMT); 
		isZparen = (kwtyp == KeywordTyp.ZPAREN); 
		if (!isZstmt && !isZparen) {
			return getNegErrCode(TokenTyp.ERRBADZPAREN);
		}
		if (isZparen) {
			return 0;
		}
		// pop do
		byteval = store.popByte();
		addrNode = store.popNode();
		if (addrNode == null) {
			return getNegErrCode(TokenTyp.ERRSTKUNDFLW);
		}
		kwtyp = KeywordTyp.values[byteval];
		if (kwtyp != KeywordTyp.DO) {
			return getNegErrCode(TokenTyp.ERRBADDO);
		}
		currNodep = addrNode.getAddr();
		out("popZparStmt: popped do, currNodep = " + currNodep);
		return 0;
	}
		
	private int popZstmt() {
		// pop zstmt
		AddrNode addrNode;
		int byteval;
		KeywordTyp kwtyp;
		boolean isZstmt;

		byteval = store.popByte();
		addrNode = store.popNode();
		if (addrNode == null) {
			return getNegErrCode(TokenTyp.ERRSTKUNDFLW);
		}
		currNodep = addrNode.getAddr();
		kwtyp = KeywordTyp.values[byteval];
		out("popZStmt: kwtyp = " + kwtyp);
		isZstmt = (kwtyp == KeywordTyp.ZSTMT); 
		if (!isZstmt) {
			return getNegErrCode(TokenTyp.ERRBADZPAREN);
		}
		out("popZStmt: currNodep = " + currNodep);
		return 0;
	}
		
	private int closeParenRtn(boolean isSemicln) {
		wassemicln = false;
		wasparen = false;
		out("closeParen: semi/wasstmt = (" + isSemicln + ", " + wasstmt + ")");
		if (!isSemicln && !wasstmt) { 
			return doAddZtuple();
		}
		if (isSemicln) {
			return popZstmt();
		}
		return popZparStmt(false);
	}

	private int doAddSemicolon() {
		int rtnval;

		if (wassemicln) {
			return 0;
		}
		//wasdo = true;
		if (!isTopKwtyp(KeywordTyp.ZSTMT)) { 
			return getNegErrCode(TokenTyp.ERRSEMICLN);
		}
		rtnval = closeParenRtn(true);
		if (rtnval == 0) {
			rtnval = doAddParenRtn(true);
		}
		wassemicln = true;
		return rtnval;
	}
	
	private int closeParen() {
		return closeParenRtn(false);
	}
	
	private int doAddParen() {
		return doAddParenRtn(false);
	}
		
	private int doAddParenRtn(boolean isSemicln) {
		if (wasparen) {
			return getNegErrCode(TokenTyp.ERRPARENRPT);
		}
		wasparen = true;
		if (!isSemicln) {
			wasstmt = false;
		}
		return 0;
	}
	
	private boolean isTopKwtyp(KeywordTyp kwtyp) {
		int byteval;
		KeywordTyp topktyp;
		
		byteval = store.topByte();
		topktyp = KeywordTyp.values[byteval];
		return (kwtyp == topktyp);
	}
	
	private boolean isConstCellTyp(NodeCellTyp celltyp) {
		switch (celltyp) {
		case BOOLEAN:
		case INT:
		case LONG:
		case DOUBLE:
		case STRING:
			out("isConst-celltyp = " + celltyp);
			return true;
		default:
			return false;
		}
	}
	
	private int genCrPathCall(String varName, int downp, int rightp) {
		Node node;
		Node prevNode;
		Page page;
		int idx;
		KeywordTyp kwtyp;
		NodeCellTyp celltyp;
		BifTyp biftyp;
		int kwidx;
		int crPathLen;
		int crPathVal;

		crPathLen = varName.length() - 2;
		crPathVal = getCrPathVal(varName);
		//store.setVarName(downp, varName);
		out("genCrPathCall: len = " + crPathLen + ", val = " + crPathVal);
		
		// (crpath
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		node = page.getNode(idx);
		biftyp = BifTyp.CRPATH;
		kwidx = biftyp.ordinal() | (BIFBYTNO << 8);
		kwtyp = KeywordTyp.NULL;
		celltyp = NodeCellTyp.KWD;
		node.setKeywordTyp(kwtyp);
		node.setDownCellTyp(celltyp.ordinal());
		node.setRightCellTyp(NodeCellTyp.PTR.ordinal());
		node.setDownp(kwidx);
		prevNode = node;

		// (crpath -> len
		node = new Node(0, 0, 0);
		rightp = store.allocNode(node);
		prevNode.setRightp(rightp);
		page.setNode(idx, prevNode);
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		kwtyp = KeywordTyp.NULL;
		celltyp = NodeCellTyp.INT;
		node.setDownCellTyp(celltyp.ordinal());
		node.setRightCellTyp(NodeCellTyp.PTR.ordinal());
		node.setDownp(crPathLen);
		prevNode = node;

		// (crpath, len -> val
		node = new Node(0, 0, 0);
		rightp = store.allocNode(node);
		prevNode.setRightp(rightp);
		page.setNode(idx, prevNode);
		page = store.getPage(rightp);
		idx = store.getElemIdx(rightp);
		kwtyp = KeywordTyp.NULL;
		celltyp = NodeCellTyp.INT;
		node.setDownCellTyp(celltyp.ordinal());
		node.setRightCellTyp(NodeCellTyp.PTR.ordinal());
		node.setDownp(crPathVal);
		page.setNode(idx, node);
		currNodep = rightp;
		return currNodep;
	}
	
	public boolean isCrPathName(String name) {
		int i, len;
		char c;
		
		len = name.length();
		if (len < 4) {
			return false;
		}
		if (name.charAt(0) != 'c') {
			return false;
		}
		if (name.charAt(len - 1) != 'r') {
			return false;
		}
		for (i = 1; i < len - 1; i++) {
			c = name.charAt(i);
			if ((c != 'a') && (c != 'd')) {
				return false;
			}
		}
		return true;
	}
	
	public int getCrPathVal(String name) {
		int i, k, n;
		char c;
		int sum = 0;
		
		n = name.length() - 1;
		for (i = 1; i < n; i++) {
			c = name.charAt(i);
			if (c == 'a') {
				k = 1;
			}
			else if (c == 'd') {
				k = 0;
			}
			else {
				return 0;  // error condition ignored by calling func
			}
			sum = (2 * sum) + k;
		}
		return sum;
	}
	
	public int getNegErrCode(TokenTyp toktyp) {
		return -toktyp.ordinal();
	}
	
	public void putRtnCodeErr(int rtncode) {
		TokenTyp toktyp;
		if (rtncode >= 0) {
			return;
		}
		toktyp = TokenTyp.values[-rtncode];
		out(putErr(toktyp));
		fatalRtnCode = rtncode;
	}

	public String putErr(TokenTyp toktyp) {
		return putColErr(toktyp, 0);
	}

	private String putColErr(TokenTyp toktyp, int colIdx) {
		return putFullErr(toktyp, colIdx, "");
	}

	private String putTokErr(TokenTyp toktyp, String token) {
		return putFullErr(toktyp, 0, token);
	}

	private String putFullErr(TokenTyp toktyp, int colIdx, String token) {
		String outbuf, msg;
		String colStr = "";
		char sp = ' ';

		if (isClean) {
			isClean = false;
			dirtyLine = lineCount;
			dirtyCol = colCount;
		}
		if (colIdx > 0) {
			colStr = " at col. " + colIdx;
		}
		else if (token.length() > 0) {
			colStr = ", token: " + token;
		}
		msg = getTokErrStr(toktyp);
		outbuf = TABSTR + "ERR" + sp + toktyp + sp + msg + colStr;
		out("");
		outDetl(outbuf);
		return outbuf;
	}

	private int putPar(int typ) {
		char ch = ' ';
		char sp = ' ';
		String outbuf;
		int rtnval = 0;

		switch (typ) {
		case 1:
			ch = '(';
			//rtnval = addNode(NodeCellTyp.PAREN, 0, 0.0, "");
			rtnval = doAddParen();
			break;
		case 2:
			ch = ')';
			rtnval = closeParen();
			break;
		case 3:
			ch = ';';
			//rtnval = addNode(NodeCellTyp.SEMICLN, 0, 0.0, "");
			rtnval = doAddSemicolon();
			break;
		}
		outbuf = TABSTR + "PAR" + sp + ch;
		outDetl(outbuf);
		return rtnval;
	}

	private void putCmt(int typ) {
		char ch = ' ';
		char sp = ' ';
		String outbuf;

		switch (typ) {
		case 1:
			ch = '{';
			break;
		case 2:
			ch = '}';
			break;
		case 3:
			ch = '#';
			break;
		}
		outbuf = TABSTR + "CMT" + sp + ch;
		outDetl(outbuf);
	}

	public String getOpStr(KeywordTyp kwtyp) {
		switch (kwtyp) {
		case DOT: return ":";
		case SET: return "=";
		case ADDSET: return "+=";
		case MINUSSET: return "-=";
		case MPYSET: return "*=";
		case DIVSET: return "/=";
		case IDIVSET: return "//=";
		case MODSET: return "%=";
		case SHLSET: return "<<=";
		case SHRSET: return ">>=";
		case SHRUSET: return ">>>=";
		case ANDBSET: return "&=";
		case XORBSET: return "^=";
		case ORBSET: return "|=";
		case ANDSET: return "&&=";
		case XORSET: return "^^=";
		case ORSET: return "||=";
		case QUEST: return "?";
		case MINUS: return "-";
		case NOTBITZ: return "~";
		case NOT: return "!";
		case DIV: return "/";
		case IDIV: return "//";
		case MOD: return "%";
		case MPY: return "*";
		case ADD: return "+";
		case GE: return ">=";
		case LE: return "<=";
		case GT: return ">";
		case LT: return "<";
		case EQ: return "==";
		case NE: return "!=";
		case SHL: return "<<";
		case SHR: return ">>";
		case SHRU: return ">>>";
		case ANDBITZ: return "&";
		case XORBITZ: return "^";
		case ORBITZ: return "|";
		case AND: return "&&";
		case XOR: return "^^";
		case OR: return "||";
		case INCINT: return "++";
		case DECINT: return "--";
		case STRDO: return "%";
		case STRCAT: return "+";
		case STAR: return "*";
		case DBLSTAR: return "**";
		default: return "";
		}
	}

	public KeywordTyp getOpKeyword(String op) {
		int len = op.length();
		switch (len) {
		case 1: return getOpKw1(op);
		case 2: return getOpKw2(op);
		case 3: return getOpKw3(op);
		case 4:
			if (op.equals(">>>=")) {
				return KeywordTyp.SHRUSET;
			}
		}
		return KeywordTyp.NULL;
	}

	private KeywordTyp getOpKw1(String op) {
		char ch = op.charAt(0);
		switch (ch) {
		case ':': return KeywordTyp.DOT;
		case '=': return KeywordTyp.SET;
		case '+': return KeywordTyp.ADD;
		case '-': return KeywordTyp.MINUS;
		case '*': return KeywordTyp.MPY;
		case '/': return KeywordTyp.DIV;
		case '%': return KeywordTyp.MOD;
		case '<': return KeywordTyp.LT;
		case '>': return KeywordTyp.GT;
		case '&': return KeywordTyp.ANDBITZ;
		case '^': return KeywordTyp.XORBITZ;
		case '|': return KeywordTyp.ORBITZ;
		case '?': return KeywordTyp.QUEST;
		case '~': return KeywordTyp.NOTBITZ;
		case '!': return KeywordTyp.NOT;
		}
		return KeywordTyp.NULL;
	}

	private KeywordTyp getOpKw2(String op) {
		char ch = op.charAt(0);
		char ch2 = op.charAt(1);

		switch (ch) {
		case '=': 
			if (ch2 == '=') {
				return KeywordTyp.EQ;
			}
			break;
		case '+': 
			if (ch2 == '=') {
				return KeywordTyp.ADDSET;
			}
			if (ch2 == '+') {
				return KeywordTyp.INCINT;
			}
			break;
		case '-': 
			if (ch2 == '=') {
				return KeywordTyp.MINUSSET;
			}
			if (ch2 == '-') {
				return KeywordTyp.DECINT;
			}
			break;
		case '*': 
			if (ch2 == '=') {
				return KeywordTyp.MPYSET;
			}
			if (ch2 == '*') {
				return KeywordTyp.DBLSTAR;
			}
			break;
		case '/': 
			if (ch2 == '=') {
				return KeywordTyp.DIVSET;
			}
			if (ch2 == '/') {
				return KeywordTyp.IDIV;
			}
			break;
		case '%': 
			if (ch2 == '=') {
				return KeywordTyp.MODSET;
			}
			break;
		case '<': 
			if (ch2 == '=') {
				return KeywordTyp.LE;
			}
			if (ch2 == '<') {
				return KeywordTyp.SHL;
			}
			break;
		case '>':
			if (ch2 == '=') {
				return KeywordTyp.GE;
			}
			if (ch2 == '>') {
				return KeywordTyp.SHR;
			}
			break;
		case '&': 
			if (ch2 == '=') {
				return KeywordTyp.ANDBSET;
			}
			if (ch2 == '&') {
				return KeywordTyp.AND;
			}
			break;
		case '^': 
			if (ch2 == '=') {
				return KeywordTyp.XORBSET;
			}
			if (ch2 == '^') {
				return KeywordTyp.XOR;
			}
			break;
		case '|': 
			if (ch2 == '=') {
				return KeywordTyp.ORBSET;
			}
			if (ch2 == '|') {
				return KeywordTyp.OR;
			}
			break;
		case '!': 
			if (ch2 == '=') {
				return KeywordTyp.NE;
			}
			break;
		}
		return KeywordTyp.NULL;
	}

	private KeywordTyp getOpKw3(String op) {
		char ch = op.charAt(0);
		char ch2 = op.charAt(1);
		char ch3 = op.charAt(2);

		switch (ch) {
		case '>':
			if (ch2 != '>') {
				return KeywordTyp.NULL;
			}
			if (ch3 == '>') {
				return KeywordTyp.SHRU;
			}
			if (ch3 == '=') {
				return KeywordTyp.SHRSET;
			}
		case '/':
			if (ch2 != '/') {
				return KeywordTyp.NULL;
			}
			if (ch3 == '=') {
				return KeywordTyp.IDIVSET;
			}
			break;
		case '<':
			if (ch2 != '<') {
				return KeywordTyp.NULL;
			}
			if (ch3 == '=') {
				return KeywordTyp.SHLSET;
			}
			break;
		case '&':
			if (ch2 != '&') {
				return KeywordTyp.NULL;
			}
			if (ch3 == '=') {
				return KeywordTyp.ANDSET;
			}
			break;
		case '^':
			if (ch2 != '^') {
				return KeywordTyp.NULL;
			}
			if (ch3 == '=') {
				return KeywordTyp.XORSET;
			}
			break;
		case '|':
			if (ch2 != '|') {
				return KeywordTyp.NULL;
			}
			if (ch3 == '=') {
				return KeywordTyp.ORSET;
			}
			break;
		}
		return KeywordTyp.NULL;
	}

	public String getTokErrStr(TokenTyp toktyp) {
		switch (toktyp) {
		case ERRCLOSEBRACE:
			return "Missing open brace";
		case ERRSYM:
			return "Invalid character";
		case ERRCLOSEQUOTE:
			return "Missing close quotation mark";
		case ERRIDENTIFIER:
			return "Invalid identifier";
		case ERRIDOVERSLASH:
			return "Invalid identifier (backslash too many)";
		case ERRSYSFUNC:
			return "Invalid system function";
		case ERROP:
			return "Invalid operator";
		case ERRNUM:
			return "Invalid numeric constant";
		case ERRINCMTEOF:
			return "Missing close brace";
		case ERRSTKOVRFLW:
			return "Stack overflow";
		case ERRSTKUNDFLW:
			return "Stack underflow";
		case ERRFREE:
			return "Free up memory failure";
		case ERRPOSTPARTOK:
			return "Invalid token after open paren";
		case ERRDOMISSINGBLK: 
			return "DO not followed by open paren";
		case ERRPARENRPT: 
			return "Open paren twice in a row";
		case ERRBADPOPBYTE: 
			return "Invalid byte popped";
		case ERRMULTISTRLITBADCHAR:
			return "Invalid non-white space: multiline string literal";
		case ERRSEMICLN:
			return "Semicolon encountered unexpectedly";
		case ERRBADDO:
			return "Internal error: expecting DO on stack";
		case ERRBADZPAREN:
			return "Internal error: expecting ZPAREN/ZSTMT on stack";
		case ERRZKEYWD:
			return "Z-keyword encountered: internal use only";
		default:
			return "";
		}
	}

}
