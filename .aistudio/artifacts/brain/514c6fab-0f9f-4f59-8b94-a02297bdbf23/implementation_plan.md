# StoryFlip — Interactive Flipbook Reader & Storyboard Studio

An immersive Android application combining a physical-book-like Flipbook Document Reader with a Cinematic Script-to-Storyboard generator. Users can read documents and scripts with realistic 3D-curl page flips in portrait or landscape double-page spreads, as well as parse scripts into automated cinematic storyboard shot sequences with AI sketches and PDF/image export.

### User Review & Critical Decisions

> [!IMPORTANT]
> Based on your responses, we have aligned the architecture to deliver the full physical Flipbook Reader experience while seamlessly incorporating the Script-to-Storyboard generation capabilities:

- **Confirmed Decision 1 (Core Reader Experience)**: A complete virtual hardcover flipbook with natural 3D page curls, 6 edge/corner grab zones (top, middle, bottom for both left and right edges), button-triggered curls, burgundy bookmark ribbon, portrait 9:16 single page and landscape 16:9 double-page spreads with 3-second orientation recommendation toasts.
- **Confirmed Decision 2 (Visual Style)**: Cinematic film storyboard sketch and concept art aesthetic for storyboard panels, paired with refined paper/leather editorial styling for the reader.
- **Confirmed Decision 3 (Script Breakdown)**: Automated AI scene and shot extraction (using Gemini API) with full manual shot editing, camera angle controls, and director notes.
- **Confirmed Decision 4 (Export & Sharing)**: Storyboard deck export to printable PDF and high-res image saves, alongside document bookmarking and reading state persistence via Room.

---

### 1. Overview & Core Concept

- **What It Does**:
  1. **Document & Script Flipbook Reader**: Renders imported scripts and documents (TXT, PDF, EPUB, DOCX) as a physical hardcover book with embossed typography, satin bookmark ribbon, and realistic page curls across single-page portrait and double-page landscape spreads.
  2. **AI Script-to-Storyboard Engine**: Ingests scripts, extracts scenes, dialogue, and shot descriptions, and generates a sequence of cinematic storyboard panels with shot framing, camera directions, and visual sketches.
  3. **Storyboard Flipbook & Shot Deck**: View storyboards either in a director grid or flip through them like a real physical animator's sketchbook.
  4. **Audio & TTS Reading Player**: Floating translucent pill player supporting playback, line highlighting, and speed adjustments.
  5. **Shared Reader Tools**: Long-press (500ms) glass toolbar for font selection (Literata, Merriweather, Georgia, Atkinson Hyperlegible), text size (14–32sp), themes (Paper, Sepia, Midnight), and zoom/pan.

- **Target Audience / Persona**: Filmmakers, screenwriters, animators, creative directors, and avid digital book readers who want tactile, physical-book-like engagement.
- **Key Value**: Bridges creative script writing with visual pre-visualization in a tactile, distraction-free reading and flipping environment.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Library & Script Ingestion Flow**:
   - User opens the app into the Library showing imported books, scripts, and recent storyboard projects with reading progress bars.
   - User taps "+ Import / New Script" and chooses a file or pastes raw script text.
   - Parsing screen displays document type detection, stage indicators, and parsing progress.
2. **Hardcover Presentation & Opening Flow**:
   - Once parsed, the book presents as a closed deep-navy textured hardcover in portrait (9:16) with embossed gold typography.
   - Tapping the cover triggers a realistic opening animation into single-page reading mode (Page 1) and displays a 3-second recommendation banner: *"Best viewing angle: 16:9 (Landscape) — Switch to read two pages side by side."*
   - Tapping "Switch to 16:9" or physically rotating the device smoothly transitions into a two-page spread (Spread 0: Page 1 Left, Page 2 Right).
3. **Natural Page-Flipping Flow**:
   - Swiping or dragging from any of the 6 grab zones (top corner, middle edge, bottom corner on either side) renders real-time page curl geometry with dynamic shadow gradients and lighting.
   - Navigating via Previous/Next buttons initiates the exact same physics-driven page-flip animation.
