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
 * $Id: Pair.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.util;

/**
 * This class holds a pair of any object. Objects are stored as X and Y.
 * 
 * @author Rob Jansen
 */
public class Pair<T> {

	/**
	 * The first object in the pair
	 */
	private T x;
	/**
	 * The second object in the pair
	 */
	private T y;

	/**
	 * Creates a pair of the chosen type with the given values, setting x in the
	 * x (first) part and y in the y (second) part.
	 */
	public Pair(T x, T y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * @return the x part (first) of the pair
	 */
	public T getX() {
		return x;
	}

	/**
	 * @return the y part (second) of the pair
	 */
	public T getY() {
		return y;
	}

	/**
	 * @param x
	 *            the x (first) part of the pair to set
	 */
	public void setX(T x) {
		this.x = x;
	}

	/**
	 * @param y
	 *            the y (second) part of the pair to set
	 */
	public void setY(T y) {
		this.y = y;
	}

	/**
	 * Wraps the toString of the x and y parts in parenthesis and separated by a
	 * comma, and returns the resulting string.
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "(" + this.x.toString() + ", " + this.y.toString() + ")";
	}

}
