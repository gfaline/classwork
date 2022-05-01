/*"Make sure that you at the beginning of the file detail who was responsible for what parts of the code"
* Gwen Edgar: Original Navigation, Checkout, Navigation Debugging, collisions, multi-agent norms
* Michaela Freed: Getting Items From Shelf/Counter, Navigation Debugging, Most Cartwork, multi-agent norms
* Ray: ApproachRegister, Checkout, Leave, multi-agent norms
*/

import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.Math;
import java.util.Observable;
import java.util.random.*;
import java.util.stream.Collectors;

import com.supermarket.*;

public class Agent2 extends SupermarketComponentImpl {
	// WrongShelfNorm covered since we never put anything back
	// WallCollisionNorm covered by not going further right/east than the rear aisle hub + counters or further left/west than the
	// asile hub or register other than leaving

    public Agent2() {
		super();
		shouldRunExecutionLoop = true;
		log.info("In Agent constructor.");
    }

    boolean firsttime = true;

	String goal;
	String goalType;
	double[] goalPosition;
	ArrayList<String> shopping_list;
	ArrayList<Integer> quantity_list;
	Observation.InteractiveObject relevantObj;

	int player_index = 0;
	Observation.Player player;

	// CartTheftNorm - whenever we let go of the cart, we save where we left the cart and return to that cart.
	int cart_index = -1;
	double[] returnToCartPosition;
	Observation.Cart cart;
	
	String[] checkedOutItems;	// = cart.purchased_contents;
	String[] contents;			// = cart.contents;

	int collision_step_counter = -1;
	int max_collision_steps = 1;

	Observation obs;

    @Override
    protected void executionLoop() {

		obs = getLastObservation();

		if (firsttime) {
			firsttime(obs);
			firsttime = false;
		} 		
		everytime(obs);
	}

	private void firsttime(Observation obs) {
		player_index = this.playerIndex;
		player = obs.players[player_index];

		System.out.println(obs.carts.length + " carts");
		System.out.println(obs.shelves.length + " shelves");
		System.out.println(obs.counters.length + " counters");
		System.out.println(obs.registers.length + " registers");
		System.out.println(obs.cartReturns.length + " cartReturns");
		System.out.println("Shoppping list: " + Arrays.toString(player.shopping_list));

		// shopping list manipulation
		shopping_list = new ArrayList<String>(Arrays.asList(player.shopping_list));
		// shopping_list = new ArrayList<String>(Arrays.asList(new String[] {"apples"}));
		// shopping_list = new ArrayList<String>(Arrays.asList(new String[] {"fish", "raspberry","banana","strawberry","prepared foods"}));
		for (int i=0; i < shopping_list.size(); i++) {
			if (shopping_list.get(i).equals("fish")) {
				shopping_list.set(i, "fresh fish"); 
			}
		}
		quantity_list = new ArrayList<Integer>(Arrays.stream(player.list_quant).boxed().collect(Collectors.toList()));
		// quantity_list = new ArrayList<Integer>(Arrays.asList(new Integer[] {1}));
		// quantity_list = new ArrayList<Integer>(Arrays.asList(new Integer[] {1,1,1,1,1}));
		goal = shopping_list.get(0);
	}

	/**
	 * some things that need to be updated on every iteration
	 * @param obs
	 */
	private void everytime(Observation obs) {
		player = obs.players[player_index];
		goalType = getGoalType(obs);
		if (goalType.equals("cart return")) {
			cartReturnProcess(obs);
		} else {
			// don't have cart_index until after cart return is done
			cart = obs.carts[cart_index];
			// System.out.println("goaltype="+goalType);
			if (goalType.equals("shelf")) {
				shelfProcess(obs);
			} else if (goalType.equals("counter")) {
				// System.out.println("goal type is counter");
				counterProcess(obs);
			} else if (goalType.equals("register")) {
				checkoutProcess(obs);
			} 
		}
	}

