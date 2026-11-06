# PhotoSlides SHOW (Prototyp)

Android aplikácia pre **telefón a tablet** určená čisto na **prezentáciu existujúcich fotiek** z lokálneho úložiska alebo z cloudových služieb, s hudobným podkladom a tmavým dizajnom s kontrastne ohraničenými tlačidlami.

---

## Hlavné funkcie prototypu

### 1. Zdroje fotiek (Lokálne úložisko & Cloud)
- **Albumy v zariadení**: automatické načítanie albumov cez `MediaStore`.
- **Priečinky (vrátane podpriečinkov)**: výber cez systémový *Storage Access Framework* (`OpenDocumentTree`), ktorý sprístupňuje interné úložisko, SD kartu aj pripojené cloudové úložiská (**Google Drive, OneDrive, Dropbox, Nextcloud**).
- **Výber fotiek (Photo Picker / Dokumenty)**: priamy výber fotiek vrátane cloudových poskytovateľov integrovaných v Androide (**Google Fotky**).
- **Vstavaná ukážková kolekcia (Demo)**: 8 vygenerovaných fotiek (na šírku aj na výšku) pripravených na okamžité vyskúšanie bez nutnosti udeľovať povolenia.

### 2. Široké možnosti nastavenia prezentácie
- **Dĺžka zobrazenia fotky**: `2 s` až `30 s`.
- **Rýchlosť prechodu**: `200 ms` až `2500 ms`.
- **7 štýlov prechodov + Náhodný mix**:
  - *Bez prechodu*, *Prelínanie (Fade)*, *Posun vodorovne*, *Posun zvislo*, *Priblíženie (Zoom-In)*, *Oddialenie (Zoom-Out)*, *Jemné zasunutie (Push-Fade)*, *Náhodný mix*.
- **Ken Burns efekt**: plynulý jemný zoom a posun počas zobrazenia snímky.
- **Poradie prehrávania**:
  - *Od najstarších*, *Od najnovších*, *Podľa názvu*, *Náhodne* (s automatickým premiešaním po každom kole), *Podľa zdrojov*.
- **Filtrovanie podľa kritérií**:
  - **Orientácia**: *Všetky*, *Len na šírku*, *Len na výšku*.
  - **Časové obdobie**: *Celé obdobie*, *Posledný týždeň*, *Posledný mesiac*, *Posledný rok*, *V tento deň v minulosti* (spomienky z rovnakého dňa v predchádzajúcich rokoch).
- **Režim stolového fotorámika**:
  - Nekonečná slučka, digitálne hodiny v rohu, indikátor priebehu snímky, časovač automatického vypnutia (*15 min, 30 min, 1 hod, 2 hod*), zákaz zhasnutia obrazovky (`FLAG_KEEP_SCREEN_ON`).

### 3. Automatické rozpoznanie orientácie fotiek (Na výšku / Na šírku)
- Rozpoznanie skutočnej orientácie kombináciou `ExifInterface` (`TAG_ORIENTATION`) a rozmerov obrázka v [`PhotoLoader.kt`](app/src/main/java/com/tiborhreus/photoslidesshow/data/PhotoLoader.kt).
- **Spájanie dvoch fotiek na výšku vedľa seba** (`pairPortraits`): ak je obrazovka otočená na šírku (tablet alebo otočený telefón), aplikácia automaticky spojí dve portrétové fotky do jednej dvoj-snímky vedľa seba.
- **Inteligentné prispôsobenie mierky**: ak je pomer strán fotky blízky obrazovke, vyplní obrazovku; inak zobrazí celú fotku s **rozmazaným pozadím z danej fotky** alebo s čistým čiernym pozadím.

### 4. Hudba k prezentácii
- **Lokálne súbory**: výber ľubovoľného počtu MP3 / FLAC / WAV / OGG súborov so zapnutím náhodného poradia (prehrávané cez `Media3 ExoPlayer`).
- **Voľne dostupné internetové rádiá bez reklám**: predvolené ambientné a chill stanice (*SomaFM Groove Salad, Drone Zone, Lush, Radio Paradise*) + možnosť zadať vlastnú URL streamu.
- **Voľne dostupné hudobné knižnice (Royalty-Free)**: rýchle odkazy na *Pixabay Music, Free Music Archive, Incompetech, ccMixter*.
- **Streamovacie služby**: prepojenie so **Spotify, YouTube Music, Apple Music, Deezer, TIDAL, SoundCloud, Amazon Music** – v tomto režime aplikácia nepreberá audio fokus, takže hudba zo streamovacej služby hrá nerušene na pozadí.
- **Plynulý nábeh hlasitosti (Fade-In)** a regulácia hlasitosti.

### 5. Dizajn a adaptívne rozloženie (Telefón & Tablet)
- Čisto **tmavý režim** (`#0B0C10` / `#15171D`) s vysokokontrastným ohraničením všetkých tlačidiel (`#E4E6EB` pre bežné tlačidlá a jantárová `#FFB627` pre hlavné akcie a aktívne voľby).
- Automatické prepnutie medzi **jednostĺpcovým rozložením na telefóne** a **dvojstĺpcovým rozložením na tablete** (od šírky `720 dp`).

---

## Štruktúra projektu

- [`app/src/main/java/com/tiborhreus/photoslidesshow/MainActivity.kt`](app/src/main/java/com/tiborhreus/photoslidesshow/MainActivity.kt) – vstupný bod aplikácie a navigácia.
- [`app/src/main/java/com/tiborhreus/photoslidesshow/AppViewModel.kt`](app/src/main/java/com/tiborhreus/photoslidesshow/AppViewModel.kt) – správa stavu nastavení, zdrojov, hudby a zoznamu fotiek.
- [`app/src/main/java/com/tiborhreus/photoslidesshow/data/`](app/src/main/java/com/tiborhreus/photoslidesshow/data/) – dátové modely, načítanie fotiek s EXIF orientáciou (`PhotoLoader.kt`), skladanie snímok (`PlaylistBuilder.kt`), ukladanie nastavení (`PrefsStore.kt`) a generátor ukážkových fotiek (`DemoPhotoGenerator.kt`).
- [`app/src/main/java/com/tiborhreus/photoslidesshow/ui/theme/Theme.kt`](app/src/main/java/com/tiborhreus/photoslidesshow/ui/theme/Theme.kt) – tmavá farebná schéma s kontrastnými okrajmi.
- [`app/src/main/java/com/tiborhreus/photoslidesshow/ui/components/Components.kt`](app/src/main/java/com/tiborhreus/photoslidesshow/ui/components/Components.kt) – ohraničené tlačidlá (`BorderedButton`, `BorderedIconButton`, `ChoiceChip`) a adaptívne stĺpce (`AdaptiveColumns`).
- [`app/src/main/java/com/tiborhreus/photoslidesshow/ui/screens/`](app/src/main/java/com/tiborhreus/photoslidesshow/ui/screens/) – obrazovky `HomeScreen.kt`, `SourcesScreen.kt`, `MusicScreen.kt`, `SlideshowScreen.kt`.

## Spustenie v Android Studio
1. Otvorte priečinok projektu v **Android Studio Ladybug (alebo novšom)** s JDK 17.
2. Synchronizujte Gradle a spustite konfiguráciu `app` na telefóne, tablete alebo emulátore (Android 8.0 / API 26+).
