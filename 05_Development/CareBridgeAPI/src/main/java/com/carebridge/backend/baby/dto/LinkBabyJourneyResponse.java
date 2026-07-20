package com.carebridge.backend.baby.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data @Builder
public class LinkBabyJourneyResponse { private UUID babyId; private UUID relatedJourneyId; }
