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
 * $Id: Relay.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.node;

import main.network.Datagram;
import main.network.HalfDuplexChannel;
import main.node.Directory.NodeType;
import main.resource.Configuration;
import main.scheduling.Scheduler;

/**
 * A Tor client that also forwards data for others. Exit relays have the
 * additional task of converting cells from the circuit to datagrams before
 * sending data to the server, and converting data from the server into cells
 * for the circuit.
 * 
 * @author Rob Jansen
 */
public class Relay extends TorNode {

	/**
	 * Indicates if this relay is an exit node
	 */
	private boolean isExit;

	private Client localClient;

	/**
	 * The relay sets its exitNode status using the given isExit flag.
	 * 
	 * @param isExit
	 *            specifies if this relay is an exit node
	 * @see main.node.Client for parameter definitions
	 */
	public Relay(NodeType type, Scheduler scheduler, boolean isExit,
			int contributedBandwidth, Client localClient) {
		super(type, scheduler, contributedBandwidth, contributedBandwidth);
		this.isExit = isExit;
		this.localClient = localClient;
		
		if(localClient != null){
			localClient.setHostRelay(this);
		}

		// relays get a bonus
		if (localClient != null && Configuration.NETWORK_PRIORITY) {
			localClient.earnTickets(Configuration.TICKETS_RELAY_BONUS);
		}
	}

	/**
	 * Forwards a datagram to the next hop by incrementing the cell's current
	 * channel. If we are using priority, the ticket balance for this relay is
	 * adjusted appropriately.
	 * 
	 * @param cell
	 */
	private void forward(long time, Datagram cell) {
		// forward to next hop
		cell.setChannel(cell.getChannel().getNextChannel());
		getTorNode().send(time, cell);

		// add to my ticket counter
		if (Configuration.NETWORK_PRIORITY) {
			int income = 0;
			switch (cell.getMessage().getPriority()) {
			case LOW_LATENCY:
				income = Configuration.TICKETS_RATE_LOW_LATENCY;
				break;
			case HIGH_THROUGHPUT:
				income = Configuration.TICKETS_RATE_HIGH_THROUGHPUT;
				break;
			}
			localClient.earnTickets(income);
		}
	}

	/**
	 * @return true if this relay is an exit node, false otherwise
	 */
	public boolean isExit() {
		return isExit;
	}

	/**
	 * If this relay is the destination the client's code will receive the data.
	 * If this is the exit node for this datas circuit, we convert to/from cells
	 * as appropriate. Finally, the forward method is called to handle
	 * forwarding.
	 * 
	 * @see main.node.Client#receive(main.network.Datagram)
	 */
	@Override
	public void receive(long time, Datagram data) {
		HalfDuplexChannel receivingChannel = data.getChannel();

		if (isExit) {
			// I am an exit node
			Server server = data.getRequest().getServer();
			if (receivingChannel.getSender() == server) { // isFromServer
				// I need to convert regular data into cells
				data.setCell(true);
			} else if (receivingChannel.getNextChannel().getReceiver() == server) { // isForServer
				// I need to convert cells to regular data
				data.setCell(false);
			} else {
				// I am serving as a regular relay
			}
		}
		forward(time, data);
	}

	public Client getLocalClient() {
		return localClient;
	}
}
