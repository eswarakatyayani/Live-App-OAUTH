# mongo-oauth-demo

A Java **Spring Boot** service that:

1. Issues an **OAuth2 access token** using the `client_credentials` grant (you control the **client id** & **client secret**).
2. Exposes a **read-only, JWT-protected** endpoint that returns sample data from **MongoDB Atlas** (`sample_mflix.movies`).
3. Deploys to **Render** (PaaS) via Docker.

Once deployed you get a public base URL. From *any* laptop you call the token endpoint with your client id/secret, then call the data endpoint with the returned access token.

```
client_id + client_secret  ──▶  POST /oauth2/token  ──▶  access_token
access_token (Bearer)       ──▶  GET  /api/movies    ──▶  movie data
```

---

## Architecture

| Concern | Implementation |
|---|---|
| Token issuance | Spring Authorization Server — `POST /oauth2/token`, `client_credentials` grant |
| API protection | Spring Resource Server — validates the signed JWT |
| Data | Spring Data MongoDB → Atlas `sample_mflix.movies` (read-only) |
| Hosting | Render web service (Docker) |

Tokens are RS256 JWTs signed by a key generated at startup (valid 1 hour). The data endpoint (`/api/**`) requires a valid Bearer token; `/` and `/health` are public.

---

## Project layout

```
mongo-oauth-demo/
├── pom.xml
├── Dockerfile
├── render.yaml
├── src/main/java/com/chubb/demo/
│   ├── DemoApplication.java
│   ├── config/AuthorizationServerConfig.java   # token endpoint + registered client
│   ├── config/ResourceServerConfig.java        # protects /api/**
│   ├── controller/HomeController.java           # /, /health (public)
│   ├── controller/MovieController.java          # /api/movies (protected)
│   ├── model/Movie.java
│   └── repository/MovieRepository.java
└── src/main/resources/application.yml
```

---

## Step 1 — MongoDB Atlas (free tier)

1. Create a free account at <https://www.mongodb.com/cloud/atlas> and create an **M0 (free)** cluster.
2. **Load sample data:** cluster → **`...`** → **Load Sample Dataset**. This creates `sample_mflix` (with the `movies` collection).
3. **Database user:** Database Access → Add New Database User (username + password). Remember these.
4. **Network access:** Network Access → Add IP Address → **Allow access from anywhere** (`0.0.0.0/0`) so Render can connect.
5. **Connection string:** Cluster → **Connect** → **Drivers** → copy the SRV string. It looks like:

   ```
   mongodb+srv://<user>:<password>@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
   ```

   Replace `<user>` / `<password>` with your DB user's credentials. This is your `MONGODB_URI`.

---

## Step 2 — Run locally (optional, to verify before deploying)

Requires JDK 17+ and Maven (or use the Docker build).

```bash
# from the project root
export MONGODB_URI="mongodb+srv://<user>:<password>@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority"
export OAUTH_CLIENT_ID="demo-client"
export OAUTH_CLIENT_SECRET="demo-secret"

mvn spring-boot:run
```

On Windows PowerShell:

```powershell
$env:MONGODB_URI="mongodb+srv://<user>:<password>@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority"
$env:OAUTH_CLIENT_ID="demo-client"
$env:OAUTH_CLIENT_SECRET="demo-secret"
mvn spring-boot:run
```

The app starts on <http://localhost:8080>. Test it (see **Step 4**, replacing the base URL with `http://localhost:8080`).

---

## Step 3 — Deploy to Render

1. Push this project to a GitHub repo.
2. Go to <https://dashboard.render.com> → **New** → **Web Service** → connect the repo.
3. Render detects the `Dockerfile`. Choose the **Free** plan.
4. Add **Environment Variables**:

   | Key | Value |
   |---|---|
   | `MONGODB_URI` | your Atlas SRV string from Step 1 |
   | `MONGODB_DATABASE` | `sample_mflix` |
   | `OAUTH_CLIENT_ID` | choose one, e.g. `demo-client` |
   | `OAUTH_CLIENT_SECRET` | choose a strong secret |

   > A `render.yaml` is included if you prefer **Blueprint** deploys (**New → Blueprint**). You still set the secret values in the dashboard.

5. Deploy. When it's live you get a URL like `https://mongo-oauth-demo.onrender.com`.

> **Free tier note:** the service sleeps after inactivity, so the first request after idle may take ~30–50s to wake.

---

## Step 4 — Use the API from any laptop

Set your base URL (Render URL or `http://localhost:8080`).

### 4a. Get an access token

```bash
curl -X POST "https://mongo-oauth-demo.onrender.com/oauth2/token" \
  -u "demo-client:demo-secret" \
  -d "grant_type=client_credentials" \
  -d "scope=data.read"
```

Response:

```json
{
  "access_token": "eyJraWQiOi...<JWT>...",
  "token_type": "Bearer",
  "expires_in": 3599,
  "scope": "data.read"
}
```

> `-u client:secret` sends HTTP Basic auth. You can instead pass `-d "client_id=demo-client" -d "client_secret=demo-secret"`.

### 4b. Call the protected data endpoint

```bash
curl "https://mongo-oauth-demo.onrender.com/api/movies?limit=5" \
  -H "Authorization: Bearer <PASTE_ACCESS_TOKEN_HERE>"
```

Returns movie documents from `sample_mflix.movies`.

### PowerShell one-liner (token → data)

```powershell
$base = "https://mongo-oauth-demo.onrender.com"
$token = (Invoke-RestMethod -Method Post -Uri "$base/oauth2/token" `
  -Headers @{ Authorization = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("demo-client:demo-secret")) } `
  -Body @{ grant_type = "client_credentials"; scope = "data.read" }).access_token

Invoke-RestMethod -Uri "$base/api/movies?limit=5" -Headers @{ Authorization = "Bearer $token" }
```

---

## Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET`  | `/` | public | Service info |
| `GET`  | `/health` | public | Health check |
| `POST` | `/oauth2/token` | client id/secret | Issues access token (`client_credentials`) |
| `GET`  | `/api/movies?limit=N` | Bearer JWT | Sample movie data (N = 1–100, default 10) |

---

## Building behind a corporate network (TLS interception)

If `mvn` fails with `PKIX path building failed ... unable to find valid certification path`,
your network intercepts TLS and Java doesn't trust the proxy's certificate. On a
corporate-managed Windows machine that CA is in the Windows certificate store, so tell
Java to use it:

```powershell
$env:MAVEN_OPTS="-Djavax.net.ssl.trustStoreType=Windows-ROOT"
mvn clean package -DskipTests
```

(This only affects local builds. Render builds in the cloud and is unaffected.)

---

## Notes & next steps

- **Without a valid token**, `/api/movies` returns `401 Unauthorized` — that's the OAuth2 protection working.
- The signing key is in-memory, so existing tokens become invalid after a restart/redeploy. For stable keys, load an RSA keypair from config/secret instead of generating one in `AuthorizationServerConfig`.
- Clients/tokens are in-memory. To support many clients or persistent tokens, back the authorization server with a database (`JdbcRegisteredClientRepository`).
- To enforce the scope on the endpoint, add `.requestMatchers("/api/**").hasAuthority("SCOPE_data.read")` in `ResourceServerConfig`.
