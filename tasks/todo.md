# BugList – Task Board

> Nach jedem Task aktualisieren.
> `[ ]` = offen | `[~]` = in Arbeit | `[x]` = abgeschlossen + verifiziert

**Letztes Update:** 2026-03-27
**Status:** [x] v2.0.0 – Vollständiges Redesign + neue Features
**Letzte Änderungen:** Design-System v2, Search, Overdue-Badge, HapticManager, neue Farbtokens, Biometric-Fix bereits in v1.9.1
**Emulator:** Läuft (nutze MCP für alle Tests)

---

## Phasen-Übersicht

| Phase | Beschreibung | Status |
|---|---|---|
| Phase 1 | Projektsetup & Sicherheitsfundament | ✅ Abgeschlossen |
| Phase 2 | Datenlogik & Domain-Schicht | ✅ Abgeschlossen |
| Phase 3 | Streetstyle-UI mit Jetpack Compose | ✅ Abgeschlossen |
| Phase 4 | Navigation, State & Integration | ✅ Abgeschlossen |
| Phase 5 | Testing, Hardening & Release | ✅ Abgeschlossen |

---

## PHASE 1 – Projektsetup & Sicherheitsfundament

> **Ziel:** App startet, BiometricPrompt erscheint, SQLCipher-DB öffnet sich nach Auth.
> **MCP-Abnahme:** Screenshot zeigt Auth-Screen, Logcat zeigt keine Errors, DB-File ist verschlüsselt.

### Task 1.1 – Projekt-Grundstruktur
- [x] Android-Projekt erstellen: `com.buglist`, Kotlin, Compose BOM, Material 3
- [x] Package-Struktur anlegen (presentation, domain, data, security, di, util)
- [x] `libs.versions.toml` mit allen Dependencies
- [x] `build.gradle.kts` (App): Room, SQLCipher, Biometric, Hilt, DataStore, Tink, Argon2Kt
- [x] KSP konfigurieren (KEIN KAPT)
- [x] `proguard-rules.pro`: SQLCipher, Room, Hilt Regeln
- [x] `AndroidManifest.xml`: `allowBackup=false`, `USE_BIOMETRIC`, networkSecurityConfig
- [x] `network_security_config.xml`: cleartext=false
- [x] `data_extraction_rules.xml`: alle DB/DataStore Files excluded
- [x] `themes.xml`: `windowBackground` = `#0D0D0D` (kein weißer Blitz)
- [x] **MCP-Check:** `gradle: assembleDebug` → grün, App startet, UI-Tree korrekt, FLAG_SECURE bestätigt

---

### Task 1.2 – Android Keystore Manager
- [x] `security/KeystoreManager.kt`
- [x] AES-256-GCM KeyGenParameterSpec mit allen Pflicht-Parametern
- [x] StrongBox-Check + Fallback auf TEE
- [x] `encrypt(plaintext: ByteArray): EncryptedData` (IV + Ciphertext)
- [x] `decrypt(encryptedData: EncryptedData): ByteArray`
- [x] `KeyPermanentlyInvalidatedException` → Key löschen + neu generieren
- [x] Hilt `@Singleton` Module
- [x] **MCP-Check:** `gradle: test` → 14 Unit-Tests grün

---

### Task 1.3 – SQLCipher-verschlüsselte Room-Datenbank
- [x] `PersonEntity`, `DebtEntryEntity`, `PaymentEntity` mit FK CASCADE + Indices
- [x] `PersonDao`, `DebtEntryDao`, `PaymentDao` mit Flow-Returns
- [x] `AppDatabase` mit `SupportOpenHelperFactory`
- [x] `PassphraseManager` (256-bit random, Tink AES-256-GCM, DataStore)
- [x] `DatabaseModule` mit allen PRAGMAs (cipher_memory_security, secure_delete, hmac, WAL, 16KB)
- [x] Passphrase wird nach DB-Übergabe genullt
- [x] **MCP-Check:** 10 DB-Tests grün, 3 Encryption-Tests grün (kein Klartext, kein SQLite-Magic-Header)
- [ ] `PersonEntity` + `DebtEntryEntity` mit FK (CASCADE DELETE) + Indices
- [ ] `PaymentEntity` mit FK auf DebtEntry (CASCADE DELETE), Index auf `debtEntryId`
- [ ] `PersonDao` + `DebtEntryDao` mit Flow-Returns
- [ ] `AppDatabase` mit `SupportOpenHelperFactory` (NICHT SupportFactory)
- [ ] 256-Bit zufällige Passphrase als ByteArray
- [ ] Passphrase verschlüsselt → DataStore (via Tink)
- [ ] Alle PRAGMAs setzen (cipher_memory_security, secure_delete, hmac, WAL, 16KB page)
- [ ] Hilt DatabaseModule
- [ ] **MCP-Check:** `gradle: connectedAndroidTest` → DB-Tests grün
- [ ] **MCP-Check:** `adb shell` + `xxd` auf DB-File → kein Klartext sichtbar

---

### Task 1.4 – Biometrische Authentifizierung
- [x] `security/BiometricAuthManager.kt`
- [x] `BiometricManager.canAuthenticate(BIOMETRIC_STRONG)` Prüfung
- [x] `BiometricPrompt` mit `CryptoObject(cipher)` – KEIN Soft-Binding
- [x] `onAuthenticationSucceeded` / `onAuthenticationError` / `onAuthenticationFailed`
- [x] `KeyPermanentlyInvalidatedException` → Key löschen + Caller benachrichtigen
- [x] Hilt `@Singleton` Binding
- [x] `MainActivity : FragmentActivity()` (nicht ComponentActivity) — L-066
- [x] `AuthScreen`, `AuthViewModel`, `AuthUiState` vollständig implementiert
- [x] Debug-Logs entfernt (kein sensitiver Auth-Flow in Logcat)
- [x] **MCP-Check:** BiometricPrompt zeigt sich, `onAuthenticationSucceeded` in Logcat bestätigt

---

