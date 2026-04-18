# 🔱 BUGLIST — MASTER REDESIGN PROMPT
> Gib Claude diese gesamte Datei. Er liest sie, führt alle Skills aus und setzt alles automatisch um.

---

## 🤖 ANWEISUNG AN CLAUDE

Du bist ein Senior Android Engineer & UI/UX Designer.
Du hast folgende MCP-Skills installiert und **MUSST** sie alle aktiv nutzen:

- `android-ninja` (drjacky) → Android-spezifischer Code, Jetpack Compose, Gradle
- `agent-skills` (callstackincubator) → Agentenhafte Aufgabenplanung & Ausführung
- `expo/skills` → Cross-Platform Komponentenlogik als Referenz
- `react-native-skills` (gigs-slc) → Mobile UX-Patterns, Touch-Gesten, Animationen
- `ui-ux-pro-max-skill` (nextlevelbuilder) → **Vollständiges Design-System** — nutze diesen Skill für JEDE UI-Entscheidung

**WICHTIG:** Bevor du irgendetwas schreibst, ruf JEDEN Skill einmal auf und lies seine Ausgabe vollständig. Dann starte die Implementierung. Arbeite vollständig automatisch durch — frag nicht nach, entscheide selbst.

---

## 📋 PROJEKTÜBERSICHT

**App-Name:** BugList  
**Typ:** Native Android App (Kotlin + Jetpack Compose)  
**Zweck:** Offline-First verschlüsselter Schuldentracker  
**Stil:** Gangster-Rap / Streetstyle Dark Mode — KEIN Light Mode  
**Aktuelle Version:** 1.6.8  
**Zielversion nach Redesign:** 2.0.0

---

## 🎨 AUFGABE 1 — VOLLSTÄNDIGES DESIGN-SYSTEM (ui-ux-pro-max-skill)

Nutze `ui-ux-pro-max-skill` und erstelle ein komplettes Design-Token-System als Kotlin-Datei:

### Datei: `ui/theme/BugListTheme.kt`

```kotlin
// Folgende Token MÜSSEN enthalten sein:

// FARBEN
val Gold = Color(0xFFFFD700)           // Primär-Akzent
val GoldDim = Color(0xFFB8960C)        // Gedrückter Zustand
val GoldGlow = Color(0x33FFD700)       // Glow-Effekt (20% Alpha)
val DebtGreen = Color(0xFF00E676)      // Person schuldet MIR
val DebtRed = Color(0xFFFF1744)        // ICH schulde
val DebtGreenDim = Color(0xFF00C853)
val DebtRedDim = Color(0xFFD50000)
val SurfaceDark = Color(0xFF0A0A0A)    // Hintergrund
val SurfaceCard = Color(0xFF141414)    // Karten-Hintergrund
val SurfaceElevated = Color(0xFF1C1C1C)// Erhöhte Flächen
val SurfaceOverlay = Color(0xFF242424) // Dialoge / Sheets
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFAAAAAA)
val TextMuted = Color(0xFF666666)
val StatusOpen = Color(0xFFFFD700)
val StatusPartial = Color(0xFFFF9800)
val StatusPaid = Color(0xFF00E676)
val StatusCancelled = Color(0xFF666666)
val BorderSubtle = Color(0xFF2A2A2A)
val BorderGold = Color(0x66FFD700)     // Gold-Border (40% Alpha)

// TYPOGRAFIE — alle drei Fonts pflichtmäßig einbinden
// Oswald: alle Headlines (H1–H4)
// Roboto Condensed: Body-Text, Labels, Descriptions
// Bebas Neue: Alle Geldbeträge (AmountText)

// ABSTÄNDE — 4er-Raster
val SpaceXS = 4.dp
val SpaceSM = 8.dp
val SpaceMD = 12.dp
val SpaceLG = 16.dp
val SpaceXL = 20.dp
val Space2XL = 24.dp
val Space3XL = 32.dp
val Space4XL = 48.dp

// RADIEN
val RadiusSM = 6.dp
val RadiusMD = 10.dp
val RadiusLG = 14.dp
val RadiusXL = 20.dp
val RadiusFull = 50.dp   // Pills / Chips

// SCHATTEN / ELEVATION — komplett als BoxShadow-Modifier
// GoldShadow: für FAB, aktive Elemente
// CardShadow: für Schulden-Karten
```

---

## 📱 AUFGABE 2 — ALLE SCREENS KOMPLETT NEU DESIGNEN

Nutze `ui-ux-pro-max-skill` + `android-ninja` für JEDEN Screen.  
Jeder Screen muss die neue Design-Sprache vollständig umsetzen.

---

### SCREEN 1: `BiometricLockScreen.kt`

**Konzept:** Ganzer schwarzer Screen. Mittig das App-Logo „BUGLIST" in Oswald ExtraBold, darunter ein animierter Fingerabdruck-Icon der in Gold pulsiert. Kein weiterer Text außer „ENTSPERREN".

