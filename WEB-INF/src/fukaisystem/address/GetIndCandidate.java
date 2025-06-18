package fukaisystem.address;

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

public class GetIndCandidate extends GenericServlet {

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
		int key = 0;
		StringBuilder err = new StringBuilder("");
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
					key = ciDTO.getKey();
					isValidOnly = ciDTO.isValidOnly();
				} else {
					err.append(className + "readObjectがString型ではありません\n");
					lg.error(className + "readObjectがString型ではありません");
				}
			}

			try {
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
				ps = c.prepareStatement(sql.toString());
				int i = 1;
				ps.setString(i++, "%" + input + "%");
				ps.setString(i++, "%" + input + "%");
				ps.setString(i++, "%" + input + "%");
				if (key > 0) {
					ps.setInt(i++, key);
				}
				rs = ps.executeQuery();
				while (rs.next()) {
					Vector<String> record = new Vector<String>();
					record.add(rs.getString("CD"));
					record.add(rs.getString("CD"));
					StringBuilder sb = new StringBuilder();
					/*
					if(!rs.getString("部署名").equals("")) {
						sb.append(rs.getString("部署名") + " ");
					}
					if(!rs.getString("役職名").equals("")) {
						sb.append(rs.getString("役職名") + " ");
					}
					 */
					if (!rs.getString("氏名").equals("")) {
						sb.append(rs.getString("氏名") + " ");
					}
					sb.append(rs.getString("会社名"));
					record.add(sb.toString());
					candidate.add(record);
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
