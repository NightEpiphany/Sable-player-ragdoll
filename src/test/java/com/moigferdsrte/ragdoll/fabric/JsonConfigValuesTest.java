package com.moigferdsrte.ragdoll.fabric;

import com.moigferdsrte.ragdoll.fabric.util.JsonConfigValues;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsonConfigValuesTest {
    @Test void usesDefaultsForAbsentKeys() {
        var root = Json5Parser.parse("{}").getAsJsonObject();
        assertTrue(JsonConfigValues.bool(root, "enabled", true));
        assertEquals(60, JsonConfigValues.integer(root, "ticks", 60));
        assertEquals(4.0, JsonConfigValues.number(root, "distance", 4, .5, 16));
    }

    @Test void refusesTypeCoercionAndInvalidNumbers() {
        for (String value : new String[]{"'true'", "1", "null", "[]"}) {
            var root = Json5Parser.parse("{enabled:" + value + "}").getAsJsonObject();
            assertThrows(IllegalArgumentException.class, () -> JsonConfigValues.bool(root, "enabled", true));
        }
        for (String value : new String[]{"NaN", "Infinity", "-1", "2147483648", "1.5", "'60'"}) {
            var root = Json5Parser.parse("{ticks:" + value + "}").getAsJsonObject();
            assertThrows(IllegalArgumentException.class, () -> JsonConfigValues.integer(root, "ticks", 60));
        }
    }

    @Test void acceptsRangeBoundariesAndRejectsValuesOutside() {
        assertEquals(.5, JsonConfigValues.number(Json5Parser.parse("{x:.5}").getAsJsonObject(), "x", 4, .5, 16));
        assertEquals(16, JsonConfigValues.number(Json5Parser.parse("{x:16}").getAsJsonObject(), "x", 4, .5, 16));
        assertThrows(IllegalArgumentException.class, () -> JsonConfigValues.number(Json5Parser.parse("{x:17}").getAsJsonObject(), "x", 4, .5, 16));
    }
}
