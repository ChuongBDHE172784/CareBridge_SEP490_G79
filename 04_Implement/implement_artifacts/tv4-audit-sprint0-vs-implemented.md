# TV4 Sprint 0 Audit — Spec 121UC vs 04_Implement/ Reality

**Ngày kiểm tra:** 2026-07-03
**Spec tham chiếu:** `function-spec-task-allocation-reprioritized-121uc.md`
**Code thực tế:** `04_Implement/` folders

---







## Tóm tắt tình hình

| Vấn đề | Mức độ | Mô tả ngắn |
|---|---|---|
| UC numbering KHÔNG KHỚP | 🔴 Nghiêm trọng | 04_Implement/ dùng UC-87..UC-99, 103, 104, 129 — khác với 121UC spec (UC-60..UC-71, 77..82) |
| V2-deferred UCs trong Sprint 0 | 🔴 Nghiêm trọng | Consultation/payment/partner UCs đang có TDS nhưng spec nói V2 mới làm |
| Domain assignment sai | 🟡 Trung bình | Một số UC gán sai owner (VD: UC99 moderation là của TV3) |
| Sprint 0 packages chưa có | 🔴 Nghiêm trọng | `expert`, `map`, `location`, `nearbycare` packages chưa tồn tại trong code backend |
| Header TDS ≠ Tên folder | 🟡 Trung bình | Một số folder có header TDS sai tên UC gốc |

---







## 1. UC Numbering Mismatch — Vấn đề lớn nhất

**121UC Spec gán TV4:**

| 121UC Spec | Tên UC | 04_Implement/ thực tế |
|---|---|---|
| **UC-60** | Submit Expert Profile | `UC87_CreateExpertProfile/` |
| **UC-61** | Update Expert Profile | `UC88_UpdateExpertProfile/` |
| **UC-62** | Submit/Replace Verification Docs | `UC89_UploadVerificationDocuments/` |
| **UC-63** | View Verification Status & Renew | **KHÔNG CÓ** |
| **UC-64** | Configure Expert Availability | `UC90_ConfigureAvailability/` |
| **UC-65** | Browse Verified Expert Directory | `UC164_SearchExpert/` ❓ |
| **UC-66** | View Verified Expert Profile | **KHÔNG CÓ** (trừ UC104?) |
| **UC-67** | View Expert Question Queue | `UC91_ViewExpertQuestionQueue/` |
| **UC-68** | Post Verified Expert Answer | `UC92_PostExpertAnswer/` |
| **UC-69** | View Contribution Points & Badges | `UC98_ViewContributionPoints/` |
| **UC-70** | Review Expert Verification (admin) | `UC103_VerifyExpertProfile/` |
| **UC-71** | Restrict/Suspend/Reinstate Expert | `UC104_RevokeExpertBadge/` |

| 121UC Spec | 04_Implement/ thực tế |
|---|---|
| **UC-77** | `UC62_OpenEmergencyFlow/` 🟡 |
| **UC-78** | `UC63_FindNearbyCareFacility/` 🟡 |
| **UC-79** | `UC64_QuickCallOrNavigate/` + `UC129_CalculateDistanceRouteAndETA/` |
| **UC-80** | `UC65_SendFamilyEmergencyAlert/` 🟡 |
| **UC-81** | **KHÔNG CÓ** |
| **UC-82** | **KHÔNG CÓ** |

**Kết luận:** Có 2 hệ thống UC numbering tồn tại song song trong project. 04_Implement/ đang dùng một bộ số cũ (hoặc từ spec 241 UC gốc) — cần xác nhận bộ nào là chính thức.

---







## 2. V2-Deferred UCs Xuất Hiện Trong Sprint 0 Scope (KHÔNG NÊN CÓ)

Theo spec 121UC, các MF sau là V2 Deferred — KHÔNG được implement trong Sprint 0:

