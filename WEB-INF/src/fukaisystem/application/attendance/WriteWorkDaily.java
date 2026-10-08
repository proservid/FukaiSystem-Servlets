package fukaisystem.application.attendance;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import javax.servlet.ServletResponse;

import fukaisystem.domain.attendance.WorkDaily;
import fukaisystem.dto.attendance.WorkDailiesDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 日次集計データを登録・変更する
 *
 * <p>複数件をひとつのトランザクションで処理する。自動コミットのままでは
 * 途中で失敗したときに一部だけ登録された状態が確定してしまう。
 *
 * <p>MERGE の対象には {@code WITH (HOLDLOCK)} を指定する。既定の READ COMMITTED では
 * 一致判定のために取った共有ロックが判定直後に解放されるため、判定と登録の間に
 * 同じ年月日・人員CDの集計が割り込み、主キー違反になることがある。
 */
public class WriteWorkDaily extends ServiceFoundation {

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		WorkDailiesDTO dto = cast(response, o, WorkDailiesDTO.class);
		if (dto == null) {
			return null; // cast() がクライアントに送信済み
		}
		List<WorkDaily> workDailies = dto.getWorkDailies();

		LocalDateTime now = LocalDateTime.now();
		try (
			PreparedStatement ps = c.prepareStatement(
				"MERGE INTO T_日次集計 WITH (HOLDLOCK) AS d"
					+ " USING (SELECT ? AS date, ? AS cd, ? AS total, ? AS scheduled, ? AS earlyWork, ? AS overTime, ? AS night, ? AS lateEarly, ? AS holiday, ? AS sunday, ? AS trip, ? AS absence, ? AS amPaid, ? AS pmPaid, ? AS paid, ? AS comp, ? AS now) AS w"
					+ "  ON d.年月日=w.date AND d.人員CD=w.cd"
					+ " WHEN MATCHED THEN"
					+ "  UPDATE SET"
					+ "   d.実労働時間 = w.total,"
					+ "   d.所定内 = w.scheduled,"
					+ "   d.早出 = w.earlyWork,"
					+ "   d.時間外労働 = w.overTime,"
					+ "   d.深夜労働 = w.night,"
					+ "   d.遅刻早退 = w.lateEarly,"
					+ "   d.土休FLG = w.holiday,"
					+ "   d.日曜FLG = w.sunday,"
					+ "   d.出張FLG = w.trip,"
					+ "   d.欠勤FLG = w.absence,"
					+ "   d.前休FLG = w.amPaid,"
					+ "   d.後休FLG = w.pmPaid,"
					+ "   d.有給FLG = w.paid,"
					+ "   d.代休FLG = w.comp,"
					+ "   d.集計日時 = w.now"
					+ " WHEN NOT MATCHED THEN"
					// 所定内・早出の追加で列の並びに依存しないよう、INSERT は列名を明記する
					+ " INSERT(年月日, 人員CD, 実労働時間, 所定内, 早出, 時間外労働, 深夜労働, 遅刻早退, 土休FLG, 日曜FLG, 出張FLG, 欠勤FLG, 前休FLG, 後休FLG, 有給FLG, 代休FLG, 集計日時)"
					+ " VALUES(w.date, w.cd, w.total, w.scheduled, w.earlyWork, w.overTime, w.night, w.lateEarly, w.holiday, w.sunday, w.trip, w.absence, w.amPaid, w.pmPaid, w.paid, w.comp, w.now);"
			);
		) {
			for (WorkDaily wd : workDailies) {
				int i = 1;
				ps.setObject(i++, wd.getWorkDate());
				ps.setInt(i++, wd.getEmployeeNo());
				ps.setInt(i++, wd.getTotalMinutes());
				ps.setInt(i++, wd.getScheduledMinutes());
				ps.setInt(i++, wd.getEarlyWorkMinutes());
				ps.setInt(i++, wd.getOvertimeMinutes());
				ps.setInt(i++, wd.getLateNightMinutes());
				ps.setInt(i++, wd.getLateEarlyMinutes());
				ps.setBoolean(i++, wd.isHoliday());
				ps.setBoolean(i++, wd.isSunday());
				ps.setBoolean(i++, wd.isBusinessTrip());
				ps.setBoolean(i++, wd.isAbsence());
				ps.setBoolean(i++, wd.isAmPaidHoliday());
				ps.setBoolean(i++, wd.isPmPaidHoliday());
				ps.setBoolean(i++, wd.isPaidHoliday());
				ps.setBoolean(i++, wd.isCompDay());
				ps.setObject(i++, now);
				ps.addBatch();
			}
			// null を返すと access() がロールバックしてしまうため、必ず件数を返す
			return Integer.valueOf(ps.executeBatch().length);
		}
	}
}
