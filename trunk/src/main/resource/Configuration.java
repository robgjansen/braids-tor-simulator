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
 * $Id: Configuration.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import main.scheduling.Scheduler.SchedulingAlgorithm;
import main.system.Driver;

/**
 * @author Rob Jansen
 * 
 */
public class Configuration {
	/**
	 * Specifies number of workers for the simulation. 1 means multi-threading
	 * is turned off and we only use a single thread of execution, and value < 1
	 * means we use a number of workers equal to the number of processor cores
	 * available on the system, value > 1 means we use the specified number of
	 * workers.
	 */
	public static int NUM_WORKERS;
	/**
	 * Setting for number clients who run a FileSharer application.
	 */
	public static int FS_CLIENTS;
	/**
	 * Setting for number of peers each FileSharer application connects to.
	 */
	public static int FS_PEERS;
	/**
	 * Setting for number of relays that run a FileSharer application.
	 */
	public static int FS_RELAYS;
	/**
	 * Setting for number of cells that can be transferred for each ticket.
	 */
	public static int CELLS_PER_TICKET;
	/**
	 * Setting for downstream bandwidth of WebBrowser clients, in kbps. Set to
	 * -1 to draw from relay distribution.
	 */
	public static int CLIENT_BANDWIDTH_DOWN;
	/**
	 * Setting for upstream bandwidth of WebBrowser clients, in kbps. Set to -1
	 * to draw from relay distribution.
	 */
	public static int CLIENT_BANDWIDTH_UP;
	/**
	 * Setting for downstream bandwidth of FileSharer relays and clients, in
	 * kbps. Set to -1 to draw from relay distribution.
	 */
	public static int FILESHARER_BANDWIDTH_DOWN;
	/**
	 * Setting for upstream bandwidth of FileSharer relays and clients, in kbps.
	 * Set to -1 to draw from relay distribution.
	 */
	public static int FILESHARER_BANDWIDTH_UP;
	/**
	 * Setting for debugging flag. If debug is true, additional messages will be
	 * printed to a debug log file.
	 */
	public static boolean DEBUG;
	/**
	 * Setting for the experiment runtime, in minutes.
	 */
	public static int ENDTIME;
	/**
	 * Setting for the number of exit relays.
	 */
	public static int EXIT_RELAYS;
	/**
	 * Setting for the filename of the configuration file.
	 */
	public static String FILENAME;
	/**
	 * Setting for the number of free tickets each client will receive when
	 * distributed.
	 */
	public static int FREE_TICKETS_AMOUNT;
	/**
	 * Setting for the period between free ticket distribution events, in
	 * minutes.
	 */
	public static int FREE_TICKETS_PERIOD;
	/**
	 * Setting for the delay differentiation parameter for the high-throughput
	 * service class. Lower values means higher priority. This value is ignored
	 * if the network is not using priority.
	 */
	public static int HIGH_THROUGHPUT_DDP;
	/**
	 * Setting for the delay differentiation parameter for the low-latency
	 * service class. Lower values means higher priority. This value is ignored
	 * if the network is not using priority.
	 */
	public static int LOW_LATENCY_DDP;
	/**
	 * Setting for dynamic buffer mode. In dynamic mode, buffers add and remove
	 * themselves from the scheduling ring when they become non-empty and empty,
	 * respectively. In non-dynamic mode, all buffers are part of the scheduling
	 * ring, even if they are empty.
	 */
	public static boolean NETWORK_DYNAMIC_BUFFERS;
	/**
	 * Setting for global network latency between all nodes, in nanoseconds.
	 */
	public static long NETWORK_LATENCY;
	/**
	 * Setting for priority mode. Priority mode will cause each datagram to
	 * receive a priority mark based on number of tickets available, which can
	 * be used during scheduling. The HPD scheduler will create 3 service
	 * classes in priority mode and use tickets when sending data. If priority
	 * mode is false, HPD scheduler will create a service class for each
	 * circuit.
	 */
	public static boolean NETWORK_PRIORITY;
	/**
	 * Setting for the delay differentiation parameter for the normal service
	 * class. Lower values means higher priority. This value is ignored if the
	 * network is not using priority.
	 */
	public static int NORMAL_DDP;
	/**
	 * Setting for number of relays that do not also run an application, and are
	 * not exit nodes.
	 */
	public static int NORMAL_RELAYS;
	/**
	 * Setting for the scheduling algorithm for all relays to use.
	 */
	public static SchedulingAlgorithm SCHEDULER;
	/**
	 * Setting for the prng seed.
	 */
	public static int SEED;
	/**
	 * Setting for the number of servers.
	 */
	public static int SERVERS;
	/**
	 * Setting for the number of tickets to charge for each high-throughput
	 * datagram for each relay in the circuit.
	 */
	public static int TICKETS_RATE_HIGH_THROUGHPUT;
	/**
	 * Setting for the number of tickets to charge for each low-latency datagram
	 * for each relay in the circuit.
	 */
	public static int TICKETS_RATE_LOW_LATENCY;
	/**
	 * If FileSharers that are relays get unlimited tickets.
	 */
	public static boolean TICKETS_FS_VIP;
	/**
	 * NUmber of tickets relay get as a bonus, since we assume they have been
	 * collecting.
	 */
	public static int TICKETS_RELAY_BONUS;
	/**
	 * Setting for number of clients that run a WebBrowser application.
	 */
	public static int WEB_CLIENTS;
	/**
	 * Setting for number of relays that also run a WebBrowser application.
	 */
	public static int WEB_RELAYS;
	/**
	 * Setting for number of exit relays that also run a WebBrowser application.
	 */
	public static int WEB_EXIT_RELAYS;
	/**
	 * Setting for number of exit relays that also run a FileSharer application.
	 */
	public static int FS_EXIT_RELAYS;
	/**
	 * All applications will start between 0 and this number of minutes.
	 */
	public static int APPLICATION_STARTUP;
	/**
	 * A fraction to multiply by the thinktime drawn from the distribution. This
	 * will alter the time between web requests.
	 */
	public static double THINKTIME_ADJUSTMENT;

