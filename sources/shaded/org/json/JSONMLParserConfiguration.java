/*
 * Decompiled with CFR 0.152.
 */
package org.json;

import org.json.ParserConfiguration;

public class JSONMLParserConfiguration
extends ParserConfiguration {
    public static final int DEFAULT_MAXIMUM_NESTING_DEPTH = 512;
    public static final JSONMLParserConfiguration ORIGINAL = new JSONMLParserConfiguration();
    public static final JSONMLParserConfiguration KEEP_STRINGS = new JSONMLParserConfiguration().withKeepStrings(true);

    public JSONMLParserConfiguration() {
        this.maxNestingDepth = 512;
    }

    protected JSONMLParserConfiguration(boolean bl, int n) {
        super(bl, n);
    }

    @Override
    protected JSONMLParserConfiguration clone() {
        return new JSONMLParserConfiguration(this.keepStrings, this.maxNestingDepth);
    }

    public JSONMLParserConfiguration withKeepStrings(boolean bl) {
        return (JSONMLParserConfiguration)super.withKeepStrings(bl);
    }

    public JSONMLParserConfiguration withMaxNestingDepth(int n) {
        return (JSONMLParserConfiguration)super.withMaxNestingDepth(n);
    }
}

