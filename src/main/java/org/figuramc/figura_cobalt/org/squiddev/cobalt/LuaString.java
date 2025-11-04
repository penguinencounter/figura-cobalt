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
import org.figuramc.figura_cobalt.cc.tweaked.cobalt.internal.string.NumberParser;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.figuramc.figura_cobalt.org.squiddev.cobalt.Constants.NIL;

/**
 * Subclass of {@link LuaValue} for representing lua strings.
 * <p>
 * Because lua string values are more nearly sequences of bytes than
 * sequences of characters or unicode code points, the {@link LuaString}
 * implementation holds the string value in an internal byte array.
 * <p>
 * {@link LuaString} values are generally not mutable once constructed,
 * so multiple {@link LuaString} values can chare a single byte array.
 * <p>
 * Currently {@link LuaString}s are pooled via a centrally managed weak table.
 * To ensure that as many string values as possible take advantage of this,
 * Constructors are not exposed directly.  As with number, booleans, and nil,
 * instance construction should be via {@link LuaString#valueOfNoCopy(byte[])} or similar API.
 *
 * @see LuaValue
 */
public final class LuaString extends LuaValue implements Comparable<LuaString> {
	/**
	 * Size of cache of recent short strings. This is the maximum number of LuaStrings that
	 * will be retained in the cache of recent short strings. Must be a power of 2.
	 */
	public static final int RECENT_STRINGS_CACHE_SIZE = 128;

	/**
	 * Maximum length of a string to be considered for recent short strings caching.
	 * This effectively limits the total memory that can be spent on the recent strings cache,
	 * because no LuaString whose backing exceeds this length will be put into the cache.
	 */
	public static final int RECENT_STRINGS_MAX_LENGTH = 32;

	/**
	 * The contents of this string.
	 */
	private final byte[] contents;

	/**
	 * The offset into the byte array, 0 means start at the first byte
	 */
	private final int offset;

	/**
	 * The number of bytes that comprise this string
	 */
	private final int length;

	private int hashCode;

	private static class Cache {
		/**
		 * Simple cache of recently created strings that are short.
		 * This is simply a list of strings, indexed by their hash codes modulo the cache size
		 * that have been recently constructed.  If a string is being constructed frequently
		 * from different contexts, it will generally may show up as a cache hit and resolve
		 * to the same value.
		 */
		public final LuaString[] recentShortStrings = new LuaString[RECENT_STRINGS_CACHE_SIZE];

		public LuaString get(LuaString s) {
			final int index = s.hashCode() & (RECENT_STRINGS_CACHE_SIZE - 1);
			final LuaString cached = recentShortStrings[index];
			if (cached != null && s.equals(cached)) {
				return cached;
			}
			recentShortStrings[index] = s;
			return s;
		}

		public static final ThreadLocal<Cache> INSTANCE = ThreadLocal.withInitial(Cache::new);
	}

	/**
	 * Get a {@link LuaString} instance whose bytes match
	 * the supplied Java String which will be limited to the 0-255 range
	 *
	 * @param string Java String containing characters which will be limited to the 0-255 range
	 * @return {@link LuaString} with bytes corresponding to the supplied String
	 */
	public static LuaString valueOf(@Nullable AllocationTracker<LuaUncatchableError> allocTracker, String string) throws LuaUncatchableError {
		byte[] bytes = new byte[string.length()];
		if (allocTracker != null) allocTracker.track(bytes);
		encode(string, bytes, 0);
		return valueOf(null, bytes, 0, bytes.length);
	}

	// Helper to call valueOf() with no allocator, which doesn't throw the exception.
	// Use this for those static global strings which cost negligible space.
	public static LuaString valueOfNoAlloc(String string) {
		try {
			return valueOf(null, string);
		} catch (LuaUncatchableError impossible) {
			throw new IllegalStateException("Should never happen. Contact Figura devs!", impossible);
		}
	}

