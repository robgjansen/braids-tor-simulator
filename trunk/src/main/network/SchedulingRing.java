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
 * $Id: SchedulingRing.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.network;

import main.system.Driver;
import main.util.CircularSinglyLinkedList;

/**
 * A CircularLinkedList of buffers that tracks the total number of datagrams in
 * all buffers in this ring. This class is used to store a list of buffers to
 * include in scheduling decisions by a scheduler.
 * 
 * @author Rob Jansen
 */
public class SchedulingRing extends CircularSinglyLinkedList<Buffer> {
	/**
	 * Total number of datagrams in all buffers in this ring
	 */
	private int dataCount;
	
	private long wfqTotal;

	/**
	 * Create a new, empty ring
	 */
	public SchedulingRing() {
		dataCount = 0;
		wfqTotal = 0;
	}

	/**
	 * Increments the data count of this ring by the size of the given buffer.
	 * 
	 * @see main.util.CircularSinglyLinkedList#add(main.util.Identifiable)
	 */
	@Override
	public void add(Buffer b) {
		super.add(b);
		dataCount += b.getSize();
	}

	/**
	 * Increments the data count of this ring by the size of the given buffer.
	 * 
	 * @see main.util.CircularSinglyLinkedList#addInOrder(main.util.Identifiable)
	 */
	@Override
	public void addInOrder(Buffer b) {
		super.addInOrder(b);
		dataCount += b.getSize();
	}

	/**
	 * Increments the data count of this ring by the given amount. If
	 * subtraction is desired, use a negative amount.
	 * 
	 * @param amount
	 *            the amount to add to the data count
	 */
	public void changedDataCount(int amount) {
		dataCount += amount;
	}

	/**
	 * @return true if the data count of this ring is <= 0, false otherwise
	 */
	public boolean hasNoData() {
		return dataCount <= 0;
	}

	/**
	 * Removes the given buffer. Decrements the data count of this ring by the
	 * size of the given buffer.
	 * 
	 * @see main.util.CircularSinglyLinkedList#remove(main.util.Identifiable)
	 */
	@Override
	public void remove(Buffer b) {
		super.remove(b);
		dataCount -= b.getSize();
		if (dataCount < 0) {
			Driver.log.severe("BufferRing size < 0");
		}
	}

	/**
	 * @return the wfqTotal
	 */
	public long getWfqTotal() {
		return wfqTotal;
	}

	/**
	 * @param wfqTotal the wfqTotal to set
	 */
	public void setWfqTotal(long wfqTotal) {
		this.wfqTotal = wfqTotal;
	}

}
