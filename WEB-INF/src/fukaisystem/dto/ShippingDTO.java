package fukaisystem.dto;

import java.io.Serializable;
import java.sql.Date;
import java.util.Vector;

public class ShippingDTO implements Serializable {
	int shippingID;
	int accountCode;
	String distinationName;
	int shippingNum1;
	int shippingNum2;
	Date publishDate;
	int shippingMonth;
	int shippingDay;
	int shippingWay;
	int productionID;
	Vector<Vector<Object>> vector;

	public ShippingDTO(
		int shippingID,
		int accountCode,
		String distinationName,
		int shippingNum1,
		int shippingNum2,
		Date publishDate,
		int shippingMonth,
		int shippingDay,
		int shippingWay,
		int productionID,
		Vector<Vector<Object>> vector
	) {
		this.shippingID = shippingID;
		this.accountCode = accountCode;
		this.distinationName = distinationName;
		this.shippingNum1 = shippingNum1;
		this.shippingNum2 = shippingNum2;
		this.publishDate = publishDate;
		this.shippingMonth = shippingMonth;
		this.shippingDay = shippingDay;
		this.shippingWay = shippingWay;
		this.productionID = productionID;
		this.vector = vector;
	}

	public int shippingID() {
		return shippingID;
	}

	public int accountCode() {
		return accountCode;
	}

	public String distinationName() {
		return distinationName;
	}

	public int shippingNum1() {
		return shippingNum1;
	}

	public int shippingNum2() {
		return shippingNum2;
	}

	public Date publishDate() {
		return publishDate;
	}

	public int shippingMonth() {
		return shippingMonth;
	}

	public int shippingDay() {
		return shippingDay;
	}

	public int shippingWay() {
		return shippingWay;
	}

	public int productionID() {
		return productionID;
	}

	public Vector<Vector<Object>> getVector() {
		return vector;
	}

	public void setVector(Vector<Vector<Object>> vector) {
		this.vector = vector;
	}
}
