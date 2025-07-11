package fukaisystem.sql;

/**
 * Search, OrderSearch, DispatchingSearch のメソッドを分離
 * 現在使用されている様子はない
 */
public class QueryUtil {
	public static String partialDateStr(String target, String ymd, int begin, int length) {
		switch (begin) {
			case 1:
				return "substring(convert(varchar(" + length + "), " + target + ", 120), " + begin + ", "
					+ length + ")='" + ymd + "'";
			case 6:
				return "substring(convert(varchar(" + (length + 5) + "), " + target + ", 120), " + begin + ", "
					+ length + ")='" + ymd + "'";
			default:
				return "substring(convert(varchar(10), " + target + ", 120), " + begin + ", "
					+ length + ")='" + ymd + "'";
		}
	}
}
