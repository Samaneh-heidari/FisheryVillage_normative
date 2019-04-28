package fisheryvillage.common;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fisheryvillage.property.HouseType;

/**
* This class holds many constants used throughout the code
*
* @author Maarten Jensen
* @since 2018-02-20
*/
public final class Constants {

	// Initialize important IDs
	public static final String ID_CONTEXT = "fisheryvillage";
	public static final String ID_SPACE = "space";
	public static final String ID_GRID = "grid";
	public static final String ID_VALUE_LAYER = "village value layer";
	
	// Initialize world parameters
	public static final int GRID_WIDTH = 90;
	public static final int GRID_HEIGHT = 40;
	public static final int GRID_CELL_SIZE = 25;
	public static final int GRID_SEA_START = GRID_WIDTH - 12;
	public static final int GRID_VILLAGE_START = 18;
	public static final int INITIAL_POPULATION_SIZE = 50;
	public static final double NEW_RESIDENT_PROB = 0.05;
	public static final int TICKS_PER_MONTH = 4; // If you change this make sure you change the interval parameters of the repast charts
	public static final int TICKS_PER_YEAR = 12 * TICKS_PER_MONTH;
	
	// Initialize building parameters
	public static final int BUILDING_INITIAL_MONEY = 100000;
	public static final int PROPERTY_SELL_PERCENTAGE = 50;
	
	public static final int NUMBER_OF_HOUSES_CHEAP = 16;
	public static final int NUMBER_OF_HOUSES_STANDARD = 10;
	public static final int NUMBER_OF_HOUSES_EXPENSIVE = 5;
	public static final int HOUSE_CHEAP_PRICE = 25000;
	public static final int HOUSE_CHEAP_MAINTENANCE = 300;
	public static final int HOUSE_STANDARD_PRICE = 50000;
	public static final int HOUSE_STANDARD_MAINTENANCE = 600;
	public static final int HOUSE_EXPENSIVE_PRICE = 75000;
	public static final int HOUSE_EXPENSIVE_MAINTENANCE = 900;
	
	public static final int BUILDING_MONEY_DANGER_LEVEL = 30000;
	
	public static final double WORK_OUTSIDE_PROBABILITY = 0.4;
	
	public static final int EVENT_PERFORM_MIN_NUMBER_OF_ATTENDEES = 2;
	
	// Initialize employee numbers
	public static final int FACTORY_INITIAL_MAX_WORKERS = 10;
	public static final int FACTORY_MAX_WORKERS = 50;
	public static final int FACTORY_MIN_WORKERS = 3;
	public static final int CARETAKER_MAX_ELDERLY = 10;
	public static final int TEACHER_MAX_CHILDREN = 10;
	public static final int COMPANY_MAX_WORKERS = 100;
	
	public static final double PRICE_PER_KG_FISH_UNPROCESSED = 5;
	public static final double PRICE_PER_KG_FISH_PROCESSED = 10;
	public static final int FISH_PROCESSING_AMOUNT_PP = 750;
	public static final int FACTORY_BUY_PRICE = 10000;
	public static final double FISH_PROCESSING_NO_BOSS_DECREASE = 25;
	
	// Initialize boat parameters
	public static final int NUMBER_OF_BOATS = 2;
	public static final int FISH_CATCH_AMOUNT_MIN_PP = 75;
	public static final int FISH_CATCH_AMOUNT_MEDIUM_PP = 125;
	public static final int FISH_CATCH_AMOUNT_MAX_PP = 175;
	public static final double FISH_CAUGHT_NO_CAPTAIN_DECREASE = 25;
	
	public static final int BOAT_SMALL_PRICE = 10000;
	public static final int BOAT_SMALL_MAINTENANCE = 100;
	public static final int BOAT_SMALL_EMPLOYEES = 4;
	public static final int BOAT_MEDIUM_PRICE = 20000;
	public static final int BOAT_MEDIUM_MAINTENANCE = 200;
	public static final int BOAT_MEDIUM_EMPLOYEES = 6;
	public static final int BOAT_LARGE_PRICE = 30000;
	public static final int BOAT_LARGE_MAINTENANCE = 300;
	public static final int BOAT_LARGE_EMPLOYEES = 8;
	
	// Initialize population generate parameters
//	public static final int HUMAN_INIT_STARTING_MONEY = 50000;
	public static final int HUMAN_INIT_STARTING_MONEY = 100000;
	public static final int HUMAN_INIT_MIN_AGE = 18;
	public static final int HUMAN_INIT_MAX_AGE = 65;
	
