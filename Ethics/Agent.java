/*"Make sure that you at the beginning of the file detail who was responsible for what parts of the code"
* Gwen Edgar: Original Navigation, Checkout, Navigation Debugging
* Michaela Freed: Getting Items From Shelf/Counter, Navigation Debugging, Most Cartwork
* */

import java.util.Arrays;
import com.supermarket.*;
import java.lang.Math;
import java.util.ArrayList;

public class Agent extends SupermarketComponentImpl {
	// WrongShelfNorm covered since we never put anything back
	//WallCollisionNorm covered by not going further right than the rear + counters or further right than the
	// asile hub or register other than leaving

    public Agent() {
	super();
	shouldRunExecutionLoop = true;
	log.info("In Agent constructor.");
    }

    boolean firsttime = true;
	String state = "entry";
	// Options are a shelf item, counter item, cart return, checkout

	String goal = "leek";
	//string[] testingShoppingList = ["cheese wheel", "steak", "fish"];
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
			System.out.println("Shopping list: " + Arrays.toString(player.shopping_list));
			System.out.println(player.shopping_list.getClass());
			firsttime = false;
			shopping_list = new ArrayList<String>(Arrays.asList(player.shopping_list));
			// shopping_list = new ArrayList<String>(Arrays.asList(new String[] {"milk","strawberry","oranges","strawberry milk","chicken","cheese wheel","leek","yellow bell pepper"}));
			for (int i=0; i < shopping_list.size(); i++) {
				if (shopping_list.get(i).equals("fish")) {
					shopping_list.set(i, "fresh fish"); 
				}
			}
			// TODO: Add a line checking if holding cart before doing this
			// shopping_list = new ArrayList<String>();
			// shopping_list.add(0, "brie cheese");
			// shopping_list.add(0, "avocado");
			// shopping_list.add(0, "chocolate milk");
			// shopping_list.add(0, "raspberry");
			// shopping_list.add(0, "garlic");
			shopping_list.add(0, "cartReturn");
			shopping_list.add(shopping_list.size(), "register");

			quantity_list = new ArrayList<Integer>(Arrays.stream(player.list_quant).boxed().toList());
			// quantity_list = new ArrayList<Integer>(Arrays.asList(new Integer[] {1,1,1,1,3,1,1,1}));
			// quantity_list = new ArrayList<Integer>();
			// quantity_list.add(0, 1);
			// quantity_list.add(0, 2);
			// quantity_list.add(0, 1);
			// quantity_list.add(0, 1);
			// quantity_list.add(0, 1);
			quantity_list.add(0, -1);
			quantity_list.add(quantity_list.size(), -1);


