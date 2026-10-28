import json
import os
import pathlib
import queue
import subprocess
import sys
import threading
import time

binary, project_dir, ids_json = sys.argv[1:]
root = pathlib.Path(project_dir).resolve()
ids = json.loads(ids_json)
environment = os.environ.copy()
environment["ERL_FLAGS"] = "+S 2:2"
process = subprocess.Popen(
    [binary, "--stdio"], cwd=root, env=environment,
    stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
)
messages = queue.Queue()


def receive():
    try:
        while True:
            headers = {}
            while True:
                line = process.stdout.readline()
                if not line:
                    return
                if line in (b"\r\n", b"\n"):
                    break
                key, value = line.decode().split(":", 1)
                headers[key.lower()] = value.strip()
            messages.put(json.loads(process.stdout.read(int(headers["content-length"]))))
    except Exception as error:
        messages.put({"reader_error": str(error)})


def send(message):
    payload = json.dumps(message).encode()
    process.stdin.write(f"Content-Length: {len(payload)}\r\n\r\n".encode() + payload)
    process.stdin.flush()


def next_message(deadline):
    while time.monotonic() < deadline:
        try:
            message = messages.get(timeout=1)
        except queue.Empty:
            if process.poll() is not None:
                raise AssertionError(f"Expert exited with code {process.returncode}")
            continue
        if "reader_error" in message:
            raise AssertionError(message["reader_error"])
        if "id" in message and "method" in message:
            result = [] if message["method"] == "workspace/configuration" else None
            send({"jsonrpc": "2.0", "id": message["id"], "result": result})
        return message
    raise AssertionError("Timed out waiting for Expert")


threading.Thread(target=receive, daemon=True).start()
threading.Thread(target=lambda: process.stderr.read(), daemon=True).start()
try:
    deadline = time.monotonic() + 120
    send({"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {
        "processId": None, "rootUri": root.as_uri(),
        "workspaceFolders": [{"uri": root.as_uri(), "name": "fixture"}],
        "capabilities": {"workspace": {"configuration": True}},
    }})
    while next_message(deadline).get("id") != 1:
        pass
    send({"jsonrpc": "2.0", "method": "initialized", "params": {}})
    while True:
        message = next_message(deadline)
        if message.get("method") == "window/logMessage" and "Server initialized" in message["params"].get("message", ""):
            break

    files = {}
    for extension, language_id in ids.items():
        path = root / "lib" / f"sample.{extension}"
        original = "defmodule Sample do\n  def value, do: 1\nend\n" if extension in ("ex", "exs") else "<p>hello</p>\n"
        path.write_text(original)
        files[extension] = (path, original)
        send({"jsonrpc": "2.0", "method": "textDocument/didOpen", "params": {"textDocument": {
            "uri": path.as_uri(), "languageId": language_id, "version": 1, "text": original,
        }}})

    opened = set()
    compiled = False
    while len(opened) < len(files) or not compiled:
        message = next_message(deadline)
        if message.get("method") == "window/logMessage":
            log = message["params"].get("message", "")
            for extension, (path, _) in files.items():
                if f"Opened {path.as_uri()}" in log:
                    opened.add(extension)
            if log.startswith("Compiled "):
                compiled = True

    for extension, (path, _) in files.items():
        invalid = "defmodule Sample do\n  def value(\nend\n" if extension in ("ex", "exs") else "<%= if true do %>\n"
        send({"jsonrpc": "2.0", "method": "textDocument/didChange", "params": {
            "textDocument": {"uri": path.as_uri(), "version": 2}, "contentChanges": [{"text": invalid}],
        }})

    send({"jsonrpc": "2.0", "id": 2, "method": "textDocument/documentSymbol", "params": {
        "textDocument": {"uri": files["heex"][0].as_uri()},
    }})

    found = set()
    required = {"ex", "exs", "eex"}
    heex_response = False
    while not required.issubset(found) or not heex_response:
        message = next_message(deadline)
        if message.get("id") == 2 and "method" not in message:
            assert "error" not in message, f"HEEx request failed: {message['error']}"
            heex_response = True
        if message.get("method") == "textDocument/publishDiagnostics":
            params = message["params"]
            for extension, (path, _) in files.items():
                if params.get("uri") == path.as_uri() and params.get("diagnostics"):
                    found.add(extension)
    for path, original in files.values():
        assert path.read_text() == original, f"Fixture was unexpectedly saved: {path}"
    print("Expert accepted all four documents and published unsaved diagnostics for " + ", ".join(sorted(found)))
finally:
    process.terminate()
    try:
        process.wait(timeout=5)
    except subprocess.TimeoutExpired:
        process.kill()
        process.wait(timeout=5)
