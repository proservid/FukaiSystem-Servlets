package fukaisystem.dto.attendance;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ModifiedDTO implements Serializable {

	Date date;
	Map<Integer, List<Object>> modifiedMap;

	public ModifiedDTO(Date date, Map<Integer, List<Object>> modifiedMap) {
		this.date = date;
		this.modifiedMap = modifiedMap;
	}

	public Date getDate() {
		return date;
	}

	public Map<Integer, List<Object>> getMap() {
		return modifiedMap;
	}
}
