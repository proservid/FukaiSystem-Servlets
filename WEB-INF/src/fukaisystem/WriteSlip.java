package fukaisystem;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
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

import fukaisystem.dto.SlipDTO;
//import print.dto.SlipDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;



public class WriteSlip extends GenericServlet {

    /**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "WriteSlip\n";

	@Override
	public void service(ServletRequest request, ServletResponse response) {
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean isError = false;
		StringBuilder err = new StringBuilder();

		boolean isOverwrite = false;
		String name = null;
		byte[] byteSlip = null;

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
				if(obj instanceof SlipDTO) {
					isOverwrite = ((SlipDTO)obj).isOverwrite();
					name = ((SlipDTO)obj).getName();
					byteSlip = ((SlipDTO)obj).getByteSlip();
				} else {
					isError = true;
					err.append(className + "readObjectがProjectSearchDTO型ではありません\n");
					lg.error(className + "readObjectがProjectSearchDTO型ではありません");
				}
			}

			try {
				String query = isOverwrite ? "UPDATE T_伝票フォーマット SET デザインデータ=? WHERE フォーマット名=?" : "INSERT INTO T_伝票フォーマット VALUES(newid(), ?, ?)";
				InputStream is = new ByteArrayInputStream(byteSlip);
				c.setAutoCommit(false);
				ps = c.prepareStatement(query);
				ps.setBinaryStream(1, is);
				ps.setString(2, name);
				int updateCount = ps.executeUpdate();
				is.close();
				c.commit();
				lg.info("T_伝票フォーマットは" + updateCount + "件処理されました。");
			} catch(SQLException ex) {
				isError = true;
				err.append(className + "テーブル「T_伝票フォーマット」の更新に失敗しました\n");
				Logging.logStackTrace(ex, lg, className);
			}


		} catch(Exception ex) {
			Logging.logStackTrace(ex, lg, className);
		}

		/**
		 * クライアントに送信
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(isError);
			out.writeUTF(err.toString());
			out.flush();
			out.close();
		}catch(Exception ex) {
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
