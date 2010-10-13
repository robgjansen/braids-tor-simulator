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
 * $Id: Directory.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.node;

import java.util.ArrayList;
import java.util.TreeMap;

import main.application.FileSharer;
import main.application.WebBrowser;
import main.resource.Configuration;
import main.resource.Distribution;
import main.resource.Distribution.DistributionType;
import main.scheduling.ExponentialWeightedMovingAverageScheduler;
import main.scheduling.FirstComeFirstServedScheduler;
import main.scheduling.RoundRobinScheduler;
import main.scheduling.Scheduler;
import main.scheduling.HybridProportionalDelayScheduler;
import main.scheduling.WeightedFairQueueingScheduler;
import main.system.Driver;

/**
 * The Tor directory. Responsible for creating all nodes and assisting clients
 * in relay selection procedures when building circuits.
 * 
 * @author Rob Jansen
 */
public class Directory {
	/**
	 * The possible node types.
	 * 
	 * @author Rob Jansen
	 */
	protected enum NodeType {
		NORMALRELAY, EXITRELAY, WEBCLIENT, WEBRELAY, WEBEXITRELAY, FSCLIENT, FSRELAY, FSEXITRELAY, SERVER, ;
	}

	/**
	 * Used for distributing unique ids to objects.
	 */
	private static int idHandout = 0;
	/**
	 * Global network latency as set in the configuration
	 */
	public static final long latency = Configuration.NETWORK_LATENCY;

	/**
	 * @return the next integer value of the id counter
	 */
	public static int getUniqueId() {
		idHandout++;
		if (idHandout == Integer.MAX_VALUE) {
			idHandout = Integer.MIN_VALUE + 1;
		}
		return idHandout;
	}

	/**
	 * List of relays that will exit traffic for clients
	 */
	private ArrayList<Relay> exitRelays;

	/**
	 * A map of relays that can be chosen to exit traffic, representing a CDF so
	 * each relay is chosen with probability proportional to its bandwidth.
	 */
	private TreeMap<Double, Relay> exitRelaySelection;

	/**
	 * List of all nodes in the simulation
	 */
	private ArrayList<Node> nodes;

	/**
	 * A map of relays that can be chosen as non-exit relays, representing a CDF
	 * so each relay is chosen with probability proportional to its bandwidth.
	 */
	private TreeMap<Double, Relay> nonexitRelaySelection;

	/**
	 * A map of servers, representing a CDF where each server is chosen with
	 * equal probability.
	 */
	private TreeMap<Double, Server> serverSelection;

	/**
	 * List of relays, used only during setup.
	 */
	private ArrayList<Relay> relays;
	/**
	 * List of servers, used only during setup.
	 */
	private ArrayList<Server> servers;
	/**
	 * Running total of the bandwidth of all exit relays.
	 */
	private double totalExitRelayBandwidth;
	/**
	 * Running total of the bandwidth of all relays.
	 */
	private double totalRelayBandwidth;