| Folder | UC # trong 121UC Spec | Feature | Trạng thái 04_Implement/ | Vấn đề |
|---|---|---|---|---|
| `UC93_SuggestPrivateConsultation/` | MF-15 | Paid Consultation | TDS Draft, Owner: TV4-Lâm | 🔴 **V2 only — không nên có** |
| `UC94_ViewSharedHealthSummary/` | (consultation) | Expert view shared summary | TDS Draft, Owner: TV4-Lâm | 🔴 **consultation adjacent, V2** |
| `UC95_ManageConsultationSession/` | MF-15 | Manage consultation session | TDS Draft, Owner: TV4-Lâm | 🔴 **V2 only** |
| `UC96_WriteConsultationSummary/` | MF-15 | Write consultation summary | TDS Draft, Owner: TV4-Lâm | 🔴 **V2 only** |
| `UC97_ViewRevenueAndCommission/` | MF-15 | Revenue & commission | TDS Draft, Owner: TV4-Lâm | 🔴 **V2 only** |
| `UC120_SubmitServiceListing/` | MF-16 | Partner service listing | TDS Draft | 🔴 **V2 only** |
| `UC121_SubmitSponsoredContent/` | MF-16 | Sponsored content | TDS Draft | 🔴 **V2 only** |
| `UC122_ViewPartnerPerformance/` | MF-16 | Partner performance | TDS Draft | 🔴 **V2 only** |
| `UC123_ApprovePartnerProfile/` | MF-16 | Partner approval | TDS only | 🔴 **V2 only** |
| `UC124_ApproveSponsoredServiceCampaign/` | MF-16 | Campaign approval | TDS only | 🔴 **V2 only** |
| `UC125_RemovePartnerContent/` | MF-16 | Remove partner content | TDS only | 🔴 **V2 only** |
| `UC126_ProcessPaymentTransaction/` | MF-15 | Payment | TDS Draft | 🔴 **V2 only** |
| `UC127_CalculateCommission/` | MF-15 | Commission | TDS Draft | 🔴 **V2 only** |
| `UC154_EstablishRealtimeCommunicationSession/` | MF-15 | Realtime chat/voice/video | TDS Draft | 🔴 **V2 only** |

**Tổng V2 UCs xuất hiện: 14 UCs**

---







## 3. Domain Assignment Wrong — UCs thuộc member khác

| Folder | TDS Owner | 121UC Spec Owner | Vấn đề |
|---|---|---|---|
| `UC99_ViewModerationQueue/` | `HuyND` (TV3) | TV3-Huy ✅ | OK — nhưng folder này nằm trong scope TV4 trong 04_Implement/ |
| `UC66_ConnectHealthDevice/` | `TV2-Bách` | TV2-Bách ✅ | OK |
| `UC67_ImportDeviceDataManually/` | `TV2-Bách` | TV2-Bách ✅ | OK |
| `UC68_DisconnectHealthDevice/` | `TV2-Bách` | TV2-Bách ✅ | OK |
| `UC69_ViewDeviceDataTrend/` | `TV2-Bách` | TV2-Bách ✅ | OK |
| `UC70_CreateCareGroup/` | `PhuongNT` (TV1) | TV2-Bách | 🟡 **Owner khác 121UC spec** |
| `UC71_InviteFamilyMember/` | `TV2-Bách` | TV2-Bách ✅ | OK |

---







## 4. TDS Headers Không Khớp Folder Names

| Folder | Header trong TDS | Đúng? |
|---|---|---|
| `UC87_CreateExpertProfile/` | `# UC-87 Create Expert Profile` | ✅ |
| `UC88_UpdateExpertProfile/` | `# UC-88 Update Expert Profile` | ✅ |
| `UC89_UploadVerificationDocuments/` | `# UC-89 Upload Verification Documents` | ✅ |
| `UC90_ConfigureAvailability/` | `# UC-90 Configure Availability` | ✅ |
| `UC91_ViewExpertQuestionQueue/` | `# UC-91 View Expert Question Queue` | ✅ |
| `UC92_PostExpertAnswer/` | `# UC-92 Post Expert Answer` | ✅ |
| `UC93_SuggestPrivateConsultation/` | `# UC-93 Suggest Private Consultation` | ✅ |
| `UC62_OpenEmergencyFlow/` | `# UC-62 Open Emergency Flow` | ✅ (nhưng UC-62 ≠ UC-77 trong 121UC) |
| `UC63_FindNearbyCareFacility/` | `# UC-63 Find Nearby Care Facility` | ✅ (nhưng UC-63 ≠ UC-78) |
| `UC64_QuickCallOrNavigate/` | `# UC-64 Quick Call or Navigate` | ✅ (nhưng UC-64 ≠ UC-79) |
| `UC65_SendFamilyEmergencyAlert/` | `# UC-65 Send Family Emergency Alert` | ✅ (nhưng UC-65 ≠ UC-80) |

---







## 5. Missing UCs — Có trong 121UC Spec nhưng Không có trong 04_Implement/

