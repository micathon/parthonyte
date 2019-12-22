import init.InitMain;

public class remo {

	public static void main(String[] args) {
		InitMain initobj = new InitMain();
		String fileName = "";

		if (args.length > 1) {
			System.out.println("Usage: java remo [filename]");
			return;
		}
		if (args.length == 1) {
			fileName = "../dat/" + args[0] + ".rlsp";
		}
		initobj.runInit(fileName);
	}

}
