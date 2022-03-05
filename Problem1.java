import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Problem1 {
	static final AtomicBoolean EATEN = new AtomicBoolean(false);
	static final AtomicBoolean ANNOUNCE = new AtomicBoolean(false);
	static final Object MINOTAUR = new Object();
	static int numGuests;

	public static void main(String[] args) throws InterruptedException {
		System.out.println("How many guests does the Minotaur have?");
		Scanner scanner = new Scanner(System.in);
		String line = scanner.nextLine();
		scanner.close();
		
		numGuests = Integer.parseInt(line);
		Thread[] threads = new Thread[numGuests];
		Object[] locks = new Object[numGuests];
		for (int i = 0; i < numGuests; i++) {
			final int j = i;
			locks[j] = new Object();
			threads[j] = new Thread(() -> guest(j == 0, locks[j]));
			threads[j].start();
		}
		
		Random rand = new Random();
		int count = 0;
		boolean[] visited = new boolean[numGuests];
		synchronized (MINOTAUR) {
			while (true) {
				// pick a random guest
				int i = rand.nextInt(numGuests);
				Object guest = locks[i];
				
				// wake up guest thread
				synchronized (guest) {
					guest.notify();
				}
				// wait for guest to respond
				MINOTAUR.wait();
				visited[i] = true;
				
				// if guests announce everyone has visited, end the game
				if (ANNOUNCE.get())
					break;
				
				// count visits to show progress is being made, no deadlocks
				if (++count % 100000 == 0)
					System.out.println(count + " visits made");
			}
		}
		System.out.println(count + " visits made");
		
		// Game over, interrupt threads
		for (Thread thread : threads)
			thread.interrupt();
		
		System.out.println("Guests: Everyone has visited the labyrinth");
		if (checkAllVisited(visited))
			System.out.println("Minotaur: That is correct");
		else
			System.out.println("Minotaur: That is incorrect");
	}

	public static boolean checkAllVisited(boolean[] visited) {
		for (boolean b : visited)
			if (b == false)
				return false;
		return true;
	}

	// code for the guests
	public static void guest(boolean isCounter, Object guest) {
		int count = 1; // used by counter; counter counts himself at the start
		boolean ate = false; // used by other guests; if the guest has eaten, they shouldn't eat again
		
		synchronized (guest) {
			while (true) {
				// wait to be called into the labyrinth
				try {
					guest.wait();
				} catch (InterruptedException e) {
					// Game over
					return;
				}
				
				// in the labyrinth
				
				// logic for counter
				if (isCounter) {
					// if eaten==true, at least 1 guest has visited
					// set eaten = false
					if (EATEN.getAndSet(false)) {
						count++;
						if (count == numGuests)
							ANNOUNCE.set(true);
					}
				}
				// logic for other guests
				else {
					// if you haven't already eaten, and a cake is available, eat the cake
					if (!ate && !EATEN.getAndSet(true)) {
						ate = true;
					}
				}
				
				// out of the labyrinth, let minotaur know
				synchronized (MINOTAUR) {
					MINOTAUR.notify();
				}
			}
		}
	}

}
