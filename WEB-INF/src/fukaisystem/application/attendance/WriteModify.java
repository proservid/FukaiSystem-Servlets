package fukaisystem.application.attendance;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.sql.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletResponse;

import fukaisystem.dto.attendance.ModifiedDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 打刻修正データを登録し、修正日時を返す
 *
 * <p>連番は {@code (SELECT COUNT(*) + 1 …)} で採番するため、数える処理と登録の間に
 * 同じ年月日・人員の登録が割り込むと連番が重複する。これを防ぐため、
 * 登録の前に対象年月日の範囲をロックし、以降の登録を待たせる。
 * また複数件をひとつのトランザクションで登録するので、途中で失敗しても
 * 一部だけ登録された状態にはならない。
 */
public class WriteModify extends ServiceFoundation {

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		ModifiedDTO dto = cast(response, o, ModifiedDTO.class);
		if (dto == null) {
			return null; // cast() がクライアントに送信済み
		}
		Date date = new Date(dto.getDate().getTime()); // java.util.Date を java.sql.Date に変換
		Map<Integer, List<Object>> map = dto.getMap();

		if (date == null || map == null) {
			return null;
		}

		// 連番を数えてから登録し終えるまで、同じ年月日への登録を割り込ませない
		lock(c, "T_打刻修正", "年月日=?", date);

		LocalDateTime now = LocalDateTime.now();
		try (
			PreparedStatement ps = c.prepareStatement(
				"INSERT INTO T_打刻修正 VALUES(?, ?, (SELECT COUNT(*) + 1 FROM T_打刻修正 WHERE 年月日=? AND 人員CD=?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
			);
		) {
			for (Map.Entry<Integer, List<Object>> m : map.entrySet()) {
				int i = 1;
				ps.setDate(i++, date);
				ps.setInt(i++, m.getKey());
				ps.setDate(i++, date);
				ps.setInt(i++, m.getKey());
				for (Object value : m.getValue()) {
					ps.setObject(i++, value == null ? null : value);
				}
				ps.setObject(i++, now);
				ps.setNull(i++, Types.TIMESTAMP);
				ps.addBatch();
			}
			ps.executeBatch();
		}

		return now; // 修正日時。null を返すと access() がロールバックしてしまう
	}
}
