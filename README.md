# CareBridge

CareBridge is a modern, premium Maternal and Early Childhood Healthcare Platform designed to support mothers and families through their healthcare journey, facilitating connections with experts and medical services.

---

## 🚀 Tech Stack

### Backend
- **Core**: Java 21, Spring Boot 3.5.x, Maven
- **Database**: PostgreSQL, Flyway (Migration)
- **Architecture**: Modular Monolith

### Frontend Web
- **Core**: React, TypeScript, Vite
- **Styling**: Vanilla CSS / Warm Claymorphism UI/UX design language

### Mobile App
- **Core**: Flutter, Dart

### Infrastructure & Integration
- **Infrastructure**: Docker Compose, GitLab CI/CD
- **Integrations**: Supabase, Firebase, TrackAsia, ZegoCloud, VNPay, Gmail SMTP, Gemini API

---

## 📁 Project Structure

The project follows a standard Software Development Life Cycle (SDLC) structure:

```text
CareBridge_SEP490_G79/
├── 01_Planning/                  # Meeting minutes, progress reports, schedule, and risk management
├── 02_Requirements/              # SRS, Use Cases, Business Rules, Context Diagrams, DFD, and RTM
├── 03_Design/                    # Architecture, Technical Design, UI/UX mockups, and Diagrams (Class, Sequence, Activity)
├── 04_Implement/                 # Implementation artifacts
├── 05_Development/               # Source code directories
│   ├── CareBridgeAPI/            # Java Spring Boot backend service
│   ├── CareBridgeMobileApp/      # Flutter mobile application
│   ├── CareBridgeWebApp/         # React web application
│   ├── Contracts/                # Shared API contracts or design files
│   ├── Database/                 # Database schema scripts & migrations
│   ├── Deployment/               # Docker & deployment configurations
│   ├── DevTools/                 # Development and developer helper tools
│   └── MachineLearning/          # Machine learning and AI modules
├── 06_Testing/                   # Test cases, reports, automated tests, security & privacy audits, and UAT
├── 07_Reports/                   # Project reports
└── 08_References/                # References & templates
```

---

## 🛠️ Quick Start & Commands

### 🟢 Backend (`05_Development/CareBridgeAPI`)
1. Create a `.env` file from `.env.example` with valid credentials:
   - `SUPABASE_DB_URL`, `JWT_SECRET`, etc.
   - *Note: Values containing spaces or `&` must be wrapped in quotes.*
2. Run the application:
   ```bash
   set -a && source .env && set +a && ./mvnw spring-boot:run
   ```
3. Build and Package:
   ```bash
   ./mvnw clean package
   ```
4. Test:
   ```bash
   ./mvnw test
   ```

### 🔵 Web (`05_Development/CareBridgeWebApp`)
1. Install dependencies:
   ```bash
   npm install
   ```
2. Run development server:
   ```bash
   npm run dev
   ```
3. Build for production:
   ```bash
   npm run build
   ```

### 📱 Mobile (`05_Development/CareBridgeMobileApp`)
- Run on Chrome:
  ```bash
  flutter run -d chrome
  ```
- Run on Emulator:
  ```bash
  flutter run
  ```
- Run on Physical Device (with custom API URL):
  ```bash
  flutter run -d <device-id> --dart-define=API_BASE_URL=http://<LAN_IP>:8080
  ```
- Build APK:
  ```bash
  flutter build apk
  ```
- Run Tests:
  ```bash
  flutter test
  ```

---

## 👥 Demo & Test Accounts

Use these pre-configured accounts for testing different roles:

| Role | Email | Password |
| :--- | :--- | :--- |
| **SYSTEM_ADMIN** | `admin@carebridge.dev` | `Test@1234` |
| **MODERATOR** | `moderator@carebridge.dev` | `Test@1234` |
| **CONTENT_ADMIN** | `content@carebridge.dev` | `Test@1234` |
| **EXPERT** | `expert@carebridge.dev` | `Test@1234` |
| **PARTNER** | `partner@carebridge.dev` | `Test@1234` |
| **MOTHER** | `mother@carebridge.dev` | `Test@1234` |
| **FAMILY** | `family@carebridge.dev` | `Test@1234` |

---

## ⚙️ Development Guidelines

1. **Git Dual Remotes**: Ensure safety rules are followed. Always pull from both `github` and `gitlab` remotes before pushing, and never push directly to `dev`.
2. **Database Migrations**: Use Flyway for all schema changes under `05_Development/Database`. Never modify an already-applied migration file.
3. **Architecture Rules**: Follow the Modular Monolith style. Do not introduce microservices, MongoDB, or new heavy infrastructure/dependencies without approval.
