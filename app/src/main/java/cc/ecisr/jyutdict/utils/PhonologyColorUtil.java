package cc.ecisr.jyutdict.utils;

import java.nio.charset.StandardCharsets;

/** 與網頁端相同的 BLAKE2b 字串取色規則。 */
public final class PhonologyColorUtil {
    private static final long[] IV = {
            0x6a09e667f3bcc908L, 0xbb67ae8584caa73bL,
            0x3c6ef372fe94f82bL, 0xa54ff53a5f1d36f1L,
            0x510e527fade682d1L, 0x9b05688c2b3e6c1fL,
            0x1f83d9abfb41bd6bL, 0x5be0cd19137e2179L
    };
    private static final int[][] SIGMA = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},
            {14, 10, 4, 8, 9, 15, 13, 6, 1, 12, 0, 2, 11, 7, 5, 3},
            {11, 8, 12, 0, 5, 2, 15, 13, 10, 14, 3, 6, 7, 1, 9, 4},
            {7, 9, 3, 1, 13, 12, 11, 14, 2, 6, 5, 10, 4, 0, 15, 8},
            {9, 0, 5, 7, 2, 4, 10, 15, 14, 1, 11, 12, 6, 8, 3, 13},
            {2, 12, 6, 10, 0, 11, 8, 3, 4, 13, 7, 5, 15, 14, 1, 9},
            {12, 5, 1, 15, 14, 13, 4, 10, 0, 7, 6, 3, 9, 2, 8, 11},
            {13, 11, 7, 14, 12, 1, 3, 9, 5, 0, 15, 4, 8, 6, 2, 10},
            {6, 15, 14, 9, 11, 3, 0, 8, 12, 2, 13, 7, 1, 4, 10, 5},
            {10, 2, 8, 4, 7, 6, 1, 5, 15, 11, 9, 14, 3, 12, 13, 0},
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},
            {14, 10, 4, 8, 9, 15, 13, 6, 1, 12, 0, 2, 11, 7, 5, 3}
    };

    private PhonologyColorUtil() {
    }

    public static final class Colors {
        public final String accent;
        public final String accentDark;
        public final String surface;
        public final String surfaceDark;
        public final String stripe;
        public final String stripeDark;

        Colors(int hue, int lightSaturation, int darkSaturation, int surfaceSaturation) {
            accent = hsl(hue, lightSaturation, 43);
            accentDark = hsl(hue, Math.max(darkSaturation - 5, 34), 68);
            surface = hsl(hue, surfaceSaturation, 91);
            surfaceDark = hsl(hue, 18, 21);
            stripe = hsla(hue, lightSaturation, 43, 0.24);
            stripeDark = hsla(hue, Math.max(darkSaturation - 5, 34), 68, 0.18);
        }
    }

    public static Colors forValue(String value) {
        byte[] digest = blake2b8(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
        int hue = (((digest[0] & 0xff) << 8) | (digest[1] & 0xff)) % 360;
        int lightSaturation = 56 + (digest[2] & 0xff) % 13;
        int darkSaturation = 42 + (digest[2] & 0xff) % 11;
        int surfaceSaturation = 52 + (digest[3] & 0xff) % 9;
        return new Colors(hue, lightSaturation, darkSaturation, surfaceSaturation);
    }

    public static String normaliseCheckedFinal(String value) {
        if (value == null || value.isEmpty()) return "";
        if (value.endsWith("p")) return value.substring(0, value.length() - 1) + "m";
        if (value.endsWith("t")) return value.substring(0, value.length() - 1) + "n";
        if (value.endsWith("k")) return value.substring(0, value.length() - 1) + "ng";
        return value;
    }

    private static byte[] blake2b8(byte[] input) {
        long[] hash = IV.clone();
        hash[0] ^= 0x01010000L ^ 8L;
        int offset = 0;
        long count = 0;
        while (offset + 128 < input.length) {
            byte[] block = new byte[128];
            System.arraycopy(input, offset, block, 0, 128);
            count += 128;
            compress(hash, block, count, false);
            offset += 128;
        }
        byte[] finalBlock = new byte[128];
        int remaining = input.length - offset;
        System.arraycopy(input, offset, finalBlock, 0, remaining);
        count += remaining;
        compress(hash, finalBlock, count, true);
        byte[] output = new byte[8];
        for (int i = 0; i < output.length; i++) {
            output[i] = (byte) (hash[0] >>> (i * 8));
        }
        return output;
    }

    private static void compress(long[] hash, byte[] block, long count, boolean last) {
        long[] message = new long[16];
        for (int word = 0; word < 16; word++) {
            long value = 0;
            for (int index = 0; index < 8; index++) {
                value |= ((long) block[word * 8 + index] & 0xffL) << (index * 8);
            }
            message[word] = value;
        }
        long[] values = new long[16];
        System.arraycopy(hash, 0, values, 0, 8);
        System.arraycopy(IV, 0, values, 8, 8);
        values[12] ^= count;
        if (last) values[14] = ~values[14];
        for (int[] sigma : SIGMA) {
            mix(values, 0, 4, 8, 12, message[sigma[0]], message[sigma[1]]);
            mix(values, 1, 5, 9, 13, message[sigma[2]], message[sigma[3]]);
            mix(values, 2, 6, 10, 14, message[sigma[4]], message[sigma[5]]);
            mix(values, 3, 7, 11, 15, message[sigma[6]], message[sigma[7]]);
            mix(values, 0, 5, 10, 15, message[sigma[8]], message[sigma[9]]);
            mix(values, 1, 6, 11, 12, message[sigma[10]], message[sigma[11]]);
            mix(values, 2, 7, 8, 13, message[sigma[12]], message[sigma[13]]);
            mix(values, 3, 4, 9, 14, message[sigma[14]], message[sigma[15]]);
        }
        for (int index = 0; index < 8; index++) {
            hash[index] ^= values[index] ^ values[index + 8];
        }
    }

    private static void mix(long[] values, int a, int b, int c, int d,
                            long left, long right) {
        values[a] += values[b] + left;
        values[d] = Long.rotateRight(values[d] ^ values[a], 32);
        values[c] += values[d];
        values[b] = Long.rotateRight(values[b] ^ values[c], 24);
        values[a] += values[b] + right;
        values[d] = Long.rotateRight(values[d] ^ values[a], 16);
        values[c] += values[d];
        values[b] = Long.rotateRight(values[b] ^ values[c], 63);
    }

    private static String hsl(int hue, int saturation, int lightness) {
        return "hsl(" + hue + "," + saturation + "%," + lightness + "%)";
    }

    private static String hsla(int hue, int saturation, int lightness, double alpha) {
        return "hsla(" + hue + "," + saturation + "%," + lightness + "%," + alpha + ")";
    }
}
