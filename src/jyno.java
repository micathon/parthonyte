import init.InitMain;

public class jyno {

	public static void main(String[] args) {
		InitMain initobj = new InitMain();
		String fileName = "";

		if (args.length > 1) {
			System.out.println("Usage: java jyno [filename]");
			return;
		}
		if (args.length == 1) {
			fileName = "../dat/" + args[0] + ".jyn";
		}
		initobj.runInit(fileName);
	}

}
