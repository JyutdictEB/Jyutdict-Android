package cc.ecisr.jyutdict.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 可跨進程重啟、亦可由系統按空間需要清理的文字快取。
 * 適合只讀文章與生成報告，避免每次打開頁面都請求伺服器。
 */
public final class DiskTextCache {
    private static final String DIRECTORY = "readonly_http";

    private DiskTextCache() {
    }

    public static String readFresh(Context context, String key, long maxAgeMillis) {
        File file = fileFor(context, key);
        if (!file.isFile()) return null;
        if (System.currentTimeMillis() - file.lastModified() > maxAgeMillis) return null;
        return read(file);
    }

    public static String readAny(Context context, String key) {
        File file = fileFor(context, key);
        return file.isFile() ? read(file) : null;
    }

    public static boolean write(Context context, String key, String value) {
        File directory = new File(context.getCacheDir(), DIRECTORY);
        if (!directory.isDirectory() && !directory.mkdirs()) return false;
        File target = fileFor(context, key);
        File temporary = new File(directory, target.getName() + ".tmp");
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(temporary), StandardCharsets.UTF_8)) {
            writer.write(value == null ? "" : value);
        } catch (IOException e) {
            return false;
        }
        if (target.exists() && !target.delete()) return false;
        return temporary.renameTo(target);
    }

    /** Clears only the recreatable read-only HTTP cache owned by this helper. */
    public static int clear(Context context) {
        File directory = new File(context.getCacheDir(), DIRECTORY);
        File[] files = directory.listFiles();
        if (files == null) return 0;
        int deleted = 0;
        for (File file : files) {
            if (file.isFile() && file.delete()) deleted++;
        }
        if (directory.isDirectory()) directory.delete();
        return deleted;
    }

    private static String read(File file) {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append('\n');
            }
            return result.toString();
        } catch (IOException e) {
            return null;
        }
    }

    private static File fileFor(Context context, String key) {
        return new File(new File(context.getCacheDir(), DIRECTORY), digest(key) + ".txt");
    }

    private static String digest(String key) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(String.valueOf(key).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) result.append(String.format("%02x", value & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(String.valueOf(key).hashCode());
        }
    }
}
