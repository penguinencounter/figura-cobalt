package org.figuramc.figura_cobalt.cc.tweaked.cobalt.internal.unwind;

import org.figuramc.figura_cobalt.org.squiddev.cobalt.Varargs;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The state of an object being unwound.
 *
 * @deprecated This should not be referenced from user code.
 */
@Deprecated
public class UnwindState {
	public int state;
	public @Nullable Varargs resumeArgs;
	public @Nullable Object child;

	public static UnwindState getOrCreate(Object x) {
		return x == null ? new UnwindState() : (UnwindState) x;
	}
}
