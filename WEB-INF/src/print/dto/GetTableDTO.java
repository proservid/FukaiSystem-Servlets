/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package print.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 *
 * @author kameura
 */
public class GetTableDTO implements Serializable {
	Map<String, List<String>> tables;
	Map<String, Map<String, Object>> conditions;
	String order;

	public GetTableDTO(
		Map<String, List<String>> tables,
		Map<String, Map<String, Object>> conditions,
		String order
	) {
		this.tables = tables;
		this.conditions = conditions;
		this.order = order.isEmpty() ? "" : " ORDER BY " + order;
	}

	public Map<String, List<String>> getTables() {
		return tables;
	}

	public Map<String, Map<String, Object>> getConditions() {
		return conditions;
	}

	public String getOrder() {
		return order;
	}
}
 