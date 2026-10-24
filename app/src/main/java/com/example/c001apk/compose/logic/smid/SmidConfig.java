package com.example.c001apk.compose.logic.smid;

import android.content.Context;

public final class SmidConfig {
    public static final String DIGITAL_UNION_PACKAGE_NAME = "com.coolapk.market";
    public static final String VERSION_NAME = "16.1.3";
    public static final long VERSION_CODE = 2604022L;
    public static final String DIGITAL_UNION_API_KEY =
            "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBALxHQz5bfGTMX1+1SNNgar8RiOO9iJ8fDfU3gaGHeFhIAw507PXbGdswW2Vf4xzJ0eh1qm21Y6x2YtsKHv8H2sUCAwEAAQ==";
    public static final String DIGITAL_UNION_CUSTOM = "";
    public static final String DIGITAL_UNION_DISABLE = "1";
    public static final String DIGITAL_UNION_STORE = null;

    private SmidConfig() {
    }

    public static String buildStoreValue(Context context) {
        return DIGITAL_UNION_STORE;
    }
}
