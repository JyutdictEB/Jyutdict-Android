package cc.ecisr.jyutdict.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import android.graphics.Color;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import cc.ecisr.jyutdict.struct.FjbHeaderInfo;

@RunWith(AndroidJUnit4.class)
public class ColorUtilInstrumentedTest {
    @Test
    public void parsesDelimitedAndArrayLocationColorsWithoutDroppingEntries() throws Exception {
        ArrayList<String> colors = ColorUtil.parseLocationColors(
                "#FF0000,#00FF00",
                new JSONArray("[\"#0000FF\",\"invalid\"]")
        );

        assertEquals(3, colors.size());
        assertArrayEquals(
                new int[]{Color.RED, Color.GREEN, Color.BLUE},
                ColorUtil.locationColorInts(colors)
        );
    }

    @Test
    public void sheetHeaderKeepsAllColorsForEveryColumnKind() throws Exception {
        FjbHeaderInfo.load(new JSONArray("["
                + "{\"index\":1,\"col\":\"綜\",\"kind\":0,"
                + "\"color\":\"#FF0000,#00FF00\"},"
                + "{\"index\":2,\"col\":\"港\",\"kind\":1,"
                + "\"colors\":[\"#0000FF\",\"#FFFF00\"]}"
                + "]"));

        assertArrayEquals(
                new int[]{Color.RED, Color.GREEN},
                ColorUtil.locationColorInts(FjbHeaderInfo.getColumnColors("綜"))
        );
        assertArrayEquals(
                new int[]{Color.BLUE, Color.YELLOW},
                ColorUtil.locationColorInts(FjbHeaderInfo.getColumnColors("港"))
        );
    }
}