	// Initialize population parameters
	public static final int HUMAN_RELATIVE_WIFE_MIN_AGE = -20;
	public static final int HUMAN_RELATIVE_WIFE_MAX_AGE = -10;
	public static final int HUMAN_ANCESTORS_LAYERS = 1; //0:none, 1:parents, 2:grandparents, 3:great-grandparents, 4:great-great-grandparents, 5:etc
	public static final int HUMAN_MAX_CHILD_GET_AGE = 45;
	public static final int HUMAN_ADULT_AGE = 18;
	public static final int HUMAN_ELDERLY_AGE = 65;
	public static final int HUMAN_ELDERLY_CARE_AGE = 85;
	public static final double HUMAN_PROB_GET_RELATION = 0.5;
	public static final double HUMAN_PROB_GET_HOUSE = 0.5;
	public static final double HUMAN_PROB_KEEP_PREV_JOB = 0.9;
	public static final double GET_NO_CHILDREN = 0.1;
	
	public static final double HUMAN_PROB_SEARCH_NEW_JOB = 0.033; //Roughly every 2.5 years

	public static final int HUMAN_CHILDREN_WANTED_MIN = 1;
	public static final int HUMAN_CHILDREN_WANTED_MAX = 6;
	public static final double HUMAN_PROB_GET_CHILD = 0.5;
	// Formula parameters given in Resident.calculateChildrenWanted()
	
	// Initialize ecosystem parameters
	public static final int ECOSYSTEM_STABLE_FISH = 200000;
	public static final int ECOSYSTEM_MAX_REPOPULATE_UPPER = 150000;
	public static final int ECOSYSTEM_MAX_REPOPULATE_LOWER = 50000;
	public static final int ECOSYSTEM_IN_DANGER = 75000;
	public static final int ECOSYSTEM_REPOPULATE_AMOUNT = 2000; 
	//Based on Constants.FISH_CATCH_AMOUNT_MEDIUM_PP (125) * Constants.NUMBER_OF_BOATS (2) * BoatType.LARGE.getEmployeeCapacity() (8);
	
	// Initialize money parameters
	public static final double LIVING_COST_ADULT = 300;
	public static final double LIVING_COST_CHILD = 150;
	public static final double LIVING_COST_ELDERLY = 150;
	public static final double COST_SCHOOL_INSIDE = 25;
	public static final double COST_SCHOOL_OUTSIDE = 100;
	public static final double TAX_PERCENTAGE = 50;
	public static final double PERC_FROM_TAX_TO_COUNCIL = 50;
	
	public static final double COUNCIL_MAYOR_IMPORTANCE = 0.1;
	
	// Initialize salary and benefits parameters
	public static final double SALARY_TEACHER = 3000;
	public static final double SALARY_FACTORY_WORKER = 2000;
	public static final double SALARY_FACTORY_BOSS = 6000; 
	public static final double SALARY_MAYOR = 4000;
	public static final double SALARY_OUTSIDE_WORK = 2500;
	public static final double SALARY_ELDERLY_CARETAKER = 2000;
	public static final double BENEFIT_UNEMPLOYED = 800;
	public static final double BENEFIT_ELDERLY = 800;
	public static final int SALARY_MULTIPLIER_CAPTAIN = 2;
	public static final double ELDERLY_CARE_COST = BENEFIT_ELDERLY - LIVING_COST_ELDERLY;
	
	// Initialize donation
	public static final double DONATE_FACTOR_OF_LEFT_OVER_MONEY = 0.1;
	public static final double MONEY_DANGER_LEVEL = 5000;
	public static final double DONATE_MONEY_MINIMUM_SAVINGS_WITHOUT_INCOME = 50000;
	public static final double DONATE_MONEY_WITHOUT_INCOME = 50;

	public static final double MIGRATE_CHANCE = 0.2;
	
	// Initialize icon paths
	public static final String ICON_CHILD = "./icons/child";
	public static final String ICON_TEACHER = "./icons/teacher";
	public static final String ICON_PROCESSOR = "./icons/processor";
	public static final String ICON_FACTORY_BOSS = "./icons/factory_boss";
	public static final String ICON_CARETAKER = "./icons/caretaker";
	public static final String ICON_WORKER_OUTSIDE = "./icons/business";
	public static final String ICON_FISHER = "./icons/fisher";
	public static final String ICON_CAPTAIN = "./icons/captain";
	public static final String ICON_MAYOR = "./icons/mayor";
	public static final String ICON_UNEMPLOYED = "./icons/unemployed";
	public static final String ICON_ELDER = "./icons/elder";
	public static final String ICON_ELDEST = "./icons/eldest";
	
	public static final String ICON_OWNED = "./icons/owned.png";
	public static final String ICON_NOT_OWNED = "./icons/not_owned.png";
	
	// Initialize graphics
	public static final Font FONT_SMALL = new Font("Tahoma", Font.PLAIN , 10);
	
	/*
	 * norm related
	 */
	
	/*
	 * default values for donation norm of each group:
	 * norms can be dynamically changed. These would be the defualt interpretation of donation norm of each group
	 */
	public static final boolean DOANTION_PERCENTAGE = false; //true means use exact amount as a norm instead of percentage when deciding about how much to donate
	
