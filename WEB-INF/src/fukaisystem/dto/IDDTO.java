/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto;

import java.io.Serializable;

/**
 *
 * @author kameura
 */
public class IDDTO implements Serializable {
	private int estID;
	private int prdID;
	private int dlvID;
	public IDDTO(int estID, int prdID, int dlvID) {
		this.estID = estID;
		this.prdID = prdID;
		this.dlvID = dlvID;
	}
	public int getEstID() {
		return estID;
	}
	public int getPrdID() {
		return prdID;
	}
	public int getDlvID() {
		return dlvID;
	}
}