	private void cartReturnProcess(Observation obs) {
		Observation.CartReturn cartReturn = getGoalCartReturn(obs);
		boolean hasCart = playerIsHoldingCart(player);
		boolean canInteract = canInteractWithACartReturn(obs, player);
		boolean canApproach = canApproachCartReturn(obs, player);
		if (hasCart) {  
			// you have a cart. record which cart is yours. the next step, you will start shopping.
			cart_index = player.curr_cart;
			cart = obs.carts[cart_index];
		} else if (canInteract) {
			// if you don't have a cart but you can interact with the cart return, get a cart
			interact1x();
		} else if (canApproach) {
			// if you don't have a cart + you can't interact with the cart return and you're able to approach the cart return
			approachCartReturn(obs, cartReturn, player);
		} else {
			// if you don't have a cart + you can't interact with the cart return + you can't approach the cart return, go closer to the cart return
			navigateToCartReturn(obs, player, cartReturn);
		}
	}

	private void shelfProcess(Observation obs) {
		Observation.Shelf shelf = getGoalShelf(obs);
		if (!playerHasItemQuantity(cart, goal)) { // get more of the item of food
			// System.out.println("does not have enough");
			if (canApproachShelf(obs, shelf, player)) {
				// System.out.println("can approach");
				if (playerIsHoldingCart(player)) {
					// you're holding a cart. let go + record where you left it
					releaseShoppingCart(obs, player);
				} else if (playerIsHoldingFood(player)) {
					// player is holding the cart
					// System.out.println("holding " + goal);
					goToCartLocation(obs, player);
					if (cart.canInteract(player)) {
						interact2x();
						System.out.println("placing item from "+goal+ " shelf in cart");
					}
				} else if (shelf.canInteract(player)) {
					// you can interact with the shelf. grab the item
					interact2x();
					System.out.println("interacting with shelf " + goal);
				} else {
					approachShelf(obs, shelf, player);
				}
			} else {
				// System.out.println("going to shelf location");
				goToShelfLocation(obs, shelf, player);
			}

		} else if (!playerIsHoldingCart(player)) {
			toggleShoppingCart();
		} else {
			System.out.println("Player is done with the "+goal+" shelf");
			crossOffItem();
		}
	}

	private void counterProcess(Observation obs) {
		Observation.Counter counter = getGoalCounter(obs);
		if (!playerHasItemQuantity(cart, goal)) { // get more of the item of food
			// System.out.println("I think i'm about to error");
			if (canApproachCounter(obs, counter, player)) {
				// System.out.println("Can approach counter");
				if (playerIsHoldingCart(player)) {
					// you're holding a cart. let go + record where you left it
					releaseShoppingCart(obs, player);
				} else if (playerIsHoldingFood(player)) {
					// player is holding the cart
					// System.out.println("holding " + goal);
					goToCartLocation(obs, player);
					if (cart.canInteract(player)) {
						interact2x();
						System.out.println("placing item from "+goal+" counter in cart");
					}
				} else if (counter.canInteract(player)) {
					// you can interact with the shelf. grab the item
					interact2x();
					System.out.println("interacting with counter " + goal);
				} else {
					approachCounter(obs, counter, player);
				}
			} else {
				goToCounterLocation(obs, counter, player);
			}

		} else if (!playerIsHoldingCart(player)) {
			toggleShoppingCart();
		} else {
			System.out.println("Player is done with the "+goal+" counter");
			crossOffItem();
		}
	}

	private void checkoutProcess(Observation obs) {
		Observation.Register register = getGoalRegister(obs);
		if (cart.contents.length != 0) {
			if (goToRegisterLocation(obs, register, player)) {//canApproachRegister(obs, register, player)){
				System.out.println("here");
				if (playerIsHoldingCart(player)) {
					releaseShoppingCart(obs, player);
				} else if (register.canInteract(player)) {
					interact3x();
					System.out.println("interacting with register + checking out");
				} else {
					approachRegister(obs, register, player);
				}
			} /*else {
				goToRegisterLocation(obs, register, player);
			}*/
		} else if (!playerIsHoldingCart(player)) {
			goToCartLocation(obs, player);
			if (cart.canInteract(player)) {
				toggleShoppingCart();
				System.out.println("Picking up cart after successfully checking out");
			}
		} else {
			goWest();
			//mygoWest(obs, player.position[0], player.position[1]);
			//does not check west safe, because no Observation player
		}
	}