	public static final char[] VALID_OPERATORS = {'>','=','<'};
	public static final String CHEAP_DONATION_DEFAULT_NORM_TITLE = "300 < |donation| < 500";
	public static final String STANDARD_DONATION_DEFAULT_NORM_TITLE = "200 < |donation| < 300";
	public static final String EXPENSIVE_DONATION_DEFAULT_NORM_TITLE = "300 < |donation| < 500";
	
	public static final String CHEAP_DONATION_DEFAULT_NORM_PERCENTAGE_TITLE = "90 < |donation%| < 100";
	public static final String STANDARD_DONATION_DEFAULT_NORM_PERCENTAGE_TITLE = "50 < |donation%| < 60";
	public static final String EXPENSIVE_DONATION_DEFAULT_NORM_PERCENTAGE_TITLE = "10 < |donation%| < 20";
	
	
	public static final List NORM_TYPE_LIST = Collections.unmodifiableList(new ArrayList<String>() {{
	    add("ACTION");
	}});
	public static final int NORM_INIT_REPETITION = 50;
	public static final int NORM_REPETITION_NEW_MEMBER = 1;
	
	/*
	 * variables of function of following neighbors behavior percentage over time
	 * take a look at read me.docx for more info
	 */
	public static final int T_OBSERVATION = 0; //t_0
	public static final int T_ADOPTATION = 5; //t_1
	public static final int T_INTERNALIZATION = 10; //t_2
	public static final int T_DISAPPEARING = 30; //t_3
	
	public static final double FOLLOW_NORM_PROBABILITY_OSERVATION_PHASE = 0.0;//y_0
	public static final double FOLLOW_NORM_PROBABILITY_ADAPTATION_PHASE = 0.005; //y_1
	public static final double FOLLOW_NORM_PROBABILITY_INTERNALIZATION_PHASE = 0.7; //y_3
//	public static final double FOLLOW_NORM_PROBABILITY_MAX = 0.8;
	public static final double SLOP_OBSERVATION_PHASE = ((FOLLOW_NORM_PROBABILITY_ADAPTATION_PHASE - FOLLOW_NORM_PROBABILITY_OSERVATION_PHASE)/(T_ADOPTATION - T_OBSERVATION));
//	public static final double SLOP_ADAPTATION_PHASE = (FOLLOW_NORM_PROBABILITY_INTERNALIZATION_PHASE-FOLLOW_NORM_PROBABILITY_ADAPTATION_PHASE)/(T_INTERNALIZATION - T_OBSERVATION);
//	public static final double SLOP_INTERNALIZATION_PHASE = 0;
	
	/* 
	 * probability of considering previous groups norms in decisions
	 * and
	 * probability of considering other groups norms in decisions.
	 */
	public static final double CONSIDERING_PREVIOUS_GROUPS_PERCENTAGE = 0.30;
	public static final double CONSIDERING_OTHER_GROUPS_PERCENTAGE = 0.30;
	
	
	/*
	 * group ids
	 */
	public static final int CHEAP_GROUP_ID = 0;
	public static final int STANDARD_GROUP_ID = 1;
	public static final int EXPENSIVE_GROUP_ID = 2;
	public static final int HOMELESS_GROUP_ID = 3;
	public static final int WITH_OTHERS_GROUP_ID = 4;
	
	/*
	 * values and prioritized neighborhoods
	 */
//	public static final String[] POWER_NEIGHBORHOOD_PRIORITY = {HouseType.EXPENSIVE.name(), HouseType.STANDARD.name(), HouseType.CHEAP.name()};
	public static final String[] POWER_NEIGHBORHOOD_PRIORITY = {HouseType.CHEAP.name(), HouseType.STANDARD.name(), HouseType.EXPENSIVE.name()};
	public static final String[] SELFDIRECTION_NEIGHBORHOOD_PRIORITY = {HouseType.EXPENSIVE.name(), HouseType.STANDARD.name(), HouseType.CHEAP.name()};
//	public static final String[] SELFDIRECTION_NEIGHBORHOOD_PRIORITY = {HouseType.CHEAP.name(), HouseType.STANDARD.name(), HouseType.EXPENSIVE.name()};
	public static final String[] TRADITION_NEIGHBORHOOD_PRIORITY = {HouseType.CHEAP.name(), HouseType.STANDARD.name(), HouseType.EXPENSIVE.name()};
	public static final String[] UNIVERSALISM_NEIGHBORHOOD_PRIORITY = {HouseType.CHEAP.name(), HouseType.STANDARD.name(), HouseType.EXPENSIVE.name()};

	/*
	 * simulation setting related variables
	 */
	public static final double FISH_POPULATION_CHANGE_MULTIPLIER = 1.0;
	public static final int FISH_POPULATION_CHANGE_TICK = 200;
}