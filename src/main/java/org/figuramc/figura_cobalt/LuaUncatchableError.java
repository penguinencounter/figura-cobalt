package org.figuramc.figura_cobalt;

// Don't let Lua catch these errors by marking them "Throwable" instead of "Exception", since
// ProtectedCall only catches Exception and VirtualMachineError.
//
// This is essentially what CC:Tweaked does with timeout/interrupt, they create "HardAbortError extends Error".
// We don't extend "Error" here because we want OOMs to be a checked exception, not unchecked.
//
// For users of this library, they should use instances or subclasses of this for situations that should be unrecoverable.
public class LuaUncatchableError extends Throwable {
    public LuaUncatchableError() { super(); }
    public LuaUncatchableError(String message) { super(message); }
    public LuaUncatchableError(Throwable cause) { super(cause); }
    public LuaUncatchableError(String message, Throwable cause) { super(message, cause); }
}
