package fukaisystem.application.attendance;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.dto.attendance.DataVectorDTO;
import fukaisystem.dto.attendance.FilterDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 個人の集計データをを返す
 */
public class ReadPersonal extends ServiceFoundation {

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		FilterDTO dto = cast(response, o, FilterDTO.class);
		if (dto == null) {
			return null;
		}
		String num = "";
		String group = "";
		String having = "";
		if (dto.getMiddle() == null) {
			num = "MONTH(年月日)";
			group = "YEAR(年月日), MONTH(年月日)";
			having = "YEAR(年月日)=? AND ? IS NULL";
		} else {
			num = "DAY(年月日)";
			group = "YEAR(年月日), MONTH(年月日), DAY(年月日)";
			having = "YEAR(年月日)=? AND MONTH(年月日)=?";
		}

		Vector<Vector<Object>> dataVector = new Vector<>();
		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT " + num + " AS 番号,"
					+ " SUM(実労働時間) AS 実労働,"
					+ " SUM(CASE WHEN 土休FLG='true' THEN 実労働時間 ELSE 0 END) AS 土休実労働,"
					+ " SUM(CASE WHEN 日曜FLG='true' THEN 実労働時間 ELSE 0 END) AS 日曜実労働,"
					+ " SUM(時間外労働) AS 残業,"
					+ " SUM(CASE WHEN 土休FLG='true' THEN 時間外労働 ELSE 0 END) AS 土休残業,"
					+ " SUM(CASE WHEN 日曜FLG='true' THEN 時間外労働 ELSE 0 END) AS 日曜残業,"
					+ " SUM(深夜労働) AS 深夜,"
					+ " SUM(CASE WHEN 土休FLG='true' THEN 深夜労働 ELSE 0 END) AS 土休深夜,"
					+ " SUM(CASE WHEN 日曜FLG='true' THEN 深夜労働 ELSE 0 END) AS 日曜深夜,"
					+ " SUM(CASE WHEN 欠勤FLG='false' AND 有給FLG='false' THEN 1 ELSE 0 END) AS 出勤,"
					+ " SUM(CASE WHEN 土休FLG='true' THEN 1 ELSE 0 END) AS 土休,"
					+ " SUM(CASE WHEN 日曜FLG='true' THEN 1 ELSE 0 END) AS 日曜,"
					+ " SUM(CASE WHEN 出張FLG='true' THEN 1 ELSE 0 END) AS 出張,"
					+ " SUM(CASE WHEN 遅早FLG='true' THEN 1 ELSE 0 END) AS 遅早,"
					+ " SUM(CASE WHEN 欠勤FLG='true' THEN 1 ELSE 0 END) AS 欠勤,"
					+ " SUM(CASE WHEN 有給FLG='true' THEN 1 ELSE 0 END) AS 有給"
					+ " FROM T_日次集計 GROUP BY " + group + ", 人員CD HAVING " + having + " AND 人員CD=?"
			);
		) {
			ps.setInt(1, dto.getCoarse());
			ps.setObject(2, dto.getMiddle(), Types.INTEGER); // null がありうる
			ps.setInt(3, dto.getFine());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Vector<Object> v = new Vector<>();
				v.add(rs.getInt("番号"));
				v.add(rs.getInt("実労働"));
				v.add(rs.getInt("土休実労働"));
				v.add(rs.getInt("日曜実労働"));
				v.add(rs.getInt("残業"));
				v.add(rs.getInt("土休残業"));
				v.add(rs.getInt("日曜残業"));
				v.add(rs.getInt("深夜"));
				v.add(rs.getInt("土休深夜"));
				v.add(rs.getInt("日曜深夜"));
				v.add(rs.getInt("出勤"));
				v.add(rs.getInt("土休"));
				v.add(rs.getInt("日曜"));
				v.add(rs.getInt("出張"));
				v.add(rs.getInt("遅早"));
				v.add(rs.getInt("欠勤"));
				v.add(rs.getInt("有給"));
				dataVector.add(v);
			}
		}
		return new DataVectorDTO(dataVector);
	}
}
