# Digi Verse (v31.0)

**Digi Verse** is an Android virtual monster raising, training, evolution, and real-time multiplayer tamer RPG built with Kotlin, Jetpack Compose (Material 3), Glance AppWidgets, and Firebase Firestore.

## Key Features
- **Rookie-Only Starter Monster Selection**:
  - New and reset tamers strictly choose 3 **Rookie-stage (LVL 1)** starter monsters; Champion, Ultimate, and Mega stages are locked during starter selection and must be unlocked via Training Drills (LVL 20+).
- **Automatic Game Launch Update Alerts**:
  - Automatically checks GitHub Releases and repository build metadata on game launch and alerts players with an interactive update dialog, top-bar badge, and download banner when a newer version is available.
- **Animated Monster Holo-Stage**:
  - **Sleeping (`Cryo-Rest`)**: Floating stasis animation, pulsing cryo-shield dome, and rising `"Zzz... +STA"` particles.
  - **Eating (`Nutrient Feed`)**: Chomping squash-and-stretch animation, flying Cyber-Ration & Berry projectiles, and satiety heart rings.
  - **Training (`Holo-Drill`)**: High-tempo combat lunges, dual neon plasma shockwaves, orbiting sparks, and stat-boost callouts.
  - **Wild Taming (`Tamming Berry Throw`)**: Parabolic berry throw trajectory, impact capture waves, monster shake reactions, and golden capture lock burst.
- **100 Unique Monsters & Branching Evolution Trees**:
  - Monsters level up exclusively through Training Drills (`10 DigiCoins` per drill, `+5 DigiCoins` every 5 minutes, or `100 DigiCoins` for 24-Hour Offline Auto-Training) and unlock branching evolutions starting at **LVL 20+**.
  - **6 Rare Level-100 Mythic Monsters** unlockable individually per monster upon reaching **LVL 100**.
- **10 Timed Expeditions & Ultra Rare Loot Drops**:
  - Send squad monsters on 10 timed expeditions (15m up to 24h).
  - Only the **24-Hour Expedition (`Digital Abyss Odyssey`)** brings back **100 DigiCoins**; every completed expedition also rolls an **Ultra Rare chance (11%–20%)** to grant **Tamming Berries** or a **random Food Ration**.
- **Daily Login Retention Rewards**:
  - Claim once per calendar day for a 7-day retention streak granting either **10–25 DigiCoins**, **1–2x Random Tamming Berries**, or the **Day 7 Jackpot (+25 DigiCoins & +2x Tamming Berries)**.
- **4 Live Home Screen Widgets (`2x1`)**:
  - Real-time Health, Stamina/Cryo-Rest, Hunger/Quick-Feed, and Evolution Sync widgets.

## Building & Deploying to GitHub
1. **Push from Google AI Studio**: Use the **Push to GitHub** button in the top-right toolbar / Settings menu of Google AI Studio to sync this codebase directly to your GitHub repository.
2. **Automatic GitHub Actions APK Build**: Every push to `main` or `master` runs `.github/workflows/android-build.yml` and publishes `DigiVerse-v31-debug-apk` under the **Actions** tab on GitHub.
3. **Direct APK Download in AI Studio**: Open the **Settings / Export** menu in Google AI Studio to download the compiled APK/AAB or project ZIP directly.
