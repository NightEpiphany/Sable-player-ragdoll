package com.moigferdsrte.ragdoll.fabric;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Json5ParserTest {
    @Test void readsJson5SyntaxWithoutDamagingStrings() {
        var root = Json5Parser.parse("""
                { // comment
                  enabled: true, /* comment */
                  label: 'https://example.test/*literal*/',
                  hex: -0x2a, decimal: +.5, trailing: 5., exponent: 2e2,
                  values: [null, false, 'text',],
                  中文: '\\u4f60\\x21',
                }
                """).getAsJsonObject();
        assertTrue(root.get("enabled").getAsBoolean());
        assertEquals("https://example.test/*literal*/", root.get("label").getAsString());
        assertEquals(-42, root.get("hex").getAsInt());
        assertEquals(0.5, root.get("decimal").getAsDouble());
        assertEquals(5, root.get("trailing").getAsInt());
        assertEquals(200, root.get("exponent").getAsInt());
        assertEquals(3, root.getAsJsonArray("values").size());
        assertEquals("你!", root.get("中文").getAsString());
    }

    @Test void supportsEscapedKeysAndLineContinuations() {
        var root = Json5Parser.parse("{\\u006bey: 'line\\\r\ncontinued'}").getAsJsonObject();
        assertEquals("linecontinued", root.get("key").getAsString());
    }

    @Test void rejectsMalformedAndExcessivelyNestedInput() {
        for (String source : new String[]{"{x:}", "{x:1,,}", "/*", "{} garbage", "[1 2]", "{x:01a}", "{'x':'\\01'}", "{x:'unterminated}"}) {
            assertThrows(IllegalArgumentException.class, () -> Json5Parser.parse(source), source);
        }
        assertThrows(IllegalArgumentException.class, () -> Json5Parser.parse("[".repeat(66) + "0" + "]".repeat(66)));
    }
}
