package ru.avladimirov.threadterminal.main;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A global thread scheduler, that runs tasks in threads, arranges them by
 * priority, allows any of them to run exclusively. Priority has two levels -
 * high and low. Tasks of one priority level are executed in their sequential
 * order. Exclusivity means that no other task will be executed while an
 * exclusive one is in process. Non-exclusive (usual) tasks are executed in
 * parallel using all available threads, as they are intended to do by a
 * ThreadPoolExecutor.
 *
 * @see ExclusiveCallable
 * @see UsualCallable
 * @author Vladimirov.A.A
 */
public class ThreadTerminal {

	//TODO why ReentrantLocks?
	//TODO implement useful executor methods: awaitTermination, isTerminated, ...
	private PriorityExecutor executor;
	private PriorityBlockingQueue<ComparableRunnable> queue;
	//terminal self name
	private String name;
	private int poolSize;

	/**
	 * A lock used for scheduling execution between all tasks in the executor.
	 * It's necessary only for maintaining exclusive/usual execution.
	 */
	private Lock taskLock = new ReentrantLock ();

	/**
	 * If there is an active exclusive callable currently executing, all usual
	 * callables must await until it's finished and signals all of them using
	 * this condition.
	 */
	private Condition condition = taskLock.newCondition ();

	/**
	 * A lock for providing atomicity for some inner operations. Is used by
	 * {@link TerminalState} instance.
	 */
	private Lock innerLock;

	private TerminalState terminalState;

	//only for testing purposes. Should be false in daily usage.
	private boolean selfTest = false;

	/**
	 *
	 * @param name the name of this terminal
	 * @param poolSize the count of threads in the executor pool
	 */
	ThreadTerminal (String name, int poolSize, boolean selfTest) {
		this.name = name;
		this.poolSize = poolSize;
		this.selfTest = selfTest;
		this.terminalState = new TerminalState (name, poolSize, selfTest);
		this.innerLock = terminalState.getInnerLock ();
		queue = new PEBQueue<> (this);

		//some timeout should be specified due to ThreadPoolExecutor's getTask() method.
		executor = new PriorityExecutor (this, poolSize, poolSize, 10, TimeUnit.SECONDS, queue);
	}

	/**
	 * Submits a callable task with a high priority level. That means it is put
	 * to the head of the task queue and will be the next task to be executed.
	 *
	 * @param <T> the type of the callable's returned result
	 * @param callable a standard callable to be executed
	 * @param name the name of the task
	 * @param priority can be one of {@link Priority} levels
	 * @param exclusively if true, it will be executed exclusively, if false,
	 * then in parallel with all other tasks.
	 * @return an object of Future, from which you can obtain the results of
	 * your callable's execution.
	 */
	public <T> Future<T> submit (Callable<T> callable, String name, Priority priority, boolean exclusively) {
		innerLock.lock ();
		try {
			if (terminalState.getPendingTasksNames ().keySet ().contains (name)
					|| terminalState.getActiveTasksNames ().keySet ().contains (name)) {
				name = name + UUID.randomUUID ().toString ().substring (0, 6);
			}
		} finally {
			innerLock.unlock ();
		}
		PriorityCallable<T> priorityCallable = newPriorityCallable (callable, name, priority, exclusively);
		return executor.submit (priorityCallable);
	}

	/**
	 * Submits a callable task with a high priority level. That means it is put
	 * to the head of the task queue and will be the next task to be executed.
	 *
	 * @param <T> the type of the callable's returned result
	 * @param callables a map with callables to be submitted and their names.
	 * @param priority can be one of {@link Priority} levels
	 * @param exclusively if true, it will be executed exclusively, if false,
	 * then in parallel with all other tasks.
	 * @return an object of Future, from which you can obtain the results of
	 * your callable's execution.
	 * @throws java.lang.InterruptedException exception is rethrown from the
	 * inner {@link ThreadPoolExecutor#invokeAll(java.util.Collection)} method.
	 */
	public <T> List<Future<T>> invokeAll (Map<Callable<T>, String> callables, Priority priority, boolean exclusively) throws InterruptedException {
		List<PriorityCallable<T>> priCallables = new ArrayList<> ();
		for (Entry<Callable<T>, String> entry : callables.entrySet ()) {
			priCallables.add (newPriorityCallable (entry.getKey (), entry.getValue (), priority, exclusively));
		}
		return executor.invokeAll (priCallables);
	}

