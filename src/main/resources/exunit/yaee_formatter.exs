defmodule Yaee.ExUnitFormatter do
  use GenServer

  def init(_options), do: {:ok, MapSet.new()}

  def handle_cast({:module_started, test_module}, started) do
    emit("testSuiteStarted", name: inspect(test_module.name), flowId: inspect(test_module.name))
    {:noreply, started}
  end

  def handle_cast({:module_finished, test_module}, started) do
    emit("testSuiteFinished", name: inspect(test_module.name), flowId: inspect(test_module.name))
    {:noreply, started}
  end

  def handle_cast({:test_started, test}, started) do
    start_test(test)
    {:noreply, MapSet.put(started, test_id(test))}
  end

  def handle_cast({:test_finished, test}, started) do
    id = test_id(test)
    unless MapSet.member?(started, id), do: start_test(test)

    common = [name: to_string(test.name), flowId: inspect(test.module)]
    case test.state do
      {:failed, failures} ->
        details = Enum.map_join(failures, "\n", fn {kind, reason, stack} -> Exception.format(kind, reason, stack) end)
        emit("testFailed", common ++ [message: "ExUnit test failed", details: details])
      {:skipped, reason} -> emit("testIgnored", common ++ [message: inspect(reason)])
      {:excluded, reason} -> emit("testIgnored", common ++ [message: inspect(reason)])
      {:invalid, reason} -> emit("testIgnored", common ++ [message: inspect(reason)])
      _ -> :ok
    end
    emit("testFinished", common ++ [duration: div(test.time || 0, 1_000)])
    {:noreply, MapSet.delete(started, id)}
  end

  def handle_cast(_event, started), do: {:noreply, started}

  defp test_id(test), do: {test.module, test.name, Map.get(test, :parameters)}

  defp start_test(test) do
    file = test.tags[:file]
    line = test.tags[:line]
    location = if file && line, do: "file://#{Path.expand(file)}:#{line}", else: ""
    emit("testStarted", name: to_string(test.name), flowId: inspect(test.module), locationHint: location)
  end

  defp emit(type, attributes) do
    fields = Enum.map_join(attributes, " ", fn {key, value} -> "#{key}='#{escape(value)}'" end)
    IO.puts("##teamcity[#{type} #{fields}]")
  end

  defp escape(value) do
    value
    |> to_string()
    |> String.replace("|", "||")
    |> String.replace("'", "|'")
    |> String.replace("\n", "|n")
    |> String.replace("\r", "|r")
    |> String.replace("[", "|[")
    |> String.replace("]", "|]")
  end
end
