package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

/**
 * 指定した登録データを削除する
 *
 * <p>親と明細をひとつのトランザクションで削除する。別々にコミットすると、
 * 親の削除だけが成立して明細が孤児として残ることがある。
 *
 * <p>削除の前に親の行をロックし、削除中に同じデータの更新が割り込んで
 * 明細だけが復活するのを防ぐ。明細は必ず親を経由して操作されるため、
 * 明細テーブルをロックする必要はない。
 */
public class DeleteData extends ServiceFoundation {

	/** 種別ごとの親テーブル名。添字は種別（0は未使用） */
	private static final String[] PARENT_TABLES = { null, "T_見積_親", "T_製作_親", "T_在庫_親", "T_売上_親", "T_出庫_親" };

	/** 種別ごとの明細テーブル名。添字は種別（0は未使用） */
	private static final String[] CHILD_TABLES = { null, "T_見積_子", "T_製作_子", "T_在庫_子", "T_売上_子", "T_出庫_子" };

	/** 種別ごとの親IDの列名。添字は種別（0は未使用） */
	private static final String[] KEY_COLUMNS = { null, "見積親ID", "製作親ID", "在庫親ID", "売上親ID", "出庫親ID" };

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		Integer[] param = cast(response, o, Integer[].class);
		if (param == null) {
			return null; // cast() がクライアントに送信済み
		}
		int type = param[0];
		int id = param[1];

		if (type < 1 || type >= PARENT_TABLES.length) {
			return Integer.valueOf(0); // 対象外の種別は何もしない
		}
		String parentTable = PARENT_TABLES[type];
		String childTable = CHILD_TABLES[type];
		String condition = KEY_COLUMNS[type] + "=?";

		lock(c, parentTable, condition, id);

		int count = 0;
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + parentTable + " WHERE " + condition);) {
			ps.setInt(1, id);
			count += ps.executeUpdate();
		}
		// 明細
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + childTable + " WHERE " + condition);) {
			ps.setInt(1, id);
			count += ps.executeUpdate();
		}
		// null を返すと access() がロールバックしてしまうため、必ず件数を返す
		return Integer.valueOf(count);
	}

}
