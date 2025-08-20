package org.figuramc.figura_cobalt.org.squiddev.cobalt.function;

import org.figuramc.figura_cobalt.LuaUncatchableError;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.LuaState;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.LuaTable;

import java.util.function.Supplier;

/**
 * Utility class for registering multiple functions for a library. This behaves similarly to Lua's {@code luaL_Reg}.
 */
public class RegisteredFunction {
	private final String name;
	private final Supplier<LibFunction> factory;

	private RegisteredFunction(String name, Supplier<LibFunction> factory) {
		this.name = name;
		this.factory = factory;
	}

	public LibFunction create() {
		LibFunction function = factory.get();
		function.name = name;
		return function;
	}

	public static LuaTable bind(LuaState state, RegisteredFunction[] functions) throws LuaUncatchableError {
		LuaTable table = new LuaTable(0, functions.length, state.allocationTracker);
		bind(table, functions);
		return table;
	}

	public static void bind(LuaTable table, RegisteredFunction[] functions) throws LuaUncatchableError {
		for (RegisteredFunction def : functions) {
			table.rawset(def.name, def.create());
		}
	}

	public static RegisteredFunction ofFactory(String name, Supplier<LibFunction> fn) {
		return new RegisteredFunction(name, fn);
	}

	public static RegisteredFunction of(String name, LibFunction.ZeroArg fn) {
		return new RegisteredFunction(name, () -> LibFunction.create(fn));
	}

	public static RegisteredFunction of(String name, LibFunction.OneArg fn) {
		return new RegisteredFunction(name, () -> LibFunction.create(fn));
	}

	public static RegisteredFunction of(String name, LibFunction.TwoArg fn) {
		return new RegisteredFunction(name, () -> LibFunction.create(fn));
	}

	public static RegisteredFunction of(String name, LibFunction.ThreeArg fn) {
		return new RegisteredFunction(name, () -> LibFunction.create(fn));
	}

	public static RegisteredFunction ofV(String name, LibFunction.ManyArgs fn) {
		return new RegisteredFunction(name, () -> LibFunction.createV(fn));
	}

	public static RegisteredFunction ofS(String name, LibFunction.Suspended fn) {
		return new RegisteredFunction(name, () -> LibFunction.createS(fn));
	}
}
