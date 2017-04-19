package ru.avladimirov.threadterminal.main;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * A static class for obtaining instances of {@link ThreadTerminal}.
 *
 * @author Vladimirov.A.A
 */
public final class Terminals {

	private static Logger logger = null;

	private Terminals () {
	}

	/**
	 * Creates a new instance of a {@link ThreadTerminal}.
	 *
	 * @param name terminal's name
	 * @param poolSize the count of threads in the terminal.
	 * @return a new {@link ThreadTerminal}
	 */
	public static ThreadTerminal newThreadTerminal (String name, int poolSize) {
		return new ThreadTerminal (name, poolSize, false);
	}

	/**
	 * A global function for obtaining the logger inside the package.
	 *
	 * @return the one and only logger for this package
	 */
	static Logger getLogger () {
		if (logger == null) {
			logger = LogManager.getLogger (Terminals.class);
			PatternLayout layout = new PatternLayout ("%d{ISO8601} [%t] %p %C{2}, %M: %m%n");
			logger.addAppender (new ConsoleAppender (layout));
			logger.setLevel (Level.ALL);
		}
		return logger;
	}
}