	/**
	 * Used in the Exponential weighted moving average scheduler as the interval
	 * between re-scaling cell counts, in milliseconds.
	 */
	public static int EWMA_INTERVAL;

	/**
	 * Used in the Exponential weighted moving average scheduler as the
	 * percentage to scale old cells per interval.
	 */
	public static double EWMA_SCALE_FACTOR;

	/**
	 * Fraction used to specify weights used for Proportional Average Delay and
	 * Waiting Time Priority Schedulers. Set to 0 to use WTP algorithm only, and
	 * set to 1 to use PAD algorithm only.
	 */
	public static double HPD_FRACTION;

	/**
	 * Used as the interval in weighted fair queueing to recompute weights, in
	 * milliseconds.
	 */
	public static double WFQ_INTERVAL;

	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#WFQ_INTERVAL
	 */
	private static final String CONFIG_WFQ_INTERVAL = "network.wfq_interval_ms";

	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#EWMA_INTERVAL
	 */
	private static final String CONFIG_EWMA_INTERVAL = "network.ewma_interval_ms";

	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#HPD_FRACTION
	 */
	private static final String CONFIG_HPD_FRACTION = "network.hpd_fraction";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#EWMA_SCALE_FACTOR
	 */
	private static final String CONFIG_EWMA_SCALE_FACTOR = "network.ewma_scale_factor";

