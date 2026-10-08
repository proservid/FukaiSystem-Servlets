package fukaisystem.application.staff;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

/**
 * 人員データを削除する
 *
 * <p>複数件の削除をひとつのトランザクションで行う。自動コミットのままでは
 * 途中で失敗したときに一部だけ削除された状態が確定してしまう。
 *
 * <p>工数データを持つ人員は削除できない。クライアントも削除前に判定しているが、
 * その判定は画面を開いた時点のデータに基づくため、判定から削除までの間に
 * 工数が登録されると孤立した工数データが残る。これを防ぐため、対象行をロックしたうえで
 * サーバ側でも工数の有無を確かめてから削除する。
 */
public class DeleteMember extends ServiceFoundation {

	@SuppressWarnings("unchecked")
	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		List<Integer> delMembers = cast(response, o, List.class);
		if (delMembers == null) {
			return null; // cast() がクライアントに送信済み
		}
		if (delMembers.isEmpty()) {
			return Integer.valueOf(0);
		}

		// 同じ人員を対象とする削除どうしがデッドロックしないよう、CDの昇順にロックする
		List<Integer> targets = new ArrayList<Integer>(delMembers);
		Collections.sort(targets);
		for (int cd : targets) {
			lock(c, "M_人員", "CD=?", cd);
		}

		// クライアントの判定は画面を開いた時点のものなので、削除の直前に取り直す
		try (
			PreparedStatement ps = c.prepareStatement("SELECT 1 FROM T_加工実績 WHERE 担当者CD=?");
		) {
			for (int cd : targets) {
				ps.setInt(1, cd);
				try (ResultSet rs = ps.executeQuery();) {
					if (rs.next()) {
						throw new SQLException("工数データがある人は削除できません。先に工数データを削除してください");
					}
				}
			}
		}

		try (
			PreparedStatement ps = c.prepareStatement("delete from M_人員 where CD=?");
		) {
			for (int m : targets) {
				ps.setInt(1, m);
				ps.addBatch();
			}
			return ps.executeBatch().length;
		}
	}
}
