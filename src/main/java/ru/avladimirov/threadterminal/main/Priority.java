package ru.avladimirov.threadterminal.main;

/**
 * An enum for defining prioirity levels. Thus a user can use only levels
 * provided by this enum.
 *
 * @author Vladimirov.A.A
 */
public enum Priority {

	HIGH (1),
	LOW (2),;

	private int value;

	Priority (int value) {
		this.value = value;
	}

	public int getValue () {
		return value;
	}
}
