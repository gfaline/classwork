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
	//string[] testingShoppingList = ["cheese wheel", "steak", "fish"];
	int num_goal;
	double[] goalPosition;
	ArrayList<String> shopping_list;
	ArrayList<Integer> quantity_list;
	boolean atHub = false;
	boolean checkoutOut = false;
	Observation.InteractiveObject relevantObj;

	int cart_index = -1;
	double[] returnToCartPosition;
	Observation.Cart cart;

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
			//shopping_list = new ArrayList<String>(Arrays.asList(player.shopping_list));
			shopping_list = new ArrayList<String>();
			shopping_list.add(0, "cheese wheel");
			shopping_list.add(0, "cheese wheel");
			shopping_list.add(0, "brie cheese");

			// shopping_list.add(0, "fish");
			shopping_list.add(0,"prepared foods");
			// the name of the counter is different than the name of the food. hard-code the correction
			for (int i=0; i < shopping_list.size(); i++) {
				if (shopping_list.get(i).equals("fish")) {
					shopping_list.set(i, "fresh fish"); 
				}
			}
			// TODO: Add a line checking if holding cart before doing this
			shopping_list.add(0, "cartReturn");
			shopping_list.add(shopping_list.size(), "register");

			//quantity_list = new ArrayList<Integer>(Arrays.stream(player.list_quant).boxed().toList());
			quantity_list = new ArrayList<Integer>();
			quantity_list.add(0, 1);
			//quantity_list.add(0, 1);
			//quantity_list.add(0, 1);
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
			//System.out.println("Counter food type: ");
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

		// get most recent info about the cart
		if (cart_index != -1) {
			cart = obs.carts[cart_index];
		}

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
				if(playerIsHoldingCart(player) || checkoutOut)
					goToY(obs, player, goalPosition[1] + 1.0);
				else
					goToY(obs, player, goalPosition[1]);
				approachRegister(obs, relevantObj, player);
			} else {
				System.out.println(relevantObj.position[1]);
				approachCartReturn(obs, relevantObj, player);
			}
		} else if (relevantObj.getClass().equals(Observation.Counter.class)) {
			// System.out.println("The class equals gets a result of a counter item");
			// if (!atHub)
			// 	goToRearAisleHub(obs, player);
			// // goToY(obs, player, goalPosition[1]);
			// approachCounter(obs, relevantObj, player);
			// if (playerIsHoldingFood(player)) {
			// 	// returnToCart(obs, player);
			// }
			// if ( false/*player has enough of the thing*/){}
			//goToX(obs, obs.players[0], goalPosition[0]);
			//Here we want to be inRearAisleHub or besideCounters

			if (!playerHasItemQuantity(cart, goal)){
				// you don't have enough of the thing that you need
				if (!atHub)
					goToRearAisleHub(obs, player);
				approachCounter(obs, relevantObj, player);
				if (playerIsHoldingFood(player) /*&& !playerIsHoldingCart(player)*/) {
					System.out.println("I would be returning to the cart here");
					// returnToCart(obs, player);
					returnToCartLocation(obs, player, returnToCartPosition[0], returnToCartPosition[1]);
					if (cart.canInteract(player)) {
						// interact with cart
						System.out.println("trying to interact with cart");
						interactWithObject();
						interactWithObject();
								
					}
				}
			} else if (playerIsHoldingCart(player)) {
				System.out.println("Player thinks it is holding a cart");
				crossOffItem();
			} else {
				returnToCartLocation(obs, player, returnToCartPosition[0], returnToCartPosition[1]);
				toggleShoppingCart();
			}
		} else if (relevantObj.getClass().equals(Observation.Shelf.class)) {
			boolean inAisle = amInAisle(obs, player);
			//goTo aisle hub. Go to y cord - width. Go to x cord. go north/south (whichever is the one supposed to)
			if (!playerHasItemQuantity(cart, goal)){
				// you don't have enough of the thing that you need
				if (!atHub)
					goToAisleHub(obs, player);
				approachShelf(obs, relevantObj, player);
				if (playerIsHoldingFood(player) /*&& !playerIsHoldingCart(player)*/) {
					System.out.println("I would be returning to the cart here");
					// returnToCart(obs, player);
					returnToCartLocation(obs, player, returnToCartPosition[0], returnToCartPosition[1]);
					if (cart.canInteract(player)) {
						// interact with cart
						System.out.println("trying to interact with cart");
						interactWithObject();
						interactWithObject();
								
					}
				}
			} else if (playerIsHoldingCart(player)) {
				System.out.println("Player thinks it is holding a cart");
				crossOffItem();
			} else {
				returnToCartLocation(obs, player, returnToCartPosition[0], returnToCartPosition[1]);
				toggleShoppingCart();
			}
		} 
		// System.out.println("here I am! Leaving the main loop!!!");

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
			double xChange = ply.position[0];
			while (xChange < 4.4){
				if (!detectCollison(obs, xChange, ply.position[1])) {
					System.out.println("Going East to avoid baskets");
					goEast();
					xChange += .3;
				}
				else
					System.out.println("I cannot go east to avoid the baskets. I am stuck");
			}
			//goEast();
			//goEast();
			//goEast();

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
					next = y + .3;
					willMoveCollide = detectCollison(obs, x, next);
					if (willMoveCollide){
						next = y - .3;
						willMoveCollide = detectCollison(obs, x, next);
						if(willMoveCollide){
							System.out.println("This is just broken at this point");
						}
						else {
							if (ply.direction != 0)
								goNorth();
							goNorth();
							System.out.println("Going north");
						}

					}
					else {
						if (ply.direction != 1)
							goSouth();
						goSouth();
						System.out.println("Going south");
					}
				}
				else{
					if (ply.direction != 2)
						goEast();
					goEast();
					System.out.println("Going east");
				}
			}
			else{
				if (ply.direction != 3)
					goWest();
				goWest();
				System.out.println("Going west");
			}
		}
		else{
			atHub = true;
		}
	}

	private boolean detectCollison(Observation obs, double x, double y){
		boolean tempVal = false;
		Observation.Player player = obs.players[0];

		if (x > 3.5 && x < 4.0 && y > 18.5 && y < 19.0){
			System.out.println("I think I will collide with the baskets.");
			return true;
		}

		for (Observation.Shelf obj: obs.shelves){
			tempVal = obj.collision(player, x, y);
			if(tempVal){
				System.out.println("colliding with shelf " + obj.food);
				return true;
			}
		}

		for (Observation.Counter obj: obs.counters){
			tempVal = obj.collision(player, x, y);
			if(tempVal){
				System.out.println("colliding with counter " + obj.food);
				return true;
			}
		}

		for (int i=0; i<obs.carts.length; i++) {
			Observation.InteractiveObject obj = obs.carts[i];
			tempVal = obj.collision(player, x, y);
			if (playerIsHoldingCart(player) && i == player.curr_cart) {
				tempVal = false;
			}
			if(tempVal){
				System.out.println("colliding with cart");
				return true;
			}
		}

		for (Observation.InteractiveObject obj: obs.registers){
			tempVal = obj.collision(player, x, y);
			if(tempVal){
				System.out.println("colliding with registers");
				return true;
			}
		}

		for (Observation.InteractiveObject obj: obs.cartReturns){
			tempVal = obj.collision(player, x, y);
			if(tempVal){
				System.out.println("colliding with cart return");
				return true;
			}
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

	private void returnToCartLocation(Observation obs, Observation.Player ply, double targetX, double targetY) {
		double xdiff = Math.abs(targetX - ply.position[0]);
		double ydiff = Math.abs(targetY - ply.position[1]);
		if (ydiff >= 0.1) {
			// go to the correct y location first
			if (targetY > ply.position[1]) {
				if (ply.direction != 1)
					goSouth();
				goSouth();
			} else {
				if (ply.direction != 0)
					goNorth();
				goNorth();
			}
		} else if (xdiff >= 0.1) { 
			// go to the correct x location
			if (targetX > ply.position[0]){
				if (ply.direction != 2)
					goEast();
				goEast();
			}
			else {
				if (ply.direction != 3)
					goWest();
				goWest();
			}	
		} else {
			System.out.println("w/in threshold of cart interaction");
			if (cart.direction != ply.direction) {  
				// turn to face the cart
				System.out.println("turning to face the cart");
				if (cartIsFacingEast(cart)) {
					goEast();
				}
				else if (cartIsFacingWest(cart)) {
					goWest();
				}
				if (cartIsFacingNorth(cart)) {
					goNorth();
				}
				if (cartIsFacingSouth(cart)) {
					goSouth();
				}
			}
			
		}
	}

	private void goToCounterLocation(Observation obs, Observation.Player ply, double targetX, double targetY) {
		double xdiff = Math.abs(targetX - ply.position[0]);
		double ydiff = Math.abs(targetY - ply.position[1]);
		if (xdiff >= 0.8) { 
			// go to the correct x location
			if (targetX > ply.position[0]){
				if (ply.direction != 2)
					goEast();
				goEast();
				System.out.println("I am going East");
			}
			else {
				if (ply.direction != 3)
					goWest();
				goWest();
				System.out.println("I am going West");
			}	
		} else if (ydiff >= 0.2) {
			// go to the correct y location first
			if (targetY > ply.position[1]) {
				if (ply.direction != 1)
					goSouth();
				goSouth();
				System.out.println("I am going South");
			} else {
				if (ply.direction != 0)
					goNorth();
				goNorth();
				System.out.println("I am going North");
			}
		} else {
			System.out.println("w/in threshold of cart interaction");
		}
	}
	
	private void approachCounter(Observation obs, Observation.InteractiveObject counter, Observation.Player ply){
		// Has it go south of the aisle
		double x_target = relevantObj.position[0];
		double y_target = relevantObj.position[1];
		if (playerIsHoldingCart(ply)) {
			// player is holding cart. need to navigate to the counter
			if (playerIsFacingNorth(ply)) {
				// park the cart on the top side of the counter 
				y_target = relevantObj.position[1];
				System.out.println("Facing north, y_target = " + y_target);

			} else if (playerIsFacingSouth(ply)) {
				// park the cart on the right side of the shelf 
				y_target = relevantObj.position[1] + relevantObj.height + 0.5;
				System.out.println("Facing south, y_target = " + y_target);
			} else {
				// park the car in the middle of the counter (should not happen...)
				y_target = relevantObj.position[1] + Math.ceil(relevantObj.height / 2) + 0.5;
				System.out.println("I shouldn't be here, but y_target = " + y_target);
			}
			x_target = relevantObj.position[0] - ply.width - 0.1;
			returnToCartPosition = ply.position.clone();

			goToCounterLocation(obs, ply, x_target, y_target);
		} else if (!playerIsHoldingFood(ply)) {
			// player is not holding cart. player needs to navigate to counter to pick up item
			x_target = relevantObj.position[0] - ply.width - 0.1;
			y_target = relevantObj.position[1] + (Math.ceil(relevantObj.height / 2)) + .1;
			goToCounterLocation(obs, ply, x_target, y_target);
		} /*else {
			// player is holding food. player needs to navigate back to cart to put item back
			System.out.println("I need to go to back to the cart");
			x_target = returnToCartPosition[0];
			y_target = returnToCartPosition[1];
			returnToCartLocation(obs, ply, x_target, y_target);
		} */

		double ydiff = Math.abs(y_target - ply.position[1]);
		double xdiff = Math.abs(x_target - ply.position[0]);
		System.out.println("Xdiff: " + xdiff + ", Ydiff: " + ydiff);
		// if you're next to the counter
		if (ydiff < 1.5 && xdiff < .9) {
			System.out.println("I am in this mysterious location");

			// if you're holding the cart, let go
			if (playerIsHoldingCart(ply)) {
				System.out.println("releasing cart");
				toggleShoppingCart();
				returnToCartPosition = ply.position;
				cart_index = ply.curr_cart;
			} else if (!playerIsHoldingCart(ply) && !playerIsHoldingFood(ply)) {
				System.out.println("not holding cart, going to counter");
				goToX(obs, ply, counter.position[0] - 0.1, relevantObj.position[1] + (Math.ceil(relevantObj.height / 2)) );
				goToY(obs, ply, relevantObj.position[1] + (Math.ceil(relevantObj.height / 2)));
				if (!counter.collision(ply, ply.position[0] + 0.3, ply.position[1])) {
					// System.out.println("I am not colliding wiwth the counter");
					// if you're still west of the counter, go east
					goEast();
				} else if (ply.holding_food == null || ply.holding_food.equals("")) {
					// System.out.println("here 4");
					// if you're at the counter and you're not holding an item yet
					if (ply.direction != 0) {
						// make sure you're facing the right direction
						goEast();
					}
					if (counter.canInteract(ply)) {
						// grab the item
						interactWithObject();
						interactWithObject();
						interactWithObject();
					}
				} 
			} /*else if (playerIsHoldingFood(ply)) {
				System.out.println("I'm holding " + ply.holding_food + " and I need to return to the cart");
				returnToCartLocation(obs, ply, x_target, y_target);

			}*/

		}
		 
	}

	private void approachRegister (Observation obs, Observation.InteractiveObject register, Observation.Player ply){
		double ydiff = Math.abs(register.position[1] - ply.position[1]);
		if (ydiff < 2.6) {
			if (playerIsHoldingCart(ply) && !checkoutOut) {
				System.out.println("Putting cart below register, this includes toggling it");
				goWest();
				goWest();
				goWest();
				goWest();
				goWest();
				goWest();
				goWest();
				goWest();
				goWest();
				goWest();
				goWest();
				goWest();
				toggleShoppingCart();
				returnToCartPosition = ply.position.clone();
				goEast();
				goEast();
				goEast();
				goEast();
				goEast();
				goEast();
				goEast();
				goEast();

			} else {
				//goToY(obs, ply, goalPosition[1]);
				if (ydiff < 1.7 && !register.collision(ply, ply.position[0] - .3, ply.position[1])) {
					System.out.println("I need to approach the register");
					//goWest();
					goToX(obs, ply, goalPosition[0], goalPosition[1]);
				}
				if (relevantObj.canInteract(ply) && !checkoutOut){
					System.out.println("Trying to checkout");
					interactWithObject();
					checkoutOut = true;
				}
				if (checkoutOut){
					System.out.println("Trying to leave");
					double x_target = returnToCartPosition[0];
					double y_target = returnToCartPosition[1];
					returnToCartLocation(obs, ply, x_target, y_target);
					toggleShoppingCart();
					String[] checkedOutItems = cart.purchased_contents;
					System.out.println("Purchased: " + Arrays.toString(checkedOutItems));
					String[] contents = cart.contents;
					System.out.println("Cart Contents: " + Arrays.toString(contents));
					/*
					toggleShoppingCart();
					goWest();
					goWest();
					goWest();
					goWest();
					goWest();
					goWest();
					goWest();
					goWest();*/
				}
			}
		}

	}

	private void approachShelf  (Observation obs, Observation.InteractiveObject shelf, Observation.Player ply) {
		// Has it go south of the aisle
		double x_target = relevantObj.position[0];
		double y_target = relevantObj.position[1];
		if (playerIsHoldingCart(ply)) {
			System.out.println("holding cart, need to navigate to correct shelf");
			// player is holding cart. need to navigate to the shelf
			if (playerIsFacingWest(ply)) {
				// park the cart on the left side of the shelf 
				x_target = relevantObj.position[0];

			} else if (playerIsFacingEast(ply)) {
				// park the cart on the right side of the shelf 
				x_target = relevantObj.position[0] + relevantObj.width;
			} else {
				// park the car in the middle of the shelf (should not happen...)
				x_target = relevantObj.position[0] + Math.ceil(relevantObj.width / 2);
			}
			y_target = relevantObj.position[1] + (Math.ceil(relevantObj.height / 2)) + .1;
			returnToCartPosition = ply.position.clone();

			goToY(obs, ply, y_target);
			goToX(obs, ply, x_target, y_target);
			// System.out.println("x_target = " + x_target);
		} else if (!playerIsHoldingFood(ply)) {
			// player is not holding cart. player needs to navigate to shelf to pick up item
			x_target = relevantObj.position[0] + Math.ceil(relevantObj.width / 2);
			y_target = relevantObj.position[1] + (Math.ceil(relevantObj.height / 2)) + .1;
			goToY(obs, ply, y_target);
			goToX(obs, ply, x_target, y_target);

		} /*else {
			// player is holding food. player needs to navigate back to cart to put item back
			System.out.println("I need to go to back to the cart");
			x_target = returnToCartPosition[0];
			y_target = returnToCartPosition[1];
			returnToCartLocation(obs, ply, x_target, y_target);
		}*/

		double ydiff = Math.abs(y_target - ply.position[1]);
		double xdiff = Math.abs(x_target - ply.position[0]);
		System.out.println("Xdiff: " + xdiff + ", Ydiff: " + ydiff);
		// if you're next to the shelf
		if (ydiff < 1.5 && xdiff < .5) {
			// if you're holding the cart, let go
			if (playerIsHoldingCart(ply)) {
				System.out.println("releasing cart");
				toggleShoppingCart();
				returnToCartPosition = ply.position;
				cart_index = ply.curr_cart;
				// cart = obs.carts[ply.curr_cart];
			} else if (!playerIsHoldingCart(ply) && !playerIsHoldingFood(ply)) {
				System.out.println("not holding cart, going to shelf");
				goToX(obs, ply, shelf.position[0] + (relevantObj.width / 2.0), relevantObj.position[1] + (Math.ceil(relevantObj.height / 2)) + .1);
				if (!shelf.collision(ply, ply.position[0], ply.position[1] - .3)) {
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
						interactWithObject();
					}
				}
			} /*else if (playerIsHoldingFood(ply)) {
				System.out.println("I'm holding " + ply.holding_food + " and I need to return to the cart");
				returnToCartLocation(obs, ply, x_target, y_target);

			}*/

		}
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

		if(obs.atCartReturn(ply.index) && !playerIsHoldingCart(ply)){
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

	private boolean playerIsFacingNorth(Observation.Player player) {
		return player.direction == 0;
	}

	private boolean playerIsFacingSouth(Observation.Player player) {
		return player.direction == 1;
	}

	private boolean playerIsFacingEast(Observation.Player player) {
		return player.direction == 2;
	}

	private boolean playerIsFacingWest(Observation.Player player) {
		return player.direction == 3;
	}

	private boolean cartIsFacingNorth(Observation.Cart cart) {
		return cart.direction == 0;
	}

	private boolean cartIsFacingSouth(Observation.Cart cart) {
		return cart.direction == 1;
	}

	private boolean cartIsFacingEast(Observation.Cart cart) {
		return cart.direction == 2;
	}

	private boolean cartIsFacingWest(Observation.Cart cart) {
		return cart.direction == 3;
	}

	private boolean playerHasItemQuantity(Observation.Cart cart, String item) {
		// is item in cart.contents? if not, then item_quantity = 1 (because 1 thing has been put in the cart)
		// in that case, return true, because this is only called after an item has been put in the cart
		int target_quantity = quantity_list.get(0);
		System.out.println("here I am! I need quantity = " + target_quantity + " of item = " + item );
		if (cart == null || cart.contents == null || cart.contents.length == 0) {
			System.out.println("nothing in cart");
			return false;
		}
		System.out.println("something in cart");
		int index_of_item = cart.contents.length-1;
		if (!cart.contents[index_of_item].equals(item)) {
			// you don't have any of the item
			return false;
		}
		// do you have enough of the item?
		return cart.contents_quant[index_of_item] == target_quantity;
		/**
		if (target_quantity == 1 && cart.contents.length > 0) {
			return true;
		} 
		else { 
			// you need more than 1 of the item
			if (cart.contents.length == 0) {
				System.out.println("don't have any of these");
				// you do not have more than 1 of the item.
				return false;
			} else {
				// you ave the right amount of the item
				int index_of_item = cart.contents.length-1;
				return cart.contents_quant[index_of_item] + 1 == target_quantity;
			}
		} */
	}
}
