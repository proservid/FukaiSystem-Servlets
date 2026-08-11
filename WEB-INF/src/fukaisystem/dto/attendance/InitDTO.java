package fukaisystem.dto.attendance;

import java.io.Serializable;
import java.util.Map;

import fukaisystem.domain.attendance.WorkSchedule;

public class InitDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	Map<Integer, String> validMemberMap;
	Map<Integer, Map<Integer, Boolean>> holidayMap;
	WorkSchedule workSchedule;

	public InitDTO(Map<Integer, String> validMemberMap, Map<Integer, Map<Integer, Boolean>> holidayMap,
		WorkSchedule workSchedule) {
		this.validMemberMap = validMemberMap;
		this.holidayMap = holidayMap;
		this.workSchedule = workSchedule;
	}

	public Map<Integer, String> getMemberMap() {
		return validMemberMap;
	}

	public Map<Integer, Map<Integer, Boolean>> getHolidayMap() {
		return holidayMap;
	}

	/**
	 * 就業時間マスタの内容を取得する
	 *
	 * @return 就業時間
	 */
	public WorkSchedule getWorkSchedule() {
		return workSchedule;
	}
}
