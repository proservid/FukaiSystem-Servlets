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
public class HistoryDTO implements Serializable {

	private int accountCode, coarseCategory, middleCategory, fineCategory, productionNum1, productionNum2;
	private String productionNum3, article, orderY, orderM, orderD, dueY, dueM, dueD, receiptY, receiptM, receiptD;
	private boolean isStock, isAndSearch;

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

	public String article() {
		return article;
	}

	public String productionNum3() {
		return productionNum3;
	}

	public String orderY() {
		return orderY;
	}

	public String orderM() {
		return orderM;
	}

	public String orderD() {
		return orderD;
	}

	public String dueY() {
		return dueY;
	}

	public String dueM() {
		return dueM;
	}

	public String dueD() {
		return dueD;
	}

	public String receiptY() {
		return receiptY;
	}

	public String receiptM() {
		return receiptM;
	}

	public String receiptD() {
		return receiptD;
	}

	public int accountCode() {
		return accountCode;
	}

	public int productionNum1() {
		return productionNum1;
	}

	public int productionNum2() {
		return productionNum2;
	}

	public int coarseCategory() {
		return coarseCategory;
	}

	public int middleCategory() {
		return middleCategory;
	}

	public int fineCategory() {
		return fineCategory;
	}

	public boolean isAndSearch() {
		return isAndSearch;
	}

	public boolean isStock() {
		return isStock;
	}

	/**
	 * サーブレット登録用
	 * 
	 * @param order 順番
	 * 
	 * @return 値
	 */
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

	/**
	 * サーブレット登録用
	 * 
	 * @param order 順番
	 * 
	 * @return 値
	 */
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

}
