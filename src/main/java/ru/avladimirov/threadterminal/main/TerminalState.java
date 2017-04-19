package ru.avladimirov.threadterminal.main;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import static ru.avladimirov.threadterminal.main.Terminals.getLogger;

/**
 * Contains info about hosting {@link ThreadTerminal} inner state: queue length,
 * pending/active/finished tasks' count, hosting threadTerminal name and
 * poolSize. All methods are synchronized.
 * <br>
 * The class is constructed in such a way, that any {@link ThreadTerminal}
 * should use only a single instance of it, and at any given moment provide a
 * copy, a snapshot of it, that is not backed by the original instance. So, you
 * can never change the inner state of the terminal from outside, only the
 * terminal itself can do this.
 * <br>
 * The main requirement when using this class is to provide atomicity during
 * writing terminal state to it. Thus all public and package private methods are
 * made synchronized, but that's not enough. Changing terminal's inner state
 * (submitting new tasks, finishing active ones, ...) should be done atomically
 * with registering these changes in its {@link ThreadTerminal} instance.
 *
 * @author Vladimirov.A.A
 */
public class TerminalState implements Cloneable {

	private final String terminalName;
	private final int poolSize;
	private long finishedTasksCount = 0;
	private long activeTasksCount = 0;
	private long pendingTasksCount = 0;
	private long queuedTasksCount = 0;
	private TreeMap<Long, String> last100finishedTasksNames = new TreeMap<> ();
	private LinkedHashMap<String, Long> activeTasksNames = new LinkedHashMap<> ();
	private LinkedHashMap<String, Long> pendingTasksNames = new LinkedHashMap<> ();

	//A count of currently executing usual tasks
	private int activeUsuals = 0;
	//A count of currently executing exclusive tasks
	private int activeExclusives = 0;

	//for changing some inner variables outside the class body
	private Lock innerLock = new ReentrantLock ();

	private boolean selfTest = false;

	private LinkedHashMap<TerminalState, Long> testStateMap;

	/**
	 * An inner constructor used solely for the purpose of cloning.
	 *
	 * @param terminalName from the object that calls this method
	 * @param poolSize from the object that calls this method
	 * @param finishedTasksCount from the object that calls this method
	 * @param activeTasksCount from the object that calls this method
	 * @param pendingTasksCount from the object that calls this method
	 * @param queuedTasksCount from the object that calls this method
	 * @param last100finishedTasksNames from the object that calls this method
	 * @param activeTasksNames from the object that calls this method
	 * @param pendingTasksNames from the object that calls this method
	 * @param next100pendingTasksNames from the object that calls this method
	 */
	private TerminalState (
			String terminalName,
			int poolSize,
			long finishedTasksCount,
			long activeTasksCount,
			long pendingTasksCount,
			long queuedTasksCount,
			TreeMap<Long, String> last100finishedTasksNames,
			LinkedHashMap<String, Long> activeTasksNames,
			LinkedHashMap<String, Long> pendingTasksNames,
			int activeUsuals,
			int activeExclusives,
			boolean selfTest
	) {
		this.terminalName = terminalName;
		this.poolSize = poolSize;
		this.finishedTasksCount = finishedTasksCount;
		this.activeTasksCount = activeTasksCount;
		this.pendingTasksCount = pendingTasksCount;
		this.queuedTasksCount = queuedTasksCount;
		this.last100finishedTasksNames = (TreeMap<Long, String>) last100finishedTasksNames;
		this.activeTasksNames = activeTasksNames;
		this.pendingTasksNames = pendingTasksNames;
		this.activeUsuals = (activeUsuals);
		this.activeExclusives = (activeExclusives);
		this.selfTest = selfTest;
		this.testStateMap = null;
	}

	/**
	 * A standard constructor to be used inside the {@link ThreadTerminal}.
	 *
	 * @param terminalName of the hosting terminal
	 * @param poolSize of the hosting terminal
	 */
	TerminalState (String terminalName, int poolSize, boolean selfTest) {
		this.terminalName = terminalName;
		this.poolSize = poolSize;
		this.selfTest = selfTest;
		if (selfTest) {
			testStateMap = new LinkedHashMap<> ();
			testStateMap.put (this.clone (), System.nanoTime ());
			this.printStateCounts ("");
		}

	}

	public synchronized String getTerminalName () {
		return terminalName;
	}

	public synchronized int getPoolSize () {
		return poolSize;
	}

	public synchronized long getFinishedTasksCount () {
		return finishedTasksCount;
	}

	public synchronized long getActiveTasksCount () {
		return activeTasksCount;
	}

	public synchronized long getPendingTasksCount () {
		return pendingTasksCount;
	}

	public synchronized TreeMap<Long, String> getLast100finishedTasksNames () {
		return last100finishedTasksNames;
	}

	public synchronized Map<String, Long> getActiveTasksNames () {
		return activeTasksNames;
	}

	public synchronized Map<String, Long> getPendingTasksNames () {
		return pendingTasksNames;
	}

	public synchronized long getQueuedTasksCount () {
		return queuedTasksCount;
	}

	synchronized void setQueuedTasksCount (long queuedTasksCount) {
		this.queuedTasksCount = queuedTasksCount;
	}

