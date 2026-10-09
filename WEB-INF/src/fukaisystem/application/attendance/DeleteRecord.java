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
 *
 * <p>T_打刻 と T_打刻修正 の削除日時をまとめて立てるため、更新の前に対象行を
 * {@link #lock(Connection, String, String, Object...)} でロックし、
 * 同じ年月日・人員の登録や削除が割り込まないようにする。
 */
public class DeleteRecord extends ServiceFoundation {

	/** ロック・更新の対象を特定する条件 */
	private static final String KEY_CONDITION = "年月日=? AND 人員CD=?";

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		DeleteRecordDTO dto = cast(response, o, DeleteRecordDTO.class);
		if (dto == null) {
			return null;
		}
		Date date = new Date(dto.getDate().getTime());
		int memberCD = dto.getMemberCD();

		lock(c, "T_打刻", KEY_CONDITION, date, memberCD);
		lock(c, "T_打刻修正", KEY_CONDITION, date, memberCD);

		LocalDateTime now = LocalDateTime.now();
		int count = 0;
		try (
			PreparedStatement ps = c.prepareStatement("UPDATE T_打刻 SET 削除日時=? WHERE " + KEY_CONDITION);
		) {
			ps.setObject(1, now);
			ps.setDate(2, date);
			ps.setInt(3, memberCD);
			count += ps.executeUpdate();
		}
		try (
			PreparedStatement ps = c.prepareStatement("UPDATE T_打刻修正 SET 削除日時=? WHERE " + KEY_CONDITION);
		) {
			ps.setObject(1, now);
			ps.setDate(2, date);
			ps.setInt(3, memberCD);
			count += ps.executeUpdate();
		}

		// null を返すと access() がロールバックしてしまうため、必ず件数を返す
		return Integer.valueOf(count);
	}
}
