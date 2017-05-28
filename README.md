threadTerminal
===========================================================================

ThreadTerminal is a framework for scheduling and controlling task execution 
in a java pool executor.

The main difference:
1. Tasks are reordered inside the queue based on their priority level. You assign each 
task's priority when you submit them for execution.
2. Each task can be executed exclusively: that means, all other threads will be blocked for 
the time of its execution.

There are two priority levels: HIGH and LOW. If you submit a task with a HIGH level to the 
ThreadTerminal, it will be executed next after the last task with the HIGH level in the queue,
a task with a LOW level is put to the end of the queue.
Thus tasks are ordered by their priority level and inside one level by 
the order of submission.

## Notes:

* if you have N threads in a pool, the first N submitted tasks are not guaranteed to preserve the order of 
execution based on the priority and the order of submission.
*  priority and submission order among exclusive tasks is always guaranteed by their nature: in fact, they use the executor
with no matter how large pool as a single-threaded executor.
* even if tasks are released by the queue correctly in terms of order, there is no guarantee that once they
are put each in its own thread inside the pool, they will be executed in the same order.

Basically, this framework is ideal when you have many independant parallel tasks executed non-stop 
(refreshing data frequently in your app), but from time to time you need some task either to use all available resources
or just avoid shared mutability with refreshing tasks and to be executed ASAP.

## Dependencies: 
* log4j for logging
* junit for testing

## Basic usage
1. Create new terminal with some name and a pool of 4 threads.
```java
ThreadTerminal terminal = Terminals.newThreadTerminal ("termini", 4);
```

2. Create new terminal with some name and a pool of 4 threads.
```java
ThreadTerminal terminal = Terminals.newThreadTerminal ("termini", 4);
```

3. Submit new Callable() with the name "prettyCallable", low priority (will be executed in queue order) and of no exclusivity.
```java
terminal.submit (newCallable (), "prettyCallable", Priority.LOW, false);
```

4. Submit new Callable() with the name "exclusiveCallable", high priority (will be executed next) and exclusive (will be executed alone in the whole pool).
```java
terminal.submit (newCallable (), "exclusiveCallable", Priority.HIGH, true);
```

At any time you can get info about terminal using
```java
TerminalState state = terminal.snapshotTerminalState ();
```

For more info on usage see Main.java and tests.