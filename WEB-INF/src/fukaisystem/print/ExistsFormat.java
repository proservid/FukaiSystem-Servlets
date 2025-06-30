package fukaisystem.print;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletResponse;

import com.proservid.print.dao.GetFormatDAO;

import fukaisystem.ServiceFoundation;

/**
 * フォーマットが登録されているかどうか調べる
 */
public class ExistsFormat extends ServiceFoundation {
	protected static final String className = "ExistsFormat";

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		String name = cast(response, o, String.class);
		return GetFormatDAO.existsFormat(c, name);
	}
}
