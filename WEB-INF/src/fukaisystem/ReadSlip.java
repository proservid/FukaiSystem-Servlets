package fukaisystem;

import java.io.ByteArrayOutputStream;
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
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class ReadSlip extends GenericServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "ReadSlip\n";

	@Override
	public void service(ServletRequest request, ServletResponse response) {
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuilder err = new StringBuilder();

		String name = null;
		SlipDTO slipDTO = null;
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
				if (obj instanceof String) {
					name = (String) obj;
				} else {
					err.append(className + "readObjectがString型ではありません\n");
					lg.error(className + "readObjectがString型ではありません");
				}
			}

			try {
				ps = c.prepareStatement("SELECT * FROM T_伝票フォーマット WHERE フォーマット名=?");
				ps.setString(1, name);
				rs = ps.executeQuery();
				while (rs.next()) {
					// rs.getString("帳票名");
					InputStream is = rs.getBinaryStream("デザインデータ");
					// 入力ストリームからバイト配列へ読み込み
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					int DEFAULT_BUFFER_SIZE = 1024 * 4;
					byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
					int size = -1;
					while (-1 != (size = is.read(buffer))) {
						baos.write(buffer, 0, size);
					}
					is.close();
					baos.close();
					slipDTO = new SlipDTO(false, name, baos.toByteArray());
				}
			} catch (SQLException ex) {
				err.append(className + "テーブル「T_帳票」の読込に失敗しました\n");
				Logging.logStackTrace(ex, lg, className);
			}

		} catch (Exception ex) {
			Logging.logStackTrace(ex, lg, className);
		}

		/**
		 * クライアントに送信
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(slipDTO);
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
