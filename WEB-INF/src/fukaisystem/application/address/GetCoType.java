package fukaisystem.application.address;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

public class GetCoType extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		Vector<String> types = new Vector<String>();

		try (
			PreparedStatement ps = c.prepareStatement("SELECT 種別 FROM M_会社種別");
		) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				types.add(rs.getString("種別"));
			}

		}

		return types;
	}

}
