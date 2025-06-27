package fukaisystem.table;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.ServletResponse;

import com.proservid.print.dao.TableDAO;

import fukaisystem.ServiceFoundation;

public class GetNames extends ServiceFoundation {
	protected static final String className = "GetNames";

	@Override
    public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
        if (o == null) {
            return getTableNames(c);
        }
        String tableName = cast(response, o, String.class);
        return getColumnNames(c, tableName);
    }

	/**
	 * テーブル名及びビュー名の一覧を取得する
	 * 
	 * @return テーブル、ビューの一覧
	 * @throws SQLException 
	 */
	public Vector<String> getTableNames(Connection c) throws SQLException {
		return TableDAO.getTableNames(c);
	}

	/**
	 * テーブル名を指定してその全カラム名をVectorで返す
	 * 
	 * @param tableName 全カラム名を取得するテーブル名
	 * @return tableName で指定されたテーブルの全カラム名のVector
	 * @throws SQLException 
	 */
	public Vector<String> getColumnNames(Connection c, String tableName) throws SQLException {
		return TableDAO.getColumnNames(c, tableName);
	}
}
