/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto;

import java.io.Serializable;
import java.util.Map;

/**
 *
 * @author kameura
 */
public class ChartDTO implements Serializable {
	String name, display;
	Map<Integer, String> contacts;
	Map<Integer, String> models;
	Map<Integer, ProductNumber> numbers;
	public ChartDTO(String name, String display, Map<Integer, String> contacts, Map<Integer, String> models, Map<Integer, ProductNumber> numbers) {
		this.name = name;
		this.display = display;
		this.contacts = contacts;
		this.models = models;
		this.numbers = numbers;
	}
	public String getName() {
		return name;
	}
	public String getDisplay() {
		return display;
	}
	
	public Map<Integer, String> getContacts() {
		return contacts;
	}
	public Map<Integer, String> getModels() {
		return models;
	}
	public Map<Integer, ProductNumber> getNumbers() {
		return numbers;
	}
}
