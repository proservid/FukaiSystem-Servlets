package fukaisystem.print.format;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.servlet.ServletResponse;

import com.proservid.print.dao.GetFormatDAO;

import fukaisystem.foundation.ServiceFoundation;

public class GetFormat extends ServiceFoundation {
	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		String formatName = cast(response, o, String.class);
		return new GetFormatDAO().getFormat(c, formatName);
	}
}
