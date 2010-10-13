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
 * $Id: ApplicationStart.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.event;

import main.application.TorApplication;
import main.node.Node;

/**
 * An event that starts an application upon execution.
 * 
 * @author Rob Jansen
 */
public class ApplicationStart extends Event {

	/**
	 * The application we want to start
	 */
	private TorApplication application;

	/**
	 * Create the event after the delay.
	 * 
	 * @param application
	 *            the application to start
	 * @param timeDelay
	 *            the simulation time to delay execution
	 */
	public ApplicationStart(long runTime, TorApplication application) {
		super(runTime);
		this.application = application;
	}

	/**
	 * Calls the applications start method.
	 * 
	 * @see main.event.Event#run()
	 * @see main.application.TorApplication#start()
	 */
	@Override
	public void run() {
		application.start(getTime());
	}

	@Override
	public Node getOwner() {
		// TODO Auto-generated method stub
		return application.getClient();
	}

}