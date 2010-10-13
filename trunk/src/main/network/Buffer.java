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
 * $Id: Buffer.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.network;

import java.util.LinkedList;

import main.node.Node;
import main.resource.Configuration;
import main.scheduling.Scheduler.Priority;
import main.scheduling.Scheduler.SchedulingAlgorithm;
import main.system.Driver;
import main.util.Identifiable;

/**
 * A buffer that holds data for a node. For the RoundRobin scheduler, Buffers
 * can be configured to be dynamic. Dynamic buffers add themselves to their
 * assigned Scheduling ring when they become non-empty, and remove themselves
 * when they become empty. To make sure the relative order of buffers in the
 * scheduling ring is preserved among dynamic adding and removing, buffers
 * extend Identifiable (which means they have an ID), and their ID is used to
 * order them in the scheduling ring. The addInOrder method is used when a
 * dynamic buffer adds itself to the ring.
 * 
 * @see main.resource.Configuration#NETWORK_DYNAMIC_BUFFERS
 * 
 * @author Rob Jansen
 */
public class Buffer extends Identifiable {
	/**
	 * Used to distribute IDs to buffers, since buffers need to be ordered
	 */
	private static int BUFFER_ID_COUNTER = 0;

	/**
	 * The main structure to actually store the data
	 */
	private LinkedList<Datagram> data;
	/**
	 * This buffer's ID
	 */
	private int id;
	/**
	 * The node to which this buffer belongs
	 */
	private Network network;
	/**
	 * The priority of data stored in this buffer
	 */
	private Priority priority;
	/**
	 * The SchedulingRing this buffer is assigned to
	 */
	private SchedulingRing ring;

	/**
	 * Current counter for exponential weighted moving average
	 */
	private double ewma;

	/**
	 * Last interval exponential weighted moving average was computed
	 */
	private long ewmaLastAdjustedInterval;

	/**
	 * Used in HPD scheduling - the sum of delay for packets sent from this
	 * buffer, in milliseconds
	 */
	private long hpdTotalSentPacketDelay;

	/**
	 * Used in HPD scheduling - the sum of packets sent
	 */
	private long hpdTotalSentPackets;

	private long wfqSent;
	private double wfqWeight;

	/**
	 * Create a new buffer, assigning itself the next ID in the counter and
	 * creating the underlying linked list that stores the data.
	 * 
	 * @param p
	 *            the priority of data stored in this buffer
	 * @param node
	 *            the node that owns the buffer
	 * @param ring
	 *            the SchedulingRing assigned to this buffer
	 */
	public Buffer(Priority p, Node node, SchedulingRing ring) {
		super();
		id = BUFFER_ID_COUNTER++;
		this.network = node.getNetwork();
		priority = p;
		this.ring = ring;
		data = new LinkedList<Datagram>();
		ewma = 0;
		ewmaLastAdjustedInterval = 0;
		hpdTotalSentPacketDelay = 0;
		// avoid division by 0 error
		hpdTotalSentPackets = 1;
		wfqWeight = 1.0;
		wfqSent = 0;
	}

	/**
	 * Removes the data at the front of the queue. Updates the SchedulingRing
	 * size. If dynamic and the buffer is empty after the dequeue, it will be
	 * removed from the ring.
	 * 
	 * @return the datagram that was dequeued
	 */
	public Datagram dequeue(long time) {
		Datagram d = data.removeFirst();

		// the ring lost an item
		ring.changedDataCount(-1);
		// do we need to remove this buffer from the ring
		if ((Configuration.SCHEDULER == SchedulingAlgorithm.ROUND_ROBIN
				|| Configuration.SCHEDULER == SchedulingAlgorithm.EXPONENTIAL_WEIGHTED_MOVING_AVERAGE || (Configuration.SCHEDULER == SchedulingAlgorithm.HYBRID_PROPORTIONAL_DELAY && !Configuration.NETWORK_PRIORITY))
				&& Configuration.NETWORK_DYNAMIC_BUFFERS && data.isEmpty()) {
			// remove the will-be empty buffer from the ring
			ring.remove(this);
		}

		if (Configuration.SCHEDULER == SchedulingAlgorithm.HYBRID_PROPORTIONAL_DELAY) {
			hpdTotalSentPacketDelay += time / 1000000;
			hpdTotalSentPackets += 1;
			if (hpdTotalSentPacketDelay < 0) {
				Driver.log.warning("HPD total packet delay overflowed long!");
			}
		}

		return d;
	}

