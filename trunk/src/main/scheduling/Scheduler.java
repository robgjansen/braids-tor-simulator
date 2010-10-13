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
 * $Id: Scheduler.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.scheduling;

import main.network.Buffer;
import main.network.SchedulingRing;

/**
 * The base scheduling class for any scheduling algorithm. Defines the
 * priorities an scheduling algorithms, and a method that attemps to send the
 * result of a scheduling decision.
 * 
 * @author Rob Jansen
 */
public abstract class Scheduler {
	/**
	 * Defines possible priority classes for each datagram.
	 * 
	 * @author Rob Jansen
	 */
	public enum Priority {
		HIGH_THROUGHPUT, LOW_LATENCY, NORMAL;
	}

	/**
	 * Defines implemented scheduling algorithms for the simulator.
	 * 
	 * @author Rob Jansen
	 */
	public enum SchedulingAlgorithm {
		FIRST_COME_FIRST_SERVED, ROUND_ROBIN, HYBRID_PROPORTIONAL_DELAY, EXPONENTIAL_WEIGHTED_MOVING_AVERAGE, WEIGHTED_FAIR_QUEUEING;
	}

	/**
	 * Schedule the next datagram for the given SchedulingRing. Scheduling
	 * decisions are made based on the given circular linked list of buffers.
	 * 
	 * @param ring
	 *            the ring of buffers to schedule
	 */
	public abstract void schedule(long time, SchedulingRing ring);

	/**
	 * Attempt to send the next datagram in the given buffer. Nothing is sent if
	 * the buffer is empty of null. The node attached to the buffer will send
	 * out the dequeued data on its network.
	 * 
	 * @param buffer
	 *            the buffer to dequeue
	 */
	protected void trySend(long time, Buffer buffer) {
		// if we found a buffer with data, tell the network to send
		if ((buffer != null) && !buffer.isEmpty()) {
			buffer.getNetwork().send(time, buffer.dequeue(time));
		}
	}
}
