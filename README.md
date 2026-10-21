# gator-lib-io

Legacy Gator file-import integration for Java 21 applications.

The library parses delimited imports and coordinates their validation and
database processing through `gator-lib`. Database configuration is supplied by
the consuming application and is intentionally not stored in this repository.

To ignore a file column, set its database column mapping (`columna_base_de_datos`)
to `{none}`. The legacy marker `none` is also accepted. Either marker may appear
multiple times; ignored columns may be empty or populated. Keep their headers
in the process configuration in the same order as the file. Ignored columns and
their values are excluded before database validation and staging; the remaining
columns retain their order, including date-format indexes.

Place `gator-lib-io`, `gator-lib`, and `gator-lib-utils` in sibling directories,
then build and verify with:

```bash
./gradlew clean check jar
```

Licensed under GPL-3.0-or-later; see [LICENSE](LICENSE) and [NOTICE](NOTICE).
