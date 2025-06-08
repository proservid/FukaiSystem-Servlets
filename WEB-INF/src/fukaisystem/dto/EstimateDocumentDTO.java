/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto;

//import fukaisystem.net.dto.EstimateDocumentDTO.Item.Content;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author kameura
 */
public class EstimateDocumentDTO implements Serializable {

	String estimateID;
	List<Item> documents;

	public EstimateDocumentDTO(String estimateID, List<Item> documents) {
		this.estimateID = estimateID;
		this.documents = documents;
	}

	public class Item {

		String name, figureNum, note;
		int indication, quantity, unitPrice, itemNum, unitWeight, price;
		List<Content> content;

		public Item(int indication, String name, int quantity, int unitPrice,
		String figureNum, int itemNum, int unitWeight, int price, String note,
		List<Content> content) {
			this.indication = indication;
			this.name = name;
			this.quantity = quantity;
			this.unitPrice = unitPrice;
			this.figureNum = figureNum;
			this.itemNum = itemNum;
			this.unitWeight = unitWeight;
			this.price = price;
			this.note = note;
			this.content = content;
		}

		public class Content {

			int categoryL, categoryM, categoryS, unitPrice, amount;
			float ratio;
			String material, note;

			public Content(int categoryL, int categoryM, int categoryS, String material,
			int unitPrice, int amount, float ratio, String note) {
				this.categoryL = categoryL;
				this.categoryM = categoryM;
				this.categoryS = categoryS;
				this.material = material;
				this.unitPrice = unitPrice;
				this.amount = amount;
				this.ratio = ratio;
				this.note = note;
			}
		}
	}
}

