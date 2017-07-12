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

import fukaisystem.dto.SlipSelectDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;


	public class GetSlipName extends GenericServlet {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
	    private static final Logger lg = Logger.getLogger("A1");
		private static final String className = "GetSlipName\n";

		public void service(ServletRequest request, ServletResponse response) {

			DBConnection dbc = new DBConnection();
			Connection c = dbc.getConnection();
			PreparedStatement ps = null;
			ResultSet rs = null;
			String input = "";
			Object output = "";
			Vector<String> candidate = new Vector<String>();
			Vector<Vector<String>> tableData = new Vector<Vector<String>>();
			StringBuilder err = new StringBuilder("");

			try {

				/**
				 * クライアントデータ受け取り
				 */
				ObjectInputStream in = new ObjectInputStream(request.getInputStream());
				Object obj = in.readObject();
				in.close();

				if(obj == null) {
					err.append(className + "readObjectがnullです\n");
					lg.error(className + "readObjectがnullです");
				} else {
					if(obj instanceof String) {
						input = (String)obj;
					} else {
						err.append(className + "readObjectがString型ではありません\n");
						lg.error(className + "readObjectがString型ではありません");
					}
				}

				if(input.equals("")) {
					candidate.add("");//まず、空データを追加
					//登録されている作成データを取得
					try {
						ps = c.prepareStatement("SELECT フォーマット名 FROM T_伝票フォーマット ORDER BY フォーマット名");
						rs = ps.executeQuery();
						while(rs.next()) {
							candidate.add(rs.getString("フォーマット名"));
						}
					} catch(SQLException ex) {
						err.append(className + "テーブル「T_伝票フォーマット」の読込に失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}
					//
					try {
						ps = c.prepareStatement("SELECT 伝票名,フォーマット名 FROM T_伝票");
						rs = ps.executeQuery();
						while(rs.next()) {
							Vector<String> v = new Vector<String>();
							v.add(rs.getString("伝票名"));
							v.add(rs.getString("フォーマット名"));
							tableData.add(v);
						}
						output = tableData;
					} catch(SQLException ex) {
						err.append(className + "テーブル「T_伝票」の読込に失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}
					output = new SlipSelectDTO(candidate, tableData);
				} else {
					try {
						ps = c.prepareStatement("SELECT フォーマット名 FROM T_伝票 WHERE 伝票名=?");
						ps.setString(1, input);
						rs = ps.executeQuery();
						while(rs.next()) {
							output = rs.getString("フォーマット名");
						}
					} catch(SQLException ex) {
						err.append(className + "テーブル「T_伝票」の読込に失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}
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
				out.writeObject(output);
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
