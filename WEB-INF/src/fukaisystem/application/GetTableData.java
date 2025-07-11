package fukaisystem.application;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletResponse;

import fukaisystem.dto.ColInfoDTO;
import fukaisystem.dto.SqlDTO;
import fukaisystem.dto.TableAdapter;
import fukaisystem.foundation.ServiceFoundation;

/**
 * テーブルの内容と列情報を取得するためのクラス
 * 
 * @author kameura
 *
 */
public class GetTableData extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		String tableName = "", sql = "";
		SqlDTO sqlData = cast(response, o, SqlDTO.class);
		if (sqlData.isQuery()) {
			sql = sqlData.getString();
		} else {
			tableName = sqlData.getString();
			sql = "SELECT * FROM " + tableName;
		}

		List<ColInfoDTO> colInfos = new ArrayList<ColInfoDTO>();
		List<List<Object>> contents = new ArrayList<List<Object>>();
		try (Statement st = c.createStatement();) {
			try (ResultSet rs = st.executeQuery(sql);) {
				ResultSetMetaData rsmd = rs.getMetaData();
				for (int i = 1; i <= rsmd.getColumnCount(); i++) {
					ColInfoDTO ci = new ColInfoDTO(
						rsmd.getColumnName(i),
						rsmd.getColumnTypeName(i),
						rsmd.getColumnType(i),
						rsmd.getColumnDisplaySize(i)
					);
					colInfos.add(ci);
				}

				while (rs.next()) {
					Object element = null;
					List<Object> record = new ArrayList<Object>();
					for (int i = 1; i <= colInfos.size(); i++) {
						ColInfoDTO colInfo = colInfos.get(i - 1);
						switch (colInfo.getColType()) {
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
								String strElement = ((String) element).trim();
								// 色情報なら、Color型を返す
								// if(strElement.startsWith("#")){
								// strElement = strElement.substring(1);
								// int color = Integer.parseInt(strElement, 16);
								// element = new
								// java.awt.Color((color&0xff0000)>>16,
								// (color&0xff00)>>8, color&0xff);
								// } else {
								element = strElement;
								// }
							}
						}
						record.add(element);
					}
					contents.add(record);
				}
			}
		}

		List<String> keys = new ArrayList<String>();
		if (!tableName.equals("")) {
			try (
				PreparedStatement ps = c.prepareStatement(
					"SELECT COLUMN_NAME FROM information_schema.constraint_column_usage"
						+ " WHERE table_name=? AND constraint_name LIKE 'PK_%'"
				);
			) {
				ps.setString(1, tableName);
				try (ResultSet rs = ps.executeQuery();) {
					while (rs.next()) {
						keys.add(rs.getString("COLUMN_NAME"));
					}
				}
			}
		}
		return new TableAdapter(keys, colInfos, contents);
	}
}