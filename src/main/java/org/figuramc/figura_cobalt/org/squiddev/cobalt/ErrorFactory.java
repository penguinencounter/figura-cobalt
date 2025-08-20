/*
 * The MIT License (MIT)
 *
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2020 SquidDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.figuramc.figura_cobalt.org.squiddev.cobalt;

import org.figuramc.figura_cobalt.LuaUncatchableError;
import org.figuramc.memory_tracker.AllocationTracker;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.debug.DebugFrame;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.debug.DebugHelpers;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.debug.DebugState;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.debug.ObjectName;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.function.LuaClosure;
import org.checkerframework.checker.nullness.qual.Nullable;

import static org.figuramc.figura_cobalt.org.squiddev.cobalt.Constants.NAME;

/**
 * Factory class for errors
 */
public final class ErrorFactory {
	private ErrorFactory() {
	}

	/**
	 * Get the name of a type suitable for error reporting. Unlike {@link LuaValue#luaTypeName()}, this will read the
	 * {@link Constants#NAME __name} metatag.
	 * <p>
	 * There'message a slight inconsistency in PUC Lua here:
	 * <ul>
	 *     <li>{@code luaT_objtypename} from ltm.c only uses {@code __name} for tables and userdata</li>
	 *     <li>{@code luaL_typeerror} from lauxlib.c uses {@code __name} for all types.</li>
	 * </ul>
	 * <p>
	 * We opt for the former behaviour, as it'message easier to do without access to the current {@link LuaState}.
	 *
	 * @param value The value to get the type of
	 * @return The resulting type.
	 */
	public static LuaString typeName(@Nullable LuaState state, LuaValue value) {
		// If state is present, works on everything. Otherwise, only table and userdata.
		if (state != null || value instanceof LuaTable || value instanceof LuaUserdata) {
			LuaTable metatable = value.getMetatable(state);
			if (metatable != null && metatable.rawget(NAME) instanceof LuaString s) return s;
		}
		return value.luaTypeName();
	}

	/**
	 * Throw a {@link LuaError} indicating an invalid argument was supplied to a function
	 *
	 * @param value    The current value
	 * @param expected String naming the type that was expected
	 * @return The created LuaError
	 */
	public static LuaError argError(LuaState state, LuaValue value, String expected) throws LuaUncatchableError {
		return new LuaError(new Buffer(32, state.allocationTracker)
			.append("bad argument (")
			.append(expected)
			.append(" expected, got ")
			.append(typeName(state, value))
			.append(")")
			.toLuaString()
		);
	}

	/**
	 * Throw an error indicating an incorrect number of arguments.
	 */
	public static LuaError argCountError(LuaState state, String functionName, int actualCount, int... allowedArgCounts) throws LuaUncatchableError {
		return switch (allowedArgCounts.length) {
			case 0 -> throw new IllegalArgumentException();
			case 1 -> new LuaError("Unexpected number of args to " + functionName + ". Expected " + allowedArgCounts[0] + ", got " + actualCount, state.allocationTracker);
			case 2 -> new LuaError("Unexpected number of args to " + functionName + ". Expected " + allowedArgCounts[0] + " or " + allowedArgCounts[1] + ", got " + actualCount, state.allocationTracker);
			default -> {
				String message = "Unexpected number of args to " + functionName + ". Expected ";
				for (int i = 0; i < allowedArgCounts.length - 1; i++)
					message += allowedArgCounts[i] + ", ";
				message += "or " + allowedArgCounts[allowedArgCounts.length - 1] + ", got " + actualCount;
				yield new LuaError(message, state.allocationTracker);
			}
		};
	}

	/**
	 * Throw a {@link LuaError} indicating an invalid argument was supplied to a function
	 *
	 * @param iarg index of the argument that was invalid, first index is 1
	 * @param msg  String providing information about the invalid argument
	 * @return The created LuaError
	 */
	public static LuaError argError(@Nullable AllocationTracker<LuaUncatchableError> allocTracker, int iarg, String msg) throws LuaUncatchableError {
		return new LuaError("bad argument #" + iarg + " (" + msg + ")", allocTracker);
	}

	/**
	 * Throw a {@link LuaError} indicating an invalid type was supplied to a function
	 *
	 * @param value    The current value
	 * @param expected String naming the type that was expected
	 * @return The created LuaError
	 */
	public static LuaError typeError(LuaState state, LuaValue value, String expected) throws LuaUncatchableError {
		return new LuaError(new Buffer(32, state.allocationTracker)
			.append(expected)
			.append(" expected, got ")
			.append(typeName(state, value))
			.toLuaString()
		);
	}

	public static LuaError operandError(LuaState state, LuaValue operand, String verb, int stack) throws LuaUncatchableError {
		ObjectName kind = null;
		if (stack >= 0) {
			DebugFrame info = DebugState.get(state).getStack();
			if (info != null && info.func instanceof LuaClosure closure && stack < closure.getPrototype().maxStackSize) {
				kind = DebugHelpers.getObjectName(info, stack);
			}
		}

		if (kind != null) {
			return new LuaError(new Buffer(32, state.allocationTracker)
				.append("attempt to ")
				.append(verb)
				.append(" ")
				.append(kind.what())
				.append(" '")
				.append(kind.name())
				.append("' (a ")
				.append(typeName(state, operand))
				.append(" value)")
				.toLuaString()
			);
		} else {
			return new LuaError(new Buffer(32, state.allocationTracker)
				.append("attempt to ")
				.append(verb)
				.append(" a ")
				.append(typeName(state, operand))
				.append(" value")
				.toLuaString()
			);
		}
	}

	/**
	 * Throw a {@link LuaError} based on a comparison error such as greater-than or less-than,
	 * typically due to an invalid operand type
	 *
	 * @param lhs Left-hand-side of the comparison that resulted in the error.
	 * @param rhs Right-hand-side of the comparison that resulted in the error.
	 * @return The created LuaError
	 */
	public static LuaError compareError(LuaState state, LuaValue lhs, LuaValue rhs) throws LuaUncatchableError {
		LuaString lhsType = typeName(state, lhs), rhsType = typeName(state, rhs);
		if (lhsType.equals(rhsType)) {
			return new LuaError(new Buffer(state.allocationTracker)
				.append("attempt to compare two ")
				.append(lhsType)
				.append(" values")
				.toLuaString()
			);
		} else {
			return new LuaError(new Buffer(state.allocationTracker)
				.append("attempt to compare ")
				.append(lhsType)
				.append(" with ")
				.append(rhsType)
				.toLuaString()
			);
		}
	}
}