	/*************************** CART RETURN STUFF ***************************/
	private boolean canApproachCartReturn(Observation obs, Observation.Player player) {
		boolean canApproach = true;
		double playerX = player.position[0];
		double playerY = player.position[1];

		canApproach = canApproach && playerY >= 13 && playerY <= 18.5;

		boolean canApproachX = false;
		for (Observation.CartReturn cr : obs.cartReturns) {
			canApproachX = canApproachX || (playerX >= cr.position[0] 
									  	 && playerX <= cr.position[0] + cr.width);
		}

		canApproach = canApproach && canApproachX;
		/** cart return is unobstructed */
		return canApproach;
	}

	private boolean canInteractWithACartReturn(Observation obs, Observation.Player player) {
		boolean canInteract = false;
		double playerX = player.position[0];
		double playerY = player.position[1];

		for (Observation.CartReturn cr : obs.cartReturns) {
			canInteract = canInteract || cr.canInteract(player);
		}

		/** cart return is unobstructed */
		return canInteract;
	}

	private void approachCartReturn(Observation obs, Observation.InteractiveObject cartReturn, Observation.Player ply){
		double ydiff = Math.abs(cartReturn.position[1] - ply.position[1]);
		double xdiff = Math.abs(cartReturn.position[0] - ply.position[0]);
		//goSouth();
		mygoSouth(obs, player.position[0], player.position[1]);
	}

	private void navigateToCartReturn(Observation obs, Observation.Player player, Observation.CartReturn cartReturn) {
		if (isInWestAisleHub(player)) {
			goToCartReturn(obs, player, cartReturn);
		} else if (isInAnyAisle(player)) {
			//goWest();
			mygoWest(obs, player.position[0], player.position[1]);
		} else if (isInEastAisleHub(player)) {
			goToAisle(obs, player, getClosestAisleIndex(player));
		} else {
			//goEast();
			mygoEast(obs, player.position[0], player.position[1]);
			/* you shouldn't be here... */
		}
	}

	private void goToCartReturn(Observation obs, Observation.Player player, Observation.CartReturn cartReturn) {
		double targetX = cartReturn.position[0];
		double targetY = cartReturn.position[1];
		goToLocation(obs, player, targetX, targetY, false);
	}

	/*************************** COUNTER STUFF ***************************/
	private boolean canApproachCounter(Observation obs, Observation.Counter counter, Observation.Player player) {
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
		//goEast();
		mygoEast(obs, player.position[0], player.position[1]);
		 
	}

	private void goToCounterLocation(Observation obs, Observation.Counter counter, Observation.Player ply) {
		double x_target = counter.position[0];
		double y_target = counter.position[1];

		if (isEastOfEastHub(player)) {
			// you're in the wrong aisle. go to the aisle hub
			goToAnyAisleHub(obs, player);
		} else if (isInWestAisleHub(player) || isWestOfWestHub(player)) {
			// System.out.println("going to aisle");
			goToAisle(obs, player, getClosestAisleIndex(player));
			// if (isInAnyAisle(player)) {
			// 	goEast();
			// }
		} else {
			// System.out.println("going to location");
			x_target = counter.position[0] - player.width - 0.1;
			y_target = counter.position[1] + Math.ceil(counter.height/4) ;
			goToLocation(obs, player, x_target, y_target, 0.75, 0.15, true);
		}
		
	}
	
	/*************************** SHELF STUFF ***************************/
	private boolean canApproachShelf(Observation obs, Observation.Shelf shelf, Observation.Player player) {
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
		approachable =  approachable && isInAisle(obs, player, shelf) ;

		return approachable;
	}

