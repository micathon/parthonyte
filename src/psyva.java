import init.InitMain;

public class psyva {

	public static void main(String[] args) {
		InitMain initobj = new InitMain();
		String fileName = "";

		if (args.length > 1) {
			System.out.println("Usage: java psyva [filename]");
			return;
		}
		if (args.length == 1) {
			fileName = "../dat/" + args[0] + ".psyva";
		}
		initobj.runInit(fileName);
	}

}
