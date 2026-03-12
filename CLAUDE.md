# Project: Android RPG Game

## Tech Stack
- **Language:** Kotlin
- **Framework:** LibGDX + KTX extensions
- **ECS:** Fleks (entity component system)
- **UI:** Scene2D (MVVM pattern: Views, ViewModels, Widgets)
- **Build:** `./gradlew :core:compileKotlin` to verify compilation
- **Source root:** `core/src/main/kotlin/com/github/jacks/roleplayinggame/`

## Architecture Overview

### UI Pattern (MVVM)
- **Views** (`ui/views/`) extend `Table`, use `setFillParent(true)`, built with KTX Scene2D DSL
- **ViewModels** (`ui/viewmodels/`) extend `PropertyChangeSource`, implement `EventListener`, hold observable properties via `propertyNotify`
- **Widgets** (`ui/widgets/`) are reusable UI components (extend `Table` or `WidgetGroup`)
- Views bind to ViewModel properties via `model.onPropertyChange(ViewModel::prop) { value -> ... }`
- Each View has a DSL factory function (e.g., `fun <S> KWidget<S>.battleView(...)`)

### Event System
- Events fired on `gameStage` (a LibGDX Stage) via `gameStage.fire(SomeEvent())`
- Systems and ViewModels register as `EventListener` on `gameStage`
- Events are synchronous — `fire()` blocks until all listeners return
- `system.enabled = false` only stops `onTick`/`onTickEntity`, NOT event handlers

### Viewports
- `gameStage`: `FitViewport(24f, 13.5f)` — fixed aspect ratio world rendering
- `uiStage`: `ScreenViewport()` — pixel-based, 1 unit = 1 pixel, no scaling

### Skin System (`ui/Skin.kt`)
- `Drawables` enum → atlas keys for texture drawables
- `Labels` enum → label styles (each has `.skinKey` for lookup)
- `Buttons` enum → button styles with up/down/over/disabled states
- Access: `skin[Drawables.LIFE_BAR]`, `skin[Fonts.SMALL]`
- Key drawables for bars: `BAR_GREEN_THICK` (9-patch, good for fill bars), `BAR_GREY_THICK` (9-patch, good for bar backgrounds)
- `MANA_BAR` is only 42x3px, NOT a 9-patch — do not use for resizable bars

### Battle System Flow
```
Player collides with enemy → BattleTransitionStartEvent
  → GameScreen.enterBattleMode(): disable systems, fade in, fire BattleEvent, show BattleView
  → BattleSystem creates lightweight battle enemy (ImageComponent, AnimationComponent, StatComponent, BattleComponent only)
  → Phase loop: PLAYER_TURN → RESOLVING → ENEMY_TURN → RESOLVING → PLAYER_TURN (repeat)
  → BATTLE_END → BattleEndTransitionStartEvent
  → GameScreen.exitBattleMode(): fade in, fire BattleEndEvent, enable systems, show MainGameView
```

### Key Battle Events (in `events/Events.kt`)
| Event | Purpose |
|-------|---------|
| `BattleEvent(enemy)` | Battle starts, enemy entity passed |
| `BattleEndEvent(reason)` | Battle ends (WIN/LOSE/FLEE) |
| `BattlePhaseChangedEvent(phase)` | State machine phase changed |
| `BattleActionSelectedEvent(action)` | Player chose an action (ATTACK/FLEE) |
| `BattleHealthUpdateEvent(playerPct, enemyPct)` | HP percentages updated |
| `BattleLogEvent(message)` | Battle message to display |
| `BattleLogDismissedEvent` | Player clicked to skip enemy turn delay |

### Key Components
- `StatComponent`: `currentHealth`, `maxHealth`, `currentMana`, `maxMana`, `attackDamage`, `defense`, `level`, `experience`, `xpReward`
- `BattleComponent`: `phase` (BattlePhase), `pendingPlayerAction` (BattleAction), `enemyTurnDelayTimer`, `endDelayTimer`
- `BattlePhase` enum: `PLAYER_TURN`, `RESOLVING`, `ENEMY_TURN`, `BATTLE_END`
- `BattleAction` enum: `NONE`, `ATTACK`, `FLEE`
- `AnimationComponent`: has `model` (AnimationModel enum, e.g., `SLIME`, `PLAYER`)