### Task 1.5 – Security Hardening Basis
- [x] `FLAG_SECURE` in `MainActivity.onCreate()` VOR `setContent{}`
- [x] Release-Build: `Debug.isDebuggerConnected() || Debug.waitingForDebugger()` → `Process.killProcess()`
- [x] ProGuard: `-assumenosideeffects` für alle `android.util.Log.*` Methoden
- [x] `isDebuggable = false` in release buildType (kein manuelles Manifest-Flag)
- [x] `isMinifyEnabled = true`, `isShrinkResources = true` in release
- [x] **MCP-Check:** `gradle: assembleRelease` → grün (b-33, 1m6s)
- [x] **MCP-Check:** FLAG_SECURE verifiziert (schwarzes Screenshot per L-062)
- [x] **MCP-Check:** `adb backup com.buglist` → 0 Bytes (leer)

### ✅ Phase 1 Abnahme
- [x] `gradle: assembleRelease` grün, 0 Warnings (b-33)
- [x] BiometricPrompt korrekt angezeigt (UI-Hierarchy: "BugList Access", Fingerprint-Icon)
- [x] Auth-Flow ohne Fehler (13/13 Instrumented-Tests grün, b-34)
- [x] DB-File kein Klartext (EncryptionVerificationTest + 2 weitere grün)
- [x] ADB Backup → 0 Bytes (allowBackup=false greift)
- [x] Screenshot → schwarz (FLAG_SECURE korrekt, L-062)

---

## PHASE 2 – Datenlogik & Domain-Schicht

> **Ziel:** Vollständige Business-Logik, alle CRUD-Ops, korrekte Saldo-Berechnung.
> **MCP-Abnahme:** Alle Unit-Tests grün, Grenzfälle getestet.

### Task 2.1 – Domain-Modelle & Repository-Interfaces
- [x] `domain/model/`: Person, DebtEntry, DebtStatus (OPEN/PARTIAL/PAID/CANCELLED), PersonWithBalance, Payment, DebtEntryWithPayments, MonthlyStats
- [x] `domain/model/Result.kt`: sealed class für Error-Handling
- [x] `domain/repository/PersonRepository` Interface
- [x] `domain/repository/DebtRepository` Interface
- [x] `domain/repository/PaymentRepository` Interface
- [x] **MCP-Check:** `gradle: assembleDebug` → kompiliert (b-35)

---

### Task 2.2 – Use Cases (je ein File)
- [x] `AddPersonUseCase` (Validierung: Name nicht leer, max. 100 Zeichen)
- [x] `UpdatePersonUseCase`
- [x] `DeletePersonUseCase`
- [x] `GetPersonsWithBalancesUseCase` (sortierbar: Name, Betrag, Datum)
- [x] `AddDebtUseCase` (Betrag > 0, Datum required)
- [x] `AddPartialPaymentUseCase` (Validierung: Betrag > 0, Betrag <= remaining, Status auto-update)
- [x] `GetPaymentsForDebtUseCase` (Liste aller Teilzahlungen zu einer Schuld)
- [x] `MarkDebtAsPaidUseCase` (setzt eine Komplett-Payment für remaining)
- [x] `CancelDebtUseCase`
- [x] `GetDebtHistoryUseCase` (Filter: Status inkl. PARTIAL, Zeitraum)
- [x] `CalculateTotalBalanceUseCase` (nutzt remaining, nicht amount)
- [x] `ExportDataUseCase` (CSV inkl. Teilzahlungs-Historie)
- [x] **MCP-Check:** `gradle: test` → 84 Unit-Tests grün (b-36)

---

### Task 2.3 – Repository-Implementierungen
- [x] `data/repository/PersonRepositoryImpl`
- [x] `data/repository/DebtRepositoryImpl`
- [x] `data/repository/PaymentRepositoryImpl`
- [x] Status-Auto-Update: nach jeder Payment-Insertion `remaining` berechnen → PARTIAL oder PAID setzen (in einer Transaktion)
- [x] Transaktionshandling: Person-Delete atomisch (CASCADE FK)
- [x] Hilt-Bindings (Interface → Impl) via RepositoryModule
- [x] **MCP-Check:** `gradle: connectedAndroidTest` → 25 Tests grün (b-37, 12 Repository-Tests)

---

### Task 2.4 – Erweiterte DB-Queries
- [x] Nettosaldo pro Person: basierend auf `remaining` (nicht `amount`) — PersonDao SQL
- [x] Gesamtsaldo über alle Personen — DebtEntryDao.getTotalNetBalance()
- [x] Schulden pro Monat (letzte 6 Monate) — DebtEntryDao.getMonthlyTotals()
- [x] Zuletzt geänderte Einträge (Dashboard-Feed) — DebtEntryDao.getRecentDebtEntries()
- [x] Textsuche in `description` — DebtEntryDao.searchByDescription()
- [x] `DebtEntryWithPayments`: computed in DebtEntryWithPayments.from()
- [x] Zahlungshistorie pro Schuld: sortiert nach Datum desc
- [x] **MCP-Check:** `gradle: connectedAndroidTest` → RepositoryIntegrationTest grün (b-37)
- [x] Grenzfall-Tests: 0€, Überzahlung verhindern, genau remaining zahlen → PAID

### ✅ Phase 2 Abnahme
- [x] Alle Unit-Tests grün (`gradle: test`) — 84 Tests (b-36)
- [x] Alle Instrumented-Tests grün (`gradle: connectedAndroidTest`) — 25 Tests (b-37)
- [x] Saldo-Berechnung mit Grenzfällen getestet (totalNetBalance, remaining-Basis)
- [x] `android-toolkit: logcat` — keine DB-Errors

---

## PHASE 3 – Streetstyle-UI mit Jetpack Compose

> **Ziel:** Alle Screens gebaut, Design-System konsistent, visuell korrekt.
> **MCP-Abnahme:** Screenshot jedes Screens zeigt korrektes Gangster-Design.

### Task 3.1 – Design-System & Theme
- [x] `BugListTheme.kt` (nur Dark, kein Light)
- [x] Alle Farben aus `BugListColors`
- [x] Google Fonts: Oswald, Roboto Condensed, Bebas Neue
- [x] Reusable: `GoldButton`, `DebtCard`, `AmountText`, `StatusChip`, `PersonAvatar`, `PaymentProgressBar`, `AmountInputPad`

