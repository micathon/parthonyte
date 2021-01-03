package iconst;

public enum NodeCellTyp {
	NULL, BOOLEAN, INT, LONG, DOUBLE, STRING, ID, FUNC,
	PTR, KWD, PAREN, SEMICLN, DO, LOCVAR;
	
	public static final NodeCellTyp values[] = values();
}
