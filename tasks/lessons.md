# lessons.md – BugList

> Vor jeder Session lesen.
> Nach JEDER Nutzer-Korrektur oder MCP-Fehler sofort updaten.
> Format: Problem → Ursache → Regel (+ Code wenn relevant)

---

## Sicherheit & Biometrie

### L-088 – Samsung Galaxy A Series: BIOMETRIC_STRONG schlägt fehl (Class 2 Sensor)
**Problem:** Galaxy A-Geräte zeigen "Biometric Hardware unavailable: NONE_ENROLLED" obwohl Fingerabdruck eingerichtet ist.
**Ursache:** Samsung Galaxy A Fingerprint-Sensoren sind Class 2 (BIOMETRIC_WEAK), nicht Class 3 (BIOMETRIC_STRONG). `canAuthenticate(BIOMETRIC_STRONG)` gibt `BIOMETRIC_ERROR_NONE_ENROLLED` zurück, weil keine Class-3-Biometrie existiert – auch wenn Fingerabdrücke registriert sind.
**Regel:** Immer Fallback auf `BIOMETRIC_WEAK or DEVICE_CREDENTIAL` implementieren. CryptoObject funktioniert NICHT mit dieser Kombination (Android-Einschränkung). In BugList ist Biometrie ein Gate – die DB-Passphrase wird von Tink/PassphraseManager unabhängig geschützt, daher ist `SuccessNoCipher` (kein CryptoObject) sicher.
```kotlin
// RICHTIG: Fallback-Priorität
when {
    isBiometricAvailable() -> authenticateStrong(...)   // BIOMETRIC_STRONG + CryptoObject
    isFallbackAvailable()  -> authenticateFallback(...) // BIOMETRIC_WEAK or DEVICE_CREDENTIAL, kein CryptoObject
    else -> onResult(AuthResult.HardwareUnavailable(...))
}
// Fallback-Prompt: KEIN setNegativeButtonText() wenn DEVICE_CREDENTIAL inkludiert
BiometricPrompt.PromptInfo.Builder()
    .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
    // KEIN .setNegativeButtonText() – DEVICE_CREDENTIAL hat eigenen Cancel-Button
    .build()
```

---

## Compose & UI

### L-037 – pointerInput(Unit) + stale Lambda → Backspace löscht nur einmal
**Problem:** `BackspaceButton` nutzte `pointerInput(Unit)` — nach dem ersten Löschen passierte nichts mehr.
**Ursache:** `pointerInput(Unit)` startet den Block NIE neu, weil der Key sich nie ändert. Die `onTap`-Lambda schloss über den `inputString` der ERSTEN Composition — alle folgenden Taps riefen dieselbe veraltete Lambda auf und produzierten denselben Output.
**Regel:** Wenn `pointerInput` mit stabilem Key (z.B. `Unit`) kombiniert wird mit einer Lambda, die state aus dem Composable-Scope liest: IMMER `rememberUpdatedState` verwenden.
```kotlin
// FALSCH – onTap sieht immer den ersten inputString
.pointerInput(Unit) {
    detectTapGestures(onTap = { onTap() })
}

// RICHTIG – currentOnTap zeigt immer auf die neueste Lambda
val currentOnTap by rememberUpdatedState(onTap)
.pointerInput(Unit) {
    detectTapGestures(onTap = { currentOnTap() })
}
```

### L-039 – SQLCipher @Transaction + Room InvalidationTracker → Übersicht aktualisiert sich nicht nach Tilgen
**Problem:** Nach Tilgen (Settlement) blieben Betrag und Status in PersonDetailHeader unverändert bis Expand/Collapse oder App-Neustart.
**Ursache:** `insertPaymentAndUpdateStatus` ist eine `@Transaction suspend fun` in `PaymentDao`. SQLCipher's eigenes Transaction-Handling notifiziert Room's `InvalidationTracker` nicht zuverlässig für alle beteiligten Tabellen. Obwohl `debt_entries` und `payments` beide geschrieben werden, triggert die invalidation nicht immer. In normaler Room+SQLite-Kombination funktioniert es; mit SQLCipher 4.9.0 + SupportOpenHelperFactory gibt es edge cases.
**Regel:** Wenn Writes über `@Transaction suspend fun` in DAOs mit SQLCipher gemacht werden, füge immer einen expliziten Flow-Trigger als `combine`-Parameter hinzu – analog zum Tag-Fix (L-038). Nutze `PaymentRepository.observePaymentChanges()` (basiert auf `getPaymentCount(): Flow<Int>`).
```kotlin
// FALSCH – Room InvalidationTracker kann nach SQLCipher-Transaction den Flow nicht neu triggern
combine(personRepo.get(), _tab, _expandedId, tagRepo.getAllTags()) { ... }

// RICHTIG – payment change als expliziter 5. Trigger
combine(personRepo.get(), _tab, _expandedId, tagRepo.getAllTags(), paymentRepo.observePaymentChanges()) { p, t, e, _, _ -> ... }
```

### L-041 – SQLCipher @Transaction + Room InvalidationTracker → Liste aktualisiert sich nicht nach Debt-Eintrag erstellen/bearbeiten
**Problem:** Nach dem Erstellen oder Bearbeiten eines Schulden-Eintrags musste man zwischen Tabs wechseln damit der Eintrag in der Liste erscheint.
**Ursache:** Gleiche SQLCipher-InvalidationTracker-Lücke wie L-039, aber diesmal für die `debt_entries`-Tabelle selbst. Room notifiziert `@Transaction @Query`-Flows mit `@Relation` nach direkten Inserts/Updates via SQLCipher nicht zuverlässig.
**Regel:** Füge `observeDebtEntryChanges()` (basiert auf `getDebtEntryCount(): Flow<Int>` im DAO) als expliziten Trigger hinzu. Da `combine()` auf 5 Flows begrenzt ist, Payment- und Debt-Trigger via `merge()` zusammenfassen.
```kotlin
// DebtEntryDao
@Query("SELECT COUNT(*) FROM debt_entries")
fun getDebtEntryCount(): Flow<Int>

// DebtRepositoryImpl
override fun observeDebtEntryChanges(): Flow<Unit> =
    debtEntryDao.getDebtEntryCount().mapLatest { }

// PersonDetailViewModel — 5 Flows in combine(), zwei Trigger via merge()
val uiState = combine(
    personRepository.getPersonById(personId),
    _activeTab,
    _expandedDebtId,
    tagRepository.getAllTags(),
    merge(
        paymentRepository.observePaymentChanges(),  // L-039
        debtRepository.observeDebtEntryChanges()    // L-041
    )
) { person, tab, expandedId, _, _ -> Triple(person, tab, expandedId) }
    .flatMapLatest { ... }
```

