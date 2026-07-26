package com.carebridge.backend.community.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SlugGeneratorTest {

    // COM-TC-001: Vietnamese diacritics stripped to a clean ASCII slug
    @Test
    void generate_vietnameseNameWithDiacritics_stripsToAsciiSlug() {
        String slug = SlugGenerator.generate("Chăm sóc bé sơ sinh");

        assertThat(slug).isEqualTo("cham-soc-be-so-sinh");
        assertThat(slug).matches("[a-z0-9-]+");
        assertThat(slug).doesNotStartWith("-").doesNotEndWith("-");
        assertThat(slug).doesNotContain("--");
    }

    @Test
    void generate_nameWithDefaultLetterD_convertsToPlainD() {
        String slug = SlugGenerator.generate("Tâm lý & Cảm xúc");

        assertThat(slug).isEqualTo("tam-ly-cam-xuc");
    }
}
