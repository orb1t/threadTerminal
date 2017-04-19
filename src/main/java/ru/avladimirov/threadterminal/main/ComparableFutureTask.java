package ru.avladimirov.threadterminal.main;

import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An extension of the {@link FutureTask} that supports comparability.
 *
 * @author Vladimirov.A.A
 */
public class ComparableFutureTask<T> extends FutureTask<T> implements ComparableRunnable {

	private Prioritized prioritized;
	private PriorityCallable<T> callable;
	private long orderIndex;

	/**
	 *
	 * @param callable should be a {@link PriorityCallable}, because it can be
	 * compared.
	 * @param index necessary for implementing the sequential order among
	 * callables of the same priority.
	 */
	ComparableFutureTask (PriorityCallable<T> callable, AtomicLong index) {
		super (callable);
		this.callable = callable;
		this.prioritized = callable;
		this.orderIndex = index.getAndIncrement ();
	}

	@Override
	public int getPriority () {
		return prioritized.getPriority ();
	}

	/**
	 *
	 * @return the name of the underlying callable.
	 *
	 * @see PriorityCallable
	 */
	public String getName () {
		return callable.getName ();
	}

	/**
	 *
	 * @return true if the underlying callable is exclusive, false if it's
	 * usual.
	 *
	 * @see ExclusiveCallable
	 * @see UsualCallable
	 */
	public boolean isExclusive () {
		return callable.getCallable () instanceof ExclusiveCallable;
	}

	@Override
	public long getIndex () {
		return orderIndex;
	}
}
