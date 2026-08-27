# CareBridge Edge Topology

This document is the deployment contract for CareBridge. Environment overrides
may change hostnames, images, capacity and secret sources; they must not redesign
the request path.

## Canonical request paths

Local development exercises the full edge path for both origins:

```text
Browser -> https://portal.dev.<domain> -> Cloudflare -> cloudflared
        -> nginx-edge -> Vite:5173

Web/Mobile -> https://api.dev.<domain>/api/v1/** -> Cloudflare -> cloudflared
           -> nginx-edge -> Spring Boot:8080
```

Production separates static hosting from API ingress:

```text
Web document: Client -> Cloudflare -> GitLab Pages
API traffic: Web/Mobile -> Cloudflare -> cloudflared on AWS EC2
            -> nginx-edge -> Spring Boot:8080
```

GitLab Pages is static hosting operated by GitLab. It is not an origin behind
the AWS tunnel. Only API traffic follows the complete AWS connector path.

## One-time manual Cloudflare setup for local development

Allow about 15–30 minutes for the first setup, excluding Docker image downloads.
These steps modify the owner's Cloudflare account, so an agent must not perform
them without the owner present.

### Is a custom domain required?

- The existing portal URL
  `https://su26-sep490-g79-61e20c.gitlab.io/` does not require a custom domain.
  It can remain the current Pages origin. Making it the final production origin
  would require explicit approval to change the fixed `portal.<domain>` contract
  and Backend CORS to that exact GitLab origin.
- A stable remotely-managed Cloudflare Tunnel public hostname does require a
  domain whose DNS zone is controlled in Cloudflare. The project cannot create
  Tunnel DNS records or project-specific WAF rules under `gitlab.io`, because
  GitLab owns that DNS zone.
- Without a domain, a TryCloudflare Quick Tunnel can generate a random
  `trycloudflare.com` URL for temporary development. It changes across restarts,
  has no uptime guarantee, does not support SSE, and is not the canonical
  topology encoded by this repository.

For the fixed architecture currently approved, a Cloudflare-controlled domain
is required: local development publishes both the local portal and API through
the named tunnel, while production uses a custom portal hostname for GitLab
Pages and a custom API hostname for the AWS tunnel. Using the default GitLab
Pages hostname or a Quick Tunnel is an alternative contract, not a transparent
implementation detail.

### Which Cloudflare account should own the tunnel?

Use the Cloudflare account that controls the chosen domain's DNS zone. Cloudflare
Tunnel DNS records only proxy through the tunnel when the zone and tunnel are
under the appropriate Cloudflare account. The R2 bucket and Tunnel do not
technically have to share an account; their credentials and permissions are
separate.

For CareBridge, prefer the existing project-owned Cloudflare account used for R2
if the team controls it and can add the domain there. This keeps ownership,
billing, access removal, audit and secret rotation in one project account. Do not
create production infrastructure in an unrelated personal account. If the domain
already belongs to another project-controlled Cloudflare account, create the
Tunnel in that account and leave R2 where it is.

### 1. Confirm the domain is ready

1. Sign in at `https://dash.cloudflare.com`.
2. Open **Websites** and select the domain you intend to use.
3. Confirm its status is **Active**. If the domain is absent or pending, stop:
   Tunnel public hostnames cannot be completed yet.
4. Write down the domain only, for example `carebridge.example`. Do not include
   `https://`, a path, or a trailing slash.

This contract uses `portal.dev.<domain>` and `api.dev.<domain>`. These are
multi-level subdomains. Cloudflare may require an Advanced Certificate covering
`*.dev.<domain>`. If the dashboard reports that the hostname is not covered by
an edge certificate, stop and decide whether to obtain that certificate or
renegotiate the development names to one-level alternatives such as
`portal-dev.<domain>` and `api-dev.<domain>`.

### 2. Create the named tunnel and obtain its token

1. In the main Cloudflare Dashboard, open **Networking -> Tunnels**.
2. Select **Create a tunnel**.
3. Enter `carebridge-huy-local` as the tunnel name.
4. Select **Create Tunnel**.
5. On the connector installation page, choose **Docker**.
6. Cloudflare displays a command containing `--token eyJ...`. Copy only the long
   value after `--token`. Do not run Cloudflare's displayed Docker command; this
   repository's Compose service will run the connector.
