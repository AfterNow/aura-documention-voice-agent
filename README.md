# Aura Voice Document

Working hackathon Android XR voice companion: select one of three products, ask a question, and see the original manual page alongside the conversation. Both spatial panels support movement and resizing.

## Demo
1. Keep Aura connected over USB and the backend terminal running.
2. Select DB-200H, MLG-202DR, or VSX.
3. Tap **Tap to talk**, speak, then tap **Send question**. Allow microphone permission when prompted.
4. Try “Show me the drain connection diagram” for DB-200H, “Show the component identification drawing” for MLG-202DR, or “Show the cartridge seal instructions” for VSX.
5. Ask follow-up questions, “next page”, or “zoom in”. Use Stop to interrupt. Move and resize panels using the XR system panel controls.
6. For DB-200H, ask “Start the installation checklist review.” This reviews documentation; it does not certify equipment or authorize live repair work.

## Local setup
Requirements: Android SDK 36 with platform-tools, JDK 21 (validated), Node 24 (validated), pnpm, Python with pypdf and pypdfium2.

Place the three PDFs in `work/manuals` as `db200h.pdf`, `mlg202dr.pdf`, and `vsx.pdf`. Run:

```powershell
python scripts/prepare_manuals.py --help
python scripts/prepare_manuals.py work/manuals
cd backend
pnpm install --frozen-lockfile
cd ..
```

Create project-root `.env` from `backend/.env.example`. Either OPENAI_API_KEY or OPENAI_KEY is accepted. Keys stay in the backend; they are never packaged in the APK. PDFs, generated indexes, page images, and credentials are excluded from Git.

Start the backend in a terminal:

```powershell
./scripts/run-backend.ps1
```

Build and install in another terminal:

```powershell
./scripts/deploy.ps1 -JavaHome C:\Users\phili\.jdks\jbr-21.0.11
```

The backend binds to loopback port 8787. `adb reverse tcp:8787 tcp:8787` connects the app to it. If USB is reconnected, rerun that command and tap Reconnect in the app. The backend must remain running. Offline manual browsing remains available without voice.

## Architecture and scope
Kotlin / Compose XR renders two spatial panels and original PDFs using Android PdfRenderer. PCM audio and tool events travel over WebSocket through USB forwarding. A Node backend owns OpenAI Realtime credentials, product-scoped lexical retrieval, page images, validated document tools, and a curated DB-200H review. Page display is acknowledged by the client after rendering before the agent reports success.

No camera identification, cloud deployment, authentication, hands-free voice activity detection, or production repair workflow is included. Voice uses push-to-talk. Retrieval is a small local index, and model answers still require source review. Manual contents are treated as reference data, not agent instructions.

The pump user alias P500219 maps to supplied manual P5002169 for VSX VSH/VSC/VSCS. Verify the equipment variant before applying variant-specific instructions. The dryer PDF has ManualsLib front matter; physical PDF indexes and printed labels differ and are mapped in the catalog.

## Validation
```powershell
cd backend
node --test --test-isolation=none documents.test.mjs
node evaluate-live.mjs
```

Five deterministic checks cover product isolation, page labels, invalid page/product rejection, and indexed source coverage. Six live text-to-spoken-answer cases passed across all three manuals, including diagram display acknowledgments simulated by the evaluator. Evaluation writes `work/live-evaluation.json` and uses the running backend/API credits. The user separately confirmed visible spatial panels, panel movement, and working voice on the connected Aura.

## Source checkpoints
Normal development should use Git commits and pushes. `scripts/github_checkpoint.py` is a narrowly scoped GitHub API checkpoint helper used when this Codex sandbox denied local `.git` writes. It excludes manuals and secrets but does not update local Git metadata. This workstation checkout may therefore lag the remote history; synchronize it before normal Git work, preserving working files.