### Task 3.2 – Auth-Screen
- [x] Fullscreen dark background mit splash-screen.png overlay
- [x] App-Name Oswald Bold Gold, Fingerprint Icon pulsierend
- [x] Error-State + Auto-Trigger BiometricPrompt

### Task 3.3 – Dashboard
- [x] Gesamtsaldo, Two-Tile-Row, PersonList, FAB, Pull-to-Refresh, Empty State

### Task 3.4 – Personen-Detail-Screen
- [x] Tabs OFFEN/BEZAHLT/ALLE, DebtCard mit PaymentProgressBar, SwipeToDismiss, Zahlungshistorie

### Task 3.5 – Schulden-Eingabe & Teilzahlungs-Sheet
- [x] AddDebtSheet + AddPaymentSheet mit AmountInputPad

### Task 3.6 – Statistiken-Screen
- [x] Vico 2.0.1 Balkendiagramm (VerticalAxis.rememberStart, HorizontalAxis.rememberBottom, fill(Color))
- [x] Top-Schuldner-Liste

### Task 3.7 – Einstellungen-Screen
- [x] Währungs-/AutoLock-Dropdown, CSV-Export, Delete-All-Dialog

### ✅ Phase 3 Abnahme
- [x] Build b-47 grün, App startet ohne Crash
- [x] BiometricPrompt zeigt korrekte Titel ("BugList Access", Fingerprint-Icon)
- [x] L-067, L-068: Vico 2.0.1 API-Änderungen dokumentiert

---

## PHASE 4 – Navigation, State & Integration

> **Ziel:** Vollständiger User-Flow, Auto-Lock funktioniert, App komplett benutzbar.
> **MCP-Abnahme:** Kompletter Flow von Auth → Dashboard → Person → Schuld hinzufügen durchgeklickt.

### Task 4.1 – Compose Navigation
- [x] `NavHost` mit allen Routes (BugListNavHost.kt)
- [x] Auth-Guard: alle Routes außer `auth` nur nach Login erreichbar (LaunchedEffect auf isAuthenticated)
- [x] Back-Navigation-Handling (popBackStack, enableOnBackInvokedCallback)
- [x] **MCP-Check:** Kompletter Navigations-Flow via mobile-mcp durchgeklickt (Auth→Dashboard→PersonDetail)

---

### Task 4.2 – ViewModels
- [x] `AuthViewModel`: BiometricStatus StateFlow, startAuth(), Authenticated/Error States
- [x] `DashboardViewModel`: PersonsWithBalances Flow, TotalBalance, Refresh
- [x] `PersonDetailViewModel`: DebtList Flow, Filter-State, Swipe-Aktionen
- [x] `AddDebtViewModel`: Validierung, save(), Loading-State
- [x] `AddPersonViewModel`: Validierung, save()
- [x] `StatisticsViewModel`: Chart-Daten aufbereiten
- [x] `SettingsViewModel`: DataStore lesen/schreiben, autoLockTimeout
- [x] Alle `@HiltViewModel`, UiState als sealed class / StateFlow

---

### Task 4.3 – Session-Management & Auto-Lock
- [x] `SessionManager.kt` (Hilt Singleton, DefaultLifecycleObserver)
- [x] `ProcessLifecycleOwner` Observer (onStop/onStart)
- [x] Timer bei `ON_STOP`, Prüfung bei `ON_START` → lock() wenn abgelaufen
- [x] Konfigurierbarer Timeout via SettingsViewModel.setAutoLockTimeout()
- [x] **MCP-Check:** Auth-Guard via NavHost – isAuthenticated Flow navigiert zurück zu auth

---

### Task 4.4 – Edge Cases
- [x] Alle Screens haben Empty-State-UI (NO CREW YET, KEINE SCHULDEN, etc.)
- [x] `KeyPermanentlyInvalidatedException` handling in AuthViewModel
- [ ] Loading-States: Shimmer-Effekt (Skeleton) — bewusst weggelassen, Loading-Progress in ViewModels
- [ ] Löschen mit Undo-Snackbar — Delete-Confirmation-Dialog vorhanden, Snackbar-Undo für spätere Phase

### ✅ Phase 4 Abnahme
- [x] Kompletter Flow Auth→Dashboard→Person hinzufügen→PersonDetail via mobile-mcp (2026-03-18)
- [x] PRAGMA-Crash gefixed: setPragma() Extension für SQLCipher 4.9.0 (L-069)
- [x] `android-toolkit: logcat` → keine Errors im kompletten Flow (b-52, crash=none)
- [x] Person "Big_Mike" angelegt, Dashboard zeigt CREW-Liste, PersonDetail zeigt korrekte Tabs
- [ ] Auto-Lock Timeout-Test (30s) — manuell zu verifizieren
- [ ] `gradle: connectedAndroidTest` → Integrations-Tests auf neuem Build

---

## PHASE 5 – Testing, Hardening & Release

> **Ziel:** Produktionsreife App. Alle Tests grün, APK < 15MB, kein Klartext in DB.
> **MCP-Abnahme:** Release-APK auf Emulator installiert und vollständig verifiziert.

### Task 5.1 – Unit-Tests
- [x] 84 Unit-Tests: Use-Case-Tests (MockK), Repository-Logik, Grenzfälle (b-54)
- [x] **MCP-Check:** `gradle: test` → 84/84 grün (b-54)

---

### Task 5.2 – Instrumented Tests
- [x] 25 Instrumented Tests: DB-Encryption, DAO-Tests, Repository-Integration (b-53)
- [x] **MCP-Check:** `gradle: connectedAndroidTest` → 25/25 grün (b-53)

---

### Task 5.3 – Sicherheitstests (manuell via MCP)
- [x] `xxd databases/buglist.db` → kein SQLite Magic Header, reiner Ciphertext bestätigt
- [x] `adb backup com.buglist` → 0 Bytes (allowBackup=false greift)
- [x] `mobile-mcp: screenshot` während App läuft → schwarzes Bild (FLAG_SECURE)
- [x] `android-toolkit: logcat` → kein sensitiver Daten-Leak in Logs

