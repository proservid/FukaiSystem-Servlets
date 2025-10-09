/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto;

import java.io.Serializable;

import com.proservid.print.slip.Slip;

/**
 *
 * @author kameura
 */
public class FormatDTO implements Serializable {

	private boolean isOverwrite;
	private String name;
	private String newName;
	private Slip format;

	/**
	 * 保存用
	 * 
	 * @param isOverwrite 上書き保存の場合はtrue
	 * @param name フォーマット名
	 * @param format Slipオブジェクト
	 */
	public FormatDTO(boolean isOverwrite, String name, Slip format) {
		this.isOverwrite = isOverwrite;
		this.name = name;
		this.format = format;
	}

	/**
	 * 名前変更用
	 * 
	 * @param isOverwrite 上書き保存の場合はtrue
	 * @param name 変更前のフォーマット名
	 * @param newName 変更後のフォーマット名
	 * @param format Slipオブジェクト
	 */
	public FormatDTO(boolean isOverwrite, String name, String newName, Slip format) {
		this.isOverwrite = isOverwrite;
		this.name = name;
		this.newName = newName;
		this.format = format;
	}

	public boolean isOverwrite() {
		return isOverwrite;
	}

	public String getName() {
		return name;
	}

	public String getNewName() {
		return newName;
	}

	public Slip getformat() {
		return format;
	}

}
