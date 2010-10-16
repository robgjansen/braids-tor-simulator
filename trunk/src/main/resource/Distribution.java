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
 * $Id: Distribution.java 953 2010-10-13 04:38:52Z jansen $
 */
package main.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

import main.system.Driver;
import main.util.Generator;
import main.util.SimulationClock;

/**
 * Imports data files and creates distribution maps for web traffic data
 * generation times and relay bandwidth distribution. This class stores these
 * distributions and can sample them by calling the sample method and giving the
 * distribution type.
 * 
 * @author Rob Jansen
 */
public class Distribution {
	/**
	 * The type of possible data distributions used by the simulator. Each type
	 * has an associated filename containing the cdf from which the data is
	 * drawn.
	 * 
	 * @author Rob Jansen
	 */
	public enum DistributionType {
		IDLE_TIME("unctraffic/unc03.think_times.cdf.gz"), 
		OBJECT_REQUEST_SIZE("unctraffic/unc03.emb_req_sizes.cdf.gz"), 
		OBJECT_RESPONSE_SIZE("unctraffic/unc03.emb_rsp_sizes.cdf.gz"), 
		OBJECTS_PER_PAGE("unctraffic/unc03.obj_page.cdf.gz"), 
		PAGE_REQUEST_SIZE("unctraffic/unc03.top_req_sizes.cdf.gz"), 
		PAGE_RESPONSE_SIZE("unctraffic/unc03.top_rsp_sizes.cdf.gz"), 
		RELAY_BANDWIDTH("relay_bandwidth.dat.gz");

		/**
		 * The filename containing this distribution CDF data.
		 */
		private final String filename;

		/**
		 * Creates a new distribution from the given filename.
		 * 
		 * @param filename
		 *            the name of the file from which to import data
		 */
		DistributionType(String filename) {
			this.filename = filename;
		}

		/**
		 * @return the filename of this distribution type
		 */
		public String getFilename() {
			return filename;
		}
	}

	/**
	 * Stores all the distributions keyed by the type. This allows easy
	 * retrieval of a requested distribution type during the experiment.
	 */
	private static Hashtable<DistributionType, TreeMap<Double, Integer>> distributions = new Hashtable<DistributionType, TreeMap<Double, Integer>>();
	/**
	 * Flag to prevent importing all the files multiple times.
	 */
	private static boolean isInitialized = false;

	/**
	 * Initialize every distribution type by iterating through the set of types
	 * and importing data from each file into its own table. Each table is
	 * stored in the distributions table for later retrieval.
	 * 
	 * @throws IOException
	 *             in case an error in reading a file occurs
	 * @see main.resource.Distribution.DistributionType
	 */
	public static void initialize() throws IOException {
		if (isInitialized) {
			Driver.log.warning("Distribution already initialized");
			return;
		}

		// read in all distribution data from files and store in master table
		for (DistributionType d : DistributionType.values()) {
			InputStream istream = Distribution.class.getResourceAsStream(d.getFilename());
			InputStream gzistream = new GZIPInputStream(istream);
			BufferedReader in = new BufferedReader(new InputStreamReader(gzistream));

			TreeMap<Double, Integer> map = new TreeMap<Double, Integer>();

			String line;
			while ((line = in.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}
				String[] parts = line.split(" ");
				map.put(Double.valueOf(parts[1]), Integer.valueOf(parts[0]));
			}

			distributions.put(d, map);
		}

		isInitialized = true;
		Driver.log.info(distributions.size()
				+ " distributions initialized successfully");
	}

	/**
	 * Draw a sample from the stored map (CDF) for the given distribution type.
	 * 
	 * @param type
	 *            the type of distribution to sample
	 * @return the integer sampled from the map (CDF)
	 * 
	 * @see main.resource.Distribution#sampleMap(TreeMap)
	 * @see main.resource.Distribution.DistributionType
	 */
	public static int sample(DistributionType type) {
		TreeMap<Double, Integer> map = distributions.get(type);
		return (Integer) sampleMap(map);
	}

	/**
	 * Samples the given map by drawing a random double and retrieving the key
	 * in the map closest to that double value. To accurately represent a CDF,
	 * we prefer the map key closest to the ceiling of the random double.
	 * 
	 * @param map
	 *            the TreeMap to sample
	 * @return the map object value associated with the randomly drawn key
	 */
	public static Object sampleMap(TreeMap<Double, ? extends Object> map) {
		// draw randomly from the given map
		double key = Generator.getInstance().getPrng().nextDouble();

		// return closest value to the randomly generated key
		Double ceilingKey = map.ceilingKey(key);
		Double floorKey = map.floorKey(key);

		// check if both keys are present
		if ((ceilingKey == null) && (floorKey == null)) {
			Driver.log.severe("No non-null entry for distribution sample");
		} else if (ceilingKey == null) {
			return map.get(floorKey);
		} else if (floorKey == null) {
			return map.get(ceilingKey);
		}

		// we have both keys, use ceiling key to accurately reflect cdf
		// probability
		return map.get(ceilingKey);
	}

	/**
	 * Draws a random sample from the Pareto distribution with params x_m = 1/3
	 * and k = 3/2
	 * 
	 * @return the Pareto sample converted to nanoseconds
	 */
	public static int samplePareto() {
		int delay = (int) (1f / 3f / Math.pow(Generator.getInstance().getPrng()
				.nextDouble(), (2f / 3f)) * SimulationClock.getInstance()
				.getOneSecond());
		return delay;
	}

	/**
	 * Private constructor. All distribution methods should be accessed
	 * statically.
	 */
	private Distribution() {
	}
}
