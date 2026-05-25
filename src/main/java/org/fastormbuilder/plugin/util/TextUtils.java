package org.fastormbuilder.plugin.util;

import java.io.*;
import java.nio.charset.Charset;

public class TextUtils {
    public static boolean hasValue(String str) {
        return str != null && !str.isEmpty() && !str.trim().isEmpty();
    }

    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static String valueOf(Object object) {
        return object == null ? null : object.toString();
    }

    public static String readFile(File file, Charset charset) throws IOException {
        try (Reader reader = new InputStreamReader(new FileInputStream(file), charset)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = reader.read(buf)) != -1) sb.append(buf, 0, n);
            return sb.toString();
        }
    }
}
