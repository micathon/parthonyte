import init.InitMain;

public class jte {

	public static void main(String[] args) {
		InitMain initobj = new InitMain();
		String fileName = "";

		if (args.length > 1) {
			System.out.println("Usage: java jte [filename]");
			return;
		}
		if (args.length == 1) {
			fileName = "../dat/" + args[0] + ".jte";
		}
		initobj.runInit(fileName);
	}

}