			String last_item = shopping_list.get(shopping_list.size()-2);
			if (last_item.contains("milk") || last_item.equals("fresh fish") || last_item.equals("prepared foods")) {
				shopping_list.add(shopping_list.size()-1,"swiss cheese");
				quantity_list.add(quantity_list.size()-1,1);
			}
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
			if (!playerHasItemQuantity(cart, goal)) {
				// you don't have enough of the thing that you need
				if (!atHub) {
					// go to the rear aisle hub
					goToRearAisleHub(obs, player);
				} else if (canApproachCounter(relevantObj, player)) {
					System.out.println("can approach counter");
					// you're w/in distance to be able to approach the counter
					if (playerIsHoldingCart(player)) {
						// let go of the cart + record where you left the cart
						System.out.println("releasing cart");
						toggleShoppingCart();
						returnToCartPosition = player.position;
						cart_index = player.curr_cart;
					} else if (playerIsHoldingFood(player)) {
						// return to the cart with the food
						System.out.println("returning to cart");
						goToCartLocation(obs, player, returnToCartPosition[0], returnToCartPosition[1]);
						if (cart.canInteract(player)) {
							// interact with cart
							System.out.println("trying to place item in cart");
							interact2x();
						}
					} else if (relevantObj.canInteract(player)) {
						// you can interact with the counter. grab the item
						interact3x();
 					} else {
						// you're not holding a cart or food and you're not close enough to interact with the counter
						// and you can approach the counter. 
						approachCounter(obs, relevantObj, player);
					}
				} else {
					// you're not close enough to the counter. go to the counter location
					goToCounterLocation(obs, relevantObj, player);
				}
			} else if (playerIsHoldingCart(player)) {
				// you have enough of the thing + you're holding the cart. 
				// so you're able to move onto the next thing. cross off an item to leave this counter
				crossOffItem();
			} else {
				// you have enough of the thing + you're not holding the cart
				// return to the cart + pick up the cart
				goToCartLocation(obs, player, returnToCartPosition[0], returnToCartPosition[1]);
				toggleShoppingCart();
			}
		} else if (relevantObj.getClass().equals(Observation.Shelf.class)) {
			boolean inAisle = amInAisle(obs, player);
			//goTo aisle hub. Go to y cord - width. Go to x cord. go north/south (whichever is the one supposed to)
			if (!playerHasItemQuantity(cart, goal)){
				// you don't have enough of the thing that you need
				if (!atHub && !obs.inAisle(0, getAisleIndex(relevantObj))) {
					goToAisleHub(obs, player);
				} else if (canApproachShelf(obs, relevantObj, player)) {
					System.out.println("Can approach shelf");
					if (playerIsHoldingCart(player)) {
						// let go of the cart + record where you left the cart
						System.out.println("releasing cart");
						toggleShoppingCart();
						returnToCartPosition = player.position;
						cart_index = player.curr_cart;
					} else if (playerIsHoldingFood(player) ) {
						System.out.println("I am returning to the cart here");
						goToCartLocation(obs, player, returnToCartPosition[0], returnToCartPosition[1]);
						if (cart.canInteract(player)) {
							// interact with cart
							System.out.println("trying to interact with cart");
							interact2x();
						}
					}else if (relevantObj.canInteract(player)) {
						// you can interact with the shelf. grab the item
						interact2x();
 					} else {
						// you're not holding a cart or food and you're not close enough to interact with the counter
						// and you can approach the shelf. 
						approachShelf(obs, relevantObj, player);
					}
	
				} else {
					// you're not close enough to the shelf. go to the shelf location
					goToShelfLocation(obs, relevantObj, player);
				}
			} else if (playerIsHoldingCart(player)) {
				System.out.println("Player thinks it is holding a cart");
				crossOffItem();
			} else {
				goToCartLocation(obs, player, returnToCartPosition[0], returnToCartPosition[1]);
				toggleShoppingCart();
			}
		} 
		// System.out.println("here I am! Leaving the main loop!!!");

	}

	private void goToAisleHub(Observation obs, Observation.Player ply){
		double x = ply.position[0];
		double y = ply.position[1];

		
		// if (!obs.inAisleHub(ply.index) && !acceptableYNearness(ply) && /* not in right aisle */){
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

		// Up until the next comment, these all cover the ObjectCollisionNorm

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

		// PersonalSpaceNorm, keeps player from entering unit of 1, if both players take step, it will be in range
		// So they need to be 1.15 away.
		// PlayerCollisionNorm If we follow the PersonalSpaceNorm, you cannot collide
		for (Observation.Player current_player: obs.players){
			// Yes, this is meant to check for the same object
			if (!(player == current_player)){
				double player_x = player.position[0];
				double player_y = player.position[1];
				double curr_x = current_player.position[0];
				double curr_y = current_player.position[1];
				double x_diff = Math.abs(player_x - curr_x);
				double y_diff = Math.abs(player_y - curr_y);

				if (x_diff <= 1.15 || y_diff <= 1.15)
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

	private boolean goToLocation(Observation obs, Observation.Player player, double targetX, double targetY,
							  double max_xdiff, double max_ydiff, boolean xFirst) {
		double xdiff = Math.abs(targetX - player.position[0]);
		double ydiff = Math.abs(targetY - player.position[1]);
		if (xFirst) {
			if (xdiff >= max_xdiff) {
				goToX(player, targetX );
			} else if (ydiff >= max_ydiff) {
				goToY(player, targetY);
			} else {
				return false;
			}
		} else {
			if (ydiff >= max_ydiff) {
				goToY(player, targetY);
			} else if (xdiff >= max_xdiff) {
				goToX(player, targetX );
			} else {
				return false;
			}
		}

		return true;

	}

	private void goToX(Observation.Player player, double targetX ) {
		if (isEastOf(player, targetX)) {
			if ( !isFacingWest(player) ) 
				goWest();
			goWest();
			System.out.println("I am going West");
		} else {
			if ( !isFacingEast(player) ) 
				goEast();
			goEast();
			System.out.println("I am going East");
		}
	}

	private void goToY(Observation.Player player, double targetY ) {
		if (isNorthOf(player, targetY)) {
			if ( !isFacingSouth(player) ) 
				goSouth();
			goSouth();
			System.out.println("I am going South");
		} else {
			if ( !isFacingNorth(player) ) 
				goNorth();
			goNorth();
			System.out.println("I am going North");
		}
	}

	private void goToCartLocation(Observation obs, Observation.Player ply, double targetX, double targetY) {
		if (!goToLocation(obs, ply, targetX, targetY, 0.1, 0.1, false)) {
			System.out.println("w/in threshold of cart interaction");
			turnToFace(cart, ply);			
		}
	}

	private void goToCounterLocation(Observation obs, Observation.InteractiveObject counter, Observation.Player ply) {
		// Has it go to the east aisle hub
		double x_target = relevantObj.position[0];
		double y_target = relevantObj.position[1];
		if (playerIsHoldingCart(ply)) {
			// player is holding cart. need to navigate to the counter
			if (isFacingNorth(ply)) {
				// park the cart on the top side of the counter 
				y_target = relevantObj.position[1];
				System.out.println("Facing north, y_target = " + y_target);

			} else if (isFacingSouth(ply)) {
				// park the cart on the right side of the shelf 
				y_target = relevantObj.position[1] + relevantObj.height + 1;
				System.out.println("Facing south, y_target = " + y_target);
			} else {
				// park the car in the middle of the counter (should not happen...)
				y_target = relevantObj.position[1] + Math.ceil(relevantObj.height / 2) + 0.5;
				System.out.println("I shouldn't be here, but y_target = " + y_target);
			}
			x_target = relevantObj.position[0] - ply.width - 0.1;
			// CartTheftNorm - we save where we left the cart and return to that cart.
			returnToCartPosition = ply.position.clone();

			goToLocation(obs, ply, x_target, y_target, 0.5, 1.5, true);
		} else if (!playerIsHoldingFood(ply)) {
			// player is not holding cart. player needs to navigate to counter to pick up item
			x_target = relevantObj.position[0] - ply.width - 0.1;
			y_target = relevantObj.position[1] + (Math.ceil(relevantObj.height / 2)) + .1;
			goToLocation(obs, ply, x_target, y_target, 0.5, 1.5, true);
		} 

		
	}
	
	private void goToShelfLocation(Observation obs, Observation.InteractiveObject shelf, Observation.Player player) {
		double x_target = shelf.position[0];
		double y_target = shelf.position[1];
		if (playerIsHoldingCart(player)) {
			System.out.println("holding cart, need to navigate to correct shelf");
			// player is holding cart. need to navigate to the shelf
			if (isFacingWest(player)) {
				// park the cart on the left side of the shelf 
				x_target = shelf.position[0] - .35;

			} else if (isFacingEast(player)) {
				// park the cart on the right side of the shelf 
				x_target = shelf.position[0] + shelf.width;
			} else {
				// park the cart in the middle of the shelf (should not happen...)
				x_target = shelf.position[0] + Math.ceil(shelf.width / 2);
			}
			y_target = shelf.position[1] + shelf.height + .3;
			returnToCartPosition = player.position.clone();

			goToLocation(obs, player, x_target, y_target, 0.5, 0.15, false);
		} 
	}

	private boolean canApproachCounter(Observation.InteractiveObject counter, Observation.Player player) {
		double player_x = player.position[0];
		double player_y = player.position[1];
		double player_height = player.height;
		double player_width = player.width;

		double target_x = counter.position[0];
		double target_y = counter.position[1];
		double target_height = counter.height;
		double target_width = counter.width;

		double east_aisle_hub_start_x = 15.5;

		// System.out.println("player coordinates: (" + player_x + ", " + player_y + ")");
		// System.out.println("player height = " + player_height + ", width = " + player_width);
		// System.out.println("target coordinates: (" + target_x + ", " + target_y + ")");
		// System.out.println("target height = " + target_height + ", width = " + target_width);


		// is player in x-bounds?
		boolean approachable =  player_x >= east_aisle_hub_start_x && // it's in the east aisle hub
								player_x-player_width <= target_x; // it's west of the shelf

		// is player in y-bounds?
		approachable =  approachable && 
						player_y-player_height >= target_y && // it's south of the top of the counter
						player_y+player_height <= target_y + target_height - 0.3; // it's north of the bottom of the counter

		return approachable;
	}

	private void approachCounter(Observation obs, Observation.InteractiveObject counter, Observation.Player ply){
		goEast();
		 
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
				// CartTheftNorm - we save where we left the cart and return to that cart.
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
					// ShopliftingNorm checkoutOut false until we buy things.
					interactWithObject();
					checkoutOut = true;
				}
				if (checkoutOut && Math.abs(register.position[1] + 1.0 - ply.position[1]) < .4){
					System.out.println("Trying to leave");
					double x_target = returnToCartPosition[0];
					double y_target = returnToCartPosition[1];
					goToCartLocation(obs, ply, x_target, y_target);
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

		goNorth();
	}

	private boolean canApproachShelf(Observation obs, Observation.InteractiveObject shelf, Observation.Player player) {
		double player_x = player.position[0];
		double player_y = player.position[1];
		double player_height = player.height;
		double player_width = player.width;

		double target_x = shelf.position[0];
		double target_y = shelf.position[1];
		double target_height = shelf.height;
		double target_width = shelf.width;


		// System.out.println("player coordinates: (" + player_x + ", " + player_y + ")");
		// System.out.println("player height = " + player_height + ", width = " + player_width);
		// System.out.println("target coordinates: (" + target_x + ", " + target_y + ")");
		// System.out.println("target height = " + target_height + ", width = " + target_width);


		// is player in x-bounds?
		boolean approachable =  player_x >= target_x && // it's east of the left side of the shelf
								player_x <= target_x + target_width; // it's west of the right side of the shelf

		// is player in y-bounds?
		approachable =  approachable && obs.inAisle(0, getAisleIndex(shelf)); // it's in the correct aisle

		return approachable;
	}

	private void approachCartReturn(Observation obs, Observation.InteractiveObject cartReturn, Observation.Player ply){
		double ydiff = Math.abs(cartReturn.position[1] - ply.position[1]);
		double xdiff = Math.abs(cartReturn.position[0] - ply.position[0]);
		if (obs.northOfCartReturn(ply.index)){
			if(!isFacingSouth(ply))
				goSouth();
			goSouth();
		}
		else if (obs.southOfCartReturn(ply.index)){
			if (!isFacingNorth(ply))
				goNorth();
			goNorth();
		}
		else if (xdiff > .35 && !cartReturn.collision(ply, ply.position[0] - .3, ply.position[1])){
			if (!isFacingWest(ply))
				goWest();
			goWest();
		}
		else if (!isFacingSouth(ply)){
			goSouth();
		}

		if(obs.atCartReturn(ply.index) && !playerIsHoldingCart(ply)){
			System.out.println("I am at the cart return");
			goSouth();
			// OneCartOnlyNorm we only take one cart and never return to this function as it is crossed off the list
			interactWithObject();
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

	private int getAisleIndex(Observation.InteractiveObject shelf) {
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

	private boolean isFacingNorth(Observation.Player player) {
		return player.direction == 0;
	}

	private boolean isFacingSouth(Observation.Player player) {
		return player.direction == 1;
	}

	private boolean isFacingEast(Observation.Player player) {
		return player.direction == 2;
	}

	private boolean isFacingWest(Observation.Player player) {
		return player.direction == 3;
	}

	private boolean isFacingNorth(Observation.Cart cart) {
		return cart.direction == 0;
	}

	private boolean isFacingSouth(Observation.Cart cart) {
		return cart.direction == 1;
	}

	private boolean isFacingEast(Observation.Cart cart) {
		return cart.direction == 2;
	}

	private boolean isFacingWest(Observation.Cart cart) {
		return cart.direction == 3;
	}

	private boolean isEastOf(Observation.Player player, double targetX ) {
		// is player East of targetY?
		return targetX < player.position[0];
	}

	private boolean isWestOf(Observation.Player player, double targetX ) {
		// is player West of targetY?
		return targetX > player.position[0];
	}

	private boolean isNorthOf(Observation.Player player, double targetY ) {
		// is player North of targetY?
		return targetY > player.position[1];
	}

	private boolean isSouthOf(Observation.Player player, double targetY ) {
		// is player South of targetY?
		return targetY < player.position[1];
	}

	private boolean isAtX(Observation.Player player, double targetX ) {
		return 0.15 > Math.abs(player.position[0] - targetX);
	}

	private boolean isAtY(Observation.Player player, double targetY ) {
		return 0.15 > Math.abs(player.position[1] - targetY);
	}

	private void turnToFace(Observation.Cart cart, Observation.Player ply) {
		if (cart.direction != ply.direction) {  
			// turn to face the cart
			System.out.println("turning to face the cart");
			if (isFacingEast(cart)) {
				goEast();
			}
			else if (isFacingWest(cart)) {
				goWest();
			}
			if (isFacingNorth(cart)) {
				goNorth();
			}
			if (isFacingSouth(cart)) {
				goSouth();
			}
		}
	}

	private boolean playerHasItemQuantity(Observation.Cart cart, String item) {
		int target_quantity = quantity_list.get(0);
		System.out.println("here I am! I need quantity = " + target_quantity + " of item = " + item );
		if (cart == null || cart.contents == null || cart.contents.length == 0) {
			// if the cart is empty, then you don't have enough of the thing
			// System.out.println("nothing in cart");
			return false;
		}
		// System.out.println("something in cart");
		int index_of_item = cart.contents.length-1;
		if (!cart.contents[index_of_item].equals(item)) {
			// the cart's not empty, but you don't have any of the item
			return target_quantity == 0;
		}
		// the cart's not emtpy. do you have enough of the item?
		return cart.contents_quant[index_of_item] >= target_quantity;
	}

	private void interact2x() {
		interactWithObject();
		interactWithObject();
	}
	private void interact3x() {
		interactWithObject();
		interactWithObject();
		interactWithObject();
	}
}
