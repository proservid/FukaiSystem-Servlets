package fukaisystem.application.address;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.dto.common.CandidateInputDTO;
import fukaisystem.foundation.ServiceFoundation;

public class GetIndCandidate extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		Vector<Vector<String>> candidate = new Vector<Vector<String>>();
		CandidateInputDTO ciDTO = cast(response, o, CandidateInputDTO.class);
		String input = ciDTO.getInput();
		int key = ciDTO.getKey();
		boolean isValidOnly = ciDTO.isValidOnly();

		StringBuilder sql = new StringBuilder(
			"SELECT i.CD, 部署名, 役職名, 氏名, case when 会社名 is null then '' else '(' + 会社名 + ')' end AS 会社名"
				+ " FROM M_個人 i"
				+ " LEFT OUTER JOIN M_法人 c ON i.法人CD=c.CD"
				+ " WHERE i.CD IS NOT NULL AND (氏名 LIKE ? OR シメイ LIKE ? OR 部署名+役職名 LIKE ?)"
		);
		if (isValidOnly) {
			sql.append(" AND i.有効FLG='true'");
		}
		if (key > 0) {
			sql.append(" AND 法人CD=?");
		}
		sql.append(" ORDER BY i.CD");
		try (
			PreparedStatement ps = c.prepareStatement(sql.toString());
		) {
			int i = 1;
			ps.setString(i++, "%" + input + "%");
			ps.setString(i++, "%" + input + "%");
			ps.setString(i++, "%" + input + "%");
			if (key > 0) {
				ps.setInt(i++, key);
			}
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Vector<String> record = new Vector<String>();
				record.add(rs.getString("CD"));
				record.add(rs.getString("CD"));
				StringBuilder sb = new StringBuilder();
				/*
				 * if(!rs.getString("部署名").equals("")) {
				 * 	sb.append(rs.getString("部署名") + " ");
				 * }
				 * if(!rs.getString("役職名").equals("")) {
				 * 	sb.append(rs.getString("役職名") + " ");
				 * }
				 */
				if (!rs.getString("氏名").equals("")) {
					sb.append(rs.getString("氏名") + " ");
				}
				sb.append(rs.getString("会社名"));
				record.add(sb.toString());
				candidate.add(record);
			}
		}
		return candidate;
	}
}
