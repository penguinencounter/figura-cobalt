package org.figuramc.figura_cobalt.cc.tweaked.cobalt.internal.unwind;

import org.figuramc.figura_cobalt.LuaUncatchableError;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.LuaError;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.UnwindThrowable;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.Varargs;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.debug.DebugFrame;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.unwind.SuspendedFunction;
import org.jetbrains.annotations.Contract;

/**
 * A functional interface which starts a task.
 *
 * @param <T> The result of this task.
 */
@FunctionalInterface
public interface SuspendedAction<T> {
	/**
	 * Run the provided action. If it yields, store the resulting suspended task into {@link DebugFrame#state}.
	 * <p>
	 * This method should only be used with {@link AutoUnwind} instrumented functions. It will be replaced at compile
	 * time with a direct dispatch.
	 *
	 * @param frame  The current call frame, into which to store our continuation.
	 * @param action The suspendable action. This should be a constant lambda.
	 * @return The result of evaluating this function.
	 * @throws LuaError        If the function threw a runtime error.
	 * @throws UnwindThrowable If the function yielded.
	 */
	@Contract("_, _ -> _")
	static Varargs run(DebugFrame frame, SuspendedAction<Varargs> action) throws LuaError, LuaUncatchableError, UnwindThrowable {
		throw new AssertionError("Calls to this method should not appear in transformed code.");
	}

	/**
	 * Run the provided {@link SuspendedAction}, asserting that it will never yield.
	 * <p>
	 * This method should only be used with {@link AutoUnwind} instrumented functions. It will be replaced at compile
	 * time with a direct dispatch.
	 *
	 * @param action The function to run.
	 * @param <T>    The result type of this function (and resulting task).
	 * @return The result of evaluating this function.
	 * @throws LuaError If the function threw a runtime error.
	 */
	@Contract("_ -> _")
	static <T> T noYield(SuspendedAction<T> action) throws LuaError, LuaUncatchableError {
		throw new AssertionError("Calls to this method should not appear in transformed code.");
	}

	/**
	 * Convert the provided {@link SuspendedAction} to a {@link SuspendedFunction}.
	 * <p>
	 * This method should only be used with {@link AutoUnwind} instrumented functions. It will be replaced at compile
	 * time with a direct dispatch.
	 *
	 * @param action The function to run.
	 * @param <T>    The result type of this function (and resulting task).
	 * @return A {@link SuspendedFunction}, which can be later invoked.
	 * @see org.figuramc.figura_cobalt.org.squiddev.cobalt.function.SuspendedVarArgFunction
	 */
	@Contract("_ -> _")
	static <T> SuspendedFunction<T> toFunction(SuspendedAction<T> action) {
		throw new AssertionError("Calls to this method should not appear in transformed code.");
	}

	T run() throws LuaError, LuaUncatchableError, UnwindThrowable;
}
