package fukaisystem.application.address;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;

import javax.servlet.ServletResponse;

import fukaisystem.dto.CorpDTO;
import fukaisystem.foundation.ServiceFoundation;

public class CorpRegister extends ServiceFoundation {

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws Exception {

		CorpDTO corpDTO = null;
		String idStr = "";
		boolean isDel = false;
		String cd = "0";

		if (o instanceof CorpDTO) {
			corpDTO = (CorpDTO)o;
		} else if (o instanceof String) {
			idStr = (String)o;
			isDel = true;
		} else {
			throw new Exception("invalid object");
		}

		if (isDel) {
			try (
				PreparedStatement ps = c.prepareStatement(
					"DELETE FROM M_法人 WHERE CD=?"
				);
			) {
				ps.setString(1, idStr);
				ps.executeUpdate();
				// 削除した法人については、個人の関連付けを（削除された法人）に変更する
				try (
					PreparedStatement ips = c.prepareStatement(
						"UPDATE M_個人 SET 法人CD=1 WHERE 法人CD=?"
					);
				) {
					ips.setString(1, idStr);
					ips.executeUpdate();
				}
			}
		} else {
			cd = corpDTO.getStr(22);
			if (cd.equals("0") || cd == null) { // 新規追加
				try (
					PreparedStatement ps = c.prepareStatement(
						"INSERT INTO M_法人"
							+ " (仕入先CD, 得意先CD, 種別CD, 会社名, カイシャメイ,"
							+ " 支店名, シテンメイ, 表示名, アルファベット, alpha_2, 郵便番号, 郵便枝番," // 7
							+ " 番地, 建物等, TEL1, TEL2, TEL3, FAX1, FAX2, FAX3," // 9
							+ " メール, URL, 備考, 登録番号, 有効FLG, 贈答FLG, 年賀状CD)"
							+ " OUTPUT inserted.CD as newId"
							+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ " ?, ?, ?, ?, ?, ?, ?)"
					);
				) {
					int i = 1;
					if (corpDTO.getInt(0) == 0) {
						ps.setNull(i++, Types.INTEGER);
					} else {
						ps.setInt(i++, corpDTO.getInt(0));
					}
					if (corpDTO.getInt(1) == 0) {
						ps.setNull(i++, Types.INTEGER);
					} else {
						ps.setInt(i++, corpDTO.getInt(1));
					}
					ps.setInt(i++, corpDTO.getInt(2));
					ps.setString(i++, corpDTO.getStr(0));
					ps.setString(i++, corpDTO.getStr(1));
					ps.setString(i++, corpDTO.getStr(2));
					ps.setString(i++, corpDTO.getStr(3));
					ps.setString(i++, corpDTO.getStr(4));
					ps.setString(i++, corpDTO.getStr(23));
					ps.setString(i++, corpDTO.getStr(5));
					ps.setString(i++, corpDTO.getStr(6));
					ps.setString(i++, corpDTO.getStr(7));
					ps.setString(i++, corpDTO.getStr(11));
					ps.setString(i++, corpDTO.getStr(12));
					ps.setString(i++, corpDTO.getStr(13));
					ps.setString(i++, corpDTO.getStr(14));
					ps.setString(i++, corpDTO.getStr(15));
					ps.setString(i++, corpDTO.getStr(16));
					ps.setString(i++, corpDTO.getStr(17));
					ps.setString(i++, corpDTO.getStr(18));
					ps.setString(i++, corpDTO.getStr(19));
					ps.setString(i++, corpDTO.getStr(20));
					ps.setString(i++, corpDTO.getStr(21));
					ps.setString(i++, corpDTO.getStr(24));
					ps.setBoolean(i++, corpDTO.getBool(0));
					ps.setBoolean(i++, corpDTO.getBool(1));
					// ps.setString(i, corpDTO.getStr(22));
					ps.setInt(i, corpDTO.getInt(3));
					boolean isResultSet = ps.execute();
					int updateCount = 0;
					while (true) {
						if (isResultSet) {
							ResultSet rs = ps.getResultSet();
							while (rs.next()) {
								cd = rs.getString(1);
							}
							rs.close();
						} else {
							updateCount = ps.getUpdateCount();
							if (updateCount == -1) {
								break;
							}
						}
						isResultSet = ps.getMoreResults();
					}
				}
			} else { // 更新
				try (
					PreparedStatement ps = c.prepareStatement(
						"UPDATE M_法人 SET"
							+ " 仕入先CD=?, 得意先CD=?, 種別CD=?, 会社名=?, カイシャメイ=?,"
							+ " 支店名=?, シテンメイ=?, 表示名=?, アルファベット=?, alpha_2=?, 郵便番号=?, 郵便枝番=?," // 7
							+ " 番地=?, 建物等=?, TEL1=?, TEL2=?, TEL3=?, FAX1=?, FAX2=?, FAX3=?," // 9
							+ " メール=?, URL=?, 備考=?, 登録番号=?, 有効FLG=?, 贈答FLG=?, 年賀状CD=?" // 6
							+ " WHERE CD=?"
					);
				) {
					int i = 1;
					if (corpDTO.getInt(0) == 0) {
						ps.setNull(i++, Types.INTEGER);
					} else {
						ps.setInt(i++, corpDTO.getInt(0));
					}
					if (corpDTO.getInt(1) == 0) {
						ps.setNull(i++, Types.INTEGER);
					} else {
						ps.setInt(i++, corpDTO.getInt(1));
					}
					ps.setInt(i++, corpDTO.getInt(2));
					ps.setString(i++, corpDTO.getStr(0));
					ps.setString(i++, corpDTO.getStr(1));
					ps.setString(i++, corpDTO.getStr(2));
					ps.setString(i++, corpDTO.getStr(3));
					ps.setString(i++, corpDTO.getStr(4));
					ps.setString(i++, corpDTO.getStr(23));
					ps.setString(i++, corpDTO.getStr(5));
					ps.setString(i++, corpDTO.getStr(6));
					ps.setString(i++, corpDTO.getStr(7));
					ps.setString(i++, corpDTO.getStr(11));
					ps.setString(i++, corpDTO.getStr(12));
					ps.setString(i++, corpDTO.getStr(13));
					ps.setString(i++, corpDTO.getStr(14));
					ps.setString(i++, corpDTO.getStr(15));
					ps.setString(i++, corpDTO.getStr(16));
					ps.setString(i++, corpDTO.getStr(17));
					ps.setString(i++, corpDTO.getStr(18));
					ps.setString(i++, corpDTO.getStr(19));
					ps.setString(i++, corpDTO.getStr(20));
					ps.setString(i++, corpDTO.getStr(21));
					ps.setString(i++, corpDTO.getStr(24));
					ps.setBoolean(i++, corpDTO.getBool(0));
					ps.setBoolean(i++, corpDTO.getBool(1));
					ps.setInt(i++, corpDTO.getInt(3));
					ps.setString(i, corpDTO.getStr(22));
					ps.executeUpdate();
				}
			}
		}
		return cd;
	}

}