### L-038 – suspend fun statt Flow für Tags → UI aktualisiert sich nicht live
**Problem:** Tags, die einem Debt-Eintrag zugewiesen wurden, erschienen erst nach Collapse/Expand oder App-Neustart in der Übersicht.
**Ursache:** `tagRepository.getTagsForDebtEntry()` ist eine `suspend fun` — kein Flow. Sie wird nur aufgerufen, wenn `debtRepository.getDebtEntriesWithPaymentsForPerson()` ein neues Item emittiert. Da Tag-Zuordnungen in einer Cross-Ref-Tabelle gespeichert sind (nicht in `debt_entries`), triggert eine neue Tag-Zuordnung kein Re-Emit des Debt-Flows.
**Regel:** Immer `tagRepository.getAllTags()` als zusätzlichen `combine`-Parameter in ViewModels verwenden, die Tags anzeigen. Das sorgt dafür, dass jede Tag-Änderung die gesamte Enrichment-Pipeline neu auslöst.
```kotlin
// FALSCH – tags werden nur beim debt-emit geladen
combine(personRepo.get(), _tab, _expandedId) { ... }

// RICHTIG – tags lösen ebenfalls ein Re-Emit aus
combine(personRepo.get(), _tab, _expandedId, tagRepo.getAllTags()) { p, t, e, _ -> ... }
```

---

## Workflow & MCP

### L-000 – MCP nach jedem Task nutzen, nicht nur bei Fehlern
**Problem:** MCP-Tools nur reaktiv genutzt wenn etwas crasht.
**Ursache:** "Es sieht gut aus" ohne zu verifizieren.
**Regel:** Nach JEDEM Task: Gradle build → ADB install → Logcat beobachten → Screenshot. Kein Task ist done ohne MCP-Verifikation.

### L-001 – Gradle MCP Fehler direkt fixen, nicht weitermachen
**Problem:** Compile-Fehler ignoriert und nächsten Task angefangen.
**Ursache:** Wollte Fortschritt sehen.
**Regel:** Gradle-Fehler blockieren alles. Datei + Zeile aus Gradle MCP nehmen, sofort fixen, retry. Erst wenn Build grün: weitermachen.

### L-002 – Logcat beobachten beim ersten App-Start nach Änderung
**Problem:** Crash nach Installation nicht sofort bemerkt.
**Ursache:** App gestartet, Screenshot gemacht, weitergemacht ohne Logcat.
**Regel:** App-Start IMMER mit `android-toolkit: logcat` (Tag=BugList, min. 30 Sekunden) begleiten. Crash = sofortiger Stopp und Fix.

### L-003 – Screenshot für jeden neuen Screen
**Problem:** Design-Fehler erst viel später bemerkt.
**Ursache:** Nur Code-Review, kein visueller Check.
**Regel:** Nach jedem neuen Screen: `mobile-mcp: screenshot` aufnehmen und prüfen. Farben, Fonts, Layout – alles stimmt mit CLAUDE.md überein?

### L-004 – Plan vor Code
**Problem:** Task ohne klaren Plan angefangen → Architektur-Fehler mitten im Code.
**Ursache:** Direkt losgelegt.
**Regel:** Bei 3+ Schritten oder Architektur-Entscheidungen: 2-3 Sätze Plan schreiben bevor erste Zeile Code.

### L-005 – Einen Task auf einmal
**Problem:** Mehrere Tasks gleichzeitig geöffnet → Kontext vermischt.
**Regel:** Immer nur ein Task `[~]`. Erst verifizieren und `[x]` setzen, dann nächsten starten.

---

## Android / Kotlin

### L-010 – KSP statt KAPT
**Problem:** KAPT konfiguriert → deprecated Warning, langsamerer Build.
**Ursache:** Alte Tutorials.
**Regel:** Immer KSP.
```kotlin
// FALSCH
kapt("androidx.room:room-compiler:$roomVersion")
// RICHTIG
ksp("androidx.room:room-compiler:$roomVersion")
```

### L-011 – StateFlow in ViewModels, nicht mutableStateOf
**Problem:** `mutableStateOf` im ViewModel → Recomposition-Probleme, falsche Lifecycle-Bindung.
**Regel:** ViewModel → `MutableStateFlow` + `StateFlow`. Compose → `collectAsStateWithLifecycle()`.

### L-012 – FLAG_SECURE Reihenfolge
**Problem:** Screenshot trotz FLAG_SECURE möglich.
**Ursache:** Nach `setContent{}` gesetzt.
**Regel:**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.setFlags(FLAG_SECURE, FLAG_SECURE)  // VOR setContent{}
    setContent { /* ... */ }
}
```

### L-013 – Kein hardcoded String im UI
**Regel:** ALLE UI-Strings in `res/values/strings.xml`. Keine Ausnahme.

### L-014 – Dark Mode weißer Blitz
**Problem:** Kurzer weißer Blitz beim App-Start.
**Ursache:** Default windowBackground ist weiß.
**Regel:** `themes.xml`: `<item name="android:windowBackground">@color/background</item>` und `colors.xml`: `<color name="background">#0D0D0D</color>`.

---

## Sicherheit / Kryptografie

### L-020 – Passphrase niemals als String
**Problem:** String bleibt im Java-Heap bis GC, GC ist nicht deterministisch.
**Regel:**
```kotlin
// FALSCH
val passphrase = "mein-passwort"
// RICHTIG
val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
try { openDatabase(passphrase) } finally { passphrase.fill(0) }
```

