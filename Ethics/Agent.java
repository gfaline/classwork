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
	boolean hasCart = false;
	// Options are a shelf item, counter item, cart return, checkout

	String goal = "leek";
	int num_goal;
	double[] goalPosition;
	ArrayList<String> shopping_list;
	ArrayList<Integer> quantity_list;
	boolean atHub = false;
	Observation.InteractiveObject relevantObj;
    
    @Override
    protected void executionLoop() {
		// this is called every 100ms
		// put your code in here
		Observation obs = getLastObservation();
		Observation.Player player = obs.players[0];
		//System.out.println("Target at " + obs.players[0].position[0] + " " + obs.players[0].position[1]);
		System.out.println(obs.players.length + " players at " + player.position[0] + " " + player.position[1]);
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
			System.out.println("Shoppping list: " + Arrays.toString(player.shopping_list));
			System.out.println(player.shopping_list.getClass());
			firsttime = false;
			shopping_list = new ArrayList<String>(Arrays.asList(player.shopping_list));
			shopping_list.add(0, "cartReturn");
			shopping_list.add(shopping_list.size(), "register");

			quantity_list = new ArrayList<Integer>(Arrays.stream(player.list_quant).boxed().toList());
			quantity_list.add(0, -1);
			quantity_list.add(quantity_list.size(), -1);

			goal = shopping_list.get(0);
			System.out.println(goal);
			System.out.println("New Shoppping list: " + shopping_list);

			System.out.println(player.holding_food);

		}
		System.out.println(goal);
		
		if (goal.equals("checkout") || goal.equals("register")) {
			relevantObj = obs.registers[0];
			goalPosition = relevantObj.position;
			//System.out.println("Shelf height and width: " + relevantObj.height + " " + relevantObj.width);
		}

		if (goal.equals("cartReturn")) {
			relevantObj = obs.cartReturns[0];
			goalPosition = relevantObj.position;
			//System.out.println("Shelf height and width: " + relevantObj.height + " " + relevantObj.width);
		}

		for (Observation.Counter ct : obs.counters) {
			if (ct.food.equals(goal)) {
				relevantObj = ct;
				goalPosition = ct.position;
				//System.out.println("Shelf height and width: " + ct.height + " " + ct.width);
			}
		}

		for (Observation.Shelf sh : obs.shelves) {
			if (sh.food.equals(goal)) {
				relevantObj = sh;
				goalPosition = sh.position;
				//System.out.println("Shelf height and width: " + sh.height + " " + sh.width);
			}
		}

		if (relevantObj == null) {
			System.out.println("There is no target object, it will just error from here");
		}
		System.out.println("Target at " + goalPosition[0] + " " + goalPosition[1]);

		//System.out.println(obs.interactive_stage);
		//System.out.println(obs.players[0].position[1]);

		// if the height is within 1 of the position, it can just go sideways... Unless it's a counter or checkout.
		//
		double nextX = player.position[0];
		double nextY = player.position[1];

		System.out.println("Am I in the asile hub? " + atHub + ". And now my emperical value: " + obs.inAisleHub(obs.players[0].index));

		if (goal.equals("checkout") || goal.equals("register") || goal.equals("cartReturn")) {
			if (!atHub)
				goToAisleHub(obs, player);
			if (goal.equals("checkout") || goal.equals("register")) {
				goToY(obs, player, goalPosition[1]);
				approachRegister(obs, relevantObj, player);
			} else {
				System.out.println(relevantObj.position[1]);
				approachCartReturn(obs, relevantObj, player);
			}
		} else if (relevantObj.getClass().equals(Observation.Counter.class)) {
			System.out.println("The class equals gets a result of a counter item");
			if (!atHub)
				goToRearAisleHub(obs, player);
			goToY(obs, player, goalPosition[1]);
			approachCounter(obs, relevantObj, player);
			// if (playerIsHoldingFood(player)) {
			// 	returnToCart(obs, player);
			// }
			//goToX(obs, obs.players[0], goalPosition[0]);
			//Here we want to be inRearAisleHub or besideCounters
		} else if (relevantObj.getClass().equals(Observation.Shelf.class)) {
			boolean inAisle = amInAisle(obs, player);
			//goTo aisle hub. Go to y cord - width. Go to x cord. go north/south (whichever is the one supposed to)
			if (!atHub)
				goToAisleHub(obs, player);
			// Has it go south of the aisle
			goToY(obs, player, relevantObj.position[1] + (Math.ceil(relevantObj.height / 2)) + .1);
			goToX(obs, player, relevantObj.position[0] + (Math.ceil(relevantObj.width / 2)), relevantObj.position[1] + (Math.ceil(relevantObj.height / 2)) + .1);
			approachShelf(obs, relevantObj, player);
			if (playerIsHoldingFood(player)) {
				System.out.println("here");
				returnToCart(obs, player);
			}
		} 

	}

	private void goToAisleHub(Observation obs, Observation.Player ply){
		double x = ply.position[0];
		double y = ply.position[1];

		if (!obs.inAisleHub(ply.index) && !acceptableYNearness(ply)){
			System.out.println("I am going to the aisle hub");
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
		else if (!atHub && obs.inAisleHub(ply.index)){
			System.out.println("I am moving out of the way of the basket");
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

		if (!obs.inRearAisleHub(ply.index) && !acceptableYNearness(ply)){
			System.out.println("I am going to the rear aisle hub");
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
		Observation.Player player = obs.players[0];

		for (Observation.InteractiveObject obj: obs.shelves){
			tempVal = obj.collision(player, x, y);
			if(tempVal)
				return true;
		}

		for (Observation.InteractiveObject obj: obs.counters){
			tempVal = obj.collision(player, x, y);
			if(tempVal)
				return true;
		}

		for (Observation.InteractiveObject obj: obs.carts){
			tempVal = obj.collision(player, x, y);
			if(tempVal)
				return true;
		}

		for (Observation.InteractiveObject obj: obs.registers){
			tempVal = obj.collision(player, x, y);
			if(tempVal)
				return true;
		}

		for (Observation.InteractiveObject obj: obs.cartReturns){
			tempVal = obj.collision(player, x, y);
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
		if (obs.inAisleHub(ply.index) || obs.inRearAisleHub(ply.index)) {
			if (ydiff > .4 || (vol_ydiff > .06 && vol_ydiff <= .2 && vol_ydiff > 0 && ply.direction == 1)) {
				System.out.println("I need to move in the Y direction");
				System.out.println("The volitile difference is " + vol_ydiff);
				if (targetY + (relevantObj.height / 2.0) > ply.position[1]) {
					if (ply.direction != 1)
						goSouth();
					goSouth();
				} else if (ydiff > .4) {
					if (ply.direction != 0)
						goNorth();
					goNorth();
				}
			}
		}
	}

	private void goToX(Observation obs, Observation.Player ply, double targetX, double targetY){
		//double xdiff = Math.abs(targetX +  (relevantObj.width/2.0) - ply.position[0]);
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

	private void approachRegister (Observation obs, Observation.InteractiveObject register, Observation.Player ply){
		double ydiff = Math.abs(register.position[1] - ply.position[1]);
		if (ydiff < .35 && !register.collision(ply, ply.position[0] - .3, ply.position[1])){
			System.out.println("I need to approach the register");
			goWest();
		}

	}

	private void approachShelf  (Observation obs, Observation.InteractiveObject shelf, Observation.Player ply){
		double ydiff = Math.abs(shelf.position[1] + (relevantObj.height/2.0) - ply.position[1]);
		double xdiff = Math.abs(shelf.position[0] + (relevantObj.width/2.0)- ply.position[0]);
		System.out.println("Xdiff: " + xdiff + ", Ydiff: " + ydiff);
		// if you're next to the shelf
		if (ydiff < 1.5 && xdiff < .5){
			// if you're holding the cart, let go
			if (hasCart) {
				toggleShoppingCart();
				hasCart = false;
				if (ply.direction == 2) {
					goWest();
					goWest();
					goWest();
				} else if (ply.direction == 3) {
					goEast();
					goEast();
					goEast();
				}
			}
			System.out.println("holding_food: " + ply.holding_food);
			if (!shelf.collision(ply, ply.position[0], ply.position[1] - .3)){
				System.out.println("here 3");
				// if you're still below the shelf, go up
				goNorth();
			} else if (ply.holding_food == null || ply.holding_food.equals("")) {
				System.out.println("here 4");
				// if you're at the shelf and you're not holding an item yet
				if (ply.direction != 0) {
					// make sure you're facing the right direction
					goNorth();
				}
				if (shelf.canInteract(ply)) {
					// grab the item
					interactWithObject();
				}
				// System.out.println(ply.holding_food);
			} else {
				// you're holding food. return to the cart
				
			}
			System.out.println("I need to approach the shelf");

			// if (ply.direction != 0)
			// 	goNorth();
			//goNorth();
		}
	}

	private void returnToCart(Observation obs, Observation.Player player) {
		System.out.println(player.curr_cart);
		Observation.Cart goal_cart = obs.carts[player.curr_cart];
		System.out.println("Returning to cart");
	}

	private void approachCartReturn(Observation obs, Observation.InteractiveObject cartReturn, Observation.Player ply){
		double ydiff = Math.abs(cartReturn.position[1] - ply.position[1]);
		double xdiff = Math.abs(cartReturn.position[0] - ply.position[0]);
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
		else if (xdiff > .35 && !cartReturn.collision(ply, ply.position[0] - .3, ply.position[1])){
			if (ply.direction !=2)
				goWest();
			goWest();
		}
		else if (ply.direction != 1){
			goSouth();
		}

		if(obs.atCartReturn(ply.index) && !hasCart){
			System.out.println("I am at the cart return");
			goSouth();
			interactWithObject();
			hasCart = true;
			goNorth();
			goNorth();
			goNorth();
			goNorth();
			goNorth();
			goNorth();
			goEast();
			goEast();
			goEast();
			crossOffItem();
		}
	}

	private int getAisleIndex(Observation.Shelf shelf) {
		double y_position = shelf.position[1];
		// gets the index of the aisle below a shelf
		return (int)((y_position - 1.5)/4 + 1);
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

	private boolean acceptableYNearness(Observation.Player ply){
		double targetY = goalPosition[1];
		double ydiff = Math.abs(targetY +  (relevantObj.height/2.0) - ply.position[1]);
		double vol_ydiff = (targetY + (relevantObj.height/2.0)) - ply.position[1];
		double width_diff =  (relevantObj.height/2.0) + ply.width/2.0;

		return !(ydiff > .4 || (vol_ydiff > .06 && vol_ydiff <= .2 && vol_ydiff > 0 && ply.direction == 1));

	}

	private void crossOffItem() {
		if (shopping_list.size() > 0) {
			// remove the item you just got
			shopping_list.remove(0);
			quantity_list.remove(0);
			atHub = false;
		}
		if (shopping_list.size() > 0) {
			// make the next item the target
			goal = shopping_list.get(0);
			num_goal = quantity_list.get(0);
		}
		else{
			leave();
		}

	}

	private boolean leave(){
		return true;
	}

	private boolean playerIsHoldingFood(Observation.Player player) {
		String holding_food = player.holding_food;
		return !(holding_food == null || holding_food.equals(""));
	}

	private boolean playerIsHoldingCart(Observation.Player player) {
		return player.curr_cart != -1;
	}

	//I am not sure if 0 is the top or bottom aisle
}
