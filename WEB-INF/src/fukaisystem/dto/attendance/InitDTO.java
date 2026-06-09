package fukaisystem.dto.attendance;

import java.io.Serializable;
import java.util.Map;

public class InitDTO implements Serializable {

	Map<Integer, String> validMembers;

	public InitDTO(Map<Integer, String> validMembers) {
		this.validMembers = validMembers;
	}

	public Map<Integer, String> getMembers() {
		return validMembers;
	}
}
