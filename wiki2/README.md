# OSRS Wiki Recipe Graph

Minimal Kotlin CLI that fetches OSRS Wiki wikitext through the MediaWiki API and prints a flat recipe graph.

## Run

Install Gradle or open this directory as a Gradle project in IntelliJ, then run:

```bash
gradle run --args="Cake"
gradle run --args="Cake, Iron dagger, Rune platelegs"
gradle run --args="Iron dagger"
gradle run --args="Dragonfire Shield"
gradle run --args="Black body"
./gradlew run -Pitem="Black d'hide body"
./gradlew run -Pitems="Cake, Iron dagger, Rune platelegs" -Poutput=recipes.json
```

Use `-Pitem=...` for names containing apostrophes because Gradle's `--args` parser treats single quotes specially.
Use `-Pitems=...` or `--items` with comma-separated names to crawl multiple requested items in one run. Shared sub-components are deduplicated by item ID, so an ingredient tree that appears under more than one requested item is written once.
Use `-Poutput=...`, `-Pout=...`, `--output ...`, or `-o ...` to save the combined graph to one JSON file instead of printing it to stdout.
Wiki requests are rate-limited to one request every 250 ms by default. Set `OSRS_WIKI_REQUEST_DELAY_MS` to override the delay, or `0` to disable it.

The output shape is:

```json
{
  "version": 1,
  "recipes": [
    {
      "id": 1891,
      "name": "Cake",
      "methods": [
        {
          "method": "Cooking range",
          "skills": [{ "skill": "Cooking", "level": 0, "xp": 180.0 }],
          "requires": [
            { "id": 1889, "name": "Uncooked cake", "quantity": 1 }
          ]
        }
      ]
    },
    {
      "id": 1889,
      "name": "Uncooked cake",
      "methods": [
        {
          "method": "Cooking",
          "skills": [{ "skill": "Cooking", "level": 0, "xp": 0.0 }],
          "requires": [
            { "id": 1887, "name": "Cake tin", "quantity": 1 },
            { "id": 1944, "name": "Egg", "quantity": 1 },
            { "id": 1933, "name": "Pot of flour", "quantity": 1 },
            { "id": 1927, "name": "Bucket of milk", "quantity": 1 }
          ]
        }
      ]
    }
  ]
}
```

Each entry in `recipes` is an output item. Each method only lists direct requirements, including the resolved OSRS Wiki item name for each ingredient. The `skills` entries pair each wiki recipe skill with its own level requirement and XP reward.

Items with no craftable recipe are omitted from `recipes`.
