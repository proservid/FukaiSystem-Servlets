/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto.mh;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 *
 * @author kameura
 */
public class InitialInputDTO implements Serializable {

	Map<String, String> dept;
	Map<String, String> process;
	Map<List<String>, Map<String, String>> name;
	int period;

	public InitialInputDTO(Map<String, String> dept, Map<List<String>, Map<String, String>> name, Map<String, String> process,
		int period) {
		this.dept = dept;
		this.name = name;
		this.process = process;
		this.period = period;
	}

	public Map<String, String> getDept() {
		return dept;
	}
	public Map<List<String>, Map<String, String>> getName() {
		return name;
	}
	public Map<String, String> getProcess() {
		return process;
	}
	public int getPeriod() {
		return period;
	}
}
