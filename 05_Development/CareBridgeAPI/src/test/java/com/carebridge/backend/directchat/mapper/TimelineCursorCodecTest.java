package com.carebridge.backend.directchat.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TimelineCursorCodecTest {

    @Test
    void encodeDecode_roundTrips() {
        Instant ts = Instant.parse("2026-07-15T08:00:00.123456Z");
        String kind = "MESSAGE";
        UUID resourceId = UUID.randomUUID();

        String cursor = TimelineCursorCodec.encode(ts, kind, resourceId);
        TimelineCursorCodec.DecodedCursor decoded = TimelineCursorCodec.decode(cursor);

        assertThat(decoded.sortTs()).isEqualTo(ts);
        assertThat(decoded.kind()).isEqualTo(kind);
        assertThat(decoded.resourceId()).isEqualTo(resourceId);
    }

    @Test
    void encode_isOpaqueBase64_notPlainText() {
        String cursor = TimelineCursorCodec.encode(Instant.now(), "CALL_EVENT", UUID.randomUUID());
        assertThat(cursor).doesNotContain("CALL_EVENT");
    }

    @Test
    void decode_malformedCursor_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> TimelineCursorCodec.decode("not-valid-base64!!!"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decode_wrongPartCount_throwsIllegalArgumentException() {
        String badCursor = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("12345|MESSAGE".getBytes());
        assertThatThrownBy(() -> TimelineCursorCodec.decode(badCursor))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decode_unknownKind_throwsIllegalArgumentException() {
        String badCursor = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString((Instant.now() + "|UNKNOWN|" + UUID.randomUUID()).getBytes());
        assertThatThrownBy(() -> TimelineCursorCodec.decode(badCursor))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
