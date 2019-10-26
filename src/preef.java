import init.InitMain;

public class preef {

	public static void main(String[] args) {
		InitMain initobj = new InitMain();
		String fileName = "";

		if (args.length > 1) {
			System.out.println("Usage: java preef [filename]");
			return;
		}
		if (args.length == 1) {
			fileName = "../dat/" + args[0] + ".pftx";
		}
		initobj.runInit(fileName);
	}

}
