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
 * $Id: Client.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.node;

import java.util.HashMap;

import main.application.TorApplication;
import main.event.ApplicationStart;
import main.event.FreeTicketDistribution;
import main.event.RefreshCircuit;
import main.network.Circuit;
import main.network.Datagram;
import main.network.Reply;
import main.network.Request;
import main.node.Directory.NodeType;
import main.resource.Configuration;
import main.scheduling.Scheduler;
import main.scheduling.Scheduler.Priority;
import main.system.Driver;
import main.util.Generator;
import main.util.SimulationClock;

/**
 * A Tor client. Clients are responsible for acting as a buffer between
 * applications and the Tor node. Clients manage circuits used by the
 * applications and convert application requests into cells.
 * 
 * @author Rob Jansen
 */
public class Client extends TorNode {
	/**
	 * The map of circuits keyed by the destination server
	 */
	private HashMap<Server, Circuit> circuits;
	/**
	 * The Tor directory is used to create circuits
	 */
	private Directory directory;
	/**
	 * Flag indicating this node can always send at high priority levels even if
	 * it has no tickets. This is used to emulate the case of relays
	 * continuously downloading and forwarding data for others.
	 */
	private boolean ticketVIP;

	/**
	 * The current ticket balance of this node
	 */
	private int ticketBalance;
	
	private Relay hostRelay;

	/**
	 * Creates the client and initializes the map of circuits. By default,
	 * clients are not ticketVIPs.
	 * 
	 * @param directory
	 *            the Tor directory server
	 * @see main.node.Node for parameter definitions
	 */
	public Client(NodeType type, Scheduler scheduler, int upstreamBandwidth,
			int downstreamBandwidth, Directory directory) {
		super(type, scheduler, upstreamBandwidth, downstreamBandwidth);
		circuits = new HashMap<Server, Circuit>(6);
		this.directory = directory;
		ticketVIP = false;
		ticketBalance = 0;
		hostRelay = null;
	}

	/**
	 * Given a request and a requestedPriority, compute the number of tickets
	 * required to give priority to the request. If this node is a ticketVIP or
	 * has enough tickets, the priority is set on the request.
	 * 
	 * @param request
	 *            the request message from the application
	 * @param requestedPriority
	 *            the priority requested
	 */
	private void computePriority(Request request, Priority requestedPriority) {
		// VIPs always get priority
		if (ticketVIP) {
			request.setPriority(requestedPriority);
			return;
		}

		int ticketRate = 0;
		switch (requestedPriority) {
		case LOW_LATENCY:
			ticketRate = Configuration.TICKETS_RATE_LOW_LATENCY;
			break;
		case HIGH_THROUGHPUT:
			ticketRate = Configuration.TICKETS_RATE_HIGH_THROUGHPUT;
			break;
		case NORMAL:
			// No priority requested, return
			return;
		}

		// determine number of cells in request and response
		int requestCells = (int) Math.ceil(request.getSize()
				/ Datagram.MAX_PAYLOAD_LENGTH);
		int responseCells = (int) Math.ceil(request.getRequestedDataSize()
				/ Datagram.MAX_PAYLOAD_LENGTH);
		// 6 relays in the circuit, 3 in both directions
		int ticketsRequired = (requestCells + responseCells) * ticketRate * 6;

		// check if this node can afford the requested priority for this request
		if (ticketsRequired <= ticketBalance) {
			request.setPriority(requestedPriority);
			ticketBalance -= ticketsRequired;
		}
	}

	/**
	 * @return the map of circuits this node stores
	 */
	public HashMap<Server, Circuit> getCircuits() {
		return circuits;
	}

	/**
	 * Receive incoming data for the application. The client removes the cell
	 * and when an entire reply is received, sends it to the application for
	 * measurements and further processing.
	 * 
	 * @see main.node.TorNode#receive(main.network.Datagram)
	 */
	@Override
	public void receive(long time, Datagram data) {
		// the client handles all cells
		Driver.log.fine(toString() + " received cell");
		Driver.getInstance()
				.decrementDataCount(data.getMessage().getPriority());

		// client tor node strips off cell
		data.setCell(false);

		// logging rtt and delay measurements only when entire data is received
		Reply reply = data.getReply();
		reply.receivedPart(data.getSize());

		Circuit circuit = reply.getCircuit();
		circuit.clientReceivedDatagram();

		Driver.log.fine(toString() + " received " + data.getSize()
				+ " bytes of " + data.toString() + " from "
				+ reply.getServer().toString() + " (total bytes = "
				+ reply.getDeliveredBytes() + " of " + reply.getSize() + ")");

		// if we have received the entire reply, the application receives it
		if (reply.isDelivered()) {
			// update circuit outstanding requests
			circuit.clientRemovedRequest();
			Driver.getInstance().decrementMessageCount(reply.getPriority());
			reply.getApplication().receive(time, reply);
		}
	}