```
Design-Anforderungen:
- Hintergrund: SurfaceDark (#0A0A0A)
- Logo "BUGLIST": Oswald ExtraBold, 48sp, Gold, mit 2px Gold Letter-Spacing
- Unter Logo: dünne Gold-Linie (1dp, 60% Breite, zentriert)
- Fingerabdruck-Icon: 80dp, Gold, animierte Pulse-Animation (scale 1.0→1.08, 1.5s Loop)
- Fingerabdruck umgeben von Gold-Ring mit Glow-Effekt (GoldGlow)
- "ENTSPERREN" Button: OutlinedButton, BorderGold, TextPrimary, Roboto Condensed 14sp
- Fehlerzustand: Icon wird kurz Rot (#FF1744), Shake-Animation (horizontal, 3× 6dp)
- AnimatedVisibility für Fehlermeldungstext
- Unten: App-Version in TextMuted, Roboto Condensed 11sp
```

---

### SCREEN 2: `DashboardScreen.kt` (Crew-Liste)

**Konzept:** Komplett neue Top-Bar + Crew-Karten mit premium Street-Feeling.

```
TopBar:
- Hintergrund: SurfaceDark
- Links: "BUGLIST" in Oswald Bold 22sp Gold
- Rechts: Statistik-Icon + Settings-Icon, beide Gold
- Unter TopBar: dünne GoldDim Trennlinie 0.5dp

Gesamt-Bilanz-Banner (unter TopBar):
- Hintergrund: SurfaceCard mit Gold-Gradient-Border (oben 1dp Gold→transparent)
- "NETTO-BILANZ" Label: Roboto Condensed 11sp TextMuted, ALL CAPS
- Betrag: Bebas Neue 42sp, Grün wenn positiv / Rot wenn negativ
- Drei Chips nebeneinander: "SCHULDET MIR" (Grün) / "ICH SCHULDE" (Rot) / "OFFEN" (Gold)
- Jeder Chip: kleine Zahl in Bebas Neue 16sp + Label Roboto Condensed 10sp

"CREW" Header:
- Oswald Bold 13sp TextSecondary, ALL CAPS, Letter-Spacing 3sp
- Rechts: Stift-Icon (wenn Edit-Mode) oder nichts
- Edit-Mode-Button: "✏️ SORTIEREN" → "✓ FERTIG" Toggle

Person-Karte (CrewCard):
- Hintergrund: SurfaceCard
- Linker Rand: 3dp farbige Linie (Grün wenn sie mir schulden / Rot wenn ich schulde / Gold wenn ausgeglichen)
- Avatar: Kreis 48dp, Avatarfarbe, Initialen in Oswald Bold 18sp Weiß
  → Avatar hat Gold-Ring (2dp) wenn Betrag > 100€
- Name: Oswald SemiBold 16sp TextPrimary
- Darunter: Roboto Condensed 12sp TextSecondary "X Einträge • Zuletzt: DD.MM.YY"
- Rechts: Betrag in Bebas Neue 22sp (Grün/Rot je nach Richtung)
- Darunter Betrag: Status-Chip klein (OFFEN/TEILWEISE/BEZAHLT)
- Swipe-to-Reveal: Gold-Hintergrund rechts bei Swipe nach links → "DETAILS" Icon
- Long-Press: Drag-Handle erscheint (nur im Edit-Mode)
- Pressed-State: SurfaceElevated Hintergrund + leichter Gold-Scale

FAB (Neue Person):
- Position: rechts unten, 16dp Margin
- Form: ExtendedFloatingActionButton wenn Liste leer, CircularFAB wenn gefüllt
- Farbe: Gold (#FFD700)
- Icon: Person-Add Icon, Schwarz
- Gold Glow-Schatten: 0dp offset, 12dp blur, GoldGlow Farbe
- Click-Animation: kurzer Bounce (scale 0.92 → 1.05 → 1.0)

Pull-to-Refresh:
- Indikator: Gold-Spinner, kein Standard-Material-Blau
```

---

### SCREEN 3: `PersonDetailScreen.kt`

