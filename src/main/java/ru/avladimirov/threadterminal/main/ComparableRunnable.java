package ru.avladimirov.threadterminal.main;

/**
 * An interface that provides comparability for runnables. Is necessary for
 * {@link PriorityExecutor} and its {@link PriorityBlockingQueue}.
 *
 * @author Vladimirov.A.A
 */
public interface ComparableRunnable extends Runnable, Comparable<ComparableRunnable> {

	/**
	 * For usage in the PriorityBlockingQueue
	 *
	 * @return the priority of the runnable/callable.
	 */
	public int getPriority ();

	/**
	 * For usage in the PriorityBlockingQueue
	 *
	 * @return the index of the task in the global order of submitted tasks.
	 */
	public long getIndex ();

	@Override
	default public int compareTo (ComparableRunnable another) {
		int priority = this.getPriority () - another.getPriority ();
		return priority != 0 ? priority : (int) (this.getIndex () - another.getIndex ());
	}

}
