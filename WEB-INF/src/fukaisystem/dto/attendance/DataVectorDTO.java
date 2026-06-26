package fukaisystem.dto.attendance;

import java.io.Serializable;
import java.util.Vector;

public class DataVectorDTO implements Serializable {

	Vector<Vector<Object>> dataVector;

	public DataVectorDTO(Vector<Vector<Object>> dataVector) {
		this.dataVector = dataVector;
	}

	public Vector<Vector<Object>> getDataVector() {
		return dataVector;
	}
}