	private void approachShelf  (Observation obs, Observation.InteractiveObject shelf, Observation.Player player) {
		// Has it go south of the aisle
		//goNorth();
		mygoNorth(obs, player.position[0], player.position[1]);
	}

	private void goToShelfLocation(Observation obs, Observation.Shelf shelf, Observation.Player player) {
		double x_target = shelf.position[0];
		double y_target = shelf.position[1];

		// System.out.println("isInAnyAisle = " + isInAnyAisle(player) + "; isInAisle = " + isInAisle(obs, shelf));
		if (isEastOfEastHub(player) || isWestOfWestHub(player) || isInWrongAisle(obs, player, shelf)) {
			// you're in the wrong aisle. go to the aisle hub
			goToAnyAisleHub(obs, player);
		} else {
			x_target = shelf.position[0] + Math.ceil(shelf.width / 2) - 0.1;
			y_target = getAisleMidY(getAisleIndex(shelf)) + 0.1;
			goToLocation(obs, player, x_target, y_target, 0.5, 0.15, false);
		}

	}

	/*************************** REGISTER STUFF ***************************/
	private boolean canApproachRegister(Observation obs, Observation.Register register, Observation.Player player) {
		double player_x = player.position[0];
		double player_y = player.position[1];
		double player_height = player.height;
		double player_width = player.width;

		double target_x = register.position[0];
		double target_y = register.position[1];
		double target_height = register.height;
		double target_width = register.width;

		boolean approachable = isBetweenRegisters(obs, player);


		return false;

	}

	private void approachRegister (Observation obs, Observation.InteractiveObject register, Observation.Player player){
		//goNorth();
		mygoNorth(obs, player.position[0], player.position[1]);
	}

	private boolean goToRegisterLocation(Observation obs, Observation.Register register, Observation.Player player) {
		if (isInWestAisleHub(player) || isBetweenRegisters(obs, player) ) {
			double x_target = register.position[0]+ 0.8;
			double y_target = register.position[1]+ 2.7;
			// will return true when it doesn't move (aka when it's at the right position)
			return !goToLocation(obs, player, x_target, y_target, 0.1, 0.1, false);
		} else if (isEastOfEastHub(player) || isWestOfWestHub(player) ) {
			// System.out.println("going to closest aisle hub");
			goToAnyAisleHub(obs, player);
		} else if (isInEastAisleHub(player)) {
			System.out.println("going to aisle");
			goToAisle(obs, player, getClosestAisleIndex(player));
		} else if (isInAnyAisle(player)) {
			System.out.println("leaving aisle");
			//goWest();
			mygoWest(obs, player.position[0], player.position[1]);
		} else {
			// will return true when it doesn't move (aka when it's at the right position)
			return true;
		}
		// will return false when it moves (aka when it's not at the right position)
		return false;
	}

	/*************************** CART STUFF ***************************/
	private void goToCartLocation(Observation obs, Observation.Player player) {
		double targetX = returnToCartPosition[0];
		double targetY = returnToCartPosition[1];
		if (!goToLocation(obs, player, targetX, targetY, 0.15, 0.15, false)) {
			// System.out.println("w/in threshold of cart interaction");
			turnToFace(cart, player);			
		}
	}
	
	private boolean playerIsHoldingCart(Observation.Player player) {
		return player.curr_cart != -1;
	}

