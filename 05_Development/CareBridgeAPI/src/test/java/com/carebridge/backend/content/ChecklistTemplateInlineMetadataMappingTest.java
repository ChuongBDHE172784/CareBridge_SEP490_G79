package com.carebridge.backend.content;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.checklist.model.ChecklistRecipientScope;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class ChecklistTemplateInlineMetadataMappingTest {

    @Test
    void mapsInlineChecklistMetadataWithTypedLombokAccessors() throws Exception {
        ChecklistTemplate template = ChecklistTemplate.builder()
                .recipientScope(ChecklistRecipientScope.BOTH)
                .eligibilityAnchorType(ChecklistAnchorType.DELIVERY_DATE)
                .eligibilityRangeUnit(ChecklistRangeUnit.WEEK)
                .eligibilityStartInclusive(1)
                .eligibilityEndInclusive(6)
                .build();

        assertThat(template.getRecipientScope()).isEqualTo(ChecklistRecipientScope.BOTH);
        assertThat(template.getEligibilityAnchorType()).isEqualTo(ChecklistAnchorType.DELIVERY_DATE);
        assertThat(template.getEligibilityRangeUnit()).isEqualTo(ChecklistRangeUnit.WEEK);
        assertThat(template.getEligibilityStartInclusive()).isEqualTo(1);
        assertThat(template.getEligibilityEndInclusive()).isEqualTo(6);

        assertEnumColumn("recipientScope", "recipient_scope", 10);
        assertEnumColumn("eligibilityAnchorType", "eligibility_anchor_type", 30);
        assertEnumColumn("eligibilityRangeUnit", "eligibility_range_unit", 10);
        assertColumn("eligibilityStartInclusive", "eligibility_start_inclusive");
        assertColumn("eligibilityEndInclusive", "eligibility_end_inclusive");
    }

    private static void assertEnumColumn(String fieldName, String columnName, int length) throws Exception {
        Field field = ChecklistTemplate.class.getDeclaredField(fieldName);
        assertThat(field.getAnnotation(Enumerated.class).value()).isEqualTo(EnumType.STRING);
        Column column = field.getAnnotation(Column.class);
        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.length()).isEqualTo(length);
    }

    private static void assertColumn(String fieldName, String columnName) throws Exception {
        Field field = ChecklistTemplate.class.getDeclaredField(fieldName);
        assertThat(field.getAnnotation(Column.class).name()).isEqualTo(columnName);
    }
}
