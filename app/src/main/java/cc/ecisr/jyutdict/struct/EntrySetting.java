package cc.ecisr.jyutdict.struct;

/**
 * EntrySetting 類，用於控制 Character 類輸出顯示內容的格式
 * 僅供 Character 類使用
 */
public class EntrySetting {
	// 未使用
	// 用於區分輸出不同樣式的字項
	int type;
	
	// 是否啟用地區名著色
	boolean isAreaColoring = true;
	// 地區名將著色時，明度會先乘以下面這個係數
	float areaColoringDarkenRatio = 0.92f;
	
	// 是否顯示詞場
	boolean isMeaningDomainPresence = false;
	
	// 是否顯示域外音
	boolean isDisplayEcdemic = true;
	
	public EntrySetting(int type) {
		this.type = type;
	}
	
	public EntrySetting setAreaColoringInfo(boolean isColoring, float coloringDarkenRatio) {
		this.isAreaColoring = isColoring;
		this.areaColoringDarkenRatio = coloringDarkenRatio;
		this.isDisplayEcdemic = true;
		return this;
	}
	public EntrySetting setAreaColoringInfo(boolean isColoring, float coloringDarkenRatio, boolean isDisplayEcdemic) {
		setAreaColoringInfo(isColoring, coloringDarkenRatio);
		this.isDisplayEcdemic = isDisplayEcdemic;
		return this;
	}
	
	public EntrySetting setMeaningDomainPresence(boolean isMeaningDomainPresence) {
		this.isMeaningDomainPresence = isMeaningDomainPresence;
		return this;
	}
}
