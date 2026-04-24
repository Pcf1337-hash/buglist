# BugList Stats-Seite — vom Listengrab zum Street-Dashboard

**Die Kurzfassung: Dein aktueller Screen ist ein Karteikasten, kein Dashboard.** Sieben gleichwertige Sektionen untereinander zu stapeln heißt, dem Nutzer die Priorisierung aufzuhalsen — und genau das killt den Premium-Eindruck. Finanz-Apps, die als „geil" empfunden werden (Robinhood, Revolut, Monzo, Cash App, Klarna), bauen stattdessen um **eine einzige Heldenzahl + eine dominante Chart + beschleunigte Drill-Downs**. Dazu kommt bei BugList eine zweite Ebene, die keiner dieser Mainstream-Apps hat: **Street-Editorial-Typografie, Grain-Texture und ein Gold-Akzent, der wie eine Kette getragen wird — selten und deshalb wertvoll**. Dieser Report liefert dir drei Bausteine: (1) fünf App-Referenzen mit klaren Learnings, (2) eine neu strukturierte Seitenarchitektur, (3) zwei priorisierte Umsetzungslisten — fünf Quick Wins für diese Woche, drei Game Changer für die nächsten zwei Releases. Alle technischen Vorschläge sind in Jetpack Compose direkt umsetzbar; Code-Snippets sind beigelegt, wo sie Zeit sparen.

---

## Fünf Referenz-Apps und was sie dir beibringen

**Robinhood** ist die Blaupause für interaktive Portfolio-Charts. Der entscheidende Move: Die riesige Portfolio-Zahl oben **updated live mit, wenn du den Finger über die Chart-Linie ziehst** (Crosshair + Scrub + Haptik). Die Chart-Linie wird beim Load von links nach rechts gezeichnet (Path-Animation), die Zeitraum-Tabs (1D/1W/1M/3M/1Y/ALL) sitzen unaufdringlich unter der Chart, und das Farbsystem ist strikt binär (grün-up/rot-down) ohne dekorative Elemente. Robinhood hat seine Sparkline-Logik sogar als Open-Source-Lib `robinhood/spark` auf GitHub — Vorlage für deine Mini-Sparklines. **Learning: Aus den 7 Monats-Balken wird eine scrubbare Linie mit Haptik, die deinen Schulden-Saldo über die Zeit zeigt.**

**Revolut** ist State-of-the-Art bei Analytics. Drei Features, die BugList klauen sollte: **umschaltbarer Chart-Typ** (der User entscheidet Bar/Pie/Line für dieselben Daten), **zwei tappable Cards oben** für „Spent" vs „Income" mit Period-Vergleich (+12 % vs last month), und **kategorie-basierte Ranglisten** mit Top-3-Merchants/Personen. Dazu kommt „Tilt-to-hide-balance" — Handy umdrehen blurrt alle Zahlen. **Learning: Chart-Typ-Umschaltung ist ein Killer-Feature. Gib dem User Kontrolle über die Darstellung, statt ihm eine zu diktieren.**

**Monzo Trends** ist der härteste Benchmark für Personal-Finance-Analytics. Der Balance-Tab zeigt einen scrubbaren Konto-Verlauf mit einer **„Left to Spend"-Projektion** — der Graph rechnet zukünftige Bills ein und zeigt dir, was am Monatsende übrig bleibt. Für BugList ist das genial übersetzbar: **„Erwarteter Zufluss in 30 Tagen" basierend auf historischem Rückzahlverhalten deiner Schuldner**. Monzos neuer Home-Screen nutzt zusätzlich „Spotlights" — dezente Tap-Cards für Insights, die sich reihum anzeigen. Learning: **Nicht nur Vergangenheit zeigen, sondern Zukunft prognostizieren.**