---

# Active Plan: Pokemon-Style Battle UI Overhaul

## Goal
Replace the current BattleView layout with a Pokemon-inspired design: floating stat bars over the battle scene, and a full-width opaque bottom panel that toggles between a 2x2 action grid and a message area.

## Target Layout
```
+-----------------------------------------------+
|                                                |
|              [Enemy: Name Lv.1]  (top-right)   |
|              [HP bar] [MP bar]                 |
|                                                |
|         (battle scene renders here)            |
|                                                |
|  [Player: Name Lv.1]  (lower-left)            |
|  [HP bar] [MP bar]                             |
+-----------------------------------------------+
| BOTTOM PANEL (opaque dark grey, 25% height)    |
|                                                |
| PLAYER_TURN:          | Other phases:          |
| ┌────────┬─────────┐  | Full-width message     |
| │ Attack │ Skills  │  | (click to dismiss)     |
| ├────────┼─────────┤  |                        |
| │ Items  │  Flee   │  |                        |
| └────────┴─────────┘  |                        |
+-----------------------------------------------+
```

## Implementation Steps (do one per chat)

### Step 1: Create `BattleStatBar.kt` (NEW FILE)
**Path:** `core/src/main/kotlin/com/github/jacks/roleplayinggame/ui/widgets/BattleStatBar.kt`
**Package:** `com.github.jacks.roleplayinggame.ui.widgets`

Create a lightweight widget extending `Table` with `KTable`:
- **Row 1:** Name label (`Labels.SMALL`) left-aligned + "Lv.X" label (`Labels.SMALL`) right-aligned
- **Row 2:** HP bar — `Stack` containing `BAR_GREY_THICK` background + `BAR_GREEN_THICK` fill
- **Row 3:** Mana bar — `Stack` containing `BAR_GREY_THICK` background + `BAR_GREEN_THICK` tinted blue (`Color(0.3f, 0.5f, 0.9f, 1f)`)

Public methods:
- `setName(name: String)` — updates name label
- `setLevel(level: Int)` — updates level label text to "Lv.$level"
- `life(percentage: Float, duration: Float = 0.75f)` — animates HP bar via `Actions.scaleTo(clamp(pct, 0, 1), 1f, duration)` (same technique as `CharacterInfo.life()`)
- `mana(percentage: Float, duration: Float = 0.75f)` — same animation for mana bar

Include DSL factory: `fun <S> KWidget<S>.battleStatBar(skin, init): BattleStatBar`

**Note:** The `scaleX` animation scales from origin (0,0) so bars shrink from right-to-left. If origin shifts inside Stack, override `layout()` to call `hpBarFill.setOrigin(0f, 0f)` after super.

**Verify:** `./gradlew :core:compileKotlin`

---

### Step 2: Update `BattleViewModel.kt`
**Path:** `core/src/main/kotlin/com/github/jacks/roleplayinggame/ui/viewmodels/BattleViewModel.kt`

Changes:
1. Change constructor parameter `world: World` → `private val world: World` (currently not stored as property)
2. Add observable properties:
   ```kotlin
   var playerName  by propertyNotify("Player")
   var enemyName   by propertyNotify("Enemy")
   var playerLevel by propertyNotify(1)
   var enemyLevel  by propertyNotify(1)
   var playerMana  by propertyNotify(1f)
   var enemyMana   by propertyNotify(1f)
   ```
3. In `handle()`, expand the `is BattleEvent` branch to populate these from entity data:
   - Enemy: get `AnimationComponent.model.name` for display name (convert from `BLUE_SLIME` → `Blue Slime`), `StatComponent.level`, mana percentage
   - Player: find via `world.family(allOf = arrayOf(PlayerComponent::class))`, get `StatComponent.level`, mana percentage
   - Player name can stay as "Player" for now

