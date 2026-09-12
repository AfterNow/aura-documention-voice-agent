# Aura Voice Document

Working hackathon Android XR voice companion: select one of three products, ask a question, and see the original manual page alongside the conversation. Both spatial panels support movement and resizing.

## Demo
1. Keep Aura connected over USB and the backend terminal running.
2. Select DB-200H, MLG-202DR, or VSX.
3. Tap **Start voice** once and speak naturally. Questions are submitted automatically when you finish speaking. The microphone stays on during answers and between questions. Tap **End voice** to stop listening and playback. Allow microphone permission when prompted.
4. Try “Show me the drain connection diagram” for DB-200H, “Show the component identification drawing” for MLG-202DR, or “Show the cartridge seal instructions” for VSX.
5. Ask follow-up questions, “next page”, or “zoom in”. Speak over an answer to interrupt it. Stop also ends voice interaction. Move and resize panels using the XR system panel controls.
6. For DB-200H, ask “Start the installation checklist review.” This reviews documentation; it does not certify equipment or authorize live repair work.

## Local setup
Requirements: Android SDK 36 with platform-tools, JDK 21 (validated), Node 24 (validated), pnpm, Python with pypdf and pypdfium2.

The three demo PDFs are included in `manuals/`. Generate the search index, page images, and Android assets:

```powershell
python -m pip install pypdf pypdfium2 Pillow
python scripts/prepare_manuals.py --help
python scripts/prepare_manuals.py manuals
cd backend
pnpm install --frozen-lockfile
cd ..
```

Create project-root `.env` from `backend/.env.example`. Either OPENAI_API_KEY or OPENAI_KEY is accepted. Keys stay in the backend; they are never packaged in the APK. Source PDFs are tracked in Git. Generated indexes, page images, and credentials are excluded.

Start the backend in a terminal:

```powershell
./scripts/run-backend.ps1
```

Build and install in another terminal:

```powershell
./scripts/deploy.ps1 -JavaHome $env:JAVA_HOME
```

Set JAVA_HOME to your JDK 21 installation and ANDROID_HOME to your Android SDK directory (or configure sdk.dir in local.properties). Put platform-tools on PATH so adb is available.

The backend binds to loopback port 8787. `adb reverse tcp:8787 tcp:8787` connects the app to it. If USB is reconnected, rerun that command and tap Reconnect in the app. The backend must remain running. Offline manual browsing remains available without voice.

## Architecture and scope
Kotlin / Compose XR renders two spatial panels and original PDFs using Android PdfRenderer. PCM audio and tool events travel over WebSocket through USB forwarding. A Node backend owns OpenAI Realtime credentials, product-scoped lexical retrieval, page images, validated document tools, and a curated DB-200H review. Page display is acknowledged by the client after rendering before the agent reports success.

No camera identification, cloud deployment, authentication, or production repair workflow is included. Voice uses semantic end-of-turn detection while enabled, with acoustic echo cancellation when supported by the device. Switching products, leaving the app, or disconnecting stops the microphone; tap Start voice to resume. Retrieval is a small local index, and model answers still require source review. Manual contents are treated as reference data, not agent instructions.

The pump user alias P500219 maps to supplied manual P5002169 for VSX VSH/VSC/VSCS. Verify the equipment variant before applying variant-specific instructions. The dryer PDF has ManualsLib front matter; physical PDF indexes and printed labels differ and are mapped in the catalog.

## Validation
```powershell
cd backend
node --test --test-isolation=none documents.test.mjs voice.test.mjs
node evaluate-live.mjs
node --env-file=../.env evaluate-continuous.mjs
```

Five deterministic checks cover product isolation, page labels, invalid page/product rejection, and indexed source coverage. A sixth protocol check covers continuous-voice state and interruptions. Six live text-to-spoken-answer cases passed across all three manuals, including diagram display acknowledgments simulated by the evaluator. The continuous-voice evaluation also passed two synthesized spoken questions without manual commits, a second question during the first answer, source page 14, and no further output after stopping. These checks use the running backend/API credits; they do not measure physical microphone echo. The user confirmed spatial panels, movement, and voice on the earlier push-to-talk build. The continuous build is installed for physical testing.

## Source checkpoints
Normal development should use Git commits and pushes. `scripts/github_checkpoint.py` is a narrowly scoped GitHub API checkpoint helper used when this Codex sandbox denied local `.git` writes. It includes the three demo manuals and excludes secrets but does not update local Git metadata. This workstation checkout may therefore lag the remote history; synchronize it before normal Git work, preserving working files.

## Change branches
- `feature/aura-voice-document-name`: app display name and README branding.
- `feature/continuous-voice`: built on the naming branch; automatic voice turns, speech interruptions, and a single start/end toggle.

Continuous voice follows the [OpenAI VAD guide](https://developers.openai.com/api/docs/guides/realtime-vad) and [WebSocket interruption guidance](https://developers.openai.com/api/docs/guides/realtime-conversations). The client reports played audio when interrupted so unheard answer content can be truncated. An additional protocol test covers microphone gating, VAD configuration, interrupted audio, stopping, and restarting. Physical echo rejection depends on the Aura audio route and should be tested in the demo environment.

