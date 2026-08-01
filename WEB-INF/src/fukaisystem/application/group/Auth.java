package fukaisystem.application.group;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletResponse;

import fukaisystem.dto.InitialScheduleDTO;
import fukaisystem.foundation.ServiceFoundation;

public class Auth extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		TreeMap<String, String> dept = new TreeMap<String, String>();
		Map<String, Map<String, String>> name = new LinkedHashMap<String, Map<String, String>>();

		try (
			PreparedStatement ps = c.prepareStatement(
			"SELECT RIGHT('00' + CONVERT(varchar, CD), 2) AS 部署CD,部署名 FROM M_部署"
			);
		) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				dept.put(rs.getString("部署CD"), rs.getString("部署名"));
				name.put(rs.getString("部署CD"), new LinkedHashMap<String, String>());
			}
		}
		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT CD AS 個人CD,姓+' '+名 AS 氏名,RIGHT('00' + CONVERT(varchar, 所属部署CD), 2) AS 部署CD FROM M_人員 WHERE 在籍FLG='true' AND CD>0 ORDER BY 所属部署CD,表示CD"
			);
		) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (name.containsKey(rs.getString("部署CD")))
					name.get(rs.getString("部署CD")).put(rs.getString("個人CD"), rs.getString("氏名"));
			}
		}
		return new InitialScheduleDTO(dept, name);

	}
}