### L-021 – Alte SQLCipher-Bibliothek
**Problem:** Alte Lib → `SupportFactory` (deprecated), kein 16KB-Page-Support.
**Regel:**
```kotlin
// FALSCH
implementation("net.zetetic:android-database-sqlcipher:4.5.4")
// RICHTIG
implementation("net.zetetic:sqlcipher-android:4.9.0")
```

### L-022 – EncryptedSharedPreferences deprecated
**Problem:** Seit April 2025 deprecated.
**Regel:** Ausschließlich Jetpack DataStore + Google Tink (`tink-android:1.18.0`).

### L-023 – StrongBox für Bulk-Crypto
**Problem:** StrongBox für DB-Entschlüsselung → 55× langsamer als TEE.
**Regel:** StrongBox NUR für Key-Generierung. Bulk-Crypto über TEE-gestützte Keys.

### L-024 – CryptoObject-Binding ist Pflicht
**Problem:** BiometricPrompt ohne CryptoObject → nur Software-Binding, bypassbar.
**Regel:** IMMER `BiometricPrompt.authenticate(prompt, CryptoObject(cipher))`.

### L-025 – ProGuard für SQLCipher
**Problem:** Release-Build crasht, R8 entfernt JNI-Klassen.
**Regel:** In `proguard-rules.pro`:
```
-keep class net.zetetic.** { *; }
-keep class net.sqlcipher.** { *; }
```

---

## Room / Datenbank

### L-030 – Migration nicht vergessen
**Problem:** Schema geändert ohne Migration → Crash bei bestehenden Installs.
**Regel:** `fallbackToDestructiveMigration()` NUR in Debug-Builds. Jede Schema-Änderung braucht `Migration`-Klasse.

### L-031 – Flow in DAOs für reaktive UI
**Regel:** Lesende Queries → `Flow<T>`. Schreiboperationen → `suspend fun`.

### L-032 – Indices deklarieren
**Regel:** Alle FK-Spalten und gefilterte Spalten in `@Entity(indices = [...])`:
```kotlin
@Entity(indices = [Index("personId"), Index("date"), Index("status")])
```

### L-033 – SupportOpenHelperFactory (nicht SupportFactory)
**Problem:** Alte API `SupportFactory` funktioniert nicht mit neuer sqlcipher-android Lib.
**Regel:**
```kotlin
// RICHTIG (neue Lib)
val factory = SupportOpenHelperFactory(passphrase)
Room.databaseBuilder(context, AppDatabase::class.java, "buglist.db")
    .openHelperFactory(factory)
    .build()
```

### L-034 – Teilzahlung darf remaining nicht überschreiten
**Problem:** User gibt Betrag > remaining ein → Schuld geht rechnerisch ins Negative.
**Ursache:** Keine Validierung gegen remaining.
**Regel:** `AddPartialPaymentUseCase` prüft `payment.amount <= entry.remaining` und gibt `Result.Error` zurück wenn nicht erfüllt. AmountInputPad setzt `maxValue = remaining` automatisch.

### L-035 – Status-Update nach Payment in einer Transaktion
**Problem:** Payment gespeichert aber Status bleibt OPEN → inkonsistenter Zustand.
**Ursache:** Status-Update vergessen oder als separater DB-Call.
**Regel:** Payment-Insert und Status-Update (OPEN→PARTIAL→PAID) immer in einer `@Transaction`-Funktion.
```kotlin
@Transaction
suspend fun insertPaymentAndUpdateStatus(payment: PaymentEntity, newStatus: DebtStatus) {
    insertPayment(payment)
    updateDebtStatus(payment.debtEntryId, newStatus)
}
```

### L-036 – AmountInputPad: nur ein Dezimalkomma
**Problem:** User kann mehrere Kommas eingeben → NumberFormatException beim Parsen.
**Ursache:** Kein Guard auf Komma-Duplikat.
**Regel:**
```kotlin
fun onDigitInput(input: String, digit: String): String {
    if (digit == "," && input.contains(",")) return input  // Komma ignorieren
    if (input.contains(",") && input.substringAfter(",").length >= 2) return input  // max 2 Nachkommastellen
    return input + digit
}
```

---

## Compose / UI

### L-040 – Recomposition durch Lambdas in Listen
**Problem:** Zu viele Recompositions → Jank bei langen Listen.
**Regel:** `remember { }` für Event-Handler-Lambdas in Listen-Items.

### L-041 – Material 3 DatePicker Dark Styling
**Problem:** DatePicker zeigt Light-Theme trotz Dark-App.
**Regel:** Eigenes `DatePickerColors`-Objekt mit Dark-Palette übergeben.

---

## Build / CI

### L-050 – Hilt in instrumented Tests
**Problem:** Tests schlagen fehl wegen fehlendem Hilt-Runner.
**Regel:**
```kotlin
testInstrumentationRunner = "dagger.hilt.android.testing.HiltTestRunner"
```

### L-051 – connectedAndroidTest braucht laufenden Emulator
**Problem:** Tests schlagen fehl weil kein Device verbunden.
**Regel:** Vor `gradle: connectedAndroidTest` immer `adb devices` prüfen → Emulator muss `online` zeigen.

---

### L-065 – Hilt muss >= 2.56 sein für Kotlin 2.1.x
**Problem:** `hiltJavaCompileDebug` schlägt fehl: `Unable to read Kotlin metadata due to unsupported metadata version`.
**Ursache:** Hilt 2.51.x kann die Kotlin 2.1.x Metadata-Version nicht lesen. Das Java-Annotation-Processing von Hilt liest Kotlin-Metadaten und erwartet das ältere Format.
**Regel:** Mit Kotlin 2.1.x muss Hilt >= 2.56 verwendet werden. In `libs.versions.toml`: `hilt = "2.56.2"` (oder höher).

