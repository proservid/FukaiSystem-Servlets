/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto;

import java.io.Serializable;
import java.sql.Date;
import java.util.List;
import java.util.Vector;

/**
 *
 * @author kameura
 */
public abstract class BasicDTO implements Serializable {

	public abstract String getStr(int order);

	public abstract int getInt(int order);

	public abstract boolean getBool(int order);

	public abstract Date getDate(int order);

	public abstract Vector<Vector<Object>> getVector(int order);

	public abstract List<String> getQuotationNumbers();

	public abstract List<Integer> getParents();
}
 