	/**
	 * Construct a {@link LuaString} around a byte array without copying the contents.
	 * <p>
	 * The array is used directly after this is called, so clients must not change contents.
	 *
	 * @param bytes byte buffer
	 * @param off   offset into the byte buffer
	 * @param len   length of the byte buffer
	 * @return {@link LuaString} wrapping the byte buffer
	 */
	public static LuaString valueOf(@Nullable AllocationTracker<LuaUncatchableError> allocTracker, byte[] bytes, int off, int len, boolean forceNoCopy) throws LuaUncatchableError {
		// Don't bother tracking strings that are shorter than RECENT_STRINGS_MAX_LENGTH.
		// They aren't the memory hogs anyway.
		if (bytes.length < RECENT_STRINGS_MAX_LENGTH) {
			return Cache.INSTANCE.get().get(new LuaString(bytes, off, len));
		} else if (forceNoCopy || len >= bytes.length / 2) {
			// Reuse backing only when more than half the bytes are part of the result.
			// Backing is reused, don't track.
			return new LuaString(bytes, off, len);
		} else {
			// Short result relative to the source.  Copy only the bytes that are actually to be used.
			final byte[] b = new byte[len];
			if (allocTracker != null) allocTracker.track(b);
			System.arraycopy(bytes, off, b, 0, len);
			LuaString string = new LuaString(b, 0, len);
			return len < RECENT_STRINGS_MAX_LENGTH ? Cache.INSTANCE.get().get(string) : string;
		}
	}
	public static LuaString valueOf(@Nullable AllocationTracker<LuaUncatchableError> allocTracker, byte[] bytes, int off, int len) throws LuaUncatchableError {
		return valueOf(allocTracker, bytes, off, len, false);
	}

	// Helper to call valueOf() without copying the byte[], which doesn't take significant space
	// Expects the byte[] to already be known about by the allocator (if there is an allocator).
	public static LuaString valueOfNoCopy(byte[] bytes, int off, int len) {
		try {
			return valueOf(null, bytes, off, len, true);
		} catch (LuaUncatchableError impossible) {
			throw new IllegalStateException("Should never happen. Contact Figura devs!", impossible);
		}
	}

	/**
	 * Construct a {@link LuaString} around a byte array without copying the contents.
	 * <p>
	 * The array is used directly after this is called, so clients must not change contents.
	 *
	 * @param bytes byte buffer
	 * @return {@link LuaString} wrapping the byte buffer
	 */
	public static LuaString valueOfNoCopy(byte[] bytes) {
		return valueOfNoCopy(bytes, 0, bytes.length);
	}

	/**
	 * Create a string from a concatenation of other strings. This may be more efficient than building a string
	 * with {@link Buffer} as it defers allocating the underlying byte array.
	 *
	 * @param contents  The array of strings to build this from. All values in the range {@code [offset, offset+length)}
	 *                  must be {@link LuaString}s.
	 * @param offset    The offset into this array.
	 * @param length    The number of values in this array.
	 * @param strLength The length of the resulting string. This must be equal
	 * @return The resulting Lua string.
	 */
	public static LuaString valueOfStrings(@Nullable AllocationTracker<LuaUncatchableError> allocTracker, LuaValue[] contents, int offset, int length, int strLength) throws LuaUncatchableError {
		if (length == 0 || strLength == 0) return Constants.EMPTYSTRING;
		if (length == 1) return (LuaString) contents[0];

		byte[] out = new byte[strLength];
		if (allocTracker != null) allocTracker.track(out);
		int position = 0;
		for (int i = 0; i < length; i++) position = ((LuaString) contents[offset + i]).copyTo(out, position);
		return valueOfNoCopy(out); // out is already tracked
	}

	/**
	 * Construct a {@link LuaString} around a byte array without copying the contents.
	 * <p>
	 * The array is used directly after this is called, so clients must not change contents.
	 *
	 * @param contents byte buffer
	 * @param offset   offset into the byte buffer
	 * @param length   length of the byte buffer
	 */
	private LuaString(byte[] contents, int offset, int length) {
		super(Constants.TSTRING);
		this.contents = contents;
		this.offset = offset;
		this.length = length;
	}

