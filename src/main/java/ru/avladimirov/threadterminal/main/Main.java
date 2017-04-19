package ru.avladimirov.threadterminal.main;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 *
 * @author Vladimirov.A.A
 */
public class Main {

	public static void main (String[] args) throws InterruptedException, ExecutionException {
		ThreadTerminal terminal = new ThreadTerminal ("termini", 16, false);

//		Thread monitor = new Thread (() -> {
//			while (true) {
//				TerminalState state = terminal.snapshotTerminalState ();
//				state.printStateCounts ();
//				try {
//					Thread.sleep (1);
//				} catch (InterruptedException ex) {
//					Logger.getLogger (Main.class.getName ()).log (Level.SEVERE, null, ex);
//				}
//			}
//		});
//		monitor.start ();
		terminal.submit (newCallable (1000), "1", Priority.LOW, false);
		terminal.submit (newCallable (1000), "2", Priority.LOW, false);
		terminal.submit (newCallable (1000), "3", Priority.LOW, false);
		terminal.submit (newCallable (1000), "4", Priority.LOW, false);
		terminal.submit (newCallable (1000), "5", Priority.LOW, false);
		terminal.submit (newCallable (1000), "6", Priority.LOW, false);
		terminal.submit (newCallable (3000), "7", Priority.LOW, true);
		terminal.submit (newCallable (3000), "8", Priority.LOW, true);
		terminal.submit (newCallable (1000), "9", Priority.LOW, false);
		Future task = terminal.submit (newCallable (1000), "10", Priority.LOW, false);
		Future task2 = terminal.submit (newCallable (1000), "11", Priority.LOW, false);
		terminal.submit (newCallable (1000), "12", Priority.LOW, false);
		terminal.submit (newCallable (1000), "13", Priority.LOW, false);
		terminal.submit (newCallable (1000), "14", Priority.LOW, false);
		terminal.submit (newCallable (1000), "15", Priority.LOW, false);
		terminal.submit (newCallable (1000), "16", Priority.LOW, false);
		terminal.submit (newCallable (1000), "17", Priority.LOW, false);
		terminal.submit (newCallable (1000), "18", Priority.LOW, false);
		terminal.submit (newCallable (1000), "19", Priority.LOW, false);
		terminal.submit (newCallable (1000), "20", Priority.LOW, false);
		terminal.submit (newCallable (1000), "21", Priority.LOW, false);
		terminal.submit (newCallable (3000), "22", Priority.LOW, true);
		terminal.submit (newCallable (3000), "23", Priority.LOW, true);
		terminal.submit (newCallable (3000), "24", Priority.LOW, true);
		terminal.submit (newCallable (1000), "25", Priority.LOW, false);
		terminal.submit (newCallable (1000), "26", Priority.LOW, true);
		terminal.submit (newCallable (1000), "27", Priority.LOW, false);
		terminal.submit (newCallable (1000), "28", Priority.LOW, false);
		terminal.submit (newCallable (1000), "29", Priority.LOW, false);
		terminal.submit (newCallable (1000), "30", Priority.LOW, false);
		terminal.shutdown ();
//		Thread.sleep (3000);
//		task.cancel (true);
//		task2.cancel (true);
//		System.out.println ("task is cancelled");

	}

	public static Callable newCallable (int sleepMillis) {
		return new Callable () {

			@Override
			public Object call () throws Exception {
				Thread.sleep (sleepMillis);
				return null;
			}
		};
	}
}
