package org.figuramc.figura_cobalt.org.squiddev.cobalt.unwind;

import org.figuramc.figura_cobalt.LuaUncatchableError;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.LuaError;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.LuaState;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.UnwindThrowable;

/**
 * A function which may be called once, and then later treated as a {@link SuspendedTask suspended task}.
 *
 * @param <T> The result type of this function.
 */
public interface SuspendedFunction<T> extends SuspendedTask<T> {
	T call(LuaState state) throws LuaError, LuaUncatchableError, UnwindThrowable;
}