| 121UC Spec | Tên UC | Sprint 0 cần có | Tình trạng |
|---|---|---|---|
| **UC-63** | View Verification Status & Renew | Sprint 0 (expertverification) | ❌ **THIẾU** |
| **UC-65** | Browse Verified Expert Directory | Sprint 0 | ❌ **THIẾU** (có thể là UC164_SearchExpert?) |
| **UC-66** | View Verified Expert Profile | Sprint 0 | ❌ **THIẾU** |
| **UC-81** | Create/Cancel Nearby Support Request | Sprint 2 | ❌ **THIẾU** |
| **UC-82** | Manage Expert Nearby Availability & Respond | Sprint 2 | ❌ **THIẾU** |
| **UC-77** | Open Emergency Map (with non-dispatch) | Sprint 2 | ⚠️ Có `UC62_OpenEmergencyFlow` — có thể tương ứng? |
| **UC-78** | Find Nearby Care Facilities | Sprint 2 | ⚠️ Có `UC63` — số khác |
| **UC-79** | Route/ETA + Quick Call/Navigate | Sprint 2 | ⚠️ Có `UC64` + `UC129` |
| **UC-80** | Share Location + Family Emergency Alert | Sprint 2 | ⚠️ Có `UC65` |

---







## 6. Sprint 0 Package Check Theo Spec

| Spec yêu cầu | Đã có code Java | Có TDS | Kết luận |
|---|---|---|---|
| `expert` package | Chỉ .gitkeep, rỗng | Có (UC87, UC88, UC90, UC91, UC92, UC98) | 🔴 Package skeleton chưa có, TDS chỉ là spec thôi |
| `expertverification` package | **KHÔNG TỒN TẠI** | Có (UC103, UC104) | 🔴 Package chưa tạo |
| `expertavailability` package | **KHÔNG TỒN TẠI** | Có (UC90) | 🔴 Package chưa tạo |
| `map` package | **KHÔNG TỒN TẠI** | Có (UC63, UC64, UC129) | 🔴 Package chưa tạo |
| `location` package | **KHÔNG TỒN TẠI** | **KHÔNG CÓ** | 🔴 Package chưa tạo |
| `nearbycare` package | **KHÔNG TỒN TẢI** | **KHÔNG CÓ** | 🔴 Package chưa tạo |
| `expertBadgeReadPort` contract | **KHÔNG TỒN TẠI** | **KHÔNG CÓ** | 🔴 Chưa publish |
| `RouteProvider` contract | **KHÔNG TỒN TẠI** | **KHÔNG CÓ** | 🔴 Chưa publish |
| `EmergencyMapHandoff` contract | **KHÔNG TỒN TẠI** | **KHÔNG CÓ** | 🔴 Chưa publish |
| Mock facility provider | **KHÔNG CÓ** | **KHÔNG CÓ** | 🔴 Thiếu |
| Seed verified expert | **KHÔNG RÕ** | — | 🟡 Cần kiểm tra |

---







## 7. Kết luận & Khuyến nghị

### Sprint 0 chưa đạt exit condition

Sprint 0 exit condition theo spec: **"all five modules compile together; each vertical slice can use seeded/mock data; shared contracts are merged."**

Hiện tại: ❌ **CHƯA ĐẠT** — packages chưa có, contracts chưa publish, V2-deferred UCs đang lẫn vào.

### Cần làm ngay (trước khi bắt đầu Sprint 1):

1. **Xác nhận UC numbering chính thức** — bộ nào đúng: 121UC spec (UC-60..UC-82) hay bộ số cũ (UC-87..UC-99, 103, 104)? Cần align team.
2. **XÓA hoặc chuyển V2 UCs ra** — UC93..UC97 (consultation), UC120..UC127 (partner/payment), UC154 (realtime) không thuộc Sprint 0.
3. **Tạo 6 packages** — `expert`, `expertverification`, `expertavailability`, `map`, `location`, `nearbycare` với đầy đủ controller/service/repository/entity/dto/mapper/policy.
4. **Publish 3 contracts** — `ExpertBadgeReadPort`, `RouteProvider`, `EmergencyMapHandoff`.
5. **Bổ sung thiếu** — UC-63 (Verify Status & Renew), UC-65 (Browse Directory), UC-66 (View Expert Profile), UC-81 (Nearby Support Request), UC-82 (Expert Nearby Availability).
6. **Seed mock data** — 1 verified expert, 3-5 care facilities.
7. **Flyway migration** — `expert_profiles`, `expert_credentials`, `expert_availability`, `care_facilities`, `nearby_support_requests`, `nearby_support_responses`.