7. Keep this browser tab open. It can remain waiting for a connector while the
   local files and containers are prepared.

Never paste the token into chat, a screenshot, `.env.edge-local`, or a terminal
command. Anyone holding the token can start a replica of this tunnel.

### 3. Prepare the local hostname and token files

Open macOS Terminal and run:

```bash
cd "/Users/huy/Documents/Đồ án/CareBridge_SEP490_G79"

# The repository setup normally creates this ignored file already.
test -f 05_Development/Deployment/.env.edge-local || \
  cp 05_Development/Deployment/edge-local.env.example \
     05_Development/Deployment/.env.edge-local

open -e 05_Development/Deployment/.env.edge-local
```

In TextEdit, change only the two example hostnames. For the example domain
`carebridge.example`, the beginning of the file must be:

```dotenv
CAREBRIDGE_PORTAL_HOSTNAME=portal.dev.carebridge.example
CAREBRIDGE_API_HOSTNAME=api.dev.carebridge.example
CLOUDFLARE_TUNNEL_TOKEN_FILE=05_Development/Deployment/cloudflare-tunnel-token.secret
```

Rules for both hostname values: no `https://`, no `/`, no port, and no spaces.
Save and close TextEdit.

Create the ignored token file and open it:

```bash
install -m 600 /dev/null \
  05_Development/Deployment/cloudflare-tunnel-token.secret
open -e 05_Development/Deployment/cloudflare-tunnel-token.secret
```

Paste only the `eyJ...` token copied from Cloudflare. The file must not contain
`--token`, quotes, or `CLOUDFLARE_TUNNEL_TOKEN=`. Save and close it. Confirm it is
non-empty without printing the secret:

```bash
test -s 05_Development/Deployment/cloudflare-tunnel-token.secret \
  && echo "Token file is ready"
```

Expected output: `Token file is ready`.

### 4. Start the local origin and connector

1. Open Docker Desktop and wait until it says the engine is running.
2. In Terminal, confirm both Docker and the Compose file work:

```bash
docker info >/dev/null && echo "Docker is ready"

docker compose \
  --env-file 05_Development/Deployment/.env.edge-local \
  -f docker-compose.yml config -q \
  && echo "Compose configuration is valid"
```

3. Start the stack in the background. The first build can take 5–15 minutes:

```bash
docker compose \
  --env-file 05_Development/Deployment/.env.edge-local \
  -f docker-compose.yml up -d --build
```

4. Check service state:

```bash
docker compose \
  --env-file 05_Development/Deployment/.env.edge-local \
  -f docker-compose.yml ps
```

Expected result: `backend`, `web-dev`, and `nginx-edge` become `healthy`, and
`cloudflared` is running. If one service is unhealthy, do not create routes yet.
Read only that service's logs, for example:

```bash
docker compose \
  --env-file 05_Development/Deployment/.env.edge-local \
  -f docker-compose.yml logs --tail=100 backend
```

Replace `backend` with `web-dev`, `nginx-edge`, or `cloudflared` as needed.

### 5. Wait for Cloudflare to see the connector

Return to the open Cloudflare tunnel page. Wait up to two minutes for the
connector to show **Connected** or the tunnel to show **Healthy**, then select
**Continue**. If it stays inactive, run:

```bash
docker compose \
  --env-file 05_Development/Deployment/.env.edge-local \
  -f docker-compose.yml logs --tail=100 cloudflared
```

Do not paste logs containing credentials into a public issue.

### 6. Add the portal route

1. Open **Networking -> Tunnels** and select `carebridge-huy-local`.
2. Open the **Routes** tab.
3. Select **Add route -> Published application**.
4. In **Subdomain**, enter `portal.dev`.
5. In **Domain**, select the same active domain used in `.env.edge-local`.
6. Leave **Path** empty.
7. In **Service URL**, enter exactly `http://nginx-edge:8080`.
8. Select **Save**.

Cloudflare should create the matching DNS record automatically for a domain
using full Cloudflare DNS setup.

