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
 * $Id: GzipFileHandler.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.zip.GZIPOutputStream;

/**
 * A file logging handler that logs messages to a Gzipped file.
 * 
 * @author Rob Jansen
 */
public class GzipFileHandler extends FileHandler {

	/**
	 * Create a new FileHandler and sets the output stream to a GZIPOutputStream
	 * constructed from the given filename.
	 * 
	 * @param filename
	 *            the name of the file to which this handler will log.
	 * @throws IOException
	 * @throws SecurityException
	 */
	public GzipFileHandler(String filename) throws IOException,
			SecurityException {
		setOutputStream(new GZIPOutputStream(new FileOutputStream(filename)));
	}

}
