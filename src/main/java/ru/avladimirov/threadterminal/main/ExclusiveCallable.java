package ru.avladimirov.threadterminal.main;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import static ru.avladimirov.threadterminal.main.Terminals.getLogger;

/**
 * A wrapper for a callable, that exclusively uses its {@link PriorityExecutor}.
 * When its execution begins, it waits for all currently active tasks to finish,
 * then holds all new tasks from starting, then executes its callable.
 * <br>
 * In other words, this class in pair with {@link UsualCallable} form a
 * transport, that delivers its load (your custom callables) to the pool
 * executor and regulates its execution.
 *
 * @author Vladimirov.A.A
 */
class ExclusiveCallable<T> implements Callable<T> {

	private Lock taskLock;
	private Condition exclusiveCondition;
	private Lock innerLock;
	private Callable<T> callable;
	private String name;
	private TerminalState terminalState;
	private ThreadTerminal terminal;

	/**
	 *
	 * @param callable your custom callable, it will be wrapped by this class.
	 * @param threadTerminal that runs this wrapper
	 * @param name of this wrapper.
	 */
	ExclusiveCallable (Callable<T> callable, ThreadTerminal threadTerminal, String name) {
		this.terminal = threadTerminal;
		this.terminalState = threadTerminal.getTerminalState ();
		this.callable = callable;
		this.taskLock = threadTerminal.getTaskLock ();
		this.exclusiveCondition = threadTerminal.getCondition ();
		this.innerLock = terminalState.getInnerLock ();
		this.name = name;
	}

	@Override
	public T call () throws Exception {
		taskLock.lock ();
		getLogger ().debug (name + " exc " + "tasklocks");
		try {
			//terminalStateUpdate:
			//here we update the terminal inner state: increment the count of pending tasks
			innerLock.lock ();
			getLogger ().debug (name + " exc " + "innerLocks");
			try {
				terminalState.registerNewPending (name);
			} finally {
				innerLock.unlock ();
				getLogger ().debug (name + " exc " + "inner unlocks");
			}

			//wait until all usual tasks finish executing
			boolean exitCycle = false;
			while (!exitCycle) {
				innerLock.lock ();
				try {
					if (!terminalState.activeUsualsExist ()) {
						terminalState.registerNewActive (name, true);
						exitCycle = true;
					} else {
						if (terminal.isSelfTest ()) {
							getLogger ().debug (name + " can't start: there are activeUsuals");
						}
					}
				} finally {
					innerLock.unlock ();
				}
				Thread.sleep (10);
			}

			//now we can proceed
			//terminalStateUpdate:
			//terminal inner state is changed: the task has entered active phase
			getLogger ().debug (name + " starts");

			//the CALL code itself
			return callable.call ();
		} finally {
			//terminalStateUpdate:
			//terminal inner state is changed: the task has finished execution
			innerLock.lock ();
			try {
				terminalState.registerNewFinished (name, true);
				//tell all usual pending tasks that they can continue
				exclusiveCondition.signalAll ();
			} finally {
				innerLock.unlock ();
			}
			taskLock.unlock ();
			getLogger ().debug (name + " unlocks and ends");
		}
	}
}
