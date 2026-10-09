package fukaisystem.application.schedule;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

/**
 * 指定した日の祝日設定を切り替える
 *
 * <p>登録済みなら削除し、登録されていなければ登録する。
 * 削除できたかどうかを見てから登録するため、判定と登録の間に別の切り替えが割り込むと
 * 二重登録や切り替えの取りこぼしが起きる。これを防ぐため、ひとつのトランザクションにまとめ、
 * 判定の前に対象日をロックする。
 * {@code HOLDLOCK} は該当行がない場合に範囲ロックとなるため、未登録の日でも割り込みを防げる。
 */
public class SetHoliday extends ServiceFoundation {

	/** ロック・更新の対象を特定する条件 */
	private static final String KEY_CONDITION = "祝日=?";

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		Date holiday = cast(response, o, Date.class);
		if (holiday == null) {
			return null; // cast() がクライアントに送信済み
		}

		lock(c, "T_祝日", KEY_CONDITION, holiday);

		try (
			PreparedStatement ps = c.prepareStatement("DELETE FROM T_祝日 WHERE " + KEY_CONDITION);
		) {
			ps.setDate(1, holiday);
			if (ps.executeUpdate() == 0) { // 削除できなければ登録されていないので登録
				try (PreparedStatement ps2 = c.prepareStatement("INSERT INTO T_祝日 VALUES (?)");) {
					ps2.setDate(1, holiday);
					ps2.executeUpdate();
				}
			}
			return -1;
		}
	}
}
