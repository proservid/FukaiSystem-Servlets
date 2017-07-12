package fukaisystem.dto;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author kameura
 */
public class StringDTO implements Serializable {

	private String str;
	private List<String> data;

	public StringDTO(String str, List<String> data) {
		this.str = str;
		this.data = data;
	}

	public String getString() {
		return str;
	}

	public List<String> getData() {
		return data;
	}

}

