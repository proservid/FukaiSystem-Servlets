/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto.schedule;

import java.io.Serializable;
import java.sql.Date;
import java.util.List;
import java.util.Map;

/**
 *
 * @author kameura
 */
public class ScheduleDTO implements Serializable {
	Map<String, Schedule> map;
	List<Date> holidays;
	public ScheduleDTO(Map<String, Schedule> map, List<Date> holidays) {
		this.map = map;
		this.holidays = holidays;
	}
	public Map<String, Schedule> getMap() {
		return map;
	}
	public List<Date> getHolidays() {
		return holidays;
	}
}
