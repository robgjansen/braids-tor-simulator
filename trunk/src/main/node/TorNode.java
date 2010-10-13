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
 * $Id: TorNode.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.node;

import java.util.HashMap;

import main.network.Buffer;
import main.network.Request;
import main.network.SchedulingRing;
import main.network.Circuit;
import main.network.Datagram;
import main.node.Directory.NodeType;
import main.resource.Configuration;
import main.scheduling.Scheduler;
import main.scheduling.Scheduler.Priority;
import main.scheduling.Scheduler.SchedulingAlgorithm;
import main.system.Driver;

/**
 * A base class for TorNodes. This class contains the buffers that hold data
 * that is ready to be scheduled by the node and sent out on the network. This
 * class also determines the scheduling ratio of forwarded to sourced data. Each
 * Tor node can handle two scheduling policies.
 * <p>
 * In RoundRound or ExponentialWeightedMovingAverage, a buffer is created for
 * each circuit and stored in a map keyed by the buffer so we know where to
 * enqueue outgoing data.
 * <p>
 * In WaitingTimePriority, we create 6 buffers, one for each priority level for
 * forwarded and sourced data, and aggregate all traffic into those.
 * 
 * @author Rob Jansen
 */
public abstract class TorNode extends Node {

	/**
	 * Ring of buffers holding data this node is forwarding
	 */
	SchedulingRing bufferRing;
	/**
	 * Maps each circuit for which we forward data to a buffer. Only used in
	 * scheduling policies that require separation of per-flow data (i.e.
	 * RoundRobin)
	 */
	HashMap<Circuit, Buffer> circuitBufferMap;
	/**
	 * Buffer holding forwarded HighThroughput priority data. Only used with WTP
	 * scheduling.
	 */
	Buffer highThroughput;
	/**
	 * Buffer holding forwarded LowLatency priority data. Only used with WTP
	 * scheduling.
	 */
	Buffer lowLatency;
	/**
	 * Buffer holding forwarded Normal priority data. Only used with WTP
	 * scheduling.
	 */
	Buffer normal;

	/**
	 * Creates the TorNode and creates the SchedulingRings and buffers needed
	 * based on the scheduling policy this node will implement. Sets
	 * ticketBalance to 0.
	 * 
	 * @see main.node.TorNode
	 * @see main.node.Node for parameter definitions
	 */
	public TorNode(NodeType type, Scheduler scheduler, int upstreamBandwidth,
			int downstreamBandwidth) {
		super(type, scheduler, upstreamBandwidth, downstreamBandwidth);

		// everyone has a buffer
		bufferRing = new SchedulingRing();

		// only separate flows for round robin scheduler
		// then each buffer ring will contain a buffer for each circuit
		if (Configuration.SCHEDULER == SchedulingAlgorithm.ROUND_ROBIN
				|| Configuration.SCHEDULER == SchedulingAlgorithm.EXPONENTIAL_WEIGHTED_MOVING_AVERAGE
				|| Configuration.SCHEDULER == SchedulingAlgorithm.WEIGHTED_FAIR_QUEUEING
				|| (Configuration.SCHEDULER == SchedulingAlgorithm.HYBRID_PROPORTIONAL_DELAY && !Configuration.NETWORK_PRIORITY)) {
			// RR creates buffers as circuits are created
			circuitBufferMap = new HashMap<Circuit, Buffer>();
		} else if (Configuration.SCHEDULER == SchedulingAlgorithm.HYBRID_PROPORTIONAL_DELAY) {
			// WTP creates service classes now
			normal = new Buffer(Priority.NORMAL, this, bufferRing);
			bufferRing.add(normal);
			lowLatency = new Buffer(Priority.LOW_LATENCY, this, bufferRing);
			bufferRing.add(lowLatency);
			highThroughput = new Buffer(Priority.HIGH_THROUGHPUT, this,
					bufferRing);
			bufferRing.add(highThroughput);
		} else if (Configuration.SCHEDULER == SchedulingAlgorithm.FIRST_COME_FIRST_SERVED) {
			normal = new Buffer(Priority.NORMAL, this, bufferRing);
			bufferRing.add(normal);
		} else {
			Driver.log.severe("Unimplemented scheduler chosen");
		}
	}

	/**
	 * Return the buffer ring from which scheduling decisions will be made. We
	 * consult our strategy to decide if we should relay data for others, or
	 * send out our own traffic during this scheduling decision.
	 * 
	 * If null is returned, we should be idle this timeperiod, following the
	 * implemented strategy.
	 * 
	 * @see main.node.Node#getSchedulingRing()
	 */
	@Override
	public SchedulingRing getSchedulingRing() {
		return bufferRing;
	}

	/**
	 * @return this node
	 */
	protected TorNode getTorNode() {
		return this;
	}