	/**
	 * Refresh the given circuit by setting its zombie status and removing it
	 * from the map. If it already is a zombie, this method has no effect. If it
	 * is not a zombie and has no outstanding requests in the network, it will
	 * be immediately toredown.
	 * 
	 * @param circuit
	 *            the circuit to refresh
	 */
	public void refreshCircuit(Circuit circuit) {
		// the circuit could have been removed already by an optimistic unchoke
		// if it is already a zombie, it will clean is itself when all data is
		// cleared out
		if (!circuit.isZombie()) {
			// remove it from circuits so we stop using it for new requests
			Circuit removed = circuits.remove(circuit.getServer());

			// if the circuit is a zombie already, the mapping for the server
			// will have changed - so make sure we are removing the correct
			// circuit
			if (removed != circuit) {
				Driver.log.severe(toString()
						+ " is refreshing incorrect circuit "
						+ removed.toString() + " instead of "
						+ circuit.toString());
			}

			// we want to destroy the old circuit when all data is done sending
			// this will also teardown the circuit if it currently has no data
			circuit.setZombie(true);
		}
	}

	/**
	 * Sends the request down the stack on behalf of the application. The given
	 * request is assigned to a circuit, creating a new circuit if necessary. If
	 * a new circuit is created, a CircuitRefresh event is created to refresh
	 * the circuit in 10 minutes. Request priority is computed if necesasry, and
	 * the request is split into cells and sent down to the TorNode.
	 * 
	 * @param request
	 *            the outgoing request from the application
	 * @param requestedPriority
	 *            the priority requested by the application
	 */
	public void send(long time, Request request, Priority requestedPriority) {
		// application sends data here, split into cells and forward to the
		// correct first hop connection in my network
		Driver.log.fine(toString() + " sending " + request.getSize()
				+ " byte request for " + request.getRequestedDataSize()
				+ " byte reply");

		// find the right circuit for this request based on server
		Server server = request.getServer();
		Circuit circuit = circuits.get(server);
		if (circuit == null) {
			// we have no circuit for this server, create a new one
			circuit = new Circuit(this, server, directory);
			circuits.put(server, circuit);

			// we want to refresh the circuit in 10 minutes
			long tenMinutes = SimulationClock.getInstance().getOneMinute() * 10;
			RefreshCircuit event = new RefreshCircuit(time + tenMinutes, this, circuit);
			Driver.getInstance().addEvent(event);
		}
		request.setCircuit(circuit);

		// do ticket computation if priority is enabled
		if (Configuration.NETWORK_PRIORITY) {
			computePriority(request, requestedPriority);
		}

		// finally, create cells and send them down
		for (Datagram cell : splitMessage(request, request, null, true)) {
			cell.setChannel(circuit.getEntryLink());
			Driver.getInstance().incrementDataCount(
					cell.getMessage().getPriority());
			super.send(time, cell);
		}

		// tell the circuit and Driver there is another outstanding request
		circuit.clientAddedRequest();
		Driver.getInstance().incrementMessageCount(request.getPriority());
	}

	/**
	 * Creates an ApplicationStart event at a random time between 0 and the
	 * configured number of minutes. If we are using priority, also add a
	 * FreeTicketDistribution event for this node at the same time.
	 * 
	 * @param application
	 *            the application to setup
	 */
	public void setupApplication(long time, TorApplication application) {
		// compute a random start delay between 0 and X minutes
		long tenMinutes = (long) (SimulationClock.getInstance().getOneMinute() * Configuration.APPLICATION_STARTUP);
		long delay = (long) (Generator.getInstance().getPrng().nextDouble() * tenMinutes);

		// add the starting event after the delay
		ApplicationStart e = new ApplicationStart(time + delay, application);
		Driver.getInstance().addEvent(e);

		// get free tickets when app starts
		if (Configuration.NETWORK_PRIORITY) {
			Driver.getInstance().addEvent(
					new FreeTicketDistribution(time + delay, this));
		}
	}

	/**
	 * Set this node's ticketVIP status
	 * 
	 * @param ticketVIP
	 *            the ticketVIP to set
	 */
	public void setTicketVIP(boolean ticketVIP) {
		this.ticketVIP = ticketVIP;
	}

	public void earnTickets(int numberOfTickets) {
		ticketBalance += numberOfTickets;
	}

	public void setHostRelay(Relay hostRelay) {
		this.hostRelay = hostRelay;
	}

	@Override
	public String toString() {
		String s = super.toString();
		if(hostRelay != null){
			s += hostRelay.getNetwork().toString();
		}
		return s;
	}

}
