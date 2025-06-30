package fukaisystem.print;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletResponse;

import com.proservid.print.dao.SlipDAO;

import fukaisystem.ServiceFoundation;

/**
 * 帳票名とフォーマット名の関連付けを登録する
 */
public class RegisterLink extends ServiceFoundation {
	protected static final String className = "RegisterLink";

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		String[] data = cast(response, o, String[].class);

		return SlipDAO.registerLink(c, data[0], data[1]);
	}

}
