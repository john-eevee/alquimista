NodeLine = string:trim(io:get_line("")),
CookieLine = string:trim(io:get_line("")),
case CookieLine of "" -> ok; _ -> erlang:set_cookie(node(), list_to_atom(CookieLine)) end,
Target = list_to_atom(NodeLine),
case net_adm:ping(Target) of
  pang -> io:format("ERROR\tunreachable~n");
  pong ->
    Call = fun(F, A) -> rpc:call(Target, erlang, F, A, 5000) end,
    Release = Call(system_info, [otp_release]),
    Memory = Call(memory, [total]),
    Pids = Call(processes, []),
    case {Release, Memory, Pids} of
      {R, M, Ps} when is_list(R), is_integer(M), is_list(Ps) ->
        io:format("OK\t~s\t~s\t~B\t~B~n", [NodeLine, R, M, length(Ps)]),
        lists:foreach(fun(Pid) ->
          case Call(process_info, [Pid, [registered_name, memory, message_queue_len, reductions]]) of
            Info when is_list(Info) ->
              Name = proplists:get_value(registered_name, Info, ''),
              NameText = case Name of N when is_atom(N) -> atom_to_list(N); _ -> "" end,
              Encoded = base64:encode(unicode:characters_to_binary(NameText)),
              io:format("P\t~s\t~s\t~B\t~B\t~B~n", [pid_to_list(Pid), Encoded, proplists:get_value(memory, Info, 0), proplists:get_value(message_queue_len, Info, 0), proplists:get_value(reductions, Info, 0)]);
            _ -> ok
          end
        end, Ps);
      _ -> io:format("ERROR\tUnable to read node information~n")
    end
end,
halt().
