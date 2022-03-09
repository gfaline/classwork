import java.util.Arrays;
import com.supermarket.*;
import java.lang.Math;
import java.util.ArrayList;

public class Agent extends SupermarketComponentImpl {

    public Agent() {
	super();
	shouldRunExecutionLoop = true;
	log.info("In Agent constructor.");
    }

    boolean firsttime = true;
	String state = "entry";
	// Options are a shelf item, counter item, cart return, checkout

	String goal = "leek";
	double[] goalPosition;
	ArrayList<String> shopping_list;
	boolean atHub = false;
	Observation.InteractiveObject relevantObj;
    
    @Override
    protected void executionLoop() {
	// this is called every 100ms
	// put your code in here
	Observation obs = getLastObservation();
	//System.out.println("Target at " + obs.players[0].position[0] + " " + obs.players[0].position[1]);
	System.out.println(obs.players.length + " players at " + obs.players[0].position[0] + " " + obs.players[0].position[1]);
	//Player player = obs.players[0];
	// This loop will find the position of the target given that the target is among expected items
	// Shelf usually withe 2.25 1.5 or 1.0 2.0
	if (firsttime) {
		System.out.println(obs.carts.length + " carts");
		System.out.println(obs.shelves.length + " shelves");
		System.out.println(obs.counters.length + " counters");
		System.out.println(obs.registers.length + " registers");
		System.out.println(obs.cartReturns.length + " cartReturns");
		// print out the shopping list
		System.out.println("Shoppping list: " + Arrays.toString(obs.players[0].shopping_list));
		System.out.println(obs.players[0].shopping_list.getClass());
		firsttime = false;
		shopping_list = new ArrayList<String>(Arrays.asList(obs.players[0].shopping_list));
		shopping_list.add(0, "cartReturn");
		shopping_list.add(shopping_list.size() - 1, "register");

		//goal = shopping_list.get(1);
		System.out.println(goal);

	}
	System.out.println(goal);

	if (goal.equals("checkout") || goal.equals("register")){
		relevantObj = obs.registers[0];
		goalPosition = relevantObj.position;
		//System.out.println("Shelf height and width: " + relevantObj.height + " " + relevantObj.width);
	}

	if (goal.equals("cartReturn")){
		relevantObj = obs.cartReturns[0];
		goalPosition = relevantObj.position;
		//System.out.println("Shelf height and width: " + relevantObj.height + " " + relevantObj.width);
	}

	for (Observation.Counter ct : obs.counters){
		if (ct.food.equals(goal)){
			relevantObj = ct;
			goalPosition = ct.position;
			//System.out.println("Shelf height and width: " + ct.height + " " + ct.width);
		}
	}

	for(Observation.Shelf sh: obs.shelves){
		if (sh.food.equals(goal)){
			relevantObj = sh;
			goalPosition = sh.position;
			//System.out.println("Shelf height and width: " + sh.height + " " + sh.width);
		}
	}

	if (relevantObj==null){
		System.out.println("There is no target object, it will just error from here");
	}
	System.out.println("Target at " + goalPosition[0] + " " + goalPosition[1]);

	//System.out.println(obs.interactive_stage);
	//System.out.println(obs.players[0].position[1]);

	// if the height is within 1 of the position, it can just go sideways... Unless it's a counter or checkout.
	//
	double nextX = obs.players[0].position[0];
	double nextY = obs.players[0].position[1];

	if (goal.equals("checkout") || goal.equals("register") || goal.equals("cartReturn")){
		if (!atHub)
			goToAisleHub(obs, obs.players[0]);
		if (goal.equals("checkout") || goal.equals("register")){
			goToY(obs, obs.players[0], goalPosition[1]);
			approachRegister(obs, relevantObj, obs.players[0]);
		}
		else{
			System.out.println(relevantObj.position[1]);
			approachCartReturn(obs, relevantObj, obs.players[0]);
		}
	}

	else if (relevantObj.getClass().equals(Observation.Counter.class)){
		System.out.println("The class equals gets a result of a counter item");
		if (!atHub)
			goToRearAisleHub(obs, obs.players[0]);
		goToY(obs, obs.players[0], goalPosition[1]);
		approachCounter(obs, relevantObj, obs.players[0]);
		//goToX(obs, obs.players[0], goalPosition[0]);
		//Here we want to be inRearAisleHub or besideCounters
	}

	else if (relevantObj.getClass().equals(Observation.Shelf.class)){
		boolean inAisle = amInAisle(obs, obs.players[0]);
		//goTo aisle hub. Go to y cord - width. Go to x cord. go north/south (whichever is the one supposed to)
		if (!atHub)
			goToAisleHub(obs, obs.players[0]);
		// Has it go south of the aisle
		goToY(obs, obs.players[0], relevantObj.position[1] + (Math.ceil(relevantObj.height/2)) + .1);
		goToX(obs, obs.players[0], relevantObj.position[0] + .7, relevantObj.position[1] + (Math.ceil(relevantObj.height/2)) + .1);
		approachShelf (obs, relevantObj, obs.players[0]);
		//goNorth();

	}

    }

