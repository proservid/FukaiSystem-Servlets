package fukaisystem.dto.mh;

import java.io.Serializable;
import java.sql.Date;

/**
 * 1日分の加工実績を取得するための条件
 *
 * @author kameura
 */
public class DailyInputDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Date date;
	private final String staffCD;

	/**
	 * @param date 対象日
	 * @param staffCD 担当者CD
	 */
	public DailyInputDTO(Date date, String staffCD) {
		this.date = date;
		this.staffCD = staffCD;
	}

	public Date getDate() {
		return date;
	}

	public String getStaffCD() {
		return staffCD;
	}
}
