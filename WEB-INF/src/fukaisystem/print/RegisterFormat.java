package fukaisystem.print;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.servlet.ServletResponse;

import com.proservid.print.dao.RegisterFormatDAO;
import fukaisystem.ServiceFoundation;
import fukaisystem.dto.FormatDTO;

public class RegisterFormat extends ServiceFoundation {
    @Override
    public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
        FormatDTO dto = cast(response, o, FormatDTO.class);
        return new RegisterFormatDAO().registerFormat(c, dto.isOverwrite(), dto.getName(), dto.getformat());
    }
}
