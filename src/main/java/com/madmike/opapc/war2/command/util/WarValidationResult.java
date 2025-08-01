package com.madmike.opapc.war2.command.util;

import net.minecraft.network.chat.Component;

public class WarValidationResult {
    private final boolean valid;
    private final String errorMessage;

    private WarValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    public static WarValidationResult success() {
        return new WarValidationResult(true, null);
    }

    public static WarValidationResult fail(String message) {
        return new WarValidationResult(false, message);
    }

    public boolean isValid() {
        return valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
