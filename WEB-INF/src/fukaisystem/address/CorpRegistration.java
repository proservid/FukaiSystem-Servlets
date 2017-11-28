package fukaisystem.address;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;



import org.apache.log4j.Logger;

import fukaisystem.dto.CorpDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;


public class CorpRegistration extends GenericServlet {


	private static final long serialVersionUID = 1L;
    private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "CorpRegistration\n";

	public void service(ServletRequest request, ServletResponse response) {
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean isError = false;
		CorpDTO corpDTO = null;
		StringBuilder err = new StringBuilder();
		String idStr = "";
		boolean isDel = false;
		String cd = "0";


		try {
			/**
			 * クライアントデータ受け取り
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();
			if(obj == null) {
				isError = true;
				err.append(className + "readObjectがnullです\n");
				lg.error(className + "readObjectがnullです");
			} else {
				if(obj instanceof CorpDTO) {
					corpDTO = (CorpDTO)obj;
				} else if(obj instanceof String) {
					idStr = (String)obj;
					isDel = true;
				} else {
					isError = true;
					err.append(className + "readObjectがCorpDTO型ではありません\n");
					lg.error(className + "readObjectがCorpDTO型ではありません");
				}
			}

			if(isDel) {
				try {
					ps = c.prepareStatement(
							"DELETE FROM M_法人 WHERE CD=?");
					ps.setString(1, idStr);
					ps.executeUpdate();
					//削除した法人については、個人の関連付けを（削除された法人）に変更する
					ps = c.prepareStatement(
							"UPDATE M_個人 SET 法人CD=1 WHERE 法人CD=?");
					ps.setString(1, idStr);
					ps.executeUpdate();
				} catch(SQLException ex) {
					isError = true;
					err.append(className + "テーブルの削除に失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}
			} else {
				cd = corpDTO.getStr(22);
				try {
					if(cd.equals("0") || cd == null) {//新規追加
						ps = c.prepareStatement("INSERT INTO M_法人" +
							" (仕入先CD, 得意先CD, 種別CD, 会社名, カイシャメイ," +
							" 支店名, シテンメイ, 表示名, アルファベット, alpha_2, 郵便番号, 郵便枝番," +//7
							" 番地, 建物等, TEL1, TEL2, TEL3, FAX1, FAX2, FAX3," +//9
							" メール, URL, 備考, 有効FLG, 贈答FLG, 年賀状CD)" +
							" OUTPUT inserted.CD as newId" +
							" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
									" ?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
									" ?, ?, ?, ?, ?, ?)");
						int i = 1;
						if(corpDTO.getInt(0) == 0) {
							ps.setNull(i++, Types.INTEGER);
						} else {
							ps.setInt(i++, corpDTO.getInt(0));
						}
						if(corpDTO.getInt(1) == 0) {
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
						ps.setBoolean(i++, corpDTO.getBool(0));
						ps.setBoolean(i++, corpDTO.getBool(1));
						//ps.setString(i, corpDTO.getStr(22));
						ps.setInt(i, corpDTO.getInt(3));
						boolean isResultSet = ps.execute();
						int   updateCount = 0;
						while (true) {
						   if (isResultSet) {
						         rs = ps.getResultSet();
						         while (rs.next()) {
						        	cd = rs.getString(1);
						         }
						         rs.close();
						   }
						   else {
						         updateCount = ps.getUpdateCount();
						         if (updateCount == -1) {
						            break;
						         }
						   }
						   isResultSet = ps.getMoreResults();
						}
					} else {//更新
						ps = c.prepareStatement("UPDATE M_法人 SET" +
							" 仕入先CD=?, 得意先CD=?, 種別CD=?, 会社名=?, カイシャメイ=?," +
							" 支店名=?, シテンメイ=?, 表示名=?, アルファベット=?, alpha_2=?, 郵便番号=?, 郵便枝番=?," +//7
							" 番地=?, 建物等=?, TEL1=?, TEL2=?, TEL3=?, FAX1=?, FAX2=?, FAX3=?," +//9
							" メール=?, URL=?, 備考=?, 有効FLG=?, 贈答FLG=?, 年賀状CD=?" +//6
							" WHERE CD=?");
						int i = 1;
						if(corpDTO.getInt(0) == 0) {
							ps.setNull(i++, Types.INTEGER);
						} else {
							ps.setInt(i++, corpDTO.getInt(0));
						}
						if(corpDTO.getInt(1) == 0) {
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
						ps.setBoolean(i++, corpDTO.getBool(0));
						ps.setBoolean(i++, corpDTO.getBool(1));
						ps.setInt(i++, corpDTO.getInt(3));
						ps.setString(i, corpDTO.getStr(22));
						ps.executeUpdate();
					}
				} catch(SQLException ex) {
					isError = true;
					Logging.logStackTrace(ex, lg, className);
				}
			}

		} catch(Exception ex) {
			ex.printStackTrace();
			isError = true;lg.debug("error");
			Logging.logStackTrace(ex, lg, className);
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
		} catch(Exception ex) {
			ex.printStackTrace();
			Logging.logStackTrace(ex, lg, className);
		} finally {
			try {
				if(c != null && !c.isClosed()) c.close();
			} catch(SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
// The following processes requires JDBC4.0.
			try {
				if(ps != null && !ps.isClosed()) {
					ps.close();
					lg.debug(className + "ps is closed by jdbc4.0");
				}
			} catch(SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
			try {
				if(rs != null && !rs.isClosed()) {
					rs.close();
					lg.debug(className + "rs is closed by jdbc4.0");
				}
			} catch(SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
		}
	}

}
