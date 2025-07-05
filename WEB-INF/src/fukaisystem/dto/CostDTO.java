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
public class CostDTO implements Serializable {
	String caption;
	int total;
	Vector<String> titles;
	Vector<Vector<Object>> dataVector;

	public CostDTO(String caption, int total, Vector<String> titles, Vector<Vector<Object>> dataVector) {
		this.caption = caption;
		this.total = total;
		this.titles = titles;
		this.dataVector = dataVector;
	}

	public String getCaption() {
		return caption;
	}

	public int getTotal() {
		return total;
	}

	public Vector<String> getTableTitles() {
		return titles;
	}

	public Vector<Vector<Object>> getTableData() {
		return dataVector;
	}
}
