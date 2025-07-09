package fukaisystem.print.format;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.servlet.ServletResponse;

import com.proservid.print.dao.GetFormatDAO;

import fukaisystem.foundation.ServiceFoundation;

public class GetFormatName extends ServiceFoundation {
	protected static final String className = "GetFormatName";

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		String slipName = cast(response, o, String.class);
		return GetFormatDAO.getFormatName(c, slipName);
	}

}
