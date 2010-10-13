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
 * $Id: Master.java 948 2010-10-12 03:33:57Z jansen $
 */
package main.concurrent;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import main.event.Event;
import main.node.Node;
import main.util.SimulationClock;

/**
 * Multi-threaded version of the simulator. Any two nodes can be run at the same
 * time, but all events for each node are run sequentially. Events are run in
 * time windows, and never ahead of the configured runaheadTime window.
 * 
 * @author rob
 */
public class Master implements Runnable {
	private SimulationClock clock;

	private Map<Node, Queue<Event>> register;
	private Queue<Event> futureWork;
	private Queue<Runnable> currentWork;

	private Semaphore workPermits;
	private CountDownLatch updateGate;
	private long positionTime;
	private long runaheadTime;
	private long leashTime;

	private List<Worker> workers;
	private AtomicInteger waitingWorkers;

	public Master(int numWorkers, long runaheadTime) {
		clock = SimulationClock.getInstance();

		this.runaheadTime = runaheadTime;
		positionTime = 0;
		leashTime = 0;

		futureWork = new PriorityBlockingQueue<Event>();
		currentWork = new ConcurrentLinkedQueue<Runnable>();
		register = new ConcurrentHashMap<Node, Queue<Event>>();

		workPermits = new Semaphore(0);
		updateGate = new CountDownLatch(0);
		waitingWorkers = new AtomicInteger(numWorkers);

		workers = new LinkedList<Worker>();
		for (int i = 0; i < numWorkers; i++) {
			workers.add(new Worker(this, workPermits));
		}
	}

	public long getEstimatedSize() {
		return currentWork.size() + futureWork.size();
	}

	public void run() {
		// start all workers
		for (Worker worker : workers) {
			new Thread(worker).start();
		}

		while (true) {
			// wait until all workers are finished
			try {
				updateGate.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}

			// stop the world to set up next window
			synchronized (this) {
				// jump ahead to next event time
				Event event = futureWork.peek();
				positionTime = event.getTime();
				leashTime = positionTime + runaheadTime;
				clock.set(positionTime);

				// check if the sim is over
				if (clock.isExpired()) {
					break;
				}

				// add work for the current window
				while (event != null && event.getTime() < leashTime
						&& event.getTime() < clock.getEndTime()) {
					addWork(futureWork.remove());
					event = futureWork.peek();
				}

				// we have work to do and will wait until its done
				updateGate = new CountDownLatch(1);
			}
		}

		// stop all workers
		for (Worker worker : workers) {
			worker.fire();
		}
		workPermits.release(workers.size());
	}

	protected void workDone() {
		int waiting = waitingWorkers.incrementAndGet();
		if (waiting == workers.size() && currentWork.isEmpty()) {
			// release the master so it adds more work
			updateGate.countDown();
		}
	}

	public void addWork(Event event) {
		if (event.getTime() >= leashTime) {
			// add to future work
			futureWork.add(event);
		} else {
			// dynamically add based on event owner
			Node owner = event.getOwner();
			if (owner == null) {
				// no owner means we can run it anywhere
				currentWork.add(event);
				workPermits.release();
			} else {
				// we have an owner - get its registered work
				Queue<Event> work = register.get(owner);
				if (work == null) {
					// has no work registered, so register an entry
					work = new PriorityBlockingQueue<Event>();
					work.add(event);
					register.put(owner, work);
					// this owner gets its own slave task
					Slave task = new Slave(this, owner);
					currentWork.add(task);
					workPermits.release();
				} else {
					// already registered, so add to existing work
					work.add(event);
				}
			}
		}
	}

	protected Runnable getWork() {
		waitingWorkers.decrementAndGet();
		return currentWork.poll();
	}

	protected synchronized Runnable getSlaveTask(Node owner) {
		Queue<Event> work = register.get(owner);
		if (work == null) {
			return null;
		} else if (work.isEmpty()) {
			// we need to remove the queue from the register so we know when to
			// create a new slave when more work gets added
			register.remove(owner);
			return null;
		} else {
			return work.remove();
		}
	}

}
