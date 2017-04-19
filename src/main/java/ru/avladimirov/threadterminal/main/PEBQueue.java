package ru.avladimirov.threadterminal.main;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;
import static ru.avladimirov.threadterminal.main.Terminals.getLogger;

/**
 * An extension of the {@link PriorityBlockingQueue} that supports task
 * exclusivity management.
 *
 * @author Vladimirov.A.A
 */
class PEBQueue<E> extends PriorityBlockingQueue<E> {

	private TerminalState terminalState;

	PEBQueue (ThreadTerminal terminal) {
		super ();
		this.terminalState = terminal.getTerminalState ();
	}

	@Override
	public E take () throws InterruptedException {
		Lock innerLock = terminalState.getInnerLock ();
		innerLock.lock ();
		getLogger ().debug ("queue: innerLock is acquired");
		try {
			if (!terminalState.activeExclusivesExist ()) {
				getLogger ().debug ("queue: no exclusives");
				E callable = super.poll ();
				if (callable instanceof ComparableFutureTask) {
					getLogger ().debug ("to be taken from queue: " + ((ComparableFutureTask) callable).getName ());
				}

				getLogger ().debug ("queue as array length " + this.toArray ().length);
				return callable;
			} else {
				getLogger ().debug ("queue: some exclusive is still active");
				return null;
			}
		} finally {
			innerLock.unlock ();
			getLogger ().debug ("queue: innerLock is unlocked");
		}
	}

	@Override
	public E poll () {
		Lock innerLock = terminalState.getInnerLock ();
		innerLock.lock ();
		getLogger ().debug ("queue poll: innerLock is acquired");
		try {
			if (!terminalState.activeExclusivesExist ()) {
				getLogger ().debug ("queue poll: no exclusives");
				E callable = super.poll ();
				if (callable instanceof ComparableFutureTask) {
					getLogger ().debug ("to be taken from queue poll: " + ((ComparableFutureTask) callable).getName ());
				}
				return callable;
			} else {
				return null;
			}
		} finally {
			innerLock.unlock ();
			getLogger ().debug ("queue poll: innerLock is unlocked");
		}
	}
}