```
TopBar:
- Back-Arrow Gold
- Name in Oswald Bold 20sp
- Rechts: Settlement-Button (Geldsack-Icon Gold) + Kebab-Menü

Summen-Header:
- Großer Betrag: Bebas Neue 52sp, zentriert, Grün/Rot
- Darunter: Hinweistext "⬛ Halten = Kopieren  •  2× Tippen = Teilen"
  in Roboto Condensed 11sp TextMuted
- Long-Press-Feedback: kurzer Gold-Flash über den Betrag
- DoubleTap-Feedback: Share-Icon erscheint kurz animiert

Tab-Leiste (OFFEN / TEILWEISE / BEZAHLT / ALLE):
- Tabs: Roboto Condensed 12sp ALL CAPS
- Aktiver Tab: Gold Underline 2dp + TextPrimary
- Inaktiver Tab: TextMuted
- Tab-Indicator-Animation: 300ms smooth slide

Schulden-Karte (DebtEntryCard):
- Hintergrund: SurfaceCard, Radius RadiusMD
- Obere Zeile: Datum links (Roboto Condensed 12sp TextSecondary) + Betrag rechts (Bebas Neue 24sp)
- Mittlere Zeile: Tags als Gold-Chips (height 20dp, Roboto Condensed 10sp)
- Untere Zeile: Fortschrittsbalken (wenn PARTIAL)
  → Balken: Grün-Gradient von 0% bis bezahlter Anteil
  → Darunter: "47,50 € von 100,00 € bezahlt" Roboto Condensed 11sp TextSecondary
- Status-Badge: Top-Right Corner, abgerundet, farbcodiert
- Storniert: komplette Karte 40% Opacity + Durchgestrichen

Swipe-Reveal (Swipe rechts → Bearbeiten):
- Gold Hintergrund (#FFD700), Stift-Icon Schwarz, "BEARBEITEN" Roboto Condensed 11sp Schwarz
- Schwelle: 60% Kartenbreite (exakt wie v1.6.8)

Swipe-Reveal (Swipe links → Teilzahlung):
- Orange Hintergrund (#FF9800), Münze-Icon Schwarz, "ZAHLUNG" Roboto Condensed 11sp Schwarz

Long-Press-Kontextmenü:
- ModalBottomSheet style
- Items: Bearbeiten (Stift-Icon Gold) / Stornieren (X-Icon Orange) / Löschen (Mülleimer Rot)
- Jedes Item: 56dp Höhe, Roboto Condensed 14sp, Divider zwischen Items

FAB (Neue Schuld):
- Gleicher Stil wie Dashboard-FAB
```

---

### SCREEN 4: `AddDebtSheet.kt` (Bottom Sheet)

```
Header:
- Drag-Handle: 32dp×4dp, TextMuted, zentriert oben
- Titel: "SCHULD HINZUFÜGEN" Oswald Bold 18sp Gold

Richtungs-Toggle:
- Zwei Buttons nebeneinander (50/50)
- "ER SCHULDET MIR": wenn aktiv → Grüner Hintergrund + Weiß Text + Checkmark
- "ICH SCHULDE": wenn aktiv → Roter Hintergrund + Weiß Text + Checkmark
- Inaktiv: SurfaceElevated Hintergrund + TextSecondary
- Toggle-Animation: 200ms smooth color transition

AmountInputPad (Einhand-Numpad):
- Betrag-Anzeige oben: Bebas Neue 56sp Gold, zentriert
  → "0,00 €" Platzhalter in TextMuted
  → Betrag ändert sich live beim Tippen
- Numpad 3×4 Grid:
  → Ziffern 1-9, 00, Komma, Backspace
  → Jede Taste: SurfaceElevated Hintergrund, RadiusMD
  → Ziffern: Oswald Bold 24sp TextPrimary
  → Backspace: Gold Icon
  → Pressed-State: Gold Hintergrund, Schwarz Text, Scale 0.94
  → Haptic-Feedback bei jedem Tap (android-ninja Skill nutzen für HapticFeedbackConstants)

Tags-Auswahl:
- Horizontal scrollbare Chip-Reihe
- Unausgewählt: SurfaceElevated Hintergrund, TextSecondary, BorderSubtle
- Ausgewählt: Gold Hintergrund, Schwarz Text
- Chip-Höhe: 28dp, RadiusFull

Datums-Feld:
- Roboto Condensed 14sp
- Tap → DatePickerDialog mit Dark-Theme

Kommentarfeld (wenn in Einstellungen aktiviert):
- OutlinedTextField, BorderSubtle, Gold wenn fokussiert
- "Notiz (optional)" Placeholder in TextMuted

"HINZUFÜGEN" Button:
- Volle Breite, 56dp Höhe, Gold Hintergrund, Schwarz Text
- Oswald Bold 16sp ALL CAPS
- Disabled wenn kein Betrag: 30% Opacity
- Loading-State: Gold Spinner auf Gold Hintergrund
- 80% Swipe-to-Dismiss-Schwelle beibehalten
```

---

### SCREEN 5: `SettlementSheet.kt` (Tilgung)

```
Header:
- "SCHULDEN TILGEN" Oswald Bold 18sp Gold

Betrag-Eingabe:
- Gleicher AmountInputPad wie AddDebtSheet

Echtzeit-Vorschau:
- Überschrift: "VERRECHNUNG" Oswald Bold 13sp TextSecondary ALL CAPS
- Für jeden verrechneten Eintrag eine Zeile:
  → Datum + Tags (Gold-Chips) + Betrag FIFO
  → Verbleibender Rest (wenn Teilverrechnung) in TextSecondary
- Gesamtsumme: fette Gold-Linie + "GESAMT: XX,XX €" Bebas Neue 22sp

"TILGEN" Button:
- Gleicher Stil wie "HINZUFÜGEN"-Button, aber Grün (#00E676) Hintergrund
```

