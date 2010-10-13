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
 * $Id: GenerateRequest.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.event;

import main.application.TorApplication;
import main.node.Node;
import main.node.Server;

/**
 * An event that generates a request upon execution.
 * 
 * @author Rob Jansen
 */
public class GenerateRequest extends Event {

	/**
	 * The source application that will generate the request
	 */
	private TorApplication application;
	/**
	 * The destination server of the request
	 */
	private Server server;

	/**
	 * Create the event after the delay.
	 * 
	 * @param application
	 *            the application that will generate the request
	 * @param server
	 *            the destination of the request
	 * @param timeDelay
	 *            the simulation time to delay execution
	 */
	public GenerateRequest(long runTime, TorApplication application, Server server) {
		super(runTime);
		this.application = application;
		this.server = server;
	}

	/**
	 * Calls the generate request method of the application for the server.
	 * 
	 * @see main.event.Event#run()
	 * @see main.application.TorApplication#generateRequest(Server)
	 */
	@Override
	public void run() {
		application.generateRequest(getTime(), server);
	}

	@Override
	public Node getOwner() {
		return application.getClient();
	}

}