	/**
	 * If RoundRobin scheduling is used, we need to create new buffers for the
	 * new circuit, and store them in our map for access when enqueueing data.
	 * <p>
	 * The buffers are added to the scheduling ring unless the
	 * Configuration.NETWORK_DYNAMIC_RR option is set, in which case buffers
	 * dynamically add and remove themselves to the ring depending on their data
	 * contents.
	 * 
	 * @param circuit
	 */
	public void notifyCircuitBuilt(Circuit circuit) {
		// we only care if we want to separate flows, ie RR
		if (Configuration.SCHEDULER == SchedulingAlgorithm.ROUND_ROBIN) {
			// only add buffers dynamically if dynamic flag is true
			createBufferForCircuit(circuit,
					Configuration.NETWORK_DYNAMIC_BUFFERS);
		} else if (Configuration.SCHEDULER == SchedulingAlgorithm.EXPONENTIAL_WEIGHTED_MOVING_AVERAGE) {
			// only add buffers dynamically if dynamic flag is true
			createBufferForCircuit(circuit,
					Configuration.NETWORK_DYNAMIC_BUFFERS);
		} else if (Configuration.SCHEDULER == SchedulingAlgorithm.HYBRID_PROPORTIONAL_DELAY
				&& !Configuration.NETWORK_PRIORITY) {
			// only add buffers dynamically if dynamic flag is true
			// each circuit is its own service class
			createBufferForCircuit(circuit,
					Configuration.NETWORK_DYNAMIC_BUFFERS);
		} else if (Configuration.SCHEDULER == SchedulingAlgorithm.WEIGHTED_FAIR_QUEUEING) {
			// never add buffers dynamically for scheduling purposes
			createBufferForCircuit(circuit, false);
		}
	}

	private void createBufferForCircuit(Circuit circuit, boolean isDynamic) {
		Buffer buffer = new Buffer(Priority.NORMAL, this, bufferRing);
		// dynamic means the buffer will be added when it gets data
		if (!isDynamic) {
			bufferRing.add(buffer);
		}
		circuitBufferMap.put(circuit, buffer);
	}

	/**
	 * A circuit was toredown, so if RoundRobin scheduling is used we can remove
	 * the associated buffers from our stored map and SchedulingRing.
	 * 
	 * @param circuit
	 */
	public void notifyCircuitTordown(Circuit circuit) {
		// we only care if we want to separate flows, ie RR
		if (Configuration.SCHEDULER == SchedulingAlgorithm.ROUND_ROBIN
				|| Configuration.SCHEDULER == SchedulingAlgorithm.EXPONENTIAL_WEIGHTED_MOVING_AVERAGE
				|| Configuration.SCHEDULER == SchedulingAlgorithm.WEIGHTED_FAIR_QUEUEING
				|| (Configuration.SCHEDULER == SchedulingAlgorithm.HYBRID_PROPORTIONAL_DELAY
				&& !Configuration.NETWORK_PRIORITY)) {
			bufferRing.remove(circuitBufferMap.remove(circuit));
		}
	}

	@Override
	public abstract void receive(long time, Datagram data);

	/**
	 * For RoundRobin scheduling, we do a lookup in the map to find the buffer
	 * to enqueue the given cell. If WTP is used, we check the priority and the
	 * data source to select the correct buffer. Finally we notify the network
	 * that there is data waiting to be sent.
	 */
	@Override
	public void send(long time, Datagram cell) {
		// put in correct buffer, so we can proportionately allocate bandwidth
		// to forwarded/sourced data.
		// also, add to correct buffer based on priority

		if (Configuration.SCHEDULER == SchedulingAlgorithm.HYBRID_PROPORTIONAL_DELAY && Configuration.NETWORK_PRIORITY) {
			// we aggregate traffic into 3 classes
			switch (cell.getMessage().getPriority()) {
			case NORMAL:
				normal.enqueue(time, cell);
				break;
			case LOW_LATENCY:
				lowLatency.enqueue(time, cell);
				break;
			case HIGH_THROUGHPUT:
				highThroughput.enqueue(time, cell);
				break;
			}
		} else if (Configuration.SCHEDULER == SchedulingAlgorithm.ROUND_ROBIN
				|| Configuration.SCHEDULER == SchedulingAlgorithm.EXPONENTIAL_WEIGHTED_MOVING_AVERAGE
				|| Configuration.SCHEDULER == SchedulingAlgorithm.WEIGHTED_FAIR_QUEUEING
				|| (Configuration.SCHEDULER == SchedulingAlgorithm.HYBRID_PROPORTIONAL_DELAY && !Configuration.NETWORK_PRIORITY)) {
			// we care about per-circuit traffic
			Request r = cell.getRequest();
			Circuit c = r.getCircuit();
			Buffer b = circuitBufferMap.get(c);
			b.enqueue(time, cell);
		} else if (Configuration.SCHEDULER == SchedulingAlgorithm.FIRST_COME_FIRST_SERVED) {
			normal.enqueue(time, cell);
		} else {
			Driver.log.severe("Unimplemented scheduler chosen");
		}

		// tell the network we have data to schedule
		getNetwork().notifyReadyToSend(time);
	}

}
