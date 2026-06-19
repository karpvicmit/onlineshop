package com.karpenko.onlineshop.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SlugUtilTest {

    @Test
    void generateSlug_shouldReturnEmptyString_forNull() {
        assertThat(SlugUtil.generateSlug(null)).isEmpty();
    }

    @Test
    void generateSlug_shouldReturnEmptyString_forEmptyOrBlank() {
        assertThat(SlugUtil.generateSlug("")).isEmpty();
        assertThat(SlugUtil.generateSlug("   ")).isEmpty();
    }

    @Test
    void generateSlug_shouldConvertToLowercaseAndReplaceSpaces() {
        assertThat(SlugUtil.generateSlug("Hello World")).isEqualTo("hello-world");
        assertThat(SlugUtil.generateSlug("  Leading and trailing  ")).isEqualTo("leading-and-trailing");
    }

    @Test
    void generateSlug_shouldRemoveSpecialCharacters() {
        assertThat(SlugUtil.generateSlug("Test!@#$%^&*()")).isEqualTo("test");
        assertThat(SlugUtil.generateSlug("A + B = C")).isEqualTo("a-b-c");
    }

    @Test
    void generateSlug_shouldHandleUmlauts() {
        assertThat(SlugUtil.generateSlug("München")).isEqualTo("muenchen");
        assertThat(SlugUtil.generateSlug("Österreich")).isEqualTo("oesterreich");
        assertThat(SlugUtil.generateSlug("Straße")).isEqualTo("strasse");
    }

    @Test
    void generateSlug_shouldReplaceMultipleHyphensWithSingle() {
        assertThat(SlugUtil.generateSlug("a--b---c")).isEqualTo("a-b-c");
        assertThat(SlugUtil.generateSlug("a  b  c")).isEqualTo("a-b-c");
    }
}