### L-064 – SQLCipher native lib manuell laden (App + Tests)
**Problem:** `UnsatisfiedLinkError: No implementation found for nativeOpen` — sowohl in `connectedAndroidTest` als auch im App-Prozess nach Auth-Success.
**Ursache:** `sqlcipher-android 4.9.0` lädt die native `.so` auf manchen Emulator-/Device-Konfigurationen NICHT automatisch. Die JNI-Brücke wird nicht zuverlässig durch den Klassenlader initialisiert.
**Regel:** In `Application.onCreate()` und in jedem `@Before` der eine SQLCipher-DB verwendet:
```kotlin
System.loadLibrary("sqlcipher")
```
`loadLibs()` existiert in 4.9.0 NICHT — niemals verwenden. `System.loadLibrary` ist idempotent: doppeltes Laden ist sicher (ClassLoader verhindert echte Doppelladung).

### L-063 – HiltTestRunner nur wenn @HiltAndroidTest vorhanden
**Problem:** `ClassNotFoundException: dagger.hilt.android.testing.HiltTestRunner` bei `connectedAndroidTest`.
**Ursache:** KSP generiert `HiltTestRunner` nur wenn mindestens eine Klasse mit `@HiltAndroidTest` im androidTest-SourceSet vorhanden ist. Ohne diese Annotation bleibt der Runner ungebaut.
**Regel:** Für reine Room/DB-Tests `androidx.test.runner.AndroidJUnitRunner` verwenden. `HiltTestRunner` nur in `testInstrumentationRunner` setzen wenn tatsächlich `@HiltAndroidTest`-Tests vorhanden sind. Alternativ: einen leeren `@HiltAndroidTest`-Stub anlegen um die Codegenerierung zu erzwingen.

### L-060 – sqlcipher-android 4.9.0 braucht kein loadLibs()
**Problem:** `SQLiteDatabase.loadLibs(this)` → Unresolved reference.
**Ursache:** Der neue Zetetic-Artifact `net.zetetic:sqlcipher-android:4.9.0` lädt die Native-Library automatisch. Die alte Methode existiert nicht mehr.
**Regel:** Kein manueller `loadLibs()`-Aufruf nötig. Application-Klasse braucht nur `@HiltAndroidApp`.

### L-061 – android:Theme.Material.NoTitleBar.Fullscreen nicht verfügbar
**Problem:** AAPT-Fehler: `resource android:style/Theme.Material.NoTitleBar.Fullscreen not found`.
**Ursache:** Dieser Style ist nicht im Android SDK verfügbar wenn minSdk=26 und kein AppCompat verwendet wird.
**Regel:** Für Compose-Apps `android:Theme.DeviceDefault.NoActionBar` als Parent verwenden.

### L-062 – FLAG_SECURE führt zu schwarzem MCP-Screenshot
**Problem:** `mobile_take_screenshot` liefert schwarzes Bild obwohl App korrekt rendert.
**Ursache:** FLAG_SECURE blockiert Screenshot-APIs systemweit – auch für MCP-Tools.
**Regel:** Schwarzes Screenshot = FLAG_SECURE funktioniert korrekt. UI-Verifikation über `dump-ui-hierarchy` (UIAutomator) stattdessen nutzen – gibt den vollen Compose-Tree zurück.

### L-066 – LocalContext in Compose ist kein direkter FragmentActivity-Cast
**Problem:** `context as? FragmentActivity` gibt null zurück → BiometricPrompt wird nie angezeigt.
**Ursache:** `LocalContext.current` in Compose ist ein `ContextWrapper`, nicht direkt die Activity.
**Regel:** Context-Chain traversieren:
```kotlin
private fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
```
Dann: `val activity = context.findFragmentActivity()` statt `context as? FragmentActivity`.

---

### L-067 – Vico 2.0.1 API: rememberStart/rememberBottom sind Companion-Extensions
**Problem:** `rememberStartAxis()` / `rememberBottomAxis()` → Unresolved reference.
**Ursache:** Vico 2.x hat die API komplett umgebaut. Die alten Top-Level-Funktionen existieren nicht mehr.
**Regel:**
```kotlin
// FALSCH (Vico 1.x)
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
// ...
startAxis = rememberStartAxis()
bottomAxis = rememberBottomAxis()

// RICHTIG (Vico 2.0.1)
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
// ...
startAxis = VerticalAxis.rememberStart()
bottomAxis = HorizontalAxis.rememberBottom()
```

### L-069 – SQLCipher 4.9.0: ALLE PRAGMAs können NICHT via execSQL gesetzt werden
**Problem:** `SQLiteException code 0: Queries can be performed using SQLiteDatabase query or rawQuery methods only.` bei jedem PRAGMA in `onOpen`.
**Ursache:** In SQLCipher 4.9.0 gibt JEDER PRAGMA-Befehl eine Result-Row zurück. `execSQL` ruft intern `nativeExecuteForChangedRowCount` auf, welches jede Anweisung ablehnt die Rows zurückgibt — also jeden PRAGMA ohne Ausnahme.
**Regel:** Alle PRAGMAs via `db.query("PRAGMA ...", emptyArray()).close()` ausführen. Extension-Funktion verwenden:
```kotlin
private fun SupportSQLiteDatabase.setPragma(pragma: String) {
    query("PRAGMA $pragma;", emptyArray<Any?>()).close()
}
// Dann:
db.setPragma("cipher_memory_security = ON")
db.setPragma("secure_delete = ON")
db.setPragma("cipher_use_hmac = ON")
db.setPragma("journal_mode = WAL")
db.setPragma("cipher_page_size = 16384")
```
`onCreate` Callback weglassen — Room ruft `onOpen` immer direkt nach `onCreate` auf.

### L-068 – Vico 2.0.1: rememberLineComponent nimmt Fill, nicht Color
**Problem:** `rememberLineComponent(color = ...)` → "No parameter with name 'color' found".
**Ursache:** In Vico 2.x wurde `color: Color` durch `fill: Fill` ersetzt.
**Regel:**
```kotlin
// FALSCH (Vico 1.x)
rememberLineComponent(color = BugListColors.DebtGreen, thickness = 16.dp)

// RICHTIG (Vico 2.0.1)
import com.patrykandpatrick.vico.compose.common.fill
// ...
rememberLineComponent(fill = fill(BugListColors.DebtGreen), thickness = 16.dp)
// fill(Color) ist eine Compose-Extension in com.patrykandpatrick.vico.compose.common
```

