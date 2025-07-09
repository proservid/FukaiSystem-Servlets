package fukaisystem.print.format;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletResponse;

import com.proservid.print.dao.RegisterFormatDAO;

import fukaisystem.foundation.ServiceFoundation;

/**
 * フォーマットを削除する（ID再利用のためIDを負にするのみのソフトデリート）
 */
public class DeleteFormat extends ServiceFoundation {
	protected static final String className = "DeleteFormat";

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		String formatName = cast(response, o, String.class);
		return RegisterFormatDAO.deleteFormat(c, formatName);
	}

}