4. **Storyboard Generation & Director Deck Flow**:
   - In the script reader, tapping "Generate Storyboard" sends script scenes to the AI breakdown engine.
   - The user inspects generated shot panels (Scene, Shot #, Camera Angle, Action, Dialogue, Visual Sketch Prompt) and can tweak prompts or regenerate images.
   - User can switch between Storyboard Grid View and Flipbook View, or tap "Export Storyboard" (PDF Deck or Image Archive).
5. **Reader Customization & Audio Flow**:
   - Long-pressing anywhere on the readable page for 500ms summons the floating glass control panel (Font, Size, Theme, Speed, Settings).
   - Tapping the floating audio pill player starts text-to-speech playback with karaoke sentence highlighting and animated audio frequency bars.

#### Visual Identity & Theme
- **Aesthetic Direction**: Refined editorial luxury meets vintage cinema pre-production studio. Deep navy leather, gold foil foil-pressed type, cream deckle-edge paper, and translucent frosted glass.
- **Color Palette & Themes**:
  - *Hardcover / App Chrome*: Deep Navy (`#0E1626`), Midnight Slate (`#161F30`), Embossed Gold (`#D4AF37`, `#F3E5AB`).
  - *Bookmark Ribbon*: Satin Burgundy (`#800020`, `#A31D36`).
  - *Paper Theme*: Warm Off-White (`#FBF8F1`), Charcoal Ink (`#2B2825`), Spine Shadow (`#E2DDD2`).
  - *Sepia Theme*: Warm Vintage Beige (`#F4ECD8`), Soft Bistre Brown (`#433022`), Amber Spine (`#DFD4BD`).
  - *Midnight Theme*: Dark Obsidian (`#18191D`), Off-White Silver (`#D6D8E0`), Charcoal Spine (`#101114`).
- **Typography**:
  - Display: Serif Book / Literata & Georgia styling for classical book feel.
  - Controls: Modern M3 sans-serif for crystal-clear readability on small screens.
- **Component Styling & Layout**:
  - Full edge-to-edge support with `WindowInsets.safeDrawing` protection.
  - Floating pill player with blurred glass backdrop and smooth spring animations.
  - 48dp minimum touch targets for all buttons and interactive controls.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Custom Vector Page-Curl Engine vs External Library**:
  - *Chosen Approach*: Implement a custom Compose `DrawScope` canvas page-curl renderer using mesh path clipping, diagonal fold geometry, and moving linear/radial shadow gradients.
  - *Why*: Guarantees rock-solid stability in modern Jetpack Compose without relying on obsolete or incompatible third-party view wrappers, enabling seamless 60fps performance across both portrait single-page and landscape double-page spreads.
- **Decision 2: Dual Viewing Modes (Document Reader & Storyboard Mode)**:
  - *Chosen Approach*: Unified reading state where scripts can be read in classic book mode or transformed into sequential visual storyboard cards that can also be flipped through.
  - *Why*: Satisfies both the user's primary prompt ("script to storyboard sequence") and specific clarification ("Interactive Flipbook Document Reader").
- **Decision 3: Local Persistence via Room**:
  - *Chosen Approach*: Room Database for storing document records, reading positions, bookmarks, script scenes, and generated storyboard frames.
  - *Why*: Keeps all documents, progress, and user creative work instantly available offline and fast to restore upon app reopening.
- **Decision 4: Direct REST Gemini Integration for AI Scene Extraction & Prompts**:
  - *Chosen Approach*: Direct Gemini API client (`BuildConfig.GEMINI_API_KEY`) for automated scene breakdown, shot planning, and sketch prompt crafting.
  - *Why*: Rapid, direct response with no heavy server dependencies, adhering to the project's server-side key configuration.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

#### System Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                          MainActivity (Compose)                        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
       ┌────────────────────────────┼───────────────────────────┐
       ▼                            ▼                           ▼
┌──────────────┐          ┌────────────────────┐     ┌─────────────────────┐
│ LibraryScreen│          │ FlipbookReaderView │     │StoryboardStudioView │
│ (Documents,  │          │ (Portrait Single / │     │(Shot Grid, Deck,    │
│  Recent, New)│          │  Landscape Double) │     │ AI Generator, Export│
└──────┬───────┘          └─────────┬──────────┘     └──────────┬──────────┘
       │                            │                           │
       ▼                            ▼                           ▼
┌──────────────┐          ┌────────────────────┐     ┌─────────────────────┐
│ImportManager │          │  PageFlipEngine    │     │StoryboardService    │
│(TXT/PDF/EPUB)│          │ (6 Grab Zones,     │     │(Gemini API Script   │
│              │          │  Physics & Curl)   │     │ Analysis & Prompts) │
└──────┬───────┘          └─────────┬──────────┘     └──────────┬──────────┘
       │                            │                           │
       └────────────────────────────┼───────────────────────────┘
                                    │
                                    ▼
                      ┌───────────────────────────┐
                      │    ReaderViewModel &      │
                      │    Room Local Database    │
                      │ (Documents, Pages, Shots, │
                      │  Bookmarks, Preferences)  │
                      └───────────────────────────┘
```

#### Core Data Entities
1. **DocumentEntity**: `id`, `title`, `sourceType` (TXT, PDF, EPUB, SCRIPT), `content`, `pageCount`, `lastReadPage`, `bookmarkPage`, `lastOpenedAt`.
2. **StoryboardSceneEntity**: `id`, `documentId`, `sceneNumber`, `sceneHeading`, `location`, `timeOfDay`, `summary`.
3. **StoryboardShotEntity**: `id`, `sceneId`, `shotNumber`, `shotType` (WIDE, MEDIUM, CLOSE_UP, OVER_SHOULDER), `cameraMovement`, `visualDescription`, `dialogue`, `sketchStyle`, `imageUrl`, `sketchSeed`.
4. **ReaderPreferences**: `fontFamily`, `fontSize` (14-32), `theme` (PAPER, SEPIA, MIDNIGHT), `readingSpeed` (0.5x - 2.0x), `turnSoundEnabled`.

#### Interactive Component & State Mapping
- **Orientation Coordinator**: Detects configuration changes or banner actions ("Switch to 16:9" / "Switch to 9:16"), debouncing layout transitions and preserving the exact active page index.
- **PageFlipEngine**: Listens to pointer input in 6 edge/corner zones (`PointerInputScope`), tracks drag distance and angle, and renders curl physics across the center spine. Programmatic next/previous button taps route through the identical animation engine.
- **Audio TTS Coordinator**: Uses Android `TextToSpeech` engine, tracking spoken sentence offsets, driving the animated equalizer bars in the floating player pill, and synchronizing with the active page.
- **Storyboard PDF/Image Exporter**: Uses Android native `PdfDocument` and `Bitmap` canvas utilities to compile high-resolution contact sheets and storyboard decks ready to share or save.

---

### Verification & Testing Plan

1. **Orientation & Layout Parity**:
   - Verify closed hardcover portrait opening -> Page 1 portrait single view with 3-second landscape recommendation.
   - Tap "Switch to 16:9" -> transitions to two-page spread (Page 1 Left, Page 2 Right).
   - Tap left page in landscape -> shows 3-second portrait recommendation -> switches back to Page 1 focus without resetting.
2. **Page-Turn Physics**:
   - Verify all 6 grab zones (top-left, middle-left, bottom-left, top-right, middle-right, bottom-right) trigger intuitive diagonal and lateral curling.
   - Verify previous and next arrow buttons execute the curl animation and cleanly advance/reverse spreads.
3. **Storyboard & AI Breakdown**:
   - Upload sample script -> AI analyzes scenes, dialogues, and shots -> displays cinematic storyboard frames.
   - Verify manual editing of shot notes, camera angles, and visual prompts.
4. **Audio & Controls**:
   - Verify 500ms long press displays the glass control panel with active font, size slider (14–32), theme selector, and speed chips.
   - Verify TTS play/pause and progress synchronization.
5. **Compilation & Build**:
   - Run `compile_applet` to confirm clean Kotlin compilation and zero warnings.