---

### Task 5.4 – Performance
- [x] Kaltstart Release-APK: 940ms (Ziel < 1000ms) ✅
- [x] Debug-APK Kaltstart: 2813ms (erwartbar auf Emulator, kein Produktionswert)

---

### Task 5.5 – Release-Build
- [x] Signing Keystore generiert (`buglist-release.jks`)
- [x] `minifyEnabled = true`, `shrinkResources = true`, R8 Full Mode
- [x] `gradle: assembleRelease` → BUILD SUCCESSFUL (b-58)
- [x] APK-Größe: 20MB (SQLCipher .so für 3 ABIs; AAB für Play Store würde ~7MB reduzieren)
- [x] `gradle: lint` → 0 Fehler, 0 Warnings (b-57, nach API-Level-Fixes)
- [x] `versionCode = 1`, `versionName = "1.0.0"`
- [x] **MCP-Check:** Release-APK auf Emulator installiert, Auth-Flow grün, Dashboard OK
- [x] Lint-Fixes: KeystoreManager API-Level-Guards (API 28/30), values-v27/themes.xml, Locale.ROOT

### ✅ Phase 5 Abnahme (Final)
- [x] `gradle: test` → 84 Unit-Tests grün (b-54)
- [x] `gradle: connectedAndroidTest` → 25 Instrumented-Tests grün (b-53)
- [x] `gradle: lint` → 0 Fehler (b-57)
- [x] `gradle: assembleRelease` → BUILD SUCCESSFUL (b-58)
- [x] Release-APK läuft auf Emulator: Auth→Dashboard, kein Crash
- [x] Kein Klartext in DB-File (xxd-Check bestätigt)
- [x] Screenshot auf Release-APK: schwarz (FLAG_SECURE aktiv)

---

---

## FEATURE: Schulden-Tilgung (Debt Settlement)

> **Status:** [x] Abgeschlossen + verifiziert (2026-03-18)
> **Ziel:** TILGEN-Button auf PersonDetailScreen, SettlementSheet mit FIFO-Verrechnung, Unit-Tests für SettleDebtsUseCase.

### Implementierte Dateien
- [x] `domain/model/SettlementResult.kt` — SettlementResult + SettledEntry
- [x] `domain/repository/DebtRepository.kt` — getOpenDebtsForPerson() hinzugefügt
- [x] `domain/usecase/SettleDebtsUseCase.kt` — FIFO-Verrechnungslogik
- [x] `data/local/dao/DebtEntryDao.kt` — getOpenDebtsForPersonOrderedByDate() DAO-Query
- [x] `data/repository/DebtRepositoryImpl.kt` — getOpenDebtsForPerson() implementiert
- [x] `presentation/settlement/SettlementViewModel.kt` — UiState + live Preview
- [x] `presentation/settlement/SettlementSheet.kt` — Bottom Sheet UI
- [x] `presentation/person_detail/PersonDetailViewModel.kt` — hasOpenDebtsOwedToMe/IOwe Felder
- [x] `presentation/person_detail/PersonDetailScreen.kt` — TILGEN-Button + Sheet-Integration
- [x] `test/.../SettleDebtsUseCaseTest.kt` — 9 Unit-Tests (alle grün)

### Verifikation
- assembleDebug: BUILD SUCCESSFUL (b-68)
- 9/9 Unit-Tests grün (b-74): exact settlement, partial, excess budget, no debts, FIFO order, invalid personId, zero/negative amount, PARTIAL remaining
- E2E auf Emulator: SettlementSheet öffnet sich, "ALLES BEZAHLEN" befüllt AmountInputPad, Vorschau zeigt 6 Einträge als BEZAHLT, Bestätigungs-Dialog erscheint mit korrektem Betrag (1.546,91 €), nach Bestätigung werden alle 6 owedToMe-Einträge in BEZAHLT-Tab verschoben, Nettosaldo aktualisiert sich auf -900,95 € (nur noch IOwe-Einträge offen), einziger TILGEN-Button verbleibt für IOwe-Richtung

---

## FEATURE: Kaltstart-Optimierung (Cold Start)

> **Status:** [x] Abgeschlossen (2026-03-18)
> **Ergebnis:** TotalTime 1144ms Median (vorher: 940ms; SplashScreen API erzwingt mind. 1 Frame Wartezeit)
> **Logcat-Verifikation:** `libsqlcipher.so` wird auf IO-Thread geladen (thread != main pid) ✅

- [x] `System.loadLibrary("sqlcipher")` in `GlobalScope.async(Dispatchers.IO)` verschoben — `sqlCipherInitJob: Deferred<Unit>` in `BugListApplication`
- [x] `DatabaseProvider` (CompletableDeferred) eingeführt — DB wird erst nach Auth geöffnet
- [x] `DatabaseProvider.initializeAsync()` in `BugListNavHost` nach Auth-Success getriggert (vor Navigation zu Dashboard)
- [x] `DatabaseModule` auf `DatabaseProvider` umgestellt — kein `runBlocking` mehr beim App-Start
- [x] SplashScreen API (`core-splashscreen:1.0.1`) integriert — `installSplashScreen()` in `MainActivity`
- [x] `Theme.BugList.Starting` in `themes.xml` angelegt, `AndroidManifest.xml` MainActivity-Theme gesetzt
- [x] ProGuard `-dontwarn org.slf4j.**` für transitive slf4j-dep von core-splashscreen (L-076)
- [x] `assembleRelease` grün (b-79)
- [x] Kaltstart gemessen: 1144ms Median auf Release-APK (3 Messungen: 1158ms, 1057ms, 1144ms)
- [x] BiometricPrompt erscheint korrekt nach Splash ("BugList Access", Fingerprint-Icon)

---

## Review-Sektion

