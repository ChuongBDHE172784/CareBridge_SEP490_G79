# Firebase setup — UC-144D Direct Consult Chat & Call

Cloud Firestore carries only five-field reconciliation signals. PostgreSQL remains the
durable source of truth for every message and call record.

## Project setup

Use the existing `carebridge-4d86e` Firebase project and enable Cloud Firestore in Native
mode. Backend, Auth, and FCM reuse the same service-account credentials. The legacy
database product is not required for this feature.

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
firebase deploy --only firestore:rules,firestore:indexes --project carebridge-4d86e
```

## Backend

Set the existing service-account value and enable the Firestore publisher:

```dotenv
FIREBASE_CREDENTIALS_BASE64=<base64-service-account-json>
CAREBRIDGE_FIREBASE_FIRESTORE_ENABLED=true
DIRECTCHAT_FIREBASE_EVENT_RETENTION_HOURS=24
```

The hourly backend job deletes expired documents in bounded collection-group batches.
Firestore failures are best-effort and never roll back committed PostgreSQL data.

## Mobile and Web

Mobile uses `google-services.json`, `firebase_auth`, and `cloud_firestore`. Web uses the
public Firebase app values in `CareBridgeWebApp/.env.example`; no database URL is needed.
Both clients listen only to the newest added inbox document and then fetch all missing REST
timeline pages. Firestore payloads are never rendered directly.
