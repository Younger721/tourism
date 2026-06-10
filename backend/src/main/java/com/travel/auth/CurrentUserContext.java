package com.travel.auth;

public final class CurrentUserContext {
    public static final String REQUEST_ATTRIBUTE = CurrentUserContext.class.getName() + ".USER";

    private CurrentUserContext() {
    }
}
