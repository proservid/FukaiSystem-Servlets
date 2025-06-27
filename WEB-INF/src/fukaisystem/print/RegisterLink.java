package fukaisystem.print;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.ServletResponse;

import com.proservid.print.dao.RegisterFormatDAO;

import fukaisystem.ServiceFoundation;

public class RegisterLink extends ServiceFoundation {
    protected static final String className = "RegisterLink";
    @Override
    public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
        @SuppressWarnings("unchecked")
        Vector<Vector<String>> data = cast(response, o, Vector.class);

        return RegisterFormatDAO.registerLink(c, data);
    }

}