	private void goToAisleHub(Observation obs, Observation.Player ply){
		double x = ply.position[0];
		double y = ply.position[1];

		if (!obs.inAisleHub(ply.index)){
			//System.out.println("Player x y: " + x + " " + y);
			if (x > (4.5 - ply.width)){
				//Check for collission on next step
				double next = x + .3;
				boolean willMoveCollide = detectCollison(obs, next, y);
				if (willMoveCollide){
					next = y - .3;
					willMoveCollide = detectCollison(obs, x, next);
					if (willMoveCollide){
						next = y + .3;
						willMoveCollide = detectCollison(obs, x, next);
						if(willMoveCollide){
							System.out.println("This is just broken at this point");
						}
						else {
							if (ply.direction != 0)
								goNorth();
							goNorth();
						}

					}
					else {
						if (ply.direction != 1)
							goSouth();
						goSouth();
						//System.out.println("Going south");
					}
				}
				else{
					if (ply.direction != 2)
						goWest();
					goWest();
				}
			}
			else{
				if (ply.direction != 3)
					goEast();
				goEast();
				//System.out.println("Going west");
			}
		}
		else if (!atHub){
			// If you are here, you're in the asile hub but may not be clear of the basket. Manually move you to be out of the way of the basket.
			if (ply.direction != 3)
				goEast();
			goEast();
			goEast();
			goEast();
			goEast();
			atHub = true;
		}
		else{
			atHub = true;
		}
	}

	private void goToRearAisleHub(Observation obs, Observation.Player ply){
		double x = ply.position[0];
		double y = ply.position[1];

		if (!obs.inRearAisleHub(ply.index)){
			//System.out.println("Player x y: " + x + " " + y);
			if (x < (15.5 + ply.width)){
				//Check for collission on next step
				double next = x + .3;
				boolean willMoveCollide = detectCollison(obs, next, y);
				if (willMoveCollide){
					next = y - .3;
					willMoveCollide = detectCollison(obs, x, next);
					if (willMoveCollide){
						next = y + .3;
						willMoveCollide = detectCollison(obs, x, next);
						if(willMoveCollide){
							System.out.println("This is just broken at this point");
						}
						else {
							if (ply.direction != 0)
								goNorth();
							goNorth();
						}

					}
					else {
						if (ply.direction != 1)
							goSouth();
						goSouth();
						//System.out.println("Going south");
					}
				}
				else{
					if (ply.direction != 2)
						goEast();
					goEast();
				}
			}
			else{
				if (ply.direction != 3)
					goWest();
				goWest();
				//System.out.println("Going west");
			}
		}
		else{
			atHub = true;
		}
	}

	private boolean detectCollison(Observation obs, double x, double y){
		boolean tempVal = false;

		for (Observation.InteractiveObject obj: obs.shelves){
			tempVal = obj.collision(obs.players[0], x, y);
			if(tempVal)
				return true;
		}

		for (Observation.InteractiveObject obj: obs.counters){
			tempVal = obj.collision(obs.players[0], x, y);
			if(tempVal)
				return true;
		}

		for (Observation.InteractiveObject obj: obs.carts){
			tempVal = obj.collision(obs.players[0], x, y);
			if(tempVal)
				return true;
		}

		for (Observation.InteractiveObject obj: obs.registers){
			tempVal = obj.collision(obs.players[0], x, y);
			if(tempVal)
				return true;
		}

		for (Observation.InteractiveObject obj: obs.cartReturns){
			tempVal = obj.collision(obs.players[0], x, y);
			if(tempVal)
				return true;
		}

		return false;
	}

