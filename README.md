# gator-lib-io

Legacy Gator file-import integration for Java 21 applications.

The library parses delimited imports and coordinates their validation and
database processing through `gator-lib`. Database configuration is supplied by
the consuming application and is intentionally not stored in this repository.

Place `gator-lib-io`, `gator-lib`, and `gator-lib-utils` in sibling directories,
then build and verify with:

```bash
./gradlew clean check jar
```

Licensed under GPL-3.0-or-later; see [LICENSE](LICENSE) and [NOTICE](NOTICE).
