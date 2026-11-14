# Release 2.6.3 (78): Betragseingabe und einzelne Tilgungen

## Änderungen

- Alle Schnellbeträge entfernt, einschließlich der negativen Werte. Die gemeinsame Betragseingabe für neue Summen und Tilgungen hat einen kontrastreichen cyanfarbenen Rand an jeder Taste.
- Bei „Tilgung“ sind Überschrift, Personenname, Richtung, offene Schulden und Vorschau über der Betragseingabe entfernt. Angezeigt werden nur Schließen, Summenfeld, Bestätigung und Tastatur.
- Die Tilgungstastatur passt ihre Tastenhöhe an kleinere Displays an. Ziffern und Löschtaste sind ohne festen vertikalen Innenabstand mittig ausgerichtet, damit sie auf kleineren Tasten vollständig sichtbar bleiben.
- Ziffern und Löschtaste reagieren mit einem kräftigen LED-Neonimpuls, äußerem Glow, Nachleuchten und einer Federbewegung; keine ständig laufende Animation oder zusätzliche Asset-Datei.
- „Bezahlt“ zeigt jede gespeicherte Zahlung mit Richtung, Datum und Betrag, auch bei noch offenen Schulden. Neueste Zahlungen stehen oben.
- Die zugehörige Schulden- und Zahlungshistorie öffnet sich erst beim Tippen auf den Betrag. Die Kopierfunktion verwendet dieselben einzelnen Zahlungen.
- Alte Bezahlt-Markierungen ohne Zahlungsdatensatz bleiben als solche erkennbar; ein fehlendes Bezahldatum wird nicht erfunden.
- Datenbankschema, FIFO-Zuordnung, Sync und bestehende Zahlungen bleiben unverändert.
- Nos-Easter-Egg nach erfolgreicher Tilgung: eigene animierte MVP-Plakette mit Vektor-Kusssiegel und Partikeln. Zeigt die insgesamt getilgte Summe und die Rückzahlungsquote nach Geldbetrag, ohne „EHRE, NOS.“.
- MVP-Statistik verwendet alle historischen Einträge der Person, unabhängig vom aktiven Tab. Stornierungen sind ausgeschlossen; Währungen und Zahlungsrichtungen werden getrennt berechnet. Alte Bezahlt-Markierungen ohne Zahlungsdatensatz zählen als vollständig getilgt. Keine Verrechnung mit Gegenschulden.

- Erneutes Öffnen der Tilgung kann keinen alten Erfolg mehr auslösen: frischer Zustand beim Laden, einmaliges Abschlussereignis, Sperre gegen doppelte Buchung und kein MVP bei Fehlern oder Nullbuchungen.

- „Neue Schuld“ und Bearbeiten haben ein fest sichtbares X; Wischgeste und Schließen per Hintergrund sind abgeschaltet. Die Eingabe lässt sich weiterhin intern scrollen.
- Bestätigungsbuttons für neue Summen und Tilgung sind größer, gelbgolden mit dunkler Schrift, hellem Rand und Häkchen.

## Prüfung

- Build 75: 118 Unit-Tests und 36 instrumentierte Android-Tests bestanden. Release-Build erfolgreich; Lint ohne Fehler.
- Update der signierten GitHub-Version 2.6.1 auf Build 73 im API-36-Pixel-Emulator: vorhandene Person und 100-Euro-Schuld erhalten.
- Danach 20 Euro und 50 Euro getilgt: offen bleiben 30 Euro. „Bezahlt“ zeigt zwei Zeilen mit 50 Euro und 20 Euro sowie jeweiligem Datum. Keine zusätzliche 100-Euro-Zahlung.
- Die Historie ist zunächst verborgen und lässt sich am Betrag öffnen.
- S26: Update der installierten GitHub-Version 2.6.1 (72) auf 2.6.2 (75) erfolgreich. Sichtbare Bestandsdaten auf Startseite und Gastliste vor/nach identisch; Belegnummern, Gastrolle und Kopplung erhalten.
- Build 76 auf dem S26: festes X sichtbar und funktionsfähig; Wischen schließt die Summeneingabe nicht. Neue Tilgungsmaske und Zifferneingabe geprüft, ohne eine Zahlung in Bestandsdaten zu buchen.
- Build 77 ergänzt einen direkten LED-Impuls auf Klick mit Mindestleuchtdauer, einschließlich erster Berührung und barrierefreier Aktivierung. 118 Unit-Tests und 36 instrumentierte Android-Tests bestanden; Lint ohne Fehler.
- Öffentlicher Build 78 verwendet denselben geprüften Anwendungscode, mit Version 2.6.3 statt der Testversion 2.6.2, damit auch Testgeräte das In-App-Update erkennen.
- Nach Build 76 wurde das USB-Kabel des S26 getrennt. Die zusätzliche LED-Korrektur und die öffentliche Versionsnummer wurden deshalb nicht mehr per USB auf diesem Gerät installiert.

## Auslieferung

- Test-APK: `build/qa/release-2.6.3/app-release.apk` (Build 78).
- Bisheriges Release-Zertifikat; direkt über 2.6.1 installierbar.
- Noch nicht als neues GitHub-Release veröffentlicht.