	/**
	 * Initialize schedulers, nodes and path selection mechanisms, according to
	 * the configuration settings.
	 */
	public Directory() {
		// initialize
		exitRelaySelection = new TreeMap<Double, Relay>();
		nonexitRelaySelection = new TreeMap<Double, Relay>();
		serverSelection = new TreeMap<Double, Server>();
		totalRelayBandwidth = 0.0;
		totalExitRelayBandwidth = 0.0;

		// create our schedulers
		Scheduler serverScheduler = new FirstComeFirstServedScheduler();
		Scheduler torScheduler = getTorScheduler();

		// create nodes list
		int totalNodes = Configuration.FS_CLIENTS + Configuration.WEB_CLIENTS
				+ Configuration.NORMAL_RELAYS + Configuration.EXIT_RELAYS
				+ Configuration.FS_RELAYS + Configuration.WEB_RELAYS
				+ Configuration.WEB_EXIT_RELAYS + Configuration.SERVERS;
		nodes = new ArrayList<Node>(totalNodes);
		relays = new ArrayList<Relay>();
		exitRelays = new ArrayList<Relay>();
		servers = new ArrayList<Server>();

		// create all types of nodes
		long currentTime = 0;
		createNodes(currentTime, NodeType.SERVER, Configuration.SERVERS, serverScheduler);
		createNodes(currentTime, NodeType.NORMALRELAY, Configuration.NORMAL_RELAYS,
				torScheduler);
		createNodes(currentTime, NodeType.EXITRELAY, Configuration.EXIT_RELAYS, torScheduler);
		createNodes(currentTime, NodeType.WEBCLIENT, Configuration.WEB_CLIENTS, torScheduler);
		createNodes(currentTime, NodeType.WEBRELAY, Configuration.WEB_RELAYS, torScheduler);
		createNodes(currentTime, NodeType.WEBEXITRELAY, Configuration.WEB_EXIT_RELAYS,
				torScheduler);
		createNodes(currentTime, NodeType.FSCLIENT, Configuration.FS_CLIENTS, torScheduler);
		createNodes(currentTime, NodeType.FSRELAY, Configuration.FS_RELAYS, torScheduler);
		createNodes(currentTime, NodeType.FSEXITRELAY, Configuration.FS_EXIT_RELAYS,
				torScheduler);

		// compute selection maps for relays and servers
		computeRelaySelectionProbabilities();
		computeServerSelectionProbabilities();

		// garbage collect these, we have saved the selection maps and all nodes
		relays = null;
		exitRelays = null;
		servers = null;
	}

	/**
	 * Computes probabilities of selecting relays for circuits, following
	 * http://www.torproject.org/svn/trunk/doc/spec/path-spec.txt
	 */
	private void computeRelaySelectionProbabilities() {
		// FIXME relay selection computations more elegant
		// exit relays
		// we pick a given router as an exit with probability proportional to
		// its bandwidth
		double probability = 0.0;
		TreeMap<Double, ArrayList<Relay>> sortedProbabilities = new TreeMap<Double, ArrayList<Relay>>();
		for (Relay relay : exitRelays) {
			probability = relay.getNetwork().getUpstreamBandwidth()
					/ totalExitRelayBandwidth;
			putProb(probability, relay, sortedProbabilities);
		}
		fillCDF(exitRelaySelection, sortedProbabilities);

		sortedProbabilities.clear();
		double weightFactor = (totalExitRelayBandwidth - (totalRelayBandwidth / 3))
				/ totalExitRelayBandwidth;
		boolean considerExit = totalExitRelayBandwidth >= totalRelayBandwidth / 3;
		double normalRelayBandwidth = totalRelayBandwidth
				- totalExitRelayBandwidth;
		totalRelayBandwidth = normalRelayBandwidth + totalExitRelayBandwidth
				* weightFactor;

		// non exit relays
		// same as above, except exit relay bandwidth is weighted to evenly
		// distribute bandwidth
		if (considerExit) {
			for (Relay relay : relays) {
				if (relay.isExit()) {
					probability = relay.getNetwork().getUpstreamBandwidth()
							* weightFactor / totalRelayBandwidth;
				} else {
					probability = relay.getNetwork().getUpstreamBandwidth()
							/ totalRelayBandwidth;
				}
				putProb(probability, relay, sortedProbabilities);
			}
		} else {
			for (Relay relay : relays) {
				if (relay.isExit()) {
					continue;
				}
				probability = relay.getNetwork().getUpstreamBandwidth()
						/ normalRelayBandwidth;
				putProb(probability, relay, sortedProbabilities);
			}
		}
		fillCDF(nonexitRelaySelection, sortedProbabilities);
	}

	/**
	 * Builds the server selection map, giving equal probability to each server.
	 */
	private void computeServerSelectionProbabilities() {
		double equalProbability = 1.0 / servers.size();
		double key = equalProbability;
		for (Server server : servers) {
			serverSelection.put(key, server);
			key += equalProbability;
			if (key > 1.0) {
				key = 1.0;
			}
		}
	}

