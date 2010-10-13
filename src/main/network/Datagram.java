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
 * $Id: Datagram.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.network;

import main.system.Driver;

/**
 * The most generic kind of data an application can generate.
 * 
 * @author Rob Jansen
 */
public class Datagram {

	/**
	 * The cell size will be padded to this length if data is too small.
	 */
	public static final int CELL_LENGTH = 512;

	/**
	 * The maxmimum size for the payload (size - command size - circuit id size)
	 */
	public static final int MAX_PAYLOAD_LENGTH = CELL_LENGTH - 14;

	/**
	 * The channel transporting the data. This is updated as each hop "fowards"
	 * the data along the path.
	 */
	private HalfDuplexChannel channel;

	/**
	 * If this data is currently encrypted in a cell
	 */
	private boolean isCell;

	/**
	 * The unencrypted (non-cell) size of this data piece in bytes.
	 */
	private int pieceSize;

	/**
	 * The time the data arrived in the most recent buffer it was stored.
	 */
	private long queueArrivalTime;

	/**
	 * The server's reply this data is part of, which may be null if the data is
	 * being transferred from the client to the server.
	 */
	private Reply reply;

	/**
	 * The client's request this data is a part of.
	 */
	private Request request;

	/**
	 * A phantom datagram does nothing but take up bandwidth, causing the link
	 * to be idle for a sending period. It has no destination or useful
	 * information.
	 */
	private boolean isPhantom;

	/**
	 * Constructs a datagram associated with the given request, with the given
	 * cell status and given size. This data is travelling from the client to
	 * the server.
	 * 
	 * @param request
	 *            the request associated with this data
	 * @param isCell
	 *            if this data should be a cell
	 * @param pieceSize
	 *            the regular, unencrypted data size
	 */
	public Datagram(Request request, boolean isCell, int pieceSize) {
		if (pieceSize > MAX_PAYLOAD_LENGTH) {
			Driver.log
					.severe("Attempting to create a cell with a payload that is too large");
		}
		this.pieceSize = pieceSize;
		this.request = request;
		this.isCell = isCell;
		isPhantom = false;
		queueArrivalTime = 0;
	}

	/**
	 * Constructs a datagram associated with the given request and reply, with
	 * the given cell status and given size. This data is travelling from the
	 * server to the client.
	 * 
	 * @param request
	 *            the request associated with this data
	 * @param reply
	 *            the reply associated with this data
	 * @param isCell
	 *            if this data should be a cell
	 * @param pieceSize
	 *            the regular, unencrypted data size
	 */
	public Datagram(Request request, Reply reply, boolean isCell, int pieceSize) {
		if (pieceSize > MAX_PAYLOAD_LENGTH) {
			Driver.log
					.severe("Attempting to create a reply data with a payload that is too large");
		}
		this.pieceSize = pieceSize;
		this.request = request;
		this.reply = reply;
	}

	/**
	 * @return the nextHop
	 */
	public HalfDuplexChannel getChannel() {
		return channel;
	}

	/**
	 * @return the reply if it is not null, otherwise the request
	 */
	public Message getMessage() {
		if (reply != null) {
			return reply;
		} else {
			return request;
		}
	}

	/**
	 * @return the pieceSize
	 */
	public int getPieceSize() {
		return pieceSize;
	}

	/**
	 * @return the queueArrivalTime
	 */
	public long getQueueArrivalTime() {
		return queueArrivalTime;
	}

	/**
	 * @return the reply
	 */
	public Reply getReply() {
		return reply;
	}

	/**
	 * @return the request
	 */
	public Request getRequest() {
		return request;
	}

	public int getSize() {
		if (isCell) {
			return CELL_LENGTH;
		} else {
			return pieceSize;
		}
	}

	/**
	 * @return the isCell
	 */
	public boolean isCell() {
		return isCell;
	}

	/**
	 * @param isCell
	 *            the isCell to set
	 */
	public void setCell(boolean isCell) {
		this.isCell = isCell;
	}

	/**
	 * @param nextHop
	 *            the nextHop to set
	 */
	public void setChannel(HalfDuplexChannel nextHop) {
		channel = nextHop;
	}

	/**
	 * @param queueArrivalTime
	 *            the queueArrivalTime to set
	 */
	public void setQueueArrivalTime(long queueArrivalTime) {
		this.queueArrivalTime = queueArrivalTime;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getMessage().toString();
	}

	/**
	 * @return true if this is phantom data, false otherwise
	 */
	public boolean isPhantom() {
		return isPhantom;
	}

	/**
	 * Sets this data as phantom data, which means it will encur bandwidth by
	 * the sender, but not be received by anyone.
	 */
	public void setPhantom() {
		this.isPhantom = true;
	}

}
