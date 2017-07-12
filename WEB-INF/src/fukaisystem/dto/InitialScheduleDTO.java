/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto;

import java.io.Serializable;
import java.util.Map;

/**
 *
 * @author kameura
 */
public class InitialScheduleDTO implements Serializable {

	Map<String, String> dept;
	Map<String, Map<String, String>> name;

	public InitialScheduleDTO(Map<String, String> dept, Map<String, Map<String, String>> name) {
		this.dept = dept;
		this.name = name;
	}

	public Map<String, String> getDept() {
		return dept;
	}
	public Map<String, Map<String, String>> getName() {
		return name;
	}
}
