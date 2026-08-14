package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletResponse;

import fukaisystem.dto.business.ChartDTO;
import fukaisystem.dto.business.ProductNumber;
import fukaisystem.foundation.ServiceFoundation;

/**
 * カルテ用データを取得する
 */
public class GetChart extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		String name = "";
		String display = "";

		Map<Integer, String> contacts = new LinkedHashMap<Integer, String>();
		Map<Integer, String> models = new LinkedHashMap<Integer, String>();
		Map<Integer, ProductNumber> numbers = new HashMap<Integer, ProductNumber>();

		Integer code = cast(response, o, Integer.class);

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT"
					+ " CASE"
					+ " WHEN 種別CD = 1 THEN '㈱'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
					+ " WHEN 種別CD = 2 THEN 会社名+'㈱' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
					+ " WHEN 種別CD = 3 THEN '㈲'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
					+ " WHEN 種別CD = 4 THEN 会社名+'㈲' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
					+ " ELSE 会社名 END,"
					+ " 表示名"
					+ " FROM  M_法人 c"
					+ " WHERE 得意先CD=?"
			);
		) {
			ps.setInt(1, code);
			try (ResultSet rs = ps.executeQuery();) {
				if (rs.next()) {
					name = rs.getString(1);
					display = rs.getString(2);
				}
			}
		}
		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT 機械番号,案件名,製作期,製作番号,製作枝番 FROM T_製作_親"
					+ " WHERE 得意先CD=? AND 機械番号>0 ORDER BY 製作期,製作番号"
			);
		) {
			ps.setInt(1, code);
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					models.put(rs.getInt("機械番号"), rs.getString("案件名"));
					numbers.put(
						rs.getInt("機械番号"),
						new ProductNumber(rs.getInt("製作期"), rs.getInt("製作番号"), rs.getString("製作枝番"))
					);
				}
			}
		}
		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT p.CD,氏名 FROM M_個人 p"
					+ " LEFT OUTER JOIN M_法人 c"
					+ " ON p.法人CD=c.CD"
					+ " WHERE 得意先CD=? ORDER BY p.CD"
			);
		) {
			ps.setInt(1, code);
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					contacts.put(rs.getInt("CD"), rs.getString("氏名"));
				}
			}
		}
		return new ChartDTO(name, display, contacts, models, numbers);
	}
}
