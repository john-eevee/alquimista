# Yaee: Elixir Expert

Elixir language support for IntelliJ-based IDEs powered by the [Expert language server](https://github.com/expert-lsp/expert).

## Requirements

- IntelliJ-based IDE 2026.2 or newer
- Elixir and Erlang/OTP, installed directly or managed by mise
- Expert is downloaded automatically from the latest stable GitHub release when the server first fails to start and no custom executable is configured

Expert downloads are selected for the current OS and architecture and checked against the release SHA-256 manifest. The executable path can be set under **Settings | Languages & Frameworks | Elixir Expert**. When mise is available, the plugin launches Expert and Mix commands through `mise exec`, so project versions from `mise.toml` are used.

## Features

- Expert LSP completion, hover documentation, diagnostics, go to definition, find references, document formatting, symbols, code actions, and code lenses
- Expert quick fixes, refactorings, source actions, and the `Reindex` command
- ExUnit test discovery and gutter actions that run `mix test path/to/file_test.exs:line`
- Tools | Elixir actions for running all Mix tests, formatting the project, and fetching dependencies
- A Mix project wizard that creates a starter application and ExUnit test
- An Elixir SDK type for registering an Elixir executable or installation directory
- Elixir, EEx, and HEEx TextMate grammars
- Live templates for functions, `case`, `with`, pipelines, ExUnit tests, and module docs
- Postfix templates for `.inspect`, `.dbg`, `.ok`, `.error`, `.pipe`, and `.if`

LSP features are presented by the IntelliJ Platform. The available behavior follows the capabilities advertised by the installed Expert release.

## Build and verify

```shell
./gradlew test
./gradlew verifyPlugin
```

Run the plugin in a development IDE with:

```shell
./gradlew runIde
```

TextMate grammars are derived from [thiagopromano/explore](https://github.com/thiagopromano/explore) and remain under Apache-2.0; see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
