NodeLine = string:trim(io:get_line("")),
CookieLine = string:trim(io:get_line("")),
case CookieLine of "" -> ok; _ -> erlang:set_cookie(node(), list_to_atom(CookieLine)) end,
Target = list_to_atom(NodeLine),
TraceAction = fun(Kind, Encoded) ->
  try
    case Kind of
      start ->
        Pid = binary_to_term(base64:decode(string:trim(Encoded)), [safe]),
        true = is_pid(Pid) andalso node(Pid) =:= Target,
        {TraceSession, TraceTable} = case get(observer_trace) of
          undefined ->
            {module, dbg} = code:ensure_loaded(dbg),
            true = erlang:function_exported(dbg, session_create, 1),
            Table = ets:new(observer_trace, [ordered_set, public]),
            Session = dbg:session_create(alquimista_observer),
            Handler = fun(Event, Acc) ->
              Index = erlang:unique_integer([monotonic, positive]),
              ets:insert(Acc, {Index, Event}),
              case ets:info(Acc, size) > 500 of true -> ets:delete(Acc, ets:first(Acc)); false -> ok end,
              Acc
            end,
            try
              {ok, _} = dbg:session(Session, fun() -> dbg:tracer(process, {Handler, Table}) end),
              {ok, Target} = dbg:session(Session, fun() -> dbg:n(Target) end),
              put(observer_trace, {Session, Table}),
              {Session, Table}
            catch InitClass:InitError -> dbg:session_destroy(Session), ets:delete(Table), erlang:raise(InitClass, InitError, []) end;
          Existing -> Existing
        end,
        _ = TraceTable,
        case dbg:session(TraceSession, fun() -> dbg:p(Pid, [send, 'receive', procs]) end) of
          {ok, _} -> io:format("RESULT\tb2s=~n");
          _ -> io:format("ERROR\tCould not start tracing~n")
        end;
      events ->
        case get(observer_trace) of
          undefined -> io:format("RESULT\t~n");
          {_, Table} ->
            Events = ets:tab2list(Table),
            lists:foreach(fun({Key, _}) -> ets:delete(Table, Key) end, Events),
            Text = unicode:characters_to_binary(lists:join($\n, [io_lib:format("~P", [Event, 12]) || {_, Event} <- Events])),
            io:format("RESULT\t~s~n", [base64:encode(Text)])
        end;
      stop ->
        case erase(observer_trace) of
          undefined -> ok;
          {Session, Table} -> dbg:session_destroy(Session), ets:delete(Table)
        end,
        io:format("RESULT\tb2s=~n")
    end
  catch _:_ -> io:format("ERROR\tTrace unavailable; isolated tracing requires compatible Erlang runtime tools~n") end,
  io:format("END~n")
end,
Action = fun(Kind, Encoded) ->
  try
    Pid = binary_to_term(base64:decode(string:trim(Encoded)), [safe]),
    true = is_pid(Pid) andalso node(Pid) =:= Target,
    case Kind of
      inspect ->
        Info = rpc:call(Target, erlang, process_info, [Pid, [registered_name, current_function, initial_call, status, message_queue_len, messages, dictionary, current_stacktrace, links, monitors, monitored_by, memory, reductions]], 5000),
        case Info of
          undefined -> io:format("ERROR\tProcess no longer exists~n");
          {badrpc, _} -> io:format("ERROR\tCould not inspect process~n");
          _ -> io:format("RESULT\t~s~n", [base64:encode(unicode:characters_to_binary(io_lib:format("~P", [Info, 25])))])
        end;
      state ->
        case rpc:call(Target, sys, get_state, [Pid, 2000], 3000) of
          {badrpc, _} -> io:format("ERROR\tProcess does not expose OTP state~n");
          State -> io:format("RESULT\t~s~n", [base64:encode(unicode:characters_to_binary(io_lib:format("~P", [State, 25])))])
        end;
      kill ->
        case rpc:call(Target, erlang, exit, [Pid, kill], 5000) of
          true -> io:format("RESULT\tb2s=~n");
          _ -> io:format("ERROR\tCould not terminate process~n")
        end
    end
  catch _:_ -> io:format("ERROR\tInvalid process identifier~n") end,
  io:format("END~n")
end,
TableAction = fun(Encoded) ->
  try
    Tid = binary_to_term(base64:decode(string:trim(Encoded)), [safe]),
    Available = rpc:call(Target, ets, all, [], 5000),
    true = is_list(Available) andalso lists:member(Tid, Available),
    Result = rpc:call(Target, ets, select, [Tid, [{'_', [], ['$_']}], 200], 5000),
    case Result of
      {Objects, _} when is_list(Objects) ->
        Lines = [io_lib:format("~P", [Object, 25]) || Object <- Objects],
        Value = unicode:characters_to_binary(lists:join($\n, Lines)),
        io:format("RESULT\t~s~n", [base64:encode(Value)]);
      '$end_of_table' -> io:format("RESULT\tW10=~n");
      _ -> io:format("ERROR\tTable is unavailable or not readable~n")
    end
  catch _:_ -> io:format("ERROR\tInvalid table identifier~n") end,
  io:format("END~n")
end,
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
        Sampler = Call(spawn, [observer_backend, etop_collect, [self()]]),
        case is_pid(Sampler) of
          true -> receive
            {Sampler, StatsInfo} when is_tuple(StatsInfo), tuple_size(StatsInfo) >= 5, element(1, StatsInfo) =:= etop_info ->
              case element(5, StatsInfo) of
                SchedulerList when is_list(SchedulerList) -> lists:foreach(fun({Id, Active, Total}) -> io:format("Q\t~B\t~B\t~B~n", [Id, Active, Total]) end, SchedulerList);
                _ -> ok
              end
          after 3000 -> ok end;
          false -> ok
        end,
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
                io:format("T\t~s\t~s\t~s\t~B\t~B\t~s\t~s\t~s~n", [Encode(proplists:get_value(name, Info)), Encode(Tid), Encode(proplists:get_value(owner, Info)), proplists:get_value(size, Info, 0), proplists:get_value(memory, Info, 0), Encode(proplists:get_value(protection, Info)), Encode(proplists:get_value(type, Info)), base64:encode(term_to_binary(Tid))]);
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
              io:format("P\t~s\t~s\t~B\t~B\t~B\t~s~n", [pid_to_list(Pid), Encoded, proplists:get_value(memory, Info, 0), proplists:get_value(message_queue_len, Info, 0), proplists:get_value(reductions, Info, 0), base64:encode(term_to_binary(Pid))]);
            _ -> ok
          end
        end, Ps);
      _ -> io:format("ERROR\tUnable to read node information~n")
    end
end,
io:format("END~n"),
Self(Self);
    "trace_start\t" ++ Token -> TraceAction(start, Token), Self(Self);
    "trace_events\n" -> TraceAction(events, ""), Self(Self);
    "trace_stop\n" -> TraceAction(stop, ""), Self(Self);
    "table\t" ++ Token -> TableAction(Token), Self(Self);
    "state\t" ++ Token -> Action(state, Token), Self(Self);
    "inspect\t" ++ Token -> Action(inspect, Token), Self(Self);
    "kill\t" ++ Token -> Action(kill, Token), Self(Self);
    _ -> Self(Self)
  end
end,
Loop(Loop),
halt().
