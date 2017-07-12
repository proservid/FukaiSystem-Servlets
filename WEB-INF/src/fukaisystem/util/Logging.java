package fukaisystem.util;

import org.apache.log4j.Logger;

public class Logging {
	public static void logStackTrace(Exception ex, Logger lg, String className) {
		ex.printStackTrace();
		StackTraceElement[] element = ex.getStackTrace();
		StringBuilder sb = new StringBuilder(className);
		sb.append(ex.toString() + "\n");
		for(int i = 0; i < element.length; i++) {
			sb.append(element[i].toString() + "\n");
		}
		lg.error(sb.toString());
	}
}
