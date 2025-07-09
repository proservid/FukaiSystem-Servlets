package fukaisystem.print.slip;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletResponse;

import com.proservid.print.dao.SlipDAO;

import fukaisystem.foundation.ServiceFoundation;

/**
 * 帳票名を変更する
 */
public class RenameSlip extends ServiceFoundation {
	protected static final String className = "RenameSlip";

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		String[] names = cast(response, o, String[].class);
		return SlipDAO.rename(c, names[0], names[1]);
	}
}