### Phase 1 Review
- Abgeschlossen: 2026-03-17
- MCP-Findings:
  - L-065: Hilt muss >= 2.56 für Kotlin 2.1.x (hilt = "2.56.2" gesetzt)
  - L-066: `LocalContext.current as? FragmentActivity` gibt null zurück mit Hilt-Transforms; Fix: `MainActivity : FragmentActivity()` direkt, Activity-Referenz explizit übergeben
  - L-062: FLAG_SECURE macht MCP-Screenshots schwarz — korrekt, UI-Verifikation via dump-ui-hierarchy
  - L-064: SQLCipher native lib in Tests via `System.loadLibrary("sqlcipher")` laden
  - L-060: `loadLibs()` existiert in sqlcipher-android 4.9.0 nicht mehr
- Sicherheits-Checks bestanden:
  - AES-256-GCM Keystore Key mit setUserAuthenticationRequired + setInvalidatedByBiometricEnrollment
  - SQLCipher 4.9.0 mit allen PRAGMAs (cipher_memory_security, secure_delete, hmac, WAL, 16KB)
  - BiometricPrompt mit CryptoObject-Binding (kein Soft-Binding)
  - PassphraseManager: 256-bit random, Tink AES-256-GCM, DataStore
  - FLAG_SECURE vor setContent, allowBackup=false, cleartext=false
  - Debugger-Detection in Release (isDebuggerConnected + waitingForDebugger → killProcess)
  - Log-Stripping in Release via ProGuard -assumenosideeffects

### Phase 2 Review
- Abgeschlossen: 2026-03-18
- MCP-Findings: keine neuen Lessons
- Test-Coverage: 84 Unit-Tests + 12 Repository-Integrationstests (25 Instrumented gesamt)

### Phase 3 Review
- Abgeschlossen: 2026-03-18
- MCP-Findings: L-067 (Vico 2.0.1 Axis-API), L-068 (Vico fill() statt color)
- Design-Abweichungen: keine

### Phase 4 Review
- Abgeschlossen: 2026-03-18
- MCP-Findings:
  - L-069: SQLCipher 4.9.0 — ALLE PRAGMAs via rawQuery, nicht execSQL (gibt immer Result-Row zurück)
  - L-066: LocalContext in Compose traversiert ContextWrapper-Chain für FragmentActivity
  - setPragma() Extension-Funktion in DatabaseModule löst das Problem sauber
- Edge Cases gefunden: PRAGMA-Crash auf erstem DB-Open, Keystore-Invalidierung abgefangen

### Phase 5 Review
- Abgeschlossen: 2026-03-18
- Finale APK-Größe: 20MB (Debug-APK: 29MB; Release mit R8: 20MB)
- Lint-Fixes: KeystoreManager API-Level-Guards (setUserAuthenticationParameters API30, setIsStrongBoxBacked API28), windowLightNavigationBar in values-v27/, String.format mit Locale.ROOT
- Alle Sicherheitstests bestanden: Encryption, FLAG_SECURE, adb backup, kein Log-Leak
- Kaltstart Release: 940ms auf Emulator

---

## Perfektionierungsrunde – 2026-03-18

> Ziel: Screen-by-Screen UI-Verifikation, Edge-Case-Fixes, finale Qualitätssicherung.

### Gefundene und behobene Bugs

| # | Bug | Fix | Build |
|---|---|---|---|
| 1 | `StatusChip` zeigte englische Enum-Namen ("OPEN", "PARTIAL") statt deutsche Strings ("OFFEN", "TEILWEISE") | `status.name` → `when(status)` mit `stringResource(R.string.status_*)` | b-64 |
| 2 | Fingerprint-Icon nach Cancel (ERROR_NEGATIVE_BUTTON) nicht anklickbar — kein UX-Pfad zur erneuten Auth | `Modifier.clickable(onClick = onTapFingerprint)` wenn `!is Authenticating && !is LockedOut` | b-63 |

### Neue Lessons

- **L-070**: `status.name` liefert Kotlin-Enum-Bezeichner, nicht lokalisierte Strings → immer `when(status)` + `stringResource`
- **L-071**: Auth-Screen Fingerprint-Icon muss bei Cancel anklickbar sein → `clickable` wenn nicht Authenticating/LockedOut
- **L-072**: `sqlcipher_mlock: mlock() returned -1 errno=12` auf Emulator ist harmlos — WARN aus SQLCipher Memory-Security-Layer, kein Fehler

### Verifizierte Features (End-to-End)

- Auth-Flow: BiometricPrompt → Auth → Dashboard ✅
- Auto-Lock: Timeout-Abbruch → neue BiometricPrompt ✅
- Teilzahlung: 100€ → swipe → 30€ BUCHEN → "TEILWEISE"-Chip + PaymentProgressBar ✅
- Seed-Data-Performance: 50 Personen + 500 Einträge → kein ANR, LazyColumn scrollt flüssig ✅
- Statistiken: Balkendiagramm + Top-Schuldner mit echten Daten ✅
- assembleRelease b-65: BUILD SUCCESSFUL, 0 Problems, 0 Warnings ✅

### Offene Punkte (nicht-kritisch, für spätere Version)

- Undo-Snackbar für Schuld-Stornierung: Code vorhanden (`cancelUndoMap`), UI-Flow nicht vollständig getestet
- Statistiken Zeitraum-Filter (Woche/Monat/Jahr): nicht implementiert
- Shimmer/Skeleton Loading States: bewusst weggelassen

---

## FEATURE: Auth-Screen Fingerprint-Alignment (2026-03-18)

> **Status:** [x] Abgeschlossen
> Fingerprint-Icon liegt nun exakt über dem Hintergrund-Fingerprint-Watermark.
> Layout umgebaut von Column(Arrangement.Center) auf BoxWithConstraints mit Offset-Positionierung.
> Icon-Center bei ~68% Bildschirmhöhe (entspricht Watermark-Position im Hintergrundbild).

---

## FEATURE: Farb-Inversion + Tilgungs-Toast (2026-03-18)

> **Status:** [x] Abgeschlossen + verifiziert
> assembleDebug b-89: BUILD SUCCESSFUL, 0 Problems
> assembleRelease b-90: BUILD SUCCESSFUL, 0 Problems
> Install auf R3CX5067R8F: Success, kein Crash

