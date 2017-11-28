package fukaisystem.group;

	import java.io.ObjectInputStream;
	import java.io.ObjectOutputStream;
	import java.security.MessageDigest;
	import java.security.NoSuchAlgorithmException;
	import java.sql.Connection;
	import java.sql.PreparedStatement;
	import java.sql.ResultSet;
	import java.sql.SQLException;
	import java.util.LinkedHashMap;
	import java.util.Map;
	import java.util.TreeMap;
	import java.util.Vector;
	import javax.servlet.GenericServlet;
	import javax.servlet.ServletRequest;
	import javax.servlet.ServletResponse;
	import org.apache.log4j.Logger;
	import fukaisystem.dto.DMDTO;
	import fukaisystem.sql.DBConnection;
	import fukaisystem.util.Logging;

	public class GetDandM extends GenericServlet {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
	   private static final Logger lg = Logger.getLogger("A1");
		private static final String className = "GetDandM\n";

		public void service(ServletRequest request, ServletResponse response) {

			DBConnection dbc = new DBConnection();
			Connection c = dbc.getConnection();
			PreparedStatement ps = null;
			ResultSet rs = null;
			StringBuilder err = new StringBuilder();

			TreeMap<String, String> dept = new TreeMap<String, String>();
			Map<String, Vector<Vector>> name = new LinkedHashMap<String, Vector<Vector>>();
			DMDTO dmd = null;

			try {

				/**
				 * クライアントデータ受け取り
				 */
				ObjectInputStream in = new ObjectInputStream(request.getInputStream());
				Object[] obj = (Object[])in.readObject();
				in.close();

				try {
					ps = c.prepareStatement("SELECT RIGHT('00' + CONVERT(varchar, CD), 3) AS 部署CD,部署名 FROM M_部署");
					rs = ps.executeQuery();
					while(rs.next()) {
						dept.put(rs.getString("部署CD"), rs.getString("部署名"));
						name.put(rs.getString("部署CD"), new Vector<Vector>());
					}
					ps = c.prepareStatement("SELECT CD,姓,名 ,RIGHT('00' + CONVERT(varchar, 所属部署CD), 3) AS 部署CD,在籍FLG FROM M_人員 WHERE CD>0 ORDER BY 所属部署CD,表示CD");
					rs = ps.executeQuery();
					while(rs.next()) {
						Vector v = new Vector();
						//v.add(rs.getInt("管理ID"));
						v.add(rs.getInt("CD"));
						v.add(rs.getString("姓"));
						v.add(rs.getString("名"));
						v.add(rs.getBoolean("在籍FLG"));
						if(name.containsKey(rs.getString("部署CD"))) name.get(rs.getString("部署CD")).add(v);
					}
					dmd = new DMDTO(dept, name);
				} catch(SQLException ex) {
					Logging.logStackTrace(ex, lg, className);
					err.append("DBエラー\n");
				}


				/**
				 * クライアントに送信
				 */


				response.setContentType("application/octet-stream");
				ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
				out.writeObject(dmd);
				out.writeUTF(err.toString());
				out.flush();
				out.close();

			} catch(Exception ex) {
				Logging.logStackTrace(ex, lg, className);
			} finally {
				try {
					if(c != null && !c.isClosed()) c.close();
				} catch(SQLException ex) {
					Logging.logStackTrace(ex, lg, className);
				}
	//The following processes requires JDBC4.0.
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

		 /**
		  * MD5で文字列を暗号化し、暗号化されたバイナリを16進数表記の文字列に変換した値を取得する
		  * 
		  * @param str
		  *            暗号化対象の文字列
		  * @return 暗号化した結果を16進数表記に変換した文字列
		  */
		  public static String digestMd5(char[] c) throws NoSuchAlgorithmException {
			  String str = new String(c);
		      if (str == null || str.length() == 0) {
		          throw new IllegalArgumentException("文字列がNull、または空です。");
		      }

		      // MD5で暗号化したByte型配列を取得する
		      MessageDigest md5 = MessageDigest.getInstance("MD5");
		      md5.update(str.getBytes());
		      byte[] enclyptedHash = md5.digest();

		      // 暗号化されたByte型配列を、16進数表記文字列に変換する
		      return bytesToHexString(enclyptedHash);
		  }
		  /**
		   * Byte型配列から16進数表記文字列へ変換する
		   * @param fromByte 変換対象Byte型配列
		   * @return 16進数表記に変換後の文字列
		   */
		   public static String bytesToHexString(byte[] fromByte) {

		       StringBuilder hexStrBuilder = new StringBuilder();
		       for (int i = 0; i < fromByte.length; i++) {

		           // 16進数表記で1桁数値だった場合、2桁目を0で埋める
		           if ((fromByte[i] & 0xff) < 0x10) {
		               hexStrBuilder.append("0");
		           }
		           hexStrBuilder.append(Integer.toHexString(0xff & fromByte[i]));
		       }

		       return hexStrBuilder.toString();
		   }

	}
