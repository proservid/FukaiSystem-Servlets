package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.dto.DestinationDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 指定された得意先の納入先リストを更新する
 */
public class DestinationRegister extends ServiceFoundation {

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		DestinationDTO dto = cast(response, o, DestinationDTO.class);
		int accountCD = dto.getAccountCD();
		Vector<Vector<Object>> destinations = dto.getDestinations();

		if (accountCD < 1 || destinations == null || destinations.size() < 1) {
			return null;
		}
		try (
			PreparedStatement ps = c.prepareStatement(
				"DELETE FROM M_納入先 WHERE 得意先CD=?"
			);
		) {
			ps.setInt(1, accountCD);
			ps.executeUpdate();
		}
		try (
			PreparedStatement ps = c.prepareStatement(
				"INSERT INTO M_納入先 VALUES(?, ?, ?)"
			);
		) {
			int i = 1;
			for (Vector<Object> record : destinations) {
				String destination = (String) record.get(0);
				if (destination.isEmpty()) {
					continue;
				}
				ps.setInt(1, accountCD);
				ps.setInt(2, i++); // 納入先CD
				ps.setString(3, destination);
				ps.addBatch();
			}
			ps.executeBatch();
		}
		return true;
	}
}