**Klarna Money Story** ist der Breakout-Pattern der letzten Jahre. Einmal im Jahr (oder Monat) bekommt der User eine **Instagram-Stories-artige animierte Zusammenfassung**: Full-Screen Vertical Cards, Progress-Dots oben, animierte Quiz-Questions („Wer hat dir am meisten geschuldet, schätz mal?"), Countup-Animationen, Sticker-Reveal. Resultat: **+68 % mehr User, die danach Budgets gesetzt haben**. Für BugList: Ein monatlicher „Monats-Rapport" als Story-Sequenz mit goldenen Ticker-Tape-Confetti und Street-Typografie. **Learning: Gamifiziere den Rückblick, nicht die Hauptansicht.**

**Cash App** ist das Beispiel für radikale Reduktion mit Mut zur Typografie. Ein einziger Cash-Green-Akzent auf tiefem Schwarz, Oversized-Display-Sans für den Balance (80sp+), kaum Karten, kaum Divider — nur rohe Blocks. Seit 2025 hat Cash App zusätzlich **Moneybot**, einen konversationellen AI-Assistenten für Insights. **Learning: Mut zur Hero-Zahl. Ein 80sp-Saldo in Bebas Neue auf Schwarz mit Gold-Akzent schlägt jede Card-Orgie.**

Zum Vergleich, der Anti-Patron: **Splitwise**. Eine flache Bill-Liste mit Teal-Orange-Akzenten, keine echte Analytics, keine Trends — mehrere veröffentlichte UX-Case-Studies kritisieren genau das: „endless scrollable list", „debts hidden", „no hierarchy". **Dein aktueller Screen ist näher an Splitwise als an Monzo. Genau das willst du fixen.**

---

## Welche Metriken wirklich wehtun (im positiven Sinn)

Der entscheidende Mehrwert liegt nicht in der Optik, sondern in Kennzahlen, **die andere Debt-Apps nicht zeigen**. Ich habe 20 KPIs gescreent (abgeleitet aus B2B-Collections-KPIs wie DSO, CEI, Recovery Rate, Aging Buckets), hier die acht mit höchstem Impact für BugList:

Der **Reliability Score** (0-100) pro Person ist der Killer: ein zusammengesetzter Wert aus durchschnittlichem Zahlungsverzug, Anteil zurückgezahlter Schulden, Write-Off-Anteil und Varianz. Direkt inspiriert von FICO (Payment History = 35 % des Gesamtscores). Visualisierung: **großer Gradient-Progress-Ring auf der Personen-Detailseite, darunter A+/B/C-Label** („A+ — zahlt Ø 3 Tage früh").

**Average Debt Duration** (Ø Tage offen) ist die B2C-Variante von Days-Sales-Outstanding. Eine einzige Zahl beantwortet „Sind die Leute zuverlässig geworden oder schlimmer?". Darstellung als KPI-Karte mit Sparkline und Delta („Ø 12 Tage, −3 vs Vormonat").

**Aging Buckets** (0-7 / 8-30 / 31-90 / 90+ Tage) als 100%-stacked horizontal Bar ist der Collections-Industrie-Standard und zeigt sofort, wo Eskalation nötig ist. **At-Risk-Betrag** (Summe der Schulden >60 Tage) gehört als eigene rote Hero-Karte oben — emotional wichtig, weil es die Summe zeigt, die wahrscheinlich nie zurückfließt.

**Cash-Flow-Forecast** gewichtet offene Schulden mit der individuellen Zuverlässigkeit des Schuldners und projiziert: „In den nächsten 30 Tagen fließen Dir erwartungsgemäß 342 € zu". Das ist die Monzo-Left-to-Spend-Logik, invertiert.

**Debt Concentration / Pareto-Index** zeigt, wie klumpig dein Risiko ist. Wenn 80 % des offenen Volumens auf 2 Personen entfallen, ist das eine Insight-Card wert: „Kevin und Tobi halten 80 % deiner Kohle — Klumpenrisiko".

**Repayment Rate** (recovered / total lent, in %) pro Person und insgesamt — ideal für einen Donut mit Prozent-Hero in der Mitte. **Velocity** (neue Schulden vs getilgte Schulden pro Monat) als duale Balken zeigt, ob du gerade besser oder schlechter wirst, unabhängig vom absoluten Saldo. Und **Streaks** (längste offene Schuld in Tagen, längste Rückzahlstreak) triggern Duolingo-artige Retention — eine Badge-Card mit fetter Bebas-Neue-Zahl (47 TAGE) ist ein emotionaler Anker.

Weitere Metriken, die in die „Analytics"-Sektion gehören, aber nicht in den Hero-Bereich: Write-Off Rate, Time-to-First-Payment, Partial-Payment-Behavior, Seasonal Heatmap, Lifetime Volume, Interest Lost (entgangene Zinsen — witzig und aufklärerisch).

---

## Die neue Architektur — Bento statt Liste

Dein aktuelles Problem: **sieben gleichwertige Sektionen in Scroll-Reihenfolge**. Keine Hierarchie, keine Priorität. Die Lösung ist ein dreistufiger Aufbau, der den klassischen F-Pattern-Lesefluss bedient und Wichtiges nie unter dem Fold versteckt.

**Stage 1 — Hero + Sticky Header (erste 40 % der Screenhöhe).** Ganz oben ein `LargeTopAppBar` mit `exitUntilCollapsedScrollBehavior`: im expanded State zeigt es den Netto-Saldo als Bebas-Neue-Zahl (88sp), darunter Delta vs Vormonat, darunter die **scrubbare Line-Area-Chart** des Saldo-Verlaufs über den gewählten Zeitraum. Beim Scrollen kollabiert der Header zu einer kompakten Zeile mit der Zahl — sie bleibt immer erreichbar. Darunter Segmented Control mit 7D / 30D / 6M / ALL.

**Stage 2 — Bento-Grid mit Key-Metrics (zweiter Screen).** `LazyVerticalGrid(GridCells.Fixed(4))` mit gezielten `GridItemSpan`: eine große Tile über volle Breite für „At-Risk Betrag" mit rotem Akzent, zwei 2-Spalten-Tiles für „Rückzahlquote" (Progress-Ring) und „Ø Dauer" (KPI + Sparkline), vier 1-Spalten-Tiles für die Aging-Buckets. Das macht den zweiten Screen scanbar in zwei Sekunden.

**Stage 3 — Insight-Cards als horizontale LazyRow.** Automatisch generierte Callouts auf Regel-Basis: „⚠️ Kevin zahlt seit 47 Tagen nicht — 3× Reminder ignoriert", „🎉 Neuer Zahlungsrekord: 4 Debts in einer Woche eingetrieben", „💡 Deine Dezember-Schulden sind historisch 3× höher als sonst". Jede Card ist tappable und öffnet ein `ModalBottomSheet` mit Drill-Down. Drei Severity-Level (Warning / Info / Celebration) mit unterschiedlichem Border-Akzent.

**Stage 4 — Activity Heatmap.** Ein GitHub-Style-Grid (7 Zeilen × 26 Wochen), Gold-Akzent für Zahlungstage, dezentes Grau für Neu-Schulden-Tage. Zeigt Saisonalität auf einen Blick. Tap auf eine Zelle öffnet Bottom-Sheet mit Tagesdetail.

**Stage 5 — Top-Lists mit Personen-Cards.** Horizontale `HorizontalPager`-Card-Stacks statt statischer Top-5-Listen: swipebare Schuldner-Cards mit Avatar, Reliability-Ring, Betrag, Delta-Arrow und „Erinnern"-CTA. Zwei Pager: „Debtors" und „Creditors".

**Stage 6 — Collapsible Analytics-Deep-Dive.** Alles, was nicht in die oberen Stages passt (Status-Distribution, Kategorien-Donut, Velocity-Chart, Paid-Debts-Historie), landet in kollabierbaren Sektionen. Default collapsed. Progressive Disclosure nach NN/g.

**Stage 7 (separat, nicht in der Hauptseite) — Monthly Rapport als Story.** Ein dedizierter Entry-Point oben („MONATS-RAPPORT ANSEHEN") öffnet eine Full-Screen Story-Sequence im Klarna-Money-Story-Stil: goldene Count-Up-Animationen, B/W-Sticker, Street-Typografie, 5-7 Cards nacheinander.

**Die Reihenfolge-Logik:** Was muss der User in 2 Sekunden sehen? → Netto-Saldo + Trend (Stage 1). Was in 5 Sekunden? → Key Metrics + Insights (Stages 2-3). Was beim echten Interesse? → Drill-Downs (Stages 4-6). Und was für Delight? → Story-Rapport (Stage 7).

---

## Street-Aesthetic ohne Cringe-Falle

Die größte Gefahr bei Gangster-Rap-Ästhetik in einer Finanz-App ist, dass sie peinlich wird — Dollar-Zeichen-Sparkles, Goldketten-Icons, „HUSTLER"-Copy-Strings. Die Lösung ist **„Premium Street Finance"**: die Disziplin eines Robinhood-Dashboards, angereichert mit der Editorial-DNA von Off-White und der Album-Cover-Ästhetik von Kendrick/Pusha T. Die 60/30/10-Regel ist Pflicht: **60 % Schwarz-Surfaces, 30 % Off-White/Grau für Text, maximal 10 % Gold**. Gold ist kein Teppich, Gold ist eine Kette.

Das **Typografie-System** ist das Rückgrat. Oswald Bold UPPERCASE mit +120 letter-spacing für Labels („T O T A L  D E B T"), Bebas Neue in 72-96sp für alle Hero-Zahlen mit tabular figures (damit der Counter beim Hochzählen nicht wackelt), Roboto Condensed für Body und Listen. Ergänzungs-Vorschlag: **JetBrains Mono oder Space Mono** für Serial-Numbers und Timestamps. Die Off-White-DNA lebt genau in diesem Kontrast: Display-Bold neben klein-technisch-Mono. Serial-Nummern wie `BL-20260418-0042` auf jeder Schuld machen die App sofort hypebeast-referenziert, ohne dass du ein einziges Logo klauen musst.

**Texturen und Motive** setzen du sparsam ein. Ein globales Grain-Overlay mit 4-6 % Opacity (BlendMode.Overlay, 128×128 tileable PNG oder BitmapShader) nimmt der App die digitale Sterilität. „Taped Cards" mit zwei weißen Sticker-Schnipseln in den Ecken (leicht rotiert, 2-3°) geben Polaroid-Lookbook-Feel. Ein eigenes „EXPLICIT DEBT"-Badge in der Anmutung des Parental-Advisory-Logos kannst du einmal pro Screen für Overdue-Items einsetzen — niemals das RIAA-Original kopieren. Overdue-Stempel in Rot, 3° rotiert, mit rauer Kante, wirkt street. Barcodes oder Guilloche-Pattern als Subtile-Background-Ornament (2 % Opacity) in Transaction-Details fügen Geldschein-DNA hinzu, ohne Klischee zu werden.

**Farbsystem:** Pures `#000` meiden (kein Headroom für Grain) — `#0D0D0D` als Background, `#1A1A1A` für Cards, `#242424` für Modale. Gold `#FFD700` flat, nur für Hero-Zahl, primären CTA, aktiven Tab-Indikator. Chrome-Gradient (`#FFD700 → #B8860B → #FFF1A8`) maximal einmal pro Screen auf einem einzigen Hero-Element. Rot `#FF3B30` statt `#FF0000` (weniger alarmierend), Grün `#00D26A` statt `#00FF00`. Neon-Grün `#39FF14` nur für „Payment Received"-Celebration-Moments.

**Do's / Don'ts — die wichtigsten:**

- ✅ Oversized Numbers, UPPERCASE-Tracking-Labels, Grain-Overlay, Serial-Numbers in Mono, Taped-Card-Motive, asymmetrische Layouts mit linksbündigem Hero, tabular figures auf allen Zahlen, Haptic-Feedback auf kritischen Aktionen, sparsam Gold.
- ❌ Graffiti-Fonts (Urban Jungle, Ghetto Marquee), Drip-Emojis, 💰💵-Icons, Goldketten-Clipart, Comic Sans / Impact / Script, Gold-Gradients auf kleinem Text, Drop-Shadows auf Typografie, Purple/Magenta (das ist Vaporwave, nicht Hip-Hop), „HUSTLER / BOSS / GRIND"-Strings, Rotation über 5°, AI-generierte Gangster-Fotos.

Referenz-Moodboard zum Kalibrieren: **Off-White App Concept von Carlos Erazo (Behance), Cyberpunk 2077 UI Art Bible, GTA V Pausemenü, StockX App, Cuberto Finance Dashboard Dark, Drake OVO-Branding, Pusha T Daytona Cover, Kendrick DAMN.-Cover**. Genau diese Mischung: Dark-Finance-Disziplin × Street-Typo-Aggression × Hypebeast-Meta-Codes.

---

## Top 5 Quick Wins — diese Woche machbar

**1. Hero-Balance mit Animated Counter und tabular figures ersetzen.** Aktuell vier gleich große Tiles; neu: eine 88sp Bebas-Neue-Zahl linksbündig oben, darunter mini-Delta in Grün/Rot, rechts ein Mini-Chevron für Drill-Down. Die Zahl zählt beim Screen-Load von 0 hoch (1200ms, FastOutSlowInEasing). Implementation: `Animatable(0f)` + `LaunchedEffect` + `fontFeatureSettings = "tnum"`. Aufwand: 2-3 Stunden. Impact: der ganze Screen wirkt sofort teurer.

**2. Segmented Control für Zeiträume (7D / 30D / 6M / ALL) über dem Monats-Balkendiagramm.** Statt fester 6 Monate wählt der User jetzt den Zeitraum. Nutze Material 3 `SegmentedButton` mit Gold-Underline auf Active-State. Der bestehende Chart bleibt, nur die Datenquelle ändert sich. Aufwand: 3-4 Stunden.

**3. Grain-Texture-Overlay global.** Ein 128×128 Noise-PNG als `BitmapShader` mit `TileMode.REPEAT` und `BlendMode.Overlay` auf der Root-Surface, 5 % Opacity. Ein Modifier, zehn Zeilen Code, und die App hat sofort Editorial-Feel statt Default-Android-Look. Aufwand: 1-2 Stunden.

**4. Sticky Header mit kollabierender Hero-Zahl.** `Scaffold` + `LargeTopAppBar` + `exitUntilCollapsedScrollBehavior` + `nestedScroll`. Beim Scrollen schrumpft die Zahl von 88sp auf 24sp und bleibt in der TopBar sichtbar. Aufwand: 2-3 Stunden. Pattern ist Material-3-nativ.

**5. Insight-Cards als horizontale LazyRow über den Stats.** Regel-basiert: scanne Daten einmal, generiere 3-5 Callouts („Kevin zahlt seit 47 Tagen nicht", „Rekord-Monat: 4 Debts eingetrieben"). `sealed class Insight` mit drei Varianten, `OutlinedCard` mit farbigem `BorderStroke` je Severity. Das ist der **größte Wahrnehmungssprung** bei minimalem Aufwand — die App fühlt sich plötzlich an, als würde sie mitdenken. Aufwand: 6-8 Stunden (inkl. Regel-Engine).

---

## Top 3 Game Changer — real game-changing

**Game Changer 1: Scrubbare Saldo-Linie mit Live-Update (Robinhood-Pattern).** Ersetze den statischen 6-Monats-Balkenchart durch eine Line-Area-Chart des Netto-Saldos über Zeit, bei der der Finger-Drag ein vertikales Crosshair erzeugt, die Hero-Zahl oben live zum historischen Wert updated und Haptik (tick-pattern) gibt. Implementation: Vico `LineCartesianLayer` mit `LineFill.single(Brush.verticalGradient(gold-to-transparent))` für Gradient-Fill + `CartesianMarker` + `rememberMarkerVisibilityListener` für Scrub-Events + `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)`. Das ist der einzelne sichtbarste Upgrade — User spüren sofort, dass die App nicht mehr statisch ist. Aufwand: 2-3 Tage. Impact: maximal.

**Game Changer 2: Reliability-Score pro Person mit Gradient-Progress-Ring und Cash-Flow-Forecast.** Öffne ein `ModalBottomSheet` beim Tap auf einen Debtor. Darin: ein großer Gradient-Progress-Ring (0-100, Sweep-Gradient `#FF3B30 → #FFD700 → #00D26A`) mit dem Reliability-Score, darunter ein Ranking-Badge (A+/A/B/C/D), dann ein Mini-Chart der historischen Zahlungsdauer dieser Person, dann die prognostizierte Rückzahl-Wahrscheinlichkeit der offenen Schulden in % und Cash-Flow-Forecast in Tagen. Das ist der **funktionale USP gegen Splitwise**: keine andere Debt-App gibt dir eine derart präzise „Ist-diese-Person-zuverlässig?"-Antwort. Backend ist simpel (gewichteter Score aus fünf historischen Kennzahlen). Aufwand: 4-6 Tage. Impact: das ist das Feature, von dem User ihren Freunden erzählen.

**Game Changer 3: Monats-Rapport als animierte Story-Sequence (Klarna-Money-Story-DNA).** Ein separater Entry-Point oben rechts in der TopBar („📼 RAPPORT"). Tap öffnet eine Full-Screen `HorizontalPager` mit 5-7 Story-Cards: (1) „Dein April 2026" Titelkarte mit Chrome-Gold-Wordmark, (2) „Du hast €X,XXX zurückbekommen" mit Countup-Animation, (3) „Größter Schuldner: [Name]" mit animiertem Reliability-Ring, (4) „Dein Streak: 14 Tage clean" mit Badge-Flip, (5) Heatmap-Reveal „Deine Zahlungstage", (6) „Pareto-Alert: 80 % deiner Schulden kommen von 2 Leuten", (7) Share-CTA mit Story-Image-Export. Street-Typografie, goldene Ticker-Tape-Confetti beim Reveal, Bass-Tick-Haptik bei jedem Swipe. `HorizontalPager` + `AnimatedVisibility` + `Animatable` + `HapticFeedback`. Das ist viral. Klarna hat mit diesem Pattern +68 % Budget-Setter generiert — Retention-Hebel sondergleichen. Aufwand: 8-10 Tage. Impact: langfristig der größte Hebel.

---

## Zehn konkrete Verbesserungen mit Compose-Umsetzung

Geordnet nach Impact/Aufwand-Verhältnis. Jede Verbesserung kommt mit **WAS / WARUM BESSER / WIE in Compose**.

**1. Hero-Card mit oversized Bebas-Neue-Zahl + Countup**
**[Was]** Netto-Saldo als 88sp-Display ganz oben, linksbündig, mit `€` superscript klein, darunter Delta-Arrow in Grün/Rot und Sparkline. **[Warum besser]** Beantwortet „Wie stehe ich gerade?" in <1 Sekunde — aktuell beanspruchen vier 4-Tile-Karten gleiche Aufmerksamkeit, keine Priorität. **[Wie]** `Text(style = typography.displayLarge.copy(fontFeatureSettings = "tnum"))` + `Animatable` Countup + `Modifier.wrapContentWidth(Alignment.Start)`. Sparkline als Canvas mit `drawPath` oder Vico `LineCartesianLayer` ohne Achsen.

**2. Scrubbare Saldo-Linie mit Haptik**
**[Was]** Interaktive Line-Area-Chart unter dem Hero, Finger-Drag bewegt Crosshair + updated Hero-Zahl live. **[Warum besser]** Transformiert statisches Display in exploratives Tool; Robinhood-Gold-Standard. **[Wie]** Vico `CartesianChartHost` + `LineCartesianLayer` mit `LineFill.single(fill(Brush.verticalGradient(listOf(Gold.copy(alpha=0.3f), Color.Transparent))))` + `rememberMarker()` + `CartesianMarkerVisibilityListener` → State hochreichen, Hero-Zahl daran binden + `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.TextHandleMove)` bei Position-Wechsel.

**3. Bento-Grid statt linearer Tiles**
**[Was]** Asymmetrisches Grid: eine breite Tile (At-Risk), zwei 2-Span-Tiles (Rückzahlrate, Ø Dauer), vier 1-Span-Tiles (Aging Buckets). **[Warum besser]** Tile-Größe kommuniziert Wichtigkeit — aktuell sind alle Tiles gleich groß. **[Wie]** `LazyVerticalGrid(GridCells.Fixed(4))` + gezielte `item(span = { GridItemSpan(maxLineSpan) })` für Hero-Tile + `item(span = { GridItemSpan(2) })` für Mid-Tiles. Spacing 12.dp.

**4. Segmented Control für Zeiträume**
**[Was]** 7D / 30D / 6M / ALL Tabs über jedem Zeitreihen-Chart. **[Warum besser]** Universelle Konvention, null Lernkurve, erhöht Explorations-Willen. **[Wie]** Material 3 `SingleChoiceSegmentedButtonRow` + `SegmentedButton(selected = index == selectedIndex, ...)` mit custom Gold-Underline-Indikator bei Active-State.

**5. Insight-Cards als horizontale LazyRow**
**[Was]** Auto-generierte Callouts auf Regel-Basis. **[Warum besser]** App fühlt sich intelligent an; hohes Perceived-Value pro Dev-Stunde. **[Wie]** `sealed interface Insight { data class Warning(...), Info(...), Celebration(...) }` + `LazyRow` mit `OutlinedCard` je Severity-Color (`BorderStroke(1.5.dp, accent)`). Regel-Engine: einmal pro Session berechnen und cachen. Beispiele: `daysSinceLastPayment(p) > 45 → Warning`, `paretoTop2Share > 0.75 → Info`.

**6. GitHub-Style Activity-Heatmap**
**[Was]** 7×26 Grid der letzten 26 Wochen, Intensität = Anzahl Zahlungen/neue Schulden pro Tag. **[Warum besser]** Zeigt Saisonalität und Muster auf 120dp Höhe — was ein Balkendiagramm nie in dieser Dichte schafft. **[Wie]** Canvas + doppelte Schleife `for (c in 0 until cols) for (r in 0 until 7) drawRoundRect(color = gold.copy(alpha = 0.08f + 0.92f * intensity), ...)`. Gap 3.dp, CornerRadius 25% der Zellgröße. Tap → Bottom-Sheet mit Tagesdetail.

**7. Progress-Ring mit Sweep-Gradient für Reliability-Score**
**[Was]** Kreisförmige 0-100-Anzeige mit Gradient `#FF3B30 → #FFD700 → #00D26A`, Zahl in der Mitte. **[Warum besser]** Einzelner Score + Trend-Arrow machen komplexe Person-Zuverlässigkeit in einer Glance greifbar. **[Wie]** Canvas mit `drawArc(brush = Brush.sweepGradient(...), startAngle = -90f, sweepAngle = 360f * score, style = Stroke(14.dp.toPx(), cap = StrokeCap.Round))` + `Animatable` für Progress-Animation.

**8. Swipebare Top-Debtor-Cards (HorizontalPager)**
**[Was]** Top-5-Schuldner als Pager statt statischer Liste, jede Card mit Avatar, Reliability-Ring, Betrag, Quick-Action-Button. **[Warum besser]** Gestensensitiv, fokussiert auf eine Person zur Zeit, bietet Quick-Actions. **[Wie]** `HorizontalPager(state = rememberPagerState { debtors.size }, contentPadding = PaddingValues(horizontal = 32.dp), pageSpacing = 12.dp)` + pro Page eine `PersonCard`. Pagination-Dots via `HorizontalPagerIndicator` (Accompanist) oder Custom.

**9. ModalBottomSheet für Drill-Downs**
**[Was]** Tap auf jede KPI-Tile oder Person öffnet ein Bottom-Sheet mit Detail-Chart und Transaction-History. **[Warum besser]** Hält Kontext der Hauptseite sichtbar, ist einhandtauglich, ist Material-3-nativ. **[Wie]** `ModalBottomSheet(onDismissRequest = ..., sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false), dragHandle = { BottomSheetDefaults.DragHandle() })`. Inhalt: Vico-Chart + Transaction-LazyColumn.

**10. Custom Typografie + Grain-Overlay global**
**[Was]** Oswald/Bebas Neue/Roboto Condensed in Compose-Theme, plus globales Grain-Overlay mit 5 % Opacity. **[Warum besser]** Transformiert den gesamten Look der App von „Standard-Android" zu „Editorial-Magazin". **[Wie]** `FontFamily(Font(R.font.oswald_bold, FontWeight.Bold))` in `res/font/`, im Theme an Material-3-Typography-Rollen binden (`displayLarge = TextStyle(fontFamily = BebasNeue, fontFeatureSettings = "tnum")`). Grain: `BitmapShader` mit `Shader.TileMode.REPEAT` auf 128×128 Noise-PNG, via `Modifier.drawWithCache` mit `onDrawWithContent { drawContent(); drawRect(brush = shaderBrush, alpha = 0.05f, blendMode = BlendMode.Overlay) }`.

**Chart-Library-Empfehlung nach Typ:** Vico für Line-with-fill (Saldo-Verlauf) und Bar (Velocity). Canvas-Custom für Sparklines (minimalistisch), Heatmap (keine Lib deckt das gut ab), Progress-Ring (zu simpel für Lib-Overhead). YCharts nur, wenn du einen komplett out-of-the-box Donut mit Labels brauchst — ansonsten `drawArc(style = Stroke)` reicht. **Tabular figures (`fontFeatureSettings = "tnum"`) sind Pflicht** auf allen Zahlen — ohne sie wackeln Counter und Tabellen beim Update, und das killt Premium-Feel sofort.

---

## Conclusion — der Sprung, den du machst

Die transformative Einsicht aus der Recherche: **Finance-Apps, die als premium wahrgenommen werden, verzichten auf Vollständigkeit zugunsten von Hierarchie**. Robinhood zeigt dir nicht alle Metriken — es zeigt dir eine Zahl und eine Chart. Monzo versteckt 80 % der Analytics in „Trends". Cash App zeigt im Homescreen praktisch nichts außer dem Saldo. Was sie besser machen als BugList aktuell, ist nicht die Menge an Information, sondern die **Priorisierung**. Dein Screen hat zu viele Tiles mit gleichem Gewicht.

Die zweite Einsicht ist gegenläufig zum ersten: **Die Street-Ästhetik ist kein Skin über einer Standard-App, sondern das Differenzierungsmerkmal gegen 100 % des Marktes**. Es gibt keine Debt-App mit Hypebeast-Editorial-DNA. Das ist der Platz, den BugList besetzen kann — und genau deshalb ist die Typografie-Disziplin (Oswald + Bebas Neue + Mono, Uppercase-Labels, tabular figures, oversized Numbers, Grain-Overlay, Gold als Kette) wichtiger als jedes einzelne Feature. **Wenn du nur drei Dinge umsetzt: Hero-Zahl in Bebas Neue 88sp, Grain-Overlay, Reliability-Score-Ring**. Damit hebst du dich schon visuell und funktional von Splitwise, Tricount und Settle Up ab.

Und die dritte Einsicht: **Story-Formate schlagen statische Dashboards im Retention**. Klarnas Money Story hat deshalb funktioniert, weil sie Finanzen in ein Format gießt, das User aus Instagram kennen. Dein Monats-Rapport mit goldenen Count-Ups, Street-Typografie und Bass-Tick-Haptik ist der Baustein, der BugList zu einer App macht, über die Leute reden.

Aktueller Status: solide Infrastruktur, boring Layer on top. Nach dem Redesign: **ein Tool, das wie ein Rap-Album-Liner-Notes aussieht und wie ein Bloomberg-Terminal denkt**. Das ist der Abstand, den du in zwei Releases zurücklegen kannst.