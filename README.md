# ASHIRU CLOUD PC

An Android client that dispatches approved jobs to temporary GitHub-hosted runners.

## Current implementation

- Native Kotlin Android app (min SDK 26 / Android 8.0+).
- GitHub Actions workflow dispatch for cloud jobs.
- Encrypted GitHub token storage using Android Keystore AES-GCM.
- System information job.
- Allow-listed terminal commands (non-interactive).
- Python task execution with a 5-minute process timeout.
- APK build workflow using Java 17 and Gradle 8.9.
- Job status links and workflow artifact access through GitHub Actions.

## Important architecture limits

This is a **temporary cloud runner**, not a permanent VPS, desktop streaming service, or always-on computer. A GitHub-hosted runner starts for a workflow, executes the job, uploads its artifact, and is then discarded. There is no live shell session between Android and the runner. Ctrl/Alt and navigation buttons are UI shortcuts; they cannot interrupt or control a job that is already running.

Runner CPU, memory, runtime, concurrency, and usage are controlled by GitHub and the account's current plan. A GPU is not guaranteed. Do not use this as a place to host a 24/7 server.

## Connect the Android app

1. Open GitHub Settings → Developer settings → Personal access tokens → Fine-grained tokens.
2. Create a token restricted to **only** `hamitbull/ASHIRU-CLOUD-PC`.
3. Grant repository permissions:
   - **Actions: Read and write**
   - **Contents: Read-only**
   - Metadata is granted automatically as read-only.
4. Set an expiry date. Do not share the token with anyone.
5. Install the APK and save the token in the app. It is encrypted with Android Keystore on the device; it is not written into this repository.

Never paste a token into source code, README files, GitHub commits, or chat messages.

## Build the Android APK

The `Build ASHIRU CLOUD PC APK` workflow runs on pushes to `main` and can also be started manually from the Actions tab.

It runs:

```bash
gradle assembleDebug
```

The APK is uploaded as the workflow artifact `ASHIRU-CLOUD-PC-debug-apk`. Open the successful workflow run → Artifacts to download it. The debug APK is for testing, not a signed Play Store release.

## Cloud job types

- `system_info`: reports runner OS, CPU identifier, Python version, UTC time, and working directory.
- `terminal`: only accepts exact allow-listed commands: `pwd`, `ls`, `ls -la`, `python3 --version`, `uname -a`, `df -h`, `free -h`, `whoami`, `date -u`.
- `python`: executes user-provided Python source in a temporary job, limited to 12,000 characters and 5 minutes. Treat code as powerful: it can read/write the checked-out workspace and use the runner's network. No application secrets are intentionally passed to the job.
- `build_apk`: dispatches the dedicated Android build workflow.

## Repository security

The cloud workflow grants only `contents: read`. Do not add deployment credentials or unrelated secrets to the workflow. Review any future workflow changes carefully before use.