---

### SCREEN 6: `StatisticsScreen.kt`

```
TopBar:
- "STATISTIKEN" Oswald Bold 22sp Gold

Kachel-Grid (2×2 oben):
- Kachel 1: "SCHULDET MIR" → Grüner Betrag in Bebas Neue 28sp
- Kachel 2: "ICH SCHULDE" → Roter Betrag
- Kachel 3: "NETTO" → Grün/Rot je nach Vorzeichen
- Kachel 4: "OFFEN" → Gold Zahl
- Jede Kachel: SurfaceCard, RadiusLG, Gold-Akzent-Linie oben 2dp

Sektions-Header-Stil (für alle 6 Sektionen):
- "█ TOP SCHULDNER" in Oswald Bold 13sp TextSecondary ALL CAPS
- Gold vertikale Linie 3dp links

Top-Schuldner / Top-Gläubiger:
- Rang-Zahl: Bebas Neue 20sp Gold (#1) / TextSecondary (#2–5)
- Avatar + Name + Betrag in einer Zeile
- Horizontaler Balken (Breite proportional zum Max-Betrag), Gold-Gradient

Letzte Aktivität (7 Einträge):
- Timeline-Stil: Gold Dot + Linie + Eintrag-Info

Status-Verteilung:
- Horizontale SegmentedBar: Gold (Offen) + Orange (Teilweise) + Grün (Bezahlt) + Grau (Storniert)
- Darunter Legende mit Prozentzahlen

Monatsdiagramm (Vico):
- Vico BarChart komplett in Dark-Theme
- Balken: Gold-Gradient (oben hell, unten GoldDim)
- Achsenbeschriftung: Roboto Condensed 11sp TextSecondary
- Monat-Labels: "JAN", "FEB" etc.
```

---

### SCREEN 7: `SettingsScreen.kt`

```
TopBar:
- "EINSTELLUNGEN" Oswald Bold 22sp Gold

Sektionen mit Gruppenheadern:
- Gleicher Header-Stil wie Statistiken

Einstellungs-Items:
- Höhe: 64dp
- Icon links: Gold (24dp)
- Titel: Roboto Condensed 16sp TextPrimary
- Subtitle: Roboto Condensed 12sp TextSecondary
- Rechts: Toggle (Gold wenn an) oder Chevron oder Wert

Gefährliche Aktionen ("ALLE DATEN LÖSCHEN"):
- Roter Text, Rotes Icon
- Bestätigungs-Dialog mit zwei Buttons: "ABBRECHEN" (OutlinedButton) + "LÖSCHEN" (FilledButton Rot)

Tag-Verwaltung:
- Chips in einer Wrap-Row angezeigt
- "+ TAG HINZUFÜGEN" Chip mit gestricheltem Gold-Border
- Löschen per Long-Press auf Chip → Shake-Animation + Bestätigung
```

---

### SCREEN 8: `UpdateDialog.kt`

```
- ModalDialog, SurfaceOverlay Hintergrund, RadiusXL
- Header: "UPDATE VERFÜGBAR 🔱" Oswald Bold 20sp Gold
- Version: "v1.6.8 → v2.0.0" Roboto Condensed 14sp TextSecondary
- Changelog: scrollbare LazyColumn, max 240dp Höhe
  → Jede Zeile: "• " + Roboto Condensed 13sp TextPrimary
- Download-Fortschrittsbalken: Gold, animiert
- Buttons: "ÜBERSPRINGEN" (TextButton TextMuted) + "JETZT LADEN" (FilledButton Gold)
```

---

## ⚙️ AUFGABE 3 — ANIMATIONEN & MICRO-INTERACTIONS

Nutze `react-native-skills` + `ui-ux-pro-max-skill` als Referenz für folgende Animationen (alle in Jetpack Compose Animate* APIs umsetzen):

```kotlin
// 1. Gold-Pulse für Fingerabdruck-Icon (BiometricScreen)
val scale by animateFloatAsState(
    targetValue = if (isIdle) 1.0f else 1.08f,
    animationSpec = infiniteRepeatable(
        animation = tween(1500, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )
)

// 2. FAB Bounce beim Klicken
// scale: 1.0 → 0.92 → 1.05 → 1.0 über 300ms

// 3. Karten-Erscheinen (LazyColumn items)
// AnimatedVisibility mit fadeIn + slideInVertically(initialOffsetY = { it / 3 })

// 4. Betrag-Änderung Counter-Animation
// animateIntAsState für Zähler-Effekt wenn Betrag sich ändert

// 5. Tab-Indicator smooth slide
// animateDpAsState für x-Position des Indicators

// 6. Swipe-Reveal Color-Transition
// lerp() zwischen SurfaceCard und Gold/Orange basierend auf swipeProgress

// 7. Shake-Animation für Fehler
// keyframes { 0ms: 0dp; 50ms: -6dp; 100ms: 6dp; 150ms: -6dp; 200ms: 6dp; 250ms: 0dp }

// 8. Status-Badge Pop
// AnimatedContent mit scaleIn/scaleOut wenn Status sich ändert

// 9. Fortschrittsbalken animiert von 0 auf aktuellen Wert beim Erscheinen
// animateFloatAsState mit tween(800ms)

// 10. Gold-Glow Pulsieren auf aktiven Elementen
// animateColorAsState zwischen GoldGlow und Color.Transparent
```

