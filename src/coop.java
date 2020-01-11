import init.InitMain;

public class coop {

	public static void main(String[] args) {
		InitMain initobj = new InitMain();
		String fileName = "";

		if (args.length > 1) {
			System.out.println("Usage: java coop [filename]");
			return;
		}
		if (args.length == 1) {
			fileName = "../dat/" + args[0] + ".coop";
		}
		initobj.runInit(fileName);
	}

}
