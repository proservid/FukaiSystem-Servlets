package fukaisystem.dto.mh;

import java.io.Serializable;
import java.sql.Date;

/**
 * 工数管理の加工実績一覧を取得するための検索条件
 * <p>
 * 対象日を指定した場合は、その日から翌日までの範囲で絞り込む。
 * 製作期などの条件は {@code null}（文字列の場合は空文字列も可）を指定すると絞り込みに使用しない。
 *
 * @author kameura
 *
 */
public class TbConditionDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private final boolean byInputDate;
	private final Date date;
	private final Integer period;
	private final Integer number;
	private final String branch;
	private final String staffCD;
	private final String workCD;

	/**
	 * 対象日で絞り込む条件を生成する
	 *
	 * @param byInputDate 入力日時で絞り込む場合は {@code true}、着手日時で絞り込む場合は {@code false}
	 * @param date 対象日
	 */
	public TbConditionDTO(boolean byInputDate, Date date) {
		this(byInputDate, date, null, null, null, null, null);
	}

	/**
	 * 製番・担当者・加工で絞り込む条件を生成する
	 *
	 * @param period 製作期
	 * @param number 製作番号
	 * @param branch 製作枝番
	 * @param staffCD 担当者CD
	 * @param workCD 加工CD
	 */
	public TbConditionDTO(Integer period, Integer number, String branch, String staffCD, String workCD) {
		this(false, null, period, number, branch, staffCD, workCD);
	}

	private TbConditionDTO(boolean byInputDate, Date date, Integer period, Integer number, String branch,
		String staffCD, String workCD) {
		this.byInputDate = byInputDate;
		this.date = date;
		this.period = period;
		this.number = number;
		this.branch = branch;
		this.staffCD = staffCD;
		this.workCD = workCD;
	}

	public boolean isByInputDate() {
		return byInputDate;
	}

	public Date getDate() {
		return date;
	}

	public Integer getPeriod() {
		return period;
	}

	public Integer getNumber() {
		return number;
	}

	public String getBranch() {
		return branch;
	}

	public String getStaffCD() {
		return staffCD;
	}

	public String getWorkCD() {
		return workCD;
	}
}
