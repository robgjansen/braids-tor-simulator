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
 * $Id: Driver.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.system;

import java.io.FileInputStream;
import java.util.PriorityQueue;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import main.concurrent.Master;
import main.event.Event;
import main.event.Heartbeat;
import main.node.Directory;
import main.resource.Configuration;
import main.resource.Distribution;
import main.scheduling.Scheduler.Priority;
import main.util.Generator;
import main.util.GzipFileHandler;
import main.util.SimulationClock;
import main.util.SimulationFormatter;

/**
 * The main class that initializes logging and the Tor network, and runs
 * experiments. This class hold the main event queue and facilitates global
 * aggregated message statistics.
 * 
 * @author Rob Jansen
 */
public class Driver {
	/**
	 * The current instance of the simulator.
	 */
	private static final Driver DRIVER = new Driver();
	/**
	 * The instance of the logger, used to log messages from anywhere in the
	 * simulator.
	 */
	public static Logger log = Logger.getLogger("simulation");;

	/**
	 * @return the static instance of the Driver
	 */
	public static Driver getInstance() {
		return DRIVER;
	}

	/**
	 * Main entrance point for the simulator. Takes a configuration file as a
	 * parameter.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		DRIVER.drive(args);
	}

	/**
	 * The clock to track simulation time.
	 */
	private SimulationClock clock;
	/**
	 * The number of datagrams in transit in the entire network for each
	 * priority level.
	 */
	private long[] outstandingDataCounters;
	/**
	 * The number of messages in transit in the entire network for each priority
	 * level.
	 */
	private long[] outstandingMessageCounters;
	/**
	 * The main event queue. Prioritizes events based on time.
	 */
	private PriorityQueue<Event> pendingEvents;

	/**
	 * Used in multi-threading mode to manage events and workers.
	 */
	private Master master;
	/**
	 * The wall clock experiment start timestamp.
	 */
	private long startingWallClock;

	private long webConnectionsCount;

	private long fsConnectionsCount;
	
	public boolean generateTraffic;

	/**
	 * The system, containing all nodes and facilitating relay and server
	 * selection.
	 */
	@SuppressWarnings("unused")
	private Directory system;

	/**
	 * Adds an event to the main priority queue.
	 * 
	 * @param event
	 *            the event to add
	 * @return true if the queue changed as a result of the add
	 */
	public boolean addEvent(Event event) {
		// only add event if it will actually get executed
		if(event.getTime() <= clock.getEndTime()){
			/*
			 * We go through the master if we are running multi-threaded for
			 * synchronization.
			 */
			if (Configuration.NUM_WORKERS > 1) {
				master.addWork(event);
			} else {
				pendingEvents.add(event);
			}
		}
		return true;
	}

	/**
	 * Decrements the datagram counter.
	 */
	public synchronized void decrementDataCount(Priority p) {
		outstandingDataCounters[p.ordinal()]--;
	}

	/**
	 * Decrements the message counter.
	 */
	public synchronized void decrementMessageCount(Priority p) {
		outstandingMessageCounters[p.ordinal()]--;
	}

	/**
	 * Initialize config, logging, and the Tor network and run if initialization
	 * was successful.
	 * 
	 * @param args
	 *            an optional config file. if not supplied, default parameters
	 *            will be used for a small experiment.
	 */
	private void drive(String[] args) {
		if (initialize(args)) {
			// if not multi-threading, Driver runs the sim
			if (Configuration.NUM_WORKERS > 1) {
				log.info("Starting master and " + Configuration.NUM_WORKERS
						+ " workers.");
				master.run();
			} else {
				runSimulation();
			}
			generateSummary();
		}
	}

	/**
	 * Logs a final summary of simulation time and wall-clock time.
	 */
	private void generateSummary() {
		log.config(webConnectionsCount + " total web and " + fsConnectionsCount
				+ " total filesharing connections");
		log.info("Total simulation time = "
				+ SimulationClock.getInstance().getTimeAsMinutes() + " of "
				+ Configuration.ENDTIME + " minutes");
		log.info("Total wall time = "
				+ (System.currentTimeMillis() - startingWallClock)
				/ (1000 * 60) + " minutes");
	}

	/**
	 * Computes and prints status information about the current amount of memory
	 * used by the VM, and the current number of messages and datagrams in the
	 * network for each priority level, and number of events in the queue.
	 * Schedules another heartbeat in one minute.
	 */
	public synchronized void generateStatus(long time) {
		long memory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
		long size = 0;
		if (pendingEvents == null) {
			size = master.getEstimatedSize();
		} else {
			size = pendingEvents.size();
		}
		log.info("Heartbeat "
				+ (time / SimulationClock.getInstance().getOneMinute()) + "/"
				+ Configuration.ENDTIME + " minutes " + memory + "MB "
				+ getCounterString(outstandingMessageCounters, " messages ")
				+ getCounterString(outstandingDataCounters, " datagrams ")
				+ size + " events ");
		long oneMinute = SimulationClock.getInstance().getOneMinute();
		addEvent(new Heartbeat(time + oneMinute));
	}

	/**
	 * Compute a string that contains the cumulative total of all values of
	 * counters in the given array.
	 * 
	 * @param counters
	 *            an array of counters for tracking the number of objects for
	 *            each priority level
	 * @param label
	 *            the type of object the counters are counting
	 * @return a string representing the values of each counter for each
	 *         priority level
	 */
	private String getCounterString(long[] counters, String label) {
		long total = 0;
		for (int i = 0; i < counters.length; i++) {
			total += counters[i];
		}
		String result = total + label;
		Priority[] values = Priority.values();
		for (int i = 0; i < values.length; i++) {
			result += counters[values[i].ordinal()] + " " + values[i].name()
					+ " ";
		}
		return result;
	}

