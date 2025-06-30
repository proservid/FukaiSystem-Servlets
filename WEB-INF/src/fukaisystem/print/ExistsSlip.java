package fukaisystem.print;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletResponse;

import com.proservid.print.dao.SlipDAO;

import fukaisystem.ServiceFoundation;

/**
 * 帳票が登録されているかどうか調べる
 */
public class ExistsSlip extends ServiceFoundation {
	protected static final String className = "ExistsSlip";

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		String name = cast(response, o, String.class);
		return SlipDAO.exists(c, name);
	}
}
