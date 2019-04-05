package normFramework;

import java.util.ArrayList;

import lombok.EqualsAndHashCode;
import repast.simphony.space.continuous.NdPoint;
import fisheryvillage.common.Logger;
import fisheryvillage.common.SimUtils;
import valueframework.common.Log;

@EqualsAndHashCode
public class Group {
	private int id;
	private String title;

	//in the case of neighboring groups, group title is the houseType.
	
	public Group(int gid, String ntitle){
		id = gid;
		this.title = ntitle;
		addToContext();
	}
	
	private void addToContext() {		
		SimUtils.getContext().add(this);		
	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
}
