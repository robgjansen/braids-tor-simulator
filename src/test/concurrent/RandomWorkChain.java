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
 * $Id: RandomWorkChain.java 948 2010-10-12 03:33:57Z jansen $
 */
package test.concurrent;

import java.util.Random;

import main.concurrent.Master;
import main.event.Event;
import main.node.Node;

public class RandomWorkChain extends Event {
	private Master master;
	private Random prng;

	public RandomWorkChain(long runTime, Master master, Random prng) {
		super(runTime);
		this.master = master;
		this.prng = prng;
	}

	@Override
	public void run() {
		// get random delay in millis
		int milliDelay = prng.nextInt(200);
		System.out.println("time: " + getTime() + " random delay: " + milliDelay + " ms " + master.getEstimatedSize() + " queued events");
		long nanoTime = getTime() + (milliDelay * 1000000);
		master.addWork(new RandomWorkChain(nanoTime, master, prng));
	}

	@Override
	public Node getOwner() {
		return null;
	}

}