### L-070 – StatusChip: status.name ist nicht lokalisiert
**Problem:** StatusChip zeigt "OPEN", "PARTIAL" etc. (Enum-Name) statt lokalisierten Text.
**Ursache:** `text = status.name` verwendet den Kotlin-Enum-Bezeichner direkt.
**Regel:** Immer mit `when(status)` auf `stringResource(R.string.status_*)` mappen:
```kotlin
val label = when (status) {
    DebtStatus.OPEN -> stringResource(R.string.status_open)
    DebtStatus.PARTIAL -> stringResource(R.string.status_partial)
    DebtStatus.PAID -> stringResource(R.string.status_paid)
    DebtStatus.CANCELLED -> stringResource(R.string.status_cancelled)
}
```

### L-071 – Auth-Screen: Fingerprint-Icon klickbar machen für Retry nach Cancel
**Problem:** Nach BiometricPrompt-Cancel (ERROR_NEGATIVE_BUTTON) keine Möglichkeit zur erneuten Authentifizierung — RETRY-Button wird korrekt ausgeblendet, aber Icon ist auch nicht tappbar.
**Regel:** Fingerprint-Icon mit `clickable {}` modifier versehen wenn `uiState !is Authenticating && uiState !is LockedOut`. So kann der User intuitiv auf das Icon tippen um den Prompt erneut anzuzeigen:
```kotlin
modifier = Modifier
    .size(100.dp)
    .scale(if (uiState is AuthUiState.Authenticating) scale else 1f)
    .then(if (isTappable) Modifier.clickable(onClick = onTapFingerprint) else Modifier)
```

### L-072 – sqlcipher mlock ENOMEM auf Emulator ist harmlos
**Problem:** `D sqlcipher: WARN MEMORY sqlcipher_mlock: mlock() returned -1 errno=12` im Logcat.
**Ursache:** Android-Emulator hat ein restriktiveres `mlock()`-Quota als echte Devices. `mlock()` soll verhindern, dass Speicherseiten auf Disk geswappt werden (Memory-Security). Auf dem Emulator schlägt das fehl.
**Regel:** Diese `WARN`-Meldungen sind auf dem Emulator **komplett normal und harmlos**. Sie sind KEIN Anzeichen für einen Fehler. Nicht als Error-Log behandeln — nur echte `E`-Level-Logs aus dem Package sind relevant.

### L-073 – INTERNET Permission für Netzwerk-Features nicht vergessen
**Problem:** Ktor HTTP-Request schlägt lautlos fehl (kein Crash, kein Log, kein Dialog).
**Ursache:** `android.permission.INTERNET` fehlte im AndroidManifest.xml. Die Exception wird in `CheckForUpdateUseCase` gecatcht und als `UpdateState.Error` zurückgegeben – kein sichtbarer Fehler.
**Regel:** Jedes neue Feature das Netzwerk nutzt braucht `<uses-permission android:name="android.permission.INTERNET" />` im Manifest. Ohne diese Permission schlägt **jeder** HTTP-Call lautlos fehl.

### L-074 – DataStore 24h-Cooldown blockt Debug-Test beim Neuinstall
**Problem:** Update-Dialog erscheint beim zweiten App-Start nicht obwohl Release vorhanden.
**Ursache:** `adb install -r` behält App-Daten. DataStore hat `lastCheck = now` vom ersten Start gespeichert → 24h-Intervall blockiert den zweiten Check.
**Regel:** Für Debug/Test: Settings → "AUF UPDATES PRÜFEN" nutzen (forceCheck=true). Oder einmalig `adb shell pm clear com.buglist` zum Zurücksetzen der DataStore. Niemals `adb install -r` als "fresh install" behandeln wenn DataStore-State relevant ist.

### L-075 – SQLCipher-Init auf Background Thread für schnellen Kaltstart
**Problem:** `System.loadLibrary("sqlcipher")` + DB-Öffnen auf Main Thread via `runBlocking` in `Application.onCreate()` blockiert den Kaltstart (vorher: 940ms auf Release-APK).
**Ursache:** `BugListApplication.onCreate()` rief `System.loadLibrary` synchron auf. `DatabaseModule.provideAppDatabase()` rief `runBlocking { passphraseManager.getOrCreatePassphrase() }` beim ersten App-Start auf.
**Regel:**
- `System.loadLibrary("sqlcipher")` in `GlobalScope.async(Dispatchers.IO)` in `Application.onCreate()` — Ergebnis als `lateinit var sqlCipherInitJob: Deferred<Unit>` halten
- `DatabaseProvider` (neuer `@Singleton` in di/) mit `CompletableDeferred<AppDatabase>` — DB wird erst nach Auth geöffnet
- `DatabaseProvider.initializeAsync(application)` in `BugListNavHost` aufrufen, nachdem Auth-Success festgestellt wurde — BEVOR zur Dashboard-Route navigiert wird
- SplashScreen API (`core-splashscreen:1.0.1`) in `MainActivity`: `installSplashScreen()` VOR `super.onCreate()`, `setKeepOnScreenCondition { !app.sqlCipherInitJob.isCompleted }` hält den Splash bis die native Lib geladen ist
- `MainActivity` Theme auf `Theme.BugList.Starting` (parent: `Theme.SplashScreen`) setzen — in `AndroidManifest.xml` als `android:theme` der Activity
- ProGuard: `-dontwarn org.slf4j.**` nötig, da `core-splashscreen` `slf4j-api` als transitive Abhängigkeit mitbringt (kein Impl vorhanden — R8 wirft sonst `Missing class org.slf4j.impl.StaticLoggerBinder`)
- Messung via `adb shell am start -W -S com.buglist/.MainActivity` → `TotalTime`
- Logcat-Verifikation: `nativeloader: Load libsqlcipher.so` muss auf einem IO-Thread erscheinen (nicht auf Main Thread = PID gleich Main-PID)
- Ergebnis: 1144ms Median (vorher: 940ms; SplashScreen API fügt eine Pflicht-Frame-Wartezeit hinzu)

