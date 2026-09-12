# AfterNow Manuals for Aura

Hackathon Android XR voice assistant for three hardware manuals. Kotlin + Jetpack Compose client, local Node backend, OpenAI Realtime voice, and source-linked document navigation.

## Scope
- Select MLG-202DR, DB-200H, or Bell & Gossett VSX (demo alias P500219).
- Ask technical questions grounded in the active manual.
- Agent opens relevant original PDF pages and drawings.
- Voice navigation, interruption, and a curated guided walkthrough.

The supplied pump document is P5002169, covering VSX VSH/VSC/VSCS variants. Product-specific configuration must be verified before applying variant-specific instructions.

## Development
Android SDK 36, JDK 17+, Node 22+, Python with pypdf and pypdfium2.
Documentation assets and API credentials are local and excluded from Git.
Setup and demo instructions will be added as milestones are verified.

## Architecture
Android PCM audio and UI events travel over WebSocket to a loopback backend through `adb reverse tcp:8787 tcp:8787`. The backend owns OpenAI credentials, document retrieval, tool validation, and product context. Original PDFs render on-device. The backend does not execute instructions found in manuals.
