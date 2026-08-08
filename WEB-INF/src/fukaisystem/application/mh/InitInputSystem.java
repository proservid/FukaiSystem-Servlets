package fukaisystem.application.mh;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletResponse;

import fukaisystem.dto.mh.InitialInputDTO;
import fukaisystem.foundation.ServiceFoundation;

public class InitInputSystem extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		TreeMap<String, String> dept = new TreeMap<String, String>();
		Map<String, String> process = new LinkedHashMap<String, String>();
		Map<List<String>, Map<String, String>> name = new LinkedHashMap<List<String>, Map<String, String>>();
		int period = 0;

		try (
			PreparedStatement ps = c
				.prepareStatement("SELECT RIGHT('00' + CONVERT(varchar, CD), 2) AS 部署CD,部署名 FROM M_部署");
		) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				dept.put(rs.getString("部署CD"), rs.getString("部署名"));
			}
		}

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT CASE WHEN 所属部署CD IN(2,3,4) THEN RIGHT('00' + CONVERT(varchar, CD), 2) ELSE CONVERT(varchar, CD) END AS 個人CD,姓+' '+名 AS 氏名,"
					+ "RIGHT('00' + CONVERT(varchar, 所属部署CD), 2) AS 部署CD"
					+ " FROM M_人員 WHERE 在籍FLG='true' ORDER BY 表示CD"
			);
		) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				List<String> subKey = new ArrayList<String>();
				subKey.add(rs.getString("部署CD"));
				if (!name.containsKey(subKey)) {
					name.put(subKey, new LinkedHashMap<String, String>());
				}
				name.get(subKey).put(rs.getString("個人CD"), rs.getString("氏名"));
			}
		}

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT RIGHT('00' + CONVERT(varchar, CD), 2) AS 小分類CD,小分類名 FROM M_加工_子"
					+ " WHERE 使用FLG='true' AND CD<200 ORDER BY 大分類CD,中分類CD"
			);
		) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				process.put(rs.getString("小分類CD"), rs.getString("小分類名"));
			}
		}

		try (
			PreparedStatement ps = c.prepareStatement("SELECT MAX(期) AS 当期 FROM M_期 WHERE 自 < ?");
		) {
			ps.setDate(1, new java.sql.Date(new java.util.Date().getTime()));
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				period = rs.getInt("当期");
			}

		}

		return new InitialInputDTO(dept, name, process, period);
	}
}
