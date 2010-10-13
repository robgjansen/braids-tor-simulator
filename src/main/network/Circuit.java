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
 * $Id: Circuit.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.network;

import java.util.ArrayList;

import main.node.Client;
import main.node.Directory;
import main.node.Node;
import main.node.Relay;
import main.node.Server;
import main.system.Driver;

/**
 * A circuit consisting of a client, 3 relays, and a server. Circuits keep track
 * of the number of outstanding requests they are transferring and do not get
 * toredown until all data is cleared.
 * 
 * @author Rob Jansen
 */
public class Circuit {
	/**
	 * The client that created the circuit
	 */
	private Client client;
	/**
	 * The Tor Directory, used for selecting relays
	 */
	private Directory directory;
	/**
	 * The first channel in the circuit. Channels are linked so each hop knows
	 * where to forward data.
	 */
	private HalfDuplexChannel entryLink;
	/**
	 * A relay in the path.
	 */
	private Relay firstHop, secondHop, thirdHop;
	/**
	 * A flag indicating the client is finished using this circuit
	 */
	private boolean isZombie;
	/**
	 * The number of requests currently in transit in this circuit
	 */
	private int outstandingRequests;
	/**
	 * Keeps track of the total number of datagrams the client received from this circuit.
	 */
	private int datagramCount;
	/**
	 * The client's communication partner
	 */
	private Server server;

	/**
	 * Create a new circuit with the given client, server, and relays selected
	 * from the directory.
	 * 
	 * @param client
	 *            the circuit creator
	 * @param server
	 *            the communication partner
	 * @param directory
	 *            the directory from which relays are selected
	 */
	public Circuit(Client client, Server server, Directory directory) {
		this.client = client;
		this.server = server;
		this.directory = directory;
		build();
		isZombie = false;
		outstandingRequests = 0;
		datagramCount = 0;
	}

	/**
	 * Builds a circuit by creating the necessary connections between randomly
	 * selected relays. After building the circuit, data can flow in the circuit
	 * in a single direction by following the entry link. Each Tor node in the
	 * path will be notified of the circuit creation.
	 */
	private void build() {
		// get non-intersecting relays - tor does not select the same relay
		// twice in any circuit
		ArrayList<Relay> excludeList = new ArrayList<Relay>();
		firstHop = directory.pathSelectRelay(client, excludeList);
		excludeList.add(firstHop);
		secondHop = directory.pathSelectRelay(client, excludeList);
		excludeList.add(secondHop);
		thirdHop = directory.pathSelectExit(client, excludeList);

		// create first connection and save pointer
		HalfDuplexChannel channel = new HalfDuplexChannel(client, firstHop);
		entryLink = channel;
		// create remaining channels
		channel = createNextChannel(channel, secondHop);
		channel = createNextChannel(channel, thirdHop);
		channel = createNextChannel(channel, server);
		channel = createNextChannel(channel, thirdHop);
		channel = createNextChannel(channel, secondHop);
		channel = createNextChannel(channel, firstHop);
		channel = createNextChannel(channel, client);

		client.notifyCircuitBuilt(this);
		firstHop.notifyCircuitBuilt(this);
		secondHop.notifyCircuitBuilt(this);
		thirdHop.notifyCircuitBuilt(this);

		String status = "Built new circuit: " + toString();
		Driver.log.fine(status);
	}

	/**
	 * Increment the number of outstanding requests.
	 */
	public void clientAddedRequest() {
		outstandingRequests++;
	}

	/**
	 * Decrement the number of outstanding requests. If this circuit is a
	 * zombie, i.e. the client is finished with it, and it has no more
	 * outstanding requests, it is tore-down.
	 */
	public void clientRemovedRequest() {
		outstandingRequests--;
		// teardown if no more outstanding requests and is a zombie
		if (isZombie && (outstandingRequests <= 0)) {
			teardown();
		}
	}

	/**
	 * Links the given previous channel to a new channel to the given receiver.
	 * 
	 * @param previousChannel
	 *            the previous channel in the linked path
	 * @param nextReceiver
	 *            the receiver of the next channel in the linked path
	 * @return the new linked channel
	 */
	private HalfDuplexChannel createNextChannel(
			HalfDuplexChannel previousChannel, Node nextReceiver) {
		Node nextSender = null;
		if (previousChannel != null) {
			nextSender = previousChannel.getReceiver();
		}
		HalfDuplexChannel nextChannel = new HalfDuplexChannel(nextSender,
				nextReceiver);
		previousChannel.setNextChannel(nextChannel);
		return nextChannel;
	}

	/**
	 * @return the first channel constructing the circuit. This link can be
	 *         followed to send data to the server and back.
	 */
	public HalfDuplexChannel getEntryLink() {
		return entryLink;
	}

	/**
	 * @return the server in this circuit
	 */
	public Server getServer() {
		return server;
	}

	/**
	 * Compute if the given relay is the exit node (third hop) in this circuit.
	 * 
	 * @param relay
	 *            the relay to test
	 * @return true in case the given relay is the exit node, false otherwise
	 */
	public boolean isExit(Relay relay) {
		return thirdHop.equals(relay);
	}

	/**
	 * @return the zombie status of this circuit
	 */
	public boolean isZombie() {
		return isZombie;
	}

	/**
	 * @param isZombie
	 *            the zombie status to set for this circuit
	 */
	public void setZombie(boolean isZombie) {
		this.isZombie = isZombie;
		if (outstandingRequests <= 0) {
			teardown();
		}
	}

	/**
	 * Destroys a circuit by dropping the connections between nodes. This
	 * effectively removes the forwarding links for this circuit. Each Tor node
	 * in the circuit is notified of the teardown. All references to nodes in
	 * the path are removed, and the circuit is unusable after this call.
	 */
	private void teardown() {
		HalfDuplexChannel link = entryLink;

		while (link != null) {
			HalfDuplexChannel next = link.getNextChannel();
			link.setNextChannel(null);
			link = next;
		}

		client.notifyCircuitTordown(this);
		firstHop.notifyCircuitTordown(this);
		secondHop.notifyCircuitTordown(this);
		thirdHop.notifyCircuitTordown(this);

		entryLink = null;

		Driver.log.fine("circuit " + toString() + " tore down");

		client = null;
		firstHop = null;
		secondHop = null;
		thirdHop = null;
		server = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "[" + client.toString() + "]-->[" + firstHop.toString()
				+ "]-->[" + secondHop.toString() + "]-->["
				+ thirdHop.toString() + "]-->[" + server.toString();
	}
	
	/**
	 * Increment the counter of datagrams received by the client.
	 */
	public void clientReceivedDatagram(){
		datagramCount++;
	}
	
	/**
	 * Reset the counter of datagrams received by the client.
	 */
	public void resetDatagramCount(){
		datagramCount = 0;
	}

	/**
	 * @return the datagramCount
	 */
	public int getDatagramCount() {
		return datagramCount;
	}

}
