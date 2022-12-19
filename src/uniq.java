import init.InitMain;

public class uniq {

	public static void main(String[] args) {
		InitMain initobj = new InitMain();
		String fileName = "";
		boolean isUnitTest = false;
		boolean isMain = false;
		boolean isRunTest = false;

		if (args.length > 2) {
			System.out.println("Usage: java uniq [filename]");
			return;
		}
		if (args.length == 1) {
			fileName = "../dat/" + args[0] + ".uniq";
		}
		else if (args.length == 2) {
			isMain = args[0].equals("main");
			if (args[1].equals("-u")) {
				fileName = "../dat/test/" + args[0] + ".test";
				isUnitTest = true;
			}
			else if (args[1].equals("-r")) {
				fileName = "../dat/rt/" + args[0] + ".test";
				isRunTest = true;
			}
			else {
				System.out.println("Usage: java uniq filename [-u][-r]");
				return;
			}
		}
		initobj.runInit(fileName, isUnitTest, isMain, isRunTest);
	}

}
