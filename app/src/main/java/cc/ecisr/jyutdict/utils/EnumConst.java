package cc.ecisr.jyutdict.utils;

public class EnumConst {

	// 0b0000 0000 00[11 11][11]
	public static final int QUERYING_CHARA = 0b01;
	public static final int QUERYING_PRON = 0b10;
	public static final int QUERYING_SHEET = 0b11;
	public static final int QUERYING_MODE_MASK = 0b11;

	public static final int DISPLAY_CHECKING_IS_INNER = 1 << 2;
	public static final int DISPLAY_CHECKING_INI = 1 << 3;
	public static final int DISPLAY_CHECKING_FIN = 1 << 4;
	public static final int DISPLAY_CHECKING_TON = 1 << 5;
	public static final int DISPLAY_CHECKING_MASK = 0b111100;

	
	public static final int INITIALIZE_LOCATIONS = 3286;
	public static final int CHECKING_VERSION = 3453;
	
	public static final int GETTING_CONTENT_LOCATION = 0;
	public static final int GETTING_CONTENT_WANSHYU = 1;
	
	public static final int ACTIVITY_REQUESTING_SETTING = 8308;
	public static final int ACTIVITY_CHECKING_INFO_PRIVACY = 8309;
}
