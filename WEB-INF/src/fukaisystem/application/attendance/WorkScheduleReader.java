package fukaisystem.application.attendance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalTime;

import fukaisystem.domain.attendance.WorkSchedule;

/**
 * 就業時間マスタ（{@code M_就業時間}）を読み込んで {@link WorkSchedule} を組み立てる。
 *
 * <p>{@code M_就業時間} は暫定テーブルであり、列構成が変わる可能性がある。
 * <b>テーブルの列を知っているのは本クラスだけ</b>にすること。
 * 列が増減した場合に修正するのは本クラスのみで、
 * {@link WorkSchedule} やその利用側（日次勤怠集計・加工実績入力）には影響させない。
 *
 * <p>時刻の列は {@code time(0)} 型、計算単位・上限の列は時間ではなく分数のため {@code int} 型。
 *
 * <p>{@code fukaisystem.domain.attendance} パッケージは DBアクセスを行わない設計のため、
 * 読み込み処理は application 層に置いている。
 */
public final class WorkScheduleReader {

	private WorkScheduleReader() {
		throw new AssertionError("インスタンス化できません");
	}

	/**
	 * 就業時間マスタから就業時間を読み込む。
	 *
	 * <p>複数行が登録されている場合は先頭の1行を採用する。
	 *
	 * @param c Connectionオブジェクト
	 *
	 * @return 就業時間マスタの内容から組み立てた就業時間
	 * @throws SQLException 就業時間マスタが未登録の場合、内容が不正な場合、または読み込みに失敗した場合
	 */
	public static WorkSchedule read(Connection c) throws SQLException {
		try (
			PreparedStatement ps = c.prepareStatement("SELECT * FROM M_就業時間");
		) {
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				throw new SQLException("就業時間マスタ（M_就業時間）が登録されていません");
			}
			return WorkSchedule.builder()
				.startTime(time(rs, "始業"))
				.endTime(time(rs, "終業"))
				.breakStartTime(time(rs, "休憩開始"))
				.breakEndTime(time(rs, "休憩終了"))
				.businessTripBreakEndTime(time(rs, "出張休憩終了"))
				.overtimeStartTime(time(rs, "残業開始"))
				.overtimeUnit(rs.getInt("残業単位分"))
				.earlyWorkStartTime(time(rs, "早出開始"))
				.earlyWorkUnit(rs.getInt("早出単位分"))
				.lateNightStartTime(time(rs, "深夜開始"))
				.lateNightEndTime(time(rs, "深夜終了"))
				.maxWorkMinutes(rs.getInt("労働上限分"))
				.build();
		} catch (IllegalStateException | IllegalArgumentException e) {
			// 就業時間マスタの内容が不正（前後関係の矛盾・範囲外の値など）
			throw new SQLException("就業時間マスタ（M_就業時間）の内容が不正です: " + e.getMessage(), e);
		}
	}

	/**
	 * {@code time(0)} 型の列を {@link LocalTime} として取得する
	 *
	 * @param rs ResultSetオブジェクト
	 * @param columnName 列名
	 *
	 * @return 列の時刻
	 * @throws SQLException 列が未設定（NULL）の場合
	 */
	private static LocalTime time(ResultSet rs, String columnName) throws SQLException {
		Time value = rs.getTime(columnName);
		if (value == null) {
			throw new SQLException("就業時間マスタ（M_就業時間）の " + columnName + " が未設定です");
		}
		return value.toLocalTime();
	}
}