	/**
	 * Adds the data to the end of the queue. Updates the SchedulingRing size.
	 * If dynamic and the buffer was empty before the enqueue, it will be added
	 * to the ring.
	 * 
	 * @param d
	 *            the datagram enqueued
	 */
	public void enqueue(long time, Datagram d) {
		d.setQueueArrivalTime(time);

		// do we need to add this buffer to the ring
		if ((Configuration.SCHEDULER == SchedulingAlgorithm.ROUND_ROBIN
				|| Configuration.SCHEDULER == SchedulingAlgorithm.EXPONENTIAL_WEIGHTED_MOVING_AVERAGE || (Configuration.SCHEDULER == SchedulingAlgorithm.HYBRID_PROPORTIONAL_DELAY && !Configuration.NETWORK_PRIORITY))
				&& Configuration.NETWORK_DYNAMIC_BUFFERS && data.isEmpty()) {
			// add this to the ring dynamically for quicker scheduling
			// decisions
			ring.addInOrder(this);
		}
		// the ring will gain an item
		ring.changedDataCount(1);

		data.addLast(d);
	}

	/**
	 * @return the id
	 */
	@Override
	public int getId() {
		return id;
	}

	/**
	 * @return the node this buffer belongs to
	 */
	public Network getNetwork() {
		return network;
	}

	/**
	 * @return the priority
	 */
	public Priority getPriority() {
		return priority;
	}

	/**
	 * @return the number of datagrams in the buffer
	 */
	public int getSize() {
		return data.size();
	}

	/**
	 * @return true if the buffer is empty, false otherwise
	 */
	public boolean isEmpty() {
		return data.isEmpty();
	}

	/**
	 * @return the head of the queue, without removing it
	 */
	public Datagram peekFirst() {
		return data.peekFirst();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return priority + "_Buffer" + data.toString();
	}

	/**
	 * @return the ewma
	 */
	public double getEwma() {
		return ewma;
	}

	/**
	 * @param ewma
	 *            the ewma to set
	 */
	public void setEwma(double ewma) {
		this.ewma = ewma;
	}

	/**
	 * @return the ewmaLastAdjustedInterval
	 */
	public long getEwmaLastAdjustedInterval() {
		return ewmaLastAdjustedInterval;
	}

	/**
	 * @param ewmaLastAdjustedInterval
	 *            the ewmaLastAdjustedInterval to set
	 */
	public void setEwmaLastAdjustedInterval(long ewmaLastAdjustedInterval) {
		this.ewmaLastAdjustedInterval = ewmaLastAdjustedInterval;
	}

	/**
	 * @return the hpdTotalSentPacketDelay
	 */
	public long getHpdTotalSentPacketDelay() {
		return hpdTotalSentPacketDelay;
	}

	/**
	 * @return the hpdTotalSentPackets
	 */
	public long getHpdTotalSentPackets() {
		return hpdTotalSentPackets;
	}

	/**
	 * @return the wfqSent
	 */
	public long getWfqSent() {
		return wfqSent;
	}

	/**
	 * @param wfqSent
	 *            the wfqSent to set
	 */
	public void setWfqSent(long wfqSent) {
		this.wfqSent = wfqSent;
	}

	/**
	 * @return the wfqQuota
	 */
	public double getWfqWeight() {
		return wfqWeight;
	}

	/**
	 * @param wfqWeight
	 *            the wfqQuota to set
	 */
	public void setWfqWeight(double wfqWeight) {
		this.wfqWeight = wfqWeight;
	}

}
