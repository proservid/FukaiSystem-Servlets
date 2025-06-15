/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto;

//import fukaisystem.net.dto.QuotationDTO.Item.Content;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author kameura
 */
public class QuotationDTO implements Serializable {

	String quotationID;
	List<Item> documents;

	public QuotationDTO(String quotationID, List<Item> documents) {
		this.quotationID = quotationID;
		this.documents = documents;
	}

	public class Item {

		String name, figureNum, note;
		int indication, quantity, unitPrice, itemNum, unitWeight, price;
		List<Content> content;

		public Item(
			int indication,
			String name,
			int quantity,
			int unitPrice,
			String figureNum,
			int itemNum,
			int unitWeight,
			int price,
			String note,
			List<Content> content
		) {
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

			int coarseCategory, middleCategory, fineCategory, unitPrice, amount;
			float ratio;
			String material, note;

			public Content(
				int coarseCategory,
				int middleCategory,
				int fineCategory,
				String material,
				int unitPrice,
				int amount,
				float ratio,
				String note
			) {
				this.coarseCategory = coarseCategory;
				this.middleCategory = middleCategory;
				this.fineCategory = fineCategory;
				this.material = material;
				this.unitPrice = unitPrice;
				this.amount = amount;
				this.ratio = ratio;
				this.note = note;
			}
		}
	}
}
