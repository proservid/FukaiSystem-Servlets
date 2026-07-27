package fukaisystem.dto.attendance;

import java.io.Serializable;
import java.util.Vector;

public class DataVectorDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	Vector<Vector<Object>> dataVector;

	public DataVectorDTO(Vector<Vector<Object>> dataVector) {
		this.dataVector = dataVector;
	}

	public Vector<Vector<Object>> getDataVector() {
		return dataVector;
	}
}
