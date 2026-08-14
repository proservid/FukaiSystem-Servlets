package fukaisystem.application.mh;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletResponse;

import fukaisystem.application.attendance.WorkScheduleReader;
import fukaisystem.domain.attendance.WorkSchedule;
import fukaisystem.dto.mh.InputDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 加工実績を登録・更新する
 *
 * <p>着手日時〜終了日時の実働時間は、就業時間マスタ（{@code M_就業時間}）の設定に従い
 * {@link WorkSchedule#calcWorkMinutes(int, int)} で算出する
 * （始業前の切り捨て・休憩・定時後休憩の控除）。
 * 出張の場合は就業時間を適用せず、入力された休憩時間のみを控除するため、
 * 就業時間マスタは参照しない。
 */
public class InputRegister extends ServiceFoundation {

	/** 出張の加工CD */
	private static final String BUSINESS_TRIP = "17";

	/** 加工実績の時間の刻み（分）。15分＝25 として登録する */
	private static final int TIME_UNIT_MINUTES = 15;

	/** 加工実績の時間の刻みあたりの値。15分＝25、1時間＝100 */
	private static final int TIME_UNIT_VALUE = 25;

	/** 時間の端数を切り上げる下限（分）。3捨4入のため 15分の 0.4 にあたる 6分以上を切り上げる */
	private static final int TIME_ROUND_UP_MINUTES = 6;

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		InputDTO inputDTO = cast(response, o, InputDTO.class);

		int year = inputDTO.getInt(2);
		int month = inputDTO.getInt(3) - 1;
		int day = inputDTO.getInt(4);
		int fromT = inputDTO.getInt(5);
		int fromM = inputDTO.getInt(6);
		int toT = inputDTO.getInt(7);
		int toM = inputDTO.getInt(8);
		int rest = inputDTO.getInt(9);
		int id = inputDTO.getInt(10);
		String processCD = inputDTO.getString(2);
		boolean isBusinessTrip = BUSINESS_TRIP.equals(processCD);

		Calendar cal = Calendar.getInstance();
		cal.set(year, month, day, fromT, fromM, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date from = new Date(cal.getTimeInMillis());
		cal.set(year, month, day, toT, toM, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date to = new Date(cal.getTimeInMillis());

		// 実働時間の算出（00:00 起点の分数で計算する）
		int fromMinutes = fromT * 60 + fromM;
		int toMinutes = toT * 60 + toM;
		int minutes;
		if (isBusinessTrip) {
			// 出張は就業時間を適用せず、入力された休憩時間のみを控除する（就業時間マスタは参照しない）
			minutes = Math.max(0, toMinutes - fromMinutes - rest);
		} else {
			WorkSchedule schedule = WorkScheduleReader.read(c);
			minutes = schedule.calcWorkMinutes(fromMinutes, toMinutes);
		}
		int time = toWorkTimeUnit(minutes);
		String note = isBusinessTrip ? "休憩" + rest + "分" : "";

		if (id == 0) {
			try (
				PreparedStatement ps = c.prepareStatement(
					"INSERT INTO T_加工実績 (製作期,製作番号,製作枝番,加工CD,時間,着手日時,終了日時,単価,担当者CD,備考,入力日時)"
						+ " VALUES(?,?,?,?,?,?,?,"
						+ "(select 単価 from M_加工_単価 wp1 where wp1.CD=? and 適用開始日<? AND NOT EXISTS ("
						+ "	SELECT 1 FROM M_加工_単価 wp2"
						+ "	WHERE wp1.適用開始日<wp2.適用開始日 AND wp1.CD=wp2.CD AND 適用開始日<?)),"
						+ "?,?,?)"
				);
			) {
				/*
				 * "MERGE INTO T_加工実績2 AS t"+//(製作期,製作番号,製作枝番,加工CD,時間,着手日時,終了日時,担当者CD,備考)" +
				 * " USING (SELECT ? AS 製作期, ? AS 製作番号, ? AS 製作枝番, ? AS 加工CD, ? AS 時間, ? AS 着手日時, ? AS 終了日時, ? AS 担当者CD, ? AS 備考) AS w"
				 * +
				 * " ON t.着手日時=w.着手日時 AND t.担当者CD=w.担当者CD" +
				 * " WHEN MATCHED THEN" +
				 * "   UPDATE SET t.製作期=w.製作期, t.製作番号=w.製作番号, t.製作枝番=w.製作枝番, t.加工CD=w.加工CD, t.時間=w.時間, t.着手日時=w.着手日時, t.終了日時=w.終了日時, t.担当者CD=w.担当者CD, t.備考=w.備考, t.入力年月日=?"
				 * +
				 * " WHEN NOT MATCHED THEN" +
				 * "   INSERT VALUES(w.製作期, w.製作番号, w.製作枝番, w.加工CD, w.時間, w.着手日時, w.終了日時, w.担当者CD, w.備考, ?)" +
				 * " OUTPUT deleted.ID as oldId, inserted.着手日時 as newId;");
				 */
				int i = 1;
				ps.setInt(i++, inputDTO.getInt(1) == 0 ? 0 : inputDTO.getInt(0)); // 番号が0なら期も0
				ps.setInt(i++, inputDTO.getInt(1));
				ps.setString(i++, inputDTO.getInt(1) == 0 ? "" : inputDTO.getString(3)); // 番号が0なら枝番なし
				ps.setString(i++, processCD); // 加工CD
				ps.setInt(i++, time); // 時間
				ps.setTimestamp(i++, new Timestamp(from.getTime())); // 着手日時
				ps.setTimestamp(i++, new Timestamp(to.getTime())); // 終了日時

				ps.setString(i++, processCD); // 加工CD
				ps.setTimestamp(i++, new Timestamp(from.getTime())); // 着手日時
				ps.setTimestamp(i++, new Timestamp(from.getTime())); // 着手日時

				ps.setString(i++, inputDTO.getString(1)); // 担当者CD
				ps.setString(i++, note); // 備考
				ps.setTimestamp(i, new Timestamp(new Date().getTime()));
				ps.executeUpdate();
			}
		} else {
			try (
				PreparedStatement ps = c.prepareStatement(
					"UPDATE T_加工実績  SET 製作期=?,製作番号=?,製作枝番=?,加工CD=?,時間=?,着手日時=?,終了日時=?,"
						+ "単価=(select 単価 from M_加工_単価 wp1 where wp1.CD=加工CD and 適用開始日<? AND NOT EXISTS ("
						+ "	SELECT 1 FROM M_加工_単価 wp2"
						+ "	WHERE wp1.適用開始日<wp2.適用開始日 AND wp1.CD=wp2.CD AND 適用開始日<?)),"
						+ "担当者CD=?,備考=?,入力日時=? WHERE ID=?"
				);
			) {
				int i = 1;
				ps.setInt(i++, inputDTO.getInt(1) == 0 ? 0 : inputDTO.getInt(0)); // 番号が0なら期も0
				ps.setInt(i++, inputDTO.getInt(1));
				ps.setString(i++, inputDTO.getInt(1) == 0 ? "" : inputDTO.getString(3)); // 番号が0なら枝番なし
				ps.setString(i++, processCD); // 加工CD
				ps.setInt(i++, time); // 時間
				ps.setTimestamp(i++, new Timestamp(from.getTime())); // 着手日時
				ps.setTimestamp(i++, new Timestamp(to.getTime())); // 終了日時
				ps.setTimestamp(i++, new Timestamp(from.getTime())); // 着手日時
				ps.setTimestamp(i++, new Timestamp(from.getTime())); // 着手日時
				ps.setString(i++, inputDTO.getString(1)); // 担当者CD
				ps.setString(i++, note); // 備考
				ps.setTimestamp(i++, new Timestamp(new Date().getTime()));
				ps.setInt(i, id);
				ps.executeUpdate();
			}
		}
		return -1;
	}

	/**
	 * 実働時間（分）を T_加工実績.時間 の単位に変換する
	 *
	 * <p>15分＝25、1時間＝100 の単位で、15分未満の端数は3捨4入する
	 * （15分の 0.4 にあたる 6分以上で切り上げ、5分以下は切り捨て）。
	 *
	 * @param minutes 実働時間（分）
	 *
	 * @return T_加工実績.時間 に登録する値
	 */
	private static int toWorkTimeUnit(int minutes) {
		int blocks = minutes / TIME_UNIT_MINUTES;
		int remainder = minutes % TIME_UNIT_MINUTES;
		return (remainder >= TIME_ROUND_UP_MINUTES) ? (blocks + 1) * TIME_UNIT_VALUE : blocks * TIME_UNIT_VALUE;
	}

}
