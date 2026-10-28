defmodule Alquimista.ExUnitFormatter do
  use GenServer

  def init(_options), do: {:ok, %{tests: MapSet.new(), suites: MapSet.new()}}

  def handle_cast({:module_started, _test_module}, state), do: {:noreply, state}

  def handle_cast({:module_finished, test_module}, state) do
    if MapSet.member?(state.suites, test_module.name) do
      emit("testSuiteFinished", name: inspect(test_module.name), flowId: inspect(test_module.name))
    end
    {:noreply, %{state | suites: MapSet.delete(state.suites, test_module.name)}}
  end

  def handle_cast({:test_started, %{state: {:excluded, _}}}, started), do: {:noreply, started}

  def handle_cast({:test_started, test}, started) do
    started = ensure_suite(test, started)
    start_test(test)
    {:noreply, %{started | tests: MapSet.put(started.tests, test_id(test))}}
  end

  def handle_cast({:test_finished, %{state: {:excluded, _}}}, started), do: {:noreply, started}

  def handle_cast({:test_finished, test}, started) do
    started = ensure_suite(test, started)
    id = test_id(test)
    unless MapSet.member?(started.tests, id), do: start_test(test)

    common = [name: to_string(test.name), flowId: inspect(test.module)]
    case test.state do
      {:failed, failures} ->
        emit("testFailed", common ++ [message: "ExUnit test failed", details: format_failures(failures)])
      {:skipped, reason} -> emit("testIgnored", common ++ [message: inspect(reason)])
      {:invalid, %{state: {:failed, failures}}} ->
        emit("testFailed", common ++ [message: "ExUnit setup failed", details: format_failures(failures)])
      {:invalid, reason} -> emit("testFailed", common ++ [message: "ExUnit setup failed", details: inspect(reason)])
      _ -> :ok
    end
    emit("testFinished", common ++ [duration: div(test.time || 0, 1_000)])
    {:noreply, %{started | tests: MapSet.delete(started.tests, id)}}
  end

  def handle_cast(_event, started), do: {:noreply, started}

  defp test_id(test), do: {test.module, test.name, Map.get(test, :parameters)}

  defp ensure_suite(test, state) do
    if MapSet.member?(state.suites, test.module) do
      state
    else
      emit("testSuiteStarted", name: inspect(test.module), flowId: inspect(test.module))
      %{state | suites: MapSet.put(state.suites, test.module)}
    end
  end

  defp format_failures(failures), do: Enum.map_join(failures, "\n", fn {kind, reason, stack} -> Exception.format(kind, reason, stack) end)

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
