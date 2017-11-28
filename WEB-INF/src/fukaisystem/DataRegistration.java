package fukaisystem;

import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.*;
import fukaisystem.dto.StringDTO;

import org.apache.log4j.Logger;

import fukaisystem.sql.DBConnection;


public class DataRegistration extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static Logger lg = Logger.getLogger("dbtool");


	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();

		PreparedStatement ps = null;
		Statement st = null;
		ResultSet rs = null;
		StringBuilder msg = new StringBuilder();
		StringBuilder err = new StringBuilder();
		try {

			Object obj = null;
			List<String> lines = null;

			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			try {
				obj = in.readObject();
			} catch (ClassNotFoundException ex) {}
			in.close();

			String tableName = "";
			if(obj instanceof StringDTO) {
				StringDTO stringDTO = (StringDTO)obj;
				tableName = stringDTO.getString();
				lines = stringDTO.getData();
			}
			if(lines == null) lg.debug("linesnull");

			int k = 0;
			try {
				st = c.createStatement();
				rs = st.executeQuery("SELECT * FROM " + tableName);
				ResultSetMetaData rsmd = rs.getMetaData();
				int cols = rsmd.getColumnCount();
				int[] types = new int[cols];


//				StringBuilder sql = new StringBuilder("SET IDENTITY_INSERT " + tableName + " ON; INSERT INTO ");
				StringBuilder sql = new StringBuilder("INSERT INTO ");
				sql.append(tableName);
				sql.append(" (");
				for(int i = 0; i < cols; i++) {
					if(i == cols - 1) sql.append(rsmd.getColumnName(i+1)+") ");
					else sql.append(rsmd.getColumnName(i+1)+", ");
				}
				sql.append("VALUES (");
				for(int i = 0; i < cols; i++) {
//					if(i == cols - 1) sql.append("?); SET IDENTITY_INSERT " + tableName + " OFF");
					if(i == cols - 1) sql.append("?);");
					else sql.append("?, ");
					types[i] = rsmd.getColumnType(i + 1);
				}


				c.setAutoCommit(false);
				ps = c.prepareStatement(sql.toString());
				for(int i = 1; i < lines.size(); i++) {
					String[] strs = lines.get(i).split("\t", cols);
					for(int j = 0; j < cols; j++) {//テーブルの列数を超えるデータは無視
						boolean isEmpty = false;
						if(strs.length <= j) {//テーブルの列数に満たない場合は空データで埋める
							isEmpty = true;
						} else if(strs[j].equals("")) {
							isEmpty = true;
						}
						if(isEmpty) {
							switch(types[j]) {
								case Types.BIT:
								case Types.BOOLEAN:
									ps.setBoolean(j + 1, false);
									break;
								case Types.INTEGER:
								case Types.SMALLINT:
								case Types.TINYINT:
								case Types.BIGINT:
									ps.setInt(j + 1, 0);
									break;
								case Types.CHAR:
								case Types.VARCHAR:
	//							case Types.NCHAR:
	//							case Types.NVARCHAR:
									ps.setString(j + 1, strs[j]);
									break;
								case Types.DATE:
									ps.setDate(j + 1, null);
									break;
								case Types.TIMESTAMP:
									ps.setTimestamp(j + 1, null);
									break;
								case Types.BINARY:
								case Types.VARBINARY:
								case Types.JAVA_OBJECT:
								case Types.OTHER:
									ps.setObject(j + 1, null);
									break;
								default:
									ps.setString(j + 1, null);
									break;
							}
						} else {
							ps.setString(j + 1, strs[j]);
						}
					}
					ps.addBatch();
					k++;
				}

				int[] updateCounts = ps.executeBatch();
				c.commit();
				msg.append(updateCounts.length + "件処理されました。");
			} catch(SQLException ex) {
				ex.printStackTrace();
				String errStr = String.valueOf(k + 1) + "行目：" + ex;
				err.append(errStr);
				lg.error(errStr);
			} catch(Exception ex) {
				ex.printStackTrace();
				StringWriter writer = new StringWriter();
				PrintWriter writer2 = new PrintWriter(writer);
				ex.printStackTrace(writer2);
				writer2.flush();
				err.append(writer.toString());
			}



	//クライアントに送信

			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(msg.toString());
			out.writeUTF(err.toString());
			out.flush();
			out.close();
		}catch(Exception ex) {
			ex.printStackTrace();
			lg.error(ex);
		} finally {
			try {
				if(c != null && !c.isClosed()) c.close();
			} catch(SQLException ex) {
				lg.error("c:" + ex);
			}
			// The following processes requires JDBC4.0.
			try {
				if(ps != null && !ps.isClosed()) {
					ps.close();
					lg.debug("ps is closed by jdbc4.0");
				}
			} catch(SQLException ex) {
				lg.error("ps:" + ex);
			}
			try {
				if(rs != null && !rs.isClosed()) {
					rs.close();
					lg.debug("rs is closed by jdbc4.0");
				}
			} catch(SQLException ex) {
				lg.error("rs:" + ex);
			}
		}
	}
}