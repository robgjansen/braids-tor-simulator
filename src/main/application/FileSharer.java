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
 * $Id: FileSharer.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.application;

import java.util.ArrayList;
import java.util.HashMap;

import main.event.OptimisticUnchoke;
import main.network.Circuit;
import main.network.Reply;
import main.network.Request;
import main.node.Client;
import main.node.Directory;
import main.node.Server;
import main.resource.Configuration;
import main.scheduling.Scheduler.Priority;
import main.system.Driver;
import main.util.SimulationClock;

/**
 * A BitTorrent-like application that exchanges blocks with peers through the
 * Tor network.
 * <p>
 * The official protocol is located at:
 * http://www.bittorrent.org/beps/bep_0003.html
 * <p>
 * BitTorrent splits files into pieces of 256K each. BT block requests of 32K <=
 * x <= 128K are made for data making up a piece. This means multiple block
 * requests are required for a single piece. Additionally, it is recommended to
 * keep several requests in the queue, based on the bandwidth-delay product, to
 * maximize throughput.
 * <p>
 * The request message header is 13 bytes piece message header is 9 bytes +
 * block payload. Other values: btBlockSize = 32768; btPieceSize = 262144;
 * blocksPerPiece = btPieceSize / btBlockSize;
 * 
 * @author Rob Jansen
 */
public class FileSharer extends TorApplication {
	/**
	 * The size of a single block in BitTorrent.
	 */
	private final int btBlockSize = 32768;
	/**
	 * A list of all the Servers this application is communicating with.
	 */
	private ArrayList<Server> peers;

	/**
	 * Create a new BitTorrent application.
	 * 
	 * @param directory
	 *            the Tor network through which the application will communicate
	 * @param client
	 *            the client running the application
	 */
	public FileSharer(Directory directory, Client client) {
		super(client, directory);
		peers = new ArrayList<Server>(6);
	}

	/**
	 * Implements the "optimistic unchoke" algorithm of BitTorrent by refreshing
	 * the peer's circuit with the slowest node, as computed by local
	 * performance in terms of the number of datagrams received. This method
	 * chains another OptimisticUnchoke for this application in another 30
	 * seconds.
	 */
	public void doOptimisticUnchoke(long time) {
		HashMap<Server, Circuit> circuits = getClient().getCircuits();
		Circuit slowest = null;
		for (Server peer : peers) {
			Circuit c = circuits.get(peer);
			if (c != null) {
				if ((slowest == null)
						|| (c.getDatagramCount() < slowest.getDatagramCount())) {
					slowest = c;
				}
				// reset counters for next iteration
				c.resetDatagramCount();
			}
		}

		if (slowest != null) {
			// this causes an updated server and a new path
			client.refreshCircuit(slowest);
			// this is a new connection
			Driver.getInstance().incrementFSConnectionCount();
		}

		// do another unchoke later
		long delay = SimulationClock.getInstance().getOneSecond() * 30;
		Driver.getInstance().addEvent(new OptimisticUnchoke(time + delay, this));
	}

	/**
	 * Exchanges a block with a peer by generating a block-sized request for a
	 * block-sized reply.
	 * 
	 * @see main.application.TorApplication#generateRequest(main.node.Server)
	 */
	@Override
	public void generateRequest(long time, Server peer) {
		if(!Driver.getInstance().generateTraffic){
			return;
		}
		// our client will make request another block
		Request request = new Request(time, this, peer, btBlockSize, btBlockSize,
				false);

		client.send(time, request, Priority.HIGH_THROUGHPUT);
	}

	/**
	 * Receives a reply from the client. Logs the round trip time of the
	 * request/reply combination and chains another request generation.
	 * 
	 * @see main.application.TorApplication#receive(main.network.Reply)
	 */
	@Override
	public void receive(long time, Reply reply) {
		// we are finished downloading an entire reply
		long rtt = computeRtt(time, reply.getRequest().getCreationTimestamp());
		Driver.log.config(toString() + " " + reply.toString()
				+ " rtt measurement: " + reply.getRequest().getSize()
				+ " bytes uploaded and " + reply.getSize()
				+ " bytes downloaded in " + rtt + " milliseconds");

		Server peer = reply.getServer();

		// exchange another piece
		generateRequest(time, peer);
	}

	/**
	 * Starts the application by creating main.resources.Configuration.BT_PEERS
	 * number of random servers to act as peers, and starts the generation
	 * process by generating a request for each. Also starts the optimistic
	 * unchoke process by scheduling the first unchoke event in 30 seconds.
	 * 
	 * @see main.application.TorApplication#start()
	 */
	@Override
	public void start(long time) {
		// start transfers with multiple peers
		for (int i = 0; i < Configuration.FS_PEERS; i++) {
			// we want mutually exclusive peers
			Server newpeer = null;
			while ((newpeer == null) || peers.contains(newpeer)) {
				newpeer = getDirectory().getRandomServer();
			}
			peers.add(newpeer);
			generateRequest(time, newpeer);

			// this is a new connection
			Driver.getInstance().incrementFSConnectionCount();
		}
		long delay = SimulationClock.getInstance().getOneSecond() * 30;
		Driver.getInstance()
				.addEvent(new OptimisticUnchoke(time + delay, this));
	}
}