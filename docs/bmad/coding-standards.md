# CareBridge Coding Standards

**Status**: Draft  
**Version**: 1.0  
**Date**: 2026-06-17  
**Author**: Claude Sonnet 4.6 (Anthropic)

---

## 1. General Principles

- **Clarity over cleverness**: Code should be easy to read and understand
- **Consistency**: Follow these standards uniformly across the codebase
- **Simplicity**: Prefer simple, straightforward solutions; avoid premature optimization
- **Safety**: Never compromise healthcare safety, security, or privacy for convenience
- **Testability**: Write code that is easy to test (dependency injection, small functions, pure logic)

---

## 2. Backend (Java / Spring Boot)

### 2.1 Package and File Naming

| Element | Convention | Example |
|---------|------------|---------|
| Packages | all lowercase, dot-separated, start with `com.carebridge.backend` | `com.carebridge.backend.security.controller` |
| Classes | PascalCase | `UserService.java`, `HealthRecordController.java` |
| Interfaces | PascalCase with `Interface` suffix optional; prefer noun form | `HealthRecordRepository.java` (interface), `HealthRecordServiceImpl.java` (impl) |
| Methods | camelCase, verb-noun | `createUser()`, `findById()`, `validateConsent()` |
| Variables | camelCase, noun | `userList`, `healthRecord`, `isValid` |
| Constants | UPPER_SNAKE_CASE | `MAX_FILE_SIZE`, `DEFAULT_ROLE` |
| Test classes | Same as class under test + `Tests` suffix | `UserServiceTests.java` |

### 2.2 Layer Structure

Each domain module:

```
{domain}/
├── {Domain}Controller.java        (if domain has REST API)
├── {Domain}Service.java           (interface)
├── {Domain}ServiceImpl.java       (implementation)
├── {Domain}Repository.java        (Spring Data interface)
├── {Domain}.java                  (JPA @Entity)
├── dto/
│   ├── request/
│   │   ├── Create{Domain}Request.java
│   │   ├── Update{Domain}Request.java
│   │   └── Get{Domain}Query.java  (for query params)
│   └── response/
│       ├── {Domain}Response.java
│       └── {Domain}DetailResponse.java
├── mapper/
│   └── {Domain}Mapper.java        (MapStruct or manual)
└── policy/
    └── {Domain}Policy.java        (business rules)
```

### 2.3 Controller Conventions

- Annotate with `@RestController` and `@RequestMapping("/api/v1/{domain-plural}")`
- Use `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`
- Validate request DTOs with `@Valid`
- Return `ResponseEntity<T>` or use `@RestController` with response wrapper
- Do **not** throw exceptions directly; let `@ControllerAdvice` handle

**Example**:
```java
@RestController
@RequestMapping("/api/v1/health-records")
@RequiredArgsConstructor
public class HealthRecordController {

    private final HealthRecordService healthRecordService;
    private final HealthRecordMapper mapper;

    @PostMapping
    @PreAuthorize("hasRole('MOTHER') or hasRole('FAMILY')")
    public ResponseEntity<HealthRecordResponse> create(
            @Valid @RequestBody CreateHealthRecordRequest request,
            Principal principal) {
        HealthRecord record = healthRecordService.create(principal.getName(), request);
        return ResponseEntity.ok(mapper.toResponse(record));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@healthRecordPolicy.canView(#id, principal)")
    public ResponseEntity<HealthRecordResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(healthRecordService.findById(id));
    }
}
```

### 2.4 Service Conventions

- Define interface and implementation separately (except trivial cases)
- Annotate impl with `@Service` and `@Transactional` (read-only methods can be `@Transactional(readOnly = true)`)
- Use constructor injection (final fields) - **never** field injection
- Keep methods focused; single responsibility per method
- Check preconditions explicitly and throw domain exceptions

