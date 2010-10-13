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
 * $Id: FreeTicketDistribution.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.event;

import main.node.Client;
import main.node.Node;
import main.resource.Configuration;
import main.system.Driver;
import main.util.SimulationClock;

/**
 * An event that distributes free tickets to a client upon execution.
 * 
 * @author Rob Jansen
 */
public class FreeTicketDistribution extends Event {

	/**
	 * The client that will receive tickets.
	 */
	private Client client;

	/**
	 * Create the event after the delay.
	 * 
	 * @param client
	 *            the client to distribute tickets
	 * @param timeDelay
	 *            the simulation time to delay execution
	 */
	public FreeTicketDistribution(long runTime, Client client) {
		super(runTime);
		this.client = client;
	}

	/**
	 * Computes the amount of tickets the client will receive based on the
	 * configuration settings for ticket amount and cells per ticket. Increases
	 * the clients ticket balance by that amount and schedules another
	 * distribution event for the same client after the ticket period delay.
	 * 
	 * @see main.event.Event#run()
	 */
	@Override
	public void run() {
		int ticketHandout = Configuration.FREE_TICKETS_AMOUNT
				* Configuration.CELLS_PER_TICKET;
		client.earnTickets(ticketHandout);
		long delay = Configuration.FREE_TICKETS_PERIOD
				* SimulationClock.getInstance().getOneMinute();
		Driver.getInstance()
				.addEvent(new FreeTicketDistribution(getTime() + delay, client));
	}

	@Override
	public Node getOwner() {
		return client;
	}

}
