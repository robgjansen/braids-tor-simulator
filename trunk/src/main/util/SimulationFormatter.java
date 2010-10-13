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
 * $Id: SimulationFormatter.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * Formats a summary of a log record. Slightly customized the output line to
 * include things I wanted.
 * 
 * @author Rob Jansen
 */
public class SimulationFormatter extends SimpleFormatter {
	private final static String format = "{0,date} {0,time}";
	private Object args[] = new Object[1];
	Date dat = new Date();
	private MessageFormat formatter;
	private String lineSeparator = "\n";

	@Override
	public synchronized String format(LogRecord record) {
		StringBuffer sb = new StringBuffer();
		// Minimize memory allocations here.
		dat.setTime(record.getMillis());
		args[0] = dat;
		StringBuffer text = new StringBuffer();
		if (formatter == null) {
			formatter = new MessageFormat(format);
		}
		formatter.format(args, text, null);
		sb.append(text);
		sb.append(" ");
		if (record.getSourceClassName() != null) {
			sb.append(record.getSourceClassName());
		} else {
			sb.append(record.getLoggerName());
		}
		if (record.getSourceMethodName() != null) {
			sb.append(" ");
			sb.append(record.getSourceMethodName());
		}
		sb.append(" ");
		sb.append(SimulationClock.getInstance().getTimeAsNanoseconds());
		sb.append(" @");
		String message = formatMessage(record);
		sb.append(record.getLevel().getLocalizedName());
		sb.append(": ");
		sb.append(message);
		sb.append(lineSeparator);
		if (record.getThrown() != null) {
			try {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				record.getThrown().printStackTrace(pw);
				pw.close();
				sb.append(sw.toString());
			} catch (Exception ex) {
			}
		}
		return sb.toString();
	}
}