**Example**:
```java
@Service
@Transactional
@RequiredArgsConstructor
public class HealthRecordServiceImpl implements HealthRecordService {

    private final HealthRecordRepository repository;
    private final HealthRecordMapper mapper;
    private final ConsentService consentService;
    private final AuditService auditService;

    @Override
    public HealthRecord create(String userId, CreateHealthRecordRequest request) {
        // Validate
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        // Business rule: consent required
        consentService.ensureConsent(userId, DataType.HEALTH_RECORD, Purpose.CREATE);

        // Build entity
        HealthRecord record = mapper.toEntity(request);
        record.setUserId(userId);
        record.setCreatedAt(Instant.now());

        // Save
        HealthRecord saved = repository.save(record);

        // Audit
        auditService.log(Action.CREATE_HEALTH_RECORD, userId, saved.getId(), null);

        return saved;
    }
}
```

### 2.5 Repository Conventions

- Extend `JpaRepository<Entity, ID>` for PostgreSQL entities
-  Use `JpaRepository<Entity, ID>`for PostgreSQL entities.
   Do not introduce MongoRepository or MongoDB unless explicitly approved.
- Define query methods using Spring Data derived queries when possible:
  - `findByUserIdOrderByCreatedAtDesc(Long userId)`
  - `findByCategoryAndStatus(Category category, PostStatus status)`
- Use `@Query` for complex JPQL queries; keep queries readable with line breaks
- Never put business logic in repository

**Example**:
```java
@Repository
public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {
    List<HealthRecord> findByUserIdOrderByCreatedAtDesc(String userId);
    List<HealthRecord> findByBabyIdAndRecordType(Long babyId, RecordType type);
    @Query("SELECT h FROM HealthRecord h WHERE h.userId = :userId AND h.createdAt >= :since")
    List<HealthRecord> findRecentByUser(@Param("userId") String userId, @Param("since") Instant since);
}
```

### 2.6 Entity Conventions

- Use JPA annotations (`@Entity`, `@Table`, `@Id`, `@GeneratedValue`)
- Prefer `Long` for primary keys (auto-generated)
- Use `@Column(nullable = false)` for required fields
- Use `@Enumerated(EnumType.STRING)` for enums (not ordinal)
- Include audit fields: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`
- Use `@CreationTimestamp` and `@UpdateTimestamp` if using Hibernate
- Avoid lazy loading surprises: use `@JsonIgnore` on back-references or DTO projection
- Lombok: use `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder` but be cautious with `@Data` (it includes `equals()` and `hashCode()` which may cause issues with JPA)

**Example**:
```java
@Entity
@Table(name = "health_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(name = "baby_id")
    private Long babyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordType recordType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
```

### 2.7 DTO Conventions

- DTOs are **immutable** when possible (final fields, constructor)
- Use validation annotations: `@NotNull`, `@Size(min = 2, max = 100)`, `@Email`, `@Pattern(regexp = "...")`
- For nested objects, use `@Valid` to cascade validation
- Use `@JsonInclude(JsonInclude.Include.NON_NULL)` to omit null fields in JSON
- Request DTOs may have mutable setters for frameworks; response DTOs should be immutable

**Example**:
```java
@Getter @ToString @EqualsAndHashCode
public class CreateHealthRecordRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 1000)
    private String description;

    @NotNull
    private RecordType recordType;

    @NotNull
    @PastOrPresent
    private LocalDate recordDate;

    @Positive
    private Long babyId;

    @Valid
    private List<MeasurementDto> measurements;
}
```

### 2.8 Mapper Conventions

- Use **MapStruct** for simple mappings (type-safe, compile-time generated)
- Manual mapping acceptable for complex logic
- Mapper interfaces annotated with `@Mapper(componentModel = "spring")` to integrate with Spring
- MapEntity → Response, Request → Entity
- Separate mapper for each domain

**Example**:
```java
@Mapper(componentModel = "spring")
public interface HealthRecordMapper {

    HealthRecord toEntity(CreateHealthRecordRequest request);

    HealthRecord toEntity(UpdateHealthRecordRequest request, @MappingTarget HealthRecord entity);

    HealthRecordResponse toResponse(HealthRecord entity);

