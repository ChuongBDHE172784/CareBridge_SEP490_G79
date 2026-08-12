package com.carebridge.backend.content.exception;

import org.springframework.http.HttpStatus;
import java.util.Map;

public class ContentException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;
    private final Map<String, Object> metadata;

    public ContentException(String code, String message, HttpStatus httpStatus) {
        this(code, message, httpStatus, Map.of());
    }

    public ContentException(
            String code, String message, HttpStatus httpStatus, Map<String, Object> metadata) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /**
     * Machine-readable context for clients.  The map is intentionally immutable so
     * exception handling cannot accidentally mutate the response contract.
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public static ContentException duplicateContent() {
        return new ContentException(
                "CNT-002",
                "Content with same title, stage and type already exists",
                HttpStatus.CONFLICT);
    }

    public static ContentException topicNotFound(String topicId) {
        return new ContentException(
                "CNT-003",
                "Topic not found: " + topicId,
                HttpStatus.BAD_REQUEST);
    }

    public static ContentException contentNotFound() {
        return new ContentException(
                "CNT-003",
                "Content not found or not available",
                HttpStatus.NOT_FOUND);
    }

    public static ContentException validationFailed(String field, String message) {
        return new ContentException(
                "CNT-001",
                "Validation failed: " + field + " - " + message,
                HttpStatus.BAD_REQUEST);
    }

    /** Checklist configuration validation retaining CNT-001 compatibility. */
    public static ContentException checklistValidationFailed(
            String field, String message, String reasonCode) {
        return new ContentException(
                "CNT-001",
                "Validation failed: " + field + " - " + message,
                HttpStatus.BAD_REQUEST,
                Map.of("reasonCode", reasonCode));
    }

    // UC-107 (CB-CONTENT-IMP-006 §11.3)
    public static ContentException alreadyArchived() {
        return new ContentException(
                "CNT-006",
                "Content item is already archived",
                HttpStatus.CONFLICT);
    }

    public static ContentException hideReasonRequired() {
        return new ContentException(
                "CNT-007",
                "Reason is required to hide content",
                HttpStatus.BAD_REQUEST);
    }

    // UC-108 (CB-CONTENT-IMP-005 §10)
    public static ContentException notPendingReview() {
        return new ContentException(
                "CNT-008",
                "Content item is not pending review",
                HttpStatus.CONFLICT);
    }

    public static ContentException decisionReasonRequired() {
        return new ContentException(
                "CNT-009",
                "Reason required for REJECT",
                HttpStatus.BAD_REQUEST);
    }

    public static ContentException notCurrentlyPublished() {
        return new ContentException("CNT-010", "Content item is not currently published", HttpStatus.CONFLICT);
    }

    public static ContentException unpublishReasonRequired() {
        return new ContentException("CNT-011", "Reason required for unpublish", HttpStatus.BAD_REQUEST);
    }

    public static ContentException invalidContentStatusTransition() {
        return new ContentException(
                "CNT-012",
                "Content Admin may only save a draft or submit it for review",
                HttpStatus.CONFLICT);
    }

    // UC-243 (CB-CONTENT-IMP-011)
    public static ContentException checklistTemplateNotFound() {
        return new ContentException(
                "CHKTPL-003",
                "Không tìm thấy checklist template",
                HttpStatus.NOT_FOUND);
    }

    public static ContentException checklistTemplateInvalidStatusTransition() {
        return new ContentException(
                "CHKTPL-004",
                "Content Admin chỉ có thể lưu bản nháp hoặc gửi để chờ duyệt",
                HttpStatus.CONFLICT);
    }

    public static ContentException checklistTemplateArchiveReasonRequired() {
        return new ContentException(
                "CHKTPL-005",
                "Lý do bắt buộc khi lưu trữ (xóa) checklist template",
                HttpStatus.BAD_REQUEST);
    }

    public static ContentException checklistTemplateAlreadyArchived() {
        return new ContentException(
                "CHKTPL-006",
                "Checklist template đã được lưu trữ trước đó",
                HttpStatus.CONFLICT);
    }

    // §14 addendum — approval flow
    public static ContentException checklistTemplateNotPendingReview() {
        return new ContentException(
                "CHKTPL-007",
                "Checklist template không ở trạng thái chờ duyệt",
                HttpStatus.CONFLICT);
    }

    public static ContentException checklistTemplateDecisionReasonRequired() {
        return new ContentException(
                "CHKTPL-008",
                "Lý do bắt buộc khi từ chối checklist template",
                HttpStatus.BAD_REQUEST);
    }

    public static ContentException checklistTemplateItemReferenceInvalid() {
        return new ContentException(
                "CHKTPL-009",
                "Checklist item id is duplicated or does not belong to this template",
                HttpStatus.BAD_REQUEST);
    }

    public static ContentException templateRoleRequired() {
        return new ContentException(
                "TEMPLATE_ROLE_REQUIRED",
                "At least one checklist template recipient role is required",
                HttpStatus.BAD_REQUEST);
    }

    public static ContentException familyStageNotAllowed() {
        return new ContentException(
                "FAMILY_STAGE_NOT_ALLOWED",
                "Family-only checklist templates cannot define a lifecycle stage or substage",
                HttpStatus.BAD_REQUEST);
    }

    public static ContentException substageStageMismatch() {
        return new ContentException(
                "SUBSTAGE_STAGE_MISMATCH",
                "Checklist substage does not belong to the selected lifecycle stage",
                HttpStatus.BAD_REQUEST);
    }

    public static ContentException itemTargetRequired() {
        return new ContentException(
                "ITEM_TARGET_REQUIRED",
                "Every checklist item must define a valid target subject",
                HttpStatus.BAD_REQUEST);
    }

    /** V2 recommendation leaves must not accept a legacy MOTHER/BABY target. */
    public static ContentException itemTargetUnsupported() {
        return new ContentException(
                "ITEM_TARGET_UNSUPPORTED",
                "V2 recommendation checklist items cannot define a target subject",
                HttpStatus.BAD_REQUEST);
    }

    /** Requiredness is a V1 authoring concern; V2 leaves are advisory copy only. */
    public static ContentException itemRequirednessUnsupported() {
        return new ContentException(
                "ITEM_REQUIREDNESS_UNSUPPORTED",
                "V2 recommendation checklist items cannot define requiredness",
                HttpStatus.BAD_REQUEST);
    }

    public static ContentException versionImmutable() {
        return new ContentException(
                "VERSION_IMMUTABLE",
                "Approved checklist template versions are immutable",
                HttpStatus.CONFLICT);
    }

    public static ContentException migrationReviewRequired() {
        return new ContentException(
                "MIGRATION_REVIEW_REQUIRED",
                "Migrated checklist template must be explicitly reviewed before activation",
                HttpStatus.CONFLICT);
    }

    /**
     * Imported pregnancy recommendation copy is not distributable until its
     * clinical/copy provenance has an explicit sign-off.  This is deliberately
     * separate from the technical migration-review state above.
     */
    public static ContentException checklistProvenanceSignOffRequired() {
        return new ContentException(
                "CHECKLIST_PROVENANCE_SIGN_OFF_REQUIRED",
                "Pregnancy checklist provenance must be explicitly signed off before activation",
                HttpStatus.CONFLICT);
    }

    public static ContentException lifecycleContextUnavailable() {
        return new ContentException(
                "CNT-013",
                "Lifecycle content context unavailable",
                HttpStatus.CONFLICT);
    }
}
