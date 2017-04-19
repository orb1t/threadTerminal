/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.avladimirov.threadterminal.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Vladimirov.A.A
 */
public class TerminalStateTest {

	public TerminalStateTest () {
	}

	@BeforeClass
	public static void setUpClass () {
	}

	@AfterClass
	public static void tearDownClass () {
	}

	@Before
	public void setUp () {
	}

	@After
	public void tearDown () {
	}

	public static Callable newSleepCallable (int sleepMillis) {
		return new Callable () {

			@Override
			public Object call () throws Exception {
				Thread.sleep (sleepMillis);
				return null;
			}
		};
	}

	public static Callable<List<UUID>> newCalcCallable () {
		return () -> {
			ArrayList<UUID> entityUuids = new ArrayList<> ();
			for (int i = 0; i < 1000; i++) {
				double t = Math.pow (i, i);
			}

			return entityUuids;
		};
	}

	public static void assertTerminalState (TerminalState state, long queued, long pending, long active, long finished) {
		assertNotNull (state);
		assertTrue (state.getQueuedTasksCount () == queued);
		assertTrue (state.getPendingTasksCount () == pending);
		assertTrue (state.getActiveTasksCount () == active);
		assertTrue (state.getFinishedTasksCount () == finished);
		assertTrue (state.getActiveTasksCount () == (state.getActiveExclusives () + state.getActiveUsuals ()));
	}

	public static long getTaskSum (TerminalState state) {
		return state.getQueuedTasksCount ()
				+ state.getPendingTasksCount ()
				+ state.getActiveTasksCount ()
				+ state.getFinishedTasksCount ();
	}

	/**
	 * Checks that for each two seqential states in the map their timings are
	 * also sequential.
	 *
	 * @param stateMap
	 */
	public static void assertStatesAreSequential (LinkedHashMap<TerminalState, Long> stateMap) {
		Entry<TerminalState, Long> previous = null;
		for (Entry<TerminalState, Long> entry : stateMap.entrySet ()) {
			if (previous == null) {
				continue;
			}
			assertTrue (entry.getValue () > previous.getValue ());
			previous = entry;
		}
	}

	public static void assertTaskSumInMap (LinkedHashMap<TerminalState, Long> stateMap, long sum) {
		long total = 0;
		for (TerminalState state : stateMap.keySet ()) {
			//wait while all tasks are queued
			if (total < sum) {
				total = getTaskSum (state);
			} //once they are all queued, the sum should be the same for the rest
			else {
				assertTrue (getTaskSum (state) == sum);
			}
		}
	}

	public static void assertExclusivesAndUsualsDontOverlap (LinkedHashMap<TerminalState, Long> stateMap, long sum) {
		int counter = stateMap.entrySet ().iterator ().next ().getKey ().getPoolSize ();
		for (TerminalState state : stateMap.keySet ()) {
			//first 'poolSize' tasks will be added simultaneously to the executor
			if (counter > 0) {
			} else {
				int usuals = state.getActiveUsuals ();
				int exclusives = state.getActiveExclusives ();
				if (usuals > 0) {
					assertTrue (exclusives == 0);
				}
				if (exclusives > 0) {
					assertTrue (usuals == 0);
				}
			}
		}
	}

	/**
	 * Check that all countings between a group of active tasks and a group of
	 * finished tasks are correct. (For example, if a task was finished in this
	 * snapshot, than the count of actives should be less by one than in the
	 * previous snapshot). Check that exactly the same task was active in
	 * previous snapshot and now is in 'finished' in the next snapshots.
	 *
	 * @param stateMap
	 */
	public static void assertActiveFinishedInteraction (LinkedHashMap<TerminalState, Long> stateMap) {
		Entry<TerminalState, Long> previous = null;
		for (Entry<TerminalState, Long> entry : stateMap.entrySet ()) {
			if (previous == null) {
				previous = entry;
				continue;
			}
			if (previous.getKey ().getFinishedTasksCount () < entry.getKey ().getFinishedTasksCount ()
					&& previous.getKey ().getFinishedTasksCount () != 0) {
				String prevFinished = previous.getKey ().getLast100finishedTasksNames ().lastEntry ().getValue ();
				String lastFinished = entry.getKey ().getLast100finishedTasksNames ().lastEntry ().getValue ();
				Set<String> prevActives = previous.getKey ().getActiveTasksNames ().keySet ();
				Set<String> lastActives = entry.getKey ().getActiveTasksNames ().keySet ();

				assertTrue (prevActives.contains (lastFinished));
				assertTrue (!lastActives.contains (lastFinished));
				assertTrue (!lastFinished.equals (prevFinished));

				//check counting between groups
				assertTrue ((prevFinished + prevActives.size ()) == (lastFinished + lastActives.size ()));
				assertTrue (prevActives.size () == lastActives.size () + 1);
				assertTrue (previous.getKey ().getFinishedTasksCount () == entry.getKey ().getFinishedTasksCount () - 1);
			}
		}
	}

