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
 * $Id: Ncpus.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.util;

/**
 * Simple class to make a system call and print the number of available
 * processor on the current physical machine.
 * 
 * @author Rob Jansen
 */
public class Ncpus {

	/**
	 * Prints the result of Runtime.getRuntime().availableProcessors() and
	 * exits.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("This machine has "
				+ Runtime.getRuntime().availableProcessors() + " processors");
	}

}
