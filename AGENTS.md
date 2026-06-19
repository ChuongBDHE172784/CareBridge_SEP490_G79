# Repository Guidelines

## Project Structure & Module Organization

This repository follows an SDLC folder layout. Requirements and design live in `01_Requirements/` and `02_Design/`; machine-learning assets in `03_MachineLearning/`; testing artifacts in `05_Testing/`; deployment material in `06_Deployment/`.

Application code is under `04_SourceCode/`:

- `Backend/`: Spring Boot Java 17 service. Main code is in `src/main/java/com/carebridge/backend`, resources and Flyway migrations are in `src/main/resources`, and tests are in `src/test/java`.
- `Frontend/`: Vite React TypeScript app. Use `src/app` for routing/providers/layouts, `src/features/<feature>` for feature modules, `src/shared` for reusable UI, API, hooks, and utilities.
- `MobileApp/`: Flutter app. Use `lib/app`, `lib/core`, `lib/features/<feature>`, `lib/shared`, and `test/`.

## Build, Test, and Development Commands

- Backend: from `04_SourceCode/Backend`, run `.\mvnw.cmd spring-boot:run` to start locally, `.\mvnw.cmd test` for tests, and `.\mvnw.cmd clean package` for a full build.
- Frontend: from `04_SourceCode/Frontend`, run `npm install`, `npm run dev`, `npm run build`, `npm run lint`, and `npm run preview`.
- Mobile: from `04_SourceCode/MobileApp`, run `flutter pub get`, `flutter run`, `flutter test`, and `flutter analyze`.

## Coding Style & Naming Conventions

Follow each stack's conventions. Java uses package names under `com.carebridge.backend`, PascalCase classes, camelCase methods/fields, and uppercase constants. React components use PascalCase `.tsx` files; hooks start with `use`; feature folders stay organized by `components`, `models`, `pages`, and `services`. Dart files use `snake_case.dart`, classes use PascalCase, and private members begin with `_`. Keep indentation consistent with existing files; format Dart with `dart format`.

## Testing Guidelines

Backend tests use Spring Boot test dependencies and should mirror production packages under `src/test/java`; name tests `*Test.java`. Frontend has no test runner configured yet, so run `npm run lint` and `npm run build` before PRs. Mobile tests use `flutter_test`; place tests in `test/` and name files `*_test.dart`.

## Commit & Pull Request Guidelines

Git history uses Conventional Commits with scopes, for example `feat(security): add STORY-002 OTP auth scaffold` and `docs(codex): add STORY-002 plans and handoff`. Use `type(scope): summary`, keep summaries imperative, and include story IDs when relevant.

PRs should include a short description, linked issue/story, affected modules, test evidence, and screenshots for UI changes. Call out database migrations, configuration changes, or new secrets explicitly.

## Security & Configuration Tips

Do not commit credentials, `.env` files, or local IDE secrets. Backend configuration belongs in `application.yaml` with environment-specific values supplied externally. Review Flyway migration names before merging database changes.
