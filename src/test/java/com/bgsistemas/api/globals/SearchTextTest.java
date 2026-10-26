package com.bgsistemas.api.globals;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// BGSI-302: the text of the search box, ready for LIKE ... ESCAPE '!'
class SearchTextTest {

    @Test
    void shouldReturnNullWhenThereIsNothingToSearch() {
        assertThat(SearchText.likePattern(null)).isNull();
        assertThat(SearchText.likePattern("")).isNull();
        assertThat(SearchText.likePattern("   ")).isNull();
    }

    @Test
    void shouldTrimLowercaseAndWrapTheText() {
        assertThat(SearchText.likePattern("  Bar La PLAZA ")).isEqualTo("%bar la plaza%");
    }

    @Test
    void shouldEscapeTheSpecialCharactersOfLike() {
        assertThat(SearchText.likePattern("100%")).isEqualTo("%100!%%");
        assertThat(SearchText.likePattern("a_b")).isEqualTo("%a!_b%");
        assertThat(SearchText.likePattern("hi!")).isEqualTo("%hi!!%");
    }

    @Test
    void shouldCutVeryLongTexts() {
        assertThat(SearchText.likePattern("x".repeat(500))).hasSize(102);
    }
}