---

## 🔧 AUFGABE 4 — KOMPONENTEN-BIBLIOTHEK

Nutze `android-ninja` Skill. Erstelle alle wiederverwendbaren Composables:

### `ui/components/BugListComponents.kt`

```kotlin
// PFLICHT-KOMPONENTEN — alle vollständig implementieren:

// 1. AmountText(amount: Double, modifier: Modifier)
//    → Bebas Neue Font, Farbe automatisch Grün/Rot/Gold je nach Vorzeichen

// 2. GoldChip(text: String, selected: Boolean, onClick: () -> Unit)
//    → Tag-Chip mit Gold-Farben wie beschrieben

// 3. StatusBadge(status: DebtStatus, modifier: Modifier)
//    → Farbcodierter Badge mit AnimatedContent

// 4. BugListTopBar(title: String, actions: List<TopBarAction>)
//    → Standardisierte TopBar für alle Screens

// 5. SectionHeader(title: String, icon: ImageVector? = null)
//    → Gold-Akzentlinie + Oswald Header

// 6. PersonAvatar(name: String, color: Color, size: Dp, showGoldRing: Boolean)
//    → Initialen-Avatar mit optionalem Gold-Ring

// 7. DebtProgressBar(paid: Double, total: Double, modifier: Modifier)
//    → Animierter Grün-Gradient Fortschrittsbalken

// 8. BugListCard(modifier: Modifier, accentColor: Color, content: @Composable () -> Unit)
//    → Standard-Karte mit linker Akzentlinie

// 9. GoldFAB(onClick: () -> Unit, extended: Boolean, label: String)
//    → Gold-FAB mit Glow und Bounce-Animation

// 10. ConfirmDeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit)
//     → Bestätigungs-Dialog für Löschvorgänge

// 11. AmountInputPad(value: String, onValueChange: (String) -> Unit)
//     → Vollständiges Numpad wie in Aufgabe 2 beschrieben

// 12. BugListSwipeableCard(
//         content: @Composable () -> Unit,
//         onSwipeRight: () -> Unit,
//         onSwipeLeft: () -> Unit,
//         leftRevealContent: @Composable () -> Unit,
//         rightRevealContent: @Composable () -> Unit,
//         swipeThreshold: Float = 0.6f
//     )
//     → Wiederverwendbare Swipe-Karte für PersonDetail und CrewList
```

---

## 🛡️ AUFGABE 5 — SICHERHEIT BEIBEHALTEN (android-ninja)

**ALLE folgenden Sicherheits-Features müssen exakt erhalten bleiben — kein einziges darf entfernt werden:**

```kotlin
// security/KeystoreManager.kt — UNVERÄNDERT lassen:
// setUserAuthenticationRequired(true)
// setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)
// setInvalidatedByBiometricEnrollment(true)

// AndroidManifest.xml — ALLE Flags prüfen:
// android:allowBackup="false" ✓
// android:networkSecurityConfig="@xml/network_security_config" ✓

// network_security_config.xml:
// cleartextTrafficPermitted="false" ✓

// MainActivity.kt:
// window.setFlags(FLAG_SECURE, FLAG_SECURE) // muss in onCreate() stehen

// Release-Build — Debugger-Detection MUSS bleiben:
// if (BuildConfig.DEBUG.not() && Debug.isDebuggerConnected()) {
//     Process.killProcess(Process.myPid())
// }

// SQLCipher PRAGMAs — alle 5 müssen gesetzt sein:
// cipher_memory_security = ON
// secure_delete = ON
// cipher_use_hmac = ON
// journal_mode = WAL
// cipher_page_size = 16384

// Biometrie-Fallback für Samsung A-Serie:
// BIOMETRIC_STRONG → bei Fehler → BIOMETRIC_WEAK + Passwort-Fallback
```

---

## 📦 AUFGABE 6 — GRADLE & DEPENDENCIES AKTUALISIEREN

Nutze `android-ninja` Skill für korrekte Gradle-Konfiguration:

### `build.gradle.kts` (App-Modul) — Pflicht-Updates:

