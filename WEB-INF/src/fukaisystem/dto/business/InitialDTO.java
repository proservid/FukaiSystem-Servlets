/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto.business;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 *
 * @author kameura
 */
public class InitialDTO implements Serializable {

	private List<String> slipList;
	private Map<Integer, String> typeMap;
	private Map<Integer, String> routeMap;
	private Vector<String> dueVector;
	private Vector<String> placeVector;
	private Vector<String> termVector;
	private Vector<String> validityVector;
	private Map<Integer, String> submitMap;
	private Map<Integer, String> currencyMap;
	private Map<Integer, String> stateMap;
	private Map<Integer, String> wayMap;
	private Map<Integer, String> partialMap;
	private Map<Integer, String> indicationMap;
	private Map<Integer, String> unitMap;
	private Map<Integer, String> supplierMap;
	private Map<Integer, Integer> processingCostMap;
	private Map<Integer, String> materialCostMap;
	private Map<Integer, String> coarseCategoryMap;
	private Map<List<Integer>, Map<Integer, String>> middleCategoryMap;
	private Map<List<Integer>, Map<Integer, String>> fineCategoryMap;
	private Map<Integer, Map<Integer, Map<Integer, Integer>>> costMap;
	private Map<Integer, Double> sgMap; // 比重

	public InitialDTO(
		List<String> slipList,
		Map<Integer, String> typeMap,
		Map<Integer, String> routeMap,
		Vector<String> dueVector,
		Vector<String> placeVector,
		Vector<String> termVector,
		Vector<String> validityVector,
		Map<Integer, String> submitMap,
		Map<Integer, String> currencyMap,
		Map<Integer, String> stateMap,
		Map<Integer, String> wayMap,
		Map<Integer, String> partialMap,
		Map<Integer, String> indicationMap,
		Map<Integer, String> unitMap,
		Map<Integer, String> supplierMap,
		Map<Integer, Integer> processingCostMap,
		Map<Integer, String> materialCostMap,
		Map<Integer, String> coarseCategoryMap,
		Map<List<Integer>, Map<Integer, String>> middleCategoryMap,
		Map<List<Integer>, Map<Integer, String>> fineCategoryMap,
		Map<Integer, Map<Integer, Map<Integer, Integer>>> costMap,
		Map<Integer, Double> sgMap
	) {
		this.slipList = slipList;
		this.typeMap = typeMap;
		this.routeMap = routeMap;
		this.dueVector = dueVector;
		this.placeVector = placeVector;
		this.termVector = termVector;
		this.validityVector = validityVector;
		this.submitMap = submitMap;
		this.currencyMap = currencyMap;
		this.stateMap = stateMap;
		this.wayMap = wayMap;
		this.partialMap = partialMap;
		this.indicationMap = indicationMap;
		this.unitMap = unitMap;
		this.supplierMap = supplierMap;
		this.processingCostMap = processingCostMap;
		this.materialCostMap = materialCostMap;
		this.coarseCategoryMap = coarseCategoryMap;
		this.middleCategoryMap = middleCategoryMap;
		this.fineCategoryMap = fineCategoryMap;
		this.costMap = costMap;
		this.sgMap = sgMap;
	}

	public List<String> getSlipList() {
		return slipList;
	}

	public Map<Integer, String> getTypeMap() {
		return typeMap;
	}

	public Map<Integer, String> getRouteMap() {
		return routeMap;
	}

	public Vector<String> getTermVector() {
		return termVector;
	}

	public Vector<String> getDueVector() {
		return dueVector;
	}

	public Vector<String> getPlaceVector() {
		return placeVector;
	}

	public Vector<String> getValidityVector() {
		return validityVector;
	}

	public Map<Integer, String> getSubmitMap() {
		return submitMap;
	}

	public Map<Integer, String> getCurrencyMap() {
		return currencyMap;
	}

	public Map<Integer, String> getStateMap() {
		return stateMap;
	}

	public Map<Integer, String> getWayMap() {
		return wayMap;
	}

	public Map<Integer, String> getPartialMap() {
		return partialMap;
	}

	public Map<Integer, String> getIndicationMap() {
		return indicationMap;
	}

	public Map<Integer, String> getUnitMap() {
		return unitMap;
	}

	public Map<Integer, String> getSupplierMap() {
		return supplierMap;
	}

	public Map<Integer, Integer> getProcessingCostMap() {
		return processingCostMap;
	}

	public Map<Integer, String> getMaterialCostMap() {
		return materialCostMap;
	}

	public Map<Integer, String> getCoarseCategoryMap() {
		return coarseCategoryMap;
	}

	public Map<List<Integer>, Map<Integer, String>> getMiddleCategoryMap() {
		return middleCategoryMap;
	}

	public Map<List<Integer>, Map<Integer, String>> getFineCategoryMap() {
		return fineCategoryMap;
	}

	public Map<Integer, Map<Integer, Map<Integer, Integer>>> getCostMap() {
		return costMap;
	}

	public Map<Integer, Double> getSgMap() {
		return sgMap;
	}

	// 以下、更新があったマスタを部分的に後で差し替えるためのメソッド
	public void setDueVector(Vector<String> dueVector) {
		this.dueVector = dueVector;
	}

	public void setPlaceVector(Vector<String> placeVector) {
		this.placeVector = placeVector;
	}

	public void setTermVector(Vector<String> termVector) {
		this.termVector = termVector;
	}

	public void setValidityVector(Vector<String> validityVector) {
		this.validityVector = validityVector;
	}
}
