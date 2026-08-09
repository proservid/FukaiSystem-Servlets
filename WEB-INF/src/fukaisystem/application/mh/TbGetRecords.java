package fukaisystem.application.mh;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.servlet.ServletResponse;

import fukaisystem.dto.mh.TbConditionDTO;
import fukaisystem.foundation.ServiceFoundation;
import fukaisystem.sql.ResultSetConverter;

/**
 * 工数管理のチェック表・時間表・検索の一覧を取得するためのクラス
 * <p>
 * 列見出しはクライアントが固定で持っているため、列の順序を変更してはならない。
 * 順序は 連番, 日付, 製番, 担当者CD, 担当者名, 加工CD, 工程名, 開始, 終了, 工数 とする。
 *
 * @author kameura
 *
 */
public class TbGetRecords extends ServiceFoundation {

	/** 一覧の基となるクエリ（末尾に検索条件と並べ替えを連結する） */
	private static final String QUERY =
		"SELECT w.ID AS 連番,"
			+ " CONVERT(varchar, w.着手日時, 112) AS 日付,"
			+ " RIGHT('00' + CAST(w.製作期 AS varchar), 2)+'-'"
			+ "+RIGHT('0000' + CAST(w.製作番号 AS varchar), 4)+w.製作枝番 AS 製番,"
			+ " w.担当者CD,"
			+ " m.姓+m.名 AS 担当者名,"
			+ " w.加工CD,"
			+ " k.小分類名 AS 工程名,"
			+ " LEFT(CONVERT(varchar, w.着手日時, 108), 5) AS 開始,"
			+ " LEFT(CONVERT(varchar, w.終了日時, 108), 5) AS 終了,"
			+ " CONVERT(float, w.時間)/100 AS 工数"
			+ " FROM T_加工実績 w"
			+ " LEFT OUTER JOIN M_加工_子 k ON w.加工CD=k.CD"
			+ " LEFT OUTER JOIN M_人員 m ON w.担当者CD=m.CD";

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		TbConditionDTO condition = cast(response, o, TbConditionDTO.class);
		if (condition == null) {
			return null;
		}

		StringBuilder where = new StringBuilder();
		List<Object> parameters = new ArrayList<Object>();

		if (condition.getDate() != null) {
			Calendar nextDay = Calendar.getInstance();
			nextDay.setTime(condition.getDate());
			nextDay.add(Calendar.DATE, 1);
			String column = condition.isByInputDate() ? "w.入力日時" : "w.着手日時";
			addCondition(where, parameters, column + ">=?", condition.getDate());
			addCondition(where, parameters, column + "<?", new Date(nextDay.getTimeInMillis()));
		}
		addCondition(where, parameters, "w.製作期=?", condition.getPeriod());
		addCondition(where, parameters, "w.製作番号=?", condition.getNumber());
		addCondition(where, parameters, "w.製作枝番=?", trimToNull(condition.getBranch()));
		addCondition(where, parameters, "w.担当者CD=?", trimToNull(condition.getStaffCD()));
		addCondition(where, parameters, "w.加工CD=?", trimToNull(condition.getWorkCD()));

		try (
			PreparedStatement ps = c.prepareStatement(QUERY + where + " ORDER BY 日付,w.着手日時,製番");
		) {
			for (int i = 0; i < parameters.size(); i++) {
				ps.setObject(i + 1, parameters.get(i));
			}

			try (ResultSet rs = ps.executeQuery();) {
				return ResultSetConverter.toTable(rs);
			}
		}
	}

	/**
	 * 値が指定されている場合に限り、検索条件とそのパラメータを追加する
	 *
	 * @param where 条件の連結先（1件目は WHERE、2件目以降は AND で連結する）
	 * @param parameters パラメータの追加先
	 * @param expression 追加する条件（パラメータを1個だけ含むこと）
	 * @param value 条件に設定する値（{@code null} の場合は何もしない）
	 */
	private void addCondition(StringBuilder where, List<Object> parameters, String expression, Object value) {
		if (value == null) {
			return;
		}
		where.append(where.length() == 0 ? " WHERE " : " AND ").append(expression);
		parameters.add(value);
	}

	/**
	 * 前後の空白を除いた文字列を取得する
	 *
	 * @param value 対象の文字列
	 *
	 * @return 空白を除いた文字列、対象が {@code null} または空文字列の場合は {@code null}
	 */
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
