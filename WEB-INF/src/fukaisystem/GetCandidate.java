package fukaisystem;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.CandidateInputDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class GetCandidate extends GenericServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	static final Logger lg = Logger.getLogger("A1");
	private static final String className = "GetCandidate\n";

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		String input = "";
		StringBuilder err = new StringBuilder("");
		String key = "";
		boolean isValidOnly = false;

		Vector<Vector<String>> candidate = new Vector<Vector<String>>();
		try {

			/**
			 * クライアントデータ受け取り
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();

			if (obj == null) {
				err.append(className + "readObjectがnullです\n");
				lg.error(className + "readObjectがnullです");
			} else {
				if (obj instanceof CandidateInputDTO) {
					CandidateInputDTO ciDTO = (CandidateInputDTO) obj;
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
				} else {
					err.append(className + "readObjectがString型ではありません\n");
					lg.error(className + "readObjectがString型ではありません");
				}
			}

			try {
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
				ps = c.prepareStatement(sql.toString());
				int i = 1;
				ps.setString(i++, "%" + input + "%");
				ps.setString(i++, "%" + input + "%");
				ps.setString(i++, "%" + input + "%");
				ps.setString(i++, "%" + input + "%");
				ps.setString(i++, "%" + input + "%");
				ps.setString(i++, input + "%");
				ps.setString(i++, input + "%");
				rs = ps.executeQuery();
				while (rs.next()) {
					Vector<String> v = new Vector<String>();
					v.add(rs.getString("ID"));
					v.add(rs.getString(key));
					v.add(rs.getString("社名"));
					candidate.add(v);
				}
			} catch (SQLException ex) {
				err.append(ex.getMessage());
				err.append("ErrorCode：" + ex.getErrorCode());
				err.append("SQLState：" + ex.getSQLState());
				err.append("テーブル「T_テーブル名」の読込に失敗しました\n");
				Logging.logStackTrace(ex, lg, className);
				ex.printStackTrace();
			}

		} catch (Exception ex) {
			Logging.logStackTrace(ex, lg, className);
			ex.printStackTrace();
		}

		/**
		 * クライアントに送信
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(candidate);
			out.writeUTF(err.toString());
			out.flush();
			out.close();
		} catch (Exception ex) {
			Logging.logStackTrace(ex, lg, className);
			ex.printStackTrace();
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
