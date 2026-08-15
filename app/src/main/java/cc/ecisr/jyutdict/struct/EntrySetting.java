package cc.ecisr.jyutdict.struct;

/**
 * EntrySetting 類，用於控制 FjbCharacter 類輸出顯示內容的格式
 * 僅供 FjbCharacter 類使用
 */
public class EntrySetting {
	// 是否啟用地區名著色
	boolean isAreaColoring = true;
	// 地區名將著色時，明度會先乘以下面這個係數
	float areaColoringDarkenRatio = 0.92f;
	
	// 是否顯示詞場
	boolean isMeaningDomainPresence = false;
	
	boolean isUsingNightMode = false;

	boolean isPresentIpa = true;
	
	public EntrySetting setAreaColoringInfo(boolean isColoring, float coloringDarkenRatio) {
		this.isAreaColoring = isColoring;
		this.areaColoringDarkenRatio = coloringDarkenRatio;
		return this;
	}
	
	public EntrySetting setMeaningDomainPresence(boolean isMeaningDomainPresence) {
		this.isMeaningDomainPresence = isMeaningDomainPresence;
		return this;
	}
	
	public EntrySetting setUsingNightMode(boolean isUsingNightMode) {
		this.isUsingNightMode = isUsingNightMode;
		return this;
	}
	public EntrySetting setPresentIpa(boolean isPresentIpa) {
		this.isPresentIpa = isPresentIpa;
		return this;
	}

	public boolean isAreaColoring() {
		return isAreaColoring;
	}

	public float getAreaColoringDarkenRatio() {
		return areaColoringDarkenRatio;
	}

	public boolean isUsingNightMode() {
		return isUsingNightMode;
	}
}
