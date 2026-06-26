package fukaisystem.dto.attendance;

import java.io.Serializable;
import java.util.List;

import fukaisystem.domain.attendance.WorkDaily;

public class WorkDailiesDTO implements Serializable {

	List<WorkDaily> workDailies;

	public WorkDailiesDTO(List<WorkDaily> workDailies) {
		this.workDailies = workDailies;
	}

	public List<WorkDaily> getWorkDailies() {
		return workDailies;
	}
}
