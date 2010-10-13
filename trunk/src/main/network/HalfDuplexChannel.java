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
 * $Id: HalfDuplexChannel.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.network;

import main.node.Node;

/**
 * Represents a half duplex channel. This channel sends data in one direction
 * only, from a sender to a receiver.
 * 
 * @author Rob Jansen
 */
public class HalfDuplexChannel {

	/**
	 * The link to the next channel in the path
	 */
	private HalfDuplexChannel nextChannel;

	/**
	 * The Node representing the receiver in this channel.
	 */
	private Node receiver;

	/**
	 * The Node representing the sender of this channel.
	 */
	private Node sender;

	/**
	 * Creates a channel between the given sender and receiver
	 * 
	 * @param sender
	 *            the sender of this channel
	 * @param receiver
	 *            the receiver of this channel
	 */
	public HalfDuplexChannel(Node sender, Node receiver) {
		this.sender = sender;
		this.receiver = receiver;
	}

	/**
	 * @return the nextChannel in the path
	 */
	public HalfDuplexChannel getNextChannel() {
		return nextChannel;
	}

	/**
	 * @return the receiver Node of this channel
	 */
	public Node getReceiver() {
		return receiver;
	}

	/**
	 * @return the sender Node of this channel
	 */
	public Node getSender() {
		return sender;
	}

	/**
	 * @param nextChannel
	 *            the nextChannel link to set
	 */
	public void setNextChannel(HalfDuplexChannel nextChannel) {
		this.nextChannel = nextChannel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "[" + sender.toString() + "]-->[" + receiver.toString() + "]";
	}

}
