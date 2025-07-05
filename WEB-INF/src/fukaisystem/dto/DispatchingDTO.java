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
public class DispatchingDTO implements Serializable {
	int dispatchingID, orderNum1, dispatchNum1, orderNum2, dispatchNum2, coarseCategory, middleCategory, fineCategory;
	String orderNum3, dispatchNum3, year, month, day, name, use, remark;
	Vector<Vector<Object>> vector;
	Date date;
	boolean isAndSearch;

	public DispatchingDTO(
		int dispatchingID,
		int orderNum1,
		int orderNum2,
		String orderNum3,
		int dispatchNum1,
		int dispatchNum2,
		String dispatchNum3,
		String year,
		String month,
		String day,
		String name,
		String use,
		String remark,
		Vector<Vector<Object>> vector,
		Date date,
		int coarseCategory,
		int middleCategory,
		int fineCategory,
		boolean isAndSearch
	) {
		this.dispatchingID = dispatchingID;
		this.orderNum1 = orderNum1;
		this.orderNum2 = orderNum2;
		this.orderNum3 = orderNum3;
		this.dispatchNum1 = dispatchNum1;
		this.dispatchNum2 = dispatchNum2;
		this.dispatchNum3 = dispatchNum3;
		this.name = name;
		this.use = use;
		this.remark = remark;
		this.year = year;
		this.month = month;
		this.day = day;
		this.vector = vector;
		this.date = date;
		this.coarseCategory = coarseCategory;
		this.middleCategory = middleCategory;
		this.fineCategory = fineCategory;
		this.isAndSearch = isAndSearch;
	}

	public String dispatchNum3() {
		return dispatchNum3;
	}

	public String year() {
		return year;
	}

	public String month() {
		return month;
	}

	public String day() {
		return day;
	}

	public String use() {
		return use;
	}

	public String remark() {
		return remark;
	}

	public String orderNum3() {
		return orderNum3;
	}

	public String name() {
		return name;
	}

	public int dispatchNum1() {
		return dispatchNum1;
	}

	public int dispatchNum2() {
		return dispatchNum2;
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

	public int orderNum1() {
		return orderNum1;
	}

	public int orderNum2() {
		return orderNum2;
	}

	public int dispatchingID() {
		return dispatchingID;
	}

	public Vector<Vector<Object>> getVector() {
		return vector;
	}

	public Date date() {
		return date;
	}

	public boolean isAndSearch() {
		return isAndSearch;
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
				str = dispatchNum3;
				break;
			case 1:
				str = year;
				break;
			case 2:
				str = month;
				break;
			case 3:
				str = day;
				break;
			case 4:
				str = use;
				break;
			case 5:
				str = remark;
				break;
			case 6:
				str = orderNum3;
				break;
			case 7:
				str = name;
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
				i = dispatchNum1;
				break;
			case 1:
				i = dispatchNum2;
				break;
			case 2:
				i = coarseCategory;
				break;
			case 3:
				i = middleCategory;
				break;
			case 4:
				i = fineCategory;
				break;
			case 5:
				i = orderNum1;
				break;
			case 6:
				i = orderNum2;
				break;
			case 7: // 5→7
				i = dispatchingID;
				break;
		}
		return i;
	}
}
