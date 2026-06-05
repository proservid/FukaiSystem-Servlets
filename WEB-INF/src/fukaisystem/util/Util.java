package fukaisystem.util;

public class Util {
	/**
	 * 文字列が空データかどうか調べる
	 *
	 * @param s 調べる文字列
	 * @return 文字列が null, 空文字, 空白文字のみの場合は true, それ以外の場合は false
	 */
	public static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}
}