	@Override
	@Deprecated
	public String toString() {
		try {
			return decode(null, contents, offset, length);
		} catch (LuaUncatchableError impossible) {
			throw new IllegalStateException("Should never happen. Contact Figura devs!", impossible);
		}
	}

	public String toJavaString(@Nullable AllocationTracker<LuaUncatchableError> allocTracker) throws LuaUncatchableError {
		return decode(allocTracker, contents, offset, length);
	}

	@Override
	public LuaTable getMetatable(LuaState state) {
		return state.stringMetatable;
	}

	public int length() {
		return length;
	}

	//region Equality and comparison
	@Override
	public int compareTo(LuaString rhs) {
		byte[] bytes = contents, rhsBytes = rhs.contents;
		// Find the first mismatched character in 0..n
		int len = Math.min(length, rhs.length);
		int mismatch = Arrays.mismatch(bytes, offset, offset + len, rhsBytes, rhs.offset, rhs.offset + len);
		if (mismatch >= 0) return Byte.compareUnsigned(bytes[offset + mismatch], rhsBytes[rhs.offset + mismatch]);

		// If one is a prefix of the other, sort by length.
		return length - rhs.length;
	}

	@Override
	public boolean equals(Object o) {
		return this == o || (o instanceof LuaString str && equals(str));
	}

	private boolean equals(LuaString s) {
		if (this == s) return true;
		if (s.length != length) return false;
		if (contents == s.contents && s.offset == offset) return true;
		if (s.hashCode() != hashCode()) return false;

		return equals(contents, offset, s.contents, s.offset, length);
	}

	// Figura function
	// Not sure about how to handle encodings...
	public boolean equals(String javaString) {
		int c = javaString.length();
		if (this.length != c) return false;
        for (int i = 0; i < c; i++)
			if ((contents[this.offset + i] & 0xFF) != javaString.charAt(i))
				return false;
		return true;
	}

	public static boolean equals(LuaString a, int aOffset, LuaString b, int bOffset, int length) {
		return equals(a.contents, a.offset + aOffset, b.contents, b.offset + bOffset, length);
	}

	private static boolean equals(byte[] a, int aOffset, byte[] b, int bOffset, int length) {
		return Arrays.equals(a, aOffset, aOffset + length, b, bOffset, bOffset + length);
	}

	// Figura function
	// Check if it ends with the given java string, java string must be ascii
	public boolean endsWith(String javaString) {
		assert javaString.chars().allMatch(c -> c < 128);
		int c = javaString.length();
		int o = this.length - c;
		if (o < 0) return false;
		o += this.offset;
		for (int i = 0; i < c; i++)
			if ((contents[o + i] & 0xFF) != javaString.charAt(i))
				return false;
		return true;
	}


	@Override
	public int hashCode() {
		int h = hashCode;
		if (h != 0) return h;

		h = length;  /* seed */
		int step = (length >> 5) + 1;  /* if string is too long, don't hash all its chars */
		for (int l1 = length; l1 >= step; l1 -= step)  /* compute hash */ {
			h = h ^ ((h << 5) + (h >> 2) + (((int) contents[offset + l1 - 1]) & 0x0FF));
		}
		return hashCode = h;
	}
	// endregion

	// region String operations
	// Substring of existing string -> no real new allocation
	public LuaString substringOfLen(int beginIndex, int length) {
		return valueOfNoCopy(contents, offset + beginIndex, length);
	}

	public LuaString substringOfEnd(int beginIndex, int endIndex) {
		return valueOfNoCopy(contents, offset + beginIndex, endIndex - beginIndex);
	}

	public LuaString substring(int beginIndex) {
		return valueOfNoCopy(contents, offset + beginIndex, length - 1);
	}

	public byte byteAt(int index) {
		if (index < 0 || index >= length) throw new IndexOutOfBoundsException();
		return contents[offset + index];
	}

