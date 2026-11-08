# Tilgen und Ambient, freigegeben 2026-11-08

Stand der Umsetzung: Commit auf `a5a3db5`. Kein Release.

## Tilgen

Nur dieses Fenster. Neue Schuld und Teilzahlung behalten die Plus- und Minuszeilen.

- Kein Schließen durch Wischen oder Tippen auf den Scrim.
- Schließen nur über „Schließen“ und die System-Zurück-Taste, ohne Buchung.
- Installierte Material-3-API (BOM 2026.09): `sheetGesturesEnabled = false`, `dragHandle = null`, `shouldDismissOnBackPress = true`, `shouldDismissOnClickOutside = false`. Der öffentliche Konstruktor hat kein `shouldDismissOnScrimClick`. `securePolicy` bleibt ungesetzt.
- Keine 80-Prozent-Schwelle.
- Ziffernfeld fest unten, Kopf und Vorschau dürfen scrollen. Tasten 64–72 dp, nie unter 48 dp, Abstand 8 dp, fester Cyan-Rand. Komma, Löschen, Bestätigen bleiben erreichbar. Große Schrift wird nicht künstlich unlesbar verkleinert. Reicht die Höhe nicht, wird das Sheet hoch und der Kopf scrollt.

## Licht

Schwarz und Tiefblau. Breites cyanblaues Licht nur im Hintergrund, plus ein sparsamer Reflex auf dekorativen Kartenrändern. Text, Salden, Richtung und Tasten bleiben an ihrem Platz. Der Tastenrand im Tilgen-Feld ist konstant.

Sensor: `TYPE_GAME_ROTATION_VECTOR`, Pitch und Roll aus der Rotationsmatrix nach Display-Remap, nicht aus rohen Quaternionen. Neutralpunkt bei Resume und Drehung. Tiefpass 275 ms, Totzone 1 Grad, Eingabe ±12 Grad, Verschiebung höchstens 8 dp. Nur während `RESUMED`. Listener aus in der Pause und solange Tilgen offen ist. Eigener Schalter. Systemanimationen 0 oder fehlender Sensor: statisches Bild. Kein Hintergrunddienst, keine Ortung, kein Tempo, kein Hand- oder Auto-Versprechen. Kein Korn, kein Blur, kein RenderEffect.

Grün bleibt „schuldet mir“, Rot bleibt „ich schulde“. `FLAG_SECURE`, Biometrie und Sync bleiben.
