import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Banker {

	// global variable to store the number of units for each resource in the system
	public static Map<Integer, Integer> resourceMap = new HashMap<Integer, Integer>();

	// check if every task is either terminated or aborted
	public static boolean isFinished(ArrayList<Task> tList) {
		boolean sentinel = true;
		for (Task t : tList) {
			if (!t.state.equals("terminated") && !t.state.equals("aborted")) {
				// if anything isn't either of the two states then the program keeps running
				sentinel = false;
			}
		}
		return sentinel;
	}

	// check deadlock for FIFO
	public static boolean isDeadlocked(ArrayList<Task> tList, ArrayList<Task> bList) {
		// if there's nothing blocked, then it's not deadlocked
		if (bList.isEmpty()) {
			return false;
		}
		// if there's something running, then it's not deadlocked
		for (Task t : tList) {
			if (t.state.equals("running"))
				return false;
		}
		// if a request can be granted, then it's not deadlocked
		for (Task b : bList) {
			if (resourceMap.get(Integer.parseInt(b.activityQ.get(0).get(3))) >= Integer.parseInt(b.activityQ.get(0).get(4))) {
				return false;
			}
		}
		// if it gets to this point, then every condition needed for deadlock is met
		return true;
	}

	// check safety for Banker's algorithm
	public static boolean isSafe(Task t) {
		// go through all resources
		for (int key : t.resourceHoldings.keySet()) {
			// if the number of resources available in the system is less than the amount needed by the task
			if (t.resourceClaims.get(key) - t.resourceHoldings.get(key) > resourceMap.get(key)) {
				// block the task and return that it's not safe
				t.state = "blocked";
				return false;
			}
		}
		// return that the task is safe if all the criteria are satisfied
		return true;
	}

	// FIFO method
	public static void fifo(String temp1, int lineCount) {
		// read input
		String[] arr = temp1.split("\\s+");

		ArrayList<Task> tasks = new ArrayList<Task>();
		int t = Integer.parseInt(arr[0]);
		int r = Integer.parseInt(arr[1]);

		// initialize tasks
		for (int i = 1; i < t + 1; i++) {
			tasks.add(new Task(i, r));
		}

		// add resources and their initial values to the system
		for (int i = 2; i < 2 + r; i++) {
			resourceMap.put(i - 1, Integer.parseInt(arr[i]));
		}

		int curr = 2 + r;
		// create an 'activity' which will be added to a data structure in the task class
		for (int i = 0; i < lineCount - 1; i++) {
			String instruction = arr[curr];
			String taskNum = arr[curr + 1];
			String delay = arr[curr + 2];
			String type = arr[curr + 3];
			String numR = arr[curr + 4];
			curr += 5;
			tasks.get(Integer.parseInt(taskNum) - 1).addActivity(instruction, taskNum, delay, type, numR);
		}

		// list to store all tasks that are blocked
		ArrayList<Task> blockedQ = new ArrayList<Task>();

		// continue to run until everything's finished
		while (!isFinished(tasks)) {
			// contains how many of each resource to add at the end of the cycle
			Map<Integer, String> addMap = new HashMap<Integer, String>();
			// contain the tasks that can be resolved, which will then be removed from blockedQ
			ArrayList<Task> reminders = new ArrayList<Task>();
			// iterate through blockedQ to check which tasks can be resolved
			for (Task tempT : blockedQ) {
				ArrayList<String> lmao = tempT.activityQ.get(0);
				// check if the amount requested is less than or equal to the amount in the system
				if (lmao.get(0).equals("request") && Integer.parseInt(lmao.get(4)) <= resourceMap.get(Integer.parseInt(lmao.get(3)))) {
					// set the state to unstarted and update the task holdings and resource map
					tempT.state = "unstarted";
					tempT.resourceHoldings.replace(Integer.parseInt(lmao.get(3)), Integer.parseInt(lmao.get(4) + tempT.resourceHoldings.get(Integer.parseInt(lmao.get(3)))));
					resourceMap.replace(Integer.parseInt(lmao.get(3)), resourceMap.get(Integer.parseInt(lmao.get(3))) - Integer.parseInt(lmao.get(4)));
					reminders.add(tempT);
				}
			}

			// remove resolved tasks from blockedQ
			if (!reminders.isEmpty()) {
				for (Task elem : reminders) {
					blockedQ.remove(elem);
				}
			}

			// iterate through tasks
			for (Task tempT : tasks) {
				ArrayList<String> tempAct = null;
				if (!tempT.activityQ.isEmpty()) {
					// tempAct goes line by line through activityQ in the task
					tempAct = tempT.popActivity();
					// tempAct will equal null when there's a delay
					if (tempAct == null) {
						tempT.timeTaken++;
						continue;
					}
				}

				// check running and blocked tasks
				if (tempT.state.equals("running") || tempT.state.equals("blocked")) {
					if (tempAct.get(0).equals("request")) {
						// gauge request calculate how much of the resource will be left in the system
						int lol = resourceMap.get(Integer.parseInt(tempAct.get(3))) - Integer.parseInt(tempAct.get(4));
						// check if the request can be granted
						if (lol >= 0) {
							// adjust the resourceMap and current resource holdings of the task
							resourceMap.replace(Integer.parseInt(tempAct.get(3)), lol);
							int newVal = tempT.resourceHoldings.get(Integer.parseInt(tempAct.get(3))) + Integer.parseInt(tempAct.get(4));
							tempT.resourceHoldings.replace(Integer.parseInt(tempAct.get(3)), newVal);
							// set the task to running
							tempT.state = "running";
						} else {
							// if the request can't be granted then block the task and add it to the blockedQ
							tempT.state = "blocked";
							if (!blockedQ.contains(tempT))
								blockedQ.add(tempT);
							// re-add the activity back to the front of the activityQ
							tempT.activityQ.add(0, tempAct);
							tempT.waitingTime += 1;
						}
						tempT.timeTaken += 1;
					} else if (tempAct.get(0).equals("release")) {
						// variables for clarity
						int newTotal = Integer.parseInt(tempAct.get(4));
						int index = Integer.parseInt(tempAct.get(3));
						int remainingHold = tempT.resourceHoldings.get(index) - newTotal;

						// addMap takes every resource and how many of the resources that are being released
						// then adds these resources back to the resourceMap at the end of the cycle
						if (addMap.containsKey(index))
							addMap.put(index, Integer.toString(Integer.parseInt(addMap.get(index)) + newTotal));
						else
							addMap.put(index, Integer.toString(newTotal));

						// set the new holdings to however many it had prior minus the quantity released
						tempT.resourceHoldings.put(Integer.parseInt(tempAct.get(3)), remainingHold);
						tempT.timeTaken += 1;

					} else if (tempAct.get(0).equals("terminate")) {
						// terminate if it says terminate
						tempT.state = "terminated";
					}
				}

				if (tempT.state.equals("unstarted")) {
					// terminate if it says terminate
					if (tempAct.get(0).equals("terminate")) {
						tempT.state = "terminated";
					} else if (tempAct.get(0).equals("initiate")) {
						int counter = 1;
						// counter counts number of consecutive initiates
						for (ArrayList<String> tempLst : tempT.activityQ) {
							if (tempLst.get(0).equals("initiate"))
								counter++;
							else
								break;
						}
						// set up state and claims
						tempT.state = "running";
						tempT.resourceClaims.put(Integer.parseInt(tempAct.get(3)) - 1, Integer.parseInt(tempAct.get(4)));
						tempT.timeTaken += counter;
					} else if (tempAct.get(0).equals("request")) {
						// set up state and claims
						tempT.state = "running";
						tempT.resourceClaims.put(Integer.parseInt(tempAct.get(3)) - 1, Integer.parseInt(tempAct.get(4)));
						tempT.timeTaken++;
					}
				}
			}

			// continue running until deadlock is resolved
			while (isDeadlocked(tasks, blockedQ) && !blockedQ.isEmpty()) {
				int lowTaskNum = blockedQ.get(0).taskNumber;
				Task lowestTask = blockedQ.get(0);
				for (Task blockT : blockedQ) {
					if (blockT.taskNumber < lowTaskNum) {
						// get the lowest task number as well as the task that has this number to remove
						lowestTask = blockT;
						lowTaskNum = blockT.taskNumber;
					}
				}
				lowestTask.state = "aborted";
				for (int i = 1; i < resourceMap.size() + 1; i++) {
					int temporary = resourceMap.get(i) + lowestTask.resourceHoldings.get(i);
					resourceMap.put(i, temporary);
				}
				// get rid of the task with the lowest task number
				blockedQ.remove(lowestTask);
			}

			// add the corresponding amounts of input to each resource
			for (int key : addMap.keySet()) {
				int var = resourceMap.get(key) + Integer.parseInt(addMap.get(key));
				resourceMap.replace(key, var);
			}
		}

		// print outputs
		System.out.println("FIFO");
		// variables to store final data
		int totalTime = 0;
		int totalWait = 0;
		for (int i = 1; i < tasks.size() + 1; i++) {
			// only print aborted if aborted
			if (tasks.get(i - 1).state.equals("aborted")) {
				System.out.println("Task " + i + "\taborted");
			} else {
				System.out.println("Task " + i + "\t" + tasks.get(i - 1).timeTaken + " " + tasks.get(i - 1).waitingTime  + " " + tasks.get(i - 1).getWaitingPercentage() + "%");
				totalTime += tasks.get(i - 1).timeTaken;
				totalWait += tasks.get(i - 1).waitingTime;
			}
		}
		// rounding percentages
		double waitPer = (double) totalWait / totalTime;
		System.out.println("Total " + "\t" + totalTime + " " + totalWait + " " + Math.round(waitPer * 100) + "%");
	}

	public static void banker(String temp1, int lineCount) {
		// copy same parsing structure as FIFO
		String[] arr = temp1.split("\\s+");

		ArrayList<Task> tasks = new ArrayList<Task>();
		int t = Integer.parseInt(arr[0]);
		int r = Integer.parseInt(arr[1]);

		for (int i = 1; i < t + 1; i++) {
			tasks.add(new Task(i, r));
		}
		for (int i = 2; i < 2 + r; i++) {
			resourceMap.put(i - 1, Integer.parseInt(arr[i]));
		}

		int curr = 2 + r;
		for (int i = 0; i < lineCount - 1; i++) {
			String instruction = arr[curr];
			String taskNum = arr[curr + 1];
			String delay = arr[curr + 2];
			String type = arr[curr + 3];
			String numR = arr[curr + 4];
			curr += 5;
			tasks.get(Integer.parseInt(taskNum) - 1).addActivity(instruction, taskNum, delay, type, numR);
		}

		ArrayList<Task> blockedQ = new ArrayList<Task>();
		// variable used for printing error trap location
		int cycle = 0;

		// same conditional: keep running until all tasks are aborted or terminated
		// most of the code is identical to FIFO except for key commented parts
		while (!isFinished(tasks)) {
			Map<Integer, String> addMap = new HashMap<Integer, String>();
			ArrayList<Task> reminders = new ArrayList<Task>();

			for (Task tempT : blockedQ) {
				ArrayList<String> lmao = tempT.activityQ.get(0);
				// check for safety instead of seeing if it can be resolved
				if (lmao.get(0).equals("request") && isSafe(tempT)) {
					tempT.state = "unstarted";
					tempT.resourceHoldings.replace(Integer.parseInt(lmao.get(3)), Integer.parseInt(lmao.get(4) + tempT.resourceHoldings.get(Integer.parseInt(lmao.get(3)))));
					resourceMap.replace(Integer.parseInt(lmao.get(3)), resourceMap.get(Integer.parseInt(lmao.get(3))) - Integer.parseInt(lmao.get(4)));
					reminders.add(tempT);
				}
			}

			// deal with tasks that can be granted and still yield a safe state
			if (!reminders.isEmpty()) {
				for (Task elem : reminders) {
					blockedQ.remove(elem);
				}
			}

			// deal with delays
			for (Task tempT : tasks) {
				ArrayList<String> tempAct = null;
				if (!tempT.activityQ.isEmpty()) {
					tempAct = tempT.popActivity();
					if (tempAct == null) {
						// wait and increment time if there's a delay
						tempT.timeTaken++;
						continue;
					}
				}

				// check running and blocked states
				// only one if statement is different from fifo
				if (tempT.state.equals("running") || tempT.state.equals("blocked")) {
					if (tempAct.get(0).equals("request")) {
						int lol = resourceMap.get(Integer.parseInt(tempAct.get(3))) - Integer.parseInt(tempAct.get(4));
						// instead of checking if a request can be granted, check if it yields a safe state
						if (isSafe(tempT)) {
							resourceMap.replace(Integer.parseInt(tempAct.get(3)), lol);
							int newVal = tempT.resourceHoldings.get(Integer.parseInt(tempAct.get(3))) + Integer.parseInt(tempAct.get(4));
							tempT.resourceHoldings.replace(Integer.parseInt(tempAct.get(3)), newVal);
							tempT.state = "running";
						} else {
							tempT.state = "blocked";
							// same as FIFO
							if (!blockedQ.contains(tempT))
								blockedQ.add(tempT);
							tempT.activityQ.add(0, tempAct);
							tempT.waitingTime += 1;
						}
						tempT.timeTaken += 1;
					} else if (tempAct.get(0).equals("release")) {
						// same as FIFO
						int newTotal = Integer.parseInt(tempAct.get(4));
						int remainingHold = tempT.resourceHoldings.get(Integer.parseInt(tempAct.get(3))) - Integer.parseInt(tempAct.get(4));
						int index = Integer.parseInt(tempAct.get(3));

						if (addMap.containsKey(index)) {
							addMap.replace(index, Integer.toString(Integer.parseInt(addMap.get(index)) + newTotal));
						} else {
							addMap.put(Integer.parseInt(tempAct.get(3)), Integer.toString(newTotal));
						}

						tempT.resourceHoldings.replace(Integer.parseInt(tempAct.get(3)), remainingHold);
						tempT.timeTaken += 1;
					} else if (tempAct.get(0).equals("terminate")) {
						tempT.state = "terminated";
					}
				}

				// same as FIFO
				if (tempT.state.equals("unstarted")) {
					if (tempAct.get(0).equals("terminate")) {
						tempT.state = "terminated";
					} else if (tempAct.get(0).equals("initiate")) {
						int counter = 1;
						for (ArrayList<String> tempLst : tempT.activityQ) {
							if (tempLst.get(0).equals("initiate"))
								counter++;
							else
								break;
						}

						tempT.state = "running";
						tempT.resourceClaims.put(Integer.parseInt(tempAct.get(3)), Integer.parseInt(tempAct.get(4)));
						// check if the resource claim exceeds the available quantity in the system
						if (tempT.resourceClaims.get(Integer.parseInt(tempAct.get(3))) > resourceMap.get(Integer.parseInt(tempAct.get(3)))) {
							// error trap with printed statements and setting the state of the task to 'aborted'
							System.out.println("Banker aborts task " + tempT.taskNumber + " before run begins:");
							System.out.println("claim for resourse " + tempAct.get(3) + " (" + Integer.toString(tempT.resourceClaims.get(Integer.parseInt(tempAct.get(3)))) + ") exceeds number of units present (" + Integer.toString(resourceMap.get(Integer.parseInt(tempAct.get(3)))) + ")");
							tempT.state = "aborted";
						}
						tempT.timeTaken += counter;
					} else if (tempAct.get(0).equals("request")) {
						int counter = 1;
						for (ArrayList<String> tempLst : tempT.activityQ) {
							if (tempLst.get(0).equals("initiate"))
								counter++;
							else
								break;
						}

						tempT.state = "running";
						tempT.resourceClaims.put(Integer.parseInt(tempAct.get(3)), Integer.parseInt(tempAct.get(4)));
						tempT.timeTaken += counter;
					}
				}
			}

			// deadlock check removed due to redundancy
			// will never occur in a Banker's algorithm simulation

			for (int key : addMap.keySet()) {
				int var = resourceMap.get(key) + Integer.parseInt(addMap.get(key));
				resourceMap.replace(key, var);
			}
			// increment cycle to keep track of time
			cycle++;
		}

		// print output
		System.out.println("BANKER'S");
		int totalTime = 0;
		int totalWait = 0;
		for (int i = 1; i < tasks.size() + 1; i++) {
			// only print 'aborted' for aborted tasks
			if (tasks.get(i - 1).state.equals("aborted")) {
				System.out.println("Task " + i + "\taborted");
			} else {
				// print out terminated tasks and total their times
				System.out.println("Task " + i + "\t" + tasks.get(i - 1).timeTaken + " " + tasks.get(i - 1).waitingTime  + " " + tasks.get(i - 1).getWaitingPercentage() + "%");
				totalTime += tasks.get(i - 1).timeTaken;
				totalWait += tasks.get(i - 1).waitingTime;
			}
		}
		// int -> double -> int casting to get more visually appealing output
		double waitPer = (double) totalWait / totalTime;
		System.out.println("Total " + "\t" + totalTime + " " + totalWait + " " + Math.round(waitPer * 100) + "%");
	}

	public static void main(String[] args) throws IOException {
		File file = new File(args[0]);
		@SuppressWarnings("resource")
		BufferedReader br = new BufferedReader(new FileReader(file));
		StringBuilder stored = new StringBuilder();
		String s;
		int lineCount = 0;
		// parse data from input to a string
		while ((s = br.readLine()) != null) {
			stored.append(s.trim() + " ");
			if (!s.isEmpty())
				lineCount++;
		}

		// isolate string elements into an array
		String temp1 = stored.toString();
		// run fifo method
		fifo(temp1, lineCount);
		System.out.println();
		// run banker method
		banker(temp1, lineCount);
	}
}
