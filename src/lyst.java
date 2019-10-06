import init.InitMain;

public class lyst {

	public static void main(String[] args) {
		InitMain initobj = new InitMain();
		String fileName = "";

		if (args.length > 1) {
			System.out.println("Usage: java lyst [filename]");
			return;
		}
		if (args.length == 1) {
			fileName = "../dat/" + args[0] + ".lyst";
		}
		initobj.runInit(fileName);
	}

}
