/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 *
 * @author kameura
 */
public class InitialDTO implements Serializable {

	List<String> slips;
	Map<Integer, String> types;
	Map<Integer, String> routes;
	Vector<String> dues;
	Vector<String> places;
	Vector<String> terms;
	Vector<String> validities;
	Map<Integer, String> submits;
	Map<Integer, String> currencies;
	Map<Integer, String> states;
	Map<Integer, String> ways;
	Map<Integer, String> partials;
	Map<Integer, String> indications;
	Map<Integer, String> units;
	Map<Integer, String> suppliers;
	Map<Integer, Integer> works;
	Map<Integer, String> materials;
	Map<Integer, String> coarseCategories;
	Map<List<Integer>, Map<Integer, String>> middleCategories;
	Map<List<Integer>, Map<Integer, String>> fineCategories;
	Map<Integer, Map<Integer, Map<Integer, Integer>>> costs;
	// 比重
	Map<Integer, Double> sgs;

	public InitialDTO(
		List<String> slips,
		Map<Integer, String> types,
		Map<Integer, String> routes,
		Vector<String> dues,
		Vector<String> places,
		Vector<String> terms,
		Vector<String> validities,
		Map<Integer, String> submits,
		Map<Integer, String> currencies,
		Map<Integer, String> states,
		Map<Integer, String> ways,
		Map<Integer, String> partials,
		Map<Integer, String> indications,
		Map<Integer, String> units,
		Map<Integer, String> suppliers,
		Map<Integer, Integer> works,
		Map<Integer, String> materials,
		Map<Integer, String> coarseCategories,
		Map<List<Integer>, Map<Integer, String>> middleCategories,
		Map<List<Integer>, Map<Integer, String>> fineCategories,
		Map<Integer, Map<Integer, Map<Integer, Integer>>> costs,
		Map<Integer, Double> sgs
	) {
		this.slips = slips;
		this.types = types;
		this.routes = routes;
		this.dues = dues;
		this.places = places;
		this.terms = terms;
		this.validities = validities;
		this.submits = submits;
		this.currencies = currencies;
		this.states = states;
		this.ways = ways;
		this.partials = partials;
		this.indications = indications;
		this.units = units;
		this.suppliers = suppliers;
		this.works = works;
		this.materials = materials;
		this.coarseCategories = coarseCategories;
		this.middleCategories = middleCategories;
		this.fineCategories = fineCategories;
		this.costs = costs;
		this.sgs = sgs;
	}

	public List<String> getSlips() {
		return slips;
	}

	public Map<Integer, String> getTypes() {
		return types;
	}

	public Map<Integer, String> getRoutes() {
		return routes;
	}

	public Vector<String> getTerms() {
		return terms;
	}

	public Vector<String> getDues() {
		return dues;
	}

	public Vector<String> getPlaces() {
		return places;
	}

	public Vector<String> getValidities() {
		return validities;
	}

	public Map<Integer, String> getSubmits() {
		return submits;
	}

	public Map<Integer, String> getCurrencies() {
		return currencies;
	}

	public Map<Integer, String> getStates() {
		return states;
	}

	public Map<Integer, String> getWays() {
		return ways;
	}

	public Map<Integer, String> getPartials() {
		return partials;
	}

	public Map<Integer, String> getIndications() {
		return indications;
	}

	public Map<Integer, String> getUnits() {
		return units;
	}

	public Map<Integer, String> getSuppliers() {
		return suppliers;
	}

	public Map<Integer, Integer> getWorks() {
		return works;
	}

	public Map<Integer, String> getMaterial() {
		return materials;
	}

	public Map<Integer, String> getcoarseCategories() {
		return coarseCategories;
	}

	public Map<List<Integer>, Map<Integer, String>> getmiddleCategories() {
		return middleCategories;
	}

	public Map<List<Integer>, Map<Integer, String>> getfineCategories() {
		return fineCategories;
	}

	public Map<Integer, Map<Integer, Map<Integer, Integer>>> getCosts() {
		return costs;
	}

	public Map<Integer, Double> getSGs() {
		return sgs;
	}
}
