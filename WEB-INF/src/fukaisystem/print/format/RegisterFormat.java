package fukaisystem.print.format;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.servlet.ServletResponse;

import com.proservid.print.dao.RegisterFormatDAO;

import fukaisystem.dto.FormatDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * フォーマットを登録する
 */
public class RegisterFormat extends ServiceFoundation {
	protected static final String className = "RegisterFormat";

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		FormatDTO dto = cast(response, o, FormatDTO.class);
		boolean result = new RegisterFormatDAO().registerFormat(c, dto.isOverwrite(), dto.getName(), dto.getformat());
		if (!result) {
			throw new SQLException();
		}
		return result;
	}
}