	/**
	 * Create quantity of the given type of nodes, each of which use the given
	 * scheduler. This method contains the logic for the bandwidth and
	 * application of each node in the system.
	 * 
	 * @param nodeType
	 *            the type of node to create
	 * @param quantity
	 *            the number of nodes to create
	 * @param scheduler
	 *            the scheduler the new nodes will use
	 */
	private void createNodes(long time, NodeType nodeType, int quantity,
			Scheduler scheduler) {

		Client c;
		Relay r;
		// bandwidths are in kilobits per second
		for (int i = 0; i < quantity; i++) {
			switch (nodeType) {
			case WEBCLIENT:
				c = createWebClient(time, nodeType, scheduler);
				nodes.add(c);
				break;

			case FSCLIENT:
				c = createFSClient(time, nodeType, scheduler);
				nodes.add(c);
				break;

			case NORMALRELAY:
				r = createRelay(nodeType, scheduler, false);

				nodes.add(r);
				relays.add(r);
				break;

			case EXITRELAY:
				// exit relays same configuration as normal relays, except the
				// exit flag is true
				r = createRelay(nodeType, scheduler, true);

				nodes.add(r);
				relays.add(r);
				exitRelays.add(r);
				break;

			case WEBRELAY:
				r = createWebRelay(time, nodeType, scheduler, false);

				nodes.add(r);
				relays.add(r);
				break;

			case WEBEXITRELAY:
				// web exit relays same configuration as web relays, except the
				// exit flag is true
				r = createWebRelay(time, nodeType, scheduler, true);

				nodes.add(r);
				relays.add(r);
				exitRelays.add(r);
				break;

			case FSRELAY:
				r = createFSRelay(time, nodeType, scheduler, false);

				nodes.add(r);
				relays.add(r);
				break;

			case FSEXITRELAY:
				r = createFSRelay(time, nodeType, scheduler, true);

				nodes.add(r);
				relays.add(r);
				exitRelays.add(r);
				break;

			case SERVER:
				// servers have (practically) unlimited bandwidth
				Server s = new Server(nodeType, scheduler, getMaxBandwidth(),
						getMaxBandwidth());
				nodes.add(s);
				servers.add(s);
				break;
			}
		}
	}

	private Relay createFSRelay(long time, NodeType nodeType, Scheduler scheduler,
			boolean isExit) {
		// file sharer bandwidth
		int bwUp = Configuration.FILESHARER_BANDWIDTH_UP;
		int bwDown = Configuration.FILESHARER_BANDWIDTH_DOWN;
		// if configured as -1, draw from the relay distribution instead
		if (bwUp < 0 || bwDown < 0) {
			bwUp = bwDown = getBandwidthSample();
		}

		// we contribute a fraction of our bandwidth to Tor
		int bwContributed = Integer.MAX_VALUE;
		// can not contribute more than our total
		while (bwContributed > bwUp) {
			bwContributed = getBandwidthSample();
		}

		// keep track of total bandwidth so we can compute path
		// selection probabilities
		totalRelayBandwidth += bwContributed;
		if (isExit) {
			totalExitRelayBandwidth += bwContributed;
		}

		// the relay handles Tor forwarding, client handles the application
		Client c = new Client(nodeType, scheduler, bwUp-bwContributed, bwDown-bwContributed, this);
		Relay r = new Relay(nodeType, scheduler, isExit, bwContributed, c);
		c.setupApplication(time, new FileSharer(this, c));

		if (Configuration.TICKETS_FS_VIP) {
			// BT relays get infinite tickets
			c.setTicketVIP(true);
		}

		return r;
	}

