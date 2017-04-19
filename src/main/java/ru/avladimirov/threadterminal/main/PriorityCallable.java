package ru.avladimirov.threadterminal.main;

import java.util.concurrent.Callable;

/**
 * A wrapper for a Callable that adds a priority feature to it.
 *
 * @author Vladimirov.A.A
 */
class PriorityCallable<T> implements Callable<T>, Prioritized {

	private int priority;
	private Callable<T> callable;
	private String name;

	/**
	 *
	 * @param callable should be either {@link ExclusiveCallable} or
	 * {@link UsualCallable}.
	 */
	PriorityCallable (Callable<T> callable, String name, int priority) {
		this.callable = callable;
		this.priority = priority;
		this.name = name;
	}

	@Override
	public T call () throws Exception {
		try {
			return (T) callable.call ();
		} catch (Exception ex) {
			throw ex;
		}
	}

	@Override
	public int getPriority () {
		return priority;
	}

	String getName () {
		return name;
	}

	Callable getCallable () {
		return callable;
	}

	boolean isExclusive () {
		return callable instanceof ExclusiveCallable;
	}

	@Override
	public String toString () {
		String addition = " ";
		if (callable instanceof ExclusiveCallable) {
			addition = "true";
		}
		if (callable instanceof UsualCallable) {
			addition = "false";
		}
		return name + "," + addition + "," + priority;
	}

}
