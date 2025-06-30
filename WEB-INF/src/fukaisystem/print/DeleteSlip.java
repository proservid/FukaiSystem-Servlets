package fukaisystem.print;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletResponse;

import com.proservid.print.dao.SlipDAO;

import fukaisystem.ServiceFoundation;

/**
 * 帳票の登録を削除する
 */
public class DeleteSlip extends ServiceFoundation {
	protected static final String className = "DeleteSlip";

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		String name = cast(response, o, String.class);
		return SlipDAO.delete(c, name);
	}
}