	/**
	 * TODO
	 * 
	 * @param nodeType
	 * @param scheduler
	 * @param isExit
	 */
	private Relay createWebRelay(long time, NodeType nodeType, Scheduler scheduler,
			boolean isExit) {
		// web relays contribute bandwidth according to the distribution
		// and have a client's bandwidth in addition for their traffic
		int bwUp = Configuration.CLIENT_BANDWIDTH_UP;
		int bwDown = Configuration.CLIENT_BANDWIDTH_DOWN;
		// if configured as -1, draw from the relay distribution instead
		if (bwUp < 0 || bwDown < 0) {
			bwUp = bwDown = getBandwidthSample();
		}
		int bwContributed = getBandwidthSample();
		bwUp += bwContributed;
		bwDown += bwContributed;

		// keep track of total bandwidth so we can compute path
		// selection probabilities
		totalRelayBandwidth += bwContributed;
		if (isExit) {
			totalExitRelayBandwidth += bwContributed;
		}

		// the relay handles Tor forwarding, client handles the application
		Client c = new Client(nodeType, scheduler, bwUp-bwContributed, bwDown-bwContributed, this);
		Relay r = new Relay(nodeType, scheduler, isExit, bwContributed, c);
		c.setupApplication(time, new WebBrowser(this, c));

		return r;
	}

	/**
	 * TODO
	 * 
	 * @param nodeType
	 * @param scheduler
	 * @param isExit
	 * @return
	 */
	private Relay createRelay(NodeType nodeType, Scheduler scheduler,
			boolean isExit) {
		// bandwidth is drawn from relay distribution, all of which is
		// contributed
		int bwContributed = getBandwidthSample();

		// keep track of total bandwidth so we can compute path
		// selection probabilities
		totalRelayBandwidth += bwContributed;
		if (isExit) {
			totalExitRelayBandwidth += bwContributed;
		}

		// the relay handles Tor forwarding, client handles the application
		Relay r = new Relay(nodeType, scheduler, isExit, bwContributed, null);

		return r;
	}

	/**
	 * TODO
	 * 
	 * @param nodeType
	 * @param scheduler
	 * @return
	 */
	private Client createFSClient(long time, NodeType nodeType, Scheduler scheduler) {
		// file sharer bandwidth
		int bwUp = Configuration.FILESHARER_BANDWIDTH_UP;
		int bwDown = Configuration.FILESHARER_BANDWIDTH_DOWN;
		// if configured as -1, draw from the relay distribution instead
		if (bwUp < 0 || bwDown < 0) {
			bwUp = bwDown = getBandwidthSample();
		}

		// this client runs a FileSharer
		// the relay handles Tor forwarding, client handles the application
		Client c = new Client(nodeType, scheduler, bwUp, bwDown, this);
		c.setupApplication(time, new FileSharer(this, c));

		return c;
	}

	/**
	 * TODO
	 * 
	 * @param nodeType
	 * @param scheduler
	 * @return
	 */
	private Client createWebClient(long time, NodeType nodeType, Scheduler scheduler) {
		// web clients bandwidth
		int bwUp = Configuration.CLIENT_BANDWIDTH_UP;
		int bwDown = Configuration.CLIENT_BANDWIDTH_DOWN;
		// if configured as -1, draw from the relay distribution instead
		if (bwUp < 0 || bwDown < 0) {
			bwUp = bwDown = getBandwidthSample();
		}

		// this client runs a web browser
		Client c = new Client(nodeType, scheduler, bwUp, bwDown, this);
		c.setupApplication(time, new WebBrowser(this, c));

		return c;
	}

	/**
	 * Converts the given list of relays, sorted by probability, to the TreeMap
	 * that represents the selection CDF. The list may contain duplicates.
	 * 
	 * @param targetMap
	 *            the map that will hold the CDF
	 * @param sortedProbabilities
	 *            the list of relays, sorted by their probability of selection
	 */
	private void fillCDF(TreeMap<Double, Relay> targetMap,
			TreeMap<Double, ArrayList<Relay>> sortedProbabilities) {
		double count = 0.0;
		for (Double key : sortedProbabilities.keySet()) {
			for (Relay r : sortedProbabilities.get(key)) {
				count += key;
				targetMap.put(count, r);
			}
		}
	}

	/**
	 * Get a sample bandwidth from the relay advertised bandwidth distribution.
	 * The slowest 10% of relay bandwidths are not used, and the bandwidth drawn
	 * is clipped at 20MB.
	 * 
	 * @return the bandwidth drawn, in kbps
	 */
	private int getBandwidthSample() {
		int bandwidth = 0;
		// the slowest 10% of relays are not used
		while (bandwidth < 8192) {
			bandwidth = Distribution.sample(DistributionType.RELAY_BANDWIDTH);
		}
		// bandwidth from sample is in Bps, should be clipped at 20MB
		int twentyMB = 20 * 1024 * 1024;
		if (bandwidth > twentyMB) {
			bandwidth = twentyMB;
		}
		// bandwidth as kbps
		bandwidth = bandwidth * 8 / 1000;
		return bandwidth;
	}

