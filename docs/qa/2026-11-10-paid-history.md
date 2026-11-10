# Nachvollziehbare Bezahltliste – Teststand 10.11.2026

## Verhalten

Die Tilgung bezahlt weiterhin die ältesten offenen Schulden zuerst. Das Archiv zeigt
jetzt, wann jede einzelne Schuld vollständig ausgeglichen wurde.

- „Bezahlt“ enthält ausschließlich vollständig bezahlte Schulden. Stornierte Einträge bleiben unter „Alle“.
- Sortierung und Tagesgruppen beziehen sich auf das Bezahldatum, neueste zuerst.
- Das ursprüngliche Schulddatum bleibt als „Schuld vom …“ separat sichtbar.
- Abschlussdatum = Zeitpunkt der chronologisch ersten Zahlung, mit der der Originalbetrag vollständig erreicht wird.
  Die Berechnung erfolgt in Cent und funktioniert auch bei unsortiert gelieferten Zahlungsdatensätzen.
- Aufklappbarer Verlauf: Datum/Uhrzeit, Betrag, optionale Notiz und Restbetrag nach jeder Zahlung.
  Die abschließende Zahlung wird mit „Damit vollständig bezahlt“ hervorgehoben.
- Fehlt ein vollständiger Zahlungsnachweis, erscheint „Bezahldatum nicht erfasst“.
  Es wird kein Datum aus dem ursprünglichen Schulddatum oder dem heutigen Tag erfunden.
  Unvollständige Zahlungsaufzeichnungen werden entsprechend gekennzeichnet.
- Erhaltene und selbst gezahlte Summen sind getrennt; unterschiedliche Währungen werden nicht zusammengerechnet.
- Kompakter Archivkopf, explizite Kopierfunktion mit Abschlussdatum, Tags und Belegnummer in den Details.
- Neue Datenbankfelder oder Migrationen sind nicht nötig; bestehende Zahlungsdaten und Sync bleiben verwendbar.

## Verifikation

- 90 Unit-Tests bestanden (Gradle MCP b-7), darunter acht neue Tests für Abschlusszeitpunkt,
  Reihenfolge, Altdaten, Stornierungen, Teilzahlungen und Cent-Rundung.
- 30 instrumentierte Tests auf `emulator-5554` bestanden (b-8), darunter zwei neue Compose-Tests:
  ursprüngliches Datum versus Bezahldatum, aufklappbare Restbeträge und unbekannte Altdaten.
- Finale Debug- und Release-Builds erfolgreich (b-10).
- Lint Debug: 0 Fehler, 121 bestehende Hinweise; Release-Lint erfolgreich.
- Finale signierte APK auf `emulator-5556` als Update installiert. Vorhandene Testschulden und Zahlungen erhalten.
- Manuell und direkt im Emulatorfenster geprüft: kompakte Liste, Tagesgruppe, vollständiger Karteneintrag,
  Aufklappen und Scrollen des Zahlungsverlaufs.
- Bestandsbeispiel: 125,50 € Schuld, Zahlungen 25,50 €, 20 € und 80 €.
  Verlauf zeigt Rest 100 €, dann 80 €, dann vollständigen Ausgleich; Belegnummer bleibt erhalten.
- Nach dem Start und der Bedienung der finalen Release-APK: leerer Crash-Puffer.
- Es wurde ausschließlich auf den Emulatoren gearbeitet; das zusätzlich angeschlossene physische Gerät wurde nicht verändert.

Der Test auf dem persönlichen Handy und die Bewertung mit dem Kollegen bleiben vor Veröffentlichung sinnvoll.
Es wurden kein GitHub-Release, kein Tag und kein Push vorgenommen.

## APK

- `build/qa/BugList-v2.6.0-paid-history.apk`
- Version 2.6.0, versionCode **59**, Paket `com.buglist`.
- 13.876.407 Bytes; bisheriger Release-Signierschlüssel, Signatur mit `apksigner` geprüft.
- SHA-256: `d06b5c01f688386d8c5353a0b56dcd74dfa6c8cd666f94d7276eec802cd5b394`.
- Die vorherige Test-APK (Build 58) bleibt unter ihrem bisherigen Dateinamen erhalten.
