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
 * $Id: Message.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.network;

import main.application.TorApplication;
import main.node.Server;
import main.scheduling.Scheduler.Priority;

/**
 * A message between client and server.
 * 
 * @author Rob Jansen
 */
public abstract class Message {
	/**
	 * The application that generated this message.
	 */
	private TorApplication application;
	/**
	 * The circuit transferring this message
	 */
	private Circuit circuit;
	/**
	 * A data creation timestamp.
	 */
	private long creationTimestamp;
	/**
	 * The number of bytes of this message that have made it to the destination
	 */
	private int deliveredBytes;
	/**
	 * The priority of this message
	 */
	private Priority priority;
	/**
	 * The server the message is to/from
	 */
	private Server server;
	/**
	 * The total size of the message, in bytes
	 */
	private int size;

	/**
	 * Create the message, setting the timestamp to now and delivered bytes to 0
	 * 
	 * @param application
	 *            the application that caused generation of this message
	 * @param server
	 *            the server which the application is communicating with
	 * @param size
	 *            the total size, in bytes, of the message
	 */
	public Message(long time, TorApplication application, Server server, int size) {
		this.server = server;
		this.size = size;
		this.application = application;
		creationTimestamp = time;
		deliveredBytes = 0;
	}

	/**
	 * @return the application that caused generation of this message
	 */
	public TorApplication getApplication() {
		return application;
	}

	/**
	 * @return The circuit transferring this message
	 */
	public Circuit getCircuit() {
		return circuit;
	}

	/**
	 * @return the creationTimestamp
	 */
	public long getCreationTimestamp() {
		return creationTimestamp;
	}

	/**
	 * @return the number of bytes of this message that have been delivered
	 */
	public int getDeliveredBytes() {
		return deliveredBytes;
	}

	/**
	 * @return the priority of this message
	 */
	public Priority getPriority() {
		return priority;
	}

	/**
	 * @return the server which the application is communicating with
	 */
	public Server getServer() {
		return server;
	}

	/**
	 * @return the size of this message in bytes
	 */
	public int getSize() {
		return size;
	}

	/**
	 * @return true if the bytes delivered equals the size of the message, false
	 *         otherwise
	 */
	public boolean isDelivered() {
		return deliveredBytes == size;
	}

	/**
	 * Increment the counter of delivered bytes
	 * 
	 * @param size
	 *            the number of bytes of this message that has been received
	 */
	public void receivedPart(int size) {
		deliveredBytes += size;
	}

	/**
	 * @param circuit
	 *            the circuit to set
	 */
	public void setCircuit(Circuit circuit) {
		this.circuit = circuit;
	}

	/**
	 * @param priority
	 *            the priority to set
	 */
	public void setPriority(Priority priority) {
		this.priority = priority;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		switch (priority) {
		case NORMAL:
			return "NormalData";
		case LOW_LATENCY:
			return "LowLatencyData";
		case HIGH_THROUGHPUT:
			return "HighThroughputData";
		}
		return super.toString();
	}

}
