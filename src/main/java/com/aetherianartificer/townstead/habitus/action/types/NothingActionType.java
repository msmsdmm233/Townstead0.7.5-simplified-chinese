package com.aetherianartificer.townstead.habitus.action.types;

import com.aetherianartificer.townstead.habitus.action.Action;
import com.aetherianartificer.townstead.habitus.action.ActionType;
import com.google.gson.JsonObject;

/**
 * Does nothing (Apoli's meta {@code nothing}); useful as an explicit no-op branch.
 *
 * <p>JSON: {@code { "type":"townstead_origins:nothing" }}</p>
 */
public final class NothingActionType implements ActionType {

    public static final String KEY = "townstead_origins:nothing";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public Action parse(JsonObject json) {
        return ctx -> {};
    }
}
