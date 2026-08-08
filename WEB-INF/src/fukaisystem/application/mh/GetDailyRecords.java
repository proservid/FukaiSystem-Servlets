package fukaisystem.application.mh;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.dto.DailyInputDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 指定した担当者が指定日に入力した加工実績の一覧を取得するためのクラス
 * <p>
 * 列見出しはクライアントが固定で持っているため、列の順序を変更してはならない。
 * 順序は 連番, 製番, CD, 工程名, 日付, 開始, 終了 とする。
 *
 * @author kameura
 *
 */
public class GetDailyRecords extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		DailyInputDTO condition = cast(response, o, DailyInputDTO.class);
		if (condition == null) {
			return null;
		}

		Calendar nextDay = Calendar.getInstance();
		nextDay.setTime(condition.getDate());
		nextDay.add(Calendar.DATE, 1);

		Vector<Vector<Object>> records = new Vector<Vector<Object>>();

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT w.ID AS 連番,"
					+ " RIGHT('00' + CAST(w.製作期 AS varchar), 2)+'-'"
					+ "+RIGHT('0000' + CAST(w.製作番号 AS varchar), 4)+w.製作枝番 AS 製番,"
					+ " w.加工CD AS CD,"
					+ " k.小分類名 AS 工程名,"
					+ " CONVERT(varchar, w.着手日時, 111) AS 日付,"
					+ " LEFT(CONVERT(varchar, w.着手日時, 108), 5) AS 開始,"
					+ " LEFT(CONVERT(varchar, w.終了日時, 108), 5) AS 終了"
					+ " FROM T_加工実績 w"
					+ " LEFT OUTER JOIN M_加工_子 k ON w.加工CD=k.CD"
					+ " WHERE w.入力日時>=? AND w.入力日時<? AND w.担当者CD=?"
					+ " ORDER BY 製番"
			);
		) {
			ps.setDate(1, condition.getDate());
			ps.setDate(2, new Date(nextDay.getTimeInMillis()));
			ps.setString(3, condition.getStaffCD());

			try (ResultSet rs = ps.executeQuery();) {
				int columnCount = rs.getMetaData().getColumnCount();
				while (rs.next()) {
					Vector<Object> record = new Vector<Object>(columnCount);
					for (int i = 1; i <= columnCount; i++) {
						Object element = rs.getObject(i);
						if (rs.wasNull()) {
							element = "";
						} else if (element instanceof String) {
							element = ((String) element).trim();
						}
						record.add(element);
					}
					records.add(record);
				}
			}
		}

		return records;
	}
}
