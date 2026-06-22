package com.carebridge.backend.expert.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private UUID id;

    private UUID expertId;

    private UUID motherId;

    private UUID bookingId;

    private Integer rating;

    private String comment;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;
}
