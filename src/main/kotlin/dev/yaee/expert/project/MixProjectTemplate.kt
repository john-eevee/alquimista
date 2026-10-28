package dev.yaee.expert.project

object MixProjectTemplate {
    fun files(projectName: String): Map<String, String> {
        val appName = normalizeAppName(projectName)
        val moduleName = appName.split('_').joinToString("") { part -> part.replaceFirstChar(Char::uppercase) }
        return linkedMapOf(
            ".gitignore" to "/deps\n/_build\n/.expert\n/cover\n",
            ".formatter.exs" to "[\n  inputs: [\"{mix,.formatter}.exs\", \"{config,lib,test}/**/*.{ex,exs}\"]\n]\n",
            "mix.exs" to """defmodule $moduleName.MixProject do
  use Mix.Project

  def project do
    [
      app: :$appName,
      version: "0.1.0",
      elixir: "~> 1.15",
      start_permanent: Mix.env() == :prod,
      deps: deps()
    ]
  end

  def application do
    [extra_applications: [:logger]]
  end

  defp deps do
    []
  end
end
""",
            "lib/$appName.ex" to """defmodule $moduleName do
  @moduledoc "A small Elixir application."

  def hello, do: :world
end
""",
            "test/test_helper.exs" to "ExUnit.start()\n",
            "test/${appName}_test.exs" to """defmodule ${moduleName}Test do
  use ExUnit.Case, async: true
  doctest $moduleName

  test "greets the world" do
    assert $moduleName.hello() == :world
  end
end
""",
        )
    }

    fun normalizeAppName(projectName: String): String {
        var name = projectName.trim().lowercase()
            .replace(Regex("[^a-z0-9_]+"), "_")
            .trim('_')
        if (name.isEmpty()) name = "elixir_app"
        if (name.first().isDigit()) name = "app_$name"
        return name
    }
}
