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
 * $Id: Generator.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.util;

import java.util.Random;

/**
 * A singleton generator used for generating random numbers. Must first be
 * initialized with a seed
 * 
 * @author Rob Jansen
 */
public class Generator {
	/**
	 * The singleton instance of Generator
	 */
	private static final Generator GENERATOR = new Generator();

	/**
	 * @return the singleton instance of Generator
	 */
	public static Generator getInstance() {
		return GENERATOR;
	}

	/**
	 * Flag tracking initialization of this generator.
	 */
	private boolean didInit = false;

	/**
	 * The underlying random instance used to generate random numbers.
	 */
	private Random prng;

	/**
	 * If this generator has not been initialized, it will be initialized here
	 * with a seed of 1. If a custom seed is desired, init() must be called
	 * before calling this method.
	 * 
	 * @return the prng
	 */
	public Random getPrng() {
		if (prng == null) {
			init(1);
		}
		return prng;
	}

	/**
	 * Creates the new random number generator using the given seed. If this
	 * generator has already been initialized, this method has no effect.
	 * 
	 * @param seed
	 */
	public void init(long seed) {
		if (didInit) {
			return;
		}
		prng = new Random(seed);
		didInit = true;
	}

	/**
	 * Private constructor for singleton class.
	 */
	private Generator() {
	}
}
