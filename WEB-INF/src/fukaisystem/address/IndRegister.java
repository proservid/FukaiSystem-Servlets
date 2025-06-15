package fukaisystem.address;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.IndDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class IndRegister extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "IndRegister\n";

	public void service(ServletRequest request, ServletResponse response) {
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean isError = false;
		IndDTO indDTO = null;
		StringBuilder err = new StringBuilder();
		String idStr = "";
		boolean isDel = false;
		String cd = "0";
		String corpCD = "0";

		try {
			/**
			 * クライアントデータ受け取り
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();
			if (obj == null) {
				isError = true;
				err.append(className + "readObjectがnullです\n");
				lg.error(className + "readObjectがnullです");
			} else {
				if (obj instanceof IndDTO) {
					indDTO = (IndDTO) obj;
					corpCD = indDTO.getStr(4);
				} else if (obj instanceof String) {
					idStr = (String) obj;
					isDel = true;
				} else {
					isError = true;
					err.append(className + "readObjectがIndDTO型ではありません\n");
					lg.error(className + "readObjectがIndDTO型ではありません");
				}
			}

			if (corpCD != null) {
				ps = c.prepareStatement("select * from M_法人 where CD=?");
				ps.setString(1, corpCD);
				rs = ps.executeQuery();
				if (!rs.next()) {
					// 個人が所属する会社がM_法人になければ解除する
					corpCD = "0";
				}
			}

			if (isDel) {
				try {
					ps = c.prepareStatement(
						"DELETE FROM M_個人 WHERE CD=?;DELETE FROM M_個人住所 WHERE 個人CD=?"
					);
					ps.setString(1, idStr);
					ps.setString(2, idStr);
					ps.executeUpdate();
				} catch (SQLException ex) {
					isError = true;
					err.append(className + "テーブルの削除に失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}
			} else {
				cd = indDTO.getStr(22);
				try {
					if (cd.equals("0") || cd == null) { // 新規追加
						ps = c.prepareStatement(
							"INSERT INTO M_個人"
								+ " (法人CD, 部署名, 役職名, 氏名, シメイ, 敬称, TEL1, TEL2, TEL3, FAX1, FAX2, FAX3, メール, 備考,"
								+ "住所FLG,有効FLG, 贈答FLG, 喪FLG, 年賀状CD)"
								+ " OUTPUT inserted.CD as newId"
								+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
						);
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
								rs = ps.getResultSet();
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
							ps = c.prepareStatement(
								"INSERT INTO M_個人住所"
									+ " VALUES( ?, ?, ?, ?, ?, ?)"
							);
							int j = 1;
							ps.setString(j++, cd);
							ps.setString(j++, indDTO.getStr(5));
							ps.setString(j++, indDTO.getStr(6));
							ps.setString(j++, indDTO.getStr(7));
							ps.setString(j++, indDTO.getStr(11));
							ps.setString(j++, indDTO.getStr(12));
							ps.executeUpdate();
						}
					} else { // 更新
						ps = c.prepareStatement(
							"UPDATE M_個人 SET"
								+ " 法人CD=?, 部署名=?, 役職名=?, 氏名=?, シメイ=?, 敬称=?,"
								+ " TEL1=?, TEL2=?, TEL3=?, FAX1=?, FAX2=?, FAX3=?, メール=?, 備考=?,"
								+ " 住所FLG=?, 有効FLG=?, 贈答FLG=?, 喪FLG=?, 年賀状CD=?" // 6
								+ " WHERE CD=?"
						);
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
							ps = c.prepareStatement(
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
							int j = 1;
							ps.setString(j++, indDTO.getStr(22));
							ps.setString(j++, indDTO.getStr(5));
							ps.setString(j++, indDTO.getStr(6));
							ps.setString(j++, indDTO.getStr(7));
							ps.setString(j++, indDTO.getStr(11));
							ps.setString(j++, indDTO.getStr(12));
							ps.setBoolean(j++, indDTO.getBool(5));
							ps.executeUpdate();
						} else {
							// 個人住所なし
							ps = c.prepareStatement("DELETE FROM M_個人住所 WHERE 個人CD=?");
							ps.setString(1, cd);
							ps.executeUpdate();
						}
					}
				} catch (SQLException ex) {
					isError = true;
					Logging.logStackTrace(ex, lg, className);
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			isError = true;
			lg.debug("error");
			lg.error(ex);
		}
		/**
		 * クライアントに送信
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(cd);
			out.writeUTF(err.toString());
			out.flush();
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			Logging.logStackTrace(ex, lg, className);
		} finally {
			try {
				if (c != null && !c.isClosed())
					c.close();
			} catch (SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
			// The following processes requires JDBC4.0.
			try {
				if (ps != null && !ps.isClosed()) {
					ps.close();
					lg.debug(className + "ps is closed by jdbc4.0");
				}
			} catch (SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
			try {
				if (rs != null && !rs.isClosed()) {
					rs.close();
					lg.debug(className + "rs is closed by jdbc4.0");
				}
			} catch (SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
		}
	}

}
