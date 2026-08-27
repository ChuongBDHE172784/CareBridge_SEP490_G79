package com.carebridge.backend.vaccination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.vaccination.config.VaccinationProperties;
import com.carebridge.backend.vaccination.entity.VaccinationRecord;
import com.carebridge.backend.vaccination.entity.VaccinationRecordStatus;
import com.carebridge.backend.vaccination.entity.VaccinationReferenceSchedule;
import com.carebridge.backend.vaccination.repository.VaccinationRecordRepository;
import com.carebridge.backend.vaccination.repository.VaccinationReferenceRepository;
import com.carebridge.backend.vaccination.service.impl.VaccinationBookService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * MF-03 step 1 — registering a baby materialises the whole expected vaccination book from
 * the active catalogue, projected onto the birth date.
 */
@ExtendWith(MockitoExtension.class)
class VaccinationBookServiceTest {

    private static final UUID BABY_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final LocalDate BIRTH_DATE = LocalDate.of(2026, 1, 15);

    @Mock private VaccinationReferenceRepository referenceRepository;
    @Mock private VaccinationRecordRepository recordRepository;
    @Mock private AuditService auditService;
    @Spy private VaccinationProperties properties = new VaccinationProperties();
    @InjectMocks private VaccinationBookService bookService;

    private BabyProfile baby(LocalDate birthDate) {
        return BabyProfile.builder()
                .id(BABY_ID)
                .ownerUserId(OWNER_ID)
                .nickname("Bean")
                .birthDate(birthDate)
                .status(BabyProfileStatus.ACTIVE)
                .build();
    }

    private VaccinationReferenceSchedule dose(String name, int doseNumber, int offsetDays) {
        return VaccinationReferenceSchedule.builder()
                .id(UUID.randomUUID())
                .vaccineName(name)
                .doseNumber((short) doseNumber)
                .offsetDays(offsetDays)
                .scheduleVersion("vn-2026")
                .build();
    }

    private void arrangeCatalogue(VaccinationReferenceSchedule... doses) {
        when(referenceRepository.findByScheduleVersionOrderByOffsetDaysAscDoseNumberAscVaccineNameAsc("vn-2026"))
                .thenReturn(List.of(doses));
    }

