# Campus Day authentication

Authentication is a separate feature module under `com.spark.campusday.auth`.
It owns credentials, OTP challenges, providers, access tokens and refresh sessions.
The `student` module continues to own the verified academic profile. Local
registration creates that profile only after email verification; a first-time
Google user completes it through the authenticated profile-completion endpoint.

## Required environment variables

Production deployments must set:

- `AUTH_JWT_SECRET`: Base64-encoded secret containing at least 64 random bytes.
- `AUTH_OTP_PEPPER`: independent random server secret used when hashing OTPs.
- `BREVO_API_KEY`: server-side Brevo API key.
- `BREVO_SENDER_EMAIL`: a sender verified in Brevo.
- `BREVO_SENDER_NAME`: optional; defaults to `Campus Day`.
- `FRONTEND_ORIGINS`: comma-separated trusted React origins.

Google Sign-In is enabled when Spring receives:

- `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID`
- `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET`

The Google Console callback URI is:

`{backend-base-url}/login/oauth2/code/google`

The React application starts Google login by navigating to:

`{backend-base-url}/oauth2/authorization/google`

The local React dev server proxies `/api`, `/oauth2` and `/login/oauth2` to the
Spring Boot server. Restart Vite after changing `vite.config.ts`. The checked-in
local OAuth return URL targets `http://localhost:4173/auth/callback`; override
`AUTH_FRONTEND_OAUTH_SUCCESS_URI` and `AUTH_FRONTEND_OAUTH_FAILURE_URI` whenever
the frontend runs on a different origin.

## API flow

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Store a temporary registration and send a Brevo OTP |
| `POST` | `/api/auth/verify-email` | Verify OTP, create student/account, return token pair |
| `POST` | `/api/auth/resend-verification` | Send a replacement verification OTP |
| `POST` | `/api/auth/login` | Email/password login |
| `POST` | `/api/auth/refresh` | Rotate an opaque refresh token and return a new pair |
| `POST` | `/api/auth/logout` | Revoke a refresh token |
| `POST` | `/api/auth/forgot-password` | Send a password-reset OTP without revealing account existence |
| `POST` | `/api/auth/reset-password` | Verify OTP, change password and revoke all refresh sessions |
| `POST` | `/api/auth/oauth/exchange` | Exchange the one-time Google callback code for a token pair |
| `POST` | `/api/auth/complete-profile` | Create the missing student profile for a Google account |
| `GET` | `/api/auth/me` | Return the authenticated account summary |

Access tokens are signed JWTs. Refresh tokens, OTPs and Google exchange codes
are random opaque values; only their SHA-256 hashes are stored. MongoDB TTL
indexes remove expired temporary registrations, OTPs, refresh sessions and
OAuth exchange tickets.

During this backend migration, only the authenticated auth endpoints are
protected. Existing feature APIs remain unchanged until their React clients are
connected to JWT and authorization rules can be enabled feature by feature.
