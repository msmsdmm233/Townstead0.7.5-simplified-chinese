package com.aetherianartificer.townstead.habitus.action.types;

import com.aetherianartificer.townstead.habitus.action.Action;
import com.aetherianartificer.townstead.habitus.action.ActionType;
import com.aetherianartificer.townstead.habitus.action.Actions;
import com.google.gson.JsonObject;

/**
 * Runs every action in {@code actions} in order (Apoli's meta {@code and}). A bare JSON
 * array does the same; this is the named form.
 *
 * <p>JSON: {@code { "type":"townstead_origins:and", "actions":[ {...}, {...} ] }}</p>
 */
public final class AndActionType implements ActionType {

    public static final String KEY = "townstead_origins:and";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public Action parse(JsonObject json) {
        Action inner = Actions.parse(json.get("actions"));
        if (inner == null) return null;
        return inner::run;
    }
}
