import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Utility {
	public static String getNow() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ").format(new Date());
	}

//	public static void writeLog(String log) {
//		File file = new File("/root");
//		String br;
//		if (file.exists()) {
//			file = new File("/root/server.log");
//			br = "\n";
//		} else {
//			file = new File("D:\\server.log");
//			br = "\r\n";
//		}
//		FileWriter fileWriter;
//		try {
//			fileWriter = new FileWriter(file, true);
//			fileWriter.append(getNow() + log + br);
//			fileWriter.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
}
