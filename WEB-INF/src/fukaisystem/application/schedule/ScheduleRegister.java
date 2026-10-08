package fukaisystem.application.schedule;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletResponse;

import fukaisystem.dto.schedule.Banner;
import fukaisystem.dto.schedule.Daily2;
import fukaisystem.dto.schedule.Repeat;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 予定・繰り返し・バナーを登録する
 *
 * <p>繰り返しとバナーは対象の人員分をいったん消してから入れ直すため、
 * 消してから入れ終えるまでの間に同じ人員への登録が割り込むと、
 * 一方の入力が丸ごと消える。これを防ぐため、削除の前に対象人員の行をロックする。
 * デッドロックを避けるため、ロックは人員CDの昇順に取る。
 *
 * <p>予定の MERGE には {@code WITH (HOLDLOCK)} を指定する。既定の READ COMMITTED では
 * 一致判定のために取った共有ロックが判定直後に解放されるため、判定と登録の間に
 * 同じ人員CD・年月日の登録が割り込み、主キー違反になることがある。
 */
public class ScheduleRegister extends ServiceFoundation {

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		Daily2 daily = cast(response, o, Daily2.class);
		if (daily == null) {
			return null; // cast() がクライアントに送信済み
		}
		String myCD = daily.getCD();
		Object[] members = daily.getMembers();
		int withBit = daily.getWithBit();
		int limit = 0;

		if ((withBit & Daily2.DAILY) == Daily2.DAILY) {
			limit = members.length;
		} else {
			limit = 0;
		}
		try (
			PreparedStatement ps = c.prepareStatement(
				"MERGE INTO T_予定 WITH (HOLDLOCK) AS y"
					+ " USING (SELECT ? AS 人員CD, ? AS 年月日, ? AS 内容, ? AS 登録日時) AS w"
					+ " ON y.人員CD=w.人員CD AND y.年月日=w.年月日"
					+ " WHEN MATCHED THEN"
					+ "   UPDATE SET y.人員CD=w.人員CD, y.年月日=w.年月日, y.内容=w.内容, y.登録日時=w.登録日時 "
					+ " WHEN NOT MATCHED THEN"
					+ "   INSERT VALUES(w.人員CD, w.年月日, w.内容, w.登録日時);"
			);
		) {
			for (int i = 0; i <= limit; i++) {
				if (i == limit)
					ps.setString(1, myCD); // 自分
				else
					ps.setString(1, members[i].toString()); // 同行者
				ps.setDate(2, daily.getDate());
				ps.setString(3, daily.getText());
				ps.setTimestamp(4, new Timestamp(new java.util.Date().getTime()));
				ps.addBatch();
			}
			ps.executeBatch();
		}

		if ((withBit & Daily2.REPEAT) == Daily2.REPEAT) {
			limit = members.length;
		} else {
			limit = 0;
		}
		// 消してから入れ終えるまで、同じ人員への登録を割り込ませない
		lockMembers(c, "T_繰り返し", myCD, members, limit);
		StringBuilder query = new StringBuilder("DELETE FROM T_繰り返し WHERE 人員CD IN (?");
		for (int i = 0; i < limit; i++) {
			query.append(",?");
		}
		query.append(")");
		try (
			PreparedStatement ps = c.prepareStatement(query.toString());
		) {
			for (int i = 0; i <= limit; i++) {
				if (i == limit)
					ps.setString(i + 1, myCD); // 自分
				else
					ps.setString(i + 1, members[i].toString()); // 同行者
			}
			ps.executeUpdate();
		}

		try (
			PreparedStatement ps = c.prepareStatement(
				"INSERT INTO T_繰り返し (人員CD,曜日ごとFLG,ごと,内容,登録日時) VALUES(?,?,?,?,?)"
			);
		) {
			for (Repeat r : daily.getRepeats()) {
				if (r.getValue() > 0 && !r.getContent().equals("")) {
					for (int i = 0; i <= limit; i++) {
						if (i == limit)
							ps.setString(1, myCD); // 自分
						else
							ps.setString(1, members[i].toString()); // 同行者
						ps.setBoolean(2, r.isWeekly());
						ps.setInt(3, r.getValue());
						ps.setString(4, r.getContent());
						ps.setTimestamp(5, new Timestamp(new java.util.Date().getTime()));
						ps.addBatch();
					}
				}
			}
			ps.executeBatch();
		}

		if ((withBit & Daily2.BANNER) == Daily2.BANNER) {
			limit = members.length;
		} else {
			limit = 0;
		}
		// 消してから入れ終えるまで、同じ人員への登録を割り込ませない
		lockMembers(c, "T_バナー", myCD, members, limit);
		query = new StringBuilder("DELETE FROM T_バナー WHERE 人員CD IN (?");
		for (int i = 0; i < limit; i++) {
			query.append(",?");
		}
		query.append(") AND (開始日<=? and 終了日>=?)");
		if (daily.getIds().size() > 0) {
			query.append(" AND (");
			boolean flg = false;
			for (Integer i : daily.getIds()) {
				if (flg) {
					query.append(" OR ");
				} else
					flg = true;
				query.append("ID=" + i);
			}
			query.append(")");
		}
		try (
			PreparedStatement ps = c.prepareStatement(query.toString());
		) {
			for (int i = 0; i <= limit; i++) {
				if (i == limit)
					ps.setString(i + 1, myCD); // 自分
				else
					ps.setString(i + 1, members[i].toString()); // 同行者
			}
			ps.setDate(limit + 2, daily.getDate());
			ps.setDate(limit + 3, daily.getDate());
			ps.executeUpdate();
		}

		try (
			PreparedStatement ps = c.prepareStatement(
				"INSERT INTO T_バナー (人員CD,開始日,終了日,内容,登録日時) VALUES(?,?,?,?,?)"
			);
		) {
			for (Banner b : daily.getBanners()) {
				if (!b.getContent().equals("")) {
					for (int i = 0; i <= limit; i++) {
						if (i == limit)
							ps.setString(1, myCD); // 自分
						else
							ps.setString(1, members[i].toString()); // 同行者
						ps.setDate(2, b.getFrom());
						ps.setDate(3, b.getTo());
						ps.setString(4, b.getContent());
						ps.setTimestamp(5, new Timestamp(new java.util.Date().getTime()));
						ps.addBatch();
					}
				}
			}
			ps.executeBatch();
		}
		return -1;
	}

	/**
	 * 登録対象の人員の行をロックする
	 *
	 * <p>デッドロックを避けるため、人員CDの昇順にロックを取る。
	 *
	 * @param c Connectionオブジェクト
	 * @param table ロック対象のテーブル名
	 * @param myCD 自分の人員CD
	 * @param members 同行者の人員CD
	 * @param limit 同行者を対象に含める件数。0 なら自分だけが対象
	 *
	 * @throws SQLException
	 */
	private void lockMembers(Connection c, String table, String myCD, Object[] members, int limit)
		throws SQLException {

		List<String> targets = new ArrayList<String>();
		targets.add(myCD); // 自分
		for (int i = 0; i < limit; i++) {
			targets.add(members[i].toString()); // 同行者
		}
		Collections.sort(targets);
		for (String cd : targets) {
			lock(c, table, "人員CD=?", cd);
		}
	}

}
