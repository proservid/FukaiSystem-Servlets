package fukaisystem.print.format;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.servlet.ServletResponse;

import com.proservid.print.dao.GetFormatDAO;

import fukaisystem.foundation.ServiceFoundation;

public class GetFormatNames extends ServiceFoundation {
	protected static final String className = "GetFormatNames";

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		return GetFormatDAO.getFormatNames(c);
	}

}
