# ASHIRU CLOUD PC

Android client for submitting approved, short-lived cloud jobs to GitHub Actions.

## Project status

Initial repository setup. The Android app, secure credential storage, job API client, and allow-listed workflow runner will be added in the next implementation commit.

## Security

- Never commit GitHub Personal Access Tokens or other secrets.
- Use a fine-grained token restricted to this repository and store it only in Android Keystore-backed encrypted storage.
- Cloud jobs run on temporary GitHub-hosted runners; this is not a persistent VPS or unrestricted interactive shell.

## Planned capabilities

- Dispatch approved jobs and view workflow status.
- Retrieve job output as a short-lived workflow artifact.
- Provide terminal-style controls for supported commands; key buttons do not create a persistent shell session.
