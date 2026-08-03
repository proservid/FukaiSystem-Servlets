package fukaisystem.application.group;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.servlet.ServletResponse;

import fukaisystem.dto.Banner;
import fukaisystem.dto.Daily2;
import fukaisystem.dto.Repeat;
import fukaisystem.foundation.ServiceFoundation;

public class ScheduleRegister extends ServiceFoundation {

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		Daily2 daily = cast(response, o, Daily2.class);
		String myCD = daily.getCD();
		Object[] members = daily.getMembers();
		int withBit = daily.getWithBit();
		int limit = 0;

		if ((withBit & Daily2.DAILY) == Daily2.DAILY) {
			limit = members.length;
		} else {
			limit = 0;
		}
		PreparedStatement ps = c.prepareStatement(
			"MERGE INTO T_予定 AS y"
				+ " USING (SELECT ? AS 人員CD, ? AS 年月日, ? AS 内容, ? AS 登録日時) AS w"
				+ " ON y.人員CD=w.人員CD AND y.年月日=w.年月日"
				+ " WHEN MATCHED THEN"
				+ "   UPDATE SET y.人員CD=w.人員CD, y.年月日=w.年月日, y.内容=w.内容, y.登録日時=w.登録日時 "
				+ " WHEN NOT MATCHED THEN"
				+ "   INSERT VALUES(w.人員CD, w.年月日, w.内容, w.登録日時);"
		);
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

		if ((withBit & Daily2.REPEAT) == Daily2.REPEAT) {
			limit = members.length;
		} else {
			limit = 0;
		}
		StringBuilder query = new StringBuilder("DELETE FROM T_繰り返し WHERE 人員CD IN (?");
		for (int i = 0; i < limit; i++) {
			query.append(",?");
		}
		query.append(")");
		ps = c.prepareStatement(query.toString());
		for (int i = 0; i <= limit; i++) {
			if (i == limit)
				ps.setString(i + 1, myCD); // 自分
			else
				ps.setString(i + 1, members[i].toString()); // 同行者
		}
		ps.executeUpdate();

		ps = c.prepareStatement(
			"INSERT INTO T_繰り返し (人員CD,曜日ごとFLG,ごと,内容,登録日時) VALUES(?,?,?,?,?)"
		);
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

		if ((withBit & Daily2.BANNER) == Daily2.BANNER) {
			limit = members.length;
		} else {
			limit = 0;
		}
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
		ps = c.prepareStatement(query.toString());
		for (int i = 0; i <= limit; i++) {
			if (i == limit)
				ps.setString(i + 1, myCD); // 自分
			else
				ps.setString(i + 1, members[i].toString()); // 同行者
		}
		ps.setDate(limit + 2, daily.getDate());
		ps.setDate(limit + 3, daily.getDate());
		ps.executeUpdate();

		ps = c.prepareStatement(
			"INSERT INTO T_バナー (人員CD,開始日,終了日,内容,登録日時) VALUES(?,?,?,?,?)"
		);
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
		return -1;
	}

}
