package fukaisystem.application.aggregate;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

/**
 * テーブルの内容と列情報を取得するためのクラス
 * 
 * @author kameura
 *
 */
public class SetRelease extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		Date from = cast(response, o, Date.class);
		Calendar target = Calendar.getInstance();
		target.setTime(from); // 今月1日
		int month = target.get(Calendar.MONTH) + 1;
		target.add(Calendar.MONTH, 1); // 翌月1日
		Date to = new Date(target.getTimeInMillis());
		target.setTime(from); // 今月1日（次の2014年判定のためにこのタイミングでセットする必要がある）

		Date from2 = null;
		Date to2 = null;

		if (target.get(Calendar.YEAR) < 2014) { // 2013年以前は25日〆
			if (target.get(Calendar.MONTH) == 11) { // 12月は11月26日から12月31日
				to2 = to;
				target.add(Calendar.DATE, 25); // 今月26日
				target.add(Calendar.MONTH, -1); // 先月26日
				from2 = new Date(target.getTimeInMillis());
			} else {
				target.add(Calendar.DATE, 25); // 今月26日
				to2 = new Date(target.getTimeInMillis());
				if (target.get(Calendar.MONTH) == 0) { // 1月は1月1日から1月25日
					from2 = from;
				} else {
					target.add(Calendar.MONTH, -1); // 先月26日
					from2 = new Date(target.getTimeInMillis());
				}
			}
		} else { // 2014年以後は月末〆
			to2 = to;
			from2 = from;
		}

		String sql = "UPDATE T_指定納品書"
			+ " SET 〆FLG = 'false'"
			+ " WHERE 納品書日>=? and 納品書日<?";
		try (
			PreparedStatement ps = c.prepareStatement(sql);
		) {
			int n = 1;
			ps.setDate(n++, month == 1 ? from : from2); // 納品
			ps.setDate(n++, month == 12 ? to : to2); // 納品
			ps.executeUpdate();
			return false;
		}
	}
}