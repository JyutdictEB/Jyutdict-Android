package cc.ecisr.jyutdict.utils;

public final class EnumConst {
    private EnumConst() {}

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
}
