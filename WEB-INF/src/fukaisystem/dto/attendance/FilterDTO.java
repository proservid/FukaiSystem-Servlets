package fukaisystem.dto.attendance;

import java.io.Serializable;

public class FilterDTO implements Serializable {

	Integer coarse;
	Integer middle;
	Integer fine;

	public FilterDTO(Integer coarse, Integer middle, Integer fine) {
		this.coarse = coarse;
		this.middle = middle;
		this.fine = fine;
	}

	public Integer getCoarse() {
		return coarse;
	}

	public Integer getMiddle() {
		return middle;
	}

	public Integer getFine() {
		return fine;
	}
}
