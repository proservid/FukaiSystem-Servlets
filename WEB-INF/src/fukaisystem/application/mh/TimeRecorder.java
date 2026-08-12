package fugasystem.application.mh;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.servlet.ServletResponse;

import fugasystem.application.attendance.WorkScheduleReader;
import fugasystem.domain.attendance.WorkIntervals;
import fugasystem.domain.attendance.WorkSchedule;
import fugasystem.domain.mh.TimeRecord;
import fugasystem.foundation.ServiceFoundation;

/**
 * 加工実績を打刻で登録する
 *
 * <p>打刻は着手・終了・取消の3種類で、それぞれ次のように処理する。
 * <ul>
 *   <li>着手 … 仕掛テーブル（{@code W_着手}）に1件登録する。加工実績は作らない。
 *       同じ担当者・加工の着手データがすでにある場合は、製番が異なっていてもエラーとする
 *       （終了と間違えて着手を打刻した場合や、終了を打ち忘れたまま翌日着手した場合を検知するため）</li>
 *   <li>終了 … {@code W_着手} から着手データを取り出し、加工実績（{@code T_加工実績}）に登録したうえで
 *       取り出した着手データを {@code W_着手} から削除する</li>
 *   <li>取消 … {@code W_着手} の着手データを削除する。加工実績は作らない</li>
 * </ul>
 *
 * <p>{@code W_着手} の主キーは担当者CD＋加工CDのため、1人の担当者が1つの加工について
 * 同時に持てる着手は1件だけになる。終了・取消の打刻では、送られた製番が着手データの製番と
 * 一致することを確かめてから処理する（別の製番を終了・取消してしまう誤打を防ぐため）。
 *
 * <p>着手日時〜終了日時の実働時間は、就業時間マスタ（{@code M_就業時間}）の設定に従い
 * {@link WorkSchedule#calcWorkMinutes(int, int)} で算出する
 * （始業前の切り捨て・休憩・定時後休憩の控除）。
 * 日付をまたぐ場合は着手日側と終了日側に区間を分けて算出し、合計する。
 * 着手から24時間以上経過している場合は打刻の消し忘れとみなしてエラーとする。
 *
 * <p>出張（加工CD {@value #BUSINESS_TRIP}）は休憩時間を別途入力する必要があるため打刻では扱わない。
 * 出張は {@link InputRegister} による手入力で登録する。
 */
public class TimeRecorder extends ServiceFoundation {

	/** 出張の加工CD。打刻では扱わない */
	private static final int BUSINESS_TRIP = 17;

	/** 加工実績の時間の刻み（分）。15分＝25 として登録する */
	private static final int TIME_UNIT_MINUTES = 15;

	/** 加工実績の時間の刻みあたりの値。15分＝25、1時間＝100 */
	private static final int TIME_UNIT_VALUE = 25;

	/** 時間の端数を切り上げる下限（分）。3捨4入のため 15分の 0.4 にあたる 6分以上を切り上げる */
	private static final int TIME_ROUND_UP_MINUTES = 6;

	/** W_着手 の着手データを特定する条件。主キーのため最大1件に定まる */
	private static final String KEY_CONDITION = " WHERE 担当者CD=? AND 加工CD=?";

	@Override
	protected Object transaction(Connection c, ServletResponse response, Object o) throws Exception {

		TimeRecord record = cast(response, o, TimeRecord.class);
		if (record == null) {
			return null; // cast() がクライアントに送信済み
		}

		if (record.getDatetime() == null) {
			addError("打刻日時が指定されていません");
			return null;
		}
		if (record.getProcess() == BUSINESS_TRIP) {
			addError("出張は打刻では登録できません。工数入力から登録してください");
			return null;
		}

		if (record.isCancel()) { // 着手の取消では着手のフラグも立ちうるため取消を先に判定する
			return cancel(c, record);
		}
		if (record.isBegin()) {
			return begin(c, record);
		}
		return end(c, record);
	}

	/**
	 * 着手の打刻を W_着手 に登録する
	 *
	 * @param c Connectionオブジェクト
	 * @param record 打刻
	 *
	 * @return 登録できた場合は {@code Boolean#TRUE}、すでに着手済みの場合は {@code null}
	 * @throws SQLException
	 */
	private Object begin(Connection c, TimeRecord record) throws SQLException {

		if (readBegun(c, record) != null) { // 製番が異なっていても、終了していない着手があれば打刻させない
			addError("すでに着手が打刻されています");
			return null;
		}

		try (
			PreparedStatement ps = c.prepareStatement(
				"INSERT INTO W_着手 (担当者CD,所属部署CD,加工CD,製作期,製作番号,製作枝番,日時) VALUES(?,?,?,?,?,?,?)"
			);
		) {
			int i = 1;
			ps.setInt(i++, record.getWorker()); // 担当者CD
			ps.setInt(i++, record.getDept()); // 所属部署CD
			ps.setInt(i++, record.getProcess()); // 加工CD
			ps.setInt(i++, period(record)); // 製作期
			ps.setInt(i++, record.getNumber()); // 製作番号
			ps.setString(i++, branch(record)); // 製作枝番
			ps.setTimestamp(i, timestamp(record.getDatetime())); // 日時
			ps.executeUpdate();
		}
		return Boolean.TRUE;
	}

