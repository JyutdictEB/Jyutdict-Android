package cc.ecisr.jyutdict.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StringUtilTest {
    @Test
    public void sheetQueryDistinguishesPronunciationFromCharacters() {
        assertTrue(StringUtil.isSheetPronunciationInput("gwong2"));
        assertTrue(StringUtil.isSheetPronunciationInput("gwong2 zau1"));
        assertTrue(StringUtil.isSheetPronunciationInput("gwong*"));

        assertFalse(StringUtil.isSheetPronunciationInput("廣"));
        assertFalse(StringUtil.isSheetPronunciationInput("A廣"));
        assertFalse(StringUtil.isSheetPronunciationInput(""));
        assertFalse(StringUtil.isSheetPronunciationInput(null));
    }
}