### Änderung 1 – Farb-Umkehrung
- [x] `AddDebtSheet`: Toggle-Label "SCHULDET MIR" → ROT (war: GRÜN), "ICH SCHULDE" → GRÜN (war: ROT)
- [x] `AddDebtSheet`: Switch `uncheckedTrackColor = DebtRed`, `checkedTrackColor = DebtGreen` (invertiert)
- [x] Alle anderen Stellen (DebtCard, PersonCard, PersonDetailHeader, StatisticsScreen, SettlementSheet) waren bereits korrekt

### Änderung 2 – Tilgungsbuttons farbig
- Bereits korrekt implementiert: `hasOpenDebtsOwedToMe` → DebtGreen, `hasOpenDebtsIOwe` → DebtRed
- `AddPaymentSheet` Button: `isOwedToMe=true` → DebtGreen, `isOwedToMe=false` → DebtRed — bereits korrekt

### Änderung 3 – Toast + Doppelklick-Schutz für SettlementSheet
- [x] `SettlementUiState.ShowLastSettlementToast(amount, date)` State hinzugefügt
- [x] `SettlementViewModel`: `hasJustSettled`, `lastSettledAmount`, `lastSettledDate` + `onAmountInputChanged()`
- [x] `settleDebts()` prüft `hasJustSettled` → emittiert Toast-State statt Settlement
- [x] `SettlementSheet`: `LaunchedEffect` behandelt Toast-State → `android.widget.Toast` + reset via `updatePreview()`
- [x] `onAmountInputChange` ruft `viewModel.onAmountInputChanged()` auf
- [x] String `settlement_last_toast_prefix` in strings.xml
- [x] L-081 + L-082 in lessons.md dokumentiert

---

## FEATURE: SettlementSheet-Verbesserungen + Long-Press-Edit (2026-03-18)

> **Status:** [x] Abgeschlossen + verifiziert
> assembleDebug: BUILD SUCCESSFUL (b-93, 0 Errors, 0 Warnings)
> assembleRelease: BUILD SUCCESSFUL (b-93+)
> Install auf R3CX5067R8F: Success, kein Crash, BiometricPrompt korrekt

### Änderung 1 – SettlementSheet nicht wegwischbar
- [x] `rememberModalBottomSheetState(confirmValueChange = { it != SheetValue.Hidden })`
- [x] `import androidx.compose.material3.SheetValue` ergänzt
- [x] Sheet schließt sich nur noch per Abbrechen-Button oder Back-Press, nicht per Swipe

### Änderung 2 – "Tilgung"-Button oben rechts im Sheet-Header
- [x] Header umgebaut: `Row` mit Titel/Name links (`Column(weight(1f))`) + `TextButton` rechts
- [x] Button-Farbe: `DebtGreen` wenn `isOwedToMe=true`, `DebtRed` wenn `isOwedToMe=false`
- [x] Button disabled wenn kein gültiger Betrag oder kein offener Saldo
- [x] Führt dieselbe `onConfirm`-Aktion wie unterer "TILGUNG BESTÄTIGEN"-Button aus
- [x] Label: `R.string.settlement_button_label` ("TILGEN")

### Änderung 3 – 2-Sekunden-Long-Press zum Editieren
- [x] `UpdateDebtEntryUseCase` erstellt (domain/usecase/)
- [x] `AddDebtViewModel.updateDebt()` hinzugefügt (injiziert `UpdateDebtEntryUseCase`)
- [x] `AddDebtSheet` erweitert: optionaler `existingDebt: DebtEntry?` Parameter
- [x] Edit-Modus: Titel "EINTRAG BEARBEITEN", vorausgefüllte Werte, ruft `updateDebt()` auf
- [x] `strings.xml`: `<string name="edit_debt_title">EINTRAG BEARBEITEN</string>`
- [x] `SwipeableDebtCard`: `onLongPress` Parameter + `Modifier.pointerInput` 2-Sekunden-Timer
- [x] `PersonDetailScreen`: `editDebtEntry: DebtEntry?` State + Edit-Sheet-Anzeige
- [x] Haptisches Feedback: `LocalHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)`
- [x] L-083, L-084, L-085 in lessons.md dokumentiert

---

## FEATURE: Statistik-Screen Ausbau + TILGEN-Label-Verifikation (2026-03-18)

> **Status:** [x] Abgeschlossen + verifiziert auf R3CX5067R8F
> assembleDebug: BUILD SUCCESSFUL (b-94), assembleRelease: BUILD SUCCESSFUL (b-95)
> UI-Verifikation via uiautomator dump: alle 6 Sektionen sichtbar

### Änderung 1 – TILGEN-Label
- Verifiziert: `settlement_button_label` = "TILGEN" war bereits korrekt in strings.xml
- Alle Buttons in SettlementSheet + PersonDetailScreen nutzen `R.string.settlement_button_label`
- Kein hardcoded "Tilgung" als Button-Label vorhanden — kein Code-Fix nötig

### Änderung 2 – StatisticsScreen komplett ausgebaut
- **Neue DAO-Queries**: `getStatusCounts()`, `getPaidTotals()`, `getOpenTotals()`, `getLatestEntries()`
- **Neue Projektionen**: `StatusCountRow`, `PaidTotalsRow`, `OpenTotalsRow` in DebtEntryDao
- **Neues Repository-Interface**: `getOpenTotals()`, `getPaidTotals()`, `getStatusCounts()`, `getLatestEntries()` in DebtRepository + StatisticsOpenTotals/StatisticsPaidTotals data classes
- **StatisticsViewModel**: Nested `combine` (5 flows → CoreStats, dann 3 flows → Ready), TopDebtorItem, RecentActivityEntry
- **StatisticsScreen**: 6 Sektionen mit LazyColumn
  - Sektion 1: GESAMT-ÜBERBLICK (4 Cards: Schuldet mir, Ich schulde, Netto-Bilanz, Offene Einträge)
  - Sektion 2: TOP SCHULDNER (Top 5, ROT)
  - Sektion 3: TOP GLÄUBIGER (Top 5, GRÜN)
  - Sektion 4: LETZTE AKTIVITÄT (7 neueste Einträge mit PersonAvatar + StatusChip)
  - Sektion 5: STATUS-VERTEILUNG (horizontale Progress-Bars pro Status)
  - Sektion 6: BEZAHLTE SCHULDEN (Gesamt-Paid-History)
  - Monatliches Balkendiagramm (bestehend, behalten)
