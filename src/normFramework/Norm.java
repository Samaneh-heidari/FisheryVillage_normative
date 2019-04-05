package normFramework;

import lombok.Getter;
import lombok.Setter;


public class Norm {
	private String type = ""; //it can be action, intention, preference, decision style, or anything that can be a norm
	private String title = "";// i.e. |donation| > 10
	private int repetition = 0;
	private int noRepetition = 0;
	private int groupId = -1;
	
	public Norm(String normType, String normTitle, int groupID) {
		setType(normType);
		setTitle(normTitle);
		setGroupId(groupID);
	}
	
	public void increaseRepeatingNorm(){
		setRepetition(getRepetition() + 1);
	}
	
	public void incraseNoRepetition(){
		this.noRepetition ++;
	}
	
	public void resetNorm(){
		setRepetition(0);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title1) {
		this.title = title1;
	}

	public int getRepetition() {
		return repetition;
	}

	public void setRepetition(int repetition1) {
		this.repetition = repetition1;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public int getNoRepetition() {
		return noRepetition;
	}

	public void setNoRepetition(int noRepetition) {
		this.noRepetition = noRepetition;
	}
	
}