	/**
	 * The clone is not backed up by the original object. All complex inner
	 * objects like maps and lists are also cloned.
	 *
	 * @return the clone of the calling object.
	 */
	@Override
	public synchronized TerminalState clone () {
		return new TerminalState (
				terminalName,
				poolSize,
				finishedTasksCount,
				activeTasksCount,
				pendingTasksCount,
				queuedTasksCount,
				(TreeMap<Long, String>) last100finishedTasksNames.clone (),
				(LinkedHashMap<String, Long>) activeTasksNames.clone (),
				(LinkedHashMap<String, Long>) pendingTasksNames.clone (),
				activeUsuals,
				activeExclusives,
				selfTest
		);
	}

	//TODO implement map size controlling: no more 100 tasks
	synchronized void registerNewFinished (String name, boolean exclusive) {
		//tests
		TerminalState stateBefore = null;
		if (selfTest) {
			stateBefore = this.clone ();
		}

		while (last100finishedTasksNames.size () > 99) {
			last100finishedTasksNames.pollFirstEntry ();
		}
		activeTasksNames.remove (name);
		if (exclusive) {
			activeExclusives--;
		} else {
			activeUsuals--;
		}

		last100finishedTasksNames.put (System.nanoTime (), name);
		finishedTasksCount++;

		updateInnerCounters ();

		//tests
		if (selfTest) {
			TerminalState stateAfter = this.clone ();
			testStateMap.put (stateAfter, System.nanoTime ());
			this.printStateCounts ("new finished " + name);
			if (stateBefore.getFinishedTasksCount () != stateAfter.getFinishedTasksCount () - 1) {
				getLogger ().error ("atomicity break: the count of finished differs");
			}
		}
	}

	synchronized void registerNewPending (String name) {
		//tests
		TerminalState stateBefore = null;
		if (selfTest) {
			stateBefore = this.clone ();
		}

		pendingTasksNames.put (name, System.currentTimeMillis ());
		updateInnerCounters ();
		queuedTasksCount--;

		//tests
		if (selfTest) {
			TerminalState stateAfter = this.clone ();
			testStateMap.put (stateAfter, System.nanoTime ());
			this.printStateCounts ("new pending " + name);
			if (stateBefore.getPendingTasksCount () != stateAfter.getPendingTasksCount () - 1) {
				getLogger ().error ("atomicity break: the count of pending differs");
			}
		}
	}

	synchronized void registerNewActive (String name, boolean exclusive) {
		//tests
		TerminalState stateBefore = null;
		if (selfTest) {
			stateBefore = this.clone ();
		}
		activeTasksNames.put (name, System.nanoTime ());
		if (exclusive) {
			activeExclusives++;
		} else {
			activeUsuals++;
		}
		pendingTasksNames.remove (name);
		updateInnerCounters ();

		//tests
		if (selfTest) {
			TerminalState stateAfter = this.clone ();
			testStateMap.put (stateAfter, System.nanoTime ());
			this.printStateCounts ("new active " + name);
			if (stateBefore.getActiveTasksCount () != stateAfter.getActiveTasksCount () - 1) {
				getLogger ().error ("atomicity break: the count of active differs");
			}
		}
	}

	synchronized void registerNewNext (String name) {
		//tests
		TerminalState stateBefore = null;
		if (selfTest) {
			stateBefore = this.clone ();
		}

		queuedTasksCount++;

		//tests
		if (selfTest) {
			TerminalState stateAfter = this.clone ();
			testStateMap.put (stateAfter, System.nanoTime ());
			this.printStateCounts ("new next " + name);
			if (stateBefore.getQueuedTasksCount () != stateAfter.getQueuedTasksCount () - 1) {
				getLogger ().error ("atomicity break: the count of queued differs");
			}
		}
	}

	private void updateInnerCounters () {
		activeTasksCount = activeTasksNames.size ();
		pendingTasksCount = pendingTasksNames.size ();
	}

	synchronized void printStateCounts (String preString) {
		String last = "";
		if (!last100finishedTasksNames.isEmpty ()) {
			last = last100finishedTasksNames.lastEntry ().getValue ();
		}

		getLogger ().debug (terminalName + ", " + preString + " | queued " + queuedTasksCount
				+ "; pending " + pendingTasksCount + " (" + pendingTasksNames.keySet () + ")"
				+ "; active " + activeTasksCount + " (" + activeTasksNames.keySet () + ")"
				+ "; (usuals " + activeUsuals
				+ "; exclusives " + activeExclusives
				+ ") finished " + finishedTasksCount + " (" + last + ")"
		);
	}

	public synchronized int getActiveUsuals () {
		return activeUsuals;
	}

	public synchronized int getActiveExclusives () {
		return activeExclusives;
	}

	synchronized boolean activeExclusivesExist () {
		return activeExclusives > 0 ? true : false;
	}

	synchronized boolean activeUsualsExist () {
		return activeUsuals > 0 ? true : false;
	}

	public Lock getInnerLock () {
		return innerLock;
	}

	LinkedHashMap<TerminalState, Long> getTestStateMap () {
		return testStateMap;
	}

}
