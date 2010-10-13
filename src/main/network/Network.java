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
 * $Id: Network.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.network;

import main.event.NetworkReceive;
import main.event.NetworkSend;
import main.event.NodeReceive;
import main.node.Directory;
import main.node.Node;
import main.system.Driver;

public class Network {

	/**
	 * Represents the current number of bytes in the incoming network queue
	 */
	private int bytesIncomming;

	/**
	 * Represents the current number of bytes in the outgoing network queue
	 */
	private int bytesOutgoing;

	/**
	 * Downstream bandwidth of incoming data in kbps
	 */
	private int downstreamBandwidth;

	/**
	 * The node this network is servicing
	 */
	private Node node;

	/**
	 * The number of nanoseconds it takes to receive one byte
	 */
	private int timeToReceiveOneByte;

	/**
	 * The number of nanoseconds it takes to send one byte
	 */
	private int timeToSendOneByte;

	/**
	 * Upstream bandwidth of outgoing data in kbps
	 */
	private int upstreamBandwidth;

	/**
	 * Create a network for the given node, converting bandwidth to the number
	 * of nanoseconds it takes to send a byte.
	 * 
	 * @param node
	 *            the node this network transfers data for
	 * @param upstreamBandwidth
	 *            in kbps
	 * @param downstreamBandwidth
	 *            in kbps
	 */
	public Network(Node node, int upstreamBandwidth, int downstreamBandwidth) {
		this.upstreamBandwidth = upstreamBandwidth;
		timeToSendOneByte = kilobitsPerSecondToNanosecondsPerByte(upstreamBandwidth);
		this.downstreamBandwidth = downstreamBandwidth;
		timeToReceiveOneByte = kilobitsPerSecondToNanosecondsPerByte(downstreamBandwidth);
		this.node = node;
	}

	/**
	 * @return the node
	 */
	public Node getNode() {
		return node;
	}

	/**
	 * @return the conversion value
	 */
	private int kilobitsPerSecondToNanosecondsPerByte(int kbps) {
		return 8000000 / kbps;
	}

	/**
	 * Decrements the number of bytes coming into this node by the size of the
	 * given data.
	 * 
	 * @param data
	 *            the data this network finished receiving
	 */
	public void notifyFinishedReceiving(Datagram data) {
		// the network received the entire cell
		bytesIncomming -= data.getSize();
	}

	/**
	 * Decrements the number of bytes going out of this node by the size of the
	 * given data. Also notifies this network that it is ready to send more
	 * data.
	 * 
	 * @param data
	 *            the data this network finished receiving
	 */
	public void notifyFinishedSending(long time, Datagram data) {
		// the network received the entire cell
		bytesOutgoing -= data.getSize();
		// we want to attempt to schedule another data
		notifyReadyToSend(time);
	}

	/**
	 * Tells the network to check if it can schedule more data. The scheduler is
	 * called if there are currently no bytes going out.
	 */
	public void notifyReadyToSend(long time) {
		// if we are not sending, invoke the scheduler
		if (bytesOutgoing <= 0) {
			SchedulingRing ring = node.getSchedulingRing();
			if (ring != null) {
				node.getScheduler().schedule(time, ring);
			}
		}
	}

	/**
	 * Receive data into this network, incrementing the number of incoming
	 * bytes. At the point this method is called, latency should have been
	 * accounted for by the sender. The sender has attached the time it would
	 * take it to send this data. We compute the receiverDelay, and the network
	 * will be finished receiving the message after the maximum of it and the
	 * given senderDelay, since the slower connection affects incoming speed. A
	 * NodeReceive event is scheduled for that time.
	 * 
	 * @param data
	 *            the data received
	 * @param senderDelay
	 *            the time, in nanoseconds, it would take the sender to send
	 *            this data
	 */
	public void receive(long time, Datagram data, long senderDelay) {
		// the network is receiving this cell
		bytesIncomming += data.getSize();

		// we already absorbed latency, so the node will receive
		// after max(senderDelay, receiverDelay) (in nanoseconds)
		// since the slower connection affects incoming speed
		long receiverDelay = (bytesIncomming * timeToReceiveOneByte);
		long delay = Math.max(senderDelay, receiverDelay);
		Driver.getInstance().addEvent(new NodeReceive(time + delay, data));
	}

	/**
	 * Send the given data out, incrementing the number of outgoing bytes. The
	 * time it would take the sender to send this data is calculated and
	 * included in a new NetworkReceive event for the next-hop network. Another
	 * NetworkSend event for this network is created after the calculated
	 * sendDelay.
	 * 
	 * @param data
	 *            the datagram we are sending
	 */
	public void send(long time, Datagram data) {
		// this data is being sent out on the network
		bytesOutgoing += data.getSize();
		long sendDelay = (bytesOutgoing * timeToSendOneByte);

		// phantom data is received by no one
		if(!data.isPhantom()){
			// incorporate latency now, sendDelay is handled by receiver
			Driver.getInstance().addEvent(
					new NetworkReceive(time + Directory.latency, data, sendDelay));
		}

		// schedule another send after the sending delay
		Driver.getInstance().addEvent(new NetworkSend(time + sendDelay, data));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "[" + downstreamBandwidth + "kbpsDown][" + upstreamBandwidth
				+ "kbpsUp]";
	}

	public int getUpstreamBandwidth() {
		return upstreamBandwidth;
	}

}
