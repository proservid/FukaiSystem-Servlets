package fukaisystem.dto;

	import java.io.Serializable;
	import java.util.Map;
import java.util.Vector;

	/**
	 *
	 * @author kameura
	 */
	public class DMDTO implements Serializable {

		Map<String, String> dept;
		Map<String, Vector<Vector<Object>>> name;

		public DMDTO(Map<String, String> dept, Map<String, Vector<Vector<Object>>> name) {
			this.dept = dept;
			this.name = name;
		}

		public Map<String, String> getDept() {
			return dept;
		}
		public Map<String, Vector<Vector<Object>>> getName() {
			return name;
		}
	}
