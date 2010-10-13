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
 * $Id: WeightedFairQueueingScheduler.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.scheduling;

import main.network.Buffer;
import main.network.SchedulingRing;
import main.resource.Configuration;

/**
 * An extremely inefficient and VERY rough approximation of
 * weightedFairQueueing. This should not be used anywhere.
 * 
 * @author Rob Jansen
 * 
 */
public class WeightedFairQueueingScheduler extends Scheduler {

	private long lastUpdate;

	@Override
	public void schedule(long time, SchedulingRing ring) {
		if (ring.hasNoData()) {
			return;
		}

		if (time > lastUpdate + (Configuration.WFQ_INTERVAL * 1000000)) {
			updateDynamicWeights(ring);
			lastUpdate = time;
		}

		Buffer buffer, maxBuffer = null;
		double max = -1;
		long totalSent = ring.getWfqTotal();
		for (int i = 0; i < ring.size(); i++) {
			buffer = ring.advance();
			if(buffer.isEmpty()){
				continue;
			}
			double actual = 0.0;
			if(totalSent != 0){
				actual = buffer.getWfqWeight() * (buffer.getWfqSent() / totalSent);
			}
			if(actual > max){
				max = actual;
				maxBuffer = buffer;
			}
		}
		
		if(maxBuffer != null && !maxBuffer.isEmpty()){
			maxBuffer.setWfqSent(maxBuffer.getWfqSent() + 1);
			ring.setWfqTotal(ring.getWfqTotal() + 1);
			trySend(time, maxBuffer);
		}
	}

	private void updateDynamicWeights(SchedulingRing ring) {
		long totalSent = ring.getWfqTotal();
		Buffer buffer;
		double expectedSent = 1.0 / ring.size();
		for (int i = 0; i < ring.size(); i++) {
			double actualSent = 0.0;
			buffer = ring.advance();
			if(totalSent != 0){
				actualSent = buffer.getWfqSent() / totalSent;
			}
			if (actualSent > expectedSent) {
				buffer.setWfqWeight(0.1 * expectedSent);
			} else {
				buffer.setWfqWeight(2.0 * expectedSent);
			}
			buffer.setWfqSent(0);
		}
		ring.setWfqTotal(0);

	}

}
