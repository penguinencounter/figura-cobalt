package org.figuramc.figura_cobalt.org.squiddev.cobalt;

import org.figuramc.figura_cobalt.LuaUncatchableError;
import org.figuramc.memory_tracker.AllocationTracker;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The global registry, a store of Lua values
 */
public final class GlobalRegistry {

	private final @Nullable AllocationTracker<LuaUncatchableError> allocTracker;

	private final LuaTable table;

	GlobalRegistry(@Nullable AllocationTracker<LuaUncatchableError> allocTracker) throws LuaUncatchableError {
		this.allocTracker = allocTracker;
		this.table = new LuaTable(allocTracker);
	}

	/**
	 * Get the underlying registry table.
	 *
	 * @return The global debug registry.
	 */
	public LuaTable get() {
		return table;
	}

	/**
	 * Get a subtable in the global {@linkplain #get()} registry table}. If the key exists but is not a table, then
	 * it will be overridden.
	 *
	 * @param name The name of the registry table.
	 * @return The subentry.
	 */
	public LuaTable getSubTable(LuaString name) throws LuaError, LuaUncatchableError {
		LuaValue value = table.rawget(name);
		if (value instanceof LuaTable table) return table;

		LuaTable newValue = new LuaTable(allocTracker);
		table.rawset(name, newValue);
		return newValue;
	}

}
