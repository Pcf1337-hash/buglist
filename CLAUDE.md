# CLAUDE.md – BugList

> Lies diese Datei am Anfang JEDER Session vollständig durch.
> Dann: `tasks/lessons.md` → `tasks/todo.md` → loslegen.

---

## Projektübersicht

**App-Name:** BugList
**Zweck:** Offline-first Schulden-Tracker für Android
**Design:** Gangster-Rap / Streetstyle – reines Dark Mode, Gold (#FFD700), Urban Fonts
**Sicherheit:** AES-256-GCM (Android Keystore) + BiometricPrompt (BIOMETRIC_STRONG) + SQLCipher
**Plattform:** Android-Only. Min SDK 26, Target SDK 35. iOS existiert nicht.

---

## MCP Tools (Emulator läuft – aktiv nutzen)

Du hast drei MCP Server. Nutze sie bei JEDEM Task automatisch – nicht nur wenn etwas schiefgeht.

### 1. Gradle MCP (`rnett/gradle-mcp`)
Für alle Build-Operationen. Gibt strukturierte Fehler zurück (Datei + Zeile + Message).
```
assembleDebug      → nach jeder Code-Änderung
assembleRelease    → am Ende jeder Phase
connectedAndroidTest → nach jedem Task der Logik enthält
lint               → vor Phase-Abschluss
test               → Unit-Tests nach Task 2.x und 5.1
```
Bei Compile-Fehler: Gradle MCP gibt dir Datei + Zeile → direkt fixen → retry. Kein Nachfragen.

### 2. ADB / Device Control (`mobile-mcp`)
Für visuelle Verifikation auf dem laufenden Emulator.
```
mobile_screenshot          → nach jedem Screen-Build
mobile_launch_app          → App starten nach Install
mobile_tap / mobile_swipe  → UI-Flows durchklicken
mobile_list_elements_on_screen → Accessibility-Tree prüfen
```
Nach jedem neuen Screen: Screenshot aufnehmen und visuell prüfen ob Design stimmt.

### 3. Android Toolkit (`android-mcp-toolkit`)
Für Logcat und Crash-Erkennung.
```
logcat (mit Tag-Filter "BugList")  → jeden App-Start beobachten
crash detection                    → automatisch nach jedem Launch
ANR check                          → bei hängender UI
buffer clear                       → vor neuem Test-Run
```
Jeden App-Start mit Logcat begleiten. Crashes sofort fixen, nie ignorieren.

### Autonomer Test-Loop pro Task
```
1. Code schreiben / ändern
2. Gradle MCP: assembleDebug
   └─ Fehler? → fixen → zurück zu 2
3. ADB: App installieren + starten (adb install + mobile_launch_app)
4. Android Toolkit: Logcat beobachten (30 Sekunden, Tag=BugList)
   └─ Crash/ANR? → Root Cause in Logcat → fixen → zurück zu 2
5. mobile-mcp: Screenshot aufnehmen → visuell prüfen
6. Relevant für diesen Task: UI-Flow durchklicken
7. Gradle MCP: connectedAndroidTest (wenn Tests vorhanden)
8. Alles grün → Task als [x] markieren
```

---

## Tech-Stack (final, keine Diskussion)

| Komponente | Bibliothek | Version |
|---|---|---|
| Sprache | Kotlin | 2.3.x |
| UI | Jetpack Compose BOM | 2025.x |
| Design-System | Material 3 | 1.4.x |
| Datenbank | Room | 2.6.1 |
| DB-Verschlüsselung | **sqlcipher-android** (Zetetic) | **4.9.0** |
| SQLite-Bridge | androidx.sqlite | 2.4.0 |
| Biometrie | androidx.biometric | 1.1.0 |
| Key-Storage | Android Keystore AES-256-GCM | OS-nativ |
| Config-Speicher | Jetpack DataStore + Tink | 1.1.4 / 1.18.0 |
| KDF | Argon2Kt | 1.6.0 |
| DI | Hilt | 2.51.x |
| Navigation | Compose Navigation | 2.8.x |
| Charts | Vico (Compose-native) | aktuell |
| Build | Gradle + **KSP** + R8 Full Mode | 8.x |

### VERBOTEN – niemals verwenden
| Was | Warum |
|---|---|
| `EncryptedSharedPreferences` | Deprecated April 2025 → Tink nutzen |
| `android-database-sqlcipher` (alt) | Deprecated, kein 16KB-Page-Support → `sqlcipher-android` |
| `Realm` | Deprecated September 2024, tot |
| `KAPT` | Deprecated → KSP verwenden |
| `String` für Passphrasen | Bleibt im Heap → ByteArray + nullen |

---

## Architektur

```
app/src/main/java/com/buglist/
├── presentation/
│   ├── auth/           # AuthScreen, AuthViewModel
│   ├── dashboard/      # DashboardScreen, DashboardViewModel
│   ├── person_detail/  # PersonDetailScreen, PersonDetailViewModel
│   ├── add_debt/       # AddDebtSheet, AddDebtViewModel
│   ├── add_person/     # AddPersonSheet, AddPersonViewModel
│   ├── statistics/     # StatisticsScreen, StatisticsViewModel
│   └── settings/       # SettingsScreen, SettingsViewModel
├── domain/
│   ├── model/          # Person, DebtEntry, DebtStatus, DebtDirection
│   ├── repository/     # PersonRepository, DebtRepository (Interfaces)
│   └── usecase/        # Ein File pro Use Case
├── data/
│   ├── local/
│   │   ├── entity/     # PersonEntity, DebtEntryEntity
│   │   ├── dao/        # PersonDao, DebtEntryDao
│   │   └── AppDatabase.kt
│   └── repository/     # PersonRepositoryImpl, DebtRepositoryImpl
├── security/
│   ├── KeystoreManager.kt
│   ├── BiometricAuthManager.kt
│   └── PassphraseManager.kt
├── di/                 # Hilt Modules
└── util/               # Extensions, Constants

tasks/
├── todo.md
└── lessons.md
.claude/
└── settings.json       # Auto-Approve bereits konfiguriert
```

**Regel:** Domain-Schicht importiert NICHTS aus data/ oder presentation/.
**Regel:** ViewModels nutzen StateFlow, niemals mutableStateOf.
**Regel:** DAOs geben Flow<T> für Queries zurück, suspend fun für Writes.

---

## Sicherheitsregeln (absolut, keine Ausnahmen)

```kotlin
// FALSCH
val passphrase = "secret123"
db.openDatabase(passphrase)

// RICHTIG
val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
try {
    db.openDatabase(passphrase)
} finally {
    passphrase.fill(0)  // Memory nullen
}
```

```kotlin
// FLAG_SECURE – Reihenfolge kritisch
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.setFlags(FLAG_SECURE, FLAG_SECURE)  // VOR setContent{}
    setContent { BugListTheme { /* ... */ } }
}
```

```kotlin
// Keystore Key – Pflicht-Parameter
KeyGenParameterSpec.Builder("buglist_db_key", PURPOSE_ENCRYPT or PURPOSE_DECRYPT)
    .setBlockModes(BLOCK_MODE_GCM)
    .setEncryptionPaddings(ENCRYPTION_PADDING_NONE)
    .setKeySize(256)
    .setUserAuthenticationRequired(true)
    .setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)
    .setInvalidatedByBiometricEnrollment(true)  // Pflicht
    .build()
```

```sql
-- SQLCipher PRAGMAs – immer setzen
PRAGMA cipher_memory_security = ON;
PRAGMA secure_delete = ON;
PRAGMA cipher_use_hmac = ON;
PRAGMA journal_mode = WAL;
PRAGMA cipher_page_size = 16384;  -- 16KB für Android 15+
```

**Manifest-Pflichten:**
- `android:allowBackup="false"`
- `android:networkSecurityConfig="@xml/network_security_config"`
- `<uses-permission android:name="android.permission.USE_BIOMETRIC"/>`

---

## Design-System

### Farbpalette
```kotlin
object BugListColors {
    val Background  = Color(0xFF0D0D0D)  // Tiefschwarz
    val Surface     = Color(0xFF1A1A1A)  // Cards
    val SurfaceHigh = Color(0xFF242424)  // Elevated Cards
    val Gold        = Color(0xFFFFD700)  // Primary / Akzente / FAB
    val GoldDim     = Color(0xFFB8960C)  // Disabled Gold
    val Platinum    = Color(0xFFE5E4E2)  // Text Primary
    val Muted       = Color(0xFF666666)  // Text Secondary
    val DebtRed     = Color(0xFFFF3B3B)  // Negative / "Ich schulde"
    val DebtGreen   = Color(0xFF00E676)  // Positiv / "Schuldet mir"
    val Divider     = Color(0xFF2A2A2A)  // Trennlinien
}
```

### Typografie (Google Fonts)
| Rolle | Font | Style |
|---|---|---|
| App-Name, H1, H2 | **Oswald** | Bold, UPPERCASE |
| Body, Labels, Chips | **Roboto Condensed** | Regular/Medium |
| Geldbeträge, große Zahlen | **Bebas Neue** | Regular |

### Komponenten
- `GoldButton` – Primary CTA, Gold Background, schwarzer Text
- `DebtCard` – Surface Card, 12dp Radius, Gold-Border bei OPEN, Orange-Border bei PARTIAL
- `AmountText` – Bebas Neue, farbcodiert (Grün/Rot)
- `StatusChip` – OPEN=Gold Outline, PARTIAL=Orange Filled, PAID=Grün Filled, CANCELLED=Muted
- `PersonAvatar` – Initialen-Kreis, zufällige Akzentfarbe
- `PaymentProgressBar` – horizontale Leiste unter DebtCard: Gold-Füllung = bezahlter Anteil, Grau = Rest
- `AmountInputPad` – einhand-bedienbare Betragseingabe (siehe unten)
- Gold Ripple als Standard-Indication
- Empty States: Streetstyle-Illustration + Text in Oswald

### AmountInputPad – Einhand-Betragseingabe (Pflicht-Komponente)

Wird genutzt für: neues Debt anlegen, Teilzahlung eingeben, Betrag bearbeiten.

**Layout (Bottom Sheet, von oben nach unten):**
```
┌─────────────────────────────────┐
│  € 0,00          [Bebas Neue]   │  ← Anzeige-Display (Gold, groß)
├────────┬────────┬────────┬──────┤
│  -100  │  -50   │  -20   │  -10 │  ← Schnell-Minus (rot, abgerundet)
├────────┴────────┴────────┴──────┤
│  [  7  ]  [  8  ]  [  9  ]     │
│  [  4  ]  [  5  ]  [  6  ]     │  ← Numpad (Gold-Text, Dark Surface)
│  [  1  ]  [  2  ]  [  3  ]     │
│  [  ,  ]  [  0  ]  [  ⌫  ]    │
├────────┬────────┬────────┬──────┤
│  +10   │  +20   │  +50   │ +100 │  ← Schnell-Plus (grün, abgerundet)
└─────────────────────────────────┘
```

**Verhalten:**
- Numpad: direkte Eingabe des Betrags (Dezimalkomma erlaubt, max. 2 Nachkommastellen)
- ⌫: letzte Ziffer löschen, lang drücken = alles löschen
- Schnell-Buttons (+10/+20/+50/+100): addieren zum aktuellen Wert
- Schnell-Buttons (-10/-20/-50/-100): subtrahieren (min. 0, nie negativ)
- Display: aktualisiert sich live bei jeder Eingabe
- Maximalwert: 999.999,99 (darüber: kein Input mehr)
- Einhand-Optimierung: gesamtes Pad im unteren Bildschirmbereich, Daumen reicht überall hin

---

## Datenmodell

```kotlin
// Domain Models (nicht Room Entities)
data class Person(
    val id: Long = 0,
    val name: String,
    val phone: String? = null,
    val notes: String? = null,
    val avatarColor: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class DebtEntry(
    val id: Long = 0,
    val personId: Long,
    val amount: Double,           // Originalbetrag, immer positiv
    val currency: String = "EUR",
    val isOwedToMe: Boolean,      // true = Person schuldet mir
    val description: String? = null,
    val date: Long,
    val dueDate: Long? = null,
    val status: DebtStatus = DebtStatus.OPEN,
    val createdAt: Long = System.currentTimeMillis()
)

// Teilzahlung – jede Zahlung auf eine DebtEntry
data class Payment(
    val id: Long = 0,
    val debtEntryId: Long,        // FK → DebtEntry CASCADE DELETE
    val amount: Double,           // bezahlter Teilbetrag
    val note: String? = null,
    val date: Long = System.currentTimeMillis()
)

// Berechnete Sicht auf eine Schuld inkl. Teilzahlungen
data class DebtEntryWithPayments(
    val entry: DebtEntry,
    val payments: List<Payment>,
    val totalPaid: Double,        // Summe aller Payments
    val remaining: Double         // entry.amount - totalPaid
)

enum class DebtStatus { OPEN, PARTIAL, PAID, CANCELLED }
// PARTIAL = mindestens eine Teilzahlung, aber noch nicht vollständig bezahlt
// Status wird automatisch gesetzt: remaining == 0 → PAID, payments > 0 → PARTIAL

data class PersonWithBalance(
    val person: Person,
    val netBalance: Double,       // positiv = schuldet mir, negativ = ich schulde
    val openCount: Int
)
```

Room Entities spiegeln diese Struktur mit `@Entity`, `@PrimaryKey(autoGenerate = true)`,
`@ForeignKey(onDelete = CASCADE)` und Indices auf `personId`, `date`, `status`.

---

## Commit-Konventionen
```
feat:     Neue Funktionalität
fix:      Bugfix
security: Sicherheitsrelevante Änderung
test:     Nur Tests
style:    Design/UI ohne Logik
refactor: Kein Behavior-Change
docs:     Nur Dokumentation
```

---

## Häufige Fallstricke (Quick Reference)

| Problem | Lösung |
|---|---|
| SQLCipher öffnet DB nicht | Passphrase als `ByteArray` übergeben, nicht String |
| Key nach Fingerprint-Änderung ungültig | `KeyPermanentlyInvalidatedException` → Key löschen + neu generieren |
| StrongBox KeyGen schlägt fehl | `hasSystemFeature(FEATURE_STRONGBOX_KEYSTORE)` prüfen, Fallback TEE |
| FLAG_SECURE funktioniert nicht | Muss VOR `setContent{}` stehen |
| Compose-Recomposition zu oft | `remember {}` für Lambdas in Listen |
| Dark Mode weißer Blitz beim Start | `android:windowBackground="@color/background"` in themes.xml |
| Payment > DebtEntry.amount | Validierung: `payment.amount <= entry.remaining` erzwingen |
| Status nicht automatisch aktualisiert | Nach jeder Payment-Insertion: `remaining` neu berechnen → Status setzen (PARTIAL/PAID) |
| AmountInputPad Dezimal-Bug | Nur ein Komma erlauben, `replace` auf Input-String anwenden bevor Parsing |
| Room Migration fehlt | Jede Schema-Änderung braucht Migration (kein `fallbackToDestructiveMigration` in Release) |
| ProGuard entfernt SQLCipher | `-keep class net.zetetic.** { *; }` in proguard-rules.pro |
| Hilt in instrumented Tests | `testInstrumentationRunner = "dagger.hilt.android.testing.HiltTestRunner"` |
