# Firebase setup — UC-144D Direct Consult Chat & Call

Cloud Firestore carries only five-field reconciliation signals. PostgreSQL remains the
durable source of truth for every message and call record.

## Project setup

Use Firebase project `project-d04b488f-17fb-4ae5-b64` with Cloud Firestore in Native
mode. Android (`com.carebridge.app`), iOS (`com.carebridge.g79.mobile`), Web, backend
Auth, and FCM must all target this project. The legacy database product is not required.

## Rules and indexes

`firestore.rules` permits an authenticated user to read only documents under
`userConversationEvents/{theirUid}/events/{eventId}` and denies every client write.
`firestore.indexes.json` enables the `occurredAt` collection-group cleanup query and the
per-inbox descending listener query.

Verify locally:

```bash
cd 05_Development/Firebase
firebase emulators:exec --only firestore,auth \
  "cd rules-test && npm install && npm test" --project demo-carebridge
```

Deploy only after review (never from an automated coding session):

```bash
firebase login
firebase deploy --only firestore:rules,firestore:indexes \
  --project project-d04b488f-17fb-4ae5-b64
```

## Backend

Prefer keyless Application Default Credentials because this project's organization policy
blocks service-account JSON keys. The local ADC principal must have
`roles/iam.serviceAccountTokenCreator` on the managed Firebase service account:

```dotenv
FIREBASE_CREDENTIALS_BASE64=
FIREBASE_USE_APPLICATION_DEFAULT_CREDENTIALS=true
FIREBASE_PROJECT_ID=project-d04b488f-17fb-4ae5-b64
FIREBASE_SERVICE_ACCOUNT_ID=firebase-adminsdk-fbsvc@project-d04b488f-17fb-4ae5-b64.iam.gserviceaccount.com
CAREBRIDGE_FCM_ENABLED=true
CAREBRIDGE_FIREBASE_FIRESTORE_ENABLED=true
DIRECTCHAT_FIREBASE_EVENT_RETENTION_HOURS=24
```

For local development, run `gcloud auth application-default login` before starting the
backend. Hosted environments should attach a service account through workload identity/ADC.

The hourly backend job deletes expired documents in bounded collection-group batches.
Firestore failures are best-effort and never roll back committed PostgreSQL data.

## Mobile and Web

Mobile uses `google-services.json`, `firebase_auth`, and `cloud_firestore`. Web uses the
public Firebase app values in `CareBridgeWebApp/.env.example`; no database URL is needed.
Both clients listen only to the newest added inbox document and then fetch all missing REST
timeline pages. Firestore payloads are never rendered directly.
