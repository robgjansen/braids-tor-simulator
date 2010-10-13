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
 * $Id: SimulationClock.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.util;

import main.system.Driver;

/**
 * The Singleton clock for tracking simulation time.
 * 
 * @author Rob Jansen
 */
public class SimulationClock {

	/**
	 * The singleton instance of the simulation clock
	 */
	private static final SimulationClock CLOCK = new SimulationClock();

	/**
	 * Retrieves the singleton instance of the simulation clock.
	 * 
	 * @return the instance of this SimulationClock
	 */
	public static SimulationClock getInstance() {
		return CLOCK;
	}

	public long getEndTime() {
		return endTime;
	}

	/**
	 * The current time displayed on the clock in nanoseconds.
	 */
	private long currentTime;

	/**
	 * The end time of the simulation in nanoseconds.
	 */
	private long endTime;

	/**
	 * Starts the simulation clock with default start and end time set to 0. The
	 * end time must be set before updating time or an error will be thrown.
	 */
	private SimulationClock() {
		currentTime = 0;
		endTime = 0;
	}

	/**
	 * @return the number of nanoseconds in one millisecond
	 */
	public long getOneMillisecond() {
		return (long) (1000000);
	}

	/**
	 * @return the number of nanoseconds in one minute
	 */
	public long getOneMinute() {
		return 60 * (long) (1000000000);
	}

	/**
	 * @return the number of nanoseconds in one second
	 */
	public long getOneSecond() {
		return (long) (1000000000);
	}

	/**
	 * Retrieves the current time.
	 * 
	 * @return The current simulation time in minutes.
	 */
	public long getTimeAsMinutes() {
		return currentTime / ((long) (1000000000) * 60);
	}
	
	/**
	 * Retrieves the current time.
	 * 
	 * @return The current simulation time in nanoseconds.
	 */
	public long getTimeAsNanoseconds() {
		return currentTime;
	}

	/**
	 * Returns the expiration status of the simulation time.
	 * 
	 * @return true if the simulation has finished, false otherwise.
	 */
	public boolean isExpired() {
		return currentTime >= endTime;
	}

	/**
	 * Sets the end time of the simulation.
	 * 
	 * @param time
	 *            The time in minutes to end the simulation.
	 */
	public void setEndTime(int time) {
		endTime = (long) (time * 60.0 * (long) (1000000000));
	}

	/**
	 * Winds the clock forward the given amount of time.
	 * 
	 * @param time
	 *            The amount in nanoseconds to increment the clock time. This
	 *            should not be the requested clock time, but the amount to be
	 *            added to the current time.
	 */
	public void wind(long time) {
		if (endTime <= 0) {
			Driver.log.severe("Please set end time before running simulation.");
		}
		if (time < 0) {
			Driver.log.severe("May not wind clock in reverse.");
		}
		currentTime += time;
	}
	
	public void set(long time) {
		wind(time-currentTime);
	}
}