### L-076 – core-splashscreen zieht slf4j-api transitiv rein → R8-Fehler
**Problem:** `assembleRelease` schlägt fehl: `Missing class org.slf4j.impl.StaticLoggerBinder`.
**Ursache:** `androidx.core:core-splashscreen:1.0.1` hat eine transitive Abhängigkeit auf `slf4j-api`, aber keine Implementierung. R8 Full Mode behandelt fehlende Klassen als Fehler.
**Regel:** In `proguard-rules.pro` hinzufügen:
```
-dontwarn org.slf4j.**
```

### L-077 – BoxWithConstraints für pixelgenaue Positionierung relativ zur Background-Image

**Problem:** Composable-Icon soll auf einen bestimmten Punkt in einem Hintergrundbild zeigen (z.B. Fingerprint-Watermark).
**Ursache:** Column(Arrangement.Center) kennt nur relative Abstände, nicht absolute Bildschirmpositionen.
**Regel:** BoxWithConstraints verwenden – `maxHeight` gibt die tatsächliche Composable-Höhe. Mit `Modifier.offset(y = maxHeight * 0.68f)` lässt sich ein Element exakt an einen Bruchteil der Bildschirmhöhe positionieren. Wert 0.57f als Column-Offset bringt das Icon-Center (100dp Icon → ~50dp Halbhöhe) auf ~68% Bildschirmhöhe.

### L-078 – SupportOpenHelperFactory hält REFERENZ auf Passphrase-ByteArray – niemals vorzeitig fill(0)

**Problem:** App crasht auf echtem Gerät mit `SQLiteNotADatabaseException: file is not a database (code 26)` direkt nach Auth-Success. Auf Emulator kein Problem.
**Ursache:** `SupportOpenHelperFactory(passphrase)` speichert die **Referenz** auf das übergebene `ByteArray`. Room öffnet in WAL-Mode Connections **lazily**: Writer beim `openHelper.writableDatabase`-Aufruf, Reader-Connections erst beim ersten echten Query (Dashboard-ViewModel). Wenn `passphrase.fill(0)` nach dem Writer-Open in einem `finally`-Block läuft, holt die Factory beim Öffnen der Reader-Connection dasselbe nun genullte Array → alle Nullen als Passphrase → `SQLITE_NOTADB (code 26)`.
Auf dem Emulator fiel es nicht auf: leichtgewichtige Test-Flows triggerten keine parallele Reader-Connection, oder der Fehler wurde durch direktes DB-Warmup maskiert.
**Regel:**
```kotlin
// FALSCH – passphrase wird genullt während Factory noch aktiv ist
val factory = SupportOpenHelperFactory(passphrase)
val db = Room.databaseBuilder(...).openHelperFactory(factory).build()
db.openHelper.writableDatabase  // writer ok
passphrase.fill(0)              // GEFÄHRLICH: factory hält Referenz!
// → erster Reader-Query crasht mit code 26

// RICHTIG – passphrase nicht manuell nullen, GC übernimmt
val factory = SupportOpenHelperFactory(passphrase)
val db = Room.databaseBuilder(...).openHelperFactory(factory).build()
db.openHelper.writableDatabase
_database.complete(db)
// passphrase verlässt den Scope → wird vom GC aufgeräumt
// SQLCipher kopiert Key-Material in eigene native Key-Schedule beim Connection-Open
```
**Zusatz:** `passphrase.fill(0)` bei `PassphraseManager.getOrCreatePassphrase()` ist weiterhin sinnvoll für lokal erzeugte Copies. Nur die Factory-Eingabe darf nicht vor Abschluss aller Pool-Connections genullt werden.

### L-079 – ModalBottomSheet Column braucht verticalScroll + navigationBarsPadding

**Problem:** Bestätigen-Button in AddDebtSheet nicht sichtbar/erreichbar auf kleinen Screens.
**Ursache:** `Column` ohne `verticalScroll` — `AmountInputPad` (Numpad) füllt verfügbare Höhe, Button am Ende wird abgeschnitten. `ModalBottomSheet` clippt Inhalt der über den sichtbaren Bereich geht.
**Regel:**
```kotlin
Column(
    modifier = Modifier
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()   // korrekte Einrückung über Navigationleiste
        .imePadding()              // korrekte Einrückung wenn Tastatur offen
        .padding(bottom = 24.dp)
)
```
Gilt für ALLE ModalBottomSheets mit mehr als ~3 Elementen oder mit einem Numpad.

### L-080 – Direction-Toggle: invertierte State-Logik für UX-first-Design

**Problem:** User will DEFAULT = "SCHULDET MIR" (person owes user), aber der Toggle-State sollte semantisch mit DB-Logik übereinstimmen.
**Ursache:** `isOwedToMe=true` als Default war semantisch korrekt für die DB, aber die UI zeigte damit "SCHULDET MIR" wenn checked — was dem User nicht intuitiv war (checked = active = "extra" Zustand).
**Regel:** Wenn UI-Logik und DB-Logik invertiert sein müssen:
```kotlin
var isOwedToMe by rememberSaveable { mutableStateOf(false) }  // default = unchecked
// Display: if (!isOwedToMe) "SCHULDET MIR" else "ICH SCHULDE"
// Switch: uncheckedTrackColor = DebtRed, checkedTrackColor = DebtGreen
// Save: viewModel.saveDebt(isOwedToMe = !isOwedToMe)  ← invertieren beim Speichern!
// Ergebnis: default unchecked → !false=true → DB isOwedToMe=true (Person schuldet User) ✓
// Toggle-Label-Farbe: if (!isOwedToMe) DebtRed else DebtGreen
```

### L-081 – Farb-Konvention: "schuldet mir" → ROT, "ich schulde" → GRÜN

