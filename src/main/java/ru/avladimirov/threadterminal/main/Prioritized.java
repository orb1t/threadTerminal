package ru.avladimirov.threadterminal.main;

/**
 * An interface for implementing by tasks: runnables and callables. Thus all
 * runnables and callables can report their priority indexes in the global pool.
 *
 * @author Vladimirov.A.A
 */
public interface Prioritized {

	/**
	 *
	 * @return the priority of the runnable/callable in the global pool.
	 */
	public int getPriority ();
}