```kotlin
// Versions-Katalog (libs.versions.toml) aktualisieren auf:
kotlin = "2.3.0"
composeBom = "2025.05.00"              // Neueste BOM 2025
hilt = "2.56.1"
room = "2.6.1"
sqlcipher = "4.9.0"
biometric = "1.1.0"
datastore = "1.1.4"
tink = "1.18.0"
argon2kt = "1.6.0"
navigation = "2.8.9"
vico = "2.0.1"
reorderable = "2.4.3"
ktor = "3.1.2"
ksp = "2.3.0-1.0.29"

// Neue Dependency für bessere Animationen hinzufügen:
// androidx.core:core-splashscreen:1.0.1

// R8 Full Mode in proguard-rules.pro sicherstellen:
// -allowaccessmodification
// -repackageclasses
// -assumenosideeffects class android.util.Log { *; }

// compileSdk = 35
// targetSdk = 35
// minSdk = 26

// buildFeatures:
// compose = true
// buildConfig = true
```

---

## 🌟 AUFGABE 7 — NEUE FEATURES (v2.0.0)

Nutze `agent-skills` für die Planung und Umsetzung:

### Feature A: Fälligkeits-Warnung
```kotlin
// DebtEntryCard zeigt roten Badge wenn dueDate überschritten
// "ÜBERFÄLLIG" Badge: Roter Hintergrund, Weiß, Roboto Condensed 10sp Bold
// Dashboard-Karte: rote Linie links statt Grün/Rot wenn überfällig + Heute
```

### Feature B: Schnell-Suche
```kotlin
// SearchBar im Dashboard: tippen filtert Crew-Liste live
// Suchleiste: SurfaceCard Hintergrund, Gold Cursor, Gold-Icon
// AnimatedVisibility für Such-Ergebnisse
// Leerer Zustand: "NIEMAND GEFUNDEN" in Oswald Bold 16sp TextMuted
```

### Feature C: Haptic Feedback System
```kotlin
// HapticManager.kt erstellen (android-ninja Skill nutzen)
// Jeder Numpad-Tap: KEYBOARD_TAP
// Erfolgreiches Hinzufügen: CONFIRM
// Löschen / Stornieren: REJECT
// Swipe-Threshold erreicht: CLOCK_TICK
// Long-Press: LONG_PRESS (HapticFeedbackConstants)
```

### Feature D: Leerer-Zustand-Screens (Empty States)
```kotlin
// Wenn keine Personen: 
//   → Großes Gold-Icon (Menschen-Gruppe) + "KEINE CREW NOCH" Oswald Bold 20sp
//   → "Tippe auf + um eine Person hinzuzufügen" Roboto Condensed 14sp TextSecondary
//   → Animierter Gold-Pfeil zum FAB

// Wenn keine Schulden für Person:
//   → Briefumschlag-Icon Gold + "ALLES BEGLICHEN" Oswald Bold 18sp DebtGreen
```

---

## 📝 AUFGABE 8 — AKTUALISIERTE README.md ERSTELLEN

Erstelle eine komplett neue `README.md` für v2.0.0 mit:
- Alle neuen Design-System-Details
- Screenshots-Sektion mit Platzhaltern für alle 8 Screens
- Neue Features (Fälligkeits-Warnung, Schnell-Suche, Haptic Feedback, Empty States)
- Aktualisierter Changelog v2.0.0
- Aktualisierter Tech-Stack (alle neuen Versionen)

---

## 🐛 AUFGABE 9 — BUGFIX: BIOMETRIE BEIM ZURÜCKKEHREN AUS DEM HINTERGRUND

### Problem (bekannt, muss gefixt werden)
Wenn die App in den Hintergrund geht (Home-Taste, andere App) und der Nutzer zurückkehrt,
wird **keine** biometrische Abfrage ausgelöst. Die App zeigt sofort den gesperrten Screen,
aber der Fingerabdruck-Dialog erscheint nicht automatisch — der Nutzer muss manuell tippen.
Nur ein kompletter Neustart (App aus dem Recents wischen + neu öffnen) löst die Biometrie aus.

### Ursache
Der `SessionManager` setzt `isLocked = true` bei `onStop()` korrekt — aber die
`BiometricLockScreen`-Composable registriert keinen `LifecycleEventObserver` für `ON_RESUME`.
Dadurch "sieht" niemand das Zurückkehren aus dem Hintergrund und `BiometricPrompt.authenticate()`
wird nicht neu aufgerufen. Der `isAuthenticated`-State im ViewModel bleibt außerdem `true`
solange der Prozess läuft — er wird bei `onPause`/`onStop` nie zurückgesetzt.

### Fix — 3 Dateien müssen geändert werden:

---

#### FIX 1: `SessionManager.kt` — Lock-State bei onStop zurücksetzen

```kotlin
// security/SessionManager.kt
// DefaultLifecycleObserver implementieren damit onStop() den State korrekt setzt

class SessionManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : DefaultLifecycleObserver {

    // StateFlow: true = gesperrt, false = entsperrt
    private val _isLocked = MutableStateFlow(true) // App startet immer gesperrt
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    // Wird aufgerufen wenn App in Hintergrund geht
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        _isLocked.value = true  // ← DAS IST DER KERN DES FIXES
    }

    fun onAuthenticationSuccess() {
        _isLocked.value = false
    }

    fun lockNow() {
        _isLocked.value = true
    }
}
```

