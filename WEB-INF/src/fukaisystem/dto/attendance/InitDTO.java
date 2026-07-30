package fukaisystem.dto.attendance;

import java.io.Serializable;
import java.util.Map;

public class InitDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	Map<Integer, String> validMemberMap;
	Map<Integer, Map<Integer, Boolean>> holidayMap;

	public InitDTO(Map<Integer, String> validMemberMap, Map<Integer, Map<Integer, Boolean>> holidayMap) {
		this.validMemberMap = validMemberMap;
		this.holidayMap = holidayMap;
	}

	public Map<Integer, String> getMemberMap() {
		return validMemberMap;
	}

	public Map<Integer, Map<Integer, Boolean>> getHolidayMap() {
		return holidayMap;
	}
}