	/**
	 * Check countings between two seqential snapshots, where one pending task
	 * becomes active. Check that exactly the same task was in pendings, and
	 * moved to actives.
	 *
	 * @param stateMap
	 */
	public static void assertPendingActiveInteraction (LinkedHashMap<TerminalState, Long> stateMap) {
		Entry<TerminalState, Long> previous = null;
		for (Entry<TerminalState, Long> entry : stateMap.entrySet ()) {
			if (previous == null) {
				previous = entry;
				continue;
			}
			if (previous.getKey ().getPendingTasksCount () > entry.getKey ().getPendingTasksCount ()) {
				Set<String> prevPending = previous.getKey ().getPendingTasksNames ().keySet ();
				Set<String> lastPending = entry.getKey ().getPendingTasksNames ().keySet ();

				Set<String> prevActives = previous.getKey ().getActiveTasksNames ().keySet ();
				Set<String> lastActives = entry.getKey ().getActiveTasksNames ().keySet ();

				//check counting each group should differ by 1
				assertTrue ((prevPending.size () + prevActives.size ()) == (lastPending.size () + lastActives.size ()));
				assertTrue (prevPending.size () == lastPending.size () - 1);

				String newActive = "";
				for (String pender : prevPending) {
					if (!lastPending.contains (pender)) {
						newActive = pender;
						break;
					}
				}

				//check that a task really passed from pending to actives
				assertTrue (!newActive.isEmpty ());
				assertTrue (!prevActives.contains (newActive));
				assertTrue (lastActives.contains (newActive));
			}
		}
	}

	/**
	 * Test of getTerminalName method, of class TerminalState.
	 */
	@Test
	public void testGetTerminalName () {
		System.out.println ("#testGetTerminalName");
		String terminalName = "termini";
		ThreadTerminal terminal = new ThreadTerminal (terminalName, 1, true);
		TerminalState state = terminal.getTestStateMap ().keySet ().iterator ().next ();
		assertTrue (terminal.getTestStateMap ().size () == 1);
		assertEquals (terminalName, state.getTerminalName ());
	}