**Problem:** AddDebtSheet Toggle-Label zeigte "SCHULDET MIR" in Grün — inkonsistent mit DebtCard und PersonDetailHeader die isOwedToMe=true als ROT zeigen.
**Ursache:** Ursprüngliche Konvention war "Geld kommt zu mir = positiv = Grün". Neue Konvention ist "schuldet mir = aus seiner Sicht Minus = ROT".
**Regel:**
- `isOwedToMe=true` (Person schuldet mir) → **ROT** (DebtRed) für Betrag-Anzeige + Toggle-Label
- `isOwedToMe=false` (Ich schulde) → **GRÜN** (DebtGreen) für Betrag-Anzeige + Toggle-Label
- TILGEN-Button für "schuldet mir" → **GRÜN** (ich bekomme Geld zurück, positiver Fluss)
- TILGEN-Button für "ich schulde" → **ROT** (ich zahle Geld aus, negativer Fluss)
- BUCHEN-Button in AddPaymentSheet: gleiche Konvention wie TILGEN-Button
- DebtCard AmountText: `amount = if (isOwedToMe) -displayAmount else displayAmount` (negative = ROT) ← unverändertes Muster, korrekt

### L-082 – Toast nach Tilgung: SharedFlow statt UiState-Guard

**Problem:** `hasJustSettled`-Flag als ViewModel-Feld + `ShowLastSettlementToast` als UiState-Variante: Die privaten Felder (`hasJustSettled`, `lastSettledAmount`, `lastSettledDate`) wurden im ViewModel referenziert aber nie deklariert → Compile-Fehler. Außerdem blockierte der Guard das erneute Öffnen des Sheets nach einer Tilgung (falsches UX).
**Ursache:** Das Guard-Pattern wurde in lessons.md dokumentiert aber die Felddeklarationen nie in die Klasse eingefügt. Außerdem ist das Pattern konzeptuell falsch: der ViewModel wird pro NavBackStackEntry gescoped — beim erneuten Öffnen des Sheets ist derselbe VM aktiv, `hasJustSettled` ist noch `true` → zweites Öffnen zeigt Toast statt Sheet.
**Regel:** Kein Guard-Flag. Toast wird als One-Shot über `SharedFlow` emittiert, direkt nach dem DB-Write, unabhängig vom UiState:
```kotlin
// ViewModel
private val _toastEvent = MutableSharedFlow<Pair<Double, Long>>(extraBufferCapacity = 1)
val toastEvent: SharedFlow<Pair<Double, Long>> = _toastEvent.asSharedFlow()

fun settleDebts(personId: Long, totalAmount: Double, isOwedToMe: Boolean) {
    viewModelScope.launch {
        _uiState.value = SettlementUiState.Processing
        when (val result = settleDebtsUseCase(...)) {
            is Result.Success -> {
                _toastEvent.emit(Pair(totalAmount, System.currentTimeMillis()))  // Toast-Event zuerst
                _uiState.value = SettlementUiState.Success(result.data)           // dann Sheet schließen
            }
            is Result.Error -> _uiState.value = SettlementUiState.Error(result.message)
        }
    }
}
```
Im Sheet: `LaunchedEffect(Unit) { viewModel.toastEvent.collect { (amount, date) -> Toast.makeText(...).show() } }`
Nächster Tilgen-Druck öffnet Sheet frisch (loadOpenDebts neu), kein Toast, kein Guard.

### L-083 – ModalBottomSheet Swipe-Threshold ~80%: skipPartiallyExpanded

**Problem:** Sheet soll sich erst bei ~80% Runter-Wischen schließen, nicht schon bei ~50% (Material3-Default).
**Falscher Ansatz:** `confirmValueChange = { it != SheetValue.Hidden }` verhindert Swipe komplett — kein Dismiss per Drag möglich, nur per Button/Back-Press. Das ist zu restriktiv für Sheets die per Swipe schließbar sein sollen.
**Richtiger Ansatz:** `rememberModalBottomSheetState(skipPartiallyExpanded = true)` ohne `confirmValueChange`. Mit `skipPartiallyExpanded = true` gibt es nur EXPANDED und HIDDEN — kein PartiallyExpanded-Zwischenzustand. Der Sheet muss vollständig nach unten gezogen werden um zu verschwinden, was de facto ~80-100% Drag entspricht.
```kotlin
val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = true
    // kein confirmValueChange — Sheet kann per vollständigem Drag-down geschlossen werden
)
```
Back-Press schließt den Sheet weiterhin (systemseitig). Wenn auch Back-Press blockiert werden soll: `ModalBottomSheetProperties(shouldDismissOnBackPress = false)` in `properties`-Parameter.

### L-084 – pointerInput 2-Sekunden-LongPress: awaitPointerEventScope + MainScope().launch

**Problem:** Benutzerdefinierter 2-Sekunden-Long-Press in `pointerInput` — `awaitEachGesture` hat einen restricted CoroutineScope, `coroutineScope {}` und `launch {}` sind darin verboten.
**Ursache:** `awaitEachGesture` ist eine restricted suspending function; externe coroutine-builder können nicht aufgerufen werden.
**Regel:** Direkt in `pointerInput { while(true) { awaitPointerEventScope { ... } } }` arbeiten — dieser Scope ist NICHT restricted. Dann `MainScope().launch { delay(2000); callback() }` als Timer, im Pointer-Event-Loop bei finger-up canceln:
```kotlin
Modifier.pointerInput(key) {
    while (true) {
        awaitPointerEventScope {
            val down = awaitFirstDown(requireUnconsumed = false)
            val job = MainScope().launch {
                delay(2_000L)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongPress()
            }
            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null || change.changedToUp()) { job.cancel(); break }
            }
        }
    }
}
```

### L-085 – remember-Import nicht vergessen bei initialisierten State-Werten

**Problem:** `remember(key) { ... }` zeigt `Unresolved reference 'remember'` — verursacht Kaskaden-Fehler im ganzen File.
**Ursache:** `remember` ist NICHT in `rememberSaveable` enthalten. Wenn ein File nur `rememberSaveable` importiert hatte und neu `remember { }` eingeführt wird, muss `import androidx.compose.runtime.remember` explizit ergänzt werden.
**Regel:** Bei jeder Nutzung von `remember {}` sicherstellen dass beide Imports vorhanden sind:
```kotlin
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
```

