NodeLine = string:trim(io:get_line("")),
CookieLine = string:trim(io:get_line("")),
case CookieLine of "" -> ok; _ -> erlang:set_cookie(node(), list_to_atom(CookieLine)) end,
Target = list_to_atom(NodeLine),
Loop = fun Refresh(Self) ->
  case io:get_line("") of
    eof -> ok;
    "refresh\n" ->
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
        Mem = Call(memory, []),
        Io = Call(statistics, [io]),
        case {Mem, Io} of
          {Ms, {{input, In}, {output, Out}}} when is_list(Ms), is_integer(In), is_integer(Out) ->
            io:format("M\t~B\t~B\t~B\t~B\t~B\t~B\t~B~n", [proplists:get_value(processes, Ms, 0), proplists:get_value(atom, Ms, 0), proplists:get_value(binary, Ms, 0), proplists:get_value(code, Ms, 0), proplists:get_value(ets, Ms, 0), In, Out]);
          _ -> ok
        end,
        Encode = fun(Term) -> base64:encode(unicode:characters_to_binary(io_lib:format("~tp", [Term]))) end,
        Text = fun(Term) -> unicode:characters_to_binary(io_lib:format("~tp", [Term])) end,
        B64 = fun(Value) -> base64:encode(Value) end,
        TreeLine = fun(Id, Parent, Label, Pid, Kind) -> io:format("S\t~s\t~s\t~s\t~s\t~s~n", [B64(Id), B64(Parent), B64(Label), B64(Pid), B64(Kind)]) end,
        Walk = fun Visit(Self, Pid, Id, Depth) ->
          case Depth < 32 andalso is_pid(Pid) of
            true -> case rpc:call(Target, supervisor, which_children, [Pid], 2000) of
              Children when is_list(Children) ->
                lists:foreach(fun({{ChildId, ChildPid, Type, _}, Index}) ->
                  ChildPath = <<Id/binary, "/", (integer_to_binary(Index))/binary>>,
                  PidText = case is_pid(ChildPid) of true -> unicode:characters_to_binary(pid_to_list(ChildPid)); false -> Text(ChildPid) end,
                  TreeLine(ChildPath, Id, Text(ChildId), PidText, Text(Type)),
                  case Type of supervisor -> Self(Self, ChildPid, ChildPath, Depth + 1); _ -> ok end
                end, lists:zip(Children, lists:seq(1, length(Children))));
              _ -> ok
            end;
            false -> ok
          end
        end,
        Apps = rpc:call(Target, application, which_applications, [], 5000),
        case Apps of
          AppList when is_list(AppList) -> lists:foreach(fun({{App, _, _}, Index}) ->
            AppId = integer_to_binary(Index),
            TreeLine(AppId, <<>>, Text(App), <<>>, <<"application">>),
            case rpc:call(Target, application_controller, get_master, [App], 2000) of
              Master when is_pid(Master) -> case rpc:call(Target, application_master, get_child, [Master], 2000) of
                {Root, _} when is_pid(Root) ->
                  RootId = <<AppId/binary, "/0">>,
                  TreeLine(RootId, AppId, Text(App), unicode:characters_to_binary(pid_to_list(Root)), <<"supervisor">>),
                  Walk(Walk, Root, RootId, 0);
                _ -> ok
              end;
              _ -> ok
            end
          end, lists:zip(AppList, lists:seq(1, length(AppList))));
          _ -> ok
        end,
        Tables = rpc:call(Target, ets, all, [], 5000),
        case Tables of
          Ts when is_list(Ts) -> lists:foreach(fun(Tid) ->
            case rpc:call(Target, ets, info, [Tid], 5000) of
              Info when is_list(Info) ->
                io:format("T\t~s\t~s\t~s\t~B\t~B\t~s\t~s~n", [Encode(proplists:get_value(name, Info)), Encode(Tid), Encode(proplists:get_value(owner, Info)), proplists:get_value(size, Info, 0), proplists:get_value(memory, Info, 0), Encode(proplists:get_value(protection, Info)), Encode(proplists:get_value(type, Info))]);
              _ -> ok
            end
          end, Ts);
          _ -> ok
        end,
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
io:format("END~n"),
Self(Self);
    _ -> Self(Self)
  end
end,
Loop(Loop),
halt().