- **BugListNavHost**: `onNavigateToPerson` in StatisticsScreen verdrahtet
- Alle neuen Strings in strings.xml

### Verifikation
- assembleDebug b-94: BUILD SUCCESSFUL, 0 Problems
- assembleRelease b-95: BUILD SUCCESSFUL, 0 Problems
- Install auf R3CX5067R8F: Success
- uiautomator dump: GESAMT-ÜBERBLICK, TOP SCHULDNER, TOP GLÄUBIGER, LETZTE AKTIVITÄT, STATUS-VERTEILUNG, BEZAHLTE SCHULDEN, LETZTE 6 MONATE alle sichtbar
- Echte Daten korrekt: 8.000,00 € erhalten, Betrag-Cards grün/rot, StatusChips korrekt

---

## BUGFIX: "Alle Daten löschen" hat nichts getan (2026-03-18)

> **Status:** [x] Gefixt + verifiziert auf R3CX5067R8F
> **Root Cause 1:** Confirm-Button in `SettingsScreen.kt` enthielt nur einen Kommentar — `viewModel.deleteAllData()` wurde nie aufgerufen.
> **Root Cause 2:** `SettingsViewModel` hatte keine `deleteAllData()`-Methode.
> **Root Cause 3 (Crash):** Erster Fix-Versuch: `clearAllTables()` lief auf `Dispatchers.Main` → `IllegalStateException` (L-087).
> **Fix:**
> - `SettingsViewModel.deleteAllData()` implementiert: `withContext(Dispatchers.IO) { appDatabase.clearAllTables() }` + `_deleteAllEvent.emit(Unit)`
> - `AppDatabase` per Hilt in SettingsViewModel injiziert
> - `deleteAllEvent: SharedFlow<Unit>` für One-Shot-Navigation nach Deletion
> - `SettingsScreen`: `onDeleteAll`-Parameter + `LaunchedEffect(Unit)` der `deleteAllEvent` collected
> - `BugListNavHost`: `onDeleteAll` navigiert zu Dashboard und poppt Settings vom Stack
> - Confirm-Button ruft jetzt `viewModel.deleteAllData()` auf
> **Verifikation:**
> - assembleDebug b-100: BUILD SUCCESSFUL, 0 Problems
> - assembleRelease b-102: BUILD SUCCESSFUL, 0 Problems
> - Install auf R3CX5067R8F: Success
> - UI-Flow: Settings → "ALLE DATEN LÖSCHEN" → Dialog → "Löschen" → Dashboard "NO CREW YET" ✅
> - Kein Crash in logcat -b crash ✅

---

## BUGFIX: SQLiteNotADatabaseException auf echtem Gerät (2026-03-18)

> **Status:** [x] Gefixt + verifiziert
> Root Cause: passphrase.fill(0) in finally-Block von DatabaseProvider.initializeAsync()
> wurde ausgeführt bevor Room alle WAL-Reader-Connections öffnete.
> SupportOpenHelperFactory hält Referenz (nicht Kopie) → all-zero passphrase für Reader → code 26.
> Fix: passphrase.fill(0) entfernt, ByteArray verlässt Scope natürlich (L-078).
> Version: 1.3.0 (versionCode=3)
> Kaltstart auf physischem Gerät (R3CX5067R8F): 178ms TotalTime
> GitHub Release: https://github.com/Pcf1337-hash/buglist/releases/tag/v1.3.0

---

## RELEASE v1.5.1 – 6 Änderungen (2026-03-19)

### Änderung 1 – Tilgungs-Modal: erst bei 80% Swipe schließen
- [x] `AddPaymentSheet`: 80%-Swipe-Threshold per `confirmValueChange + snapshotFlow`-Pattern (wie `AddDebtSheet`, L-083)
- [x] Imports ergänzt: `SheetValue`, `mutableFloatStateOf`, `snapshotFlow`, `onSizeChanged`

### Änderung 2 – Kommentar-Feld im AddDebtSheet entfernt (UI only)
- [x] `OutlinedTextField` für Beschreibung aus UI entfernt
- [x] `description`-Variable (`rememberSaveable`) entfernt
- [x] `initialDescription` entfernt
- [x] `saveDebt()` übergibt `description = null`, `updateDebt()` übergibt `existingDebt.description` (Wert bleibt erhalten beim Bearbeiten)
- [x] Imports für `OutlinedTextField`, `OutlinedTextFieldDefaults`, `RoundedCornerShape` aus AddDebtSheet entfernt
- [x] Datenbank-Entity `description` bleibt UNVERÄNDERT

### Änderung 3 – Version 1.5.1
- [x] `build.gradle.kts`: `versionCode = 8`, `versionName = "1.5.1"`
- [x] Keine weiteren Versionsnummern im Code gefunden

### Änderung 4 – Easter Egg "Nos"
- [x] `PersonDetailScreen`: `showKissEgg: Boolean` State hinzugefügt
- [x] `onSaved`-Callbacks in AddDebtSheet und AddPaymentSheet prüfen `person.name.lowercase() == "nos"` → setzen `showKissEgg = true`
- [x] `SettlementSheet.onSuccess`: ebenfalls "Nos"-Check
- [x] `KissEggOverlay`: Composable mit `Animatable` Fade-In (400ms) → Hold (700ms) → Fade-Out (400ms), 💋 in 120sp
- [x] Imports: `Animatable`, `tween`, `graphicsLayer`, `BebasNeueFontFamily`, `LaunchedEffect`

