# Texture Mixer (Fabric, Minecraft Java 1.21.11)

Ein Texture-Pack-Editor direkt im Spiel: Pro Kategorie (Crystals, Pearls, Totems, Schwerter, Rüstung, Betten ...)
wählst du **unabhängig** voneinander das Pack, aus dem die Textur kommen soll. Die Mod baut daraus zur Laufzeit ein
virtuelles Resource Pack, das ganz oben im Stapel liegt – deine Original-Packs werden nie verändert.

Dazu gibt es ein zweites Menü (Settings / Sounds) mit PvP-, Hand-, Screen-, Textur-, GUI- und Sound-Einstellungen.

## Bedienung

* **U** (änderbar unter Steuerung → „Texture Mixer") öffnet den Texture Mixer.
* Oben: Kategorie-Leiste mit Item-Icons (Mausklick oder ← / →). Darunter das Raster mit allen Packs, die für diese
  Kategorie etwas enthalten. Ausgewählt = Rahmen + Häkchen. „Default" = kein Override.
* **Rechtsklick** auf eine Kachel = Favorit (★). Der Button „Favorites" filtert darauf. Oben rechts: Suchfeld.
* Unten: Profile (z. B. *Survival*, *PvP*, *Crystal PvP*; Neu / Löschen), Reset Kategorie, Reset alles, Packs neu einlesen.
* **Done** übernimmt die Auswahl, speichert sie und lädt die Resource Packs im laufenden Spiel neu. **Esc** verwirft.
* Packs einfach in den Ordner `.minecraft/resourcepacks` legen (Zip oder entpackter Ordner) – sie werden automatisch erkannt.
  Die Packs müssen **nicht** aktiviert sein.

## Settings-Menü – was wirklich funktioniert

Jede Einstellung hat eine Beschreibung (Tooltip beim Überfahren). Alles wird sofort gespeichert und wirkt sofort.
Was ich **nicht** ehrlich umsetzen konnte, ist im Menü als „nicht verfügbar" markiert und hat keine Schalter.

Umgesetzt: Hit Color, Hit Particles, Custom Hit Effects, Enchant Glint (Textur-Auswahl), Fullbright, Particles,
FPS Display, Ping Display, Keystrokes, Crystal/Pearl/Totem/Sword/Armor Texture (direkt mit dem Mixer verbunden),
GUI Scale, Menu Animation, Menu Transparency, Theme, Accent Color, Master Volume, GUI Sounds, Button Click Sound,
Hover Sound, Hit Sound.

Noch nicht umgesetzt (benötigt Eingriffe in Render-/Shader-Internals, die ohne Test gegen 1.21.11 zu riskant wären):
Crystal Glow, Damage Color, Swing Animation, Item Size/Position/Rotation, Ground Items, Zoom, Crosshair,
Weather & Time, Fire Overlay, Potion Effects, HUD Customization, Texture Quality, Custom Texture Effects,
Background Blur, Item/Crystal/Pearl/Totem Sounds, Custom Sound Packs.

## Kompilieren

Voraussetzung: **JDK 21** (`java -version` muss 21 anzeigen) und Internet für den ersten Build.

```
./gradlew build          # Windows: gradlew.bat build
```

Die fertige Mod liegt danach in `build/libs/texture-mixer-1.0.0.jar` (nicht die `-sources.jar`).
Zum Testen direkt aus Gradle: `./gradlew runClient`.

## Installieren

1. Fabric Loader für Minecraft 1.21.11 installieren (https://fabricmc.net/use/installer/).
2. **Fabric API** für 1.21.11 in den `mods`-Ordner legen.
3. `texture-mixer-1.0.0.jar` in denselben `mods`-Ordner legen.
4. Packs in `resourcepacks` ablegen, Spiel starten, **U** drücken.

## Wichtig: Versionen

Das Projekt wurde ohne Zugriff auf die Minecraft-Jars geschrieben. Der Kern (Pack-Erkennung, Kategorien, Mixing,
Config) ist mit deinen Packs getestet. Die Teile, die Minecraft-APIs benutzen (GUI, Mixins), konnten nicht
kompiliert werden. Sie nutzen Mojang-Mappings für 1.21.11. Alle versionsabhängigen Aufrufe (Identifier,
Rendering, Texturen, Sounds) stehen gebündelt in `client/McCompat.java`. Bei Compile-Fehlern wird fast immer nur
diese Datei (und die Mixins) angepasst. Versionen stehen in `gradle.properties`; aktuelle Werte findest du auf
https://fabricmc.net/develop. Im `latest.log` sollte beim Start `VirtualPackSource registriert` stehen.

## Eigene Kategorien ergänzen

`config/texturemixer/categories/meine.json` anlegen (gleiches Format wie `assets/texturemixer/categories.json`):

```json
{ "categories": [ {
  "id": "tridents", "name": "Tridents", "icon": "minecraft:trident",
  "items": ["trident"],
  "files": ["textures/entity/trident*.png"],
  "preview": ["textures/item/trident.png"]
} ] }
```

`items` erzeugt automatisch die Muster für `textures/item`, `models/item` und `items`. `*` steht für beliebige Zeichen
innerhalb eines Ordners, `**` über Ordner hinweg. Für Übersetzungen den Key `texturemixer.category.<id>` in der Lang-Datei ergänzen.

## Dateien

* `core/` – Minecraft-unabhängiger Kern (Pack-Scan, Glob-Regeln, Style-Auflösung inkl. Modell-Abhängigkeiten, Config).
* `pack/` + `mixin/PackRepositoryMixin` – virtuelles Pack, das beim Reload ganz oben eingehängt wird.
* `client/` – Texture-Mixer-GUI, Settings-GUI, Laufzeit-Features, `McCompat`.
* `resources/assets/texturemixer/` – `categories.json`, `features.json` (alle Einstellungen + Beschreibungen DE/EN), Lang-Dateien.

Nicht unterstützt: OptiFine-CIT/CTM und Shader-Packs werden nicht kombiniert.
