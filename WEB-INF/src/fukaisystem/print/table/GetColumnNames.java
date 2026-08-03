package fukaisystem.print.table;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

public class GetColumnNames extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		Vector<String> data = new Vector<String>();

		String tableName = cast(response, o, String.class);

		try (Statement st = c.createStatement();) {
			ResultSet rs = st.executeQuery("SELECT * FROM " + tableName);
			ResultSetMetaData rsmd = rs.getMetaData();

			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				if (!rs.wasNull()) {
					switch (rsmd.getColumnType(i)) {
						case Types.CHAR:
						case Types.VARCHAR:
							data.add(
								rsmd.getColumnName(i)
									+ " [" + rsmd.getColumnTypeName(i)
									+ "(" + String.valueOf(rsmd.getColumnDisplaySize(i)) + ")]"
							);
							break;
						default:
							data.add(rsmd.getColumnName(i) + " [" + rsmd.getColumnTypeName(i) + "]");
					}
				}
			}
		}
		return data;
	}

}