	private static final String CONFIG_THINKTIME_ADJUSTMENT = "network.thinktime_adjustment";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#FS_CLIENTS
	 */
	private static final String CONFIG_FS_CLIENTS = "nodes.num_fs_clients";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#FS_CLIENTS
	 */
	private static final String CONFIG_FS_EXIT_RELAYS = "nodes.num_fs_exit_relays";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#FS_PEERS
	 */
	private static final String CONFIG_FS_PEERS = "nodes.peers_per_fs";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#FS_RELAYS
	 */
	private static final String CONFIG_FS_RELAYS = "nodes.num_fs_relays";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#CELLS_PER_TICKET
	 */
	private static final String CONFIG_CELLS_PER_TICKET = "tickets.cells_per_ticket";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#CLIENT_BANDWIDTH_DOWN
	 */
	private static final String CONFIG_CLIENT_BANDWIDTH_DOWN = "network.client_bandwidth_down_kbps";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#CLIENT_BANDWIDTH_UP
	 */
	private static final String CONFIG_CLIENT_BANDWIDTH_UP = "network.client_bandwidth_up_kbps";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#CLIENT_BANDWIDTH_DOWN
	 */
	private static final String CONFIG_FILESHARER_BANDWIDTH_DOWN = "network.filesharer_bandwidth_down_kbps";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#CLIENT_BANDWIDTH_UP
	 */
	private static final String CONFIG_FILESHARER_BANDWIDTH_UP = "network.filesharer_bandwidth_up_kbps";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#DEBUG
	 */
	private static final String CONFIG_DEBUG = "debug";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#ENDTIME
	 */
	private static final String CONFIG_ENDTIME = "endtime_minutes";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#EXIT_RELAYS
	 */
	private static final String CONFIG_EXIT_RELAYS = "nodes.num_exit_relays";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#FREE_TICKETS_AMOUNT
	 */
	private static final String CONFIG_FREE_TICKETS_AMOUNT = "tickets.num_free";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#FREE_TICKETS_PERIOD
	 */
	private static final String CONFIG_FREE_TICKETS_PERIOD = "tickets.free_period_minutes";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#HIGH_THROUGHPUT_DDP
	 */
	private static final String CONFIG_HIGH_THROUGHPUT_DDP = "priority.high_throughput_ddp";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#LOW_LATENCY_DDP
	 */
	private static final String CONFIG_LOW_LATENCY_DDP = "priority.low_latency_ddp";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#NETWORK_DYNAMIC_BUFFERS
	 */
	private static final String CONFIG_NETWORK_DYNAMIC_BUFFERS = "network.dynamic_buffers";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#NETWORK_LATENCY
	 */
	private static final String CONFIG_NETWORK_LATENCY = "network.latency_in_ms";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#NETWORK_PRIORITY
	 */
	private static final String CONFIG_NETWORK_PRIORITY = "priority.use_priority";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#NORMAL_DDP
	 */
	private static final String CONFIG_NORMAL_DDP = "priority.normal_ddp";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#NORMAL_RELAYS
	 */
	private static final String CONFIG_NORMAL_RELAYS = "nodes.num_normal_relays";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#SCHEDULER
	 */
	private static final String CONFIG_SCHEDULER = "network.scheduler";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#SEED
	 */
	private static final String CONFIG_SEED = "prngseed";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#SERVERS
	 */
	private static final String CONFIG_SERVERS = "nodes.servers";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#TICKETS_RATE_HIGH_THROUGHPUT
	 */
	private static final String CONFIG_TICKETS_RATE_HIGH_THROUGHPUT = "tickets.high_throughput_rate_per_relay";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#TICKETS_RATE_LOW_LATENCY
	 */
	private static final String CONFIG_TICKETS_RATE_LOW_LATENCY = "tickets.low_latency_rate_per_relay";

	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#TICKETS_FS_VIP
	 */
	private static final String CONFIG_TICKETS_FS_VIP = "tickets.fsrelayvip";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#TICKETS_RELAY_BONUS
	 */
	private static final String CONFIG_TICKETS_RELAY_BONUS = "tickets.relay_bonus";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#WEB_CLIENTS
	 */
	private static final String CONFIG_WEB_CLIENTS = "nodes.num_web_clients";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#WEB_RELAYS
	 */
	private static final String CONFIG_WEB_RELAYS = "nodes.num_web_relays";
	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#WEB_RELAYS
	 */
	private static final String CONFIG_WEB_EXIT_RELAYS = "nodes.num_web_exit_relays";

	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#APPLICATION_STARTUP
	 */
	private static final String CONFIG_APPLICATION_STARTUP = "nodes.application_startup";

	/**
	 * The properties object that holds all configuration settings.
	 */
	private static Properties props;

	/**
	 * Property file key. This string must appear in the configuration file to
	 * change the setting.
	 * 
	 * @see main.resource.Configuration#NUM_WORKERS
	 */
	private static final String CONFIG_NUM_WORKERS = "num_workers";

