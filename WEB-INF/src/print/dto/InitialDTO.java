/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package print.dto;

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
	Vector<String> deadlines;
	Vector<String> places;
	Vector<String> terms;
	Vector<String> validities;
	Map<Integer, String> submits;
	Map<Integer, String> currencies;
	Map<Integer, String> states;
	Map<Integer, String> ways;
	Map<Integer, String> partials;
	Map<Integer, String> indication;
	Map<Integer, String> units;
	Map<Integer, String> suppliers;
	Map<Integer, Integer> works;
	Map<Integer, String> material;
	Map<Integer, String> categoryL;
	Map<List<Integer>, Map<Integer, String>> categoryM;
	Map<List<Integer>, Map<Integer, String>> categoryS;
	Map<Integer, Map<Integer, Map<Integer, Integer>>> prices;
	Map<Integer, Double> sg;

	public InitialDTO(
		List<String> slips,
		Map<Integer, String> types,
		Map<Integer, String> routes,
		Vector<String> deadlines,
		Vector<String> places,
		Vector<String> terms,
		Vector<String> validities,
		Map<Integer, String> submits,
		Map<Integer, String> currencies,
		Map<Integer, String> states,
		Map<Integer, String> ways,
		Map<Integer, String> partials,
		Map<Integer, String> indication,
		Map<Integer, String> units,
		Map<Integer, String> suppliers,
		Map<Integer, Integer> works,
		Map<Integer, String> material,
		Map<Integer, String> categoryL,
		Map<List<Integer>, Map<Integer, String>> categoryM,
		Map<List<Integer>, Map<Integer, String>> categoryS,
		Map<Integer, Map<Integer, Map<Integer, Integer>>> prices,
		Map<Integer, Double> sg
	) {
		this.slips = slips;
		this.types = types;
		this.routes = routes;
		this.deadlines = deadlines;
		this.places = places;
		this.terms = terms;
		this.validities = validities;
		this.submits = submits;
		this.currencies = currencies;
		this.states = states;
		this.ways = ways;
		this.partials = partials;
		this.indication = indication;
		this.units = units;
		this.suppliers = suppliers;
		this.works = works;
		this.material = material;
		this.categoryL = categoryL;
		this.categoryM = categoryM;
		this.categoryS = categoryS;
		this.prices = prices;
		this.sg = sg;
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

	public Vector<String> getDeadlines() {
		return deadlines;
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

	public Map<Integer, String> getIndication() {
		return indication;
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
		return material;
	}

	public Map<Integer, String> getCategoryL() {
		return categoryL;
	}

	public Map<List<Integer>, Map<Integer, String>> getCategoryM() {
		return categoryM;
	}

	public Map<List<Integer>, Map<Integer, String>> getCategoryS() {
		return categoryS;
	}

	public Map<Integer, Map<Integer, Map<Integer, Integer>>> getPrices() {
		return prices;
	}

	public Map<Integer, Double> getSG() {
		return sg;
	}
}