	/**
	 * 終了の打刻を受けて、W_着手 の着手データと合わせて加工実績を登録し、着手データを削除する
	 *
	 * @param c Connectionオブジェクト
	 * @param record 打刻
	 *
	 * @return 登録できた場合は {@code Boolean#TRUE}、着手データがない場合、製番が異なる場合、
	 *         または打刻の前後が不正な場合は {@code null}
	 * @throws SQLException
	 */
	private Object end(Connection c, TimeRecord record) throws SQLException {

		LocalDateTime to = record.getDatetime().truncatedTo(ChronoUnit.MINUTES);
		TimeRecord begun = readBegun(c, record);
		if (begun == null) {
			addError("着手が打刻されていません");
			return null;
		}
		if (!isSameProduct(record, begun)) {
			addError("着手した製番と異なります");
			return null;
		}

		LocalDateTime from = begun.getDatetime();
		if (!to.isAfter(from)) {
			addError("終了日時が着手日時より前です");
			return null;
		}
		if (!to.isBefore(from.plusDays(1))) {
			addError("着手から24時間以上経過しています。工数入力から登録してください");
			return null;
		}

		WorkSchedule schedule = WorkScheduleReader.read(c);
		int time = toWorkTimeUnit(calcWorkMinutes(schedule, from, to));

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
			int i = 1;
			ps.setInt(i++, period(record)); // 製作期
			ps.setInt(i++, record.getNumber()); // 製作番号
			ps.setString(i++, branch(record)); // 製作枝番
			ps.setInt(i++, record.getProcess()); // 加工CD
			ps.setInt(i++, time); // 時間
			ps.setTimestamp(i++, timestamp(from)); // 着手日時
			ps.setTimestamp(i++, timestamp(to)); // 終了日時

			ps.setInt(i++, record.getProcess()); // 加工CD
			ps.setTimestamp(i++, timestamp(from)); // 着手日時
			ps.setTimestamp(i++, timestamp(from)); // 着手日時

			ps.setInt(i++, record.getWorker()); // 担当者CD
			ps.setString(i++, ""); // 備考（休憩時間を入力する出張は打刻では扱わないため常に空）
			ps.setTimestamp(i, new Timestamp(new Date().getTime())); // 入力日時
			ps.executeUpdate();
		}

