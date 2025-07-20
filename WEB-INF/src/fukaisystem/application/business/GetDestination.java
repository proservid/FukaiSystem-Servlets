package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

/**
 * 入力された得意先の納入先リストを取得する
 */
public class GetDestination extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		Integer code = cast(response, o, Integer.class); // 正=選択用、負=編集用
		Vector<String> destinationsForList = new Vector<>(); // 選択用
		Vector<Vector<Object>> destinationsForTable = new Vector<>(); // 編集テーブル用
		destinationsForList.add("");

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT 納入先ID,納入先名 FROM M_納入先 WHERE 得意先CD=? ORDER BY 納入先ID"
			);
		) {
			ps.setInt(1, Math.abs(code));
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					if (code > 0) {
						destinationsForList.add(rs.getString("納入先名"));
						continue;
					}
					Vector<Object> v = new Vector<>();
					v.add(rs.getInt("納入先ID"));
					v.add(rs.getString("納入先名"));
					destinationsForTable.add(v);
				}
			}
		}
		return code > 0 ? destinationsForList : destinationsForTable;

	}
}
