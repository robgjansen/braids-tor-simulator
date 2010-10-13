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
 * $Id: NodeReceive.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.event;

import main.network.Datagram;
import main.node.Node;

/**
 * An event that notifies a node that it has finished receiving a datagram upon
 * execution.
 * 
 * @author Rob Jansen
 */
public class NodeReceive extends Event {

	/**
	 * The datagram being transferred
	 */
	private Datagram data;

	/**
	 * Create the event after the delay.
	 * 
	 * @param data
	 *            the datagram being transferred from the network up to the
	 *            node.
	 * @param timeDelay
	 *            the simulation time to delay execution
	 */
	public NodeReceive(long runTime, Datagram data) {
		super(runTime);
		this.data = data;
	}

	/**
	 * Notifies the receiver of the datagram that its network has finished
	 * receiving this datagram and tranfers the message up to the nodes receive
	 * method.
	 * 
	 * @see main.event.Event#run()
	 * @see main.network.Network#notifyFinishedReceiving(Datagram)
	 * @see main.node.Node#receive(Datagram)
	 */
	@Override
	public void run() {
		Node receiver = data.getChannel().getReceiver();
		receiver.getNetwork().notifyFinishedReceiving(data);
		receiver.receive(getTime(), data);
	}
	
	@Override
	public Node getOwner() {
		return data.getChannel().getReceiver();
	}

}
