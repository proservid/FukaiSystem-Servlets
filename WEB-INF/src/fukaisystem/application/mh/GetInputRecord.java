package fukaisystem.application.mh;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;

import javax.servlet.ServletResponse;

import fukaisystem.dto.mh.InputDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 訂正対象の加工実績を1件取得するためのクラス
 *
 * @author kameura
 *
 */
public class GetInputRecord extends ServiceFoundation {

	/** 出張の加工CD（備考に休憩時間が記録される） */
	private static final String BUSINESS_TRIP = "17";

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws Exception {

		Integer id = cast(response, o, Integer.class);
		if (id == null) {
			return null;
		}

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT"
					+ " RIGHT('00' + CONVERT(varchar, p.所属部署CD), 2) AS 所属部署CD,"
					+ " CASE WHEN p.所属部署CD IN(2,3,4) THEN RIGHT('00' + CONVERT(varchar, p.CD), 2)"
					+ " ELSE CONVERT(varchar, w.担当者CD) END AS 担当者CD,"
					+ " RIGHT('00' + CONVERT(varchar, w.加工CD), 2) AS 加工CD,"
					+ " w.製作期, w.製作番号, w.製作枝番, w.着手日時, w.終了日時, w.備考"
					+ " FROM T_加工実績 w LEFT OUTER JOIN M_人員 p ON w.担当者CD=p.CD"
					+ " WHERE w.ID=?"
			);
		) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery();) {
				if (!rs.next()) {
					addError("ID=" + id + " の加工実績がありません");
					return null;
				}

				Timestamp from = rs.getTimestamp("着手日時");
				Timestamp to = rs.getTimestamp("終了日時");
				if (from == null || to == null) {
					addError("ID=" + id + " の着手日時または終了日時が登録されていません");
					return null;
				}

				String staffCD = trim(rs.getString("担当者CD"));
				String processCD = trim(rs.getString("加工CD"));
				if (staffCD.isEmpty() || processCD.isEmpty()) {
					addError("ID=" + id + " の担当者または工程が登録されていません");
					return null;
				}

				Calendar fromCal = Calendar.getInstance();
				fromCal.setTime(from);
				Calendar toCal = Calendar.getInstance();
				toCal.setTime(to);

				return new InputDTO(
					id,
					trim(rs.getString("所属部署CD")),
					staffCD,
					processCD,
					rs.getInt("製作期"),
					rs.getInt("製作番号"),
					trim(rs.getString("製作枝番")),
					fromCal.get(Calendar.YEAR),
					fromCal.get(Calendar.MONTH) + 1,
					fromCal.get(Calendar.DATE),
					fromCal.get(Calendar.HOUR_OF_DAY),
					fromCal.get(Calendar.MINUTE),
					toCal.get(Calendar.HOUR_OF_DAY),
					toCal.get(Calendar.MINUTE),
					toRestMinutes(processCD, rs.getString("備考"))
				);
			}
		}
	}

	/**
	 * 備考に記録された休憩時間を分で取得する
	 * 出張以外の場合や、備考が「休憩○分」の形式でない場合は 0 を返す
	 *
	 * @param processCD 加工CD
	 * @param note 備考
	 *
	 * @return 休憩時間（分）
	 */
	private int toRestMinutes(String processCD, String note) {
		if (!BUSINESS_TRIP.equals(processCD) || note == null) {
			return 0;
		}
		try {
			return Integer.parseInt(note.replace("休憩", "").replace("分", "").trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * 前後の空白を除いた文字列を取得する
	 *
	 * @param value 対象の文字列
	 *
	 * @return 前後の空白を除いた文字列（{@code null} の場合は空文字）
	 */
	private String trim(String value) {
		return value == null ? "" : value.trim();
	}
}
