package fukaisystem.application.address;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletResponse;

import fukaisystem.dto.address.IndDTO;
import fukaisystem.foundation.ServiceFoundation;

public class IndRegister extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		IndDTO indDTO = null;
		String idStr = "";
		boolean isDel = false;
		String cd = "0";
		String corpCD = "0";

		if (o instanceof IndDTO) {
			indDTO = cast(response, o, IndDTO.class);
			corpCD = indDTO.getStr(4);
		} else {
			idStr = cast(response, o, String.class);
			isDel = true;
		}

		if (corpCD != null) {
			try (PreparedStatement ps = c.prepareStatement("select * from M_法人 where CD=?");) {
				ps.setString(1, corpCD);
				ResultSet rs = ps.executeQuery();
				if (!rs.next()) {
					// 個人が所属する会社がM_法人になければ解除する
					corpCD = "0";
				}
			}
		}

		if (isDel) {
			try (
				PreparedStatement ps = c
					.prepareStatement("DELETE FROM M_個人 WHERE CD=?;DELETE FROM M_個人住所 WHERE 個人CD=?");
			) {
				ps.setString(1, idStr);
				ps.setString(2, idStr);
				ps.executeUpdate();
			}
			return cd;
		}

		cd = indDTO.getStr(22);
		// 新規追加
		if (cd.equals("0") || cd == null) {
			try (
				PreparedStatement ps = c.prepareStatement(
					"INSERT INTO M_個人"
						+ " (法人CD, 部署名, 役職名, 氏名, シメイ, 敬称, TEL1, TEL2, TEL3, FAX1, FAX2, FAX3, メール, 備考,"
						+ "住所FLG,有効FLG, 贈答FLG, 喪FLG, 年賀状CD)"
						+ " OUTPUT inserted.CD as newId"
						+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
				);
			) {
				int i = 1;
				ps.setString(i++, corpCD);
				ps.setString(i++, indDTO.getStr(2));
				ps.setString(i++, indDTO.getStr(3));
				ps.setString(i++, indDTO.getStr(0));
				ps.setString(i++, indDTO.getStr(1));
				ps.setInt(i++, indDTO.getInt(0));
				ps.setString(i++, indDTO.getStr(13));
				ps.setString(i++, indDTO.getStr(14));
				ps.setString(i++, indDTO.getStr(15));
				ps.setString(i++, indDTO.getStr(16));
				ps.setString(i++, indDTO.getStr(17));
				ps.setString(i++, indDTO.getStr(18));
				ps.setString(i++, indDTO.getStr(19));
				ps.setString(i++, indDTO.getStr(21));
				ps.setBoolean(i++, indDTO.getBool(3));
				ps.setBoolean(i++, indDTO.getBool(0));
				ps.setBoolean(i++, indDTO.getBool(1));
				ps.setBoolean(i++, indDTO.getBool(2));
				ps.setInt(i, indDTO.getInt(1));
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
				if (indDTO.getBool(3)) {
					try (
						PreparedStatement ps2 = c.prepareStatement(
							"INSERT INTO M_個人住所"
								+ " VALUES( ?, ?, ?, ?, ?, ?)"
						);
					) {
						int j = 1;
						ps2.setString(j++, cd);
						ps2.setString(j++, indDTO.getStr(5));
						ps2.setString(j++, indDTO.getStr(6));
						ps2.setString(j++, indDTO.getStr(7));
						ps2.setString(j++, indDTO.getStr(11));
						ps2.setString(j++, indDTO.getStr(12));
						ps2.executeUpdate();
					}
				}
			}
			return cd;
		}

		// 更新
		try (
			PreparedStatement ps = c.prepareStatement(
				"UPDATE M_個人 SET"
					+ " 法人CD=?, 部署名=?, 役職名=?, 氏名=?, シメイ=?, 敬称=?,"
					+ " TEL1=?, TEL2=?, TEL3=?, FAX1=?, FAX2=?, FAX3=?, メール=?, 備考=?,"
					+ " 住所FLG=?, 有効FLG=?, 贈答FLG=?, 喪FLG=?, 年賀状CD=?" // 6
					+ " WHERE CD=?"
			);
		) {
			int i = 1;
			ps.setString(i++, corpCD);
			ps.setString(i++, indDTO.getStr(2));
			ps.setString(i++, indDTO.getStr(3));
			ps.setString(i++, indDTO.getStr(0));
			ps.setString(i++, indDTO.getStr(1));
			ps.setInt(i++, indDTO.getInt(0));
			ps.setString(i++, indDTO.getStr(13));
			ps.setString(i++, indDTO.getStr(14));
			ps.setString(i++, indDTO.getStr(15));
			ps.setString(i++, indDTO.getStr(16));
			ps.setString(i++, indDTO.getStr(17));
			ps.setString(i++, indDTO.getStr(18));
			ps.setString(i++, indDTO.getStr(19));
			ps.setString(i++, indDTO.getStr(21));
			ps.setBoolean(i++, indDTO.getBool(3));
			ps.setBoolean(i++, indDTO.getBool(0));
			ps.setBoolean(i++, indDTO.getBool(1));
			ps.setBoolean(i++, indDTO.getBool(2));
			ps.setInt(i++, indDTO.getInt(1));
			ps.setString(i, indDTO.getStr(22));
			ps.executeUpdate();
			if (indDTO.getBool(3)) {
				// 個人住所あり
				try (
					PreparedStatement ps2 = c.prepareStatement(
						"MERGE INTO M_個人住所 AS addr"
							+ " USING (SELECT ? AS 個人CD, ? AS alpha_2, ? AS 郵便番号, ? AS 郵便枝番," // 7
							+ " ? AS 番地, ? AS 建物等, ? AS 自宅FLG) AS w"
							+ " ON addr.個人CD=w.個人CD"
							+ " WHEN MATCHED THEN"
							+ "   UPDATE SET addr.alpha_2=w.alpha_2, addr.郵便番号=w.郵便番号, addr.郵便枝番=w.郵便枝番,"
							+ " addr.番地=w.番地, addr.建物等=w.建物等, addr.自宅FLG=w.自宅FLG"
							+ " WHEN NOT MATCHED THEN"
							+ "   INSERT VALUES(w.個人CD, w.alpha_2, w.郵便番号, w.郵便枝番,"
							+ " w.番地, w.建物等, w.自宅FLG);"
					);
				) {
					int j = 1;
					ps2.setString(j++, indDTO.getStr(22));
					ps2.setString(j++, indDTO.getStr(5));
					ps2.setString(j++, indDTO.getStr(6));
					ps2.setString(j++, indDTO.getStr(7));
					ps2.setString(j++, indDTO.getStr(11));
					ps2.setString(j++, indDTO.getStr(12));
					ps2.setBoolean(j++, indDTO.getBool(5));
					ps2.executeUpdate();
				}
			} else {
				// 個人住所なし
				try (PreparedStatement ps2 = c.prepareStatement("DELETE FROM M_個人住所 WHERE 個人CD=?");) {
					ps2.setString(1, cd);
					ps2.executeUpdate();
				}
			}
		}
		return cd;
	}

}
