package fukaisystem.dto.mh;

import java.io.Serializable;
import java.sql.Date;
import java.util.Calendar;

/**
 * 工数管理の月単位の集計を取得するための対象年月
 *
 * @author kameura
 *
 */
public class TbMonthDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private final int year;
	private final int month;

	/**
	 * @param year 対象年
	 * @param month 対象月（1 はじまり）
	 */
	public TbMonthDTO(int year, int month) {
		this.year = year;
		this.month = month;
	}

	public int getYear() {
		return year;
	}

	public int getMonth() {
		return month;
	}

	/**
	 * 対象月の初日を取得する
	 *
	 * @return 対象月の初日
	 */
	public Date getFrom() {
		return new Date(firstDate(0).getTimeInMillis());
	}

	/**
	 * 翌月の初日を取得する
	 *
	 * @return 翌月の初日
	 */
	public Date getTo() {
		return new Date(firstDate(1).getTimeInMillis());
	}

	/**
	 * 対象月から指定した月数だけ後の月の初日を取得する
	 *
	 * @param offset 対象月からの月数
	 *
	 * @return 求めた月の初日のCalendarオブジェクト
	 */
	private Calendar firstDate(int offset) {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(year, month - 1 + offset, 1);
		return cal;
	}
}
