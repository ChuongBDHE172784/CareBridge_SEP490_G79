package com.carebridge.backend.integration.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.integration.gemini.builder.GeminiPromptBuilder;
import com.carebridge.backend.integration.gemini.client.GeminiClient;
import com.carebridge.backend.integration.gemini.dto.RagAnswerRequest;
import com.carebridge.backend.integration.gemini.dto.RagExecutionContext;
import com.carebridge.backend.integration.gemini.dto.UserStage;
import com.carebridge.backend.integration.gemini.retriever.RagContextRetriever;
import com.carebridge.backend.integration.gemini.service.GeminiRagServiceImpl;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

/** Executable Story 6.9 Gemini boundary evidence for RAG-002/005/006 and SEC-004. */
class GeminiRagServiceStory69Test {

    @Test
    void uc82_69_rag_002_005_motherPromptUsesCanonicalStageWithoutPrivateLifecycleMetadata() {
        RagContextRetriever retriever = mock(RagContextRetriever.class);
        GeminiClient client = mock(GeminiClient.class);
        GeminiRagServiceImpl service = new GeminiRagServiceImpl(
                retriever, client, new GeminiPromptBuilder());
        UUID privateAuthor = UUID.fromString("69000000-0000-0000-0000-000000000701");
        UUID privateTopic = UUID.fromString("69000000-0000-0000-0000-000000000702");
        String privateSource = "PRIVATE-REVIEW-SENTINEL-69";
        ContentItem approved = ContentItem.builder()
                .id(UUID.fromString("69000000-0000-0000-0000-000000000703"))
                .type(ContentType.ARTICLE)
                .title("Curated title")
                .body("Curated approved guidance")
                .stage(ContentStage.PRE_PREGNANCY)
                .topicId(privateTopic)
                .status(ContentStatus.APPROVED)
                .authorUserId(privateAuthor)
                .sourceLabel(privateSource)
                .build();
        RagAnswerRequest request = RagAnswerRequest.builder()
                .query("synthetic question")
                .userStage(UserStage.POSTPARTUM)
                .maxContextChunks(3)
                .build();
        when(retriever.retrieveContext(
                "synthetic question", null, ContentStage.PRE_PREGNANCY, 3))
                .thenReturn(List.of(approved));
        when(client.generate(anyString())).thenReturn("synthetic answer");

        service.generateAnswer(request, new RagExecutionContext(
                true, ContentStage.PRE_PREGNANCY, UserStage.PRE_PREGNANCY));

        verify(retriever).retrieveContext(
                "synthetic question", null, ContentStage.PRE_PREGNANCY, 3);
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(client).generate(prompt.capture());
        assertThat(prompt.getValue())
                .contains("synthetic question", "Curated title", "Curated approved guidance",
                        "PRE_PREGNANCY")
                .doesNotContain(UserStage.POSTPARTUM.name(), privateAuthor.toString(),
                        privateTopic.toString(), privateSource, "authorUserId", "sourceLabel",
                        "journey", "healthNotes");
    }

    @ParameterizedTest
    @MethodSource("nonPositiveChunkLimits")
    void uc82_69_rag_006_nullAndNonPositiveChunkLimitsUseServerDefaultFive(Integer requestedLimit) {
        RagContextRetriever retriever = mock(RagContextRetriever.class);
        GeminiClient client = mock(GeminiClient.class);
        GeminiRagServiceImpl service = new GeminiRagServiceImpl(
                retriever, client, new GeminiPromptBuilder());
        RagAnswerRequest request = RagAnswerRequest.builder()
                .query("synthetic question")
                .userStage(UserStage.PREGNANCY)
                .maxContextChunks(requestedLimit)
                .build();
        when(retriever.retrieveContext("synthetic question", null, 5)).thenReturn(List.of());
        when(client.generate(anyString())).thenReturn("synthetic answer");

        service.generateAnswer(request, new RagExecutionContext(false, null, UserStage.PREGNANCY));

        verify(retriever).retrieveContext(eq("synthetic question"), eq(null), eq(5));
    }

    @ParameterizedTest(name = "RAG-004 generic downstream role {0} keeps {1}")
    @MethodSource("genericAllowedRoleStages")
    void uc82_69_rag_004_eachAllowedNonMotherRoleUsesGenericRetrieverAndUnchangedPromptStage(
            String role, UserStage requestedStage) {
        RagContextRetriever retriever = mock(RagContextRetriever.class);
        GeminiClient client = mock(GeminiClient.class);
        GeminiRagServiceImpl service = new GeminiRagServiceImpl(
                retriever, client, new GeminiPromptBuilder());
        RagAnswerRequest request = RagAnswerRequest.builder()
                .query("synthetic generic question")
                .userStage(requestedStage)
                .maxContextChunks(3)
                .build();
        when(retriever.retrieveContext("synthetic generic question", null, 3))
                .thenReturn(List.of());
        when(client.generate(anyString())).thenReturn("synthetic answer");

        service.generateAnswer(request, new RagExecutionContext(false, null, requestedStage));

        verify(retriever).retrieveContext("synthetic generic question", null, 3);
        verify(retriever, never()).retrieveContext(
                eq("synthetic generic question"), isNull(), any(ContentStage.class), eq(3));
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(client).generate(prompt.capture());
        assertThat(prompt.getValue())
                .contains("synthetic generic question", requestedStage.name())
                .doesNotContain(role);
    }

    static Stream<Arguments> nonPositiveChunkLimits() {
        return Stream.of(Arguments.of((Integer) null), Arguments.of(-1), Arguments.of(0));
    }

    static Stream<Arguments> genericAllowedRoleStages() {
        return Stream.of(
                Arguments.of("FAMILY", UserStage.PRE_PREGNANCY),
                Arguments.of("EXPERT", UserStage.PREGNANCY),
                Arguments.of("MODERATOR", UserStage.POSTPARTUM),
                Arguments.of("CONTENT_ADMIN", UserStage.BABY_CARE),
                Arguments.of("SYSTEM_ADMIN", UserStage.PRE_PREGNANCY));
    }
}