	/**
	 * Submits a callable task with a high priority level. That means it is put
	 * to the head of the task queue and will be the next task to be executed.
	 *
	 * @param <T> the type of the callable's returned result
	 * @param callables a map with callables to be submitted and their names.
	 * @param priority can be one of {@link Priority} levels
	 * @param exclusively if true, it will be executed exclusively, if false,
	 * then in parallel with all other tasks.
	 * @param timeout the maximum time to wait
	 * @param unit the time unit of the timeout argument
	 * @return an object of Future, from which you can obtain the results of
	 * your callable's execution.
	 * @throws java.lang.InterruptedException exception is rethrown from the
	 * inner {@link ThreadPoolExecutor#invokeAll(java.util.Collection)} method.
	 */
	public <T> List<Future<T>> invokeAll (Map<Callable<T>, String> callables, Priority priority, boolean exclusively, long timeout, TimeUnit unit) throws InterruptedException {
		List<PriorityCallable<T>> priCallables = new ArrayList<> ();
		for (Entry<Callable<T>, String> entry : callables.entrySet ()) {
			priCallables.add (newPriorityCallable (entry.getKey (), entry.getValue (), priority, exclusively));
		}
		return executor.invokeAll (priCallables, timeout, unit);
	}

	/**
	 * Constructs a new {@link PriorityCallable} wrapped around
	 * {@link ExclusiveCallable} or {@link UsualCallable} depending on the given
	 * arguments.
	 *
	 * @param callable a simple callable to be wrapped
	 * @param name of the callable
	 * @param priority among other callables in the pool
	 * @param exclusively if true, it will be executed exclusively, if false,
	 * then in parallel with all other tasks.
	 * @return an instance of PriorityCallable with defined exclusivity.
	 */
	private PriorityCallable newPriorityCallable (Callable callable, String name, Priority priority, boolean exclusively) {
		PriorityCallable priorityCallable;
		if (exclusively) {
			ExclusiveCallable exclusive = new ExclusiveCallable (callable, this, name);
			priorityCallable = new PriorityCallable (exclusive, name, priority.getValue ());
		} else {
			UsualCallable usual = new UsualCallable (callable, this, name);
			priorityCallable = new PriorityCallable (usual, name, priority.getValue ());
		}
		return priorityCallable;
	}

	/**
	 * Gets a {@link TerminalState} object, describing the inner state of the
	 * terminal at the moment of call. It is a clone of the real object inside
	 * the terminal, so it's not backed by it.
	 *
	 * @return a {@link TerminalState} object.
	 */
	public TerminalState snapshotTerminalState () {
		innerLock.lock ();
		try {
			return (TerminalState) terminalState.clone ();
		} finally {
			innerLock.unlock ();
		}
	}

	/**
	 * Shuts down the executor using the executor's
	 * {@link ThreadPoolExecutor#shutdown()}.
	 *
	 * @see ThreadPoolExecutor#shutdown()
	 */
	public void shutdown () {
		executor.shutdown ();
	}

	/**
	 * Shuts down the executor using the executor's
	 * {@link ThreadPoolExecutor#shutdownNow()}.
	 *
	 * @see ThreadPoolExecutor#shutdownNow()
	 */
	public void shutdownNow () {
		executor.shutdownNow ();
	}

	/**
	 * @see ThreadPoolExecutor#awaitTermination(long,
	 * java.util.concurrent.TimeUnit)
	 */
	public boolean awaitTermination (long timeout, TimeUnit unit) throws InterruptedException {
		return executor.awaitTermination (timeout, unit);
	}

	public boolean isTerminated () {
		return executor.isTerminated ();
	}

	synchronized Lock getTaskLock () {
		return taskLock;
	}

	synchronized Condition getCondition () {
		return condition;
	}

	synchronized TerminalState getTerminalState () {
		return terminalState;
	}

	LinkedHashMap<TerminalState, Long> getTestStateMap () {
		return terminalState.getTestStateMap ();
	}

	boolean isSelfTest () {
		return selfTest;
	}

	public int getPoolSize () {
		return poolSize;
	}
}
