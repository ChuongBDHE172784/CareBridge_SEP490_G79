package com.carebridge.backend.baby;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.dto.*;
import com.carebridge.backend.baby.entity.*;
import com.carebridge.backend.baby.policy.*;
import com.carebridge.backend.baby.repository.*;
import com.carebridge.backend.baby.service.BabyLinkRejectionAuditService;
import com.carebridge.backend.baby.service.impl.BabyServiceImpl;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.entity.MotherJourney;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BabyJourneyLinkServiceTest {
 @Mock BabyProfileRepository babies; @Mock BabyAccessPolicy access;
 @Mock AuditService audits; @Mock BabyJourneyLinkagePolicy policy;
 @Mock BabyLinkSubmissionRepository submissions; @Mock BabyLinkRejectionAuditService rejected;
 @InjectMocks BabyServiceImpl service;
 UUID owner=UUID.randomUUID(), journey=UUID.randomUUID(), babyId=UUID.randomUUID(), submission=UUID.randomUUID();

 @Test void createWithLinkPersistsOneBabySubmissionAndAuthoritativeLink() {
  var req=createRequest(); when(policy.requireEligibleJourney(journey,owner)).thenReturn(MotherJourney.builder().id(journey).build());
  when(submissions.findForUpdate(owner,BabyLinkOperation.CREATE_WITH_LINK,submission)).thenReturn(Optional.empty());
  when(babies.save(any())).thenAnswer(i->{BabyProfile b=i.getArgument(0); b.setId(babyId); return b;});
  var result=service.createBabyProfile(req,owner);
  assertThat(result.getRelatedJourneyId()).isEqualTo(journey);
  verify(babies,times(1)).save(argThat(b->journey.equals(b.getRelatedJourneyId())));
  verify(submissions).save(any());
 }

 @Test void changedIntentReplayIsConflictAndAuditedAsRejection() {
  var req=createRequest(); var prior=BabyLinkSubmission.builder().ownerUserId(owner).operationType(BabyLinkOperation.CREATE_WITH_LINK)
    .submissionId(submission).semanticIntent("different").babyId(babyId).journeyId(journey).build();
  when(policy.requireEligibleJourney(journey,owner)).thenReturn(MotherJourney.builder().id(journey).build());
  when(submissions.findForUpdate(owner,BabyLinkOperation.CREATE_WITH_LINK,submission)).thenReturn(Optional.of(prior));
  assertThatThrownBy(()->service.createBabyProfile(req,owner)).isInstanceOf(BusinessException.class)
    .satisfies(e->assertThat(((BusinessException)e).getCode()).isEqualTo("LINK_SUBMISSION_CONFLICT"));
  verify(rejected).record(owner,journey,"LINK_SUBMISSION_CONFLICT"); verifyNoInteractions(babies);
 }

 @Test void linkExistingRejectsOtherJourneyWithoutRelink() {
  UUID other=UUID.randomUUID(); var req=new LinkBabyJourneyRequest(); req.setRelatedJourneyId(journey); req.setSubmissionId(submission);
  var baby=BabyProfile.builder().id(babyId).ownerUserId(owner).status(BabyProfileStatus.ACTIVE).relatedJourneyId(other).build();
  when(policy.requireEligibleJourney(journey,owner)).thenReturn(MotherJourney.builder().id(journey).build());
  when(babies.findOwnedByIdForUpdate(babyId,owner)).thenReturn(Optional.of(baby));
  when(submissions.findForUpdate(owner,BabyLinkOperation.LINK_EXISTING,submission)).thenReturn(Optional.empty());
  assertThatThrownBy(()->service.linkExistingBaby(babyId,req,owner)).isInstanceOf(BusinessException.class)
    .satisfies(e->assertThat(((BusinessException)e).getCode()).isEqualTo("BABY_ALREADY_LINKED"));
  assertThat(baby.getRelatedJourneyId()).isEqualTo(other); verify(rejected).record(owner,babyId,"BABY_ALREADY_LINKED");
 }

 private CreateBabyProfileRequest createRequest(){var r=new CreateBabyProfileRequest();r.setNickname(" Bean ");r.setBirthDate(LocalDate.now());r.setRelatedJourneyId(journey);r.setSubmissionId(submission);return r;}
}
