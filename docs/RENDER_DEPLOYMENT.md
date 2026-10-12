# Render deployment

The recommended production layout is:

- `campus-day-web`: a Render static site served from the global CDN.
- `campus-day-api`: a Docker-based Spring Boot web service in Singapore.
- MongoDB: the existing external MongoDB Atlas database supplied through
  `MONGODB_URI`.

The root `Dockerfile` builds the backend. `frontend/Dockerfile` is provided for
local or image-based deployments, but `render.yaml` deliberately uses Render's
static-site runtime for the frontend because it is faster and does not consume
free web-service instance hours.

## Before deploying

1. Create a GitHub, GitLab, or Bitbucket repository and push this project.
2. In MongoDB Atlas Network Access, allow connections from Render. For a quick
   trial this is commonly `0.0.0.0/0`; for production, use the narrowest network
   access your Atlas and Render plans support.
3. Keep the MongoDB username/password only in `MONGODB_URI`. Never commit it.

## Create the Blueprint

1. Open Render Dashboard and choose **New > Blueprint**.
2. Select the repository containing `render.yaml`.
3. When prompted, set `MONGODB_URI` to the Atlas SRV connection string.
4. Apply the Blueprint and wait for both services to become live.

The Blueprint passes each generated Render hostname to the other service, so
the React build, CORS, redirects, and OAuth callback base URLs do not depend on
hardcoded `onrender.com` names.

## Optional feature secrets

Add only the features you use in the backend service's Environment page:

- Email: `BREVO_API_KEY`, `BREVO_SENDER_EMAIL`, `MEAL_MANAGER_ADMIN_EMAIL`
- Images: `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`,
  `CLOUDINARY_API_SECRET`
- Taskbot: `GEMINI_API_KEY` and/or `GROQ_API_KEY`
- Google sign-in:
  `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID` and
  `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET`
- Google Calendar: `GOOGLE_CALENDAR_CLIENT_ID`,
  `GOOGLE_CALENDAR_CLIENT_SECRET`
- Current Affairs: `NEWSDATA_API_KEY`, then set
  `CURRENT_AFFAIRS_ENABLED=true` only after rights and source checks pass.

After adding Google credentials, register these generated URLs with Google:

- Sign-in callback:
  `https://<backend-host>/login/oauth2/code/google`
- Calendar callback:
  `https://<backend-host>/api/study/calendar/integrations/google/callback`

If a custom frontend domain is added later, override these backend properties:

- `APP_CORS_ALLOWED_ORIGINS=https://your-domain.example`
- `APP_AUTH_FRONTEND_OAUTH_SUCCESS_URI=https://your-domain.example/auth/callback`
- `APP_AUTH_FRONTEND_OAUTH_FAILURE_URI=https://your-domain.example/signin?oauth=failed`
- `APP_MEAL_MANAGER_APPLICATION_FRONTEND_BASE_URL=https://your-domain.example`

## Performance expectations

The frontend development server becoming ready is not a production performance
test. A Render static site is CDN-served and should be the fast part of this
deployment. The supplied local backend log reports a 25.308 second cold start;
most of the delay occurs after MongoDB connection/index initialization begins.

Render Free web services sleep after inactivity, so the first backend request
can take roughly a minute while the service wakes. Use at least a paid web
service plan for consistently low latency. Keep the backend in Singapore near
the current Singapore Atlas cluster to reduce database round-trip time.
