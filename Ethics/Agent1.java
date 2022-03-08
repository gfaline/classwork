import java.util.Arrays;
import com.supermarket.*;

public class Agent1 extends SupermarketComponentImpl {

    public Agent1() {
	super();
	shouldRunExecutionLoop = true;
	log.info("In Agent constructor.");
    }

    boolean firsttime = true;
	String state = "entry";
    
    @Override
    protected void executionLoop() {
	// this is called every 100ms
	// put your code in here
	Observation obs = getLastObservation();
	System.out.println(obs.players.length + " players");
	//Player player = obs.players[0];
	if (firsttime) {
		System.out.println(obs.carts.length + " carts");
		System.out.println(obs.shelves.length + " shelves");
		System.out.println(obs.counters.length + " counters");
		System.out.println(obs.registers.length + " registers");
		System.out.println(obs.cartReturns.length + " cartReturns");
		// print out the shopping list
		System.out.println("Shoppping list: " + Arrays.toString(obs.players[0].shopping_list));
		firsttime = false;
	}

		//System.out.println(obs.interactive_stage);
	System.out.println(obs.players[0].position[1]);

	double y_val = obs.players[0].position[1];

	if (obs.northOfCartReturn(0) && state == "entry") {
		goSouth();
	}
	if (obs.atCartReturn(0)){
		interactWithObject();
		state = "cart";
	}

	if (state == "cart"){
		goNorth();
	}

	if (state == "cart" && y_val < 15.15){
		state = "done";
	}

	//if (obs.besideCounters(0)){
	//	state = "done";
	//}
	//boolean can = defaultCanInteract(Cart, self, .5);
	//toggleShoppingCart();
    }
}
