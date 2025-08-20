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
import org.figuramc.figura_cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura_cobalt.org.squiddev.cobalt.function.RegisteredFunction;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Random;

import static org.figuramc.figura_cobalt.org.squiddev.cobalt.ValueFactory.varargsOf;

/**
 * Subclass of {@link LibFunction} which implements the lua standard {@code math}
 * library.
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 *
 * @see LibFunction
 * @see CoreLibraries
 * @see <a href="http://www.lua.org/manual/5.1/manual.html#5.6">http://www.lua.org/manual/5.1/manual.html#5.6</a>
 */
public final class MathLib {
	private @Nullable RandomState random;

	private MathLib() {
	}

	public static void add(LuaState state) throws LuaError, LuaUncatchableError {
		var self = new MathLib();
		final RegisteredFunction[] functions = new RegisteredFunction[]{
			RegisteredFunction.of("abs", (s, arg) -> LuaDouble.valueOf(Math.abs(arg.checkDouble(s)))),
			RegisteredFunction.of("ceil", (s, arg) -> LuaDouble.valueOf(Math.ceil(arg.checkDouble(s)))),
			RegisteredFunction.of("cos", (s, arg) -> LuaDouble.valueOf(Math.cos(arg.checkDouble(s)))),
			RegisteredFunction.of("deg", (s, arg) -> LuaDouble.valueOf(Math.toDegrees(arg.checkDouble(s)))),
			RegisteredFunction.of("exp", (s, arg) -> LuaDouble.valueOf(Math.exp(arg.checkDouble(s)))),
			RegisteredFunction.of("floor", (s, arg) -> LuaDouble.valueOf(Math.floor(arg.checkDouble(s)))),
			RegisteredFunction.of("rad", (s, arg) -> LuaDouble.valueOf(Math.toRadians(arg.checkDouble(s)))),
			RegisteredFunction.of("sin", (s, arg) -> LuaDouble.valueOf(Math.sin(arg.checkDouble(s)))),
			RegisteredFunction.of("sqrt", (s, arg) -> LuaDouble.valueOf(Math.sqrt(arg.checkDouble(s)))),
			RegisteredFunction.of("tan", (s, arg) -> LuaDouble.valueOf(Math.tan(arg.checkDouble(s)))),
			RegisteredFunction.of("acos", (s, arg) -> LuaDouble.valueOf(Math.acos(arg.checkDouble(s)))),
			RegisteredFunction.of("asin", (s, arg) -> LuaDouble.valueOf(Math.asin(arg.checkDouble(s)))),
			RegisteredFunction.of("atan", MathLib::atan),
			RegisteredFunction.of("cosh", (s, arg) -> LuaDouble.valueOf(Math.cosh(arg.checkDouble(s)))),
			RegisteredFunction.of("log10", (s, arg) -> LuaDouble.valueOf(Math.log10(arg.checkDouble(s)))),
			RegisteredFunction.of("sinh", (s, arg) -> LuaDouble.valueOf(Math.sinh(arg.checkDouble(s)))),
			RegisteredFunction.of("tanh", (s, arg) -> LuaDouble.valueOf(Math.tanh(arg.checkDouble(s)))),
			RegisteredFunction.of("fmod", MathLib::fmod),
			RegisteredFunction.of("ldexp", MathLib::ldexp),
			RegisteredFunction.of("pow", (s, x, y) -> LuaDouble.valueOf(Math.pow(x.checkDouble(s), y.checkDouble(s)))),
			RegisteredFunction.of("atan2", (s, x, y) -> LuaDouble.valueOf(Math.atan2(x.checkDouble(s), y.checkDouble(s)))),
			RegisteredFunction.of("log", MathLib::log),
			RegisteredFunction.ofV("frexp", MathLib::frexp),
			RegisteredFunction.ofV("max", MathLib::max),
			RegisteredFunction.ofV("min", MathLib::min),
			RegisteredFunction.ofV("modf", MathLib::modf),
			// We need to capture the current random state. This is implemented as an upvalue in PUC Lua.
			RegisteredFunction.ofV("randomseed", self::randomseed),
			RegisteredFunction.ofV("random", self::random),
		};

		LuaTable t = new LuaTable(0, functions.length + 3, state.allocationTracker);
		RegisteredFunction.bind(t, functions);
		t.rawset("pi", LuaDouble.valueOf(Math.PI));
		t.rawset("huge", LuaDouble.POSINF);
		t.rawset("mod", t.rawget("fmod"));

		LibFunction.setGlobalLibrary(state, "math", t);
	}

