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
 * $Id: NetworkSend.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.event;

import main.network.Datagram;
import main.node.Node;

/**
 * An event that notifies a node when it has finished sending a datagram upon
 * execution.
 * 
 * @author Rob Jansen
 */
public class NetworkSend extends Event {
	/**
	 * The datagram being sent.
	 */
	private Datagram data;

	/**
	 * Create the event after the delay.
	 * 
	 * @param data
	 * @param timeDelay
	 *            the simulation time to delay execution
	 */
	public NetworkSend(long runTime, Datagram data) {
		super(runTime);
		this.data = data;
	}

	/**
	 * The sender of the datagram is notified that it has completed its part of
	 * the transmission for this datagram, which means it can schedule the next
	 * outgoing datagram on the network.
	 * 
	 * @see main.event.Event#run()
	 * @see main.network.Network#notifyFinishedSending(Datagram)
	 */
	@Override
	public void run() {
		data.getChannel().getSender().getNetwork().notifyFinishedSending(getTime(), data);
	}

	@Override
	public Node getOwner() {
		return data.getChannel().getSender();
	}

}
