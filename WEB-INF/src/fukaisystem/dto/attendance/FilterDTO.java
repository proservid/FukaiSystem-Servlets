package fukaisystem.dto.attendance;

import java.io.Serializable;

public class FilterDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	int coarse;
	int middle;
	int fine;

	public FilterDTO(int coarse, int middle, int fine) {
		this.coarse = coarse;
		this.middle = middle;
		this.fine = fine;
	}

	public int getCoarse() {
		return coarse;
	}

	public int getMiddle() {
		return middle;
	}

	public int getFine() {
		return fine;
	}
}
