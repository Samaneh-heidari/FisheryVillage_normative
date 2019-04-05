package normFramework;

import lombok.Getter;

@Getter
public enum NormTypeTitle {
	
	ACTION("DONATION");
	
	private String normTitle; 
	private NormTypeTitle(String title) {
		normTitle = title;
	}
	
}