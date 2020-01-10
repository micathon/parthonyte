import init.InitMain;

public class mris {

	public static void main(String[] args) {
		InitMain initobj = new InitMain();
		String fileName = "";

		if (args.length > 1) {
			System.out.println("Usage: java mris [filename]");
			return;
		}
		if (args.length == 1) {
			fileName = "../dat/" + args[0] + ".mris";
		}
		initobj.runInit(fileName);
	}

}
