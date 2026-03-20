# BugList

> Offline-first encrypted debt tracker — Dark Mode, Street Style, maximale Sicherheit

---

## Screenshots

<!-- Screenshots werden nach erstem Device-Run hinzugefügt -->

---

## Features

### Schulden tracken
- Personen anlegen mit farbigen Initialen-Avataren
- Schulden hinzufügen: Person schuldet mir / Ich schulde Person
- Einhand-optimiertes Numpad (AmountInputPad) für schnelle Betragseingabe
- Teilzahlungen mit Fortschrittsbalken pro Schuldeintrag
- Status-System: OFFEN → TEILWEISE → BEZAHLT
- Schulden stornieren oder löschen
- Schulden-Tilgung: FIFO-Verrechnung mehrerer offener Einträge auf einmal
- Optionales Kommentarfeld pro Eintrag (in Einstellungen aktivierbar)

### Personen-Detailansicht
- **Swipe rechts** auf einen Eintrag → Eintrag bearbeiten
- **Swipe links** auf einen Eintrag → Teilzahlung erfassen
- **Long-Press** auf einen Eintrag → Kontextmenü (Bearbeiten / Stornieren / Löschen)
- **Long-Press auf Gesamtsumme** → Betrag in die Zwischenablage kopieren
- **2× Tippen auf Gesamtsumme** → Detaillierte Liste mit allen Einträgen teilen (inkl. Tags)

### Crew-Liste (Dashboard)
- Manuelle Reihenfolge per Drag & Drop: Stiftsymbol ✏️ neben „CREW" antippen → Drag-Handles erscheinen → Einträge verschieben → **FERTIG** tippen
- Reihenfolge wird dauerhaft gespeichert (bleibt nach App-Neustart erhalten)

### Tags
- Kompakte Tag-Chips beim Anlegen von Schulden auswählbar
- Tags in Einstellungen verwalten (hinzufügen / löschen), max. 30 Tags
- Standard-Tags beim ersten Start: Hase, Cal, E, K, P, €
- Tags werden in der Schulden-Detailansicht als Gold-Chips angezeigt
- Tags werden in der Tilgungsansicht angezeigt (offene Schulden + Tilgungsvorschau)
- Tags werden beim Teilen der Detailliste mit übertragen (`[Tag1, Tag2]` pro Zeile)
- Many-to-Many-Relation: eine Schuld kann mehrere Tags haben

### Statistiken
- Gesamt-Überblick: Schuldet mir / Ich schulde / Netto-Bilanz / Offene Einträge
- Top-Schuldner (Top 5)
- Top-Gläubiger (Top 5)
- Letzte Aktivität (7 neueste Einträge)
- Status-Verteilung (horizontale Progress-Bars)
- Bezahlte Schulden (Gesamthistorie)
- Monatliches Balkendiagramm (letzte 6 Monate)

### Updates
- Automatische Update-Prüfung beim Start (GitHub Releases API)
- In-App APK-Download und Installation — kein Redirect auf den Browser
- Version überspringen möglich (wird beim nächsten Start nicht mehr angezeigt)
- Changelog im Update-Dialog ist scrollbar

### Sicherheit
- AES-256-GCM Verschlüsselung des Datenbankschlüssels via Android Keystore
- Biometrische Authentifizierung (BIOMETRIC_STRONG — nur Hardware-Biometrie)
- Fallback für Geräte ohne Class-3-Sensor (z. B. Samsung Galaxy A-Serie): Class-2-Biometrie mit manuellem Passwort-Fallback
- SQLCipher 4.9.0 Datenbankverschlüsselung (AES-256)
- `FLAG_SECURE` — kein Screenshot, keine App-Switcher-Vorschau möglich
- Kein Backup möglich (`android:allowBackup="false"`)
- Kein Klartext-Traffic (`cleartextTrafficPermitted="false"`)
- Debugger-Detection im Release-Build (automatische Prozess-Terminierung)
- ProGuard R8 Full Mode mit Log-Stripping
- Automatischer Auto-Lock nach konfigurierbarem Timeout

