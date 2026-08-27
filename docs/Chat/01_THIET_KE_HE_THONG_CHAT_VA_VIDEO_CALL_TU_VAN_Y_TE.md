# 01. THIẾT KẾ HỆ THỐNG CHAT & VIDEO CALL TƯ VẤN Y TẾ
## Nền tảng CareBridge — Module Direct Consultation (Mother ↔ Expert)

> **Phiên bản:** 1.0.0 — Ngày: 2026-08-19  
> **Tác giả:** HuyND — CareBridge SEP490-G79  
> **Phạm vi:** Toàn bộ tính năng nhắn tin trực tiếp, cuộc gọi Video/Voice và ghi âm/ghi hình tư vấn y tế giữa Người Mẹ (Mother) và Chuyên gia Y tế (Expert)

---

## Mục lục

1. [Bối cảnh & Lý do xây dựng](#1-bối-cảnh--lý-do-xây-dựng)
2. [Quy định Pháp lý & Nguyên tắc Y tế](#2-quy-định-pháp-lý--nguyên-tắc-y-tế)
3. [Quyết định Kiến trúc — Tại sao chọn ZegoCloud?](#3-quyết-định-kiến-trúc--tại-sao-chọn-zegocloud)
4. [Sơ đồ Kiến trúc Tổng thể](#4-sơ-đồ-kiến-trúc-tổng-thể)
5. [Cấu trúc CSDL (Database Schema)](#5-cấu-trúc-csdl-database-schema)
6. [Luồng Logic Code — Gửi Tin nhắn (Chat)](#6-luồng-logic-code--gửi-tin-nhắn-chat)
7. [Luồng Logic Code — Cuộc gọi Video/Voice](#7-luồng-logic-code--cuộc-gọi-videooice)
8. [Hệ thống Ghi âm/Ghi hình Tư vấn (Recording)](#8-hệ-thống-ghi-âmghi-hình-tư-vấn-recording)
9. [Cấu trúc Mã nguồn — Backend (Spring Boot)](#9-cấu-trúc-mã-nguồn--backend-spring-boot)
10. [Cấu trúc Mã nguồn — Mobile App (Flutter)](#10-cấu-trúc-mã-nguồn--mobile-app-flutter)
11. [Cấu trúc Mã nguồn — Web Admin Portal (React/TypeScript)](#11-cấu-trúc-mã-nguồn--web-admin-portal-reacttypescript)
12. [Bảo mật & Phân quyền](#12-bảo-mật--phân-quyền)
13. [Lưu trữ Bảo mật — Cloudflare R2 Private Bucket](#13-lưu-trữ-bảo-mật--cloudflare-r2-private-bucket)
14. [Admin Portal — Giám sát & Phát lại Bản ghi](#14-admin-portal--giám-sát--phát-lại-bản-ghi)
15. [Vận hành & Cấu hình Môi trường](#15-vận-hành--cấu-hình-môi-trường)

---

## 1. Bối cảnh & Lý do xây dựng

### 1.1. Vấn đề cần giải quyết

Phụ nữ mang thai tại Việt Nam — đặc biệt ở vùng xa, ngoài giờ hành chính — thường không có đủ kênh liên lạc kịp thời với bác sĩ chuyên khoa khi có lo ngại về sức khỏe thai kỳ. Việc đến phòng khám mất nhiều thời gian, chi phí cao, và trong nhiều tình huống (đau bụng nhẹ lúc đêm, lo về chế độ dinh dưỡng, thắc mắc về kết quả siêu âm) hoàn toàn có thể được giải quyết qua tư vấn từ xa.

CareBridge giải quyết khoảng cách này bằng tính năng **Tư vấn Trực tiếp (Direct Consultation)**: Người Mẹ có thể nhắn tin và gọi video trực tiếp với Chuyên gia Y tế đã được xác minh, ngay trong ứng dụng, an toàn và được ghi lại để đảm bảo chất lượng.

### 1.2. Mục tiêu thiết kế

| Mục tiêu | Yêu cầu cụ thể |
|-----------|----------------|
| **Kết nối tức thì** | Độ trễ cuộc gọi < 200ms (WebRTC P2P qua ZegoCloud) |
| **Tín hiệu hóa thời gian thực** | Firebase Realtime Database (< 50ms fanout) không polling |
| **Tuân thủ pháp luật** | Ghi âm/ghi hình phải có consent 2 chiều, mã hóa, lưu private |
| **Miễn phí hoàn toàn** | Không phụ thuộc trial/quota trả tiền cho core recording |
| **Kiểm soát Admin** | Quản trị viên giám sát, phát lại mọi bản ghi tư vấn |
| **Toàn vẹn dữ liệu** | Mọi cuộc gọi được audit log đầy đủ, không xóa mất |

---

## 2. Quy định Pháp lý & Nguyên tắc Y tế

### 2.1. Căn cứ pháp lý áp dụng

> **QUAN TRỌNG:** Đây là ứng dụng y tế xử lý thông tin sức khỏe nhạy cảm. Mọi tính năng ghi âm/ghi hình đều phải tuân thủ nghiêm ngặt các quy định sau:

| Quy định | Nội dung áp dụng |
|----------|-----------------|
| **Luật Bảo vệ Người tiêu dùng 2023 (Điều 14–17)** | Nghiêm cấm thu thập dữ liệu cá nhân mà không có sự đồng ý rõ ràng |
| **Nghị định 13/2023/NĐ-CP về Bảo vệ dữ liệu cá nhân** | Dữ liệu y tế là "dữ liệu cá nhân nhạy cảm", phải mã hóa, lưu ở Việt Nam hoặc có thỏa thuận quốc tế |
| **Luật Khám bệnh, Chữa bệnh 2023 (Điều 79)** | Hồ sơ bệnh án (bao gồm ghi âm tư vấn) phải lưu ít nhất 10 năm |
| **Thông tư 46/2018/TT-BYT** | Quy định tiêu chuẩn kỹ thuật trong khám chữa bệnh từ xa (Telemedicine) |
| **PDPA Thailand (tham khảo so sánh)** | Personal Data Protection Act — yêu cầu consent rõ ràng và quyền xóa dữ liệu |

### 2.2. Nguyên tắc ghi âm/ghi hình Y tế trong CareBridge

**Nguyên tắc 1 — Consent 2 chiều (Dual Consent):**

Trước khi bắt đầu mỗi cuộc gọi tư vấn, ứng dụng PHẢI hiển thị banner thông báo rõ ràng và lưu `consent_attested = true` vào cột `conversation_calls.consent_attested`.

**Nguyên tắc 2 — Tối thiểu hóa Dữ liệu (Data Minimization):**
- Chỉ ghi âm/ghi hình trong khoảng thời gian cuộc gọi diễn ra
- Không thu thập metadata vị trí, không ghi âm nền ngoài cuộc gọi
- TTL (presigned URL) chỉ 15 phút — không lưu link lâu dài

**Nguyên tắc 3 — Mã hóa tại Rest và Transit:**
- Tại Rest: AES-256 Server-Side Encryption trên Cloudflare R2
- Trong Transit: HTTPS/TLS 1.3 cho tất cả API call

**Nguyên tắc 4 — Access Control:**
- Chỉ `ROLE_ADMIN` mới được xem danh sách và phát lại bản ghi
- Người Mẹ và Chuyên gia không thể truy cập file ghi âm của chính họ
- Mọi truy cập bản ghi đều được ghi vào `audit_logs`

**Nguyên tắc 5 — Vì sao không lưu ở Cloudinary:**

Cloudinary không phải là kho lưu trữ tài liệu y tế:
- Không đảm bảo encryption at-rest theo chuẩn y tế
- CDN công khai — không phù hợp với private medical records
- Video trên Cloudinary accessible qua URL dự đoán được

**Giải pháp được chọn:** Cloudflare R2 Private Bucket với AES-256 + Presigned URL 15 phút.

---

## 3. Quyết định Kiến trúc — Tại sao chọn ZegoCloud?

### 3.1. So sánh các nền tảng WebRTC

| Tiêu chí | ZegoCloud | Twilio Video | Daily.co | Agora | Tự xây TURN/STUN |
|----------|-----------|-------------|----------|-------|-----------------|
| **Token-based Auth** | ✅ Server-side HMAC | ✅ | ✅ | ✅ | ❌ N/A |
| **SDK Flutter** | ✅ Chính thức | ⚠️ Community | ✅ | ✅ | ❌ |
| **SDK Web (React)** | ✅ `zego-uikit-prebuilt` | ✅ | ✅ | ✅ | ❌ |
| **Free tier đủ dùng** | ✅ 10,000 phút/tháng | ❌ Có phí | ⚠️ Giới hạn | ✅ | ❌ |
| **Server-side Recording** | ✅ (Trial: 1 tháng) | ✅ (có phí) | ✅ (có phí) | ✅ (có phí) | ❌ |
| **WebRTC P2P** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Độ trễ** | < 200ms | < 150ms | < 200ms | < 100ms | Tùy hạ tầng |

### 3.2. Lý do chọn ZegoCloud

1. **SDK Flutter + Web React đồng thời** — CareBridge có cả Mobile (Flutter) và Web Expert. ZegoCloud cung cấp cả 2 SDK chính thức.
2. **Token authentication** — Server ký token HMAC, client không bao giờ biết App Secret.
3. **Trial 1 tháng** — Đủ để demo và bảo vệ đồ án.
4. **Không phụ thuộc cho recording** — Recording được thực hiện **phía client** (Mobile/Web), upload về server tự mình lưu trữ → không phụ thuộc ZegoCloud recording sau trial.

### 3.3. Phương án Ghi âm — Tại sao chọn Client-side Recording?

```
Phân tích 3 phương án:

Phương án 1: ZegoCloud Server-side Recording
  Ưu điểm: Zero code, tự động
  Nhược điểm: CHỈ có trong trial. Hết trial => mất hoàn toàn. CHI PHÍ CAO sau trial.
  KHÔNG CHỌN: Không ổn định lâu dài

Phương án 2: Client-side Recording (MediaRecorder API / Flutter) => Upload lên CareBridge API
  Ưu điểm: 100% free, tự kiểm soát, không phụ thuộc ZegoCloud sau trial
  Ưu điểm: Bản ghi lưu trên Cloudflare R2 riêng tư, mã hóa AES-256
  Nhược điểm: Cần xử lý MediaRecorder API trên mobile, file WebM format
  CHỌN: Đây là cách tiêu chuẩn mọi hệ thống y tế thực tế làm

Phương án 3: RTMP Stream về media server tự triển khai
  Nhược điểm: Chi phí hạ tầng lớn, phức tạp, ngoài phạm vi đồ án
  KHÔNG CHỌN
```

---

## 4. Sơ đồ Kiến trúc Tổng thể

```
CAREBRIDGE CONSULTATION ARCHITECTURE

  [MOTHER - Flutter App]            [EXPERT - React Web]
           |                                |
           |  1. Gửi tin / Bắt đầu gọi     |
           v                                v
  +-----------------------------------------------------------+
  |          CareBridge REST API (Spring Boot :8080)          |
  |  POST /api/v1/direct-conversations                        |
  |  POST .../calls/{callId}/join-credentials                 |
  |  POST .../calls/{callId}/recording                        |
  +----------------------------+------------------------------+
                               |
                               | 2. Publish sự kiện lên Firebase
                               v
  +-----------------------------------------------------------+
  |         Firebase Realtime Database                        |
  |  /conversations/{convId}/events                           |
  |  (Fanout < 50ms đến tất cả subscribers)                   |
  +------------------+----------------------------------------+
                     |
           +---------+---------+
           |                   |
  3. Nhận tín hiệu CALL_INITIATED
           |                   |
  [MOTHER App]          [EXPERT Web]
  Hiển thị "Chuông"    Hiển thị cuộc gọi đến
           |                   |
           | 4. issueJoinCredentials (Server ký HMAC token)
           v                   v
  +-----------------------------------------------------------+
  |         ZegoCloud Express SDK                             |
  |  WebRTC P2P Audio/Video (<200ms latency)                  |
  |  Room ID: cb_{callId_no_dash}                             |
  +-----------------------------------------------------------+
           |
           | 5. Kết thúc cuộc gọi => upload ghi âm
           v
  +-----------------------------------------------------------+
  |   CareBridge API: uploadCallRecording (multipart/form)    |
  |   => FileServiceImpl => R2StorageService                  |
  +----------------------------+------------------------------+
                               |
                               v
  +-----------------------------------------------------------+
  |     Cloudflare R2 Private Bucket: carebridge-documents    |
  |     AES-256 Server-Side Encryption                        |
  |     Key: files/{UUID}.webm                                |
  +-----------------------------------------------------------+
                               |
                               | 6. Admin xem bản ghi
                               v
  +-----------------------------------------------------------+
  |   Admin Portal (React Web) /consultation-management       |
  |   => Presigned URL (15 phút TTL) => <video> HTML player   |
  +-----------------------------------------------------------+
```

---

## 5. Cấu trúc CSDL (Database Schema)

### 5.1. Bảng `direct_conversations`

```sql
CREATE TABLE direct_conversations (
    conversation_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mother_user_id    UUID NOT NULL REFERENCES users(user_id),
    expert_user_id    UUID NOT NULL REFERENCES users(user_id),
    status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE', 'CLOSED', 'LOCKED')),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_activity_at  TIMESTAMPTZ,
    UNIQUE (mother_user_id, expert_user_id)
);
```

**Ý nghĩa thiết kế:**
- `UNIQUE (mother_user_id, expert_user_id)`: Mỗi cặp Mother-Expert chỉ có 1 cuộc hội thoại duy nhất, không tạo mới mỗi lần gọi.
- `status = LOCKED`: Khi expert bị tạm khóa hoặc không còn active.

### 5.2. Bảng `conversation_calls`

```sql
CREATE TABLE conversation_calls (
    call_id                  UUID PRIMARY KEY,
    conversation_id          UUID NOT NULL REFERENCES direct_conversations,
    initiated_by_user_id     UUID NOT NULL REFERENCES users(user_id),
    call_type                VARCHAR(10) NOT NULL CHECK (call_type IN ('VOICE', 'VIDEO')),
    call_status              VARCHAR(20) NOT NULL DEFAULT 'INITIATED'
                               CHECK (call_status IN ('INITIATED','RINGING','ANSWERED',
                                                       'ENDED','DECLINED','MISSED','FAILED')),
    zego_room_id             VARCHAR(255) NOT NULL,   -- "cb_{callId_no_dash}"
    initiated_at             TIMESTAMPTZ NOT NULL,
    answered_at              TIMESTAMPTZ,
    ended_at                 TIMESTAMPTZ,
    duration_seconds         INTEGER,
    recording_file_id        UUID REFERENCES uploaded_files(file_id),
    consent_attested         BOOLEAN NOT NULL DEFAULT true,
    recording_status         VARCHAR(30) NOT NULL DEFAULT 'NONE'
                               CHECK (recording_status IN ('NONE','PENDING','UPLOADED','FAILED')),
    recorded_duration_seconds INTEGER,
    created_at               TIMESTAMPTZ NOT NULL
);
```

### 5.3. Bảng `uploaded_files` (đích đến của bản ghi)

```sql
CREATE TABLE uploaded_files (
    file_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id    UUID REFERENCES users(user_id),
    storage_key      TEXT NOT NULL,              -- "files/{UUID}.webm"
    storage_provider VARCHAR(20) NOT NULL,       -- 'r2' | 'cloudinary'
    kind             VARCHAR(20),               -- 'DOCUMENT'
    purpose          VARCHAR(50),               -- 'CONSULTATION_CALL_RECORDING'
    access_mode      VARCHAR(20),               -- 'PRIVATE'
    mime_type        VARCHAR(255),
    file_size_bytes  BIGINT,
    status           VARCHAR(20) DEFAULT 'ACTIVE',
    created_at       TIMESTAMPTZ DEFAULT now()
);
```

---

## 6. Luồng Logic Code — Gửi Tin nhắn (Chat)

### 6.1. Sequence — Mother gửi tin nhắn cho Expert

```
Mother App         CareBridge API              Firebase RTDB
──────────         ─────────────               ─────────────
    │                    │                           │
    │─ POST /messages ──►│                           │
    │  {body, type:TEXT} │                           │
    │                    │ DirectMessageController   │
    │                    │   => DirectMessageServiceImpl
    │                    │   ├─ Validate participant  │
    │                    │   ├─ Save DirectMessage    │
    │                    │   ├─ touchActivity()       │
    │                    │   └─ publishEvent()────────►│
    │                    │                           │
    ◄── onValue() ────────────────────────────────────│
    │  Expert App nhận tín hiệu, fetch timeline       │
```

### 6.2. Timeline Cursor — Phân trang Tin nhắn

Hệ thống dùng **cursor-based pagination** thay vì offset, ngăn bỏ sót tin nhắn khi có tin mới real-time:

```java
// ConversationTimelineRepository
Page<TimelineItemResponse> findTimeline(
    UUID conversationId,
    UUID readerId,
    Instant before,    // cursor: chỉ lấy items có sent_at < before
    int limit
);
```

---

## 7. Luồng Logic Code — Cuộc gọi Video/Voice

### 7.1. State Machine — Vòng đời cuộc gọi

```
     initiateCall()          markRinging()         answer()
  INITIATED ─────────► RINGING ─────────► ANSWERED ──────► ENDED
      │                   │                   │
      │ timeout(45s)       │ decline()          │ hangup()
      ▼                   ▼                   ▼
   MISSED              DECLINED             ENDED
```

`CallTimeoutReconciliationJob` chạy mỗi 30 giây, tự động chuyển `INITIATED/RINGING` quá 45 giây sang `MISSED`.

### 7.2. Bước 1 — Expert/Mother bắt đầu gọi

```
POST /api/v1/direct-conversations/{convId}/calls
{callType: "VIDEO"}

=> ConversationCallServiceImpl.initiateCall()
   ├─ Tạo UUID callId
   ├─ zegoRoomId = "cb_" + callId.replace("-","")
   ├─ Lưu ConversationCall (status=INITIATED)
   ├─ auditService.log(DIRECT_CALL_INITIATED)
   └─ publishEvent("CALL_INITIATED") => Firebase RTDB
```

### 7.3. Bước 2 — Firebase fanout tín hiệu đến đầu kia

```
Firebase RTDB: /conversations/{convId}/events/{eventId}
{
  "eventType": "CALL_INITIATED",
  "conversationId": "...",
  "resourceId": "callId",
  "timestamp": "..."
}

Đầu kia (DirectCallCoordinator.onSignal()):
  └─ Nhận CALL_INITIATED
     ├─ PATCH .../calls/{callId}/ringing
     └─ Hiển thị màn hình chuông + ringtone
```

### 7.4. Bước 3 — Chấp nhận / Từ chối

```
Chấp nhận:  PATCH .../calls/{callId}/answer
  => conditionallyAnswer(callId, now)  -- DB-level CAS để ngăn race condition
  => callStatus = ANSWERED, answeredAt = now()

Từ chối: PATCH .../calls/{callId}/decline
  => callStatus = DECLINED
```

### 7.5. Bước 4 — Lấy Zego Token để vào phòng

```
POST .../calls/{callId}/join-credentials

=> ConversationCallServiceImpl.issueJoinCredentials()
   ├─ Verify participant
   ├─ zegoUserId = "u_" + userId.replace("-","")
   ├─ IZegoCloudService.generateToken(roomId, zegoUserId, displayName)
   │   => HMAC-SHA256 với App Secret (server-side ONLY, không bao giờ expose)
   └─ Return: {appId, roomId, userId, displayName, token, expiresAt}
```

### 7.6. Bước 5 — Vào phòng ZegoCloud + ghi âm

```
Mobile (Flutter): ZegoExpressCallRoom
  ├─ ZegoExpressEngine.createEngineWithProfile(appId, appSign)
  ├─ loginRoom(roomId, ZegoUser(userId, displayName), token)
  ├─ startPublishingStream(streamId)
  └─ Bắt đầu ghi âm: MediaRecorder => .webm file

Web (React Expert): DirectCallProvider + zegoRoomSession
  ├─ ZegoUIKitPrebuilt.create(appId, appSign)
  ├─ init(callId, {roomID, userID, userName, token})
  └─ Bắt đầu ghi âm: MediaRecorder(stream, {mimeType: 'video/webm'})
```

### 7.7. Bước 6 — Kết thúc gọi + Upload bản ghi

```
PATCH .../calls/{callId}/end
  => callStatus = ENDED
  => durationSeconds = endedAt - answeredAt (seconds)

Sau đó upload:
POST .../calls/{callId}/recording (multipart/form-data; field: file=<blob>)

=> ConversationCallServiceImpl.uploadCallRecording()
   ├─ fileService.uploadWithPurpose(file, userId,
   │      FileKind.DOCUMENT,
   │      FilePurpose.CONSULTATION_CALL_RECORDING,
   │      FileAccessMode.PRIVATE)
   │   => R2StorageService.store(key, bytes, mimeType)
   │   => S3Client.putObject(bucket, key: "files/{UUID}.webm",
   │                         ServerSideEncryption: AES256)
   ├─ call.recordingFileId = uploadedFile.getId()
   ├─ call.recordingStatus = "UPLOADED"
   └─ auditLog(RECORDING_UPLOADED)
```

---

## 8. Hệ thống Ghi âm/Ghi hình Tư vấn (Recording)

### 8.1. Kiến trúc Client-side Recording Pipeline

```
  MOBILE (Flutter)                         WEB (Expert - React)
  ─────────────────                        ──────────────────────
  ZegoExpressEngine                        ZegoCloud UIKit
       |                                        |
       | Audio + Video streams                  | Audio + Video streams
       v                                        v
  MediaRecorder                           MediaRecorder API (Browser)
  (Platform channel)                      mimeType: 'video/webm; codecs=vp8'
       |                                        |
       | .webm file (temp dir)                  | Blob chunks (1s interval)
       v                                        v
  Upload khi kết thúc                     Kết hợp Blob[] => File
       |                                        |
       +──────────────────┬─────────────────────+
                          |
                          v
             POST /api/v1/.../recording
             (multipart/form-data; field=file)
                          |
                          v
             FileServiceImpl.uploadWithPurpose()
             purpose=CONSULTATION_CALL_RECORDING
             accessMode=PRIVATE
                          |
                          v
             R2StorageService.store(key, bytes, mime)
             S3Client.putObject(
               bucket: carebridge-documents
               key: files/{UUID}.webm
               ServerSideEncryption: AES256
             )
```

### 8.2. Màn hình Consent PDPA (Trước khi gọi)

```
+─────────────────────────────────────────────────────────+
|  THÔNG BÁO GHI ÂM / GHI HÌNH TƯ VẤN                    |
|                                                         |
|  Cuộc gọi tư vấn y tế này sẽ được ghi âm và/hoặc       |
|  ghi hình nhằm mục đích:                                |
|  • Đảm bảo chất lượng tư vấn y tế                      |
|  • Giải quyết tranh chấp y tế nếu có                   |
|  • Lưu hồ sơ sức khỏe thai kỳ theo quy định pháp luật  |
|                                                         |
|  Bản ghi được mã hóa AES-256 và lưu trữ tại            |
|  Cloudflare R2 Private Bucket. Chỉ Quản trị viên        |
|  CareBridge có thẩm quyền mới được truy cập.            |
|                                                         |
|  [✓ Đồng ý & Tham gia]          [✗ Từ chối]            |
+─────────────────────────────────────────────────────────+
```

`consent_attested = true` được lưu vào `conversation_calls` khi người dùng bấm "Đồng ý".

---

## 9. Cấu trúc Mã nguồn — Backend (Spring Boot)

### 9.1. Package `com.carebridge.backend.directchat`

```
directchat/
├── controller/
│   ├── ConversationCallController.java      ← API cuộc gọi
│   ├── DirectConversationController.java    ← API conversation
│   └── AdminConsultationCallController.java ← Admin endpoints
│
├── service/impl/
│   ├── ConversationCallServiceImpl.java     ← LOGIC CHÍNH cuộc gọi + recording
│   ├── DirectConversationServiceImpl.java
│   ├── DirectMessageServiceImpl.java
│   └── AdminConsultationCallServiceImpl.java
│
├── entity/
│   ├── ConversationCall.java               ← JPA: conversation_calls
│   ├── DirectConversation.java             ← JPA: direct_conversations
│   ├── DirectMessage.java                  ← JPA: direct_messages
│   ├── CallStatus.java                     ← Enum: INITIATED..FAILED
│   ├── CallType.java                       ← Enum: VOICE, VIDEO
│   └── MessageType.java                    ← Enum: TEXT, ATTACHMENT, CALL_EVENT, LOCATION
│
├── repository/
│   ├── ConversationCallRepository.java     ← JPA + conditional UPDATE queries
│   ├── DirectConversationRepository.java
│   └── ConversationTimelineRepository.java ← Cursor-based pagination
│
├── job/
│   ├── CallTimeoutReconciliationJob.java   ← @Scheduled: auto MISSED sau 45s
│   └── FirebaseEventRetentionJob.java      ← @Scheduled: xóa Firebase events > 24h
│
└── event/
    └── DirectMessageNotificationListener.java ← Push lên Firebase RTDB

com.carebridge.backend.file/
├── service/impl/
│   ├── FileServiceImpl.java               ← Routing: r2 vs cloudinary
│   ├── R2StorageService.java              ← AWS S3 SDK => Cloudflare R2
│   └── CloudinaryStorageService.java
└── enums/
    ├── FilePurpose.java                   ← CONSULTATION_CALL_RECORDING
    ├── FileKind.java                      ← DOCUMENT
    └── FileAccessMode.java                ← PRIVATE
```

### 9.2. REST API Endpoints — Cuộc gọi

| Method | Endpoint | Role | Mô tả |
|--------|----------|------|-------|
| `POST` | `/api/v1/direct-conversations` | MOTHER, EXPERT | Tạo hoặc lấy conversation |
| `POST` | `.../calls` | MOTHER, EXPERT, FAMILY | Bắt đầu cuộc gọi |
| `PATCH` | `.../calls/{callId}/ringing` | MOTHER, EXPERT, FAMILY | Báo đang ringing |
| `PATCH` | `.../calls/{callId}/answer` | MOTHER, EXPERT, FAMILY | Chấp nhận gọi |
| `PATCH` | `.../calls/{callId}/decline` | MOTHER, EXPERT, FAMILY | Từ chối gọi |
| `PATCH` | `.../calls/{callId}/end` | MOTHER, EXPERT, FAMILY | Kết thúc gọi |
| `POST` | `.../calls/{callId}/join-credentials` | MOTHER, EXPERT, FAMILY | Lấy Zego token |
| `POST` | `.../calls/{callId}/recording` | MOTHER, EXPERT, FAMILY | Upload bản ghi |
| `GET` | `/api/v1/admin/consultation-calls` | ADMIN | Danh sách tất cả cuộc gọi |
| `GET` | `/api/v1/admin/consultation-calls/{callId}/recording-url` | ADMIN | Presigned URL 15 phút |
| `DELETE` | `/api/v1/admin/consultation-calls/{callId}/recording` | ADMIN, SYSTEM_ADMIN | Xóa vĩnh viễn object lưu trữ và metadata bản ghi; giữ lại lịch sử cuộc gọi |

---

## 10. Cấu trúc Mã nguồn — Mobile App (Flutter)

### 10.1. Cấu trúc Thư mục

```
lib/features/directChat/
├── calls/
│   ├── direct_call_api.dart           ← HTTP: initiate/answer/end/upload
│   ├── direct_call_coordinator.dart   ← State machine (listen Firebase)
│   ├── direct_call_state.dart         ← Sealed class: idle/ringing/active
│   ├── zego_express_call_room.dart    ← ZegoExpressEngine integration
│   ├── conversation_signal_hub.dart   ← Firebase RTDB listener
│   ├── ringtone_player.dart           ← Chuông báo cuộc gọi đến
│   └── rtc_permissions.dart           ← Xin quyền micro/camera
│
├── models/
│   ├── conversation_call.dart
│   ├── direct_conversation.dart
│   ├── zego_join_credentials.dart
│   └── timeline_item.dart
│
└── screens/
    ├── direct_chat_screen.dart
    ├── conversation_list_screen.dart
    └── expert_directory_screen.dart
```

### 10.2. Upload Recording — Flutter

```dart
// direct_call_api.dart
Future<ConversationCall> uploadRecording({
  required String conversationId,
  required String callId,
  required String filePath,   // đường dẫn file .webm local
  int? recordedDurationSeconds,
  bool consentAttested = true,
}) async {
  final fields = {
    'consentAttested': consentAttested.toString(),
    if (recordedDurationSeconds != null)
      'recordedDurationSeconds': recordedDurationSeconds.toString(),
  };
  return await apiMultipart(
    '/api/v1/direct-conversations/$conversationId/calls/$callId/recording',
    fields,
    filePath: filePath,
    fileFieldName: 'file',
  );
}
```

---

## 11. Cấu trúc Mã nguồn — Web Admin Portal (React/TypeScript)

### 11.1. Cấu trúc Thư mục

```
src/features/
├── directChat/
│   ├── calls/
│   │   ├── DirectCallProvider.tsx         ← Context Provider + MediaRecorder
│   │   ├── directCallApi.ts               ← HTTP: initiate/end/upload
│   │   ├── directCallCoordinator.ts       ← State machine (listen Firebase)
│   │   ├── directCallState.ts             ← Type: idle/ringing/active/ended
│   │   ├── zegoRoomSession.ts             ← ZegoUIKitPrebuilt integration
│   │   └── rtcMediaPermissions.ts         ← Request camera/mic
│   │
│   └── components/
│       └── ChatPanel.tsx                  ← UI chat panel
│
└── consultationManagement/
    ├── components/
    │   └── CallRecordingPlayerModal.tsx    ← Player modal xem bản ghi
    └── pages/
        └── ConsultationManagementPage.tsx ← Trang quản lý tư vấn (Admin)
```

### 11.2. Luồng ghi âm và upload — Web Expert

```typescript
// DirectCallProvider.tsx (React)
const handleCallEnd = async () => {
  // 1. Dừng MediaRecorder và thu thập Blob
  recorder.stop();
  const recordedBlob = new Blob(chunks, { type: 'video/webm' });
  
  // 2. Kết thúc cuộc gọi trên server
  await endCall(conversationId, callId);
  
  // 3. Upload bản ghi (multipart/form-data qua Axios)
  const formData = new FormData();
  formData.append('file', recordedBlob, `recording-${callId}.webm`);
  formData.append('consentAttested', 'true');
  await axios.post(`.../calls/${callId}/recording`, formData);
};
```

---

## 12. Bảo mật & Phân quyền

### 12.1. Mô hình phân quyền

| Tính năng | MOTHER | FAMILY | EXPERT | ADMIN | SYSTEM |
|-----------|--------|--------|--------|-------|--------|
| Tạo conversation | ✅ | ✅ | ✅ | ❌ | ❌ |
| Xem conversation của mình | ✅ | ✅ | ✅ | ✅ | ❌ |
| Gửi tin nhắn | ✅ | ✅ | ✅ | ❌ | ❌ |
| Bắt đầu cuộc gọi | ✅ | ✅ | ✅ | ❌ | ❌ |
| Upload bản ghi | ✅ | ✅ | ✅ | ❌ | ❌ |
| Xem danh sách tất cả | ❌ | ❌ | ❌ | ✅ | ❌ |
| Phát lại bản ghi | ❌ | ❌ | ❌ | ✅ | ❌ |
| Xóa vĩnh viễn bản ghi | ❌ | ❌ | ❌ | ✅ | ❌ |
| Lấy Presigned URL | ❌ | ❌ | ❌ | ✅ | ✅ |

### 12.2. ZegoCloud Token — Server-side HMAC

Token được ký `HMAC-SHA256` bởi App Secret — chỉ tồn tại trên server. Client **không bao giờ** biết App Secret.

```java
// Token giới hạn: appId + roomId + userId + expiresAt + HMAC signature
ZegoTokenDto token = zegoCloudService.generateToken(
    "cb_" + callId_no_dash,    // roomId
    "u_" + userId_no_dash,      // zegoUserId
    user.getDisplayName()       // displayName
);
// Token chỉ valid 1 giờ và chỉ dùng được cho 1 room cụ thể
```

### 12.3. Presigned URL — Truy cập bản ghi an toàn

```java
// R2StorageService.generatePresignedUrl(key, 15)
// URL hết hạn sau 15 phút:
// https://{account}.r2.cloudflarestorage.com/carebridge-documents/files/{UUID}.webm
//   ?X-Amz-Expires=900
//   &X-Amz-Signature=<HMAC-SHA256>
```

Mọi truy cập đều bị ghi vào `audit_logs`: `RECORDING_ACCESSED`.

---

## 13. Lưu trữ Bảo mật — Cloudflare R2 Private Bucket

### 13.1. So sánh lựa chọn lưu trữ

| Tiêu chí | Cloudflare R2 | AWS S3 | Firebase Storage | Cloudinary |
|----------|--------------|--------|-----------------|------------|
| **Chi phí egress** | ✅ Miễn phí | ❌ $0.09/GB | ❌ $0.12/GB | ❌ Quota |
| **AES-256 at-rest** | ✅ | ✅ | ✅ | ⚠️ Không rõ |
| **Private bucket** | ✅ | ✅ | ✅ | ❌ CDN public |
| **S3-compatible API** | ✅ | ✅ Native | ❌ | ❌ |
| **Free tier** | ✅ 10GB/tháng | ❌ | ⚠️ Giới hạn | ❌ |
| **Phù hợp medical records** | ✅ | ✅ | ✅ | ❌ |

### 13.2. Cấu hình R2StorageConfig

```java
// R2StorageConfig.java — sanitizeEndpoint() tránh double-bucket-name bug
// .env có thể là: https://{account}.r2.cloudflarestorage.com/carebridge-documents
// => sanitize thành: https://{account}.r2.cloudflarestorage.com
private static String sanitizeEndpoint(String endpoint) {
    String clean = endpoint.stripTrailing();
    while (clean.endsWith("/")) clean = clean.substring(0, clean.length() - 1);
    // Xóa /{bucketName} ở cuối nếu có
    if (clean.endsWith("/" + bucketName)) {
        clean = clean.substring(0, clean.length() - bucketName.length() - 1);
    }
    return clean;
}
```

### 13.3. FileServiceImpl — Routing Logic

```java
// FilePurpose.CONSULTATION_CALL_RECORDING => luôn dùng R2 (private)
// FilePurpose.AVATAR, ATTACHMENT => dùng Cloudinary (public CDN)
private IStorageService resolveProvider(FilePurpose purpose, FileAccessMode mode) {
    if (mode == FileAccessMode.PRIVATE || 
        purpose == FilePurpose.CONSULTATION_CALL_RECORDING) {
        return r2StorageService;  // AES-256, Private bucket
    }
    return cloudinaryStorageService;  // Public CDN
}
```

---

## 14. Admin Portal — Giám sát & Phát lại Bản ghi

### 14.1. Trang Quản lý Tư vấn

**URL:** Admin Portal → `/admin/consultation-calls`

Thông tin hiển thị mỗi cuộc gọi:
- Mã cuộc gọi (8 ký tự đầu của callId)
- Thời gian (ngày/giờ)
- Tên Người Mẹ + số điện thoại
- Tên Chuyên gia + chuyên khoa
- Loại (Video/Voice)
- Thời lượng (mm:ss)
- Trạng thái (Hoàn thành, Nhỡ, Từ chối...)
- Nút "Phát bản ghi", "Tải file" và "Xóa bản ghi"

### 14.2. CallRecordingPlayerModal

```typescript
// 1. Gọi API lấy presigned URL (chỉ ADMIN)
const url = await getCallRecordingPresignedUrl(call.callId);

// 2. Chuẩn hóa URL (normalize legacy cloudinary vs R2)
const normalizedUrl = normalizeMediaUrl(url);

// 3. Phát trong HTML5 video player
<video src={normalizedUrl} controls autoPlay>

// 4. Fallback: nếu không phát được → nút tải file
<a href={normalizedUrl} download>Tải file (Video)</a>
```

Khi quản trị viên chọn **Xóa bản ghi**, player đang phát được dừng trước khi mở hộp thoại xác nhận. Chỉ sau khi xác nhận, backend mới khóa bản ghi cuộc gọi, xóa object khỏi Cloudflare R2 (hoặc storage provider legacy), rồi xóa metadata attachment và đặt trạng thái recording về `NONE`. Nếu storage trả lỗi, transaction không thay đổi database và hộp thoại giữ nguyên lỗi để người dùng thử lại; thao tác lặp lại khi bản ghi đã không còn là idempotent `204 No Content`.

---

## 15. Vận hành & Cấu hình Môi trường

### 15.1. Biến môi trường bắt buộc

```bash
# ZegoCloud
ZEGO_APP_ID=<appId>
ZEGO_APP_SIGN=<appSign>    # Server-only, không expose ra client

# Firebase Realtime Database
FIREBASE_DATABASE_URL=https://<project>.firebaseio.com
GOOGLE_APPLICATION_CREDENTIALS=/path/to/firebase-service-account.json

# Cloudflare R2 (bắt buộc cho production recording)
R2_ACCESS_KEY_ID=<key_id>
R2_SECRET_ACCESS_KEY=<secret_key>
R2_BUCKET_NAME=carebridge-documents
R2_ENDPOINT=https://<accountId>.r2.cloudflarestorage.com   # KHÔNG có trailing / hoặc bucket name
R2_REGION=auto

# Storage routing
FILE_STORAGE_PROVIDER=cloudinary          # public provider (ảnh, docs)
PRIVATE_FILE_STORAGE_PROVIDER=r2          # private provider (medical records)

# Call timeout
DIRECTCHAT_CALL_RING_TIMEOUT_SECONDS=45
DIRECTCHAT_FIREBASE_EVENT_RETENTION_HOURS=24
```

### 15.2. Checklist Deployment Production

```
✅ R2_ENDPOINT KHÔNG có trailing slash hoặc bucket name
✅ ZEGO_APP_SIGN KHÔNG bao giờ xuất hiện trong client-side code
✅ Firebase Service Account KHÔNG commit vào git
✅ CORS của CareBridge API cho phép origin Web Portal
✅ Cloudflare R2 CORS config cho phép presigned GET từ browser
✅ Spring Boot: spring.servlet.multipart.max-file-size=500MB
✅ Flyway V5 migration: conversation_calls có đủ cột recording_*
✅ Flyway V6 migration: backfill storage_provider=r2 cho records cũ
✅ Mọi RECORDING_ACCESSED đều ghi vào audit_logs
```

---

## Phụ lục A — Danh sách File Quan trọng

| File | Đường dẫn | Vai trò |
|------|-----------|---------|
| `ConversationCallController.java` | `backend/.../directchat/controller/` | REST API cuộc gọi |
| `ConversationCallServiceImpl.java` | `backend/.../directchat/service/impl/` | Business logic cuộc gọi + recording |
| `AdminConsultationCallServiceImpl.java` | `backend/.../directchat/service/impl/` | Admin: list + presigned URL |
| `ConversationCall.java` | `backend/.../directchat/entity/` | JPA Entity `conversation_calls` |
| `R2StorageService.java` | `backend/.../file/service/impl/` | Upload/presign Cloudflare R2 |
| `R2StorageConfig.java` | `backend/.../file/config/` | S3Client + sanitizeEndpoint() |
| `FileServiceImpl.java` | `backend/.../file/service/impl/` | Routing + uploadWithPurpose() |
| `directCallCoordinator.ts` | `webapp/src/.../directChat/calls/` | State machine cuộc gọi (Web) |
| `DirectCallProvider.tsx` | `webapp/src/.../directChat/calls/` | React Context + MediaRecorder |
| `zegoRoomSession.ts` | `webapp/src/.../directChat/calls/` | ZegoUIKitPrebuilt integration |
| `CallRecordingPlayerModal.tsx` | `webapp/src/.../consultationManagement/` | Admin player modal |
| `direct_call_coordinator.dart` | `mobileapp/lib/.../directChat/calls/` | State machine (Flutter) |
| `zego_express_call_room.dart` | `mobileapp/lib/.../directChat/calls/` | Zego Flutter integration |

## Phụ lục B — Tóm tắt Quyết định Kỹ thuật

| Quyết định | Phương án Chọn | Lý do |
|------------|---------------|-------|
| Signaling Protocol | Firebase RTDB | Real-time < 50ms, free tier, SDK Flutter + React |
| WebRTC Platform | ZegoCloud | SDK song song Flutter + Web, token HMAC |
| Recording Strategy | Client-side MediaRecorder | 100% free, không phụ thuộc ZegoCloud |
| Private Storage | Cloudflare R2 | AES-256, S3-compatible, miễn phí egress |
| Pagination Chat | Cursor-based (sent_at) | Không bỏ sót real-time, O(log n) |
| Call State Machine | DB-level conditional UPDATE | Ngăn race condition giữa các request đồng thời |
| Zego Token | Server-side HMAC | App Secret không bao giờ expose ra client |

---

*Tài liệu này được tạo bởi HuyND — CareBridge SEP490-G79*  
*Phiên bản: 1.0.0 | Ngày: 2026-08-19*
