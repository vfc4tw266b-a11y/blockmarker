# Block Marker (Fabric, Minecraft Java 26.2)

Markiert 100 feste Koordinaten. Die Markierungen sind durch Waende und Boden sichtbar, weil sie direkt auf dem
Bildschirm gezeichnet werden (orangener, pulsierender Rahmen um den Block, dazu Nummer und Entfernung).

- F6: entfernt die Markierung, die dir am naechsten ist (erscheint danach nicht mehr)
- F9: oeffnet die Liste aller Koordinaten, hinter jedem Eintrag ein Schalter (wieder markieren / ausblenden),
  dazu "Alle an", "Alle aus" und Sortierung nach Nummer oder Naehe
- Beide Tasten aenderbar unter Optionen > Steuerung > Tastenbelegung > "Block Marker"
- Ausgeblendete Markierungen werden in config/blockmarker.properties gespeichert

Die Koordinaten stehen in `Markers.java` und lassen sich dort aendern.

Bauen ohne Programme: Repository auf GitHub anlegen, alle Dateien hochladen, die Datei
.github/workflows/build.yml von Hand anlegen, unter "Actions" den Bau abwarten und unter "Artifacts" die .jar laden.