    default HealthRecordDetailResponse toDetailResponse(HealthRecord entity) {
        return HealthRecordDetailResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .recordDate(entity.getRecordDate())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
```

### 2.9 Policy Conventions

- Policies encapsulate reusable business rules
- Static methods are fine for stateless rules; instance methods if stateful or needs dependencies
- Throw domain exceptions when rule violated
- Name policies as `{Domain}{Policy}` (e.g., `ConsentCheckPolicy`, `RedFlagPolicy`)

**Example**:
```java
public class RedFlagPolicy {

    private static final List<Symptom> RED_FLAG_SYMPTOMS = List.of(
            Symptom.HEMORRHAGE_SEVERE,
            Symptom.CHEST_PAIN,
            Symptom.SHORTNESS_OF_BREATH,
            Symptom.SUICIDAL_THOUGHTS,
            Symptom.HIGH_FEVER_INFANT
    );

    public static boolean isRedFlag(Symptom symptom) {
        return RED_FLAG_SYMPTOMS.contains(symptom);
    }

    public static void ensureNotRedFlag(Symptom symptom) {
        if (isRedFlag(symptom)) {
            throw new RedFlagException("Red-flag symptom detected: " + symptom);
        }
    }
}
```

### 2.10 Exception Handling

- Create custom exception classes extending `RuntimeException`
- Use `@ControllerAdvice` with `@ExceptionHandler` to convert exceptions to HTTP responses
- Standard error response structure:
```json
{
  "timestamp": "2025-06-17T10:30:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Invalid input: title is required",
  "path": "/api/v1/health-records",
  "details": null
}
```

**Example**:
```java
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ValidationException extends RuntimeException {
    public ValidationException(String message) { super(message); }
}

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ConsentException extends RuntimeException {
    public ConsentException(String message) { super(message); }
}

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ErrorResponse error = ErrorResponse.of(400, "VALIDATION_ERROR", message);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ConsentException.class)
    public ResponseEntity<ErrorResponse> handleConsent(ConsentException ex) {
        ErrorResponse error = ErrorResponse.of(403, "CONSENT_DENIED", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse error = ErrorResponse.of(500, "INTERNAL_ERROR", "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

### 2.11 Logging

- Use SLF4J API with Logback implementation
- Logger per class: `private static final Logger logger = LoggerFactory.getLogger(ClassName.class);`
- Log levels:
  - `DEBUG`: Detailed internal state, for development
  - `INFO`: Business events (user login, record created, payment processed)
  - `WARN`: Recoverable issues, retries, fallbacks
  - `ERROR`: Failures, exceptions, system errors
- Never log sensitive data: passwords, tokens, PII (phone numbers, health details)
- Use structured logging when possible (key-value pairs)

**Example**:
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public Payment processPayment(PaymentRequest request) {
        log.info("Processing payment for user={}, amount={}", request.getUserId(), request.getAmount());

        try {
            Payment payment = paymentRepository.save(Payment.builder()
                    .userId(request.getUserId())
                    .amount(request.getAmount())
                    .status(PaymentStatus.PENDING)
                    .build());

            log.debug("Payment created with id={}", payment.getId());
            return payment;
        } catch (Exception e) {
            log.error("Payment processing failed for user={}", request.getUserId(), e);
            throw new PaymentException("Failed to process payment", e);
        }
    }
}
```

### 2.12 Testing Conventions

- JUnit 5 (`@Test`, `@BeforeEach`, `@AfterEach`)
- Mockito for mocking dependencies
- Test class location: `src/test/java/com/carebridge/backend/{domain}/`
- Test naming: `{ClassName}Tests.java` (e.g., `UserServiceTests.java`)
- Method naming: `given_when_then` or `should_ExpectedBehavior_When_Condition`

**Example**:
```java
@ExtendWith(MockitoExtension.class)
class HealthRecordServiceTests {

    @Mock
    private HealthRecordRepository repository;

    @Mock
    private ConsentService consentService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private HealthRecordServiceImpl service;

    @Test
    void givenValidRequest_whenCreate_thenSaveAndAudit() {
        // Given
        String userId = "user123";
        CreateHealthRecordRequest request = new CreateHealthRecordRequest(...);
        HealthRecord saved = HealthRecord.builder().id(1L).userId(userId).build();
        when(repository.save(any())).thenReturn(saved);

        // When
        HealthRecord result = service.create(userId, request);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(consentService).ensureConsent(eq(userId), eq(DataType.HEALTH_RECORD), eq(Purpose.CREATE));
        verify(auditService).log(eq(Action.CREATE_HEALTH_RECORD), eq(userId), eq(1L), isNull());
    }

    @Test
    void givenNoConsent_whenCreate_thenThrowConsentException() {
        // Given
        String userId = "user123";
        CreateHealthRecordRequest request = new CreateHealthRecordRequest(...);
        doThrow(new ConsentException("No consent")).when(consentService).ensureConsent(anyString(), any(), any());

        // When/Then
        assertThrows(ConsentException.class, () -> service.create(userId, request));
    }
}
```

---

## 3. Frontend (React + TypeScript + Vite)

### 3.1 File and Folder Naming

| Element | Convention | Example |
|---------|------------|---------|
| Folders (features) | kebab-case | `user-management/`, `expert-verification/` |
| Folders (shared) | kebab-case | `components/`, `hooks/`, `utils/` |
| Components | PascalCase | `UserTable.tsx`, `ConsentModal.tsx` |
| Hooks | camelCase with `use` prefix | `useAuth.ts`, `useForm.ts` |
| Services | camelCase with `Service` suffix | `apiService.ts`, `authService.ts` |
| Utilities | camelCase | `formatDate.ts`, `validation.ts` |
| Types/Interfaces | PascalCase | `User`, `ApiResponse<T>` |
| Constants | UPPER_SNAKE_CASE | `API_ENDPOINTS`, `ROLES` |
| CSS modules | kebab-case | `user-table.module.css` |

### 3.2 Feature Structure

```
features/{feature-name}/
├── pages/            (route-level components)
│   └── {Feature}Page.tsx
├── components/       (feature-specific reusable components)
│   ├── {Feature}Form.tsx
│   └── {Feature}Table.tsx
├── services/         (API calls)
│   └── {feature}Service.ts
├── models/           (TypeScript types/interfaces)
│   └── {feature}Types.ts
└── index.ts         (barrel export)
```

### 3.3 Component Conventions

- Functional components with hooks
- Props as interface or type
- Prop drilling minimized; use context or state management for global state
- Keep components small and focused (single responsibility)

**Example**:
```tsx
// features/user-management/components/UserTable.tsx
interface UserTableProps {
  users: User[];
  onEdit: (user: User) => void;
  onDelete: (userId: string) => void;
  loading: boolean;
}

export const UserTable: React.FC<UserTableProps> = ({
  users,
  onEdit,
  onDelete,
  loading
}) => {
  if (loading) return <Spinner />;

  return (
    <Table>
      <thead>
        <tr>
          <th>Name</th>
          <th>Email</th>
          <th>Role</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {users.map(user => (
          <tr key={user.id}>
            <td>{user.fullName}</td>
            <td>{user.email}</td>
            <td>{user.role}</td>
            <td>
              <Button onClick={() => onEdit(user)}>Edit</Button>
              <Button danger onClick={() => onDelete(user.id)}>Delete</Button>
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
};
```

### 3.4 API Service Conventions

- Centralized `apiClient` using Axios with interceptors (auth token, error handling)
- One service per domain: `authService`, `userService`, `healthRecordService`
- Methods return `Promise<T>` or `Promise<ApiResponse<T>>`
- Handle errors consistently

**Example**:
```typescript
// shared/api/apiClient.ts
import axios from 'axios';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  timeout: 10000,
});

apiClient.interceptors.request.use(config => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      // redirect to login
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default apiClient;

// features/auth/services/authService.ts
import apiClient from '@/shared/api/apiClient';

export interface LoginRequest {
  phone: string;
  otp: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

export const authService = {
  login: (data: LoginRequest): Promise<LoginResponse> =>
    apiClient.post('/api/v1/auth/login', data),

  logout: (): Promise<void> =>
    apiClient.post('/api/v1/auth/logout'),

  verifyOTP: (phone: string): Promise<{sent: boolean}> =>
    apiClient.post('/api/v1/auth/verify-otp', { phone }),
};
```

### 3.5 State Management

- **React Query**: server state (API data) - use `useQuery`, `useMutation`
- **Zustand** or **Context**: global UI state (auth, theme, sidebar)
- **Local state**: component-specific UI state (`useState`)

**React Query example**:
```tsx
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

export const useUsers = () => {
  return useQuery({
    queryKey: ['users'],
    queryFn: () => userService.getAll(),
  });
};

export const useCreateUser = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateUserRequest) => userService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });
};
```

### 3.6 Form Handling

- **React Hook Form** for form state and validation
- **Zod** for schema validation
- Integrate Zod with RHF via `@hookform/resolvers`

**Example**:
```tsx
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';

const createUserSchema = z.object({
  fullName: z.string().min(2, 'Name must be at least 2 characters'),
  email: z.string().email('Invalid email'),
  phone: z.string().regex(/^[0-9]{10}$/, 'Phone must be 10 digits'),
  role: z.enum(['MOTHER', 'EXPERT', 'MODERATOR']),
});

type CreateUserForm = z.infer<typeof createUserSchema>;

export const CreateUserForm: React.FC = () => {
  const { register, handleSubmit, formState: { errors } } = useForm<CreateUserForm>({
    resolver: zodResolver(createUserSchema),
  });

  const onSubmit = (data: CreateUserForm) => {
    console.log(data);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <input {...register('fullName')} />
      {errors.fullName && <span>{errors.fullName.message}</span>}
      {/* other fields */}
      <button type="submit">Create</button>
    </form>
  );
};
```

### 3.7 Styling

- Use **Tailwind CSS** or **CSS Modules** (not plain CSS-in-JS unless approved)
- Prefer Tailwind for utility-first styling
- Component-specific styles in `.module.css` files if needed
- Global styles in `src/index.css` (reset, theme variables)

---

## 4. Mobile (Flutter / Dart)

### 4.1 File and Folder Naming

| Element | Convention | Example |
|---------|------------|---------|
| Folders (features) | snake_case | `mother_journey/`, `baby_care/` |
| Folders (core/shared) | snake_case | `core/`, `shared/`, `integrations/` |
| Screens | PascalCase + `Screen` suffix | `MotherJourneyScreen.dart`, `LoginScreen.dart` |
| Widgets | PascalCase + `Widget` suffix (optional) | `BabyProfileCard.dart`, `ConsentDialog.dart` |
| Models | PascalCase | `User.dart`, `HealthRecord.dart` |
| Services | PascalCase + `Service` suffix | `AuthService.dart`, `ApiService.dart` |
| Repositories | PascalCase + `Repository` suffix | `HealthRecordRepository.dart` |
| Utilities | camelCase + `.dart` | `validators.dart`, `formatters.dart` |
| Constants | upperCamelCase or UPPER_SNAKE_CASE | `AppConstants`, `ROUTES` |

### 4.2 Feature Structure

```
features/
├── {feature_name}/
│   ├── screens/
│   │   └── {Feature}Screen.dart
│   ├── widgets/
│   │   └── {Feature}Card.dart
│   ├── services/
│   │   └── {feature}Service.dart
│   ├── repositories/
│   │   └── {feature}Repository.dart
│   └── models/
│       └── {feature}_model.dart
```

### 4.3 Widget Conventions

- Prefer `const` constructors for performance
- Use `StatelessWidget` when possible; stateful only when needed
- Extract complex UI into separate widget classes
- Use `Key` only when necessary (e.g., in lists)

**Example**:
```dart
class HealthRecordCard extends StatelessWidget {
  final HealthRecord record;
  final VoidCallback? onTap;

  const HealthRecordCard({
    super.key,
    required this.record,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        leading: Icon(Icons.medical_services),
        title: Text(record.title),
        subtitle: Text(formatDate(record.recordDate)),
        trailing: record.fileAttachment != null
            ? Icon(Icons.attach_file)
            : null,
        onTap: onTap,
      ),
    );
  }
}
```

### 4.4 State Management

- Use **Riverpod** (preferred) or **Provider** for global state
- Use `ChangeNotifier` with `Consumer`/`ConsumerWidget` for shared state
- Local UI state with `setState` in `StatefulWidget`

**Riverpod example**:
```dart
final authProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  return AuthNotifier(authService: ref.watch(authServiceProvider));
});

