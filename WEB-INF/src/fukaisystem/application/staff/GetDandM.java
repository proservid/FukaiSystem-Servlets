package fukaisystem.application.staff;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.dto.staff.DMDTO;
import fukaisystem.foundation.ServiceFoundation;

public class GetDandM extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		TreeMap<String, String> dept = new TreeMap<String, String>();
		Map<String, Vector<Vector<Object>>> name = new LinkedHashMap<String, Vector<Vector<Object>>>();

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT RIGHT('00' + CONVERT(varchar, CD), 3) AS 部署CD,部署名 FROM M_部署"
			);
		) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				dept.put(rs.getString("部署CD"), rs.getString("部署名"));
				name.put(rs.getString("部署CD"), new Vector<Vector<Object>>());
			}
		}
		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT CD,姓,名 ,RIGHT('00' + CONVERT(varchar, 所属部署CD), 3) AS 部署CD,在籍FLG,"
					+ "(CASE WHEN 担当者CD IS NULL THEN 0 ELSE 1 END) AS 工数FLG"
					+ " FROM M_人員 JIN"
					+ " LEFT OUTER JOIN (SELECT DISTINCT 担当者CD FROM T_加工実績) JIS"
					+ " ON JIN.CD=JIS.担当者CD"
					+ " WHERE CD>0 ORDER BY 部署CD,CD"
			);
		) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Vector<Object> record = new Vector<Object>();
				// record.add(rs.getInt("管理ID"));
				record.add(rs.getInt("CD"));
				record.add(rs.getString("姓"));
				record.add(rs.getString("名"));
				record.add(rs.getBoolean("在籍FLG"));
				record.add(rs.getBoolean("工数FLG"));
				if (name.containsKey(rs.getString("部署CD")))
					name.get(rs.getString("部署CD")).add(record);
			}
		}
		return new DMDTO(dept, name);
	}
}