**Do NOT** add `onSkills()` or `onItems()` callbacks — those buttons will be disabled.

**Verify:** `./gradlew :core:compileKotlin`

---

### Step 3: Rewrite `BattleView.kt`
**Path:** `core/src/main/kotlin/com/github/jacks/roleplayinggame/ui/views/BattleView.kt`

Complete rewrite. Key structure:

1. **Dark panel background:** Create a `Pixmap(1, 1, RGBA8888)` with color `(0.15, 0.15, 0.15, 1.0)` (fully opaque dark grey), wrap in `TextureRegionDrawable`, cache in skin under key `"battlePanelBgd"`

2. **Table layout** (setFillParent(true)):
   - **Row 1:** Spacer (expandX) + Enemy `BattleStatBar` (right-aligned, width = `Value.percentWidth(0.30f, this)`, padded from top/right)
   - **Row 2:** Inner table with Player `BattleStatBar` (left-aligned, bottom of cell) + spacer. This row gets `expand().fill()` + colspan(2) to absorb vertical space
   - **Row 3:** Bottom panel table with dark grey background, colspan(2), height = `Value.percentHeight(0.25f, this)`. Contains a `Stack` with:
     - `actionTable`: 2x2 grid with `defaults().expand().fill().pad(4f)`:
       - Attack (GREEN_BUTTON_MEDIUM) — calls `model.onAttack()`
       - Skills (BLUE_BUTTON_MEDIUM) — `isDisabled = true`
       - Items (YELLOW_BUTTON_MEDIUM) — `isDisabled = true`
       - Flee (RED_BUTTON_MEDIUM) — calls `model.onFlee()`
     - `messageTable`: Label (`Labels.DEFAULT`, topLeft aligned, wrap=true, padded), with ClickListener calling `model.onLogDismissed()`. Starts `isVisible = false`

3. **Data bindings:**
   - `playerLife` → `playerStatBar.life(pct)`
   - `enemyLife` → `enemyStatBar.life(pct)`
   - `playerMana` → `playerStatBar.mana(pct)`
   - `enemyMana` → `enemyStatBar.mana(pct)`
   - `playerName` → `playerStatBar.updateName(name)`
   - `enemyName` → `enemyStatBar.updateName(name)`
   - `playerLevel` → `playerStatBar.updateLevel(level)`
   - `enemyLevel` → `enemyStatBar.updateLevel(level)`
   - `battleLog` → set `messageLabel.txt` (if not blank)
   - `lootText` → set `messageLabel.txt` (if not blank)
   - `battlePhase` → toggle visibility: PLAYER_TURN shows actionTable, hides messageTable; other phases do the opposite

4. **Remove entirely:** popup system (`popup()`, `resetFadeOutDelay()`, fade actions), `CharacterInfo` references, `Drawables.SLIME`/`PLAYER`/`FRAME_BGD` imports, preferences-based init (`playerHealth`, `playerMana`, `playerExperience`), `Stage` field

**Verify:** `./gradlew :core:compileKotlin`

---

## Important Notes for Implementation
- **Do NOT modify** `GameScreen.kt`, `BattleSystem.kt`, `BattleComponent.kt`, `Events.kt`, `CharacterInfo.kt`, or `Skin.kt`
- The `CharacterInfo` widget is used by `MainGameView` for the overworld HUD — leave it untouched
- `BattleView` is instantiated at `GameScreen.kt:154` as `battleView(BattleViewModel(entityWorld, gameStage)) { isVisible = false }` — no changes needed there
- All battle log messages (damage, victory, flee, level-up) already flow through `BattleLogEvent` → `BattleViewModel.battleLog` — no new events needed
- The phase-based visibility toggle replaces the popup animation system naturally
- Message auto-advance is handled by existing timers: `enemyTurnDelayTimer` (1.5s) in ENEMY_TURN, `endDelayTimer` (1.5s) in BATTLE_END
