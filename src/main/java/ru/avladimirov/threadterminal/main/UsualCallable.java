package ru.avladimirov.threadterminal.main;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import static ru.avladimirov.threadterminal.main.Terminals.getLogger;

/**
 * A wrapper for a callable, that can be executed in parallel with other tasks
 * of this class in a common {@link PriorityExecutor}.
 * <br>
 * In other words, this class in pair with {@link ExclusiveCallable} form a
 * transport, that delivers its load (your custom callables) to the pool
 * executor and regulates its execution.
 *
 * @author Vladimirov.A.A
 */
class UsualCallable<T> implements Callable<T> {

	private Lock taskLock;
	private Condition exclusiveCondition;
	private Lock innerLock;
	private Callable<T> callable;
	private String name;
	private TerminalState terminalState;
	private ThreadTerminal terminal;

	/**
	 *
	 * @param callable your custom callable, that is wrapped by this class.
	 * @param threadTerminal that runs this wrapper
	 * @param name of this wrapper.
	 */
	UsualCallable (Callable<T> callable, ThreadTerminal threadTerminal, String name) {
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
		try {
			//terminalStateUpdate:
			//here we update the terminal inner state: increment the count of pending tasks
			innerLock.lock ();
			try {
				terminalState.registerNewPending (name);
			} finally {
				innerLock.unlock ();
			}

			//wait until an exclusive task finishes its execution
			boolean shouldWait = false;
			innerLock.lock ();
			try {
				if (terminalState.activeExclusivesExist ()) {
					getLogger ().debug (name + " can't start: there are activeExclusives");
					shouldWait = true;
					getLogger ().debug (name + ": permission's acquired");
				}
			} finally {
				innerLock.unlock ();
			}
			//TODO any ideas how to atomize activeExclusivesExist() and await()?
			if (shouldWait) {
				exclusiveCondition.await ();
			}

		} finally {
			//we don't need locking between usual tasks: they share no mutual variables,
			//so can be run in parallel. If you need to synchronize actions between your custom
			//callables, that are wrapped by this class, make additional locking inside
			//their call() methods.
			taskLock.unlock ();
			getLogger ().debug (name + ": unlocked");
		}

		//terminalStateUpdate:
		//terminal inner state is changed: the task has entered active phase
		innerLock.lock ();
		getLogger ().debug (name + " innerLock: before registering new active");
		try {
			terminalState.registerNewActive (name, false);
		} finally {
			innerLock.unlock ();
			getLogger ().debug (name + " innerLock: unlocked");
		}

		//the CALL code itself
		try {
			getLogger ().debug (name + " starts execution");
			return callable.call ();
		} finally {
			//terminalStateUpdate:
			//terminal inner state is changed: the task has finished execution
			innerLock.lock ();
			getLogger ().debug (name + " innerLock: before registering new finished");
			try {
				terminalState.registerNewFinished (name, false);
			} finally {
				innerLock.unlock ();
				getLogger ().debug (name + " innerLock: finally unlocked");
			}
			getLogger ().debug (name + " ends execution");
		}
	}
}
