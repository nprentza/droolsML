package org.nprentza;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IntGapTest {

    @Test
    void greaterThan() {
        IntGap gap = new IntGap();
        gap.addBound(">", 10);
        assertThat(gap.contains(10)).isFalse();
        assertThat(gap.contains(11)).isTrue();
        assertThat(gap).hasToString("(10, ..)");
    }

    @Test
    void lessThan() {
        IntGap gap = new IntGap();
        gap.addBound("<", 100);
        assertThat(gap.contains(100)).isFalse();
        assertThat(gap.contains(99)).isTrue();
        assertThat(gap).hasToString("(.., 100)");
    }

    @Test
    void greaterThanOrEqualTo() {
        IntGap gap = new IntGap();
        gap.addBound(">=", 10);
        assertThat(gap.contains(9)).isFalse();
        assertThat(gap.contains(10)).isTrue();
        assertThat(gap).hasToString("(9, ..)");
    }

    @Test
    void lessThanOrEqualTo() {
        IntGap gap = new IntGap();
        gap.addBound("<=", 100);
        assertThat(gap.contains(101)).isFalse();
        assertThat(gap.contains(100)).isTrue();
        assertThat(gap).hasToString("(.., 101)");
    }

    @Test
    void empty() {
        IntGap gap = new IntGap();
        gap.addBound("<", 100);
        gap.addBound(">=", 100);
        assertThat(gap.contains(98)).isFalse();
        assertThat(gap.contains(99)).isFalse();
        assertThat(gap.contains(100)).isFalse();
        assertThat(gap.contains(101)).isFalse();
        assertThat(gap).hasToString("no-gap");
    }
}
