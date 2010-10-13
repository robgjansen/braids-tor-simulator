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
 * $Id: WebBrowser.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.application;

import main.event.GenerateRequest;
import main.network.Datagram;
import main.network.Reply;
import main.network.Request;
import main.node.Client;
import main.node.Directory;
import main.node.Server;
import main.resource.Configuration;
import main.resource.Distribution;
import main.resource.Distribution.DistributionType;
import main.scheduling.Scheduler.Priority;
import main.system.Driver;
import main.util.SimulationClock;

/**
 * A web browser application that generates web traffic based on the 2003
 * Hernandez-Campos UNC web traffic model.
 * <p>
 * http://www.cs.unc.edu/Research/dirt/proj/http-model/
 * 
 * @author Rob Jansen
 */
public class WebBrowser extends TorApplication {
	/**
	 * The total number of objects on page currently being downloaded.
	 */
	private int totalObjectsOnPage;
	/**
	 * The number of objects on the current page that have been downloaded.
	 */
	private int downloadedObjectsOnPage;
	/**
	 * The time that the top-level page request was made, in nanoseconds.
	 */
	private long pageRequestTime;
	/**
	 * The total number of bytes downloaded from the current page.
	 */
	private int totalBytesReplied;
	/**
	 * The total number of bytes uploaded for the current page.
	 */
	private int totalBytesRequested;

	/**
	 * Creates the application and initializes measurement variables.
	 * 
	 * @param directory
	 *            the Tor network through which the application will communicate
	 * @param client
	 *            the client running the application
	 */
	public WebBrowser(Directory directory, Client client) {
		super(client, directory);
		reinitialize();
	}

	/**
	 * Generates multiple embedded object requests. The number of objects,
	 * request sizes, and response sizes are drawn from the traffic model
	 * distributions.
	 * 
	 * @param server
	 *            the server from which we are downloading.
	 */
	private void generateEmbeddedObjects(long time, Server server) {
		totalObjectsOnPage = Distribution
				.sample(DistributionType.OBJECTS_PER_PAGE);

		for (int i = 0; i < totalObjectsOnPage; i++) {
			int requestSize = Datagram.MAX_PAYLOAD_LENGTH
					* Distribution.sample(DistributionType.OBJECT_REQUEST_SIZE);
			int responseSize = Datagram.MAX_PAYLOAD_LENGTH
					* Distribution
							.sample(DistributionType.OBJECT_RESPONSE_SIZE);
			Request request = new Request(time, this, server, requestSize,
					responseSize, false);

			client.send(time, request, Priority.LOW_LATENCY);
		}
	}

	/**
	 * Generates a top-level page request by drawing request and response sizes
	 * from the traffic model distribution.
	 * 
	 * @see main.application.TorApplication#generateRequest(main.node.Server)
	 */
	@Override
	public void generateRequest(long time, Server server) {
		if(!Driver.getInstance().generateTraffic){
			return;
		}
		
		// start a new page request
		// initialize variables
		reinitialize();

		// assume every top level request is on a new connection
		Driver.getInstance().incrementWebConnectionCount();

		// form requests
		int requestSize = Datagram.MAX_PAYLOAD_LENGTH
				* Distribution.sample(DistributionType.PAGE_REQUEST_SIZE);
		int responseSize = Datagram.MAX_PAYLOAD_LENGTH
				* Distribution.sample(DistributionType.PAGE_RESPONSE_SIZE);
		Request request = new Request(time, this, server, requestSize, responseSize,
				true);

		// tell the client to send the request to appropriate circuit
		client.send(time, request, Priority.LOW_LATENCY);
	}

	/**
	 * Log the round trip time using the class variable measurements for
	 * download statistics, specifying the type of data downloaded.
	 * 
	 * @param description
	 *            (priority) of the data downloaded.
	 */
	private void logRtt(long time, String description) {
		// round trip time to download page and all embedded objects
		long rtt = computeRtt(time, pageRequestTime);
		Driver.log.config(toString() + " " + description + " rtt measurement: "
				+ rtt + " milliseconds to download 1 page and "
				+ totalObjectsOnPage + " total embedded objects, "
				+ totalBytesRequested + " total bytes requested, and "
				+ totalBytesReplied + " total bytes replied");
	}

	/**
	 * Receive a reply from the client. Updates statistics on the page download
	 * in progress. When the entire page is downloaded, log the statistics and
	 * schedule the next page generation after a time drawn from a traffic model
	 * distribution.
	 * 
	 * @see main.application.TorApplication#receive(main.network.Reply)
	 */
	@Override
	public void receive(long time, Reply reply) {
		Request request = reply.getRequest();
		// we are finished downloading an entire reply
		totalBytesRequested += request.getSize();
		totalBytesReplied += reply.getSize();

		if (request.isPageRequest()) {
			// this was from a page request
			pageRequestTime = request.getCreationTimestamp();
			// now download all the embedded objects
			generateEmbeddedObjects(time, reply.getServer());
		} else {
			// we finished receiving an embedded object request
			downloadedObjectsOnPage++;
			if (downloadedObjectsOnPage >= totalObjectsOnPage) {
				// we finished entire page and all objects
				// log the rtt performance
				logRtt(time, reply.toString());

				// generate new page request after delay in nanoseconds
				long delay = (long) (Distribution
						.sample(DistributionType.IDLE_TIME)
						* SimulationClock.getInstance().getOneMillisecond() * Configuration.THINKTIME_ADJUSTMENT);
				Server server = reply.getServer();
				Driver.getInstance().addEvent(
						new GenerateRequest(time + delay, this, server));
			}
		}
	}

	/**
	 * Zeroes the variables used to keep track of statistics for the page
	 * download in progress.
	 */
	private void reinitialize() {
		downloadedObjectsOnPage = 0;
		totalBytesRequested = 0;
		totalBytesReplied = 0;
		pageRequestTime = 0;
		totalObjectsOnPage = 0;
	}

	/**
	 * Starts this web browser by generating a single request to a random
	 * server.
	 * 
	 * @see main.application.TorApplication#start()
	 */
	@Override
	public void start(long time) {
		generateRequest(time, getDirectory().getRandomServer());
	}

}