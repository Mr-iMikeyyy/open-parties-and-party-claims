package com.madmike.opapc.util.claim;

public class ClaimContextHolder {
    public static final ThreadLocal<Boolean> SHOULD_CLAIM = new ThreadLocal<>();
}