	private void goToY(Observation obs, Observation.Player ply, double targetY){
		double ydiff = Math.abs(targetY +  (relevantObj.height/2.0) - ply.position[1]);
		double vol_ydiff = (targetY + (relevantObj.height/2.0)) - ply.position[1];
		double width_diff =  (relevantObj.height/2.0) + ply.width/2.0;
		//if(ydiff > .3 || (ply.position[1] + ply.width/2.0  > targetY + relevantObj.height/2.0 && ply.direction == 1)){
		if(ydiff > .4 || (vol_ydiff <= .3 && vol_ydiff > 0)){
			System.out.println("I need to move in the Y direction");
			System.out.println("The volitile difference is " + vol_ydiff);
			if (targetY + (relevantObj.height/2.0) > ply.position[1]){
				if (ply.direction != 1)
					goSouth();
				goSouth();
			}
			else if (ydiff > .4){
				if (ply.direction != 0)
					goNorth();
				goNorth();
			}
		}
	}

	private void goToX(Observation obs, Observation.Player ply, double targetX, double targetY){
		double xdiff = Math.abs(targetX - ply.position[0]);
		double ydiff = Math.abs(targetY + (relevantObj.height/2.0) - ply.position[1]);
		if (ydiff < .5 && xdiff > .3){
			System.out.println("I need to move in the X direction");
			if (targetX > ply.position[0]){
				if (ply.direction != 2)
					goEast();
				goEast();
			}
			else{
				if (ply.direction != 3)
					goWest();
				goWest();
			}
		}
	}

	private void approachCounter(Observation obs, Observation.InteractiveObject counter, Observation.Player ply){
		double ydiff = Math.abs(counter.position[1] - ply.position[1]);
		if (ydiff < .35 && !counter.collision(ply, ply.position[0], ply.position[1] + .3)){
			System.out.println("I need to approach the counter");
			goEast();
		}
	}

	private void approachRegister (Observation obs, Observation.InteractiveObject counter, Observation.Player ply){
		double ydiff = Math.abs(counter.position[1] - ply.position[1]);
		if (ydiff < .35 && !counter.collision(ply, ply.position[0] - .3, ply.position[1])){
			System.out.println("I need to approach the register");
			goWest();
		}

	}

	private void approachShelf  (Observation obs, Observation.InteractiveObject counter, Observation.Player ply){
		double ydiff = Math.abs(counter.position[1] + (relevantObj.height/2.0) - ply.position[1]);
		double xdiff = Math.abs(counter.position[0] - ply.position[0]);
		System.out.println("Xdiff: " + xdiff + ", Ydiff: " + ydiff);
		if (ydiff < 1.5 && xdiff < .5){
			if (!counter.collision(ply, ply.position[0], ply.position[1] - .3)){
				goNorth();
			}
			System.out.println("I need to approach the shelf");
			if (ply.direction != 0)
				goNorth();
			//goNorth();
		}
	}

	private void approachCartReturn(Observation obs, Observation.InteractiveObject counter, Observation.Player ply){
		double ydiff = Math.abs(counter.position[1] - ply.position[1]);
		double xdiff = Math.abs(counter.position[0] - ply.position[0]);
		if (obs.northOfCartReturn(ply.index)){
			if(ply.direction != 1)
				goSouth();
			goSouth();
		}
		else if (obs.southOfCartReturn(ply.index)){
			if (ply.direction != 0)
				goNorth();
			goNorth();
		}
		else if (xdiff > .35 && !counter.collision(ply, ply.position[0] - .3, ply.position[1])){
			if (ply.direction !=2)
				goWest();
			goWest();
		}
		else if (ply.direction != 1){
			goSouth();
		}

		if(obs.atCartReturn(ply.index)){
			toggleShoppingCart();
		}
	}

	private void nextAisle(Observation obsv, Observation.Player ply, int dir){
		//dir is 0 for north and 1 for south just like getdir case stuff
	}

	private boolean amInAisle(Observation obs, Observation.Player ply){
		boolean amIn = false;

		for (int i = 0; i > 10; i++) {
			try {
				boolean retFromCall = obs.inAisle(ply.index, i);
				if (retFromCall){
					amIn = true;
				}
			} catch (Exception e) {
				System.out.println(e);
				break;
			}
		}
		return amIn;
	}

	private boolean amAtTop(Observation obs, Observation.Player ply){
		return false;
	}

	private boolean amAtBottom(Observation obs, Observation.Player ply){
		return false;
	}

	//I am not sure if 0 is the top or bottom aisle
}
