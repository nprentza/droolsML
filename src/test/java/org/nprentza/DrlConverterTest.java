package org.nprentza;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

import org.junit.jupiter.api.Test;

class DrlConverterTest {

    @Test
    void outputMatchesSampleDrlFile() {
        String drl = DrlConverter.preamble()
                + DrlConverter.rule("AllowAdmin", "role", "==", "admin", "allow")
                + DrlConverter.rule("DenyGuest", "role", "==", "guest", "deny");
        assertThat(drl).isEqualToIgnoringWhitespace(contentOf(DrlConverterTest.class.getResource("/dataAccess.drl")));
    }
}