	/**
	 * Sample a relay from the given map until a relay is selected that is not
	 * in the exclusions list and is not the given client.
	 * 
	 * @param map
	 *            the map to sample
	 * @param client
	 *            the client to exclude from the sample
	 * @param exclusions
	 *            other relays to exclude from the sample
	 * @return the sampled relay
	 */
	private Relay getDisjoint(TreeMap<Double, Relay> map, Client client,
			ArrayList<Relay> exclusions) {
		Relay choice = null;
		// keep selecting until we get a relay not in the exclude list and who
		// is not the client
		while ((choice == null) || exclusions.contains(choice)
				|| (choice.getLocalClient() == client)) {
			choice = (Relay) Distribution.sampleMap(map);
		}
		return choice;
	}

	/**
	 * @return the maximum bandwidth value possible
	 */
	private int getMaxBandwidth() {
		return Integer.MAX_VALUE;
	}

	/**
	 * @return a randomly sampled server from the serverSelection map
	 */
	public Server getRandomServer() {
		return (Server) Distribution.sampleMap(serverSelection);
	}

	/**
	 * Check the configuration and return a new instance of the scheduler as
	 * specified. This scheduler is configured as the relay scheduler.
	 * 
	 * @return a new scheduler
	 */
	private Scheduler getTorScheduler() {
		Scheduler torScheduler;
		switch (Configuration.SCHEDULER) {
		case HYBRID_PROPORTIONAL_DELAY:
			torScheduler = new HybridProportionalDelayScheduler();
			break;
		case ROUND_ROBIN:
			torScheduler = new RoundRobinScheduler();
			break;
		case FIRST_COME_FIRST_SERVED:
			torScheduler = new FirstComeFirstServedScheduler();
			break;
		case EXPONENTIAL_WEIGHTED_MOVING_AVERAGE:
			torScheduler = new ExponentialWeightedMovingAverageScheduler();
			break;
		case WEIGHTED_FAIR_QUEUEING:
			torScheduler = new WeightedFairQueueingScheduler();
			break;
		default:
			Driver.log
					.severe("Unrecognized scheduler. Please set appropriately in config.");
			// FIXME should maybe exit... ?
			torScheduler = null;
			break;
		}
		return torScheduler;
	}

	/**
	 * Convenience method for selecting an exit relay.
	 * 
	 * @see main.node.Directory#getDisjoint(TreeMap, Client, ArrayList)
	 */
	public Relay pathSelectExit(Client client, ArrayList<Relay> excludeList) {
		return getDisjoint(exitRelaySelection, client, excludeList);
	}

	/**
	 * Convenience method for selecting a path relay.
	 * 
	 * @see main.node.Directory#getDisjoint(TreeMap, Client, ArrayList)
	 */
	public Relay pathSelectRelay(Client client, ArrayList<Relay> excludeList) {
		return getDisjoint(nonexitRelaySelection, client, excludeList);
	}

	/**
	 * Adds the given relay to the correct list keyed by the given probability
	 * in the given map. If there is not a list at the given key, one is
	 * created.
	 * 
	 * @param probability
	 *            the key in the map
	 * @param relay
	 *            the relay to add to the list found at the key
	 * @param mapList
	 *            the map containing a list of relays for each probability
	 */
	private void putProb(double probability, Relay relay,
			TreeMap<Double, ArrayList<Relay>> mapList) {
		if (mapList.get(probability) == null) {
			ArrayList<Relay> list = new ArrayList<Relay>(4);
			list.add(relay);
			mapList.put(probability, list);
		} else {
			mapList.get(probability).add(relay);
		}
	}

	public ArrayList<Node> getNodes() {
		return nodes;
	}

}
