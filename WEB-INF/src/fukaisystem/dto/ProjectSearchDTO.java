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
public class ProjectSearchDTO implements Serializable {

	String accountName, projectName, range,
		quotationNum3, birthNum3, productionNum3, acceptID,
		submitY, submitM, submitD,
		acceptY, acceptM, acceptD,
		publishY, publishM, publishD,
		dueY, dueM, dueD,
		salesY, salesM, salesD,
		shippingY, shippingM, shippingD,
		inspectionY, inspectionM, inspectionD;
	int accountID, projectCode,
		quotationNum1, quotationNum2,
		birthNum1, birthNum2,
		productionNum1, productionNum2,
		currency,
		price,
		deliveryCode,
		buyer;
	boolean isAnd;

	public ProjectSearchDTO(
		int accountID,
		String accountName,
		String projectName,
		int projectCode,
		int birthNum1,
		int birthNum2,
		String birthNum3,
		int quotationNum1,
		int quotationNum2,
		String quotationNum3,
		String submitY,
		String submitM,
		String submitD,
		String acceptID,
		String acceptY,
		String acceptM,
		String acceptD,
		int productionNum1,
		int productionNum2,
		String productionNum3,
		String publishY,
		String publishM,
		String publishD,
		String dueY,
		String dueM,
		String dueD,
		String salesY,
		String salesM,
		String salesD,
		String shippingY,
		String shippingM,
		String shippingD,
		String inspectionY,
		String inspectionM,
		String inspectionD,
		int currency,
		int price,
		String range,
		int deliveryCode,
		int buyer,
		boolean isAnd
	) {
		this.accountID = accountID;
		this.accountName = accountName;
		this.projectName = projectName;
		this.projectCode = projectCode;
		this.birthNum1 = birthNum1;
		this.birthNum2 = birthNum2;
		this.birthNum3 = birthNum3;
		this.quotationNum1 = quotationNum1;
		this.quotationNum2 = quotationNum2;
		this.quotationNum3 = quotationNum3;
		this.submitY = submitY;
		this.submitM = submitM;
		this.submitD = submitD;
		this.acceptID = acceptID;
		this.acceptY = acceptY;
		this.acceptM = acceptM;
		this.acceptD = acceptD;
		this.productionNum1 = productionNum1;
		this.productionNum2 = productionNum2;
		this.productionNum3 = productionNum3;
		this.publishY = publishY;
		this.publishM = publishM;
		this.publishD = publishD;
		this.dueY = dueY;
		this.dueM = dueM;
		this.dueD = dueD;
		this.salesY = salesY;
		this.salesM = salesM;
		this.salesD = salesD;
		this.shippingY = shippingY;
		this.shippingM = shippingM;
		this.shippingD = shippingD;
		this.inspectionY = inspectionY;
		this.inspectionM = inspectionM;
		this.inspectionD = inspectionD;
		this.currency = currency;
		this.price = price;
		this.range = range;
		this.deliveryCode = deliveryCode;
		this.buyer = buyer;
		this.isAnd = isAnd;
	}

	public String getStr(int order) {
		String s = "";
		switch (order) {
			case 0:
				s = accountName;
				break;
			case 1:
				s = range;
				break;

			case 2:
				s = projectName;
				break;
			case 3:
				s = birthNum3;
				break;
			case 4:
				s = quotationNum3;
				break;
			case 5:
				s = submitY;
				break;
			case 6:
				s = submitM;
				break;
			case 7:
				s = submitD;
				break;

			case 8:
				s = productionNum3;
				break;
			case 9:
				s = acceptID;
				break;
			case 10:
				s = acceptY;
				break;
			case 11:
				s = acceptM;
				break;
			case 12:
				s = acceptD;
				break;
			case 13:
				s = publishY;
				break;
			case 14:
				s = publishM;
				break;
			case 15:
				s = publishD;
				break;
			case 16:
				s = dueY;
				break;
			case 17:
				s = dueM;
				break;
			case 18:
				s = dueD;
				break;
			case 19:
				s = shippingY;
				break;
			case 20:
				s = shippingM;
				break;
			case 21:
				s = shippingD;
				break;
			case 22:
				s = inspectionY;
				break;
			case 23:
				s = inspectionM;
				break;
			case 24:
				s = inspectionD;
				break;
			case 25:
				s = salesY;
				break;
			case 26:
				s = salesM;
				break;
			case 27:
				s = salesD;
				break;
		}
		return s;
	}

	public int getInt(int order) {
		int i = 0;
		switch (order) {
			case 0:
				i = productionNum1;
				break;
			case 1:
				i = productionNum2;
				break;

			case 2:
				i = accountID;
				break;
			case 3:
				i = quotationNum1;
				break;
			case 4:
				i = quotationNum2;
				break;
			case 5:
				i = projectCode;
				break;
			case 6:
				i = birthNum1;
				break;
			case 7:
				i = birthNum2;
				break;
			case 8:
				i = currency;
				break;
			case 9:
				i = price;
				break;
			case 10:
				i = deliveryCode;
				break;
			case 11:
				i = buyer;
				break;
		}
		return i;
	}

	public boolean isAnd() {
		return isAnd;
	}

}