### Design
- Gangster-Rap / Streetstyle Dark Mode — kein Light Mode
- Gold (`#FFD700`) als Primärfarbe für Akzente, FAB, Chips
- Fonts: Oswald (Headlines), Roboto Condensed (Body), Bebas Neue (Beträge)
- Farbcodierung: Grün = Person schuldet mir, Rot = Ich schulde
- Einhand-optimiertes Layout — Daumen erreicht alle Elemente

---

## Tech-Stack

| Komponente | Technologie | Version |
|---|---|---|
| Sprache | Kotlin | 2.3.x |
| UI | Jetpack Compose + Material 3 | BOM 2025.x |
| Datenbank | Room | 2.6.1 |
| DB-Verschlüsselung | SQLCipher (Zetetic) | 4.9.0 |
| Sicherheit | Android Keystore AES-256-GCM | OS-nativ |
| Biometrie | androidx.biometric | 1.1.0 |
| Config | Jetpack DataStore + Tink | 1.1.4 / 1.18.0 |
| KDF | Argon2Kt | 1.6.0 |
| DI | Hilt | 2.56.x |
| Navigation | Compose Navigation | 2.8.x |
| Charts | Vico | 2.0.x |
| Drag & Drop | sh.calvin.reorderable | 2.4.3 |
| HTTP | Ktor | aktuell |
| Build | Gradle + KSP + R8 Full Mode | 8.x |

---

## Sicherheitsarchitektur

### Datenverschlüsselung

Die SQLite-Datenbank wird vollständig mit SQLCipher (AES-256) verschlüsselt.
Der 256-Bit-Zufallsschlüssel wird nie im Klartext gespeichert — er wird mit
AES-256-GCM im Android Keystore verschlüsselt und in einem Tink-gesicherten
DataStore abgelegt. Die Entschlüsselung des DB-Schlüssels ist nur nach
biometrischer Authentifizierung möglich.

### Schlüsselverwaltung

Der Keystore-Key ist mit zwingenden Parametern gebunden:

```
setUserAuthenticationRequired(true)
setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)
setInvalidatedByBiometricEnrollment(true)
```

`AUTH_BIOMETRIC_STRONG` bedeutet: ausschließlich Hardware-Biometrie (Fingerabdruck,
Face ID). PIN/Passwort als Fallback ist explizit ausgeschlossen. Wenn ein neuer
Fingerabdruck enrollt wird, wird der Key automatisch invalidiert — die App
erfordert dann eine Neueinrichtung.

### SQLCipher-Konfiguration

```sql
PRAGMA cipher_memory_security = ON;   -- Speicher wird nach Nutzung genullt
PRAGMA secure_delete = ON;            -- Gelöschte Pages werden überschrieben
PRAGMA cipher_use_hmac = ON;          -- HMAC für Page-Authentifizierung
PRAGMA journal_mode = WAL;            -- Write-Ahead Logging
PRAGMA cipher_page_size = 16384;      -- 16 KB Pages (Android 15+ Pflicht)
```

### App-Härtung

- `FLAG_SECURE` verhindert Screenshots, Screen-Recordings und App-Switcher-Thumbnails
- Release-Build: `Debug.isDebuggerConnected()` → `Process.killProcess()` bei positivem Befund
- ProGuard `-assumenosideeffects` entfernt alle `android.util.Log.*`-Aufrufe aus Release
- `android:allowBackup="false"` — ADB Backup produziert eine leere Datei
- `network_security_config.xml`: `cleartextTrafficPermitted="false"`
- `data_extraction_rules.xml`: DB und DataStore von Cloud-Backup ausgeschlossen

---

## Datenmodell

```
Person          1 ─── N   DebtEntry      1 ─── N   Payment
                               │
                               M
                               │
                          debt_entry_tags
                               │
                               M
                               │
                             Tag
```

- **Person**: Name, Telefon (optional), Notizen (optional), Avatarfarbe, `sortIndex` (manuelle Reihenfolge)
- **DebtEntry**: Betrag (immer positiv), Richtung (isOwedToMe), Status, Datum, Fälligkeitsdatum, optionale Beschreibung
- **Payment**: Teilzahlung auf eine Schuld mit Betrag und optionaler Notiz
- **Tag**: Nutzerdefiniertes Label, max. 20 Zeichen
- **DebtStatus**: `OPEN` → `PARTIAL` (erste Teilzahlung) → `PAID` (vollständig bezahlt) oder `CANCELLED`

