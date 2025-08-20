package org.figuramc.figura_cobalt.org.squiddev.cobalt.debug;

import org.figuramc.figura_cobalt.LuaUncatchableError;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.function.Dispatch;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.function.LuaFunction;

import java.util.Objects;

public record FunctionDebugHook(LuaFunction function) implements DebugHook {
	public FunctionDebugHook {
		Objects.requireNonNull(function, "function cannot be null");
	}

	@Override
	public boolean inheritHook() {
		return false;
	}

	@Override
	public void onCall(LuaState state, DebugState ds, DebugFrame frame) throws LuaError, LuaUncatchableError, UnwindThrowable {
		Dispatch.call(state, function, CALL, Constants.NIL);
	}

	@Override
	public void onReturn(LuaState state, DebugState ds, DebugFrame frame) throws LuaError, LuaUncatchableError, UnwindThrowable {
		Dispatch.call(state, function, RETURN, Constants.NIL);
	}

	@Override
	public void onCount(LuaState state, DebugState ds, DebugFrame frame) throws LuaError, LuaUncatchableError, UnwindThrowable {
		Dispatch.call(state, function, COUNT, Constants.NIL);
	}

	@Override
	public void onLine(LuaState state, DebugState ds, DebugFrame frame, int newLine) throws LuaError, LuaUncatchableError, UnwindThrowable {
		Dispatch.call(state, function, LINE, LuaInteger.valueOf(newLine));
	}
}