---

#### FIX 2: `MainActivity.kt` — SessionManager als Lifecycle-Observer registrieren

```kotlin
// MainActivity.kt — in onCreate() hinzufügen

class MainActivity : FragmentActivity() {

    @Inject lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ← KRITISCH: SessionManager muss den Activity-Lifecycle beobachten
        // Ohne diese Zeile wird onStop() im SessionManager NIE aufgerufen
        lifecycle.addObserver(sessionManager)

        // FLAG_SECURE — unverändert lassen
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        // Debugger-Detection — unverändert lassen
        if (!BuildConfig.DEBUG && Debug.isDebuggerConnected()) {
            Process.killProcess(Process.myPid())
        }

        setContent {
            BugListTheme {
                val isLocked by sessionManager.isLocked.collectAsStateWithLifecycle()

                if (isLocked) {
                    BiometricLockScreen(
                        onAuthSuccess = { sessionManager.onAuthenticationSuccess() }
                    )
                } else {
                    BugListNavHost()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Observer wird automatisch entfernt wenn Activity destroyed wird
        lifecycle.removeObserver(sessionManager)
    }
}
```

---

#### FIX 3: `BiometricLockScreen.kt` — LifecycleResumeEffect für automatischen Prompt

```kotlin
// presentation/auth/BiometricLockScreen.kt

@Composable
fun BiometricLockScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity

    // State für Fehlerzustand (Shake-Animation)
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // BiometricPrompt einmalig erstellen, nicht bei jeder Recomposition neu
    val biometricPrompt = remember {
        BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onAuthSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Nur echte Fehler anzeigen, kein Spam bei "Zu viele Versuche"
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                        errorMessage = errString.toString()
                        showError = true
                    }
                }
                override fun onAuthenticationFailed() {
                    showError = true
                    errorMessage = "Biometrie nicht erkannt"
                }
            }
        )
    }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("BugList entsperren")
            .setSubtitle("Fingerabdruck oder Gesicht verwenden")
            .setNegativeButtonText("Abbrechen")
            // BIOMETRIC_STRONG beibehalten — kein DEVICE_CREDENTIAL
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
    }

    // ← DAS IST DER EIGENTLICHE FIX:
    // LifecycleResumeEffect feuert bei JEDEM ON_RESUME Event —
    // also sowohl beim ersten Start als auch beim Zurückkehren aus dem Hintergrund.
    // onPauseOrDispose setzt showError zurück damit der Screen sauber ist.
    LifecycleResumeEffect(Unit) {
        // Kleine Verzögerung damit die Activity vollständig resumed ist
        // bevor BiometricPrompt angezeigt wird (verhindert den
        // "authenticate() called after onSaveInstanceState()" Fehler)
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                biometricPrompt.authenticate(promptInfo)
            } catch (e: Exception) {
                // Fallback: Fehler anzeigen, manueller Tap möglich
                errorMessage = "Biometrie konnte nicht gestartet werden"
                showError = true
            }
        }, 250L) // 250ms Delay — ausreichend für Activity-Resume

        onPauseOrDispose {
            showError = false
            biometricPrompt.cancelAuthentication()
        }
    }

    // --- UI (siehe AUFGABE 2 SCREEN 1 für vollständiges Design) ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SpaceLG)
        ) {
            // Logo
            Text(
                text = "BUGLIST",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = OswaldFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = Gold,
                    letterSpacing = 2.sp
                )
            )

            // Gold-Linie
            Divider(
                modifier = Modifier.fillMaxWidth(0.6f),
                color = GoldDim,
                thickness = 1.dp
            )

            // Fingerabdruck Icon mit Pulse-Animation
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseScale"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .scale(pulseScale)
            ) {
                // Glow-Ring
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(GoldGlow, CircleShape)
                )
                // Fingerabdruck-Icon
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Entsperren",
                    tint = if (showError) DebtRed else Gold,
                    modifier = Modifier.size(64.dp)
                )
            }

            // Fehlermeldung (animiert ein/ausblenden)
            AnimatedVisibility(
                visible = showError,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Text(
                    text = errorMessage,
                    color = DebtRed,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = RobotoCondensedFamily
                    )
                )
            }

            // Manueller "Entsperren" Button als Fallback
            OutlinedButton(
                onClick = {
                    showError = false
                    try {
                        biometricPrompt.authenticate(promptInfo)
                    } catch (e: Exception) {
                        showError = true
                        errorMessage = "Biometrie nicht verfügbar"
                    }
                },
                border = BorderStroke(1.dp, BorderGold),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Text(
                    text = "ENTSPERREN",
                    fontFamily = RobotoCondensedFamily,
                    fontSize = 14.sp
                )
            }
        }

        // App-Version unten
        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            color = TextMuted,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = RobotoCondensedFamily,
                fontSize = 11.sp
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Space2XL)
        )
    }
}
```