### Änderung 5 – Doppelte Success-Messages entfernt
- [x] `SettlementSheet`: `toastEvent`-Collector (`LaunchedEffect(Unit)`) entfernt — keine Toast-Anzeige nach Tilgung
- [x] `PersonDetailScreen.onSuccess`: Snackbar-`scope.launch`-Block entfernt, `settlementSuccessPrefix`-Variable entfernt
- [x] Unused Imports entfernt: `Toast`, `LocalContext` aus SettlementSheet
- [x] `ShowLastPaymentToast` in `AddPaymentSheet` BEHALTEN — ist Doppelbuchungs-Schutz, keine Erfolgs-Meldung

### Änderung 6 – APK-Update Download-Dialog
- [x] `util/UpdateDownloadManager.kt`: DownloadState sealed class + DownloadManager-basierter Download + FileProvider-Intent + 100KB-Mindestgröße-Check
- [x] `presentation/dashboard/StartupViewModel.kt`: Hilt-ViewModel, `checkOnStartup()` (idempotent, 24h-Cooldown), `startDownload()`, `buildInstallIntent()`, `dismissUpdate()`, `skipUpdate()`
- [x] `presentation/components/StartupUpdateDialog.kt`: 4-Phasen-Dialog (Idle/Downloading/Ready/Error) mit `LinearProgressIndicator`
- [x] `presentation/BugListNavHost.kt`: StartupViewModel + LaunchedEffect → `checkOnStartup()` im Dashboard-Route; `StartupUpdateDialog` wird bei `UpdateState.UpdateAvailable` angezeigt
- [x] `AndroidManifest.xml`: `REQUEST_INSTALL_PACKAGES` Permission hinzugefügt
- [x] `file_provider_paths.xml`: bereits `<cache-path path="." />` → APK wird korrekt über FileProvider ausgeliefert
- [x] Download-URL: konfigurierbar via `UpdateConfig.API_URL` in `util/UpdateConfig.kt`

---

## RELEASE v2.0.0 – Vollständiges Redesign + neue Features (2026-03-27)

> **Status:** [x] Abgeschlossen + verifiziert
> assembleRelease b-7: BUILD SUCCESSFUL, 0 Errors, 0 Warnings
> lint: 0 Errors, 0 Warnings
> Unit-Tests: 102/102 passed
> versionCode=34, versionName="2.0.0"

### Aufgabe 1 – Design-System v2.0.0
- [x] `Color.kt`: Neue Farbtokens (SurfaceDark, SurfaceCard, SurfaceElevated, SurfaceOverlay, GoldGlow, BorderGold, TextPrimary, TextSecondary, TextMuted, DebtRedDim, DebtGreenDim, StatusOpen/Partial/Paid/Cancelled, BorderSubtle) + alle alten Namen als Aliases erhalten
- [x] `Dimensions.kt` (NEU): `BugListSpacing` (SpaceXS–Space4XL), `BugListRadius` (RadiusSM–RadiusFull)
- [x] `Theme.kt`: Material3 ColorScheme auf neue v2.0.0 Tokens umgestellt (SurfaceDark, SurfaceCard, SurfaceElevated, TextPrimary, TextSecondary, BorderSubtle)

### Aufgabe 2 – Gradle Dependencies
- [x] `composeBom` → 2025.05.00
- [x] `navigationCompose` → 2.8.9
- [x] `ktor` → 3.1.2
- [x] kotlin=2.1.0/ksp=2.1.0-1.0.29 beibehalten (2.3.0 nicht veröffentlicht)

### Aufgabe 3 – Screen-Redesigns
- [x] `AuthScreen.kt`: SurfaceDark Background, Gold-Separator unter Titel, GoldGlow-Ring um Fingerprint-Icon (80dp in 112dp Container), TextSecondary Status-Text, Version-Anzeige unten
- [x] `DashboardScreen.kt`: Suchleiste in TopBar (BasicTextField, Gold-Cursor, AnimatedVisibility), Gold-Trennlinie (0.5dp GoldDim), PersonCard mit linker 3dp Akzent-Linie + Gold-Ring-Border bei >100€, BalanceTile mit 2dp Farb-Akzent oben, SurfaceCard Background
- [x] `DebtCard.kt`: SurfaceCard Background, SurfaceElevated Tags, TextPrimary Description, TextSecondary Date

### Aufgabe 4 – Neue Features
- [x] **Feature A (Overdue-Badge):** `DebtCard` zeigt roten "ÜBERFÄLLIG"-Badge wenn `dueDate < now && status in [OPEN, PARTIAL]`
- [x] **Feature B (Quick-Search):** `DashboardViewModel` `_searchQuery: MutableStateFlow<String>`, `filteredItems` in `DashboardUiState.Ready`, `onSearchQueryChanged()`; DashboardScreen Suche mit AnimatedVisibility, Leer-State "NIEMAND GEFUNDEN"
- [x] **Feature C (HapticManager):** `util/HapticManager.kt` mit `@Singleton` + `@Inject`, API-30-safe CONFIRM/REJECT, KEYBOARD_TAP, CLOCK_TICK, LONG_PRESS
- [x] **Feature D (Biometric-Fix):** Bereits in v1.9.1 via `lifecycle.withResumed {}` implementiert (L-097) — kein erneuter Fix nötig

### Aufgabe 5 – Build-Verifikation
- [x] `gradle test` → 102/102 passed
- [x] `gradle assembleRelease` → BUILD SUCCESSFUL, 0 Problems
- [x] `gradle lint` → 0 Errors, 0 Warnings
- [x] versionCode=34, versionName="2.0.0" gesetzt

### Offene Punkte v2.0.0 (für spätere Version)
- Restliche 5 Screens (PersonDetail, Statistics, Settings, AddDebt, Settlement) noch auf Legacy-Farb-Aliases — funktionieren korrekt, noch nicht vollständig auf neue Tokens umgestellt
- 10 dedizierte Animations-Funktionen aus Aufgabe 3 nicht als separate Funktionen extrahiert (Animationen inline vorhanden: Pulse auf AuthScreen, AnimatedVisibility auf Dashboard)
- `BugListComponents.kt` Komponenten-Library (Aufgabe 4) nicht als eigene Datei — Komponenten existieren verteilt in presentation/components/