---

## Architektur

Clean Architecture mit drei strikten Schichten:

```
Presentation     Compose Screens + ViewModels (StateFlow, sealed UiState)
     │
Domain           Use Cases + Repository Interfaces + Domain Models
     │
Data             Room DAOs + Entities + Repository-Implementierungen
                 Security: KeystoreManager, BiometricAuthManager, PassphraseManager
```

**Regeln:**
- Domain importiert nichts aus `data/` oder `presentation/`
- ViewModels: ausschließlich `StateFlow` — kein `mutableStateOf`
- DAOs: `Flow<T>` für Lesezugriffe, `suspend fun` für Schreibzugriffe
- KSP statt KAPT (kein Deprecated-Warning, schnellerer Build)

---

## Datenschutz

- Alle Daten bleiben auf dem Gerät (Offline-First — kein Server, keine Cloud)
- Keine `INTERNET`-Permission (bis auf optionalen Update-Check via GitHub Releases API)
- Kein Cloud-Backup (weder Android Backup Service noch ADB)
- Datenbank ist ohne biometrische Authentifizierung nicht lesbar
- Vollständiger CSV-Export der eigenen Daten über die Einstellungen möglich

---

## Installation

### Voraussetzungen

- Android 8.0 oder höher (API Level 26+)
- Biometrisches Hardware (Fingerabdruck-Sensor oder Gesichtserkennung)
- Keine Internet-Verbindung erforderlich

### APK installieren

1. [Neueste Release-APK](../../releases/latest) herunterladen
2. In Android-Einstellungen „Installation aus unbekannten Quellen" für den Browser/Datei-Manager erlauben
3. APK antippen und installieren
4. Beim ersten Start wird die biometrische Authentifizierung eingerichtet

---

## Build

