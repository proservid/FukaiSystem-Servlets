package fukaisystem.dto;

import java.io.Serializable;

/**
 * 集計条件（月次集計・年次集計）を保持するDTO
 *
 * @author kameura
 */
public class AggregateDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	/** 集計種別（メニューのインデックス、0 はじまり） */
	private int order;
	/** 年 */
	private int year;
	/** 月（年次集計では 0） */
	private int month;
	/** 仕入先CD（買掛帳票・入荷実績のみ使用、未指定は 0） */
	private int supplier;

	/**
	 * 月次集計の集計条件を生成する
	 *
	 * @param order    集計種別（メニューのインデックス、0 はじまり）
	 * @param year     年
	 * @param month    月
	 * @param supplier 仕入先CD（未指定は 0）
	 */
	public AggregateDTO(int order, int year, int month, int supplier) {
		this.order = order;
		this.year = year;
		this.month = month;
		this.supplier = supplier;
	}

	/**
	 * 年次集計の集計条件を生成する
	 *
	 * @param order 集計種別（メニューのインデックス、0 はじまり）
	 * @param year  年
	 */
	public AggregateDTO(int order, int year) {
		this(order, year, 0, 0);
	}

	public int getOrder() {
		return order;
	}

	public int getYear() {
		return year;
	}

	public int getMonth() {
		return month;
	}

	public int getSupplier() {
		return supplier;
	}
}
