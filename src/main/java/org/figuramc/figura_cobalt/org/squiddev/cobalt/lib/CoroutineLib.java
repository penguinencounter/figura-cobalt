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
package org.figuramc.figura_cobalt.org.squiddev.cobalt.lib;


import org.figuramc.figura_cobalt.LuaUncatchableError;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.debug.DebugFrame;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.function.LuaFunction;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.function.RegisteredFunction;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.function.ResumableVarArgFunction;

import static org.figuramc.figura_cobalt.org.squiddev.cobalt.ValueFactory.varargsOf;
import static org.figuramc.figura_cobalt.org.squiddev.cobalt.debug.DebugFrame.FLAG_YPCALL;

/**
 * Subclass of {@link LibFunction} which implements the lua standard {@code coroutine}
 * library.
 * <p>
 * The coroutine library in luaj has the same behavior as the
 * coroutine library in C, but is implemented using Java Threads to maintain
 * the call state between invocations.  Therefore it can be yielded from anywhere,
 * similar to the "Coco" yield-from-anywhere patch available for C-based lua.
 * However, coroutines that are yielded but never resumed to complete their execution
 * may not be collected by the garbage collector.
 *
 * @see LibFunction
 * @see CoreLibraries
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.2">http://www.lua.org/manual/5.1/manual.html#5.2</a>
 */
public final class CoroutineLib {
	private CoroutineLib() {
	}

	public static void add(LuaState state) throws LuaError, LuaUncatchableError {
		LibFunction.setGlobalLibrary(state, "coroutine", RegisteredFunction.bind(state, new RegisteredFunction[]{
			RegisteredFunction.of("create", CoroutineLib::create),
			RegisteredFunction.ofV("running", CoroutineLib::running),
			RegisteredFunction.of("status", CoroutineLib::status),
			RegisteredFunction.of("isyieldable", CoroutineLib::isyieldable),
			RegisteredFunction.of("wrap", CoroutineLib::wrap),
			RegisteredFunction.ofFactory("resume", Resume::new),
			RegisteredFunction.ofFactory("yield", Yield::new),
		}));
	}

	private static LuaValue create(LuaState state, LuaValue arg) throws LuaError, LuaUncatchableError {
		final LuaFunction func = arg.checkFunction(state);
		return new LuaThread(state, func);
	}

	private static Varargs running(LuaState state, Varargs args) {
		LuaThread r = state.getCurrentThread();
		return varargsOf(r, LuaBoolean.valueOf(r.isMainThread()));
	}

	private static LuaValue status(LuaState state, LuaValue arg) throws LuaError, LuaUncatchableError {
		return arg.checkThread(state).getStatus().getDisplayNameValue();
	}

	private static LuaValue isyieldable(LuaState state, LuaValue arg) throws LuaError, LuaUncatchableError {
		// Much simpler in our case, as coroutines can always yield.
		return LuaBoolean.valueOf(!arg.optThread(state, state.getCurrentThread()).isMainThread());
	}

	private static LuaValue wrap(LuaState state, LuaValue arg) throws LuaError, LuaUncatchableError {
		final LuaFunction func = arg.checkFunction(state);
		final LuaThread thread = new LuaThread(state, func);

		return new Wrapped(thread);
	}

	private static class Resume extends ResumableVarArgFunction<Void> {
		@Override
		protected Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, LuaUncatchableError, UnwindThrowable {
			// TODO: Is this really the right way to do this?
			di.flags |= FLAG_YPCALL;
			LuaThread thread = args.arg(1).checkThread(state);
			try {
				Varargs result = LuaThread.resume(state, thread, args.subargs(2));
				return varargsOf(Constants.TRUE, result);
			} catch (LuaError le) {
				return varargsOf(Constants.FALSE, le.getValue());
			}
		}

		@Override
		public Varargs resume(LuaState state, Void object, Varargs value) {
			return varargsOf(Constants.TRUE, value);
		}

		@Override
		public Varargs resumeError(LuaState state, Void object, LuaError error) {
			return varargsOf(Constants.FALSE, error.getValue());
		}
	}

	private static class Yield extends ResumableVarArgFunction<Void> {
		@Override
		protected Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, LuaUncatchableError, UnwindThrowable {
			return LuaThread.yield(state, args);
		}

		@Override
		public Varargs resume(LuaState state, Void object, Varargs value) {
			return value;
		}
	}

	private static class Wrapped extends ResumableVarArgFunction<Void> {
		private final LuaThread thread;

		private Wrapped(LuaThread thread) {
			this.thread = thread;
		}

		@Override
		protected Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, LuaUncatchableError, UnwindThrowable {
			return LuaThread.resume(state, thread, args);
		}

		@Override
		public Varargs resume(LuaState state, Void object, Varargs value) {
			return value;
		}
	}
}
