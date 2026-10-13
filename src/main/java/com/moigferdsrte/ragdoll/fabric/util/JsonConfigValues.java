package com.moigferdsrte.ragdoll.fabric.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class JsonConfigValues {
    private JsonConfigValues() {}
    public static boolean bool(JsonObject root, String key, boolean fallback) {
        if (!root.has(key)) return fallback;
        JsonElement value = root.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException(key + " must be boolean");
        return value.getAsBoolean();
    }
    public static double number(JsonObject root, String key, double fallback, double minimum, double maximum) {
        if (!root.has(key)) return fallback;
        JsonElement value = root.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException(key + " must be a number");
        double number = value.getAsDouble();
        if (!Double.isFinite(number) || number < minimum || number > maximum) throw new IllegalArgumentException(key + " is outside " + minimum + ".." + maximum);
        return number;
    }
    public static int integer(JsonObject root, String key, int fallback) {
        double number = number(root, key, fallback, 0, Integer.MAX_VALUE);
        if (number != Math.rint(number)) throw new IllegalArgumentException(key + " must be an integer");
        return (int) number;
    }
}
