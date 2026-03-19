package com.rcs.system.accupick;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public final class KvCsvUtil {
    private KvCsvUtil() {}

    public static String encode(Map<String, String> values) {
        StringJoiner joiner = new StringJoiner(",");
        values.forEach((key, value) -> {
            if (key == null || key.isBlank() || value == null || value.isBlank()) {
                return;
            }
            validate(key);
            validate(value);
            joiner.add(key).add(value);
        });
        return joiner.toString();
    }

    public static Map<String, String> decode(String payload) {
        Map<String, String> values = new LinkedHashMap<>();
        if (payload == null || payload.isBlank()) {
            return values;
        }
        String[] parts = payload.trim().split(",", -1);
        for (int i = 0; i + 1 < parts.length; i += 2) {
            values.put(parts[i].trim(), parts[i + 1].trim());
        }
        return values;
    }

    private static void validate(String value) {
        if (value.contains(",") || value.contains("\n") || value.contains("\r") || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("KV-CSV value contains invalid characters: " + value);
        }
    }
}
