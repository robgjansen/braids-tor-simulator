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
 * $Id: Request.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.network;

import main.application.TorApplication;
import main.node.Server;
import main.scheduling.Scheduler.Priority;

public class Request extends Message {

	/**
	 * Flag if this is a top-level web page request
	 */
	private boolean isWebPageRequest;
	/**
	 * Bytes requested, i.e. the size of the reply
	 */
	private int requestedDataSize;

	/**
	 * Creates a request, initially setting its priority to NORMAL. This can be
	 * changed later if needed.
	 * 
	 * @param application
	 *            the application generating this request
	 * @param server
	 *            the destination for this request
	 * @param size
	 *            number of bytes of this request
	 * @param requestedDataSize
	 *            number of bytes requested, i.e. the size of the reply
	 * @param isWebPageRequest
	 *            if this request is a top level web page request
	 */
	public Request(long time, TorApplication application, Server server, int size,
			int requestedDataSize, boolean isWebPageRequest) {
		super(time, application, server, size);
		this.requestedDataSize = requestedDataSize;
		this.isWebPageRequest = isWebPageRequest;
		setPriority(Priority.NORMAL);
	}

	/**
	 * @return the bytes requested, i.e. the size of the reply
	 */
	public int getRequestedDataSize() {
		return requestedDataSize;
	}

	/**
	 * @return true if this request is a top level web page request
	 */
	public boolean isPageRequest() {
		return isWebPageRequest;
	}

}
