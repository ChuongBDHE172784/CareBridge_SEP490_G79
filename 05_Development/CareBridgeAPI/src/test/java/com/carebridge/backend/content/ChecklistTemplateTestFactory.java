package com.carebridge.backend.content;

import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import java.util.UUID;
import java.util.function.Consumer;

// UC-243 (CB-CONTENT-IMP-011-TS §4) shared test fixtures
final class ChecklistTemplateTestFactory {

    static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID TEMPLATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private ChecklistTemplateTestFactory() {}

    static ChecklistTemplate makeTemplate() {
        return ChecklistTemplate.builder()
                .id(TEMPLATE_ID)
                .name("Checklist mẫu kiểm thử")
                .stage(ContentStage.PREGNANCY)
                .status(ContentStatus.DRAFT)
                .description("Mô tả kiểm thử")
                .build();
    }

    static ChecklistTemplate makeTemplate(Consumer<ChecklistTemplate> overrides) {
        ChecklistTemplate t = makeTemplate();
        overrides.accept(t);
        return t;
    }

    static ChecklistItem makeItem(ChecklistTemplate template, int order) {
        return ChecklistItem.builder()
                .id(UUID.randomUUID())
                .template(template)
                .itemText("Mục kiểm thử " + order)
                .order(order)
                .isRequired(true)
                .build();
    }
}
