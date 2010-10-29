 Copyright 2010 Rob Jansen

This file is part of braids-tor-simulator.

braids-tor-simulator is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

braids-tor-simulator is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with braids-tor-simulator. If not, see http://www.gnu.org/licenses/.

****************
* Introduction *
****************
This discrete-event-based simulator simulates Tor and BRAIDS.

**********************
* Build Instructions *
**********************
Build the program jar by running:
$ ant jar

Build the documentation by running:
$ ant javadoc

Run the jar with:
$ java -jar config.file
where config.file is a copy of the configuration file (config_default.properties can be found in src/main/resource). If no config file is given, the default config will be used. All config options are documented in src/main/resource/Configuration.java, and can be read in hte source file or after generating the documentation.

*************************************************
* High Level Documentation for BRAIDS Simulator *
*************************************************
Rob Jansen
jansen<at>cs.umn.edu

This discrete-event-based simulator simulates Tor and BRAIDS by simulating cells as they travel through the network, measuring performance in terms of round-trip delay and throughput. We simulate applications that generate requests, including a file-sharing application and web browser. This data is transferred through circuits created by the client, forwarded at each hop. The server will process a request after receiving the entire message, and return a reply back through the circuit as specified in the request. Measurements are taken at the client upon receiving a reply from the server.

All actions in the simulator are represented as events. The main events manage data generation and network functionality.

Applications generate data as follows:
Web Browsers:
    Generate top-level page requests and upon receiving a reply, issue several more embedded object requests. Measurements are done after all embedded objects are replied.
File Sharers:
    Connect to several peers and continuously exchange blocks (requests/replies).
    
Data at each hop goes through multiple layers:

*************
*Application*
*************     *************     *************     *************
*  Client   *     *   Relay   *     *   Relay   *     *   Relay   *
*************     *************     *************     *************     *************
*  TorNode  *     *  TorNode  *     *  TorNode  *     *  TorNode  *     *  Server   *
*************     *************     *************     *************     *************
*  Network  *<--->*  Network  *<--->*  Network  *<--->*  Network  *<--->*  Network  *
*************     *************     *************     *************     *************

The application generates data and pushes it down to the client. The client ensures a circuit exists, splits application requests into cells, and computes priority for each cell. These cells are then transferred to the TorNode, where they wait in buffers to be scheduled by the network. Each network is limited by its upload/download bandwidth, and message send/receive events will be delayed accordingly. When a relay receives data, it processes it by setting the next hop and again sending it to wait in the buffers for network processing. The last relay in the path strips off the cells. The server receives data and, once it can reconstruct an entire request, generates replies as specified by the request. The data transfers back to the client, where measurements are taken and logged.

Each node implements a scheduling algorithm in order to make scheduling decisions. All servers implement FirstComeFirstServed, and Tor relays use with RoundRobin, or WaitingTimePriority, as specified in the configuration file.