	private static LuaValue fmod(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError, LuaUncatchableError {
		double x = arg1.checkDouble(state);
		double y = arg2.checkDouble(state);
		double q = x / y;
		double f = x - y * (q >= 0 ? Math.floor(q) : Math.ceil(q));
		return LuaDouble.valueOf(f);
	}

	private static LuaValue ldexp(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError, LuaUncatchableError {
		double x = arg1.checkDouble(state);
		double y = arg2.checkDouble(state) + 1023.5;
		long e = (long) ((0 != (1 & ((int) y))) ? Math.floor(y) : Math.ceil(y - 1));
		return LuaDouble.valueOf(x * Double.longBitsToDouble(e << 52));
	}

	private static LuaValue log(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError, LuaUncatchableError {
		if (arg2.isNil()) {
			return LuaDouble.valueOf(Math.log(arg1.checkDouble(state)));
		} else {
			return LuaDouble.valueOf(Math.log(arg1.checkDouble(state)) / Math.log(arg2.checkDouble(state)));
		}
	}

	private static LuaValue atan(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError, LuaUncatchableError {
		if (arg2.isNil()) {
			return LuaDouble.valueOf(Math.atan(arg1.checkDouble(state)));
		} else {
			return LuaDouble.valueOf(Math.atan2(arg1.checkDouble(state), arg2.checkDouble(state)));
		}
	}

	private static Varargs frexp(LuaState state, Varargs args) throws LuaError, LuaUncatchableError {
		double x = args.arg(1).checkDouble(state);
		if (x == 0) return varargsOf(Constants.ZERO, Constants.ZERO);
		long bits = Double.doubleToLongBits(x);
		double m = ((bits & (~(-1L << 52))) + (1L << 52)) * ((bits >= 0) ? (.5 / (1L << 52)) : (-.5 / (1L << 52)));
		double e = (((int) (bits >> 52)) & 0x7ff) - 1022;
		return varargsOf(LuaDouble.valueOf(m), LuaDouble.valueOf(e));
	}

	private static LuaValue max(LuaState state, Varargs args) throws LuaError, LuaUncatchableError {
		double m = args.arg(1).checkDouble(state);
		for (int i = 2, n = args.count(); i <= n; ++i) {
			m = Math.max(m, args.arg(i).checkDouble(state));
		}
		return LuaDouble.valueOf(m);
	}

	private static LuaValue min(LuaState state, Varargs args) throws LuaError, LuaUncatchableError {
		double m = args.arg(1).checkDouble(state);
		for (int i = 2, n = args.count(); i <= n; ++i) {
			m = Math.min(m, args.arg(i).checkDouble(state));
		}
		return LuaDouble.valueOf(m);
	}

	private static Varargs modf(LuaState state, Varargs args) throws LuaError, LuaUncatchableError {
		double x = args.arg(1).checkDouble(state);
		double intPart = (x > 0) ? Math.floor(x) : Math.ceil(x);
		double fracPart = x - intPart;
		return varargsOf(LuaDouble.valueOf(intPart), LuaDouble.valueOf(fracPart));
	}

	private RandomState getRandom() {
		return random != null ? random : (random = new RandomState());
	}

	private Varargs randomseed(LuaState state, Varargs args) throws LuaError, LuaUncatchableError {
		var random = getRandom();
		long part1, part2;
		if (args.count() == 0) {
			part1 = random.seeder.nextLong();
			part2 = random.seeder.nextLong();
		} else {
			part1 = args.arg(1).checkLong(state);
			part2 = args.arg(2).optLong(state, 0);
		}

		random.seed(part1, part2);
		return varargsOf(LuaDouble.valueOf(part1), LuaDouble.valueOf(part2));
	}

	private LuaValue random(LuaState state, Varargs args) throws LuaError, LuaUncatchableError {
		if (random == null) random = new RandomState();

		switch (args.count()) {
			case 0 -> {
				return LuaDouble.valueOf(random.nextDouble());
			}
			case 1 -> {
				int high = args.arg(1).checkInteger(state);
				if (high < 1) throw ErrorFactory.argError(state.allocationTracker, 1, "interval is empty");
				return LuaDouble.valueOf(1 + random.nextLong(high - 1));
			}
			case 2 -> {
				long low = args.arg(1).checkLong(state);
				long high = args.arg(2).checkLong(state);
				if (high < low) throw ErrorFactory.argError(state.allocationTracker, 2, "interval is empty");
				return LuaDouble.valueOf(low + random.nextLong(high - low));
			}
			default -> throw new LuaError("wrong number of arguments", state.allocationTracker);
		}
	}

	private static class RandomState {
		private static final int FIGS = 53;
		private static final int SHIFT_FIGS = Long.SIZE - FIGS;
		private static final double SCALE_FIG = 0.5 / (1L << (FIGS - 1));

		final Random seeder = new Random();
		private long state0, state1, state2, state3;

		RandomState() {
			seed(seeder.nextLong(), seeder.nextLong());
		}

		long nextLong() {
			long state0 = this.state0;
			long state1 = this.state1;
			long state2 = this.state2 ^ state0;
			long state3 = this.state3 ^ state1;
			long res = Long.rotateLeft(state1 * 5, 7) * 9;
			this.state0 = state0 ^ state3;
			this.state1 = state1 ^ state2;
			this.state2 = state2 ^ (state1 << 17);
			this.state3 = Long.rotateLeft(state3, 45);
			return res;
		}

		long nextLong(long n) {
			long random = nextLong();
			// If n + 1 is a power of 2, truncate it.
			if ((n & (n + 1)) == 0) return random & n;

			// Compute next power of 2 after n, minus 1.
			long lim = (1L << (64 - Long.numberOfLeadingZeros(n))) - 1;
			assert (lim & (lim + 1)) == 0;
			assert lim > n && lim >> 1 < n;

			// Project our random number into the range 0..lim.
			random &= lim;

			// If that's not inside 0..n, then just generate another random number.
			while (Long.compareUnsigned(random, n) > 0) {
				// If not inside 0..n, then abort.
				random = nextLong() & lim;
			}

			return random;
		}

		double nextDouble() {
			double value = (nextLong() >> SHIFT_FIGS) * SCALE_FIG;
			return value < 0 ? value + 1 : value;
		}

		void seed(long part1, long part2) {
			state0 = part1;
			state1 = 0xff;
			state2 = part2;
			state3 = 0;
			for (int i = 0; i < 16; i++) nextLong();
		}
	}
}
