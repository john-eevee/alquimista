defmodule AlquimistaCredoFixture.MixProject do
  use Mix.Project

  def project do
    [app: :alquimista_credo_fixture, version: "0.1.0", elixir: ">= 1.15.0", deps: deps()]
  end

  defp deps do
    [{:credo, "1.7.19", only: :test, runtime: false}]
  end
end
