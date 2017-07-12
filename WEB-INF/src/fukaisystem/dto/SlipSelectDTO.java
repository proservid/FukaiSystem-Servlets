/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto;

import java.io.Serializable;
import java.util.Vector;

/**
 *
 * @author kameura
 */
public class SlipSelectDTO implements Serializable {
	Vector<String> candidate;
	Vector<Vector<String>> tableData;

	public SlipSelectDTO(Vector<String> candidate, Vector<Vector<String>> tableData) {
		this.candidate = candidate;
		this.tableData = tableData;
	}

	public Vector<String> getCandidate() {
		return candidate;
	}
	public Vector<Vector<String>> getTableData() {
		return tableData;
	}
}