		delete(c, record);
		return Boolean.TRUE;
	}

	/**
	 * 着手の取消を受けて、W_着手 の着手データを削除する
	 *
	 * @param c Connectionオブジェクト
	 * @param record 打刻
	 *
	 * @return 削除できた場合は {@code Boolean#TRUE}、着手データがない場合や製番が異なる場合は {@code null}
	 * @throws SQLException
	 */
	private Object cancel(Connection c, TimeRecord record) throws SQLException {

		TimeRecord begun = readBegun(c, record);
		if (begun == null) {
			addError("着手が打刻されていません");
			return null;
		}
		if (!isSameProduct(record, begun)) { // 別の製番の着手を消してしまわないようにする
			addError("着手した製番と異なります");
			return null;
		}

		delete(c, record);
		return Boolean.TRUE;
	}

	/**
	 * W_着手 から着手データを取得する
	 *
	 * <p>{@code UPDLOCK} で更新ロックを、{@code HOLDLOCK} で該当行がない場合の範囲ロックを取り、
	 * トランザクションが終わるまで保持する。これがないと {@link #begin(Connection, TimeRecord)} の
	 * 存在チェックと登録の間に別の打刻が割り込み、主キー違反の例外がそのままクライアントに返ってしまう。
	 * ロックを取ることで後から来た打刻は待たされ、待ち明けに先の着手データを読んで
	 * 「すでに着手が打刻されています」を返せる。
	 * 更新ロックは互いに非互換のため、待ち合わせがデッドロックになることはない。
	 *
	 * @param c Connectionオブジェクト
	 * @param record 打刻
	 *
	 * @return 着手データ。着手が打刻されていない場合は {@code null}
	 * @throws SQLException
	 */
	private TimeRecord readBegun(Connection c, TimeRecord record) throws SQLException {
		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT 所属部署CD,製作期,製作番号,製作枝番,日時 FROM W_着手 WITH (UPDLOCK, HOLDLOCK)" + KEY_CONDITION
			);
		) {
			setKey(ps, record);
			try (ResultSet rs = ps.executeQuery();) {
				if (!rs.next()) {
					return null;
				}
				Timestamp datetime = rs.getTimestamp("日時");
				if (datetime == null) {
					return null;
				}
				String branch = rs.getString("製作枝番");
				return new TimeRecord(
					record.getWorker(),
					rs.getInt("所属部署CD"),
					record.getProcess(),
					rs.getInt("製作期"),
					rs.getInt("製作番号"),
					branch == null ? "" : branch.trim(),
					datetime.toLocalDateTime(),
					true,
					false
				);
			}
		}
	}

	/**
	 * 打刻の製番が着手データの製番と一致するかどうかを取得する
	 *
	 * <p>W_着手 には製作番号が0のときの正規化を済ませた値が入っているため、
	 * 打刻側も同じ正規化を通してから突き合わせる。
	 *
	 * @param record 打刻
	 * @param begun 着手データ
	 *
	 * @return 製番が一致する場合は {@code true}
	 */
	private static boolean isSameProduct(TimeRecord record, TimeRecord begun) {
		return period(record) == begun.getPeriod()
			&& record.getNumber() == begun.getNumber()
			&& branch(record).equals(begun.getBranch());
	}

	/**
	 * W_着手 から着手データを削除する
	 *
	 * @param c Connectionオブジェクト
	 * @param record 打刻
	 *
	 * @return 削除した件数
	 * @throws SQLException
	 */
	private int delete(Connection c, TimeRecord record) throws SQLException {
		try (
			PreparedStatement ps = c.prepareStatement("DELETE FROM W_着手" + KEY_CONDITION);
		) {
			setKey(ps, record);
			return ps.executeUpdate();
		}
	}

	/**
	 * {@link #KEY_CONDITION} のプレースホルダに着手データを特定する値を設定する
	 *
	 * @param ps PreparedStatementオブジェクト
	 * @param record 打刻
	 *
	 * @throws SQLException
	 */
	private void setKey(PreparedStatement ps, TimeRecord record) throws SQLException {
		int i = 1;
		ps.setInt(i++, record.getWorker()); // 担当者CD
		ps.setInt(i, record.getProcess()); // 加工CD
	}

	/**
	 * 着手日時から終了日時までの実働時間（分）を算出する
	 *
	 * <p>日付をまたぐ場合は着手日側と終了日側に区間を分け、それぞれに就業時間を適用して合計する
	 * （{@link WorkSchedule#calcWorkMinutes(int, int)} は日付またぎを扱わないため）。
	 * 着手から24時間未満であることを呼び出し元が保証するので、またぐのは翌日までの1回だけになる。
	 *
	 * @param schedule 就業時間
	 * @param from 着手日時
	 * @param to 終了日時
	 *
	 * @return 実働時間（分）
	 */
	private static int calcWorkMinutes(WorkSchedule schedule, LocalDateTime from, LocalDateTime to) {
		int fromMinutes = from.getHour() * 60 + from.getMinute();
		int toMinutes = to.getHour() * 60 + to.getMinute();
		if (from.toLocalDate().equals(to.toLocalDate())) {
			return schedule.calcWorkMinutes(fromMinutes, toMinutes);
		}
		return schedule.calcWorkMinutes(fromMinutes, WorkIntervals.MINUTES_PER_DAY)
			+ schedule.calcWorkMinutes(0, toMinutes);
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

	/**
	 * 登録する製作期を取得する
	 *
	 * @param record 打刻
	 *
	 * @return 製作期。製作番号が0の場合は0
	 */
	private static int period(TimeRecord record) {
		return record.getNumber() == 0 ? 0 : record.getPeriod(); // 番号が0なら期も0
	}

	/**
	 * 登録する製作枝番を取得する
	 *
	 * @param record 打刻
	 *
	 * @return 製作枝番。製作番号が0の場合や未指定の場合は空文字（列が NOT NULL のため）
	 */
	private static String branch(TimeRecord record) {
		if (record.getNumber() == 0 || record.getBranch() == null) { // 番号が0なら枝番なし
			return "";
		}
		return record.getBranch();
	}

	/**
	 * 日時を分に切り捨てて Timestamp に変換する
	 *
	 * <p>W_着手.日時 が {@code smalldatetime}（分精度で秒を四捨五入する）のため、
	 * 登録前に切り捨てて着手日時と終了日時の値を予測できるようにする。
	 *
	 * @param datetime 日時
	 *
	 * @return 秒以下を切り捨てた Timestamp
	 */
	private static Timestamp timestamp(LocalDateTime datetime) {
		return Timestamp.valueOf(datetime.truncatedTo(ChronoUnit.MINUTES));
	}

}