class AuthNotifier extends StateNotifier<AuthState> {
  final AuthService _authService;

  AuthNotifier({required AuthService authService})
      : _authService = authService,
        super(AuthState.initial());

  Future<void> login(String phone, String otp) async {
    state = AuthState.loading();
    try {
      final user = await _authService.login(phone, otp);
      state = AuthState.authenticated(user);
    } catch (e) {
      state = AuthState.error(e.toString());
    }
  }
}
```

### 4.5 API Layer

- Use **Dio** for HTTP client (better than `http` package)
- Interceptors for auth token injection, error handling, logging
- Repository pattern to abstract data source (network vs. local)

**Example**:
```dart
class ApiClient {
  final Dio _dio = Dio();

  ApiClient() {
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          final token = localStorage.read(key: 'access_token');
          if (token != null) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          return handler.next(options);
        },
        onError: (error, handler) {
          if (error.response?.statusCode == 401) {
            // navigate to login
          }
          return handler.next(error);
        },
      ),
    );
  }

  Future<Response> get(String path) => _dio.get(path);
  Future<Response> post(String path, dynamic data) => _dio.post(path, data: data);
}
```

### 4.6 Error Handling

- Custom exception classes: `AuthException`, `NetworkException`, `ConsentException`
- Catch errors in UI and show user-friendly messages
- Use `try-catch` in async methods; propagate or handle appropriately

---

## 5. Testing Strategy

### 5.1 Backend Tests

| Test Type | Scope | Tools | Location |
|-----------|-------|-------|----------|
| Unit | Service, Policy, Mapper, Utility | JUnit 5, Mockito | `src/test/java/com/carebridge/backend/{domain}/` |
| Integration | Repository + DB, Service + real dependencies (some) | @SpringBootTest, Testcontainers (optional) | `src/test/java/com/carebridge/backend/integration/` |
| Security | RBAC, consent enforcement, authentication flows | MockMvc, @WithMockUser | `src/test/java/com/carebridge/backend/security/` |
| API | Endpoint contracts, request/response serialization | @WebMvcTest, MockMvc | `src/test/java/com/carebridge/backend/api/` |

**Minimum coverage**: 70% for services and policies.

### 5.2 Frontend Tests

| Test Type | Scope | Tools |
|-----------|-------|-------|
| Unit | Utilities, hooks (logic-only), services | Jest, Testing Library |
| Component | Render tests, user interactions | React Testing Library |
| Integration | Full page flows | React Testing Library + Mock Service Worker (MSW) |

**Focus**: Critical user journeys (login, health record creation, triage flow, post creation).

### 5.3 Mobile Tests

| Test Type | Scope | Tools |
|-----------|-------|-------|
| Unit | Services, models, utilities | test (flutter test) |
| Widget | Widget rendering, interactions | flutter_test, WidgetTester |
| Integration | Full screen flows | integration_test (driver) |

---

## 6. Security Checklist

- [ ] All API endpoints require authentication (except `/auth/login`, `/auth/verify-otp`)
- [ ] Passwords never logged
- [ ] SQL injection prevented (use JPA, never string concatenation)
- [ ] XSS prevented (React auto-escapes; avoid `dangerouslySetInnerHTML`)
- [ ] CSRF protection enabled for session-based flows (admin portal)
- [ ] Rate limiting on auth endpoints and AI triage
- [ ] File uploads validated (size, type, virus scan if feasible)
- [ ] Sensitive data encrypted at rest (PostgreSQL column-level encryption for phone numbers, health data if required)
- [ ] Secrets in environment variables, not in code
- [ ] HTTPS enforced in production

---

## 7. Performance Tips

- Database: Add indexes on foreign keys and frequently queried columns (`user_id`, `created_at`, `status`)
- N+1 queries: Use `@EntityGraph` or `JOIN FETCH` in JPA; avoid lazy loading in loops
- API pagination: Always paginate list endpoints (`Pageable` in Spring, `page` and `size` params)
- Mobile: Lazy-load images; cache API responses with React Query or Riverpod
- Caching: Consider Redis for frequently accessed data (e.g., emergency contacts, category lists) - **optional for MVP**

---

**Document End**