### 7. Add the API route

Repeat the previous route flow with these values:

| Field | Value |
|---|---|
| Subdomain | `api.dev` |
| Domain | the same active domain |
| Path | empty |
| Service URL | `http://nginx-edge:8080` |

Both routes intentionally use the same Nginx service. Nginx separates them by
the HTTP Host header. Never use `localhost:8080`: inside the cloudflared
container, `localhost` means cloudflared itself, not Nginx.

### 8. Apply the minimum edge rules

1. In Cloudflare, select the domain, then open **Rules -> Cache Rules**.
2. Create a rule named `CareBridge dev API bypass`.
3. Match hostname equal to `api.dev.<domain>`.
4. Set cache eligibility/action to **Bypass cache**, then deploy the rule.
5. Leave Cloudflare Access disabled for the complete API hostname. Browser-only
   Access login would prevent the native mobile app from calling the API.

Cloudflare DDoS protection and edge analytics apply automatically. Managed WAF,
custom rate limiting, and bot rules can be enabled separately, but must be tested
against authentication, file upload, WebSocket/HMR, and physical mobile traffic
before enforcement.

Cloudflare references:

- https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/
- https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/configure-tunnels/remote-tunnel-permissions/

## Local operations after the one-time setup

`docker compose` (Compose v2 plugin) is canonical. The root stack intentionally
has no `ports` entries.

Useful operations:

```bash
docker compose --env-file 05_Development/Deployment/.env.edge-local \
  -f docker-compose.yml ps

docker compose --env-file 05_Development/Deployment/.env.edge-local \
  -f docker-compose.yml logs -f cloudflared nginx-edge backend web-dev

docker compose --env-file 05_Development/Deployment/.env.edge-local \
  -f docker-compose.yml down
```

## Verification

Replace the example domain before running:

```bash
curl --fail https://portal.dev.<domain>/
curl -i https://api.dev.<domain>/

curl -i -X OPTIONS https://api.dev.<domain>/api/v1/auth/login \
  -H 'Origin: https://portal.dev.<domain>' \
  -H 'Access-Control-Request-Method: POST'

curl --fail https://api.dev.<domain>/api/v1/master-data/provinces
```

The API-origin root must return `404`, proving it cannot serve the portal. A
request to `https://portal.dev.<domain>/api/v1/...` must also return `404`.
An unknown tunnel hostname must not receive application content. The preflight
response must contain the exact portal origin. In the browser,
open the portal hostname, change a React component and confirm HMR reconnects
over `wss`. Direct requests to `http://localhost:5173` and
`http://localhost:8080` must fail while this stack is used.

For a physical Flutter device:

```bash
cd 05_Development/CareBridgeMobileApp
flutter devices
flutter run -d <device-id> \
  --dart-define=API_BASE_URL=https://api.dev.<domain>
```

`API_BASE_URL` contains the origin only; API methods already append `/api/v1`.

## Production contract already encoded

`docker-compose.production.yml` is a deploy-time contract for an AWS EC2 host.
It runs `ai-service`, `backend`, `nginx-edge`, and `cloudflared`; publishes no
application ports and reads the tunnel token from a file-backed Compose secret.
Its `${VAR:?}` guards reject missing image values but cannot prove digest shape.
The canonical production gate must therefore render and validate every image
before `up`:

```bash
cd 05_Development/Deployment
docker compose --env-file .env -f docker-compose.production.yml config -q
docker compose --env-file .env -f docker-compose.production.yml config --images \
  | ./validate-production-images.sh
```

Only a successful validator result permits a production `up`. This is a
deploy-time contract, not an authorization to deploy.

The production Compose file fixes topology, network boundaries and variable
names; it is intentionally not a deploy-ready host sizing policy. Before the
first AWS start, the deployment task must also:

- move Backend and AI credentials from ordinary environment inspection into an
  approved AWS/GitLab secret source and add application-side `_FILE` adapters
  where Compose file secrets are selected;
- set CPU, memory, PID and bounded Docker log policies from measured EC2 sizing;
- ensure the immutable Backend/AI images contain the health-probe tools used by
  their Compose health checks, or replace those checks with an image-independent
  probe contract;
