package fukaisystem.application.attendance;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

import javax.servlet.ServletResponse;

import fukaisystem.dto.attendance.DeleteRecordDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 打刻修正データを削除する
 */
public class DeleteRecord extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		DeleteRecordDTO dto = cast(response, o, DeleteRecordDTO.class);
		if (dto == null) {
			return null;
		}
		Date date = new Date(dto.getDate().getTime());
		int memberCD = dto.getMemberCD();

		LocalDateTime now = LocalDateTime.now();
		try (
			PreparedStatement ps = c.prepareStatement("UPDATE T_打刻 SET 削除日時=? WHERE 年月日=? AND 人員CD=?");
		) {
			ps.setObject(1, now);
			ps.setDate(2, date);
			ps.setInt(3, memberCD);
			ps.executeUpdate();
		}
		try (
			PreparedStatement ps = c.prepareStatement("UPDATE T_打刻修正 SET 削除日時=? WHERE 年月日=? AND 人員CD=?");
		) {
			ps.setObject(1, now);
			ps.setDate(2, date);
			ps.setInt(3, memberCD);
			ps.executeUpdate();
		}

		return null;
	}
}
