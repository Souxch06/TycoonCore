package fr.valoriatycoon.crates;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** Small deterministic key/value codec stored immutably with a committed crate reward. */
public record CrateRewardPayload(Map<String, String> values) {
    private static final Pattern KEY = Pattern.compile("[a-z][a-z0-9_]{0,31}");
    private static final Pattern VALUE = Pattern.compile("[A-Za-z0-9_.,-]{1,512}");

    public CrateRewardPayload {
        Objects.requireNonNull(values, "values");
        Map<String, String> sorted = new TreeMap<>();
        values.forEach((key, value) -> {
            if (key == null || value == null || !KEY.matcher(key).matches() || !VALUE.matcher(value).matches()) {
                throw new IllegalArgumentException("Invalid crate reward payload entry");
            }
            sorted.put(key, value);
        });
        if (sorted.isEmpty() || sorted.size() > 12) {
            throw new IllegalArgumentException("Crate reward payload must contain 1 to 12 entries");
        }
        values = Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }

    public static CrateRewardPayload of(String key, Object value) {
        return new CrateRewardPayload(Map.of(key, Objects.toString(value)));
    }

    public String require(String key) {
        String value = values.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing crate reward payload field: " + key);
        }
        return value;
    }

    public long requireLong(String key) {
        try {
            return Long.parseLong(require(key));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid long crate reward payload field: " + key, exception);
        }
    }

    public int requireInt(String key) {
        long value = requireLong(key);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Out-of-range crate reward payload field: " + key);
        }
        return (int) value;
    }

    public String encode() {
        return values.entrySet().stream()
                .map(entry -> entry.getKey() + '=' + entry.getValue())
                .collect(java.util.stream.Collectors.joining(";"));
    }

    public static CrateRewardPayload decode(String encoded) {
        if (encoded == null || encoded.isBlank() || encoded.length() > 2_048) {
            throw new IllegalArgumentException("Invalid encoded crate reward payload");
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String entry : encoded.split(";", -1)) {
            int separator = entry.indexOf('=');
            if (separator <= 0 || separator == entry.length() - 1) {
                throw new IllegalArgumentException("Malformed crate reward payload");
            }
            String key = entry.substring(0, separator);
            String value = entry.substring(separator + 1);
            if (values.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("Duplicate crate reward payload field: " + key);
            }
        }
        return new CrateRewardPayload(values);
    }
}
