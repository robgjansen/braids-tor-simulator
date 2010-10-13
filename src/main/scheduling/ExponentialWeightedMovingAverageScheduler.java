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
 * $Id: ExponentialWeightedMovingAverageScheduler.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.scheduling;

import main.network.Buffer;
import main.network.SchedulingRing;
import main.resource.Configuration;

/**
 * The following description taken from Tor source code:
 * https://gitweb.torproject.org//tor.git?a=blobdiff
 * ;f=src/or/relay.c;h=19bb6871ce81bd02a9cbaaa56436e2e5ce258569
 * ;hp=9b7396afac60346c937c2c336b8d7f66f14ef9a9
 * ;hb=06e8370c33d6ccb73d55e9e8c3d2673c48d7b328
 * ;hpb=c43fee131d306507937733c7ddc45a040dd2d27c
 * 
 * When choosing which cells to relay first, we favor circuits that have been
 * quiet recently. This gives better latency on connections that aren't pushing
 * lots of data, and makes the network feel more interactive.
 * 
 * Conceptually, we take an exponentially weighted mean average of the number of
 * cells a circuit has sent, and allow active circuits (those with cells to
 * relay) to send cells in order of their exponentially-weighted mean average
 * (EWMA) cell count. [That is, a cell sent N seconds ago 'counts' F^N times as
 * much as a cell sent now, for 0<F<1.0.]
 * 
 * If 'double' had infinite precision, we could do this simply by counting a
 * cell sent at startup as having weight 1.0, and a cell sent N seconds later as
 * having weight F^-N. This way, we would never need to re-scale any
 * already-sent cells.
 * 
 * To prevent double from overflowing, we could count a cell sent now as having
 * weight 1.0 and a cell sent N seconds ago as having weight F^N. This, however,
 * would mean we'd need to re-scale *ALL* old circuits every time we wanted to
 * send a cell.
 * 
 * So as a compromise, we divide time into 'ticks' (currently, 10-second
 * increments) and say that a cell sent at the start of a current tick is worth
 * 1.0, a cell sent N seconds before the start of the current tick is worth F^N,
 * and a cell sent N seconds after the start of the current tick is worth F^-N.
 * This way we don't overflow, and we don't need to constantly rescale.
 * 
 * @author Rob Jansen
 * 
 */
public class ExponentialWeightedMovingAverageScheduler extends Scheduler {

	@Override
	public void schedule(long time, SchedulingRing ring) {
		if (ring.hasNoData()) {
			return;
		}

		long millis = time/1000000;
		long currentInterval = millis / Configuration.EWMA_INTERVAL;

		// waiting time priority scheduler, based on configured service classes
		Buffer buffer, minBuffer = null;
		double min = Integer.MAX_VALUE;
		for (int i = 0; i < ring.size(); i++) {
			buffer = ring.advance();
			// nextbuffer should never be null, else we have problems
			if (!buffer.isEmpty()) {
				// check if we need to decay the EMWA
				if (currentInterval > buffer.getEwmaLastAdjustedInterval()) {
					// we need to decay the ewma counter exponentially based
					// on when it was last adjusted
					long scalePow = currentInterval
							- buffer.getEwmaLastAdjustedInterval();
					buffer.setEwma(buffer.getEwma()
							* Math.pow(Configuration.EWMA_SCALE_FACTOR, scalePow));
					buffer.setEwmaLastAdjustedInterval(currentInterval);
				}

				double temp = buffer.getEwma();
				if (temp < min) {
					min = temp;
					minBuffer = buffer;
				}
			}
		}

		if (minBuffer != null) {
			// need to scale its counter since we are sending a cell
			double scaleRemainder = millis % Configuration.EWMA_INTERVAL;
			double scaleFraction = 0 - (scaleRemainder/Configuration.EWMA_INTERVAL);
			double scaleIncrement = Math.pow(Configuration.EWMA_SCALE_FACTOR, scaleFraction);
			minBuffer.setEwma(minBuffer.getEwma() + scaleIncrement);
		}

		trySend(time, minBuffer);
	}
}
