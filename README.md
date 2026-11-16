# Digi Verse (v32.0)

**Digi Verse** is an Android virtual Digital Monster raising, training, evolution, and real-time multiplayer tamer RPG built with Kotlin, Jetpack Compose (Material 3), Glance AppWidgets, and Firebase Firestore.

## Key Features
- **10 Rookie Digital Monster Starters with Unique Stats & Abilities**:
  - New and reset tamers choose 3 **Rookie-stage (LVL 1)** starter Digital Monsters from **10 distinct choices** (`Agumon`, `Gabumon`, `Patamon`, `Guilmon`, `Veemon`, `Dorumon`, `Tentomon`, `Renamon`, `Terriermon`, `Gammamon`), each with customized base stats (`ATK`, `DEF`, `SPD`, `WIS`), a Signature Skill, and a unique passive Starter Ability.
- **200 DigiCoins Starting Balance (New Game Only)**:
  - Every brand-new game or full account reset begins with **200 DigiCoins** so new tamers can immediately train, buy rations, or activate 24-Hour Offline Auto-Training.
- **Pausable 24-Hour Offline Auto-Training & Normal Drill Lockout**:
  - Once a monster enters **24-Hour Offline Auto-Training**, normal 10-Coin training drills are locked for that monster. Players can **Pause Offline Training** at any time to freeze the countdown timer and **Restart Offline Training** whenever ready.
- **Automatic Game Launch Update Alerts**:
  - Automatically checks GitHub Releases and repository build metadata on game launch and alerts players with an interactive update dialog, top-bar badge, and download banner when a newer version is available.
- **Animated Digivice Holo-Stage & New Tamer Quick-Start Guide**:
  - Features V-Pet LCD scanline overlays, corner targeting reticles, binary data streams, and an interactive **New Tamer Quick-Start Guide** right on the Command Deck.
- **100 Unique Digital Monsters & Branching Evolution Trees**:
  - Monsters level up through Training Drills (`10 DigiCoins` per drill, `+5 DigiCoins` every 5 minutes, or `100 DigiCoins` for 24-Hour Offline Auto-Training) and unlock branching evolutions starting at **LVL 20+**.
  - **6 Rare Level-100 Mythic Monsters** unlockable individually per monster upon reaching **LVL 100**.
- **10 Timed Expeditions & Ultra Rare Loot Drops**:
  - Send squad monsters on 10 timed expeditions (15m up to 24h).
- **Daily Login Retention Rewards & 4 Live Home Screen Widgets (`2x1`)**:
  - 7-day retention streak rewards and real-time Health, Stamina/Cryo-Rest, Hunger/Quick-Feed, and Evolution Sync widgets.

## Building & Deploying to GitHub
1. **Push from Google AI Studio**: Use the **Push to GitHub** button in the top-right toolbar / Settings menu of Google AI Studio to sync this codebase directly to your GitHub repository.
2. **Automatic GitHub Actions APK Build**: Every push to `main` or `master` runs `.github/workflows/android-build.yml` and publishes `DigiVerse-v32-debug-apk` under the **Actions** tab on GitHub.
3. **Direct APK Download in AI Studio**: Open the **Settings / Export** menu in Google AI Studio to download the compiled APK/AAB or project ZIP directly.
