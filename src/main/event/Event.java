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
 * $Id: Event.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.event;

import main.node.Node;

/**
 * Represents events in the discrete event simulation. All events must inherit
 * from this class and define a time to execute. Events are comparable and are
 * ordered by the time to execute.
 * 
 * @author Rob Jansen
 */
public abstract class Event implements Comparable<Event>, Runnable {

	/**
	 * Absolute simulation time to execute this event in nanoseconds
	 */
	private long time;

	/**
	 * Create a new event after the given delay.
	 * 
	 * @param timeDelay
	 *            the amount of simulation time from now to execute the event,
	 *            in nanoseconds.
	 */
	public Event(long runTime) {
		super();
		time = runTime;
	}

	/**
	 * Establishes an order among events for the priority queue. This method
	 * should NOT be overridden in child classes
	 * 
	 * @param e
	 *            event to compare this one to
	 * 
	 * @return +, 0, - if the time of this event is respectfully greater than,
	 *         equal, or less than that of e as per Comparable<T> requires.
	 */
	public int compareTo(Event e) {
		long result = time - e.time;
		if (result > 0) {
			return 1;
		} else if (result < 0) {
			return -1;
		} else {
			return 0;
		}
	}

	/**
	 * Call-back method that all subclasses need implement. This method will be
	 * called when this event emerges from the event queue.
	 */
	public abstract void run();
	
	/**
	 * @return the node this event will most significantly affect
	 */
	public abstract Node getOwner();

	/**
	 * @return the absolute simulation time to execute this event, in
	 *         nanoseconds.
	 */
	public long getTime() {
		return time;
	}

}