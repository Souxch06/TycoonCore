/*
 * Decompiled with CFR 0.152.
 */
package marcono1234.gson.recordadapter;

import com.google.gson.FieldNamingPolicy;
import java.lang.reflect.RecordComponent;
import java.util.Locale;

public interface RecordComponentNamingStrategy {
    public static final RecordComponentNamingStrategy IDENTITY = new RecordComponentNamingStrategy(){

        @Override
        public String translateName(RecordComponent recordComponent) {
            return recordComponent.getName();
        }

        public String toString() {
            return "IDENTITY";
        }
    };
    public static final RecordComponentNamingStrategy UPPER_CAMEL_CASE = new RecordComponentNamingStrategy(){

        @Override
        public String translateName(RecordComponent recordComponent) {
            return RecordComponentNamingStrategy.uppercaseFirstLetter(recordComponent.getName());
        }

        public String toString() {
            return "UPPER_CAMEL_CASE";
        }
    };
    public static final RecordComponentNamingStrategy UPPER_CAMEL_CASE_WITH_SPACES = new RecordComponentNamingStrategy(){

        @Override
        public String translateName(RecordComponent recordComponent) {
            return RecordComponentNamingStrategy.uppercaseFirstLetter(RecordComponentNamingStrategy.separateCamelCase(recordComponent.getName(), ' '));
        }

        public String toString() {
            return "UPPER_CAMEL_CASE_WITH_SPACES";
        }
    };
    public static final RecordComponentNamingStrategy UPPER_CASE_WITH_UNDERSCORES = new RecordComponentNamingStrategy(){

        @Override
        public String translateName(RecordComponent recordComponent) {
            return RecordComponentNamingStrategy.uppercase(RecordComponentNamingStrategy.separateCamelCase(recordComponent.getName(), '_'));
        }

        public String toString() {
            return "UPPER_CASE_WITH_UNDERSCORES";
        }
    };
    public static final RecordComponentNamingStrategy LOWER_CASE_WITH_UNDERSCORES = new RecordComponentNamingStrategy(){

        @Override
        public String translateName(RecordComponent recordComponent) {
            return RecordComponentNamingStrategy.lowercase(RecordComponentNamingStrategy.separateCamelCase(recordComponent.getName(), '_'));
        }

        public String toString() {
            return "LOWER_CASE_WITH_UNDERSCORES";
        }
    };
    public static final RecordComponentNamingStrategy LOWER_CASE_WITH_DASHES = new RecordComponentNamingStrategy(){

        @Override
        public String translateName(RecordComponent recordComponent) {
            return RecordComponentNamingStrategy.lowercase(RecordComponentNamingStrategy.separateCamelCase(recordComponent.getName(), '-'));
        }

        public String toString() {
            return "LOWER_CASE_WITH_DASHES";
        }
    };
    public static final RecordComponentNamingStrategy LOWER_CASE_WITH_DOTS = new RecordComponentNamingStrategy(){

        @Override
        public String translateName(RecordComponent recordComponent) {
            return RecordComponentNamingStrategy.lowercase(RecordComponentNamingStrategy.separateCamelCase(recordComponent.getName(), '.'));
        }

        public String toString() {
            return "LOWER_CASE_WITH_DOTS";
        }
    };

    private static String uppercaseFirstLetter(String s) {
        int length = s.length();
        for (int i = 0; i < length; ++i) {
            char c = s.charAt(i);
            if (!Character.isLetter(c)) continue;
            if (Character.isUpperCase(c)) {
                return s;
            }
            char uppercased = Character.toUpperCase(c);
            if (i == 0) {
                return uppercased + s.substring(1);
            }
            return s.substring(0, i) + uppercased + s.substring(i + 1);
        }
        return s;
    }

    private static String separateCamelCase(String s, char separator) {
        StringBuilder sb = new StringBuilder();
        int length = s.length();
        int nextSectionIndex = 0;
        for (int i = 1; i < length; ++i) {
            if (!Character.isUpperCase(s.charAt(i))) continue;
            sb.append(s, nextSectionIndex, i);
            sb.append(separator);
            nextSectionIndex = i;
        }
        if (nextSectionIndex == 0) {
            return s;
        }
        sb.append(s.substring(nextSectionIndex));
        return sb.toString();
    }

    private static String uppercase(String s) {
        return s.toUpperCase(Locale.ENGLISH);
    }

    private static String lowercase(String s) {
        return s.toLowerCase(Locale.ENGLISH);
    }

    public static RecordComponentNamingStrategy fromFieldNamingPolicy(FieldNamingPolicy policy) throws IllegalArgumentException {
        return switch (policy) {
            case FieldNamingPolicy.IDENTITY -> IDENTITY;
            case FieldNamingPolicy.UPPER_CAMEL_CASE -> UPPER_CAMEL_CASE;
            case FieldNamingPolicy.UPPER_CAMEL_CASE_WITH_SPACES -> UPPER_CAMEL_CASE_WITH_SPACES;
            case FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES -> LOWER_CASE_WITH_UNDERSCORES;
            case FieldNamingPolicy.LOWER_CASE_WITH_DASHES -> LOWER_CASE_WITH_DASHES;
            case FieldNamingPolicy.LOWER_CASE_WITH_DOTS -> LOWER_CASE_WITH_DOTS;
            case FieldNamingPolicy.UPPER_CASE_WITH_UNDERSCORES -> UPPER_CASE_WITH_UNDERSCORES;
            default -> throw new IllegalArgumentException("Unsupported field naming policy " + policy);
        };
    }

    public String translateName(RecordComponent var1);
}

