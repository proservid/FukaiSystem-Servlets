package fukaisystem.dto.aggregate;

import java.io.Serializable;

/**
 * 製作伝票管理の検索条件を保持するDTO
 *
 * @author kameura
 */
public class ProductSheetDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	/** 製作期（未指定は null） */
	private Integer period;
	/** 台（1～9） */
	private int type;

	/**
	 * 製作伝票管理の検索条件を生成する
	 *
	 * @param period 製作期（未指定は null）
	 * @param type   台（1～9）
	 */
	public ProductSheetDTO(Integer period, int type) {
		this.period = period;
		this.type = type;
	}

	public Integer getPeriod() {
		return period;
	}

	public int getType() {
		return type;
	}
}
