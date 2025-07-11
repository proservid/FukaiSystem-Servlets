package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.dto.CandidateInputDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 入力された情報に合致する取引先の候補を取得する
 */
public class GetCandidate extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		String input = "";
		String key = "";
		boolean isValidOnly = false;

		Vector<Vector<String>> candidate = new Vector<Vector<String>>();
		CandidateInputDTO ciDTO = cast(response, o, CandidateInputDTO.class);

		input = ciDTO.getInput();
		switch (ciDTO.getKey()) {
			case 0:
				key = "CD";
				break;
			case 1:
				key = "得意先CD";
				break;
			case 2:
				key = "仕入先CD";
				break;
		}
		isValidOnly = ciDTO.isValidOnly();

		StringBuilder sql = new StringBuilder(
			"SELECT CD AS ID," + key + ","
				+ "CASE"
				+ " WHEN 種別CD = 1 THEN '㈱'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
				+ " WHEN 種別CD = 2 THEN 会社名+'㈱' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
				+ " WHEN 種別CD = 3 THEN '㈲'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
				+ " WHEN 種別CD = 4 THEN 会社名+'㈲' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
				+ " ELSE 会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
				+ " END AS 社名"
				+ " FROM M_法人"
				+ " WHERE " + key + "IS NOT NULL"
				+ " AND (会社名 LIKE ? OR 支店名 LIKE ? OR カイシャメイ LIKE ? OR シテンメイ LIKE ? OR アルファベット LIKE ? OR 仕入先CD LIKE ? OR 得意先CD LIKE ?)"
		);
		if (isValidOnly) {
			sql.append(" AND 有効FLG='true'");
		}
		sql.append(" ORDER BY " + key);
		try (PreparedStatement ps = c.prepareStatement(sql.toString());) {
			int i = 1;
			ps.setString(i++, "%" + input + "%");
			ps.setString(i++, "%" + input + "%");
			ps.setString(i++, "%" + input + "%");
			ps.setString(i++, "%" + input + "%");
			ps.setString(i++, "%" + input + "%");
			ps.setString(i++, input + "%");
			ps.setString(i++, input + "%");
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					Vector<String> record = new Vector<String>();
					record.add(rs.getString("ID"));
					record.add(rs.getString(key));
					record.add(rs.getString("社名"));
					candidate.add(record);
				}
			}
		}
		return candidate;

	}
}
