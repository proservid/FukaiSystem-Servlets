package fukaisystem;

import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.*;

import fukaisystem.dto.ColInfoDTO;
import fukaisystem.dto.SqlDTO;
import fukaisystem.dto.TableAdapter;
import fukaisystem.sql.DBConnection;

import org.apache.log4j.Logger;

/**
 * テーブルの内容と列情報を取得するためのクラス
 * @author kameura
 *
 */
public class GetTableData extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static Logger lg = Logger.getLogger("A1");

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String tableName = "", sql = "";
		StringBuilder err = new StringBuilder("");

		try {
	//クライアントから読み込み

			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			if(obj instanceof SqlDTO) {
				SqlDTO sqlData = (SqlDTO)obj;
				if(sqlData.isQuery()) {
					sql = sqlData.getString();
				} else {
					tableName = sqlData.getString();
					sql = "SELECT * FROM " + tableName;
				}
			}
			in.close();

			Object output = null;

			List<String> keys = new ArrayList<String>();
			List<ColInfoDTO> colInfos = new ArrayList<ColInfoDTO>();
			List<List<Object>> contents = new ArrayList<List<Object>>();
			try {
				Statement st = c.createStatement();
				rs = st.executeQuery(sql);
				ResultSetMetaData rsmd = rs.getMetaData();
				for(int i = 1; i <= rsmd.getColumnCount(); i++) {
					ColInfoDTO ci = new ColInfoDTO(rsmd.getColumnName(i), rsmd.getColumnTypeName(i), rsmd.getColumnType(i), rsmd.getColumnDisplaySize(i));
					colInfos.add(ci);
				}

				while(rs.next()) {
					Object element = null;
					List<Object> row = new ArrayList<Object>();
					for(int i = 1; i <= colInfos.size(); i++) {
						ColInfoDTO column = colInfos.get(i - 1);
						switch(column.getColType()) {
						case Types.BIT:
							element = rs.getBoolean(i);
							break;
						case Types.INTEGER:
						case Types.SMALLINT:
						case Types.TINYINT:
						case Types.BIGINT:
							element = rs.getInt(i);
							break;
						case Types.CHAR:
						case Types.VARCHAR:
							element = rs.getString(i);
							break;
						case Types.DATE:
							element = rs.getDate(i);
							break;
						case Types.TIMESTAMP:
							element = rs.getTimestamp(i);
							break;
						default:
							element = rs.getString(i);
							break;
						}
						if (rs.wasNull()) {
							element = "NULL";
						} else {
							if (element instanceof String) {
								String strElement = ((String)element).trim();
								//色情報なら、Color型を返す
//								if(strElement.startsWith("#")){
	//								strElement = strElement.substring(1);
		//							int color = Integer.parseInt(strElement, 16);
			//						element = new java.awt.Color((color&0xff0000)>>16,
				//					 (color&0xff00)>>8, color&0xff);
					//			} else {
									element = strElement;
//								}
							}
						}
						row.add(element);
					}
					contents.add(row);
				}
				rs.close();

				if(!tableName.equals("")) {
					ps = c.prepareStatement("SELECT COLUMN_NAME FROM information_schema.constraint_column_usage" +
							" WHERE table_name=? AND constraint_name LIKE 'PK_%'");
					ps.setString(1, tableName);
					rs = ps.executeQuery();
					while(rs.next()) {
						keys.add(rs.getString("COLUMN_NAME"));
					}
				}
				output = new TableAdapter(keys, colInfos, contents);
			} catch(SQLException ex) {
				err.append(ex + "\n");
				lg.error("GetElements3 " + ex);
			}

	//クライアントに送信

			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());

			out.writeObject(output);
			out.writeUTF(err.toString());
			out.flush();
			out.close();
		}catch(Exception ex) {
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