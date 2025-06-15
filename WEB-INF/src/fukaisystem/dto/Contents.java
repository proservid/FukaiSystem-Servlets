/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fukaisystem.dto;

/**
 *
 * @author kameura
 */
public class Contents {
	String[] slipNames, keys, signs, values;
	String order;

	public Contents(String[] slipNames, String[] keys, String[] signs, String[] values, String order) {
		this.slipNames = slipNames;
		this.keys = keys;
		this.signs = signs;
		this.values = values;
		this.order = order;
	}

	public String[] getSlipNames() {
		return slipNames;
	}

	public String[] getKeys() {
		return keys;
	}

	public String[] getSigns() {
		return signs;
	}

	public String[] getValues() {
		return values;
	}

	public String getOrder() {
		return order;
	}

}
