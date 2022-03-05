import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Problem2 {
	static int numGuests;
	static AtomicInteger waiting = new AtomicInteger();
	static Object[] locks;
	static AtomicBoolean[] flags;

	public static void main(String[] args) throws InterruptedException {
		System.out.println("How many guests does the Minotaur have?");
		Scanner scanner = new Scanner(System.in);
		String line = scanner.nextLine();
		long start = System.currentTimeMillis();
		scanner.close();
		
		numGuests = Integer.parseInt(line);
		Thread[] threads = new Thread[numGuests];
		locks = new Object[numGuests];
		flags = new AtomicBoolean[numGuests];
		for (int i = 0; i < numGuests; i++) {
			final int j = i;
			locks[j] = j;
			flags[j] = new AtomicBoolean();
			threads[j] = new Thread(() -> guest(j));
			threads[j].start();
		}
		
		while (waiting.get() != numGuests) {
			// spin until all threads are ready
		}
		if (numGuests > 0) {
			synchronized (locks[0]) {
				locks[0].notify();
			}
		}
		
		for (Thread thread : threads)
			thread.join();
		long time = System.currentTimeMillis() - start;
		System.out.println("Finished in " + time + "ms");
	}

	public static void guest(int index) {
		Object lock = locks[index];
		AtomicBoolean flag = flags[index];
		boolean wait = true;
		
		synchronized (lock) {
			flag.set(true);
			waiting.incrementAndGet();
			while (true) {
				// wait until notified
				if (wait) {
					try {
						lock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				// 50% chance to rejoin queue (i.e. raise flag again)
				boolean rejoin = Math.random() < 0.5;
				flag.set(rejoin);
				
				// find next thread
				int i;
				for (i = (index + 1) % numGuests; i != index; i = (i + 1) % numGuests) {
					synchronized (locks[i]) {
						if (flags[i].compareAndSet(true, false)) {
							locks[i].notify();
							break;
						}
					}
				}
				wait = i != index;
				
				if (!rejoin)
					break;
			}
		}
		
		flag.set(false);
	}

}
