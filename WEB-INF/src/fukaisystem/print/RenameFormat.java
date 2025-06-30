package fukaisystem.print;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletResponse;

import com.proservid.print.dao.RegisterFormatDAO;

import fukaisystem.ServiceFoundation;
import fukaisystem.dto.FormatDTO;

/**
 * フォーマット名を変更する
 */
public class RenameFormat extends ServiceFoundation {
	protected static final String className = "RenameFormat";

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		FormatDTO dto = cast(response, o, FormatDTO.class);
		boolean result = new RegisterFormatDAO().renameFormat(c, dto.getName(), dto.getNewName(), dto.getformat());
		if (!result) {
			throw new SQLException();
		}
		return result;
	}
}
