package com.carebridge.backend.content.service;

import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentType;
import java.util.UUID;

public interface ContentWorkloadDispatcherService {

    /**
     * Tự động lựa chọn Chuyên gia Hợp đồng (ExpertType.CONTRACTED) tối ưu để gán việc thẩm định nội dung
     * theo cơ chế Least-Loaded + Specialty Affinity + Round-Robin tie-breaker.
     *
     * @param type Phân loại nội dung (ARTICLE, FAQ, CHECKLIST, etc.)
     * @param stage Giai đoạn chăm sóc (PRE_PREGNANCY, PREGNANCY, POSTPARTUM, BABY_CARE)
     * @param preferredExpertId Chuyên gia ưu tiên (ví dụ người đã từng nhận xét trước đó khi sửa đổi lại)
     * @return UUID của chuyên gia được gán, hoặc null nếu chưa có chuyên gia hợp đồng nào khả dụng.
     */
    UUID dispatchToOptimalExpert(ContentType type, ContentStage stage, UUID preferredExpertId);
}
