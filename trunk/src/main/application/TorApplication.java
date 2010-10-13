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
 * $Id: TorApplication.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.application;

import main.network.Reply;
import main.node.Client;
import main.node.Directory;
import main.node.Server;

/**
 * Abstract class for applications that contains useful functionality that can
 * be shared across many tor applications. Each subclass will then only need to
 * implement a small number of methods for generating requests and receiving
 * replies.
 * 
 * @author Rob Jansen
 */
public abstract class TorApplication {
	/**
	 * The client on which this application is running
	 */
	protected Client client;
	/**
	 * The directory running Tor.
	 */
	private Directory directory;

	/**
	 * Creates a TorApplication that will run on the given client.
	 * 
	 * @param directory
	 *            the Tor network through which the application will communicate
	 * @param client
	 *            the client running the application
	 */
	public TorApplication(Client client, Directory directory) {
		this.client = client;
		this.directory = directory;
	}

	/**
	 * Computes the round trip time of a message, in milliseconds, based on the
	 * current simulation time.
	 * 
	 * @param start
	 *            the time when the message was initially created, in
	 *            nanoseconds
	 */
	public long computeRtt(long time, long start) {
		return (time - start) / 1000000;
	}

	/**
	 * Generates an application specific request and sends the request to the
	 * specified server. The size of the request, the requested size of the
	 * reply, and the delay between generation events is application specific.
	 * @param runner 
	 * 
	 * @param server
	 *            the communication partner
	 */
	public abstract void generateRequest(long time, Server server);

	/**
	 * @return the client
	 */
	public Client getClient() {
		return client;
	}

	/**
	 * This method processes replies received from the client to this
	 * application. Used for logging measurements and chaining requests.
	 * 
	 * @param reply
	 */
	public abstract void receive(long time, Reply reply);

	/**
	 * Starts the application by generating some number of requests, as
	 * specified by each subclass.
	 * @param currentTime 
	 */
	public abstract void start(long currentTime);

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + client.toString();
	}

	/**
	 * @return the directory
	 */
	public Directory getDirectory() {
		return directory;
	}
}
