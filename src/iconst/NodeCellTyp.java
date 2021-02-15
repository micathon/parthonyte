package iconst;

public enum NodeCellTyp {
	NULL, BOOLEAN, INT, LONG, DOUBLE, STRING, ID, FUNC,
	PTR, KWD, LOCVAR, FLDVAR;
	
	public static final NodeCellTyp values[] = values();
}