    @SuppressWarnings("unchecked")
    private List<VaccinationRecord> capturedSave() {
        ArgumentCaptor<List<VaccinationRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(recordRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void initializeBook_newBaby_materialisesEveryCatalogueDoseFromBirthDate() {
        arrangeCatalogue(
                dose("Lao (BCG)", 1, 0),
                dose("Vắc-xin 6 trong 1", 1, 60),
                dose("Vắc-xin 6 trong 1", 2, 90));
        when(recordRepository.findAllByBabyId(BABY_ID)).thenReturn(List.of());

        int created = bookService.initializeBook(baby(BIRTH_DATE));

        assertThat(created).isEqualTo(3);
        List<VaccinationRecord> saved = capturedSave();
        assertThat(saved).extracting(VaccinationRecord::getScheduledDate)
                .containsExactly(
                        LocalDate.of(2026, 1, 15),
                        LocalDate.of(2026, 3, 16),
                        LocalDate.of(2026, 4, 15));
        assertThat(saved).allSatisfy(record -> {
            assertThat(record.getStatus()).isEqualTo(VaccinationRecordStatus.SCHEDULED);
            assertThat(record.getBabyId()).isEqualTo(BABY_ID);
            // care_subject_id is NOT NULL in the canonical schema.
            assertThat(record.getCareSubjectId()).isEqualTo(BABY_ID);
            assertThat(record.getVaccinationScheduleId()).isNotNull();
            assertThat(record.getAdministeredDate()).isNull();
        });
    }

    @Test
    void initializeBook_recordsTheMaterialisationInTheAuditTrail() {
        arrangeCatalogue(dose("Lao (BCG)", 1, 0));
        when(recordRepository.findAllByBabyId(BABY_ID)).thenReturn(List.of());

        bookService.initializeBook(baby(BIRTH_DATE));

        verify(auditService).log(eq(AuditAction.VACCINATION_BOOK_INITIALIZED), eq(OWNER_ID),
                eq("BabyProfile"), eq(BABY_ID.toString()), any());
    }

    @Test
    void initializeBook_rerun_isIdempotentAndDoesNotDuplicateDoses() {
        arrangeCatalogue(dose("Lao (BCG)", 1, 0), dose("Vắc-xin 6 trong 1", 1, 60));
        when(recordRepository.findAllByBabyId(BABY_ID)).thenReturn(List.of(
                VaccinationRecord.builder().id(UUID.randomUUID()).babyId(BABY_ID)
                        .vaccineName("Lao (BCG)").doseNumber((short) 1)
                        .scheduledDate(BIRTH_DATE).status(VaccinationRecordStatus.SCHEDULED).build(),
                VaccinationRecord.builder().id(UUID.randomUUID()).babyId(BABY_ID)
                        .vaccineName("Vắc-xin 6 trong 1").doseNumber((short) 1)
                        .scheduledDate(BIRTH_DATE.plusDays(60)).status(VaccinationRecordStatus.SCHEDULED).build()));

        int created = bookService.initializeBook(baby(BIRTH_DATE));

        assertThat(created).isZero();
        verify(recordRepository, never()).saveAll(any());
    }

    @Test
    void initializeBook_doseTheMotherDeleted_isNotResurrected() {
        arrangeCatalogue(dose("Rota (uống)", 3, 120));
        when(recordRepository.findAllByBabyId(BABY_ID)).thenReturn(List.of(
                VaccinationRecord.builder().id(UUID.randomUUID()).babyId(BABY_ID)
                        .vaccineName("Rota (uống)").doseNumber((short) 3)
                        .status(VaccinationRecordStatus.DELETED).build()));

        assertThat(bookService.initializeBook(baby(BIRTH_DATE))).isZero();
        verify(recordRepository, never()).saveAll(any());
    }

    @Test
    void initializeBook_babyWithoutBirthDate_materialisesNothing() {
        assertThat(bookService.initializeBook(baby(null))).isZero();
        verify(recordRepository, never()).saveAll(any());
        verify(referenceRepository, never())
                .findByScheduleVersionOrderByOffsetDaysAscDoseNumberAscVaccineNameAsc(any());
    }

    @Test
    void initializeBook_emptyCatalogue_materialisesNothing() {
        arrangeCatalogue();

        assertThat(bookService.initializeBook(baby(BIRTH_DATE))).isZero();
        verify(recordRepository, never()).saveAll(any());
    }

    @Test
    void realignBook_correctedBirthDate_movesScheduledDosesOnly() {
        arrangeCatalogue(
                dose("Lao (BCG)", 1, 0),
                dose("Vắc-xin 6 trong 1", 1, 60),
                dose("Phế cầu (PCV-10/PCV-13)", 1, 60));
        VaccinationRecord scheduled = VaccinationRecord.builder().id(UUID.randomUUID()).babyId(BABY_ID)
                .vaccineName("Vắc-xin 6 trong 1").doseNumber((short) 1)
                .scheduledDate(BIRTH_DATE.plusDays(60)).status(VaccinationRecordStatus.SCHEDULED).build();
        VaccinationRecord completed = VaccinationRecord.builder().id(UUID.randomUUID()).babyId(BABY_ID)
                .vaccineName("Lao (BCG)").doseNumber((short) 1)
                .scheduledDate(BIRTH_DATE).administeredDate(BIRTH_DATE)
                .status(VaccinationRecordStatus.COMPLETED).build();
        when(recordRepository.findAllByBabyId(BABY_ID)).thenReturn(List.of(scheduled, completed));

        LocalDate corrected = BIRTH_DATE.plusDays(2);
        int touched = bookService.realignBook(baby(corrected));

        // The scheduled dose moves, the completed one keeps its real date, and the dose that
        // was never materialised is created.
        assertThat(touched).isEqualTo(2);
        assertThat(scheduled.getScheduledDate()).isEqualTo(corrected.plusDays(60));
        assertThat(completed.getScheduledDate()).isEqualTo(BIRTH_DATE);
        assertThat(capturedSave())
                .anySatisfy(record -> assertThat(record.getVaccineName()).isEqualTo("Phế cầu (PCV-10/PCV-13)"));
    }

    @Test
    void realignBook_postponedDose_keepsTheDateTheMotherChose() {
        arrangeCatalogue(dose("Thủy đậu", 1, 270));
        VaccinationRecord postponed = VaccinationRecord.builder().id(UUID.randomUUID()).babyId(BABY_ID)
                .vaccineName("Thủy đậu").doseNumber((short) 1)
                .scheduledDate(BIRTH_DATE.plusDays(300))
                .status(VaccinationRecordStatus.POSTPONED).build();
        when(recordRepository.findAllByBabyId(BABY_ID)).thenReturn(List.of(postponed));

        assertThat(bookService.realignBook(baby(BIRTH_DATE.minusDays(5)))).isZero();
        assertThat(postponed.getScheduledDate()).isEqualTo(BIRTH_DATE.plusDays(300));
        verify(recordRepository, never()).saveAll(any());
    }
}
