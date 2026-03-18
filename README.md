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
- Schulden stornieren (STORNIERT-Status)
- Schulden-Tilgung: FIFO-Verrechnung mehrerer offener Einträge auf einmal
- 2-Sekunden-Long-Press auf DebtCard zum Bearbeiten eines Eintrags

### Tags
- Kompakte Tag-Chips statt Freitext-Beschreibung beim Anlegen von Schulden
- Tags in Einstellungen verwalten (hinzufügen / löschen)
- Standard-Tags beim ersten Start: Hase, Cal, E, K, P, €
- Tags werden in der Schulden-Detailansicht als Gold-Chips angezeigt
- Many-to-Many-Relation: eine Schuld kann mehrere Tags haben

### Statistiken
- Gesamt-Überblick: Schuldet mir / Ich schulde / Netto-Bilanz / Offene Einträge
- Top-Schuldner (Top 5)
- Top-Gläubiger (Top 5)
- Letzte Aktivität (7 neueste Einträge)
- Status-Verteilung (horizontale Progress-Bars)
- Bezahlte Schulden (Gesamthistorie)
- Monatliches Balkendiagramm (letzte 6 Monate)

### Sicherheit
- AES-256-GCM Verschlüsselung des Datenbankschlüssels via Android Keystore
- Biometrische Authentifizierung (BIOMETRIC_STRONG — nur Hardware-Biometrie)
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

- **Person**: Name, Telefon (optional), Notizen (optional), Avatarfarbe
- **DebtEntry**: Betrag (immer positiv), Richtung (isOwedToMe), Status, Datum, Fälligkeitsdatum
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
2. In Android-Einstellungen "Installation aus unbekannten Quellen" für den Browser/Datei-Manager erlauben
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
- Nested combine für 7 Flows (L-086: CoreStats-Grouping-Pattern)

### v1.3.0
- Bugfix: `SQLiteNotADatabaseException` auf echtem Gerät nach Auth-Success
- Root Cause: `passphrase.fill(0)` in `finally`-Block während `SupportOpenHelperFactory` noch Reader-Connections öffnet
- Fix: ByteArray verlässt Scope natürlich — GC übernimmt nach Abschluss aller Connections (L-078)
- Farb-Inversion: "SCHULDET MIR" = ROT, "ICH SCHULDE" = GRÜN (konsistent mit L-081)
- Tilgungs-Toast nach erfolgreicher Settlement-Buchung (L-082: SharedFlow-Pattern)
- SettlementSheet: nicht mehr per Swipe schließbar (`skipPartiallyExpanded`, L-083)
- 2-Sekunden-Long-Press-Edit auf DebtCard (L-084: `pointerInput`-Timer-Pattern)

### v1.2.0
- Schulden-Tilgung (Settlement): FIFO-Verrechnung mehrerer offener Einträge
- SettlementSheet mit Echtzeit-Vorschau der Verrechnung
- `SettleDebtsUseCase` mit 9 Unit-Tests
- Kaltstart-Optimierung: SQLCipher-Init auf IO-Thread, SplashScreen API
- Auth-Screen: Fingerprint-Icon nach Cancel wieder klickbar (L-071)
- Statuschips zeigen lokalisierte deutsche Texte (L-070)

### v1.1.0
- Auto-Update via GitHub Releases API (Ktor)
- Auto-Lock mit konfigurierbarem Timeout (SessionManager)
- "Alle Daten löschen" in Einstellungen funktioniert (L-087: Dispatchers.IO)

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
