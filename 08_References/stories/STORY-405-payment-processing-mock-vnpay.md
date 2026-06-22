# Story: Payment Processing (Mock VNPay)

**Story ID**: STORY-405  
**Epic**: EPIC-006-expert-ecosystem.md  
**Status**: DRAFT  
**Priority**: P3  
**Story Points**: 8  
**Owner**: TV4 (Expert Consultation)  
**Sprint**: Sprint 0 - Foundation  

---

## User Story

**As a** mother user  
**I want to** pay for my consultation booking  
**So that** my consultation is confirmed and the expert receives payment

---

## Context

This story implements payment processing using a mock VNPay provider. In production, this will integrate with VNPay gateway, but for MVP we use a deterministic mock that always succeeds.

The payment flow:
1. Mother requests payment for a booking
2. System generates payment request with mock VNPay URL (or immediate success in mock mode)
3. VNPay callback updates booking payment status
4. Commission is calculated for expert

---

## Requirements

- **3.3.1.53**: Pay Consultation Fee
- **3.1.2.1**: Process Payment Transaction
- Mock provider returns deterministic success
- Record transaction in `payment_transactions` table
- Update booking `paymentStatus = PAID`
- Trigger commission calculation

---

## API Design

### POST /api/v1/payments/process
Process payment for consultation  
**Auth**: Required (MOTHER role)  
**Request**: `ProcessPaymentRequest`
```json
{
  "bookingId": "uuid",
  "paymentMethod": "VNPAY"
}
```
**Response**: `ApiResponse<PaymentResponse>`
```json
{
  "success": true,
  "data": {
    "transactionId": "MOCK-VNPAY-20260620-123456",
    "bookingId": "uuid",
    "amount": 250000.00,
    "paymentStatus": "SUCCESS",
    "gatewayResponse": {"code": "00", "message": "Success"},
    "paidAt": "2026-06-20T12:30:45"
  }
}
```

### POST /api/v1/payments/callback/vnpay
Mock VNPay callback endpoint (webhook)  
**Auth**: None (called by VNPay gateway)  
**Query params**: VNPay standard params  
**Response**: "OK"

### GET /api/v1/payments/transactions/{bookingId}
Get payment status for a booking  
**Auth**: Required (mother or expert)

---

## Mock Payment Service

```java
@Service
@Profile("dev")
public class MockPaymentService implements PaymentService {

    @Override
    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        // Simulate payment processing delay
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        return PaymentResponse.builder()
            .transactionId("MOCK-VNPAY-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")))
            .bookingId(request.getBookingId())
            .amount(request.getAmount())
            .status(PaymentStatus.SUCCESS)
            .gatewayResponse(Map.of("code", "00", "message", "Success"))
            .paidAt(Instant.now())
            .build();
    }

    @Override
    public PaymentCallbackResponse handleCallback(Map<String, String> params) {
        // Always return success for mock
        return PaymentCallbackResponse.success("MOCK-VNPAY-" + params.get("vnp_TransactionNo"));
    }
}
```

---

## Acceptance Criteria

### Scenario: Mother pays for consultation successfully
Given authenticated mother with pending booking (paymentStatus = UNPAID)  
When POST `/api/v1/payments/process` with valid bookingId  
Then status 200  
And response.data.paymentStatus = "SUCCESS"  
And booking.paymentStatus updated to "PAID"  
And commission calculated and `commission_records` entry created  
And audit log with action `PAYMENT_PROCESSED`

### Scenario: Cannot pay for non-existent booking
Given booking ID does not exist  
When process payment  
Then status 404 "Booking not found"

### Scenario: Cannot pay for already paid booking
Given booking with paymentStatus = PAID  
When process payment  
Then status 400 "Booking already paid"

### Scenario: Payment failure (mock can simulate)
Configure mock to return failure (optional advanced)  
When process payment with invalid data  
Then paymentStatus = "FAILED"  
And booking remains UNPAID

---

## Files to Create

**Backend**:
- `payment/controller/PaymentController.java`
- `payment/service/PaymentService.java` (interface)
- `payment/service/MockPaymentService.java`
- `payment/repository/PaymentTransactionRepository.java`
- `payment/entity/PaymentTransaction.java` (exists - verify)
- `payment/dto/request/ProcessPaymentRequest.java`
- `payment/dto/response/PaymentResponse.java`
- `payment/mapper/PaymentTransactionMapper.java`
- `payment/policy/PaymentPolicy.java`

**Existing**: `ConsultationBooking` entity needs `paymentStatus` field

**Mobile**: `lib/features/consultation/screens/payment_confirmation_screen.dart`  
**Frontend**: `src/features/paymentRefunds/pages/PaymentProcessPage.tsx`

---

**Story End**