	private void releaseShoppingCart(Observation obs, Observation.Player player) {
		toggleShoppingCart();
		returnToCartPosition = player.position.clone();
		System.out.println("released shopping cart");
		// return player.position.clone();
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

	/*************************** GOAL STUFF ***************************/
	private String getGoalType(Observation obs) {
		if (cart_index == -1) {
			// if you haven't set cart_index, it's because you haven't gotten a cart yet
			// go to cart return
			return "cart return";
		} else if (shopping_list.isEmpty()) {
			// if the shopping list is empty, then it's time to check out
			return "register";
		} else {
			// otherwise, you're going to a counter or a shelf
			String item = shopping_list.get(0);
			if (item.equals("prepared foods") || item.equals("fresh fish")) {
				return "counter";
			} else {
				return "shelf";
			}
		}
	}

	private Observation.Shelf getGoalShelf(Observation obs) {
		for (Observation.Shelf shelf : obs.shelves) {
			if (shelf.food.equals(goal)) {
				return shelf;
			}
		}
		return null;
	}
	private ArrayList<Observation.Shelf> getGoalShelves(Observation obs) {
		ArrayList<Observation.Shelf> shelves = new ArrayList<Observation.Shelf>();
		for (Observation.Shelf shelf : obs.shelves) {
			if (shelf.food.equals(goal)) {
				shelves.add(shelf);
			}
		}
		return shelves;
	}

	private Observation.Counter getGoalCounter(Observation obs) {
		for (Observation.Counter counter : obs.counters){
			if (counter.food.equals(goal)) {
				return counter;
			}
		}
		return null;
	}
	private ArrayList<Observation.Counter> getGoalCounters(Observation obs) {
		ArrayList<Observation.Counter> counters = new ArrayList<Observation.Counter>();
		for (Observation.Counter counter : obs.counters){
			if (counter.food.equals(goal)) {
				counters.add(counter);
			}
		}
		return counters;
	}

	private Observation.CartReturn getGoalCartReturn(Observation obs) {
		return obs.cartReturns[0];
	}

	private Observation.Register getGoalRegister(Observation obs) {
		return obs.registers[0];
	}
	
	/*************************** MISCElLANEOUS STUFF ***************************/
	private void crossOffItem() {
		if (shopping_list.size() > 0) {
			// remove the item you just got
			shopping_list.remove(0);
			quantity_list.remove(0);
		}
		if (shopping_list.size() > 0) {
			// make the next item the target
			goal = shopping_list.get(0);
			System.out.println("here I am! I need quantity = " + quantity_list.get(0) + " of item = " + goal );
		}

	}
	
	private void interact1x() {
		interactWithObject();
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

	/*********** more generic methods for navigating to specific places ***********/
	/** 
	 * This function should be used when the player is in the east or west aisle hub, and they need to get to an aisle.
	 * @param obs
	 * @param player
	 * @param aisleIndex
	 */
	private void goToAisle(Observation obs, Observation.Player player, int aisleIndex) {
		double y_aisle = getAisleMidY(aisleIndex);
		goToLocation(obs, player, 10.5, y_aisle, false);
	}

	private void goToAnyAisleHub(Observation obs, Observation.Player player) {
		if (isEastOfEastHub(player)) {
			//goWest();
			mygoWest(obs, player.position[0], player.position[1]);
		} else if (isWestOfWestHub(player)) {
			//goEast();
			mygoEast(obs, player.position[0], player.position[1]);
		} else if (isInAnyAisle(player)) {
			// TODO: add logic to decide which hub to go to 
			//goEast();
			mygoEast(obs, player.position[0], player.position[1]);
		} 
	}

	/**************** Utility methods for generic navigation ****************/
	Random rand = new Random(); //instance of random class
    int upperbound = 8;
	int upperbound2 = 20;
	int int_random = rand.nextInt(upperbound); 
	int int_random2 = rand.nextInt(upperbound2); 
	private void mygoEast(Observation obs, double player_x, double player_y){
		if(!collisionDetected(obs, player_x + 1.5, player_y)){
			System.out.println("east safe");
			goEast();
		}
		else{
			System.out.println("east not safe");
			for(int i = 0;i <= int_random;i++){
				nop();
				System.out.println("I am waiting: " + i);
			}
			for(int j = 0;j <= int_random2;j++){
				//goWest();
				mygoWest(obs, player_x, player_y);
				
				System.out.println("try west" + j);
			}
			//mygoWest(obs, player_x, player_y);
		}
		

	}

	private void mygoWest(Observation obs, double player_x, double player_y){
		if(!collisionDetected(obs, player_x - 1.5, player_y)){
			System.out.println("west safe");
			goWest();
		}
		else{
			System.out.println("west not safe");
			for(int i = 0;i <= int_random;i++){
				nop();
				System.out.println("I am waiting: " + i);
			}
			for(int j = 0;j <= int_random2;j++){
				//goEast();
				mygoEast(obs, player_x, player_y);
				
				System.out.println("try east" + j);
			}
		}
		
		//mygoEast(obs, player_x, player_y);
		
	}

	private void mygoSouth(Observation obs, double player_x, double player_y){
		if(!collisionDetected(obs, player_x, player_y + 1.5)){
			System.out.println("south safe");
			goSouth();
		}
		else{
			System.out.println("south not safe");
			for(int i = 0;i <= int_random;i++){
				nop();
				System.out.println("I am waiting: " + i);
			}
			for(int j = 0;j <= int_random2;j++){
				//goNorth();
				mygoNorth(obs, player_x, player_y);
				System.out.println("try north" + j);
			}
			//mygoNorth(obs, player_x, player_y);
		}
		
		
	}

	private void mygoNorth(Observation obs, double player_x, double player_y){
		if(!collisionDetected(obs, player_x, player_y - 1.5)){
			System.out.println("north safe");
			goNorth();
		}
		else{
			System.out.println("north not safe");
			for(int i = 0;i <= int_random;i++){
				nop();
				System.out.println("I am waiting: " + i);
			}
			for(int j = 0;j <= int_random2;j++){
				//goSouth();
				mygoSouth(obs, player_x, player_y);
				
				System.out.println("try south" + j);
			}
			//mygoSouth(obs, player_x, player_y);
		}
		
	}
	
	private boolean goToLocation(Observation obs, Observation.Player player, double targetX, double targetY, boolean xFirst) {
		return goToLocation(obs, player, targetX, targetY, 0.3, 0.3, xFirst);
	}
	
	private boolean goToLocation(Observation obs, Observation.Player player, double targetX, double targetY,
								 double max_xdiff, double max_ydiff, boolean xFirst) {
		double xdiff = Math.abs(targetX - player.position[0]);
		double ydiff = Math.abs(targetY - player.position[1]);
		// System.out.println("Xdiff = " + xdiff + ", Ydiff = " + ydiff);
		if (xFirst) {
			if (xdiff >= max_xdiff) {
				goToX(obs, player, targetX );
			} else if (ydiff >= max_ydiff) {
				goToY(obs, player, targetY);
			} else {
				return false;
			}
		} else {
			if (ydiff >= max_ydiff) {
				goToY(obs, player, targetY);
			} else if (xdiff >= max_xdiff) {
				goToX(obs, player, targetX );
			} else {
				return false;
			}
		}

		return true;

	}

	private void goToX(Observation obs, Observation.Player player, double targetX ) {
		if (isEastOf(player, targetX)) {	
			//goWest();
			mygoWest(obs, player.position[0], player.position[1]); 
			// System.out.println("I am going West");
		} else /*isWestOf(player,targetX)*/{
			//goEast();
			mygoEast(obs, player.position[0], player.position[1]);
			// System.out.println("I am going East");
		}
	}

	private void goToY(Observation obs, Observation.Player player, double targetY ) {
		if (isNorthOf(player, targetY)) {
			//goSouth();
			mygoSouth(obs, player.position[0], player.position[1]);
			// System.out.println("I am going South");
		} else /*isSouthOf(player, targetY)*/{
			//goNorth();
			mygoNorth(obs, player.position[0], player.position[1]);
			// System.out.println("I am going North");
		}
	}

	/******* Utility methods for boolean checks + navigation conditions ******/
	private boolean playerIsHoldingFood(Observation.Player player) {
		String holding_food = player.holding_food;
		return !(holding_food == null || holding_food.equals(""));
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

	private boolean isInAnyAisle(Observation.Player player) {
		double x = player.position[0];
		double width = player.width;

		return x + width >= 5.5 && x - width <= 15.5;

	}

	private boolean isInAisle(Observation obs, Observation.Player player, int aisleIndex) {
		// isInAnyAisle does x-bounds. only need to check y-bounds
		double y = player.position[1];
		double height = player.height;
		return isInAnyAisle(player) && y <= getAisleBottomY(aisleIndex) && y >= getAisleTopY(aisleIndex);
	}

	private boolean isInAisle(Observation obs, Observation.Player player, Observation.Shelf shelf) {
		return isInAisle(obs, player, getAisleIndex(shelf));
	}

	private boolean isInWrongAisle(Observation obs, Observation.Player player, Observation.Shelf shelf) {
		return isInAnyAisle(player) && !isInAisle(obs, player, shelf);
	}

	private boolean isInEastAisleHub(Observation.Player player) {
		double x = player.position[0];
		double width = player.width;

		return x >= 15.5 && x-width <= 18.25;
	}

	private boolean isInWestAisleHub(Observation.Player player) {
		double x = player.position[0];
		double width = player.width;

		return x >= 4 && x+width <= 5.5;
	}

	private boolean isEastOfEastHub(Observation.Player player) {
		return player.position[0]-player.width > 18.25;
	}

	private boolean isWestOfWestHub(Observation.Player player) {
		return player.position[0] < 4;
	}

	private boolean isBetweenRegisters(Observation obs, Observation.Player player) {
		double y = player.position[1];
		double height = obs.registers[0].height;
		double region_top    = 4.5;
		double region_bottom = 9.5;

		return isWestOfWestHub(player) && y >= region_top && y <= region_bottom;
	}

	private boolean playerHasItemQuantity(Observation.Cart cart, String item) {
		int target_quantity = quantity_list.get(0);
		if (cart == null || cart.contents == null || cart.contents.length == 0) {
			// if the cart is empty, then you don't have enough of the thing
			return false;
		}
		int index_of_item = cart.contents.length-1;
		if (!cart.contents[index_of_item].equals(item)) {
			// the cart's not empty, but you don't have any of the item
			return target_quantity == 0;
		}
		// the cart's not empty and you have at least 1 of the item. do you have enough of the item?
		return cart.contents_quant[index_of_item] >= target_quantity;
	}

	private int getAisleIndex(Observation.InteractiveObject shelf) {
		double y_position = shelf.position[1];
		// gets the index of the aisle below a shelf
		return (int)((y_position - 1.5)/4 + 1);
	}

	private double getAisleTopY(int aisleIndex) {
		return 0.5 + 4*aisleIndex - 2;
	}

	private double getAisleBottomY(int aisleIndex) {
		return 0.5 + 4*aisleIndex - 1.5;
	}

	private double getAisleMidY(int aisleIndex) {
		return (getAisleTopY(aisleIndex) + getAisleBottomY(aisleIndex))/2;
	}

	private int getClosestAisleIndex(Observation.Player player) {
		double y = player.position[1];
		double height = player.height;

		int closest_aisle = 0;
		double closest_aisle_distance = Math.abs(getAisleMidY(0) - y);
		
		double aisle_i_dist;
		for (int i=1; i<6; i++) {
			aisle_i_dist = Math.abs(getAisleMidY(i) - y);
			if (aisle_i_dist < closest_aisle_distance) {
				closest_aisle = i;
				closest_aisle_distance = aisle_i_dist;
			}
		}

		return closest_aisle;
	}
	
	/******* Collisions, etc ******/
	private boolean collisionDetected(Observation obs, double delta_x, double delta_y) {
		Observation.Player player = obs.players[player_index];
		//double new_x = player.position[0] + delta_x;
		//double new_y = player.position[1] + delta_y;
		double new_x = delta_x;
		double new_y = delta_y;

		boolean collisionDetected = false;

		// if player is holding cart, then you actually want to check if the cart is going to collide,
		// because the cart will collide before the player collides
		if (playerIsHoldingCart(player)) {
			if (delta_x < 0) { // go west
				new_x = new_x - cart.width;
			} else if (delta_x > 0) { // go east
				new_x = new_x + cart.width;
			} else if (delta_y < 0) { // go north
				new_y = new_y - cart.height;
			} else /*if (delta_y > 0)*/ { // go south
				new_y = new_y + cart.height;
			}
		}

		// dynamic obstacles! these are harder!
		// carts
		for (int i=0; i< obs.carts.length; i++) {
			if (playerIsHoldingCart(player) && i != cart_index) { // make sure you don't think you're colliding with the cart you're hodling
				Observation.Cart c = obs.carts[i];
				if (c.collision(player, new_x, new_y)) {
					System.out.println("Colliding with cart index = " + i + ". My cart index = " + cart_index);
					collisionDetected = true;
				}
			}
		}

		//walls
		//if (delta_x <= 0 || delta_y <= 0) {
		//	System.out.println("Colliding with wall");
		//	collisionDetected = true;
		//}
		
		// players
		for (int i=0; i< obs.players.length; i++) {
			if (i != player_index) { // make sure you don't compare to your self
				Observation.Player p = obs.players[i];
				double player_x = player.position[0];
				double player_y = player.position[1];

				double x_diff = Math.abs(delta_x - p.position[0]);
				double y_diff = Math.abs(delta_y - p.position[1]);

				System.out.println("xPLY AT: " + player.position[0] +", "+player.position[1]);
				System.out.println("xPLY p AT: " + p.position[0] +", "+p.position[1]);
				System.out.println("delta AT: " + delta_x +", "+delta_y);
				System.out.println("x x_diff-y_diff: " + x_diff +", "+y_diff);

				if (playerIsHoldingCart(player) && x_diff <= 1.1 && x_diff > 0 && y_diff <= 1.1 && y_diff > 0) {
					System.out.println("Colliding with player index = " + i + ". My player index = " + player_index);
					System.out.println("PLY AT: " + player.position[0] +", "+player.position[1]);
					System.out.println("PLY p AT: " + p.position[0] +", "+p.position[1]);
					System.out.println("cart w h: " + cart.width +", "+ cart.height);
					collisionDetected = true;
				}
			}
		}

		// below are static obstacles. note, baskets, shelves, counters, aisles, + cart returns should all be handled implicitly by navigation
		// check baskets (which aren't included in observation)
		if (new_x > 3.5 && new_x < 4.0 && new_y > 18.5 && new_y < 19.0){
			System.out.println("I think I will collide with the baskets.");
			collisionDetected = true;
		}

		// check shelves
		for (Observation.Shelf obj: obs.shelves){
			if(obj.collision(player, player.position[0], player.position[1])){
				System.out.println("colliding with shelf " + obj.food);
				System.out.println("PLY AT: " + player.position[0] +", "+player.position[1]);
				collisionDetected = true;
			}
		}

		// check counters
		for (Observation.Counter obj: obs.counters){
			if(obj.collision(player, player.position[0], player.position[1])){
				System.out.println("colliding with counter " + obj.food);
				System.out.println("PLY AT: " + player.position[0] +", "+player.position[1]);
				collisionDetected = true;
			}
		}

		// check registers 
		for (Observation.InteractiveObject obj: obs.registers){
			if(obj.collision(player, player.position[0], player.position[1])){
				System.out.println("colliding with registers");
				System.out.println("PLY AT: " + player.position[0] +", "+player.position[1]);
				collisionDetected = true;
			}
		}

		// check cart returns 
		for (Observation.InteractiveObject obj: obs.cartReturns){
			if(obj.collision(player, player.position[0], player.position[1])){
				System.out.println("colliding with cart return");
				collisionDetected = true;
			}
		}

		if (collisionDetected) {
			collision_step_counter += 1;
		} else {
			collision_step_counter = 0;
		}

		return collisionDetected;

	}
}