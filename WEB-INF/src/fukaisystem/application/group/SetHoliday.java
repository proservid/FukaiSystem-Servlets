package fukaisystem.application.group;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class SetHoliday extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "SetHoliday\n";
	private Date holiday;

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuilder err = new StringBuilder();

		/**
		 * クライアントデータ受け取り
		 */
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();
			if (obj == null) {
				err.append(className + "readObjectがnullです\n");
				lg.error(className + "readObjectがnullです");
			} else {
				if (obj instanceof Date) {
					holiday = (Date) obj;
				} else {
					err.append("型が一致しません\n");
					lg.error(className + "型が一致しません");
				}
			}

			try {
				ps = c.prepareStatement("DELETE FROM T_祝日 WHERE 祝日=?");
				ps.setDate(1, holiday);
				if (ps.executeUpdate() == 0) { // 削除できなければ登録されていないので登録
					ps = c.prepareStatement("INSERT INTO T_祝日 VALUES (?)");
					ps.setDate(1, holiday);
					ps.executeUpdate();
				}

			} catch (SQLException ex) {
				ex.printStackTrace();
				err.append(className + "更新に失敗しました\n");
				Logging.logStackTrace(ex, lg, className);
			}

		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		/**
		 * クライアントに送信
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());

			out.writeObject(-1); // 製作番号が0なら、該当する製作データが登録されていなくてもOKにする
			out.writeUTF(err.toString());
			out.flush();
			out.close();
		} catch (Exception ex) {
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
