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
public class OrderSearchDTO implements Serializable {

	private String orderNum3, orderY, orderM, orderD,
		dueY, dueM, dueD,
		deliveryY, deliveryM, deliveryD,
		acceptY, acceptM, acceptD;
	private int supplierID, orderNum1, orderNum2, slipNum, deliveryNum;
	private boolean isAndSearch;

	public OrderSearchDTO(
		String orderNum3,
		String orderY,
		String orderM,
		String orderD,
		String dueY,
		String dueM,
		String dueD,
		String acceptY,
		String acceptM,
		String acceptD,
		String deliveryY,
		String deliveryM,
		String deliveryD,
		int supplierID,
		int orderNum1,
		int orderNum2,
		int slipNum,
		int deliveryNum,
		boolean isAndSearch
	) {
		this.orderNum3 = orderNum3;
		this.orderY = orderY;
		this.orderM = orderM;
		this.orderD = orderD;
		this.dueY = dueY;
		this.dueM = dueM;
		this.dueD = dueD;
		this.acceptY = acceptY;
		this.acceptM = acceptM;
		this.acceptD = acceptD;
		this.deliveryY = deliveryY;
		this.deliveryM = deliveryM;
		this.deliveryD = deliveryD;
		this.supplierID = supplierID;
		this.orderNum1 = orderNum1;
		this.orderNum2 = orderNum2;
		this.slipNum = slipNum;
		this.deliveryNum = deliveryNum;
		this.isAndSearch = isAndSearch;
	}

	public String getStr(int order) {
		switch (order) {
			case 0:
				return orderNum3;
			case 1:
				return orderY;
			case 2:
				return orderM;
			case 3:
				return orderD;
			case 4:
				return dueY;
			case 5:
				return dueM;
			case 6:
				return dueD;
			case 7:
				return acceptY;
			case 8:
				return acceptM;
			case 9:
				return acceptD;
			case 10:
				return deliveryY;
			case 11:
				return deliveryM;
			case 12:
				return deliveryD;
			default:
				return "";
		}
	}

	public int getInt(int order) {
		switch (order) {
			case 0:
				return supplierID;
			case 1:
				return orderNum1;
			case 2:
				return orderNum2;
			case 3:
				return slipNum;
			case 4:
				return deliveryNum;
			default:
				return 0;
		}
	}

	public boolean isAnd() {
		return isAndSearch;
	}

}
