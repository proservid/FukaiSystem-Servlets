package fukaisystem.application.group;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

public class DeleteMember extends ServiceFoundation {

	@SuppressWarnings("unchecked")
	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		List<Integer> delMembers = null;

		delMembers = cast(response, o, List.class);

		try (
			PreparedStatement ps = c.prepareStatement("delete from M_人員 where CD=?");
		) {
			for (int m : delMembers) {
				ps.setInt(1, m);
				ps.addBatch();
			}
			return ps.executeBatch().length;
		}
	}
}
