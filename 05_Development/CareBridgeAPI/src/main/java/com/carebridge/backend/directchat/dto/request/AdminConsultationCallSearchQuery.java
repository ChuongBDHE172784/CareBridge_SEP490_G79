package com.carebridge.backend.directchat.dto.request;

import com.carebridge.backend.directchat.entity.CallStatus;
import com.carebridge.backend.directchat.entity.CallType;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminConsultationCallSearchQuery {
    private String keyword;
    private CallType callType;
    private CallStatus callStatus;
    private Boolean hasRecording;
    private Instant fromDate;
    private Instant toDate;
}
