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
	int account, l, m, s, period, number;
	String branch, article, y0, m0, d0, y1, m1, d1, y2, m2, d2;
	boolean isStock, isAndSearch;

	public HistoryDTO(
			int account, String article, int l, int m, int s,
			int period, int number, String branch,
			String y0, String m0, String d0,
			String y1, String m1, String d1,
			String y2, String m2, String d2,
			boolean isStock,
			boolean isAndSearch) {
		this.account = account;
		this.article = article;
		this.l = l;
		this.m = m;
		this.s = s;
		this.period = period;
		this.number = number;
		this.branch = branch;
		this.y0 = y0;
		this.m0 = m0;
		this.d0 = d0;
		this.y1 = y1;
		this.m1 = m1;
		this.d1 = d1;
		this.y2 = y2;
		this.m2 = m2;
		this.d2 = d2;
		this.isStock = isStock;
		this.isAndSearch = isAndSearch;
	}

	@Override
	public String getStr(int order) {
		String str = "";
		switch(order) {
			case 0:
				str = article; break;
			case 1:
				str = branch; break;
			case 2:
				str = y0; break;
			case 3:
				str = m0; break;
			case 4:
				str = d0; break;
			case 5:
				str = y1; break;
			case 6:
				str = m1; break;
			case 7:
				str = d1; break;
			case 8:
				str = y2; break;
			case 9:
				str = m2; break;
			case 10:
				str = d2; break;
		}
		return str;
	}

	@Override
	public int getInt(int order) {
		int i = 0;
		switch(order) {
			case 0:
				i = account; break;
			case 1:
				i = period; break;
			case 2:
				i = number; break;
			case 3:
				i = l; break;
			case 4:
				i = m; break;
			case 5:
				i = s; break;
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
	public List<String> getEsts() {
		return null;
	}

	@Override
	public List<Integer> getParents() {
		return null;
	}

}
