package com.aetherianartificer.townstead.root.gene;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The allele payload codec: all four historical shapes decode, and encoding is canonical. */
class AllelePayloadTest {

    @Test
    void legacySingleFloatReadsAsAnonymousChannel() {
        AllelePayload payload = AllelePayload.parse("1.050");
        assertEquals("", payload.variant());
        assertEquals(1.05f, payload.channels().get(""), 1e-5f);
        // ...and answers for any channel name, since it predates having one.
        assertEquals(1.05f, payload.channel("length", 0f), 1e-5f);
    }

    @Test
    void legacyFloatRoundTripsByteIdentical() {
        AllelePayload payload = AllelePayload.parse("1.050");
        assertEquals("1.050", payload.encode());
    }

    @Test
    void variantIdRoundTrips() {
        AllelePayload payload = AllelePayload.parse("bushy");
        assertEquals("bushy", payload.variant());
        assertTrue(payload.channels().isEmpty());
        assertEquals("bushy", payload.encode());
    }

    @Test
    void namedChannelsParseAndSortCanonically() {
        AllelePayload payload = AllelePayload.parse("length=1.100;droop=0.250");
        assertEquals(1.1f, payload.channel("length", 0f), 1e-5f);
        assertEquals(0.25f, payload.channel("droop", 0f), 1e-5f);
        assertEquals("droop=0.250;length=1.100", payload.encode());
    }

    @Test
    void variantWithChannelsRoundTrips() {
        AllelePayload payload = AllelePayload.parse("bushy|length=1.100;droop=0.250");
        assertEquals("bushy", payload.variant());
        assertEquals(1.1f, payload.channel("length", 0f), 1e-5f);
        assertEquals("bushy|droop=0.250;length=1.100", payload.encode());
    }

    @Test
    void namedLookupDoesNotFallBackWhenOtherNamedChannelsExist() {
        AllelePayload payload = AllelePayload.parse("length=1.100;droop=0.250");
        assertEquals(9f, payload.channel("curl", 9f), 1e-5f);
    }

    @Test
    void emptyAndNullParseToEmpty() {
        assertEquals(AllelePayload.EMPTY, AllelePayload.parse(null));
        assertEquals(AllelePayload.EMPTY, AllelePayload.parse(""));
        assertEquals("", AllelePayload.EMPTY.encode());
    }

    @Test
    void encodeDropsAnonymousWhenMixedWithNamed() {
        Map<String, Float> channels = new LinkedHashMap<>();
        channels.put("", 1.05f);
        channels.put("length", 1.1f);
        assertEquals("length=1.100", AllelePayload.encode("", channels));
    }

    @Test
    void variantEncodesBareWithoutChannels() {
        assertEquals("sleek", AllelePayload.encode("sleek", Map.of()));
    }
}
