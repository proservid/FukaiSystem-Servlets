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
public class DestinationDTO implements Serializable {
	int accountCD;
	Vector<Vector<Object>> destinations;

	public DestinationDTO(int accountCD, Vector<Vector<Object>> destinations) {
		this.accountCD = accountCD;
		this.destinations = destinations;
	}

	public int getAccountCD() {
		return accountCD;
	}

	public Vector<Vector<Object>> getDestinations() {
		return destinations;
	}
}
