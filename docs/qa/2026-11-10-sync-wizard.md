# Sync-Wizard und Neigungseffekt – 10.11.2026

## Änderungen

- Sichtbarer Einstieg „Handys koppeln“ oben in der Personansicht.
- Geführte Schritte: QR anzeigen/scannen (alternativ Code eingeben), Rollen auf beiden Geräten
  auswählen und ausdrücklich bestätigen, ersten Abgleich prüfen, fertig.
- Ein Host führt die Liste; das Gegenüber übernimmt sie gespiegelt im Nur-Ansehen-Modus.
  Der Wizard warnt vor dem Ersetzen der Schulden und Zahlungen dieser Person auf dem Gast.
- Gleiche Rollen werden erklärt und lassen sich korrigieren. Während der Einrichtung prüft der
  Wizard automatisch alle vier Sekunden, nur im Vordergrund und ohne überlappende Abfragen.
- Bereits im Server-Cache vorhandene eigene Hello-/Rollenmeldungen werden beim Prüfen nicht
  erneut veröffentlicht. Eine geänderte Rolle oder ein abgelaufener Cache wird erneut gesendet.
- Kamera-Berechtigungsdialog schließt die App nicht mehr. Echtes Verlassen während des Dialogs
  sperrt weiterhin die Sitzung. Kamera wird beim Schließen des Scanners freigegeben.
- Der vorhandene Gyro-Effekt war durch eine deckende Root-Surface verdeckt. Die Root-Surface
  ist jetzt transparent; auch neue Bezahltkarten besitzen wieder den dezenten bewegten Rand.
  Die früher entfernten Blur-/Grain-Effekte wurden nicht wieder eingeführt.
- Bedienungsanleitung: `docs/SYNC-ANLEITUNG.md`.

## Verifiziert

- 107 Unit-Tests bestanden (Gradle MCP b-15), inklusive Wizard-Zuständen,
  Berechtigungs-Lifecycle und Cache-/Rollenwechsel-Regressionsfällen.
- 35 instrumentierte Tests bestanden (b-13): unter anderem echte QR-Erzeugung und Decodierung,
  ausdrückliche Rollenbestätigung, Gast-Warnung, manuelle Eingabe, Produktions-Root-Hintergrund.
- Kamera-Berechtigungsdialog und laufende Kameravorschau auf Release-Build 62 getestet.
  Home aus dem Berechtigungsdialog und anschließender Neustart fordern erneut Entsperrung.
- Debug- und signierter Release-Build 63 erfolgreich (b-15). Lint: 0 Fehler, 135 Hinweise
  (darunter durch den Wizard ersetzte, nun ungenutzte alte Texte); Release-Lint erfolgreich.
- Echter Versionswechsel von der GitHub-APK 2.5.1 auf 2.6.0: vorher erstellte Person,
  100-Euro-Schuld und Belegnummer erhalten. Weitere Updates bis Build 63 erhalten diese Daten.
  Der frühere Test mit Schuld, Teilzahlung und Belegnummer ist im ersten QA-Bericht dokumentiert.
- Zwei Release-Emulatoren: Code übernommen, beide Rollen als Host bestätigt;
  Konflikt und Erklärung auf beiden sichtbar, ohne Datenersatz.
- Mit finalem Build 63 ein Gerät auf Nur ansehen geändert, ausdrückliche Warnung geprüft
  und bestätigt. Beide Geräte wechseln automatisch auf „Verbindung eingerichtet“.
- Gast zeigt statt der bisherigen 100 Euro den gespiegelten Host-Stand +20 Euro an;
  Host behält -20 Euro. Beleg BL-20261110-0002 stimmt überein. Gast hat keine Bearbeitungsaktionen.
- Gast-Archiv enthält die vollständig bezahlte Schuld 125,50 Euro aus der anderen Perspektive,
  Abschlusszeit 15:40, Zahlungen 25,50 / 20 / 80 Euro, Rest 100 / 80 / 0 Euro,
  Beleg BL-20261110-0001. Anzeige und aufgeklappter Verlauf im Emulatorfenster kontrolliert.
- Leere Crash-Puffer auf beiden Emulatoren nach dem finalen Durchlauf.
- Optischer Scan eines zweiten Displays ist auf einem echten Handy noch zu prüfen:
  Die virtuelle Kamera wurde gestartet, aber nicht erfolgreich auf das Testposter ausgerichtet.
  Der vollständige Abgleich wurde im Emulator über die alternative Code-Eingabe geprüft.

## APK

- `build/qa/BugList-v2.6.0-sync-wizard.apk`, Version 2.6.0, Build **63**, 13.908.979 Bytes.
- SHA-256: `bd7a05d430dadffe7070209410f39b0f1fc5343e9160d08571d8dc7e5246943d`.
- `apksigner verify` erfolgreich. Signaturzertifikat stimmt mit der originalen GitHub-APK 2.5.1
  überein: `b65244887feea55db81a044007d8d337af9a7a9d46a6bc59de69f0c4fb540923`.
- Auf beiden Emulatoren als Update installiert. Keine Deinstallation der Bestandsdaten für diesen Update-Test.

## Veröffentlichung

Nur Emulatoren wurden verändert. Das angeschlossene physische Handy blieb unangetastet.
Kein Push, Tag oder GitHub-Release; Veröffentlichung erst nach ausdrücklichem Nutzer-OK.
