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
 * $Id: FirstComeFirstServedScheduler.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.scheduling;

import main.network.Buffer;
import main.network.SchedulingRing;

/**
 * Scheduler based on the minimum waiting time, i.e., the datagram that arrived
 * first.
 * 
 * @author Rob Jansen
 */
public class FirstComeFirstServedScheduler extends Scheduler {

	/**
	 * Find the buffer containing the head with the minimum waiting time, and
	 * attempt to send from that buffer.
	 * 
	 * @see main.scheduling.Scheduler#schedule(SchedulingRing)
	 */
	@Override
	public void schedule(long time, SchedulingRing ring) {
		// simple fcfs scheduler, minimum of waiting time came first
		Buffer buffer, firstBuffer = null;
		long minTime = Long.MAX_VALUE;
		for (int i = 0; i < ring.size(); i++) {
			buffer = ring.advance();
			// nextbuffer should never be null, else we have problems
			if (!buffer.isEmpty()) {
				long temp = buffer.peekFirst().getQueueArrivalTime();
				if (temp < minTime) {
					minTime = temp;
					firstBuffer = buffer;
				}
			}
		}

		trySend(time, firstBuffer);
	}

}
