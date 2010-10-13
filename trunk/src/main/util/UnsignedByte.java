/**
 * Copyright 2010 Rob Jansen
 * 
 * This file is part of braids-tor-simulator.
 * 
 * braids-tor-simulator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * braids-tor-simulator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with braids-tor-simulator.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * $Id: UnsignedByte.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.util;

/**
 * An object of type {@code UnsignedByte} contains a single field whose type is
 * {@code byte}.
 * 
 * @author Rob Jansen
 */
public final class UnsignedByte implements Comparable<UnsignedByte> {

	/**
	 * A constant holding the maximum value a {@code UnsignedByte} can have,
	 * 2<sup>8</sup>-1.
	 */
	public static final int MAX_VALUE = 255;

	/**
	 * A constant holding the minimum value a {@code UnsignedByte} can have, 0.
	 */
	public static final int MIN_VALUE = 0;

	private static final long serialVersionUID = -7925048919912984591L;

	/**
	 * The number of bits used to represent a {@code UnsignedByte} value in
	 * binary form.
	 */
	public static final int SIZE = 8;

	/**
	 * Parse the string value as an unsigned byte, and return the byte value.
	 * 
	 * @param value
	 *            decimal string to parse, in base 10
	 * @return byte whose bits are set as the parsed unsigned byte
	 */
	public static final byte parseUnsignedByte(String value) {
		return parseUnsignedByte(value, 10);
	}

	/**
	 * Parse the string value in the given radix as an unsigned byte, and return
	 * the byte value.
	 * 
	 * @param value
	 *            string to parse, in the given radix
	 * @param radix
	 *            the radix to use when parsing
	 * @return byte whose bits are set as the parsed unsigned byte
	 */
	public static final byte parseUnsignedByte(String value, int radix) {
		UnsignedByte b = new UnsignedByte(Integer.parseInt(value, radix));
		return b.byteValue();
	}

	/**
	 * Creates a new UnsignedByte from the value of the given string in base 10.
	 * 
	 * @param value
	 *            of decimal to parse, in base 10
	 * @return a new UnsignedByte whose value is the parsed value
	 */
	public static final UnsignedByte valueOf(String value) {
		return valueOf(value, 10);
	}

	/**
	 * Creates a new UnsignedByte from the value of the given string in the
	 * given radix.
	 * 
	 * @param value
	 *            string to parse, in the given radix.
	 * @param radix
	 *            the radix to use when parsing
	 * @return a new UnsignedByte whose value is the parsed value
	 */
	public static final UnsignedByte valueOf(String value, int radix) {
		return new UnsignedByte(Integer.parseInt(value, radix));
	}

	/**
	 * The value of the {@code UnsignedByte}.
	 */
	private final byte value;

	public UnsignedByte() {
		value = 0;
	}

	public UnsignedByte(byte value) {
		if (value < MIN_VALUE) {
			throw new RuntimeException(
					"UnsignedByte constructor: passed byte value out of range.");
		}
		this.value = value;
	}

	public UnsignedByte(int value) {
		if ((value < MIN_VALUE) || (value > MAX_VALUE)) {
			throw new RuntimeException(
					"UnsignedByte constructor: passed int value out of range.");
		}
		this.value = (byte) value;
	}

	/**
	 * Returns the primitive underlying signed {@code byte} value of this
	 * {@code UnsignedByte}.
	 */
	public byte byteValue() {
		return value;
	}

	/**
	 * Compares two {@code UnsignedByte} objects numerically.
	 * 
	 * @param anotherByte
	 *            the {@code UnsignedByte} to be compared.
	 * @return the value {@code 0} if this {@code UnsignedByte} is equal to the
	 *         argument {@code UnsignedByte}; a value less than {@code 0} if
	 *         this {@code UnsignedByte} is numerically less than the argument
	 *         {@code UnsignedByte}; and a value greater than {@code 0} if this
	 *         {@code UnsignedByte} is numerically greater than the argument
	 *         {@code UnsignedByte} (unsigned comparison).
	 */
	public int compareTo(UnsignedByte anotherByte) {
		return intValue() - anotherByte.intValue();
	}

	/**
	 * Compares this object to the specified object. The result is {@code true}
	 * if and only if the argument is not {@code null} and is a {@code
	 * UnsignedByte} object that contains the same {@code byte} value as this
	 * object.
	 * 
	 * @param obj
	 *            the object to compare with
	 * @return {@code true} if the objects are the same; {@code false}
	 *         otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof UnsignedByte) {
			return value == ((UnsignedByte) obj).byteValue();
		}
		return false;
	}

	/**
	 * Returns the value of this {@code UnsignedByte} as an {@code int} after
	 * unsigned conversion.
	 */
	public int intValue() {
		return value < 0 ? MAX_VALUE + value + 1 : value;
	}

	/**
	 * Checks if the given bit is set.
	 * 
	 * @param bit
	 *            the bit index (0-7) that we wish to check - index - 0 contains
	 *            the most significant bit and index 7 contains the least
	 *            significant bit.
	 * @return true in case the bit is set, false otherwise
	 * @throws Exception
	 */
	public Boolean isBitSet(int bit) throws Exception {
		if ((bit < 0) || (bit > 7)) {
			throw new Exception("bit index must be between 0 and 7");
		}
		return (value & 1 << 7 - bit) != 0;
	}

	/**
	 * Returns a {@code String} object representing this {@code Byte}'s value.
	 * The value is converted to signed decimal representation and returned as a
	 * string, exactly as if the {@code byte} value were given as an argument to
	 * the {@link java.lang.Byte#toString(byte)} method.
	 * 
	 * @return a string representation of the value of this object in
	 *         base&nbsp;10.
	 */
	public String toBitString() {
		String s = "";
		for (int i = 0; i < 8; i++) {
			try {
				if (isBitSet(i)) {
					s += "1";
				} else {
					s += "0";
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return s;
	}

	/**
	 * Returns a {@code String} object representing this {@code UnsignedByte}'s
	 * value. The value is converted to unsigned decimal representation and
	 * returned as a string.
	 * 
	 * @return a string representation of the unsigned value of this object in
	 *         base&nbsp;10.
	 */
	@Override
	public String toString() {
		return String.valueOf(intValue());
	}

}