	/**
	 * Increments the datagram counter.
	 */
	public synchronized void incrementDataCount(Priority p) {
		outstandingDataCounters[p.ordinal()]++;
	}

	/**
	 * Increments the message counter.
	 */
	public synchronized void incrementMessageCount(Priority p) {
		outstandingMessageCounters[p.ordinal()]++;
	}

	/**
	 * Initializes configuration, logging, traffic and bandwidth distributions,
	 * event queue, clock, random number generator, and the Tor network. Adds
	 * the inital heartbeat event for status updates throughout the experiment.
	 * 
	 * @param args
	 *            an optional config file. If no config file is given, a default
	 *            internal file is used for a small experiment.
	 * @return true if initialization was successful, false otherwise
	 */
	private boolean initialize(String[] args) {
		try {
			if (args.length < 1) {
				String filename = "config_default.properties";
				Configuration.Configure(Configuration.class
						.getResourceAsStream(filename), filename);
			} else {
				Configuration.Configure(new FileInputStream(args[0]), args[0]);
			}
		} catch (Exception e) {
			log.severe(e.toString());
			return false;
		}

		DRIVER.initializeLogger();
		Configuration.logConfig();

		try {
			Distribution.initialize();
		} catch (Exception e) {
			log.severe(e.toString());
			log.severe("Unable to initialize distribution resources");
			return false;
		}

		outstandingDataCounters = new long[Priority.values().length];
		outstandingMessageCounters = new long[Priority.values().length];
		if (Configuration.NUM_WORKERS > 1) {
			master = new Master(Configuration.NUM_WORKERS,
					Configuration.NETWORK_LATENCY);
		} else {
			pendingEvents = new PriorityQueue<Event>();
		}

		clock = SimulationClock.getInstance();
		clock.setEndTime(Configuration.ENDTIME);
		startingWallClock = System.currentTimeMillis();

		Generator.getInstance().init(Configuration.SEED);

		system = new Directory();

		generateTraffic = true;
		/* used to test that messages get drained from the system
		long stopTime = clock.getEndTime()/2;
		addEvent(new Event(stopTime) {
			@Override
			public void run() {
				generateTraffic = false;
			}
			@Override
			public Node getOwner() {
				return null;
			}
		});
		*/
		
		addEvent(new Heartbeat(0));
		
		return true;
	}

	/**
	 * Initialize the logging formatters and handlers. The file loggers will log
	 * to a Gzipped file based on the following config filename convention. A
	 * filename in the form x_y.z will result (if debug config set to true) in
	 * results_y.gz and debug_y.gz. So change y in order to differentiate
	 * experiments using mostly similar configurations.
	 * <p>
	 * The following handlers are available for the simulator.
	 * <ul>
	 * <li>Console handler - logs INFO level and above to console
	 * <li>Debug handler - logs at FINE level and above if the debug setting is
	 * true in the config file, otherwise logs nothing
	 * <li>Config handler - logs at CONFIG level and above - measurements useful
	 * for compiling results.
	 * </ul>
	 * 
	 */
	private void initializeLogger() {
		log.setUseParentHandlers(false);
		log.setLevel(Level.ALL);
		try {
			int start = 0;
			int end = Configuration.FILENAME.indexOf('.');
			if (end < 0) {
				end = Configuration.FILENAME.length() - 1;
			}
			String logfileHelper = Configuration.FILENAME.substring(start, end);

			// create custom formatter
			SimulationFormatter sf = new SimulationFormatter();

			if (Configuration.DEBUG) {
				// log everything to a debug file - no append
				GzipFileHandler debug = new GzipFileHandler("debug"
						+ logfileHelper + ".gz");
				debug.setFormatter(sf);
				debug.setLevel(Level.FINE);
				log.addHandler(debug);
			}

			GzipFileHandler results = new GzipFileHandler(logfileHelper
					+ ".log.gz");
			results.setFormatter(sf);
			results.setLevel(Level.CONFIG);
			log.addHandler(results);

			// only log info to the console
			ConsoleHandler ch = new ConsoleHandler();
			ch.setFormatter(sf);
			ch.setLevel(Level.INFO);
			log.addHandler(ch);

			log.info("Starting simulation -- logger initialized");
		} catch (Exception e) {
			// error setting up loggers, use parent as default
			log.setUseParentHandlers(true);
			log.warning(e.toString());
			log.warning("Using default formatter");
		}
	}

	/**
	 * Runs the simulator in a while loop until the configured end time is
	 * reached, polling and executing events from the queue. Error conditions
	 * occur if the event queue empties during simulation or an old event is
	 * attempting to be executed.
	 */
	private void runSimulation() {
		while (true) {
			Event event = pendingEvents.poll();
			if (event == null) {
				log.severe("The event queue has emptied during simulation.");
				return;
			}

			if (event.getTime() < clock.getTimeAsNanoseconds()) {
				log.severe("The event happened in the past.");
				return;
			}

			// if this wind expires the time, don't execute the event
			clock.set(event.getTime());
			if (clock.isExpired()) {
				break;
			}
			event.run();
		}
	}

	public synchronized void incrementWebConnectionCount() {
		webConnectionsCount++;
	}

	public synchronized void incrementFSConnectionCount() {
		fsConnectionsCount++;
	}

}
