package fukaisystem.domain.mh;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 加工実績の打刻1件
 *
 * <p>着手・終了・取消のいずれか1つを表す。
 * 着手は仕掛テーブル（{@code W_着手}）に記録され、終了の打刻で加工実績（{@code T_加工実績}）に確定する。
 *
 * <p>クライアントからサーブレットへ直列化して送られるため、
 * クライアント側にも同じ内容の写しを置くこと。
 *
 * @author kameura
 */
public class TimeRecord implements Serializable {

	private static final long serialVersionUID = 1L;

	/** 担当者CD */
	private final int workerCD;

	/** 所属部署CD */
	private final int deptCD;

	/** 加工CD */
	private final int processCD;

	/** 製作期 */
	private final int period;

	/** 製作番号 */
	private final int number;

	/** 製作枝番 */
	private final String branch;

	/** 打刻日時 */
	private final LocalDateTime datetime;

	/** 着手の打刻の場合は {@code true}、終了の打刻の場合は {@code false} */
	private final boolean isBegin;

	/** 着手の取消の場合は {@code true} */
	private final boolean isCancel;

	/**
	 * @param workerCD 担当者CD
	 * @param deptCD 所属部署CD
	 * @param processCD 加工CD
	 * @param period 製作期
	 * @param number 製作番号
	 * @param branch 製作枝番
	 * @param datetime 打刻日時
	 * @param isBegin 着手の打刻の場合は {@code true}、終了の打刻の場合は {@code false}
	 * @param isCancel 着手の取消の場合は {@code true}
	 */
	public TimeRecord(
		int workerCD, int deptCD, int processCD,
		int period, int number, String branch,
		LocalDateTime datetime, boolean isBegin,
		boolean isCancel
	) {
		this.workerCD = workerCD;
		this.deptCD = deptCD;
		this.processCD = processCD;
		this.period = period;
		this.number = number;
		this.branch = branch;
		this.datetime = datetime;
		this.isBegin = isBegin;
		this.isCancel = isCancel;
	}

	public int getWorker() {
		return workerCD;
	}

	public int getDept() {
		return deptCD;
	}

	public int getProcess() {
		return processCD;
	}

	public int getPeriod() {
		return period;
	}

	public int getNumber() {
		return number;
	}

	public String getBranch() {
		return branch;
	}

	public LocalDateTime getDatetime() {
		return datetime;
	}

	public boolean isBegin() {
		return isBegin;
	}

	public boolean isCancel() {
		return isCancel;
	}

}
