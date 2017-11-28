package fukaisystem;

import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.*;
import fukaisystem.dto.StringDTO;
import fukaisystem.sql.DBConnection;

import org.apache.log4j.Logger;

/**
 * クエリを実行するためのクラス
 * クエリ実行の結果、テーブルが増減する可能性があるため、
 * テーブルリストをクライアントに返すようにしている
 * @author kameura
 *
 */
public class QueryExecutor extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static Logger lg = Logger.getLogger("dbtool");

	public void service(ServletRequest request, ServletResponse response) {

		StringBuilder err = new StringBuilder("");
		StringBuilder msg = new StringBuilder("");

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		Statement st = null;
		ResultSet rs = null;

		try {
			String sql = "";
			int processed = 0;

	//クライアントから読み込み
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			sql = (String)in.readObject();
			in.close();
			lg.debug(sql);

			if(sql.toUpperCase().startsWith("SELECT")) {

			} else if(sql.toUpperCase().startsWith("EXPORT")) {//エクスポート
				String ip = sql.substring(7);
				backup(true, ip, c, err);
			} else if(sql.toUpperCase().startsWith("IMPORT")) {//インポート
				String ip = sql.substring(7);
				backup(false, ip, c, err);
			} else {//更新クエリ
				lg.warn("Query was sent \"" + sql + "\"");
				try {
					st = c.createStatement();
					processed = st.executeUpdate(sql);
					msg.append(processed);
					msg.append("件処理されました。\n");
				} catch(SQLException ex) {
					err.append("エラーが発生しました。\n" + ex);
					lg.error("[2] " + ex);
				}
			}

			List<String> tableName = new ArrayList<String>();
			try {
				String[] types = {"TABLE", "VIEW"};
				DatabaseMetaData dmd = c.getMetaData();
				rs = dmd.getTables(null, "dbo", "%", types);
				while(rs.next()) {
					tableName.add(rs.getString("TABLE_NAME").trim());
				}
			} catch(SQLException ex) {
				err.append("エラーが発生しました。\n" + ex);
				lg.error("[3] " + ex);
			}


//クライアントに送信
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(new StringDTO(msg.toString(), tableName));
			out.writeUTF(err.toString());
			out.flush();
			out.close();

		}catch(Exception ex) {
			lg.error("[4] " + ex);
		} finally {
			try {
				if(c != null && !c.isClosed()) c.close();
			} catch(SQLException ex) {
				lg.error("[5] " + ex);
			}
			// The following processes requires JDBC4.0.
			try {

				if(rs != null && !rs.isClosed()) {
					rs.close();
					lg.debug("rs is closed by jdbc4.0");
				}
			} catch(SQLException ex) {
				lg.error("[6] " + ex);
			}
		}
	}



	//インポート、エクスポート
	public void backup(Boolean isPut, String ip, Connection c, StringBuilder err) {

		Connection fromConnection = null, toConnection = null;
		Statement st_f = null, st_t = null;
		ResultSet rs_f = null;
		PreparedStatement ps = null;///

		Connection c_bk = null;
		//ip = "192.168.0.1";
		String jdbc = "jdbc:sqlserver://" + ip + ":1433;databaseName=FukaiSystem;user=sa;password=3ishifukai";
		try {
			c_bk = DriverManager.getConnection(jdbc);
		} catch(SQLException ex) {
			err.append("バックアップ機に接続できません。\n" + ex);
			lg.fatal("[1] " + ex);
		}

		if(isPut) {
			fromConnection = c;
			toConnection = c_bk;
		} else {
			fromConnection = c_bk;
			toConnection = c;
		}

		try {
			String[] types = {"TABLE"};
			DatabaseMetaData dmd = fromConnection.getMetaData();
			rs_f = dmd.getTables(null, "%", "%", types);
		} catch(SQLException ex) {
			err.append("コピー元のテーブル情報が取得できません。\n" + ex);
			lg.error("[7] " + ex);
		}

		try {
			while(rs_f.next()) {
				String tableName = rs_f.getString("TABLE_NAME").trim();
//コピー先テーブル削除
				try {
					st_t = toConnection.createStatement();///
					st_t.executeUpdate("DROP TABLE " + tableName);///
				} catch(SQLException ex) {
					err.append("コピー先のテーブル " + tableName + " を削除中にエラーが発生しました。\n" + ex);
					lg.error("[8] " + ex);
				}

//テーブルの列名と型をコピー
				try {
					st_f = fromConnection.createStatement();///
					rs_f = st_f.executeQuery("SELECT * FROM " + tableName);///
					ResultSetMetaData rsmd = rs_f.getMetaData();///

					StringBuilder colData = new StringBuilder();
					for(int i = 1; i <= rsmd.getColumnCount(); i++) {
						if(i != 1)  colData.append(",");
						if (rs_f.wasNull()) {///
							colData.append("NULL");
						} else {
							switch(rsmd.getColumnType(i)) {
								case Types.BIT:
									colData.append(rsmd.getColumnName(i) + " " + rsmd.getColumnTypeName(i));
									break;
								case Types.INTEGER:
								case Types.SMALLINT:
									colData.append(rsmd.getColumnName(i) + " " + rsmd.getColumnTypeName(i));
									break;
								case Types.CHAR:
								case Types.VARCHAR:
									colData.append(rsmd.getColumnName(i) + " " + rsmd.getColumnTypeName(i));
									colData.append( "(" + String.valueOf(rsmd.getColumnDisplaySize(i)) + ")");
									break;
							}
						}
					}
					String sql = "CREATE TABLE " + tableName + " (" + colData.toString() + ")";
					lg.debug(sql);
					st_t = toConnection.createStatement();///
					st_t.executeUpdate(sql);///

//テーブルの中身をコピー
					toConnection.setAutoCommit(false);///

					StringBuilder sqlData = new StringBuilder();///
					while(rs_f.next()) {///
						List<Object> cdata = new ArrayList<Object>();
						List<Integer> ctype = new ArrayList<Integer>();
						for(int i = 1; i <= rsmd.getColumnCount(); i++) {
							if(i == 1) {
								sqlData.append("INSERT INTO " + tableName + " VALUES(?");
							} else {
								sqlData.append(", ?");
							}
							switch(rsmd.getColumnType(i)) {
								case Types.BIT:
									cdata.add(rs_f.getBoolean(i));
									ctype.add(rsmd.getColumnType(i));
									break;
								case Types.INTEGER:
								case Types.SMALLINT:
									cdata.add(rs_f.getInt(i));
									ctype.add(rsmd.getColumnType(i));
									break;
								case Types.CHAR:
								case Types.VARCHAR:
									cdata.add(rs_f.getString(i));
									ctype.add(rsmd.getColumnType(i));
									break;
							}
						}
						sqlData.append(")");///

						ps = toConnection.prepareStatement(sqlData.toString());///
						for(int i = 0; i < cdata.size(); i++) {
							switch(ctype.get(i)) {
								case Types.BIT:
									ps.setBoolean(i + 1, (Boolean)cdata.get(i));
									break;
								case Types.INTEGER:
								case Types.SMALLINT:
									ps.setInt(i + 1, (Integer)cdata.get(i));
									break;
								case Types.CHAR:
								case Types.VARCHAR:
									ps.setString(i + 1, (String)cdata.get(i));
									break;
							}
						}
						ps.addBatch();///
					}
					rs_f.close();///
					int[] updateCounts = ps.executeBatch();///
					toConnection.commit();///
					toConnection.setAutoCommit(true);///autoCommitに戻す
					lg.debug(updateCounts.length + "件処理されました。");

				} catch(SQLException ex) {
					err.append("データのバックアップ中にエラーが発生しました。\n" + ex);
					lg.error("[9] " + ex);
				}
			}
		} catch(SQLException ex) {
			err.append("エラーが発生しました。\n" + ex);
			lg.error("[10] " + ex);
		}

	}

	public String literalConvert(String literal) {
		literal = literal.replace("\'", "|\'");
		//literal = literal.replace('"', '\"');
		//literal = literal.replace('%', '\%');
		//literal = literal.replace('_', '\\_');
		//literal = literal.replace('\\', '\\\\');
		return literal;
	}

}
