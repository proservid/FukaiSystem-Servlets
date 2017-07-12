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
public class DispatchingDTO extends BasicDTO {
	int dispatchingID, period, number, slipNum, l, m, s;
	String branch, year, month, day, purpose, purpose2;
	Vector<Vector<Object>> vector;
	Date date;
	boolean isAndSearch;

	public DispatchingDTO(int dispatchingID, int period, int number,
			String branch, String year, String month, String day,
			int slipNum, String purpose, String purpose2, Vector<Vector<Object>> vector,
			Date date,
			int l, int m, int s,
			boolean isAndSearch) {
		this.dispatchingID = dispatchingID;
		this.period = period;
		this.number = number;
		this.branch = branch;
		this.slipNum = slipNum;
		this.purpose = purpose;
		this.purpose2 = purpose2;
		this.year = year;
		this.month = month;
		this.day = day;
		this.vector = vector;
		this.date = date;
		this.l = l;
		this.m = m;
		this.s = s;
		this.isAndSearch = isAndSearch;
	}

	@Override
	public String getStr(int order) {
		String str = "";
		switch(order) {
			case 0:
				str = branch; break;
			case 1:
				str = year; break;
			case 2:
				str = month; break;
			case 3:
				str = day; break;
			case 4:
				str = purpose; break;
			case 5:
				str = purpose2; break;
		}
		return str;
	}

	@Override
	public int getInt(int order) {
		int i = 0;
		switch(order) {
			case 0:
				i = period; break;
			case 1:
				i = number; break;
			case 2:
				i = l; break;
			case 3:
				i = m; break;
			case 4:
				i = s; break;
			case 5:
				i = dispatchingID; break;
			case 6:
				i = slipNum; break;
		}
		return i;
	}

	@Override
	public Vector<Vector<Object>> getVector(int order) {
		return vector;
	}

	@Override
	public Date getDate(int order) {
		return date;
	}

	public boolean isAnd() {
		return isAndSearch;
	}

	@Override
	public boolean getBool(int order) {
		return false;
	}

	@Override
	public List<String> getEsts() {
		return null;
	}

	@Override
	public List<Integer> getParents() {
		return null;
	}

}
