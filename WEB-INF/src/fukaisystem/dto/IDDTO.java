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
	private static final long serialVersionUID = -8485164825537144052L;
	private final int quotationID;
	private final int productionID;
	private final int salesID;

	public IDDTO(int quotationID, int productionID, int salesID) {
		this.quotationID = quotationID;
		this.productionID = productionID;
		this.salesID = salesID;
	}

	public int getQuotationID() {
		return quotationID;
	}

	public int getProductionID() {
		return productionID;
	}

	public int getSalesID() {
		return salesID;
	}
}
