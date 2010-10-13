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
 * $Id: Server.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.node;

import main.network.Buffer;
import main.network.Datagram;
import main.network.HalfDuplexChannel;
import main.network.Reply;
import main.network.Request;
import main.network.SchedulingRing;
import main.node.Directory.NodeType;
import main.scheduling.Scheduler;
import main.scheduling.Scheduler.Priority;
import main.system.Driver;

/**
 * The server is a simple node that stores all data in a single buffer. Replies
 * are generated after receiving an entire request, using sizes specified by the
 * request.
 * 
 * @author Rob Jansen
 */
public class Server extends Node {

	/**
	 * The ring that will contain the single server buffer.
	 */
	private SchedulingRing bufferRing;

	/**
	 * The server creates a single buffer used to store all data.
	 * 
	 * @see main.node.Node for parameter definitions
	 */
	public Server(NodeType type, Scheduler scheduler, int upstreamBandwidth,
			int downstreamBandwidth) {
		super(type, scheduler, upstreamBandwidth, downstreamBandwidth);
		bufferRing = new SchedulingRing();
		Buffer normalBuffer = new Buffer(Priority.NORMAL, this, bufferRing);
		bufferRing.add(normalBuffer);
	}

	@Override
	public SchedulingRing getSchedulingRing() {
		// no strategy needed here, all bandwidth is contributed
		return bufferRing;
	}

	/**
	 * Receives data from Tor circuits. After an entire request is received, the
	 * last data is used to send a reply.
	 */
	@Override
	public void receive(long time, Datagram data) {
		// the server only handles data
		// wait until the entire request is received
		Request request = data.getRequest();
		request.receivedPart(data.getSize());

		Driver.getInstance().decrementDataCount(data.getMessage().getPriority());

		// only send a reply if requested, i.e. reply size > 0
		if (request.isDelivered()) {
			Driver.getInstance().decrementMessageCount(request.getPriority());
			if(request.getRequestedDataSize() > 0){
				// send new reply, place in buffer and schedule as needed
				send(time, data);
			}
		}
	}

	/**
	 * Given a datagram, extract the request and use it to create and send a
	 * reply back through the circuit.
	 */
	@Override
	protected void send(long time, Datagram data) {
		Reply reply = new Reply(time, data.getRequest());
		Driver.log.fine("Server " + getId() + " sending reply of "
				+ reply.getSize() + " bytes");

		// forward the specified sized reply
		HalfDuplexChannel nextChannel = data.getChannel().getNextChannel();
		for (Datagram d : splitMessage(reply, reply.getRequest(), reply, false)) {
			d.setChannel(nextChannel);
			Driver.getInstance().incrementDataCount(d.getMessage().getPriority());
			bufferRing.current().enqueue(time, d);
		}

		Driver.getInstance().incrementMessageCount(reply.getPriority());

		// notify the network that data is ready to send
		getNetwork().notifyReadyToSend(time);
	}

}
