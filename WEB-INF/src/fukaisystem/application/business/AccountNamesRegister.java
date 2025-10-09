package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.dto.AccountNamesDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 指定された得意先の宛名リストを更新する
 */
public class AccountNamesRegister extends ServiceFoundation {

	protected static final String[] TABLE_NAMES = { "M_見積宛名", "M_出荷宛名" };

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		AccountNamesDTO dto = cast(response, o, AccountNamesDTO.class);
		int type = dto.getType();
		int accountCD = dto.getAccountCD();
		Vector<Vector<Object>> accountNames = dto.getAccountNames();

		if (accountCD < 1 || accountNames == null || accountNames.size() < 1) {
			return null;
		}
		try (
			PreparedStatement ps = c.prepareStatement(
				"DELETE FROM " + TABLE_NAMES[type] + " WHERE 得意先CD=?"
			);
		) {
			ps.setInt(1, accountCD);
			ps.executeUpdate();
		}
		try (
			PreparedStatement ps = c.prepareStatement(
				"INSERT INTO " + TABLE_NAMES[type] + " VALUES(?, ?, ?)"
			);
		) {
			int i = 1;
			for (Vector<Object> record : accountNames) {
				String destination = (String) record.get(0);
				if (destination.isEmpty()) {
					continue;
				}
				ps.setInt(1, accountCD);
				ps.setInt(2, i++); // 宛名ID
				ps.setString(3, destination);
				ps.addBatch();
			}
			ps.executeBatch();
		}
		return true;
	}
}
