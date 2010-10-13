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
 * $Id: NetworkReceive.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.event;

import main.network.Datagram;
import main.node.Node;

/**
 * An event that sends a datagram to the next networks receive method upon
 * execution.
 * 
 * @author Rob Jansen
 */
public class NetworkReceive extends Event {

	/**
	 * The datagram being sent.
	 */
	private Datagram data;
	/**
	 * The time it would take the sender to send this datagram.
	 */
	private long senderDelay;

	/**
	 * Create the event after the delay.
	 * 
	 * @param data
	 *            the datagram being sent
	 * @param senderDelay
	 *            the time it would take the sender to send this datagram
	 * @param timeDelay
	 *            the simulation time to delay execution
	 */
	public NetworkReceive(long runTime, Datagram data, long senderDelay) {
		super(runTime);
		this.data = data;
		this.senderDelay = senderDelay;
	}

	/**
	 * Checks the datagram for the next-hop and calls the receive method,
	 * passing the datagram and sender delay.
	 * 
	 * @see main.event.Event#run()
	 * @see main.network.Network#receive(Datagram, long)
	 */
	@Override
	public void run() {
		data.getChannel().getReceiver().getNetwork().receive(getTime(), data, senderDelay);
	}
	
	@Override
	public Node getOwner() {
		return data.getChannel().getReceiver();
	}

}
