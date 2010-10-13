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
 * $Id: Worker.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.concurrent;

import java.util.concurrent.Semaphore;

public class Worker implements Runnable {
	private Master master;
	private Semaphore workPermits;
	private boolean isFired;

	public Worker(Master master, Semaphore workPermits) {
		this.master = master;
		this.workPermits = workPermits;
	}

	@Override
	public void run() {
		try {
			// workers keep working until fired
			while (true) {
				workPermits.acquire();
				if (isFired) {
					break;
				}
				master.getWork().run();
				master.workDone();
			}
		} catch (InterruptedException e) {
			// The thread was interrupted and will now exit
		}
	}

	protected void fire() {
		isFired = true;
	}

}
