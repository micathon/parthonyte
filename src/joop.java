import init.InitMain;

public class joop {

	public static void main(String[] args) {
		InitMain initobj = new InitMain();
		String fileName = "";
		String filePath = "";
		boolean isUnitTest = false;
		boolean isMain = false;
		boolean isRunTest = false;

		if (args.length == 1) {
			filePath = "../dat/";
			fileName = args[0] + ".jp";
		}
		else if (args.length == 2) {
			isMain = args[0].equals("main");
			if (args[1].equals("-u")) {
				filePath = "../dat/test/";
				fileName = args[0] + ".test";
				isUnitTest = true;
			}
			else if (args[1].equals("-r")) {
				filePath = "../dat/rt/";
				fileName = args[0] + ".test";
				isRunTest = true;
			}
			else {
				System.out.println("Usage: java joop filename [-u][-r]");
				return;
			}
		}
		else if (args.length > 2) {
			System.out.println("Usage: java joop [filename]");
			return;
		}
		initobj.runInit(filePath, fileName, isUnitTest, isMain, isRunTest);
	}

}
