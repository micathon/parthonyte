import init.InitMain;

public class syva {

	public static void main(String[] args) {
		InitMain initobj = new InitMain();
		String fileName = "";

		if (args.length > 1) {
			System.out.println("Usage: java syva [filename]");
			return;
		}
		if (args.length == 1) {
			fileName = "../dat/" + args[0] + ".syva";
		}
		initobj.runInit(fileName);
	}

}
