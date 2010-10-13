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
 * $Id: ConcurrentTest.java 948 2010-10-12 03:33:57Z jansen $
 */
package test.concurrent;

import java.util.Random;

import main.concurrent.Master;
import main.util.SimulationClock;

public class ConcurrentTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SimulationClock.getInstance().setEndTime(10);
		Master testMaster = new Master(2, 100000000);
		Random prng = new Random(324528439);
		for(int i = 0; i < 1000000; i++){
			testMaster.addWork(new RandomWorkChain(prng.nextInt(100), testMaster, prng));
		}
		System.out.println("Starting test");
		testMaster.run();
		System.out.println("Finished test");
		System.exit(0);
	}

}
