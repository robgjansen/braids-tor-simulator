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
 * $Id: RefreshCircuit.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.event;

import main.network.Circuit;
import main.node.Client;
import main.node.Node;

/**
 * An event that refreshes a circuit upon execution.
 * 
 * @author Rob Jansen
 */
public class RefreshCircuit extends Event {

	private Circuit circuit;
	private Client client;

	/**
	 * Create the event after the delay.
	 * 
	 * @param client
	 *            the client that will refresh
	 * @param circuit
	 *            the circuit to be refreshed
	 * @param timeDelay
	 *            the simulation time to delay execution
	 */
	public RefreshCircuit(long runTime, Client client, Circuit circuit) {
		super(runTime);
		this.client = client;
		this.circuit = circuit;
	}

	/**
	 * Call refresh circuit for the given client and circuit
	 * 
	 * @see main.event.Event#run()
	 * @see main.node.Client#refreshCircuit(Circuit)
	 */
	@Override
	public void run() {
		client.refreshCircuit(circuit);
	}

	@Override
	public Node getOwner() {
		return client;
	}

}
