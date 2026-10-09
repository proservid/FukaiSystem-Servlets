package fukaisystem.sql;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Vector;

/**
 * ResultSet をクライアントのテーブル表示用データに変換するクラス
 *
 * @author kameura
 */
public final class ResultSetConverter {

	private ResultSetConverter() {}

	/**
	 * ResultSet のすべてのレコードをテーブル表示用データに変換する
	 * NULL は {@code null} のまま返す
	 * （クライアントの列型判定では読み飛ばされ、文字列の列では "NULL" と表示される）
	 *
	 * @param rs 変換対象の ResultSet
	 *
	 * @return レコードごとの値を保持するテーブル表示用データ
	 * @throws SQLException
	 */
	public static Vector<Vector<Object>> toTable(ResultSet rs) throws SQLException {
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		int[] colTypes = new int[columnCount];
		for (int i = 0; i < columnCount; i++) {
			colTypes[i] = rsmd.getColumnType(i + 1);
		}

		Vector<Vector<Object>> contents = new Vector<Vector<Object>>();
		while (rs.next()) {
			Vector<Object> record = new Vector<Object>(columnCount);
			for (int i = 1; i <= columnCount; i++) {
				record.add(getValue(rs, i, colTypes[i - 1]));
			}
			contents.add(record);
		}
		return contents;
	}

	/**
	 * 列の型に応じた値を取得する
	 *
	 * @param rs      取得元の ResultSet
	 * @param index   列インデックス（1 はじまり）
	 * @param colType 列の型（java.sql.Types）
	 *
	 * @return 列の型に応じた値、NULL の場合は {@code null}
	 * @throws SQLException
	 */
	private static Object getValue(ResultSet rs, int index, int colType) throws SQLException {
		Object element;
		switch (colType) {
			case Types.BIT:
				element = rs.getBoolean(index);
				break;
			case Types.INTEGER:
			case Types.SMALLINT:
			case Types.TINYINT:
			case Types.BIGINT:
				element = rs.getInt(index);
				break;
			case Types.CHAR:
			case Types.VARCHAR:
				element = rs.getString(index);
				break;
			case Types.DATE:
				element = rs.getDate(index);
				break;
			case Types.TIMESTAMP:
				element = rs.getTimestamp(index);
				break;
			default:
				element = rs.getString(index);
				break;
		}
		if (rs.wasNull()) {
			return null;
		}
		if (element instanceof String) {
			return ((String) element).trim();
		}
		return element;
	}
}