```bash
# Debug-APK
./gradlew assembleDebug

# Release-APK (Keystore erforderlich)
./gradlew assembleRelease

# Unit-Tests
./gradlew test

# Instrumentierte Tests (Emulator/Gerät erforderlich)
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

---

## Changelog

### v1.6.8
- Swipe-Schwelle auf **60 %** der Kartenbreite angehoben (vorher 50 %)
- Verhindert versehentliches Öffnen von Bearbeiten/Teilzahlung beim normalen Scrollen

### v1.6.7
- **Crew-Liste manuell sortieren**: Stiftsymbol ✏️ neben CREW-Header → Drag-Handles erscheinen → Einträge beliebig verschieben → FERTIG sichert die Reihenfolge dauerhaft
- DB-Schema v3: `sortIndex`-Spalte in der `persons`-Tabelle (Migration 2→3, bestehende Nutzer behalten alphabetische Reihenfolge)
- Dependency `sh.calvin.reorderable:reorderable-android:2.4.3` hinzugefügt

### v1.6.6
- **Tags in Tilgungsansicht**: Gold-Chips bei offenen Schulden und in der Tilgungs-Vorschau sichtbar
- **Tags beim Teilen**: 2× Tippen auf Summe → Detaillierte Liste enthält Tags (`[Tag1, Tag2]`) pro Zeile
- Hinweistext unter Summe: „Gedrückt halten = Summe  •  2× tippen = Detaillierte Liste"
- Update-Changelog im Dialog ist jetzt scrollbar (kein Abschneiden nach 6 Zeilen)
- Kommentarfeld beim Schulden-Anlegen optional: in Einstellungen ein-/ausschaltbar (Standard: aus)

### v1.6.5
- Bugfix: Liste aktualisiert sich zuverlässig nach dem Anlegen oder Bearbeiten eines Eintrags (SQLCipher InvalidationTracker-Workaround via in-memory Version-Counter)
- Bugfix: PAID/ALLE-Tab-Balance zeigt korrekten Betrag
- Bugfix: DownloadManager `UnsupportedOperationException` bei internen Pfaden behoben

### v1.6.4
- Pull-to-Refresh auf dem Dashboard (aktualisiert sofort)
- Sofortige Listenaktualisierung nach Anlegen oder Bearbeiten eines Eintrags

### v1.6.3
- **Long-Press auf Gesamtsumme** → Betrag in die Zwischenablage kopieren
- **2× Tippen auf Gesamtsumme** → Detaillierte Liste aller Einträge als Text teilen
- **LÖSCHEN** im Long-Press-Kontextmenü von Schuldeneinträgen (mit Bestätigungsdialog)

### v1.6.2
- **Long-Press-Kontextmenü** auf Schuldeneinträgen: Bearbeiten / Stornieren / Löschen

### v1.6.1 – v1.6.0
- **Swipe rechts** auf Schuldeneintrag → Eintrag direkt bearbeiten
- **Swipe links** auf Schuldeneintrag → Teilzahlung-Sheet öffnen
- Visuelle Swipe-Hinweise: Gold für „Bearbeiten", Orange für „Teilzahlung"

### v1.5.7
- In-App APK-Download beim Update: Datei wird direkt heruntergeladen und zur Installation angeboten — kein Weiterleiten auf den Browser mehr

### v1.5.6
- Bugfix: Backspace in Teilzahlungs-Sheet entfernt jetzt korrekt nur die letzte Stelle
- Bugfix: Tag-Anzeige aktualisiert sich nach dem Tilgen sofort
- Bugfix: Update-Schaltfläche im Update-Dialog funktioniert zuverlässig

### v1.5.2 – v1.5.5
- Farbkorrektur (konsistent in allen Screens): **Grün = Person schuldet mir**, **Rot = Ich schulde**
- Vorzeichen-Korrektur in Dashboard-Kacheln und Statistiken
- Biometrie-Fallback für Samsung Galaxy A-Serie (Class-2-Sensor ohne StrongBox): automatischer Fallback auf `BIOMETRIC_WEAK` mit Passwort-Sicherung

### v1.5.1
- Update-Dialog: Versionsnummer und Release-Notes werden angezeigt
- Easter Egg beim langen Halten des App-Namens
- UX-Feinschliff (Paddings, Animationen, Schriftgrößen)
- AddDebtSheet: 80 % Swipe-to-Dismiss-Schwelle verhindert versehentliches Schließen

### v1.5.0
- **Tag-System**: Tags statt Freitext-Beschreibung bei Schuldeneinträgen
- Tags in Einstellungen verwalten (hinzufügen / löschen), max. 30 Tags
- Standard-Tags beim ersten Start: Hase, Cal, E, K, P, €
- Tags werden in der Schulden-Detailansicht als Gold-Chips angezeigt
- Room DB-Migration 1→2 (neue Tabellen: `tags`, `debt_entry_tags`)
- Many-to-Many-Relation mit CASCADE DELETE

### v1.4.0
- Statistiken-Screen komplett ausgebaut (6 Sektionen, echte Daten)
- StatisticsScreen: Gesamt-Überblick, Top-Schuldner, Top-Gläubiger, Letzte Aktivität, Status-Verteilung, Bezahlte Schulden
- Nested combine für 7 Flows

### v1.3.0
- Bugfix: `SQLiteNotADatabaseException` auf echtem Gerät nach Auth-Success
- Farb-Inversion initial korrigiert: Grün = Schuldet mir, Rot = Ich schulde
- Tilgungs-Toast nach erfolgreicher Settlement-Buchung
- SettlementSheet: nicht mehr per Swipe schließbar

### v1.2.0
- Schulden-Tilgung (Settlement): FIFO-Verrechnung mehrerer offener Einträge
- SettlementSheet mit Echtzeit-Vorschau der Verrechnung
- `SettleDebtsUseCase` mit 9 Unit-Tests
- Kaltstart-Optimierung: SQLCipher-Init auf IO-Thread, SplashScreen API

### v1.1.0
- Auto-Update via GitHub Releases API (Ktor)
- Auto-Lock mit konfigurierbarem Timeout (SessionManager)
- „Alle Daten löschen" in Einstellungen

### v1.0.0
- Initiale Version
- Biometrische Authentifizierung (BIOMETRIC_STRONG)
- Verschlüsselte Datenbank (SQLCipher + Android Keystore AES-256-GCM)
- Personen und Schulden verwalten
- Teilzahlungen mit PaymentProgressBar
- Dashboard mit Gesamtbilanz
- Statistiken mit Vico 2.0.1 Balkendiagramm

---

## Lizenz

Privates Projekt — alle Rechte vorbehalten.