- add host/container monitoring and automatic recovery for dependencies that
  become unhealthy after Compose's one-time startup gates have passed.

The older `docker-compose.staging.yml` still represents the previous direct-port
staging workflow. It is not the production topology and must be migrated or
retired by the future deployment task rather than copied to AWS unchanged.

The future deployment must retain these environment contracts:

```dotenv
VITE_API_URL=https://api.<domain>
CAREBRIDGE_CORS_ALLOWED_ORIGINS=https://portal.<domain>
API_BASE_URL=https://api.<domain>
```

Before production, rotate the currently hard-coded CompreFace credentials,
remove them from tracked configuration, disable destructive Flyway cleaning,
and verify that no production Web/Mobile artifact contains localhost. Database
placement remains Supabase unless the owner separately approves an RDS migration.

## Copyable future deployment prompt

Use this prompt from a clean `HuyND` coding session when ready to deploy:

```text
Deploy CareBridge using 05_Development/Deployment/EDGE_TOPOLOGY.md and the
existing docker-compose.production.yml. Read AGENTS.md and all dual-remote Git
rules first. Use code-review-graph/GitNexus before source edits and run impact
analysis for every application symbol changed.

The fixed production architecture is:
1. portal.<DOMAIN> -> Cloudflare-proxied GitLab Pages for the React static site.
2. Every Web/Mobile API request -> https://api.<DOMAIN>/api/v1/** -> Cloudflare
   WAF/CDN/Tunnel -> cloudflared on AWS -> nginx-edge -> Spring Boot:8080.
3. AWS baseline is one Ubuntu EC2 host running Docker Compose with backend,
   ai-service, nginx-edge and cloudflared. Do not publish 80, 443, 8080 or 8001;
   use outbound Tunnel connectivity. Prefer AWS SSM over public SSH.
4. Keep the existing Supabase database unless I explicitly approve RDS.

Complete, test and document all of the following:
- Provision or guide me through EC2, IAM/SSM, EBS, Security Group, logging,
  bounded Docker logs, measured CPU/memory/PID limits, health monitoring,
  automatic recovery, backups, registry read access and rollback. Stop for every
  external-account, credential, billing or destructive approval.
- Create production Cloudflare named-tunnel/public-hostname configuration,
  WAF/rate-limit rules, `/api/*` cache bypass and secret rotation. Store tokens
  only in AWS/GitLab secret facilities; never in Git or command output.
- Move Backend/AI credentials out of inspectable Compose environment values;
  implement and test file/provider adapters before the first production start.
- Harden production startup: enforce `validate-production-images.sh` in CI and
  before Compose startup, health/readiness checks, no direct origin ports,
  exact forwarded headers and exact credentialed CORS.
- Remove and rotate tracked CompreFace keys. Set production Flyway to
  clean-disabled=true and clean-on-validation-error=false before deployment.
- Update GitLab Pages CI so a protected production job requires
  VITE_API_URL=https://api.<DOMAIN>, fails if the bundle contains localhost,
  publishes the SPA correctly at portal.<DOMAIN>, and configures custom-domain
  DNS verification without pretending Pages is behind AWS Nginx.
- Add portal.<DOMAIN> to Firebase Authorized Domains when federated auth needs
  it. Treat that as a manual external-account step.
- Prepare physical-device Android and iOS builds with
  API_BASE_URL=https://api.<DOMAIN>. Guide me through Android keystore/package
  signing/Firebase SHA values and Apple Team/Bundle ID/provisioning/APNs. Never
  commit signing keys, provisioning profiles or service credentials.
- Verify Web deep links and assets, CORS preflight, auth/refresh, file upload,
  HMR only in dev, API health, AI connectivity, mobile camera/mic/location/
  notification permissions, and confirm the API origin is reachable only
  through Cloudflare Tunnel.

Do not deploy, mutate Cloudflare/AWS/GitLab/Firebase, commit, merge or push until
you show the plan, identify required manual inputs, and receive my explicit
approval. When approved, follow the repository's granular commit and dual-remote
end-day workflow exactly.
```
