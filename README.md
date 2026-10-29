# Alquimista

<img src="docs/alquimista-logo.png" alt="Alquimista, an alchemist brewing a glowing purple potion" width="280">

Elixir support for IntelliJ-based IDEs, powered by the [Expert language server](https://github.com/expert-lsp/expert). Alquimista combines Expert's language intelligence with IntelliJ editor, project, and test runner features.

## Features

### Editing

- Native Elixir highlighting for keywords, macros, modules, functions, and other syntax, with colors inherited from the active theme.
- Native HEEx highlighting, tag completion, indentation, folding, and a structure view for markup and components, plus native EEx highlighting for template tags and Elixir expressions. Configure Elixir, HEEx, and EEx delimiter colors under **Settings | Editor | Color Scheme**; EEx expressions use the Elixir colors.
- Elixir comments, matching brackets, indentation, folding, and Complete Current Statement for delimiters and `do` blocks.
- Elixir live templates and postfix templates for common expressions.
- Theme-aware file icons and **New | Elixir File** templates for `.ex`, `.exs`, `.eex`, and `.heex` files.

### Language intelligence

- Expert supplies completion, hover documentation, diagnostics, navigation, references, symbols, formatting, code actions, and code lenses through IntelliJ's LSP integration. Available behavior depends on the installed Expert release.
- Expert quick fixes, refactorings, source actions, and the `Reindex` command are available through IntelliJ.
- Optional Credo diagnostics appear in the editor and the Problems window for Elixir files, including unsaved edits.

### Projects and tests

- Run ExUnit tests, `describe` groups, or modules from the gutter. Results appear in the test runner with failure navigation and an action to rerun failed tests.
- Create a starter Mix project and register an Elixir SDK in the IDE.
- Use **Tools | Elixir** to run Mix tests, format the project, or fetch dependencies. Editor formatting uses Expert's LSP formatter.

## Requirements and setup

- An IntelliJ-based IDE with LSP support, version 2026.2 or newer.
- Elixir and Erlang/OTP, installed directly or managed with mise.
- Expert on `PATH`, a configured Expert executable, or an internet connection for automatic download.

Open a Mix project and select an Elixir project SDK to use its `mix` executable for Credo, Mix actions, and ExUnit runs. Without one, Alquimista uses mise when available, otherwise `mix` from `PATH`. When mise is available, Expert and Mix commands run through `mise exec` and use project versions from `mise.toml`.

Set a custom Expert executable under **Settings | Languages & Frameworks | Elixir Expert**. If no executable is configured and Expert cannot start, Alquimista downloads the latest stable release for the current OS and architecture and checks it against the release SHA-256 manifest.

### Expert settings

Under **Settings | Languages & Frameworks | Elixir Expert**, configure the Expert executable, minimum workspace symbol query length (including `0` for empty queries), editor and file log levels, Elixir source directory, Elixir and Erlang runtime executables, dependency fetching, and compile-on-type. Blank paths leave Expert's defaults in effect. Applying changes restarts Expert in open projects. Options depend on the installed Expert version; see [Expert's configuration reference](https://expert-lsp.org/docs/configuration/).

The Expert runtime overrides apply to the language server; the selected Elixir SDK controls Alquimista's Mix actions, Credo, and ExUnit runs.

### Credo

Credo is disabled by default. Add it to the Mix project's dependencies, run `mix deps.get`, and enable **Settings | Languages & Frameworks | Credo** for the project. Alquimista checks and caches whether Credo is available, and analyzes `.ex` and `.exs` buffers without saving them.

## Upgrading from the previous plugin

Alquimista has a new plugin ID. Uninstall the previous version before installing it to avoid loading both plugins. Expert and Credo settings, saved ExUnit run configurations, and custom Elixir and HEEx colors use new identifiers and need to be configured again.

## Development

Use the Gradle wrapper to build and test the plugin:

```shell
./gradlew check          # Compile and run tests
./gradlew buildPlugin    # Build the distributable ZIP in build/distributions
./gradlew runIde         # Launch a development IDE with the plugin installed
./gradlew verifyPlugin   # Check compatibility with configured IntelliJ versions
```

The Expert live-diagnostics integration test can run against a local Expert executable by setting `ALQUIMISTA_EXPERT_TEST_EXECUTABLE` to its path; it is skipped when the variable is unset. The Credo integration test uses the bundled fixture and runs when Mix is available. It requires Hex and downloads the fixture’s locked dependencies on its first run into `build/test-fixtures/credo`.

See [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) for notices for bundled third-party code.
