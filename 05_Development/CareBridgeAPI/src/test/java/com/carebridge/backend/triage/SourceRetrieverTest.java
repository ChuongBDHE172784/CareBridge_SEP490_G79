package com.carebridge.backend.triage;

import com.carebridge.backend.triage.engine.MedicalSource;
import com.carebridge.backend.triage.engine.SourceRetriever;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SourceRetrieverTest {

    private final SourceRetriever retriever = new SourceRetriever();

    @ParameterizedTest
    @MethodSource("topicCases")
    void eachSupportedTopicReturnsItsSpecificContentPage(
            String symptom,
            int ageMonths,
            String expectedUrl) {
        List<MedicalSource> sources = retriever.retrieve(List.of(symptom), ageMonths);

        assertThat(sources).extracting(MedicalSource::getUrl).contains(expectedUrl);
        assertThat(sources).allSatisfy(source -> {
            URI uri = URI.create(source.getUrl());
            assertThat(uri.getScheme()).isEqualTo("https");
            assertThat(uri.getPath()).isNotBlank().isNotEqualTo("/");
        });
    }

    @Test
    void newbornPageIsNotReturnedForAnEightMonthOldInfant() {
        List<MedicalSource> sources = retriever.retrieve(List.of("fever"), 8);

        assertThat(sources).extracting(MedicalSource::getUrl).doesNotContain(
                "https://www.who.int/tools/your-life-your-health/life-phase/"
                        + "newborns-and-children-under-5-years/caring-for-newborns");
    }

    private static Stream<Arguments> topicCases() {
        return Stream.of(
                Arguments.of(
                        "fever",
                        0,
                        "https://www.who.int/tools/your-life-your-health/life-phase/"
                                + "newborns-and-children-under-5-years/caring-for-newborns"),
                Arguments.of(
                        "cough",
                        8,
                        "https://benhviennhitrunguong.gov.vn/9148.html"),
                Arguments.of(
                        "difficulty_breathing",
                        8,
                        "https://benhviennhitrunguong.gov.vn/"
                                + "mot-so-dau-hieu-cha-me-can-biet-de-dua-tre-di-kham-som.html"),
                Arguments.of(
                        "seizure",
                        8,
                        "https://benhviennhitrunguong.gov.vn/"
                                + "mot-so-dau-hieu-cha-me-can-biet-de-dua-tre-di-kham-som.html"),
                Arguments.of(
                        "diarrhea",
                        8,
                        "https://benhviennhitrunguong.gov.vn/"
                                + "bao-cao-hoi-nghi-dong-thuan-khuyen-cao-ve-chan-doan-va-dieu-tri-"
                                + "tieu-chay-cap-o-tre-em.html"),
                Arguments.of(
                        "vomiting",
                        8,
                        "https://nhidong.org.vn/cac-benh-thuong-gap/"
                                + "tre-non-oi-ba-me-lam-gi-c57-2494.aspx"));
    }
}
