# Alquimista Changelog

## [Unreleased]

### Added

- Erlang Observer tool window with scheduler, memory, and I/O charts; process information and actions; ETS browsing; and application supervision trees for distributed nodes.

- HEEx tag completion, matching-tag highlighting and navigation, indentation, folding, and a structure view.

- Expert settings for workspace symbols, logging, runtime and source paths, dependency fetching, and compile-on-type.

- Expert LSP integration for Elixir, EEx, and HEEx, including automatic, checksum-verified Expert downloads and mise support.
- Native Elixir, HEEx, and EEx syntax highlighting with theme-aware color settings.
- Elixir bracket matching, comments, indentation, folding, and Complete Current Statement for delimiters and `do` blocks.
- ExUnit gutter actions for tests, `describe` groups, and modules, with test runner results, failure navigation, and rerunning failed tests.
- Optional Credo diagnostics for Elixir files, including unsaved edits, in the editor and Problems window.
- A Mix project wizard, Elixir SDK support, Mix actions, and file templates for Elixir and template files.
- Theme-aware Elixir file icons, live templates, and postfix templates.

### Fixed

- Send Expert configuration through the LSP configuration notification so the server applies it.

### Changed

- Keep the Observer hidden node alive between refreshes and close it on disconnect.

- Renamed the project to Alquimista. The plugin ID and stored settings identifiers changed; see the README for upgrade guidance.
- Use Expert for editor formatting; the separate Mix action formats the project.
- Highlight Elixir declaration macros, modules, and function names using the active color scheme.