	/**
	 * Test of atomicity and sequentiality between state registering in one task
	 * in one thread.
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void testAtomicityOneThreadOneTask () throws InterruptedException {
		System.out.println ("#testRegisterNewAtomicity");
		String terminalName = "termini";
		ThreadTerminal terminal = new ThreadTerminal (terminalName, 1, true);
		terminal.submit (newSleepCallable (1000), "1", Priority.HIGH, false);
		Thread.sleep (600);
		//
		System.out.println ("map " + terminal.getTestStateMap ().size ());
		assertTrue (terminal.getTestStateMap ().size () == 4);
		Thread.sleep (600);
		//
		assertTrue (terminal.getTestStateMap ().size () == 5);

		//parse TerminalState
		Iterator<Entry<TerminalState, Long>> iter = terminal.getTestStateMap ().entrySet ().iterator ();
		TerminalState stateInitial = iter.next ().getKey ();
		TerminalState stateNew = iter.next ().getKey ();
		TerminalState statePending = iter.next ().getKey ();
		TerminalState stateActive = iter.next ().getKey ();
		TerminalState stateFinished = iter.next ().getKey ();

		//check the values of task counters
		assertTerminalState (stateInitial, 0, 0, 0, 0);
		assertTerminalState (stateNew, 1, 0, 0, 0);
		assertTerminalState (statePending, 0, 1, 0, 0);
		assertTerminalState (stateActive, 0, 0, 1, 0);
		assertTerminalState (stateFinished, 0, 0, 0, 1);

		assertTaskSumInMap (terminal.getTestStateMap (), 1);
		assertStatesAreSequential (terminal.getTestStateMap ());
		assertExclusivesAndUsualsDontOverlap (terminal.getTestStateMap (), 1);
	}

	/**
	 * Test of atomicity and sequentiality between state registering with two
	 * tasks in one thread.
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void testAtomicityOneThreadTwoTasks () throws InterruptedException {
		System.out.println ("#testRegisterNewAtomicity");
		String terminalName = "termini";
		ThreadTerminal terminal = new ThreadTerminal (terminalName, 1, true);
		terminal.submit (newSleepCallable (1000), "1", Priority.HIGH, false);
		terminal.submit (newSleepCallable (1000), "2", Priority.HIGH, false);
		terminal.awaitTermination (3, TimeUnit.SECONDS);
		//
		assertTrue (terminal.getTestStateMap ().size () == 9);
		assertStatesAreSequential (terminal.getTestStateMap ());
		assertExclusivesAndUsualsDontOverlap (terminal.getTestStateMap (), 2);

		//parse TerminalState
		Iterator<Entry<TerminalState, Long>> iter = terminal.getTestStateMap ().entrySet ().iterator ();
		for (int i = 0; i < 4; i++) {
			iter.next ();
		}

		TerminalState stateActive1 = iter.next ().getKey ();
		TerminalState stateFinished1 = iter.next ().getKey ();
		TerminalState statePending2 = iter.next ().getKey ();
		TerminalState stateActive2 = iter.next ().getKey ();
		TerminalState stateFinished2 = iter.next ().getKey ();

		//check the values of task counters
		assertTerminalState (stateActive1, 1, 0, 1, 0);
		assertTerminalState (stateFinished1, 1, 0, 0, 1);
		assertTerminalState (statePending2, 0, 1, 0, 1);
		assertTerminalState (stateActive2, 0, 0, 1, 1);
		assertTerminalState (stateFinished2, 0, 0, 0, 2);

		assertTaskSumInMap (terminal.getTestStateMap (), 2);

	}

	/**
	 * Test atomicity and sequentiality for multiple terminals of various
	 * poolsize and various task load (like in real life).
	 *
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@Test
	public void testStateConsistency () throws InterruptedException, IOException {
		System.out.println ("#testStateConsistency");
		for (int i = 1; i < 10; i++) {
			testNewTerminal (i * 5);
		}
	}

	/**
	 * Creates and tests a new terminal of {@code poolSize} capacity.
	 *
	 * @param poolSize
	 * @throws InterruptedException
	 */
	public static void testNewTerminal (int poolSize) throws InterruptedException {
		//create terminal with 100 tasks
		ThreadTerminal terminal = new ThreadTerminal ("terminal" + poolSize, poolSize, true);
		int tasksCount = 0;
		for (int i = 0; i < 30; i++) {
			int pack = (int) (Math.random () * 5);
			Priority priority = Math.random () > 0.5 ? Priority.HIGH : Priority.LOW;
			boolean exclusivity = Math.random () > 0.5 ? true : false;

			System.out.println ("  to submit: " + tasksCount + "-" + (tasksCount + pack) + ", pr: " + priority + " exc: " + exclusivity);

			for (int j = 0; j < pack; j++) {
				int taskDuration = (int) (Math.random () * 1000);
				terminal.submit (newCalcCallable (), ++tasksCount + "", priority, exclusivity);
				if (tasksCount > 99) {
					break;
				}
			}
			if (tasksCount > 99) {
				break;
			}
		}

		//start it
		terminal.shutdown ();
		boolean terminated = terminal.awaitTermination (100, TimeUnit.SECONDS);

		TerminalState lastState = terminal.snapshotTerminalState ();
		StringBuilder sb = new StringBuilder ();
		sb.append ("last " + lastState.getLast100finishedTasksNames ().size () + " finished tasks in the order of finishing: ").append ("\n");
		for (String name : lastState.getLast100finishedTasksNames ().values ()) {
			sb.append (name).append ("\n");
		}
		System.out.println (sb.toString ());

		//test it
		assertTrue (terminated);
		assertTaskSumInMap (terminal.getTestStateMap (), tasksCount);
		assertStatesAreSequential (terminal.getTestStateMap ());
		assertExclusivesAndUsualsDontOverlap (terminal.getTestStateMap (), tasksCount);
		assertActiveFinishedInteraction (terminal.getTestStateMap ());
		assertPendingActiveInteraction (terminal.getTestStateMap ());
		//assert that all tasks were finished
		assertTrue (lastState.getFinishedTasksCount () == tasksCount);
	}

//	@Test
	public void testNewTerminalWithTextFile () throws FileNotFoundException, IOException, InterruptedException {
		System.out.println ("#testNewTerminalWithTextFile");
		//Create tasks. Write them to file
		File file = new File ("src/test/test_" + System.currentTimeMillis () + ".csv");
		FileWriter writer = new FileWriter (file);
		int poolSize = 10;
		LinkedHashMap<PriorityCallable, Boolean> map = new LinkedHashMap<> ();
		int tasksCount = 0;
		for (int i = 0; i < 30; i++) {
			int pack = (int) (Math.random () * 5);
			Priority priority = Math.random () > 0.5 ? Priority.HIGH : Priority.LOW;
			boolean exclusivity = Math.random () > 0.5 ? true : false;
			for (int j = 0; j < pack; j++) {
				tasksCount++;
				writer.append (tasksCount + "").append (",").append (exclusivity + "").append (",").append (priority.getValue () + "").append ("\n");
				map.put (new PriorityCallable (newCalcCallable (), tasksCount + "", priority.getValue ()), exclusivity);
			}
		}
		writer.close ();

		System.out.println ("map: " + map);
		ThreadTerminal terminal = new ThreadTerminal ("terminal" + poolSize, poolSize, true);
		for (Entry<PriorityCallable, Boolean> entry : map.entrySet ()) {
			PriorityCallable callable = entry.getKey ();
			Priority priority = callable.getPriority () == Priority.HIGH.getValue () ? Priority.HIGH : Priority.LOW;
			terminal.submit (callable.getCallable (), callable.getName (), priority, entry.getValue ());
		}
		terminal.shutdown ();
		boolean terminated = terminal.awaitTermination (60, TimeUnit.SECONDS);

		TerminalState lastState = terminal.snapshotTerminalState ();
		StringBuilder sb = new StringBuilder ();
		sb.append ("last " + lastState.getLast100finishedTasksNames ().size () + " finished tasks in the order of finishing: ").append ("\n");
		for (String name : lastState.getLast100finishedTasksNames ().values ()) {
			sb.append (name).append ("\n");
		}
		System.out.println (sb.toString ());

		//test it
		assertTrue (terminated);
		assertTaskSumInMap (terminal.getTestStateMap (), tasksCount);
		assertStatesAreSequential (terminal.getTestStateMap ());
		assertExclusivesAndUsualsDontOverlap (terminal.getTestStateMap (), tasksCount);
		assertActiveFinishedInteraction (terminal.getTestStateMap ());
		assertPendingActiveInteraction (terminal.getTestStateMap ());
		//assert that all tasks were finished
		assertTrue (lastState.getFinishedTasksCount () == tasksCount);
	}

	@Test
	public void testCyclicSubmitting () throws InterruptedException, ExecutionException {
		ThreadTerminal terminal = new ThreadTerminal ("terminal", 10, true);
		int i = 0;
		while (i < 10) {
			terminal.submit (newCalcCallable (), "getJobs" + i + "1", Priority.HIGH, false);
			terminal.submit (newCalcCallable (), "getJobs" + i + "2", Priority.HIGH, true);
			terminal.submit (newCalcCallable (), "getJobs" + i + "3", Priority.HIGH, false);
			terminal.submit (newCalcCallable (), "getJobs" + i + "4", Priority.HIGH, false);
			List<UUID> uuids = terminal.submit (newCalcCallable (), "getJobs" + i + "5", Priority.HIGH, false).get ();
			i++;
		}
		terminal.shutdown ();
		terminal.awaitTermination (1, TimeUnit.DAYS);
	}
}