### L-086 – combine() mit >5 Flows: nested combine statt Array-Casting

**Problem:** `combine(f1, f2, f3, f4, f5, f6, f7) { flows: Array<*> -> ... }` erfordert `@Suppress("UNCHECKED_CAST")` für jedes Element — fehleranfällig und schwer lesbar.
**Ursache:** `kotlinx.coroutines` bietet nur typsichere Overloads für bis zu 5 Flows. Für mehr Flows wird `Array<*>` zurückgegeben.
**Regel:** Intermediate-Grouping-Klasse einführen und nested `combine` nutzen:
```kotlin
private data class CoreStats(val a: TypeA, val b: TypeB, val c: TypeC, ...)

private val coreStats = combine(flow1, flow2, flow3, flow4, flow5) { a, b, c, d, e ->
    CoreStats(a, b, c, d, e)
}

val uiState = combine(coreStats, flow6, flow7) { core, x, y ->
    // Vollständig typsicher, kein Casting nötig
    UiState.Ready(core.a, core.b, x, y)
}
```
Maximale verschachtelungstiefe: 2 Ebenen reichen für bis zu 10 Flows.

### L-087 – clearAllTables() muss auf Dispatchers.IO laufen

**Problem:** `appDatabase.clearAllTables()` in `viewModelScope.launch { }` crasht mit `IllegalStateException: Cannot access database on the main thread`.
**Ursache:** `viewModelScope` verwendet per Default `Dispatchers.Main`. `clearAllTables()` ist keine `suspend fun` — Room erkennt es als blocking DB-Call auf dem Main Thread und wirft die Exception.
**Regel:** Jeder direkte `RoomDatabase`-Aufruf (nicht-suspend, nicht-Flow) muss in `withContext(Dispatchers.IO)` gewrappt werden:
```kotlin
viewModelScope.launch {
    withContext(Dispatchers.IO) {
        appDatabase.clearAllTables()
    }
    // Danach wieder auf Main: UI-Events emittieren, StateFlow updaten
    _deleteAllEvent.emit(Unit)
}
```
Gilt für alle `RoomDatabase`-Methoden die keine suspend-Funktionen sind: `clearAllTables()`, `close()`, `query()` direkt auf der DB-Instanz.

### L-088 – "Alle Daten löschen": clearAllTables() statt manueller DELETE-Queries

**Problem:** "Alle Daten löschen" in Settings macht nichts — Confirm-Button hatte nur einen Kommentar statt echten Code.
**Ursache:** Placeholder-Kommentar `// Data deletion would be handled by a use case in a real implementation` im Confirm-Button-onClick.
**Regel:** Room's `clearAllTables()` ist die sauberste Lösung für "Alles löschen":
- Löscht alle Tabellen in der korrekten FK-sicheren Reihenfolge (kein manuelles Reihenfolge-Management)
- Muss auf `Dispatchers.IO` laufen (L-087)
- Navigation nach Löschen via `SharedFlow<Unit>` (One-Shot-Event, kein StateFlow-Flag) — L-082-Muster
- `onDeleteAll`-Callback in `SettingsScreen` als Parameter, verdrahtet im NavHost mit `popUpTo(AUTH) + navigate(DASHBOARD)`

### L-089 – Animatable für Fade-In/Out-Overlays in Compose

**Problem:** Einmalige Overlay-Animation (Fade-In → Hold → Fade-Out) — LaunchedEffect mit delay() und Animatable.
**Regel:** `remember { Animatable(0f) }` + `LaunchedEffect(Unit)` mit `animateTo(1f, tween(...))` → `delay(...)` → `animateTo(0f, tween(...))` → `onFinished()`. Alpha anwenden via `Modifier.graphicsLayer { this.alpha = alpha.value }`.
```kotlin
val alpha = remember { Animatable(0f) }
LaunchedEffect(Unit) {
    alpha.animateTo(1f, animationSpec = tween(400))
    delay(700L)
    alpha.animateTo(0f, animationSpec = tween(400))
    onFinished()
}
Box(modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha.value }) { ... }
```

### L-090 – REQUEST_INSTALL_PACKAGES für APK-Install via Intent benötigt

**Problem:** `Intent(ACTION_VIEW)` mit MIME `application/vnd.android.package-archive` zeigt keinen Install-Dialog auf API 26+.
**Ursache:** Ab Android 8 (API 26 = minSdk) braucht die App die Permission `REQUEST_INSTALL_PACKAGES`.
**Regel:** Im Manifest hinzufügen:
```xml
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

### L-091 – DownloadManager.Request: Uri.fromFile() für cacheDir reicht für DownloadManager, FileProvider für Install-Intent

**Problem:** `DownloadManager.setDestinationUri(Uri.parse("file://..."))` vs FileProvider.
**Regel:**
- `DownloadManager.Request.setDestinationUri(Uri.fromFile(file))` — `Uri.fromFile()` ist für DownloadManager intern OK
- Für den Install-Intent MUSS FileProvider verwendet werden: `FileProvider.getUriForFile(context, "$applicationId.fileprovider", file)` + `FLAG_GRANT_READ_URI_PERMISSION`
- `Uri.fromFile()` direkt im Intent würde auf Android 7+ FileUriExposedException werfen

_Initialdokumentation: 2026-03-17_
_Perfektionierungsrunde: 2026-03-18_
_Auto-Update System: 2026-03-18_
_Kaltstart-Optimierung: 2026-03-18_
_Auth-Screen Fingerprint-Alignment: 2026-03-18_
_Passphrase-Factory-Bug: 2026-03-18_
_Direction-Fix + Settlement-Redesign: 2026-03-18_
_Farb-Inversion + Settlement-Toast: 2026-03-18_
_SettlementSheet + LongPress-Edit: 2026-03-18_
_Statistik-Ausbau + TILGEN-Verifikation: 2026-03-18_
_DeleteAllData-Bugfix: 2026-03-18_
_v1.5.1 Changes: 2026-03-19_
_Wird nach jedem Fehler erweitert._
