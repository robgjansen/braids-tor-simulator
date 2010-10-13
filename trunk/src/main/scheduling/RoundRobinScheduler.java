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
 * $Id: RoundRobinScheduler.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.scheduling;

import main.network.Buffer;
import main.network.SchedulingRing;

/**
 * Scheduler based on round robin, giving each buffer a fair number scheduling
 * turns.
 * 
 * @author Rob Jansen
 */
public class RoundRobinScheduler extends Scheduler {

	/**
	 * Send from the next non-empty buffer in the circular linked list,
	 * incrementing the pointer of last buffer that was scheduled.
	 * 
	 * @see main.scheduling.Scheduler#schedule(SchedulingRing)
	 */
	@Override
	public void schedule(long time, SchedulingRing ring) {
		if (ring.hasNoData()) {
			return;
		}

		// simple round robin scheduler, skipping buffers (circuits) that have
		// no data
		Buffer nextBuffer = null;
		for (int i = 0; i < ring.size(); i++) {
			nextBuffer = ring.advance();
			// nextbuffer should never be null, else we have problems
			if (!nextBuffer.isEmpty()) {
				break;
			}
		}

		trySend(time, nextBuffer);
	}

}
