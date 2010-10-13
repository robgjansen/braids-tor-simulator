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
 * $Id: HybridProportionalDelayScheduler.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.scheduling;

import main.network.Buffer;
import main.network.Datagram;
import main.network.SchedulingRing;
import main.resource.Configuration;

/**
 * A scheduler that schedules the next datagram based on proportional average
 * delay of the buffer and waiting time of the head of each service class
 * buffer. The scheduler computes the priority of the head of each class,
 * following the proportional differentiation model of Drovolis, et al. The
 * configured delay differentiation parameters ddp are used for each class i,
 * and priority p for class i is computed as p_i = waiting_time_i / ddp_i for
 * wtp and p_i = delay_sent / total_sent / ddp for PAD. The highest priority
 * datagram is scheduled.
 * 
 * @author Rob Jansen
 */
public class HybridProportionalDelayScheduler extends Scheduler {

	/**
	 * Computes the waiting time of the head of the given buffer using the queue
	 * arrival timestamp of the datagram.
	 * 
	 * @param buffer
	 *            the buffer whose head we will compute priority
	 * @return the priority of the datagram at the head of the buffer
	 */
	private long computeNormalizedHeadWaitingTime(long time, Buffer buffer) {
		// priority(t) = waitTime(t)/DDP
		int ddp = 0;
		Datagram d = buffer.peekFirst();
		if (d == null) {
			return 0;
		}
		ddp = getDelayDifferentiationParameter(buffer.getPriority());
		return (time - d.getQueueArrivalTime()) / ddp;
	}

	/**
	 * Computes the proportional average delay of the given buffer using the
	 * queue arrival timestamp of the datagram and total number of packets sent
	 * from the buffer.
	 * 
	 * @param buffer
	 *            the buffer whose head we will compute priority
	 * @return the priority of the buffer
	 */
	private long computeNormalizedAverageDelay(Buffer buffer) {
		// priority(t) = (SumDelaySent/TotalSent)/ddp
		long delayMillis = (buffer.getHpdTotalSentPacketDelay() / buffer
				.getHpdTotalSentPackets())
				/ getDelayDifferentiationParameter(buffer.getPriority());
		// convert to nanos to match WTP
		return delayMillis * 1000000;
	}

	/**
	 * Retrieve the correct parameter from the configuration file.
	 * 
	 * @param priority
	 *            the class of service whose parameter we are requesting
	 * @return the parameter for the service class
	 */
	private int getDelayDifferentiationParameter(Priority priority) {
		switch (priority) {
		case NORMAL:
			return Configuration.NORMAL_DDP;
		case LOW_LATENCY:
			return Configuration.LOW_LATENCY_DDP;
		case HIGH_THROUGHPUT:
			return Configuration.HIGH_THROUGHPUT_DDP;
		default:
			return 1;
		}
	}

	/**
	 * Compute the WTP of each buffer in the given ring, and attempt to send
	 * data from the buffer with the highest priority.
	 * 
	 * @see main.scheduling.Scheduler#schedule(SchedulingRing)
	 */
	@Override
	public void schedule(long time, SchedulingRing ring) {
		if (ring.hasNoData()) {
			return;
		}

		// priority scheduler, based on configured service classes
		Buffer buffer, maxBuffer = null;
		long max = Long.MIN_VALUE;
		for (int i = 0; i < ring.size(); i++) {
			buffer = ring.advance();
			// nextbuffer should never be null, else we have problems
			if (!buffer.isEmpty()) {
				// WTP scheduling
				long wtpTemp = computeNormalizedHeadWaitingTime(time, buffer);
				// PAD scheduling
				long padTemp = computeNormalizedAverageDelay(buffer);
				// Hybrid mixture, using fractional hybrid parameter
				double percent = Configuration.HPD_FRACTION;
				long hpdTemp = (long) ((percent * padTemp) + ((1 - percent) * wtpTemp));
				if (hpdTemp > max) {
					max = hpdTemp;
					maxBuffer = buffer;
				}
			}
		}

		trySend(time, maxBuffer);
	}

}
