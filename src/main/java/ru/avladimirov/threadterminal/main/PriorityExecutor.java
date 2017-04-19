package ru.avladimirov.threadterminal.main;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;

/**
 * An extension of the standard java {@link ThreadPoolExecutor}, that allows
 * task scheduling based on task priority and order of submission.
 *
 * @author Vladimirov.A.A
 */
public class PriorityExecutor extends ThreadPoolExecutor {

	//A counter of all submitted tasks
	private final AtomicLong taskCounter = new AtomicLong (0);
	private ThreadTerminal terminal;
	private TerminalState terminalState;
	private Lock innerLock;
	private BlockingQueue queue;

	/**
	 * The same constructor as in the {@link ThreadPoolExecutor}.
	 *
	 * @param corePoolSize
	 * @param maximumPoolSize
	 * @param keepAliveTime
	 * @param unit
	 * @param queue
	 */
	PriorityExecutor (ThreadTerminal terminal, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, PriorityBlockingQueue<ComparableRunnable> queue) {
		super (corePoolSize, maximumPoolSize, keepAliveTime, unit, (BlockingQueue) queue);
		this.terminal = terminal;
		this.terminalState = terminal.getTerminalState ();
		this.innerLock = terminalState.getInnerLock ();
		this.queue = queue;
		this.allowCoreThreadTimeOut (true);
	}

	@Override
	protected <T> RunnableFuture<T> newTaskFor (Callable<T> callable) {
		PriorityCallable<T> priCallable = (PriorityCallable<T>) callable;
		//terminalStateUpdate:
		//here we update the terminal inner state: increment the count of submitted tasks
		innerLock.lock ();
		try {
			terminalState.registerNewNext (priCallable.getName ());
			return new ComparableFutureTask<> (priCallable, taskCounter);
		} finally {
			innerLock.unlock ();
		}
	}
}
