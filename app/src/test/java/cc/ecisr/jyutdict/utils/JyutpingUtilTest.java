package cc.ecisr.jyutdict.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class JyutpingUtilTest {
    @Test
    public void acceptsWildcardInSecondTonePosition() {
        assertTrue(JyutpingUtil.isValidJpp("goek**"));
        assertTrue(JyutpingUtil.isValidJpp("goek1**"));
        assertArrayEquals(
                new String[]{"g", "oek", "**"},
                JyutpingUtil.splitJyutping("goek**")
        );
        assertArrayEquals(
                new String[]{"g", "oek", "1**"},
                JyutpingUtil.splitJyutping("goek1**")
        );
    }

    @Test
    public void colourHashMatchesWebImplementation() {
        PhonologyColorUtil.Colors colors = PhonologyColorUtil.forValue("aa");
        assertEquals("hsl(311,67%,43%)", colors.accent);
        assertEquals("hsl(311,47%,68%)", colors.accentDark);
        assertEquals("hsl(311,55%,91%)", colors.surface);
    }
}
