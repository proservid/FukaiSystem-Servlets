/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto;

import java.io.Serializable;
import java.sql.Date;
import java.util.Vector;

/**
 *
 * @author kameura
 */
public class SalesDTO implements Serializable {
	Date date;
	int shippingID, productionID, account, state, way, tax, discount, type;
	String note;
	Vector<Vector<Object>> child;

	public SalesDTO(
		int shippingID,
		int productionID,
		int account,
		Date date,
		int state,
		int way,
		int tax,
		int discount,
		int type,
		String note,
		Vector<Vector<Object>> child
	) {
		this.shippingID = shippingID;
		this.productionID = productionID;
		this.account = account;
		this.state = state;
		this.way = way;
		this.tax = tax;
		this.discount = discount;
		this.type = type;
		this.date = date;
		this.note = note;
		this.child = child;
	}

	public String getString() {
		return note;
	}

	public int getInt(int order) {
		int i = 0;
		switch (order) {
			case 0:
				i = shippingID;
				break;
			case 1:
				i = productionID;
				break;
			case 2:
				i = account;
				break;
			case 3:
				i = state;
				break;
			case 4:
				i = way;
				break;
			case 5:
				i = tax;
				break;
			case 6:
				i = discount;
				break;
			case 7:
				i = type;
				break;

		}
		return i;
	}

	public Date getDate() {
		return date;
	}

	public Vector<Vector<Object>> getVector() {
		return child;
	}

	public void setVector(Vector<Vector<Object>> child) {
		this.child = child;
	}
}
 