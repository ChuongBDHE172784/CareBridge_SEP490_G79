package com.carebridge.backend.checklist.distribution;

import java.util.UUID;

/** Stable identifiers shared by the Phase 1 RED/Green contract tests. */
final class ChecklistDistributionTestFactory {

    static final UUID TEMPLATE_VERSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID RECIPIENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final UUID CARE_GROUP_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    static final UUID CONTEXT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    static final UUID INSTANCE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    static final UUID ITEM_VERSION_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private ChecklistDistributionTestFactory() {
    }
}
