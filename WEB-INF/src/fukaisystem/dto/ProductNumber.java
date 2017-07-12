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
public class ProductNumber implements Serializable {
	int period;
	int number;
	String branch;
	public ProductNumber(int period, int number, String branch) {
		this.period = period;
		this.number = number;
		this.branch = branch;
	}
	public int getPeriod() {
		return period;
	}
	public int getNumber() {
		return number;
	}
	public String getBranch() {
		return branch;
	}

}
