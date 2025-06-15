/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto;

import java.sql.Date;
import java.util.List;
import java.util.Vector;

/**
 *
 * @author kameura
 */
public class HistoryDTO extends BasicDTO {
	int accountCode, coarseCategory, middleCategory, fineCategory, productionNum1, productionNum2;
	String productionNum3, article, orderY, orderM, orderD, dueY, dueM, dueD, receiptY, receiptM, receiptD;
	boolean isStock, isAndSearch;

	public HistoryDTO(
		int accountCode,
		String article,
		int coarseCategory,
		int middleCategory,
		int fineCategory,
		int productionNum1,
		int productionNum2,
		String productionNum3,
		String orderY,
		String orderM,
		String orderD,
		String dueY,
		String dueM,
		String dueD,
		String receiptY,
		String receiptM,
		String receiptD,
		boolean isStock,
		boolean isAndSearch
	) {
		this.accountCode = accountCode;
		this.article = article;
		this.coarseCategory = coarseCategory;
		this.middleCategory = middleCategory;
		this.fineCategory = fineCategory;
		this.productionNum1 = productionNum1;
		this.productionNum2 = productionNum2;
		this.productionNum3 = productionNum3;
		this.orderY = orderY;
		this.orderM = orderM;
		this.orderD = orderD;
		this.dueY = dueY;
		this.dueM = dueM;
		this.dueD = dueD;
		this.receiptY = receiptY;
		this.receiptM = receiptM;
		this.receiptD = receiptD;
		this.isStock = isStock;
		this.isAndSearch = isAndSearch;
	}

	@Override
	public String getStr(int order) {
		String str = "";
		switch (order) {
			case 0:
				str = article;
				break;
			case 1:
				str = productionNum3;
				break;
			case 2:
				str = orderY;
				break;
			case 3:
				str = orderM;
				break;
			case 4:
				str = orderD;
				break;
			case 5:
				str = dueY;
				break;
			case 6:
				str = dueM;
				break;
			case 7:
				str = dueD;
				break;
			case 8:
				str = receiptY;
				break;
			case 9:
				str = receiptM;
				break;
			case 10:
				str = receiptD;
				break;
		}
		return str;
	}

	@Override
	public int getInt(int order) {
		int i = 0;
		switch (order) {
			case 0:
				i = accountCode;
				break;
			case 1:
				i = productionNum1;
				break;
			case 2:
				i = productionNum2;
				break;
			case 3:
				i = coarseCategory;
				break;
			case 4:
				i = middleCategory;
				break;
			case 5:
				i = fineCategory;
				break;
		}
		return i;
	}

	@Override
	public Vector<Vector<Object>> getVector(int order) {
		return null;
	}

	@Override
	public Date getDate(int order) {
		return null;
	}

	public boolean isAnd() {
		return isAndSearch;
	}

	@Override
	public boolean getBool(int order) {
		return isStock;
	}

	@Override
	public List<String> getQuotationNumbers() {
		return null;
	}

	@Override
	public List<Integer> getParents() {
		return null;
	}

}
 