---

### Zusammenfassung des Fixes

| Was | Warum |
|---|---|
| `SessionManager` implementiert `DefaultLifecycleObserver` | Damit `onStop()` automatisch `isLocked = true` setzt |
| `lifecycle.addObserver(sessionManager)` in `MainActivity.onCreate()` | Verbindet den SessionManager mit dem Activity-Lifecycle |
| `LifecycleResumeEffect` in `BiometricLockScreen` | Löst `authenticate()` bei JEDEM `ON_RESUME` aus — auch nach Hintergrund |
| 250ms `Handler.postDelayed` vor `authenticate()` | Verhindert den Crash "called after onSaveInstanceState()" |
| `biometricPrompt.cancelAuthentication()` in `onPauseOrDispose` | Schließt hängenden Dialog sauber wenn App wieder in Hintergrund geht |

---

## ✅ QUALITÄTSCHECKLISTE — PFLICHTPRÜFUNG VOR ABGABE

Gehe jeden Punkt durch und bestätige ihn:

```
DESIGN:
[ ] Kein einziger weißer/grauer Standard-Material-Hintergrund
[ ] Gold (#FFD700) konsistent als einzige Primärfarbe
[ ] Oswald für ALLE Headlines verwendet
[ ] Roboto Condensed für ALLE Body-Texte verwendet
[ ] Bebas Neue für ALLE Geldbeträge verwendet
[ ] Grün NUR für "Person schuldet mir" verwendet
[ ] Rot NUR für "Ich schulde" verwendet
[ ] Alle 10 Animationen implementiert
[ ] Alle 12 Komponenten in BugListComponents.kt vorhanden

FUNKTIONALITÄT:
[ ] Alle Features aus v1.6.8 vollständig erhalten
[ ] Swipe-Schwelle exakt 60% (nicht verändert)
[ ] FIFO-Settlement-Logik unverändert
[ ] Tag-System vollständig (Many-to-Many, max 30, Standard-Tags)
[ ] Drag & Drop Crew-Sortierung funktioniert
[ ] Pull-to-Refresh vorhanden
[ ] CSV-Export in Einstellungen vorhanden
[ ] Auto-Lock / SessionManager vorhanden
[ ] GitHub Update-Check vorhanden
[ ] In-App APK-Download vorhanden

BUGFIX BIOMETRIE HINTERGRUND:
[ ] SessionManager implementiert DefaultLifecycleObserver
[ ] lifecycle.addObserver(sessionManager) in MainActivity.onCreate() vorhanden
[ ] LifecycleResumeEffect in BiometricLockScreen vorhanden
[ ] 250ms postDelayed vor biometricPrompt.authenticate()
[ ] cancelAuthentication() in onPauseOrDispose vorhanden
[ ] Getestet: App in Hintergrund → zurückkehren → Dialog erscheint automatisch

SICHERHEIT:
[ ] FLAG_SECURE in MainActivity
[ ] allowBackup="false" in Manifest
[ ] cleartextTrafficPermitted="false"
[ ] Debugger-Detection im Release-Build
[ ] SQLCipher alle 5 PRAGMAs gesetzt
[ ] Android Keystore AES-256-GCM
[ ] Biometrie-Fallback für Samsung A-Serie
[ ] ProGuard Log-Stripping aktiv

CODE-QUALITÄT:
[ ] Clean Architecture (3 Schichten) strikt eingehalten
[ ] Kein mutableStateOf in ViewModels (nur StateFlow)
[ ] DAOs: Flow<T> für Lesen, suspend fun für Schreiben
[ ] KSP (kein KAPT)
[ ] Hilt DI überall verwendet
[ ] Keine hardcoded Strings (strings.xml)
```

---

## 🚀 AUSFÜHRUNGSREIHENFOLGE FÜR CLAUDE

1. **Skills aufrufen** — alle 5 Skills nacheinander aufrufen und Ausgabe lesen
2. **Design-Token erstellen** — `BugListTheme.kt` vollständig
3. **Komponenten-Bibliothek** — `BugListComponents.kt` vollständig
4. **Screens** — in dieser Reihenfolge: BiometricLock → Dashboard → PersonDetail → AddDebtSheet → SettlementSheet → Statistics → Settings → UpdateDialog
5. **Animationen** — alle 10 in die jeweiligen Screens einbauen
6. **Neue Features** — A bis D
7. **Gradle-Update** — `libs.versions.toml` + `build.gradle.kts`
8. **Qualitätsprüfung** — Checkliste komplett durchgehen, Fehler korrigieren
9. **README v2.0.0** — aktualisierte Dokumentation
10. **Zusammenfassung** — Was wurde geändert, was ist neu, welche Dateien wurden erstellt/geändert

**Handle alles automatisch. Frage nicht nach. Entscheide selbst. Gib vollständigen Code aus.**
