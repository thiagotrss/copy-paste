# CopyPaste

A simple, secure online clipboard for sharing text and files via time-limited links.

## Features

- **Paste text or upload files** — share anything with a single link
- **Google OAuth authentication** — only Google accounts can access the app
- **Optional access restriction** — limit a link to specific email addresses; leave empty to allow any authenticated user
- **24-hour TTL** — clips and files are automatically deleted after 24 hours
- **Clean UI** — Bootstrap 5 interface with copy-to-clipboard and file download support

## Tech Stack

- Kotlin + Spring Boot 3.3
- Spring Security OAuth2 (Google)
- Spring Data MongoDB
- Thymeleaf + Bootstrap 5
- Local filesystem for file storage

## Getting Started

### Prerequisites

- JDK 21+
- MongoDB running on `localhost:27017`
- A Google OAuth2 client ([create one here](https://console.cloud.google.com/apis/credentials))
  - Authorized redirect URI: `http://localhost:8090/login/oauth2/code/google`

### Configuration

Set the following environment variables:

```bash
export GOOGLE_CLIENT_ID=your-client-id
export GOOGLE_CLIENT_SECRET=your-client-secret
```

### Run

```bash
./gradlew bootRun
```

App will be available at `http://localhost:8090`.

### Docker

```bash
docker build -t copy-paste .
docker run -p 8090:8090 \
  -e GOOGLE_CLIENT_ID=your-client-id \
  -e GOOGLE_CLIENT_SECRET=your-client-secret \
  -e SPRING_DATA_MONGODB_URI=mongodb://host.docker.internal:27017/copy-paste \
  -v /data/copy-paste/uploads:/var/copy-paste/uploads \
  copy-paste
```

## Configuration Reference

| Property | Default | Description |
|---|---|---|
| `GOOGLE_CLIENT_ID` | — | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | — | Google OAuth2 client secret |
| `SPRING_DATA_MONGODB_URI` | `mongodb://localhost:27017/copy-paste` | MongoDB connection URI |
| `APP_STORAGE_UPLOAD_DIR` | `./uploads` | Directory for uploaded files |
| `APP_CLIP_TTL_HOURS` | `24` | Hours before clips expire |
| `APP_CLEANUP_CRON` | `0 0 * * * *` | Cron expression for cleanup task (every hour) |

## How It Works

1. User logs in with their Google account
2. User pastes text or selects a file, optionally specifying allowed emails
3. A unique 8-character link is generated (e.g., `/c/aBcD3fGh`)
4. The link can be shared — recipients must log in with Google to access it
5. After 24 hours the clip and any associated files are automatically deleted
