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
 * $Id: TestUnsignedByte.java 948 2010-10-12 03:33:57Z jansen $
 */
package test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import main.util.UnsignedByte;

import org.junit.Test;

/**
 * @author Rob Jansen
 */
public class TestUnsignedByte {

	/**
	 * Test method for
	 * {@link main.util.UnsignedByte#compareTo(main.util.UnsignedByte)}.
	 */
	@Test
	public void testCompareTo() {
		UnsignedByte ub1 = new UnsignedByte(85);
		UnsignedByte ub2 = new UnsignedByte(170);
		UnsignedByte ub3 = new UnsignedByte(170);
		assertTrue(ub1.compareTo(ub2) < 0);
		assertTrue(ub2.compareTo(ub3) == 0);
		assertTrue(ub2.compareTo(ub1) > 0);
	}

	/**
	 * Test method for {@link main.util.UnsignedByte#equals(java.lang.Object)}.
	 */
	@Test
	public void testEqualsObject() {
		UnsignedByte ub1 = new UnsignedByte(85);
		UnsignedByte ub2 = new UnsignedByte(85);
		UnsignedByte ub3 = new UnsignedByte(170);
		assertTrue(ub1.equals(ub2));
		assertTrue(ub2.equals(ub1));
		assertFalse(ub1.equals(ub3));
		assertFalse(ub3.equals(ub1));
	}

	/**
	 * Test method for {@link main.util.UnsignedByte#isBitSet(int)}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testIsBitSet() throws Exception {
		int i = 0;
		UnsignedByte ub = new UnsignedByte(i);
		for (int j = 0; j < 8; j++) {
			assertFalse(ub.isBitSet(j));
		}

		i = 255;
		ub = new UnsignedByte(i);
		for (int j = 0; j < 8; j++) {
			assertTrue(ub.isBitSet(j));
		}

		i = 85;
		ub = new UnsignedByte(i);
		for (int j = 0; j < 7; j += 2) {
			assertFalse(ub.isBitSet(j));
			assertTrue(ub.isBitSet(j + 1));
		}

		i = 170;
		ub = new UnsignedByte(i);
		for (int j = 0; j < 7; j += 2) {
			assertTrue(ub.isBitSet(j));
			assertFalse(ub.isBitSet(j + 1));
		}
	}

	/**
	 * Test method for
	 * {@link main.util.UnsignedByte#parseUnsignedByte(java.lang.String)}.
	 */
	@Test
	public void testParseUnsignedByteString() {
		int i = 170;
		String sInt = String.valueOf(i);
		UnsignedByte ub = new UnsignedByte(i);
		byte b = UnsignedByte.parseUnsignedByte(sInt);

		assertEquals(ub.byteValue(), b);
	}

	/**
	 * Test method for
	 * {@link main.util.UnsignedByte#parseUnsignedByte(java.lang.String, int)}.
	 */
	@Test
	public void testParseUnsignedByteStringInt() {
		int i = 170;
		String sBit = "10101010";
		UnsignedByte ub = new UnsignedByte(i);
		byte b = UnsignedByte.parseUnsignedByte(sBit, 2);

		assertEquals(ub.byteValue(), b);
	}

	/**
	 * Test method for {@link main.util.UnsignedByte#toBitString()}.
	 */
	@Test
	public void testToBitString() {
		int i = 0;
		String s = "00000000";
		UnsignedByte ub = new UnsignedByte(i);
		assertEquals(s, ub.toBitString());

		i = 1;
		s = "00000001";
		ub = new UnsignedByte(i);
		assertEquals(s, ub.toBitString());

		i = 33;
		s = "00100001";
		ub = new UnsignedByte(i);
		assertEquals(s, ub.toBitString());

		i = 85;
		s = "01010101";
		ub = new UnsignedByte(i);
		assertEquals(s, ub.toBitString());

		i = 170;
		s = "10101010";
		ub = new UnsignedByte(i);
		assertEquals(s, ub.toBitString());

		i = 255;
		s = "11111111";
		ub = new UnsignedByte(i);
		assertEquals(s, ub.toBitString());
	}

	/**
	 * Test method for {@link main.util.UnsignedByte#toString()}.
	 */
	@Test
	public void testToString() {
		int i = 170;
		UnsignedByte ub = new UnsignedByte(i);
		assertEquals(String.valueOf(i), ub.toString());
	}

	/**
	 * Test method for {@link main.util.UnsignedByte#UnsignedByte()}.
	 */
	@Test
	public void testUnsignedByte() {
		byte b = 0;
		UnsignedByte ub = new UnsignedByte();
		assertEquals(b, ub.byteValue());
	}

	/**
	 * Test method for {@link main.util.UnsignedByte#UnsignedByte(byte)}.
	 */
	@Test
	public void testUnsignedByteByte() {
		byte b = 83;
		UnsignedByte ub = new UnsignedByte(b);
		assertEquals(b, ub.byteValue());
	}

	/**
	 * Test method for {@link main.util.UnsignedByte#UnsignedByte(int)}.
	 */
	@Test
	public void testUnsignedByteInt() {
		int i = 85;
		byte b = Byte.parseByte("01010101", 2);
		UnsignedByte ub = new UnsignedByte(i);
		assertEquals(i, ub.intValue());

		i = 170;
		b = UnsignedByte.valueOf("10101010", 2).byteValue();
		ub = new UnsignedByte(i);
		assertEquals(i, ub.intValue());
		assertEquals(b, ub.byteValue());

		i = 255;
		b = ~0;
		ub = new UnsignedByte(255);
		assertEquals(i, ub.intValue());
		assertEquals(b, ub.byteValue());
	}

	/**
	 * Test method for {@link main.util.UnsignedByte#valueOf(java.lang.String)}.
	 */
	@Test
	public void testValueOfString() {
		int i = 170;
		String sBit = "10101010";
		String sInt = String.valueOf(i);
		UnsignedByte ub1 = new UnsignedByte(i);
		UnsignedByte ub2 = UnsignedByte.valueOf(sInt);

		assertEquals(sBit, ub2.toBitString());
		assertEquals(sInt, ub2.toString());
		assertEquals(i, ub2.intValue());
		assertEquals(ub1.byteValue(), ub2.byteValue());
	}

	/**
	 * Test method for
	 * {@link main.util.UnsignedByte#valueOf(java.lang.String, int)}.
	 */
	@Test
	public void testValueOfStringInt() {
		int i = 170;
		String sBit = "10101010";
		String sInt = String.valueOf(i);
		UnsignedByte ub1 = new UnsignedByte(i);
		UnsignedByte ub2 = UnsignedByte.valueOf(sBit, 2);

		assertEquals(sBit, ub2.toBitString());
		assertEquals(sInt, ub2.toString());
		assertEquals(i, ub2.intValue());
		assertEquals(ub1.byteValue(), ub2.byteValue());
	}

}
