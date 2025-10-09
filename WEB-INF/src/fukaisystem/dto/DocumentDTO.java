/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto;

import java.io.Serializable;
import java.util.Map;
import java.util.Vector;

/**
 *
 * @author kameura
 */
public class DocumentDTO implements Serializable {

	private Vector<Vector<Object>> vector;
	private Map<Integer, Vector<Vector<Object>>> map;

	public DocumentDTO(Vector<Vector<Object>> vector, Map<Integer, Vector<Vector<Object>>> map) {
		this.vector = vector;
		this.map = map;
	}

	public Vector<Vector<Object>> getVector() {
		return vector;
	}

	public Map<Integer, Vector<Vector<Object>>> getMap() {
		return map;
	}
}
