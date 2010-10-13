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
 * $Id: Node.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.node;

import java.util.ArrayList;

import main.network.Datagram;
import main.network.Message;
import main.network.Network;
import main.network.Reply;
import main.network.Request;
import main.network.SchedulingRing;
import main.node.Directory.NodeType;
import main.scheduling.Scheduler;

/**
 * Represents a basic node and the required operations all node types must have.
 * 
 * @author Rob Jansen
 */
public abstract class Node {

	/**
	 * The id of this node
	 */
	private int id;
	/**
	 * Each node has a network that sends and receives datagrams following
	 * bandwidth limitations
	 */
	private Network network;
	/**
	 * Each node has a scheduler for outgoing data
	 */
	private Scheduler scheduler;
	/**
	 * The type of this node
	 * 
	 * @see main.node.Directory.NodeType
	 */
	private NodeType type;

	/**
	 * Create a node of the given type, using the given scheduler, with upstream
	 * and downstream bandwidth as specified.
	 * 
	 * @param type
	 *            the type of this node
	 * @param scheduler
	 *            the scheduler for scheduling outgoing data
	 * @param upstreamBandwidth
	 *            the upstream bandwidth of this node, in kbps
	 * @param downstreamBandwidth
	 *            the downstream bandwidth of this node, in kbps
	 */
	public Node(NodeType type, Scheduler scheduler, int upstreamBandwidth,
			int downstreamBandwidth) {
		id = Directory.getUniqueId();
		network = new Network(this, upstreamBandwidth, downstreamBandwidth);
		this.scheduler = scheduler;
		this.type = type;
	}

	/**
	 * @return the SchedulingRing containing all buffers that should be
	 *         considered by the scheduling algorithm for the next scheduling
	 *         decision.
	 */
	public abstract SchedulingRing getSchedulingRing();

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the network
	 */
	public Network getNetwork() {
		return network;
	}

	/**
	 * @return the scheduler
	 */
	public Scheduler getScheduler() {
		return scheduler;
	}

	/**
	 * Receive the given datagram.
	 * 
	 * @param data
	 */
	public abstract void receive(long time, Datagram data);

	/**
	 * Sends the given datagram. The datagram knows its forwarding information.
	 * 
	 * @param data
	 */
	protected abstract void send(long time, Datagram data);

	/**
	 * Splits a message in to the appropriate number of datagrams, which may or
	 * may not be marked as cells. Returns an empty ArrayList of datagrams if
	 * the message size is 0.
	 * 
	 * @param createFrom
	 *            the message we want to split into cells
	 * @param request
	 *            the request associated with the message, may be the message
	 *            itself
	 * @param reply
	 *            the reply associated with the message, may be the message
	 *            itself and may be null if we are splitting a request since it
	 *            doesnt have a reply yet
	 * @param areCells
	 *            true if we should create cells, false if we should create
	 *            regular datagrams
	 * @return the ArrayList of datagrams created from the message
	 */
	// FIXME nodes do not know about cells and data from server should be MTU
	// size
	protected ArrayList<Datagram> splitMessage(Message createFrom,
			Request request, Reply reply, boolean areCells) {
		ArrayList<Datagram> datas = new ArrayList<Datagram>();
		for (int i = createFrom.getSize(); i > 0; i -= Datagram.MAX_PAYLOAD_LENGTH) {
			int length = Math.min(i, Datagram.MAX_PAYLOAD_LENGTH);

			Datagram data;
			if (reply == null) {
				data = new Datagram(request, areCells, length);
			} else {
				data = new Datagram(request, reply, areCells, length);
			}
			datas.add(data);
		}
		return datas;
	}

	/**
	 * Prints out a summary of the node, using the classes SimpleName, type
	 * toString, and network toString methods
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName() + id + "(" + type.toString() + ")"
				+ network.toString();
	}

}