	/**
	 * Parse the configuration file given by the input stream and grab values
	 * for each configuration option. Generally, all options should be placed in
	 * the configuration file, so as to not assume unsafe defaults.
	 * 
	 * @param config
	 *            stream representing the config file
	 * @param filename
	 *            the filename used to load the config
	 * @throws IOException
	 */
	public static void Configure(InputStream config, String filename)
			throws IOException {
		FILENAME = filename;

		props = new Properties();
		props.load(config);

		NUM_WORKERS = getInt(CONFIG_NUM_WORKERS);
		if (NUM_WORKERS < 1) {
			/*
			 * means we ask the system for the proc count, which means it will
			 * always be positive (safe defaults)
			 */
			NUM_WORKERS = Runtime.getRuntime().availableProcessors();
		}

		// convert ms to nanoseconds
		NETWORK_LATENCY = 1000000 * getInt(CONFIG_NETWORK_LATENCY);
		SERVERS = getInt(CONFIG_SERVERS);
		EXIT_RELAYS = getInt(CONFIG_EXIT_RELAYS);
		NORMAL_RELAYS = getInt(CONFIG_NORMAL_RELAYS);
		FS_RELAYS = getInt(CONFIG_FS_RELAYS);
		WEB_RELAYS = getInt(CONFIG_WEB_RELAYS);
		WEB_EXIT_RELAYS = getInt(CONFIG_WEB_EXIT_RELAYS);
		WEB_CLIENTS = getInt(CONFIG_WEB_CLIENTS);
		FS_CLIENTS = getInt(CONFIG_FS_CLIENTS);
		FS_EXIT_RELAYS = getInt(CONFIG_FS_EXIT_RELAYS);
		FS_PEERS = getInt(CONFIG_FS_PEERS);
		APPLICATION_STARTUP = getInt(CONFIG_APPLICATION_STARTUP);
		// kbps (bits)
		CLIENT_BANDWIDTH_UP = getInt(CONFIG_CLIENT_BANDWIDTH_UP);
		CLIENT_BANDWIDTH_DOWN = getInt(CONFIG_CLIENT_BANDWIDTH_DOWN);
		FILESHARER_BANDWIDTH_UP = getInt(CONFIG_FILESHARER_BANDWIDTH_UP);
		FILESHARER_BANDWIDTH_DOWN = getInt(CONFIG_FILESHARER_BANDWIDTH_DOWN);
		SEED = getInt(CONFIG_SEED);
		ENDTIME = getInt(CONFIG_ENDTIME);
		THINKTIME_ADJUSTMENT = getDouble(CONFIG_THINKTIME_ADJUSTMENT);

		TICKETS_RATE_LOW_LATENCY = getInt(CONFIG_TICKETS_RATE_LOW_LATENCY);
		TICKETS_RATE_HIGH_THROUGHPUT = getInt(CONFIG_TICKETS_RATE_HIGH_THROUGHPUT);
		FREE_TICKETS_PERIOD = getInt(CONFIG_FREE_TICKETS_PERIOD);
		FREE_TICKETS_AMOUNT = getInt(CONFIG_FREE_TICKETS_AMOUNT);
		CELLS_PER_TICKET = getInt(CONFIG_CELLS_PER_TICKET);
		TICKETS_FS_VIP = getBool(CONFIG_TICKETS_FS_VIP);
		TICKETS_RELAY_BONUS = getInt(CONFIG_TICKETS_RELAY_BONUS);

		// for scheduling
		NETWORK_PRIORITY = getBool(CONFIG_NETWORK_PRIORITY);
		SCHEDULER = getScheduler(CONFIG_SCHEDULER);
		LOW_LATENCY_DDP = getInt(CONFIG_LOW_LATENCY_DDP);
		HIGH_THROUGHPUT_DDP = getInt(CONFIG_HIGH_THROUGHPUT_DDP);
		NORMAL_DDP = getInt(CONFIG_NORMAL_DDP);
		NETWORK_DYNAMIC_BUFFERS = getBool(CONFIG_NETWORK_DYNAMIC_BUFFERS);

		EWMA_INTERVAL = getInt(CONFIG_EWMA_INTERVAL);
		EWMA_SCALE_FACTOR = getDouble(CONFIG_EWMA_SCALE_FACTOR);
		HPD_FRACTION = getDouble(CONFIG_HPD_FRACTION);
		WFQ_INTERVAL = getInt(CONFIG_WFQ_INTERVAL);

		DEBUG = getBool(CONFIG_DEBUG);
	}

	private static double getDouble(String key) {
		return Double.parseDouble(props.getProperty(key, "0.0"));
	}

	/**
	 * Logs the key/value pairs for the configuration currently loaded to the
	 * config level.
	 */
	public static void logConfig() {
		Driver.log.info("Using config file '" + FILENAME + "'");
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		props.list(pw);
		pw.flush();
		Driver.log.config(sw.toString());
	}

	/**
	 * Gets a boolean value associated with the given key in the property file.
	 * 
	 * @param key
	 *            the key to lookup
	 * @return the boolean value in the property file
	 */
	private static boolean getBool(String key) {
		return Boolean.parseBoolean(props.getProperty(key, "False"));
	}

	/**
	 * Gets an int value associated with the given key in the property file.
	 * 
	 * @param key
	 *            the key to lookup
	 * @return the int value in the property file
	 */
	private static int getInt(String key) {
		return Integer.parseInt(props.getProperty(key, "0"));
	}

	/**
	 * Gets a string value associated with the given key in the property file.
	 * Then returns the appropriate algorithm.
	 * 
	 * @param key
	 *            the key to lookup
	 * @return the schedulingAlgorithm the key represents
	 */
	private static SchedulingAlgorithm getScheduler(String key) {
		String scheduler = props.getProperty(key, "");
		SchedulingAlgorithm algorithm = null;
		if (scheduler.equals("HPD")) {
			algorithm = SchedulingAlgorithm.HYBRID_PROPORTIONAL_DELAY;
		} else if (scheduler.equals("FCFS")) {
			algorithm = SchedulingAlgorithm.FIRST_COME_FIRST_SERVED;
		} else if (scheduler.equals("RR")) {
			algorithm = SchedulingAlgorithm.ROUND_ROBIN;
		} else if (scheduler.equals("EWMA")) {
			algorithm = SchedulingAlgorithm.EXPONENTIAL_WEIGHTED_MOVING_AVERAGE;
		} else if (scheduler.equals("WFQ")) {
			algorithm = SchedulingAlgorithm.WEIGHTED_FAIR_QUEUEING;
		}
		return algorithm;
	}

}
