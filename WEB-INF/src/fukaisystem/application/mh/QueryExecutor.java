package fukaisystem.application.mh;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletResponse;

import fukaisystem.dto.StringDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * クエリを実行するためのクラス
 * クエリ実行の結果、テーブルが増減する可能性があるため、
 * テーブルリストをクライアントに返すようにしている
 * 
 * @author kameura
 *
 */
public class QueryExecutor extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		StringBuilder err = new StringBuilder("");
		StringBuilder msg = new StringBuilder("");

		String sql = cast(response, o, String.class);
		int processed = 0;

		if (sql.toUpperCase().startsWith("SELECT")) {

		} else if (sql.toUpperCase().startsWith("EXPORT")) { // エクスポート
			String ip = sql.substring(7);
			backup(true, ip, c, err);
		} else if (sql.toUpperCase().startsWith("IMPORT")) { // インポート
			String ip = sql.substring(7);
			backup(false, ip, c, err);
		} else { // 更新クエリ
			try (Statement st = c.createStatement();) {
				processed = st.executeUpdate(sql);
				msg.append(processed);
				msg.append("件処理されました。\n");
			}
		}

		List<String> tableName = new ArrayList<String>();
		String[] types = { "TABLE", "VIEW" };
		DatabaseMetaData dmd = c.getMetaData();
		ResultSet rs = dmd.getTables(null, "dbo", "%", types);
		while (rs.next()) {
			tableName.add(rs.getString("TABLE_NAME").trim());
		}

		return new StringDTO(msg.toString(), tableName);
	}

	// インポート、エクスポート
	public void backup(Boolean isPut, String ip, Connection c, StringBuilder err) throws SQLException {

		Connection fromConnection = null, toConnection = null;
		Statement st_f = null, st_t = null;
		ResultSet rs_f = null;
		PreparedStatement ps = null;

		Connection c_bk = null;
		// ip = "192.168.0.1";
		String jdbc = "jdbc:sqlserver://" + ip + ":1433;databaseName=FukaiSystem;user=sa;password=3ishifukai";
		c_bk = DriverManager.getConnection(jdbc);

		if (isPut) {
			fromConnection = c;
			toConnection = c_bk;
		} else {
			fromConnection = c_bk;
			toConnection = c;
		}

		String[] types = { "TABLE" };
		DatabaseMetaData dmd = fromConnection.getMetaData();
		rs_f = dmd.getTables(null, "%", "%", types);

		while (rs_f.next()) {
			String tableName = rs_f.getString("TABLE_NAME").trim();
			// コピー先テーブル削除
			st_t = toConnection.createStatement();
			st_t.executeUpdate("DROP TABLE " + tableName);

			// テーブルの列名と型をコピー
			st_f = fromConnection.createStatement();
			rs_f = st_f.executeQuery("SELECT * FROM " + tableName);
			ResultSetMetaData rsmd = rs_f.getMetaData();

			StringBuilder colData = new StringBuilder();
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				if (i != 1)
					colData.append(",");
				if (rs_f.wasNull()) {
					colData.append("NULL");
				} else {
					switch (rsmd.getColumnType(i)) {
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
							colData.append("(" + String.valueOf(rsmd.getColumnDisplaySize(i)) + ")");
							break;
					}
				}
			}
			String sql = "CREATE TABLE " + tableName + " (" + colData.toString() + ")";
			st_t = toConnection.createStatement();
			st_t.executeUpdate(sql);

			// テーブルの中身をコピー
			toConnection.setAutoCommit(false);

			StringBuilder sqlData = new StringBuilder();
			while (rs_f.next()) {
				List<Object> cdata = new ArrayList<Object>();
				List<Integer> ctype = new ArrayList<Integer>();
				for (int i = 1; i <= rsmd.getColumnCount(); i++) {
					if (i == 1) {
						sqlData.append("INSERT INTO " + tableName + " VALUES(?");
					} else {
						sqlData.append(", ?");
					}
					switch (rsmd.getColumnType(i)) {
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
				sqlData.append(")");

				ps = toConnection.prepareStatement(sqlData.toString());
				for (int i = 0; i < cdata.size(); i++) {
					switch (ctype.get(i)) {
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
				ps.addBatch();
			}
			rs_f.close();
			ps.executeBatch();
			toConnection.commit();
			toConnection.setAutoCommit(true); // autoCommitに戻す
		}
	}

	public String literalConvert(String literal) {
		literal = literal.replace("\'", "|\'");
		// literal = literal.replace('"', '\"');
		// literal = literal.replace('%', '\%');
		// literal = literal.replace('_', '\\_');
		// literal = literal.replace('\\', '\\\\');
		return literal;
	}

}
