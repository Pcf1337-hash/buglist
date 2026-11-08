# Plan: Tilgen und Ambient

1. Reiner Filter `TiltFilter` plus Matrix-Winkel, Unit-Tests zuerst: Totzone, Clamp, 8 dp, Tiefpass, Neutral, Identitätsmatrix.
2. `AmountInputPad`: Parameter für Tilgen, Plus/Minus nur wenn eingeschaltet, fester Cyan-Rand und 8 dp nur dort.
3. `SettlementSheet`: Gesten aus, Scrim aus, Zurück an, Schließen-Knopf, festes Ziffernfeld, Kopf scrollt, Sensor-Pause.
4. Hintergrundlicht und Kantenreflex lesen den Versatz nur beim Zeichnen. Schalter in den Einstellungen, Standard an. Backup-Feld mit Default, alte Backups bleiben gültig.
5. `versionCode` 58, `versionName` bleibt 2.6.0. Kein Release.
6. `compileDebugKotlin` und `testDebugUnitTest`. Emulator und S26 macht Dennis.