	public int charAt(int index) {
		if (index < 0 || index >= length) throw new IndexOutOfBoundsException();
		return Byte.toUnsignedInt(contents[offset + index]);
	}

	public boolean startsWith(byte character) {
		return length != 0 && byteAt(0) == character;
	}

	/**
	 * Java version of strpbrk - find index of any byte that in an accept string.
	 *
	 * @param accept {@link LuaString} containing characters to look for.
	 * @return index of first match in the {@code accept} string, or -1 if not found.
	 */
	public int indexOfAny(LuaString accept) {
		final int limit = offset + length;
		final int searchLimit = accept.offset + accept.length;
		for (int i = offset; i < limit; ++i) {
			for (int j = accept.offset; j < searchLimit; ++j) {
				if (contents[i] == accept.contents[j]) return i - offset;
			}
		}
		return -1;
	}

	/**
	 * Find the index of a byte in this string.
	 *
	 * @param b the byte to look for
	 * @return index of first match found, or -1 if not found.
	 */
	public int indexOf(byte b) {
		for (int i = 0, j = offset; i < length; ++i) {
			if (contents[j++] == b) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Find the index of a string starting at a point in this string
	 *
	 * @param search the string to search for
	 * @param start  the first index in the string
	 * @return index of first match found, or -1 if not found.
	 */
	public int indexOf(LuaString search, int start) {
		final int searchLen = search.length();
		final int limit = offset + length - searchLen;
		for (int i = offset + start; i <= limit; ++i) {
			if (equals(contents, i, search.contents, search.offset, searchLen)) {
				return i - offset;
			}
		}
		return -1;
	}

	/**
	 * Find the last index of a character in this string
	 *
	 * @param c the character to search for
	 * @return index of last match found, or -1 if not found.
	 */
	public int lastIndexOf(byte c) {
		for (int i = offset + length - 1; i >= offset; i--) {
			if (contents[i] == c) return i;
		}
		return -1;
	}
	// endregion

	// region Byte export

	/**
	 * Write this string to a {@link DataOutput}.
	 *
	 * @param output The output to write this to.
	 * @throws IOException If the underlying writer fails.
	 */
	public void write(DataOutput output) throws IOException {
		output.write(contents, offset, length);
	}

	/**
	 * Write this string to a {@link OutputStream}.
	 *
	 * @param output The output to write this to.
	 * @throws IOException If the underlying writer fails.
	 */
	public void write(OutputStream output) throws IOException {
		output.write(contents, offset, length);
	}

	/**
	 * Convert value to an input stream.
	 *
	 * @return {@link InputStream} whose data matches the bytes in this {@link LuaString}
	 */
	public InputStream toInputStream() {
		return new ByteArrayInputStream(contents, offset, length);
	}

	/**
	 * Convert this string to a {@link ByteBuffer}.
	 *
	 * @return A view over the underlying string.
	 */
	public ByteBuffer toBuffer() {
		return ByteBuffer.wrap(contents, offset, length).asReadOnlyBuffer();
	}

	/**
	 * Copy the bytes of the string into the given byte array.
	 *
	 * @param strOffset   offset from which to copy
	 * @param bytes       destination byte array
	 * @param arrayOffset offset in destination
	 * @param len         number of bytes to copy
	 * @return The next byte free
	 */
	public int copyTo(int strOffset, byte[] bytes, int arrayOffset, int len) {
		if (strOffset < 0 || len > length - strOffset) throw new IndexOutOfBoundsException();
		System.arraycopy(contents, offset + strOffset, bytes, arrayOffset, len);
		return arrayOffset + len;
	}

	/**
	 * Copy the bytes of the string into the given byte array.
	 *
	 * @param dest       destination byte array
	 * @param destOffset offset in destination
	 * @return The next byte free
	 */
	public int copyTo(byte[] dest, int destOffset) {
		// We could avoid unpacking the bytes here, but it's not clear it's worth it.
		System.arraycopy(contents, offset, dest, destOffset, length);
		return destOffset + length;
	}
	// endregion

	/**
	 * Convert to Java String
	 *
	 * @param bytes  byte array to convert
	 * @param offset starting index in byte array
	 * @param length number of bytes to convert
	 * @return Java String corresponding to the value of bytes interpreted using UTF8
	 * @see #encode(String, byte[], int)
	 */
	private static String decode(@Nullable AllocationTracker<LuaUncatchableError> allocTracker, byte[] bytes, int offset, int length) throws LuaUncatchableError {
		char[] chars = new char[length];
		for (int i = 0; i < length; i++) {
			chars[i] = ((char) (bytes[offset + i] & 0xFF));
		}
		String s = String.valueOf(chars);
		if (allocTracker != null) allocTracker.track(s);
		return s;
	}

	/**
	 * Encode the given Java string with characters limited to the 0-255 range,
	 * writing the result to bytes starting at offset.
	 * <p>
	 * The string should be measured first with lengthAsUtf8
	 * to make sure the given byte array is large enough.
	 *
	 * @param string Array of characters to be encoded
	 * @param bytes  byte array to hold the result
	 * @param off    offset into the byte array to start writing
	 * @see #decode(AllocationTracker, byte[], int, int)
	 */
	public static void encode(String string, byte[] bytes, int off) {
		int length = string.length();
		for (int i = 0; i < length; i++) {
			int c = string.charAt(i);
			bytes[i + off] = (c < 256 ? (byte) c : 63);
		}
	}

	// region Number conversion
	@Override
	public int checkInteger(LuaState state) throws LuaError, LuaUncatchableError {
		return (int) (long) checkDouble(state);
	}

	@Override
	public long checkLong(LuaState state) throws LuaError, LuaUncatchableError {
		return (long) checkDouble(state);
	}

	@Override
	public double checkDouble(LuaState state) throws LuaError, LuaUncatchableError {
		double d = scanNumber(10);
		if (Double.isNaN(d)) {
			throw ErrorFactory.argError(state, this, "number");
		}
		return d;
	}

	@Override
	public LuaNumber checkNumber(LuaState state) throws LuaError, LuaUncatchableError {
		return LuaDouble.valueOf(checkDouble(state));
	}

	@Override
	public LuaNumber checkNumber(LuaState state, String msg) throws LuaError, LuaUncatchableError {
		double d = scanNumber(10);
		if (Double.isNaN(d)) {
			throw new LuaError(msg, state.allocationTracker);
		}
		return LuaDouble.valueOf(d);
	}

	@Override
	public LuaValue toNumber() {
		return toNumber(10);
	}

	@Override
	public boolean isNumber() {
		double d = scanNumber(10);
		return !Double.isNaN(d);
	}

	@Override
	public double toDouble() {
		return scanNumber(10);
	}

	@Override
	public int toInteger() {
		return (int) (long) toDouble();
	}

	@Override
	public LuaValue toLuaString(LuaState state) {
		return this;
	}

	@Override
	public String checkString(LuaState state) throws LuaUncatchableError {
		return toJavaString(state.allocationTracker);
	}

	@Override
	public String checkString(LuaState state, String message) throws LuaUncatchableError {
		return toJavaString(state.allocationTracker);
	}

	@Override
	public LuaString checkLuaString(LuaState state) {
		return this;
	}

	/**
	 * convert to a number using a supplied base, or NIL if it can't be converted
	 *
	 * @param base the base to use, such as 10
	 * @return {@link LuaNumber} or {@link Constants#NIL} depending on the content of the string.
	 * @see LuaValue#toNumber()
	 */
	public LuaValue toNumber(int base) {
		double d = scanNumber(base);
		return Double.isNaN(d) ? NIL : LuaDouble.valueOf(d);
	}

	private double scanNumber(int base) {
		if (base < 2 || base > 36) return Double.NaN;
		return NumberParser.parse(contents, offset, length, base);
	}

	// endregion

}
