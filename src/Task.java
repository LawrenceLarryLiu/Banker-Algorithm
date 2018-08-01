import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Task {
	public int taskNumber; // priority management in abortions
	public int numRes; // number of resources
	public String state; // unstarted, running, blocked, terminated
	public Map<Integer, Integer> resourceClaims; // map of resource claims
	public Map<Integer, Integer> resourceHoldings; // map of current resource holdings
	public ArrayList<ArrayList<String>> activityQ; // list of all things needed to be done for each task
	public int timeTaken; // time taken
	public int waitingTime; // time spend waiting
	public int waitingPercentage; // percentage of total time spent waiting

	// constructor to initialize all instance variables of a task
	public Task(int taskNum, int numResources) {
		taskNumber = taskNum;
		numRes = numResources;
		state = "unstarted";
		resourceClaims = new HashMap<Integer, Integer>();
		resourceHoldings = new HashMap<Integer, Integer>();

		// initialize each resource map with the number of resources of the input
		// everything set to 0 because it'll be changed later
		for (int i = 0; i < numResources; i++) {
			resourceClaims.put(i + 1, 0);
			resourceHoldings.put(i + 1, 0);
		}
		activityQ = new ArrayList<ArrayList<String>>();
	}

	// add an activity to the activity queue
	public void addActivity(String inst, String num, String delay, String type, String numR) {
		ArrayList<String> temp = new ArrayList<String>();
		temp.add(inst);
		temp.add(num);
		temp.add(delay);
		temp.add(type);
		temp.add(numR);
		activityQ.add(temp);
	}

	// pop an activity
	public ArrayList<String> popActivity() {
		// only pop from activity queue when the delay hits zero
		if (state.equals("aborted"))
			return null;
		if (activityQ.get(0).get(2).equals("0")) {
			return activityQ.remove(0);
		} else {
			// if the delay isn't zero, then deincrement and return null
			activityQ.get(0).set(2, "" + (Integer.parseInt(activityQ.get(0).get(2)) - 1));
			return null;
		}
	}

	// calculation method for getting the wait percentage
	public int getWaitingPercentage() {
		double temp1 = (double) waitingTime / timeTaken;
		return (int) Math.round(temp1 * 100);
	}
}
