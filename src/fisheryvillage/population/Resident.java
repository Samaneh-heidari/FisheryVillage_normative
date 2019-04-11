package fisheryvillage.population;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.batik.bridge.NoRepaintRunnable;
import org.apache.batik.dom.svg.LiveAttributeException;
import org.apache.velocity.runtime.directive.Foreach;

import com.sun.org.apache.bcel.internal.generic.RETURN;

import normFramework.Group;
import normFramework.Norm;
import normFramework.ParsNormLogic;
import fisheryvillage.FisheryVillageContextBuilder;
import fisheryvillage.batch.BatchRun;
import fisheryvillage.batch.RunningCondition;
import fisheryvillage.common.Constants;
import fisheryvillage.common.HumanUtils;
import fisheryvillage.common.Logger;
import fisheryvillage.common.SimUtils;
import fisheryvillage.property.Boat;
import fisheryvillage.property.House;
import fisheryvillage.property.HouseType;
import fisheryvillage.property.Property;
import fisheryvillage.property.municipality.Event;
import fisheryvillage.property.municipality.EventHall;
import fisheryvillage.property.municipality.Factory;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.random.RandomHelper;
import valueframework.AbstractValue;
import valueframework.DecisionMaker;
import valueframework.ValuedAction;
import valueframework.common.Log;

/**
 * This class extends the human class with decision making options
 * @author Maarten
 *
 */
public final class Resident extends Human {

	// Variable declaration (initialization in constructor)
	private DecisionMaker decisionMaker;

	// Variable initialization
	private int childrenWanted = -1;
	private SocialStatus socialStatus = new SocialStatus();
	private String jobActionName = "none";
	private ValuedAction eventAction = null;
	private boolean canOrganize = false;
	private int graphDonateType = -1; //-1: undefined, 0: donation not possible, 1: not donate, 2: donate to council
	private int graphEventType = -1; //-1: undefined, 0: no action possible, 1:OF, 2:OC, 3:AF, 4:AC

	private double avgGroupDonationAmount; 
	

	
	public Resident(int id, boolean gender, boolean foreigner, int age, double money) {
		super(id, gender, foreigner, age, money);

		decisionMaker = new DecisionMaker();
		//initDecisionMakerWaterTanks();
	}

	public Resident(int id, boolean gender, boolean foreigner, boolean hasBeenFisher, int age, double money, int childrenWanted,
					double nettoIncome, double necessaryCost, String jobTitle, Status status, int workplaceId, int notHappyTick) {
		
		super(id, gender, foreigner, hasBeenFisher, age, money, nettoIncome, necessaryCost, status, workplaceId, notHappyTick);

		this.childrenWanted = childrenWanted;
		this.jobActionName = jobTitle;
		socialStatus.setSocialStatusWork(status);
		if (status == Status.FISHER && workplaceId != -1) {
			
			Boat boat = (Boat) SimUtils.getPropertyById(workplaceId);
			boat.addFisher(id);
		}
		decisionMaker = new DecisionMaker();
	}

	/*=========================================
	 * Main human steps 
	 *=========================================
	 */
	public void stepSaveCurrentData() {
		
		if (!BatchRun.getEnable()) {
			double self_dir = decisionMaker.getAbstractValueThreshold(AbstractValue.SELFDIRECTION);
			double tradition = decisionMaker.getAbstractValueThreshold(AbstractValue.TRADITION);
			double calculated_tick = 4 + (24 * Math.min(1, Math.max(0, (50 + tradition - self_dir) * 0.01)));
			
			String datum = getHumanVarsAsString() + "," + calculated_tick + "," + getSocialStatusValue() + "," + getPartnerId() + "," + getSalaryTaxedData() + "," + getHasEnoughMoney() + "," + HumanUtils.getChildrenUnder18(this).size() + "," + HumanUtils.getLivingPlaceType(this).name();
			datum += "," + getThresholds();
			int tick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
			agentInfo.add(tick + "," + datum + "," + socialStatusString());
		}
	}
	
	public void stepAging() {
		addAge();
	}
	
	public void stepChildrenSchooling() {
		setPrimarySchool();
	}

	public void stepResetStandardCosts() {
		resetCostIndicators();
		equalizeMoneyWithPartner();
	}

	public void stepPayStandardCosts() {
		payStandardCosts();
	}
	
	public void stepDrainTanks() {

		if (getAge() >= Constants.HUMAN_ADULT_AGE && getAge() < Constants.HUMAN_ELDERLY_CARE_AGE) {
			
			setNotHappyTick(getIsHappy());
			decisionMaker.drainTanks();
		}
		else {
			setNotHappyTick(true);
		}
	}

	public void stepWork() {
		retrieveAndShareSalary();
		if (getAge() >= Constants.HUMAN_ADULT_AGE && getAge() < Constants.HUMAN_ELDERLY_AGE) {
			socialStatus.setSocialStatusWork(getStatus());
			if (getStatus() == Status.CAPTAIN || getStatus() == Status.FISHER) {
				socialStatus.setSocialStatusBoat(SimUtils.getBoatByHumanId(getId()).getBoatType());
			}
		}
	}

	public void stepSelectWork() {
		
		if (getAge() < Constants.HUMAN_ADULT_AGE || getAge() >= Constants.HUMAN_ELDERLY_AGE) {
			return ;
		}

		//double self_dir = decisionMaker.getAbstractValueThreshold(AbstractValue.SELFDIRECTION);
		ArrayList<String> possibleActions = new ArrayList<String>();
		if (jobActionName.equals("none") || (Constants.HUMAN_PROB_SEARCH_NEW_JOB <= RandomHelper.nextDouble() && (!getIsHappy() || jobActionName.equals("Job unemployed")) ) )  { //getNotHappyTick() > 0.1 * (100 - self_dir) 
			possibleActions = getPossibleWorkActions(jobActionName); 
		}
		else {
			//possibleActions.add(jobActionName);
			possibleActions = getExtraWorkActions(jobActionName);
		}
		Logger.logInfo("H" + getId() + " possible actions: " + possibleActions);
		String actionToDo = null;
		ValuedAction selectedAction = selectActionFromPossibleActionsJob(possibleActions);
		if (selectedAction != null) {
			actionToDo = selectedAction.getTitle();
			decisionMaker.agentExecutesValuedAction(selectedAction, Constants.TICKS_PER_MONTH);
		}

		Logger.logInfo("H" + getId() + " selected action: " + actionToDo);
		if (actionToDo != null) {
			ActionImplementation.executeActionJob(actionToDo, this);
		}
		else {
			Logger.logError("H" + getId() + "Error no action to execute");
		}
	}

	private ArrayList<String> getExtraWorkActions(String jobName) {
		
		ArrayList<String> possibleActions = new ArrayList<String>();
		possibleActions.add(jobName);
		Property workplace = HumanUtils.getWorkingPlace(getWorkplaceId(), getStatus(), getSchoolType());
		if (workplace != null) {
			if (workplace instanceof Boat) {
				Boat boat = (Boat) workplace;
				ArrayList<Status> jobs = boat.getVacancy(false, getMoney());
				if (jobs.contains(Status.CAPTAIN) && jobName != "Job captain") {
					possibleActions.add("Job captain");
					Logger.logAction("H" + getId() + ", B" + boat.getId() + " add captain as possible job to fisher since there is no captain");
				}
			}
			else if (workplace instanceof Factory) {
				Factory factory = (Factory) workplace;
				ArrayList<Status> jobs = factory.getVacancy(false, getMoney());
				if (jobs.contains(Status.FACTORY_BOSS) && jobName != "Job factory boss") {
					possibleActions.add("Job factory boss");
					Logger.logAction("H" + getId() + ", Factory add Boss as possible job for factory worker since there is no Boss");
				}
			}
		}
		return possibleActions;
	}
	
	public void stepRelation() {

		if (isSingle() && getAge() >= Constants.HUMAN_ADULT_AGE && getAge() < Constants.HUMAN_ELDERLY_CARE_AGE && RandomHelper.nextDouble() < Constants.HUMAN_PROB_GET_RELATION && getPartnerId() != -2) {
			for (final Resident resident: SimUtils.getObjectsAllExcluded(Resident.class, this)) {
				if (isSingle() && HumanUtils.isPotentialCouple(resident, this) && getPartnerId() != -2) {
					if (!getAncestorsMatch(getAncestors(), resident.getAncestors())) {
						actionSetPartner(resident);
					}
					break;
				}
			}
		}
	}

	public void stepSocialEvent() {
		
		graphEventType = -1;
		if (getAge() < Constants.HUMAN_ADULT_AGE || getAge() >= Constants.HUMAN_ELDERLY_CARE_AGE) {
			return ;
		}
		
		if ((getLeftoverMoney() <= 0 || getMoney() <= Constants.MONEY_DANGER_LEVEL) && getMoney() <= Constants.DONATE_MONEY_MINIMUM_SAVINGS_WITHOUT_INCOME) {
			graphEventType = 0;
			return ;
		}
		
		EventHall eventHall = SimUtils.getEventHall();
		ArrayList<Event> possibleEvents = eventHall.getEventsWithVacancy(getId());
		eventAction = null;
		canOrganize = false;
		
		//Create possible actions
		ArrayList<String> possibleActions = new ArrayList<String>();
		if (eventHall.getVacancyForNewEvent() && getMoney() > Constants.MONEY_DANGER_LEVEL) {
			possibleActions.add("Organize free event");
			possibleActions.add("Organize commercial event");
			canOrganize = true;
		}
		for (Event event : possibleEvents) {
			if (event.getEventType().equals("Free") && !possibleActions.contains("Attend free event")) {
				possibleActions.add("Attend free event");
			}
			else if (event.getEventType().equals("Commercial") && !possibleActions.contains("Attend commercial event") && getMoney() > Constants.MONEY_DANGER_LEVEL) {
				possibleActions.add("Attend commercial event");
			}
		}
		
		if (possibleActions.size() == 0) {
			Logger.logInfo("H" + getId() + " event no possible actions");
			return ;
		}
		else {
			Logger.logInfo("H" + getId() + " event possible actions: " + possibleActions);
		}
		
		ArrayList<ValuedAction> filteredActions = decisionMaker.agentFilterActionsBasedOnValues(possibleActions);
		ValuedAction selectedAction = socialStatus.getBestActionEvent(decisionMaker, filteredActions, canOrganize);
		String actionToDo = selectedAction.getTitle();

		if (actionToDo != null) {
			eventAction = selectedAction;
			ActionImplementation.executeActionEvent(actionToDo, this);
		}
		else {
			Logger.logError("H " + getId() + " Error no action to execute");
		}	
	}

	public void stepDonate() {
		if (getAge() < Constants.HUMAN_ADULT_AGE || getAge() >= Constants.HUMAN_ELDERLY_CARE_AGE
				|| BatchRun.getRunningCondition() == RunningCondition.NO_DONATION
				|| BatchRun.getRunningCondition() == RunningCondition.NO_EV_AND_DON) {
			Log.printDebug("H"+ getId() + " is out of donation age range");
			return ;
		}
		
		graphDonateType = -1;
		
		ArrayList<String> possibleActions = new ArrayList<String>();
		possibleActions.add("Donate nothing");
		if ((getLeftoverMoney() > 0 && getMoney() > Constants.MONEY_DANGER_LEVEL) || getMoney() > Constants.DONATE_MONEY_MINIMUM_SAVINGS_WITHOUT_INCOME) {
			possibleActions.add("Donate to council");
			Log.printDebug("H" + getId() + " added donate to council as a possible action");
		}
		else {
			Logger.logInfo("H" + getId() + " donation not possible, not enough money or income");
			graphDonateType = 0;
			return ;
		}
		Logger.logAction("H" + getId() + " possible actions: " + possibleActions);

		ArrayList<ValuedAction> filteredActions = decisionMaker.agentFilterActionsBasedOnValues(possibleActions);

		ValuedAction selectedAction = socialStatus.getBestActionDonate(decisionMaker, filteredActions);
		String actionToDo = selectedAction.getTitle();

		/*****************
		 * normative decision
		*****************/		
		int neighborId = getLivingGroupId();
//		double avgNeighborsDonationAmount = getAverageDonationAmountOfGroupMates(neighborId);
//		Logger.logDebug("H"+ getId() + " avgGroupDonationAmount : " + avgGroupDonationAmount);
		stepUpdateRepetitionNorms(avgGroupDonationAmount, neighborId);
		
		double donationAmount = calculateNormativeDonationAmount();
		if (actionToDo != null) {
			decisionMaker.agentExecutesValuedAction(selectedAction, 1);
			ActionImplementation.executeActionDonate(actionToDo, this, donationAmount);
			socialStatus.setSocialStatusDonation(actionToDo);
			this.setLastDonationAmount(donationAmount);
		}
		else {
			Logger.logError("H " + getId() + " Error no action to execute");
		}
	}

	public void stepFamily() {

		if (!isSingle() && !isMan() && childrenWanted > 0 && getAge() < Constants.HUMAN_MAX_CHILD_GET_AGE && HumanUtils.isLivingTogetherWithPartner(this)
						&& RandomHelper.nextDouble() <= Constants.HUMAN_PROB_GET_CHILD) {
			Human partner = getPartner();
			if (partner == null) {
				Logger.logError("Human.stepFamily(): partner = null");
			}
			actionGetChild(partner);
		}
	}
	
	
	public void initialHousing(){
		int tick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		Logger.logDebug("H" + getId() + " initializing the housing");
		ArrayList<House> availableProperties = SimUtils.getPropertyAvailableAllRandom(House.class);
		Vector<ArrayList<House>> availablePropertiesSorted = sortPropertiesByValue(availableProperties, true);
		
		if(tick < 1){
			housing(availablePropertiesSorted);
			initializeLastDonationAmount();
		}
	}
	
	private void initializeLastDonationAmount() {		
		Collection<Norm> normList = getNormListByGroupId(getLivingGroupId());
		if(normList != null){
			double donationAmount = getNormativeAmountFromRange(normList.iterator().next().getTitle());
			Logger.logDebug("H" + getId() + " initialized his lastDonationAmount by " + donationAmount);
			setLastDonationAmount(donationAmount);
		}
		
	}

	public void housing(Vector<ArrayList<House>> availablePropertiesSorted){
		if (getAge() < Constants.HUMAN_ADULT_AGE || getAge() >= Constants.HUMAN_ELDERLY_CARE_AGE) {
			socialStatus.setSocialStatusHouse(HumanUtils.getLivingPlaceType(this));
			return ;
		}
		
		for(ArrayList<House> houseArr : availablePropertiesSorted){
			Logger.logDebug("H" + getId() + " with "+ getMoney() + "$ wants to buy a house " + houseArr.toString());
		
			for (House house : houseArr) {
				if (getMoney() > house.getPrice()) {
					actionBuyHouse(house);
					return;
				}
			}
		}
			
		House ownedHouse = HumanUtils.getOwnedHouse(this);
		if (ownedHouse != null) {
			// Sell house if in relationship and not single and owns a house
			if (!isSingle() && !HumanUtils.isLivingTogetherWithPartner(this)) { 
				actionSellHouse(ownedHouse);
				return;
			}
			if (getLeftoverMoney() < 0 && getMoney() < Constants.MONEY_DANGER_LEVEL) {
				if (ownedHouse.getHouseType() != HouseType.CHEAP) {
					actionSellHouse(ownedHouse);
				}
			}
		}
		Logger.logDebug("H" + getId() + " check this human");
		socialStatus.setSocialStatusHouse(HumanUtils.getLivingPlaceType(this));
	}
	
	public void stepHousing() {
		Logger.logDebug("H" + getId() + " age : " + getAge() + " start of stepHousing livingPlace is " + HumanUtils.getLivingPlaceType(this));
		boolean condition = RandomHelper.nextDouble() > Constants.HUMAN_PROB_GET_HOUSE && !HumanUtils.isOwningHouse(this) && getStatus() != Status.UNEMPLOYED;
		ArrayList<House> availableProperties = SimUtils.getPropertyAvailableAllRandom(House.class);
		Vector<ArrayList<House>> availablePropertiesSorted = sortPropertiesByValue(availableProperties, false);
		if(condition)
			housing(availablePropertiesSorted);
		Logger.logDebug("H" + getId() + " end of stepHousing livingPlace is " + HumanUtils.getLivingPlaceType(this));
		
	}
	
	public String getMostImportantValue(){
		return decisionMaker.getMostImportantValue();
	}
	
	private Vector<ArrayList<House>> sortPropertiesByValue(
			ArrayList<House> availableProperties, boolean oneHouseType) {
		Vector<ArrayList<House>> sortedVector = null;
		String important = getMostImportantValue();
		if(important.equals(""))
			Logger.logError("decision maker: cannot find the most important value");
		String imprtValName = important.split(";")[0];
		Logger.logDebug("H" + getId() + " the most important value is : " + imprtValName);
		String[] neighborHoodPriority = null;
		boolean ifHighValueAvailable = true;
		if(imprtValName.equals(AbstractValue.TRADITION.name()))
			neighborHoodPriority = Constants.TRADITION_NEIGHBORHOOD_PRIORITY;
		else if(imprtValName.equals( AbstractValue.POWER.name()))
			neighborHoodPriority = Constants.POWER_NEIGHBORHOOD_PRIORITY;
		else if(imprtValName.equals( AbstractValue.SELFDIRECTION.name()))
			neighborHoodPriority = Constants.SELFDIRECTION_NEIGHBORHOOD_PRIORITY;
		else if(imprtValName.equals( AbstractValue.UNIVERSALISM.name()))
			neighborHoodPriority = Constants.UNIVERSALISM_NEIGHBORHOOD_PRIORITY;
		else
			ifHighValueAvailable = false;
		
		if(ifHighValueAvailable){
			if(!oneHouseType){
				sortedVector = sortPropertiesByValueName(availableProperties, neighborHoodPriority);
			}
			else{
				String[] oneNeighborhood = {neighborHoodPriority[0]};
				sortedVector = sortPropertiesByValueName(availableProperties, oneNeighborhood);			
			}
		}
		else{
			sortedVector = new Vector<ArrayList<House>>();
			ArrayList<House> temporal = new ArrayList<House>();
			for (int j = 0; j < availableProperties.size()/2; j++) {
				temporal.add(availableProperties.get(j));
			}
			sortedVector.add(temporal);
			ArrayList<House> temporal2 = new ArrayList<House>();
			for (int j = availableProperties.size()/2 +1; j < availableProperties.size(); j++) {
				temporal2.add(availableProperties.get(j));
			}
			sortedVector.add(temporal2);
		}				

		return sortedVector;		
	}

	private Vector<ArrayList<House>> sortPropertiesByValueName(
			ArrayList<House> availableProperties,
			String[] neighborhoodPriority) {
		Vector<ArrayList<House>> sortedVector = new Vector<ArrayList<House>>();
		
		for (int i = 0; i < neighborhoodPriority.length; i++) {
			ArrayList<House> temporal = new ArrayList<House>();
			for (int j = 0; j < availableProperties.size(); j++) {
				if(availableProperties.get(j).getHouseType().name().equals(neighborhoodPriority[i]))
					temporal.add(availableProperties.get(j));
			}
			sortedVector.add(temporal);
		}
		return sortedVector;
	}

	/**
	 * Removes the agent from the context and controls loosing of a partner
	 * etc.
	 */
	public void stepRemove() {
	
		if (doesHumanDie(getAge())) {
			SimUtils.getDataCollector().addDied();
			die();
			return ;
		}
		
		if (getAge() < Constants.HUMAN_ADULT_AGE || getAge() >= Constants.HUMAN_ELDERLY_CARE_AGE)
			return ;
		
		double self_dir = decisionMaker.getAbstractValueThreshold(AbstractValue.SELFDIRECTION);
		double tradition = decisionMaker.getAbstractValueThreshold(AbstractValue.TRADITION);
		double calculated_tick = 4 + (24 * Math.min(1, Math.max(0, (50 + tradition - self_dir) * 0.01)));
		Logger.logInfo("H" + getId() + ", self-dir:" + self_dir + ", tradition:" + tradition + ", not_happy_tick:" + getNotHappyTick() + ", calculated tick:" + calculated_tick);
		if (getNotHappyTick() >= calculated_tick && RandomHelper.nextDouble() <= Constants.MIGRATE_CHANCE)
		{
			Logger.logAction("H" + getId() + " moves out because he/she is not happy tick: " + getNotHappyTick() + ", self-dir:" + self_dir);
			Logger.logInfo("H" + getId() + getDcString());
			actionMigrateOutOfTown();
		}
	}
	
	public void stepLocation() {
		
		updateLocation();
	}

	/*=========================================
	 * Actions
	 *========================================
	 */
	
	private void actionBuyHouse(House hs) {
		String houseName = hs.getHouseType().name();
		Logger.logDebug("H" + getId() + " is calling actionBuyHouse : " + houseName);
		addMoney(-1 * hs.getPrice());
		connectProperty(hs.getId());
		becomeGroupMemberByGroupName(houseName, Constants.NORM_REPETITION_NEW_MEMBER);
		Logger.logAction("H" + getId() + " bought house:" + HumanUtils.getOwnedHouse(this));
	}
	
	private void actionSellHouse(House myHouse) {
		Logger.logDebug("H" + getId() + " is calling actionSellHouse : " + myHouse.getName());
		
		Logger.logAction("H" + getId() + " sells house");
		removeAndSellProperty(myHouse.getId(), true);
		leaveGroup(getGroupIdByName(myHouse.getName()));
	}

	public void actionSetPartner(Resident newPartner) {
		
		Logger.logAction("H" + getId() + " got a relation with H" + newPartner.getId());
		setPartner(newPartner);
		newPartner.setPartner(this);
		
		if (!isMan()) {
			if (childrenWanted == -1) {
				childrenWanted = calculateChildrenWanted();
			}
		}
		else if (!newPartner.isMan()){
			if (newPartner.getChildrenWanted() == -1) {
				newPartner.setChildrenWanted(calculateChildrenWanted());
			}
		}
	}
	
	private void actionMigrateOutOfTown() {
		Logger.logAction("H" + getId() + " " + getStatus() + " migrates out of town");
		SimUtils.getDataCollector().addMigratorOut(true, getId());
		migrateOutOfTown();
	}
	
	public void actionGetChild(Human partner) {
		
		HumanUtils.spawnChild(this, partner);
		Logger.logAction("H" + getId() + "and H" + partner.getId() + " got a child");
		childrenWanted--;
	}

	public void actionFish(String fishingActionTitle) {
		
		ArrayList<String> fishingActions = new ArrayList<String>();
		fishingActions.add(fishingActionTitle);
		ArrayList<ValuedAction> evaluatedAction = decisionMaker.agentFilterActionsBasedOnValues(fishingActions);
		decisionMaker.agentExecutesValuedAction(evaluatedAction.get(0), 1);
		socialStatus.setSocialStatusFisher(fishingActionTitle);
	}
	
	public void actionEventOrganize(int profit) {
		if (eventAction != null) {
			decisionMaker.agentExecutesValuedAction(eventAction, 1);
			socialStatus.setSocialStatusEvent(eventAction.getTitle(), canOrganize);
			addMoney(profit);
		}
		else {
			Logger.logError("H" + getId() + " has no event action");
		}
	}
	
	public void actionEventAttend(int fee) {
		if (eventAction != null) {
			decisionMaker.agentExecutesValuedAction(eventAction, 1);
			socialStatus.setSocialStatusEvent(eventAction.getTitle(), canOrganize);
			addMoney(-1 * fee);
		}
		else {
			Logger.logError("H" + getId() + " has no event action");
		}
	}
	
	/*=========================================
	 * Other methods 
	 *=========================================
	 */

	public boolean getIsHappy() {
		if (socialStatus.getSocialStatusValue(decisionMaker, getStatus()) > 0.25 && decisionMaker.getSatisfiedValuesCount() >= BatchRun.getValuesSatisfiedForHappy()) {
			return true;
		}
		return false;
	}
	
	/**
	 * Select fishing action based on the decisionMaker filter on actions
	 * from the filteredActions, the best actions are selected (so the actions with the highest
	 * same value). A random action from this best actions is choosen
	 * @return
	 */
	public String selectFishingAction() {
		
		ArrayList<String> possibleActions = getFishingActions();
		ArrayList<ValuedAction> filteredActions = decisionMaker.agentFilterActionsBasedOnValues(possibleActions);
		if (filteredActions.size() >= 1)
			return socialStatus.getBestActionFish(decisionMaker, filteredActions).getTitle();
		
		Logger.logError("H" + getId() + " no filtered actions");
		return "EMPTY";
	}

	public ArrayList<String> getFishingActions() {
		
		ArrayList<String> possibleActions = new ArrayList<String>();
		if (!SimUtils.getEcosystem().fishInDanger()) {
			possibleActions.add("Fish a lot");
			possibleActions.add("Fish medium");
			possibleActions.add("Fish less");
		}
		else {
			possibleActions.add("Fish a lot danger");
			possibleActions.add("Fish medium danger");
			possibleActions.add("Fish less danger");
		}
		return possibleActions;
	}

	private int calculateChildrenWanted() {

		//double y = 7 - (1.0/15.0) * (double) SimUtils.getCouncil().getNumberOfPeople();
		//return Math.max(Constants.HUMAN_CHILDREN_WANTED_MIN, Math.min(Constants.HUMAN_CHILDREN_WANTED_MAX, (int) Math.round(y)));
		if (RandomHelper.nextDouble() < Constants.GET_NO_CHILDREN) {
			return 0;
		}
		else {
			return RandomHelper.nextIntFromTo(1, Constants.HUMAN_CHILDREN_WANTED_MAX);
		}
	}
	
	private ValuedAction selectActionFromPossibleActionsJob(ArrayList<String> possibleActions) {
		
		ArrayList<ValuedAction> filteredActions = decisionMaker.agentFilterActionsBasedOnValues(possibleActions);
		//Remove unemployed if there are more options
		if (filteredActions.size() >= 2) {
			for (ValuedAction valuedAction : filteredActions) {
				if (valuedAction.getTitle().equals("Job unemployed")) {
					filteredActions.remove(valuedAction);
					break;
				}
			}
		}
		ValuedAction selectedAction = socialStatus.getBestActionWork(decisionMaker, filteredActions);
		
		// Keep the previous job if it is in the filteredActions
		if (!jobActionName.equals("Job unemployed") && RandomHelper.nextDouble() > Constants.HUMAN_PROB_KEEP_PREV_JOB) {
			for (ValuedAction valuedAction : filteredActions) {
				if (valuedAction.getTitle().equals(jobActionName)) {
					selectedAction = valuedAction;
					break;
				}
			}
		}
		
		Logger.logInfo("H" + getId() + " jobTitle: " + jobActionName + ", selected action:" + selectedAction + " from actions: " + filteredActions);
		return selectedAction;
	}
	
	public void setSocialStatusFromData(List<String> data) {
		socialStatus.setSocialStatusFromData(data);
	}

	public String socialStatusString() {
		return socialStatus.getSocialStatusString();
	}

	public void setImportantWaterTankFromData(List<String> data) {
		decisionMaker.setImportantWaterTankFromData(data);
	}
	
	public String importantWaterTankData() {
		return decisionMaker.importantData();
	}
	
	public boolean getHasEnoughMoney() {
		if ((getLeftoverMoney() > 0 && getMoney() > Constants.MONEY_DANGER_LEVEL) || getMoney() > Constants.DONATE_MONEY_MINIMUM_SAVINGS_WITHOUT_INCOME) {
			return true;
		}
		else {
			return false;
		}
	}
	
	/*=========================================
	 * Getters and setters
	 *=========================================
	 */
	public int getChildrenWanted() {
		return childrenWanted;
	}
	
	public double getSocialStatusValue() {
		return socialStatus.getSocialStatusValue(decisionMaker, getStatus());
	}
	
	public double getSocialStatusWork() {
		return socialStatus.getSocialStatusWork();
	}
	
	public double getSocialStatusHouse() {
		return socialStatus.getSocialStatusHouse();
	}
	
	public double getSocialStatusBoat() {
		return socialStatus.getSocialStatusBoat();
	}
	
	public double getSocialStatusFishEcol() {
		return socialStatus.getSocialStatusFishEcol();
	}
	
	public double getSocialStatusFishEcon() {
		return socialStatus.getSocialStatusFishEcon();
	}
	
	public double getSocialStatusEvents() {
		return socialStatus.getSocialStatusEvents();
	}
	
	public double getSocialStatusEventsFree() {
		return socialStatus.getSocialStatusOrganizeFree();
	}
	
	public double getSocialStatusDonation() {
		return socialStatus.getSocialStatusDonation();
	}
	
	public SocialStatus getSocialStatus() {
		return socialStatus;
	}
	
	public String getJobActionName() {
		return jobActionName;
	}
	
	public void setJobActionName(String jobActionName) {
		this.jobActionName = jobActionName;
	}
	
	public double getUniversalismImportanceDistribution() {
		return decisionMaker.getUniversalismImportanceDistribution();
	}
	
	public double getLevelUniversalism() {
		return decisionMaker.getWaterTankLevel(AbstractValue.UNIVERSALISM.name());
	}
	
	public double getLevelTradition() {
		return decisionMaker.getWaterTankLevel(AbstractValue.TRADITION.name());
	}
	
	public double getLevelSelfDirection() {
		return decisionMaker.getWaterTankLevel(AbstractValue.SELFDIRECTION.name());
	}
	
	public double getLevelPower() {
		return decisionMaker.getWaterTankLevel(AbstractValue.POWER.name());
	}
	
	public double getThreshold(AbstractValue abstractValue) {
		return decisionMaker.getWaterTankThreshold(abstractValue);
	}
	
	public int getSatisfiedValuesCount() {
		return decisionMaker.getSatisfiedValuesCount();
	}
	
	public void setChildrenWanted(int childrenWanted) {
		this.childrenWanted = childrenWanted;
	}
	
	/*=========================================
	 * Graph variables
	 * ========================================
	 */
	public void setGraphDonateType(int graphDonateType) {
		this.graphDonateType = graphDonateType;
	}
	
	public int getGraphDonateType() {
		return graphDonateType;
	}
	
	public void setGraphEventType(int graphEventType) {
		this.graphEventType = graphEventType;
	}
	
	public int getGraphEventType() {
		return graphEventType;
	}
	
	/*=========================================
	 * Print stuff
	 *=========================================
	 */
	public String getThresholds() {
		return decisionMaker.getThresholds();
	}
	
	public String getDcString() {
		return decisionMaker.toString();
	}
	
	public String getHumanVarsAsString() { 

		return getId() + "," + (!isMan()) + "," + getForeigner() + "," + getHasBeenFisher() + "," + getAge() + "," + getMoney() + "," + childrenWanted +
			   "," + getNettoIncome() + "," + getNecessaryCost() + "," + jobActionName + "," + getStatus().name() + "," + getWorkplaceId() + "," + getNotHappyTick(); 
	}
	
	/*
	 * Norm Related Methods
	 * 
	 */
	
	public double[] calculatePreferenceAccordingToPreviousGroups(int currentGroup) {
		//it is an array with 2 elements. the first one is probability and the second one is the amount
		double[] returnedArray = new double[2];// = {0.0,0.0};//[prob, normative amount]
		//calculate amount
		int maxRepeated = -1; int groupId = -1; int minNotRepeated = Integer.MAX_VALUE;
		Norm selectedNorm = null;
		for (int grpId : getNormList().keySet()) {
			if(!hasGroup(grpId) && grpId != currentGroup){
				for (Norm norm : getNormList().get(grpId)) {
					if(norm.getRepetition() > maxRepeated){
						maxRepeated = norm.getRepetition();
						minNotRepeated = norm.getNoRepetition();
						selectedNorm = norm;
						groupId = grpId;
					}else if(norm.getRepetition() == maxRepeated){
						if(norm.getNoRepetition() < minNotRepeated){
							minNotRepeated = norm.getNoRepetition();
							selectedNorm = norm;
							groupId = grpId;
						}
					}
				}
			}
		}		
		if(selectedNorm != null){
			returnedArray[1] = getNormativeAmountFromRange(selectedNorm.getTitle());
			Log.printDebug("H" + getId() + " is calling calculatePreferenceAccordingToPreviousGroups and selected amount from " + selectedNorm.getTitle() + " is : " + returnedArray[1] );
			double prob = calculateFollowingNeighborsProbability(groupId, selectedNorm.getTitle());
			returnedArray[0] = prob;
		}else{
			returnedArray[0] = 0.0;
			returnedArray[1] = 0.0;
		}		
		return returnedArray;		
	}
	
	private double getNormativeAmountFromRange(String normTitle) {
		double[] minMaxNormativeAmount = ParsNormLogic.getDonationAmount(normTitle);
		if(minMaxNormativeAmount[0] >= 0 && minMaxNormativeAmount[1] >=0)
			return (minMaxNormativeAmount[0] + minMaxNormativeAmount[1])/2.0;
		if(minMaxNormativeAmount[0] < 0 && minMaxNormativeAmount [1] < 0){
			Log.printError("H" + getId() + " is trying to getNormativeAmount of " + normTitle + ", which has upper and lowerbound less than 0");
			return -1;
		}
		return Math.max(minMaxNormativeAmount[0], minMaxNormativeAmount[1]);
	}

	private double calculateNormativeDonationAmount() {
		//get avg neighbors; get preference; calculate multiplication factors; weighted sum
		int neighborId = getLivingGroupId();
		setMyValueBasedDonation(calculateValueBasedDonationAmount());
		double[] prvGrpNormProbAmt = calculatePreferenceAccordingToPreviousGroups(neighborId);
		double[] otherGrpNormProbAmt = calculatePreferenceAccordingToOtherGroups(neighborId);
		//it is an array with 2 elements. the first one is probability and the second one is the norm
		boolean ifLivingIndependent = (neighborId == 0 | neighborId == 1 | neighborId == 2);
		//TODO:gourpIds needs to be constants
		double normativeDonationAmount = 0;
		
		if(ifLivingIndependent ){		
//			Logger.logDebug("H" + getId() + " is living independently in G" + neighborId );
			//calculating norm repetition
			String normativeAction = getRepetitionGroupNorm(neighborId, avgGroupDonationAmount);
			double groupNormativeAmount = getNormativeAmountFromRange(normativeAction);
			//calculating follow_percentage neighbors or personal preference
			double followNeighborsProbability = calculateFollowingNeighborsProbability(neighborId, normativeAction);
			double followPreferenceProb = 1.0-followNeighborsProbability;
			Logger.logDebug("H" + getId() + ", followNeighborsProb = " + followNeighborsProbability);
			double considerOtherGrProb = otherGrpNormProbAmt[0] * Constants.CONSIDERING_OTHER_GROUPS_PERCENTAGE;
			double considerPrvGrProb = prvGrpNormProbAmt[0] * Constants.CONSIDERING_PREVIOUS_GROUPS_PERCENTAGE;
			double considerValueBasedProb = followPreferenceProb - considerOtherGrProb-considerPrvGrProb;
			
			//calculating normative donation amount
			normativeDonationAmount = followNeighborsProbability * groupNormativeAmount +
									  considerValueBasedProb * getMyValueBasedDonation() +
									  considerOtherGrProb * otherGrpNormProbAmt[1] +
									  considerPrvGrProb * prvGrpNormProbAmt[1];
			Logger.logInfo("H" + getId() + " normativeDoantionAmount is : " + normativeDonationAmount);
		}
		else
			System.out.println("H" + getId() + " is living with others or homeless, G" + neighborId);
		return normativeDonationAmount;
	}
	

	private void updateRepetitionOfPrviousNorms(double normativeDonationAmount, int currentGroup) {
		for(int grId: getNormList().keySet()){
			if(grId == currentGroup || isMember(grId))
				continue;
			getRepetitionGroupNorm(grId, normativeDonationAmount);
		}
	}

	private double[] calculatePreferenceAccordingToOtherGroups(int currentGroup) {
		//it is an array with 2 elements. the first one is probability and the second one is the amount
		double[] returnedArray = new double[2];// = {0.0,0.0};//[prob, amount]
		//calculate amount
		int maxRepeated = -1; int groupId = -1; int minNotRepeated = Integer.MAX_VALUE;
		Norm selectedNorm = null;
		for (int grpId : getNormList().keySet()) {
			if(hasGroup(grpId) && grpId != currentGroup){
				for (Norm norm : getNormList().get(grpId)) {
					if(norm.getRepetition() > maxRepeated){
						maxRepeated = norm.getRepetition();
						minNotRepeated = norm.getNoRepetition();
						selectedNorm = norm;
						groupId = grpId;
					}else if(norm.getRepetition() == maxRepeated){
						if(norm.getNoRepetition() < minNotRepeated){
							minNotRepeated = norm.getNoRepetition();
							selectedNorm = norm;
							groupId = grpId;
						}
					}
				}
			}
		}		
		if(selectedNorm != null){
			returnedArray[1] = getNormativeAmountFromRange(selectedNorm.getTitle());
			Log.printDebug("H" + getId() + " is calling calculatePreferenceAccordingToOtherGroups and normativeAmount from " + selectedNorm.getTitle() + " is : " + returnedArray[1]);
			double prob = calculateFollowingNeighborsProbability(groupId, selectedNorm.getTitle());
			returnedArray[0] = prob;
		}else{
			returnedArray[0] = 0.0;
			returnedArray[1] = 0.0;
		}		
		return returnedArray;		
	}

	//returns a prob number in [0:1]
	private double calculateFollowingNeighborsProbability(int neighborId, String normativeAction) {
		Log.printLog("H" + getId() + " is trying to calculateFollowingNeighborsProbability for G" + neighborId + " and norm " + normativeAction);
		int repetition = getRepetition(neighborId, normativeAction);
		int noRepetition = getNoRepetition(neighborId, normativeAction);
		double followNeighborsProbability = 0;
		//this function can be changed based on modelers preference
		if(normativeAction.equals(null) || normativeAction.equals("")){
			Logger.logDebug("H"+ getId() + ", normativeAction is null" );			
			return followNeighborsProbability;
		}if(repetition < 0 ){//norm list is null
			Log.printError("H"+ getId() + " normList is null. repetition : " + repetition + " noRepetition : " + noRepetition);
			return followNeighborsProbability;
		}
		
		if(noRepetition == 0){
			if(repetition < Constants.T_ADOPTATION){
//				call observation function
				followNeighborsProbability = observationFunction(repetition);
				Logger.logDebug("H"+ getId() + " is in observation phase for normativeAction " + normativeAction);				
			} else if(repetition < Constants.T_INTERNALIZATION){
//				call internalization function
				Logger.logDebug("H"+ getId() + " is in adoptation phase for normativeAction " + normativeAction);
				followNeighborsProbability = adoptionFunction(repetition);
			}else{
//				call internalization function
				Logger.logDebug("H"+ getId() + " is in internalization phase for normativeAction " + normativeAction);
				followNeighborsProbability = internalizationFunction(repetition);
			}
		}else{
			//call disappearing function
			Logger.logDebug("H"+ getId() + " is in disappearing phase for normativeAction " + normativeAction);
			followNeighborsProbability = disappearingFunction(noRepetition);
		}
	
		if(repetition < 0 && noRepetition > 0)
			Log.printDebug("H" + getId() + "; norm " + normativeAction + " repetition: " + repetition + "; noRepetition : " + noRepetition);
		return followNeighborsProbability;
	}

	private double disappearingFunction(double noRepetition) {
		//it's part of sigmoid funtion
		//=1/(1+0,0078*POWER(0,5;25-noRepetition))
		double prob = 1.0/(1.0+0.0078*Math.pow(0.5,25-noRepetition));
//		Logger.logDebug("H"+ getId() + " is in disappearing phase. norepetition : " + noRepetition + ", Math.pow(0.5,25-noRepetition) = " + Math.pow(0.5,25-noRepetition) + " , prob : " + prob);
		return prob;
	}

	private double internalizationFunction(double repetition) {
//		1-1/repetition^0,5
		double prob = 1-1/Math.pow(repetition, 0.5);
		return prob;
	}

	private double adoptionFunction(double repetition) {
		//fun = e ^(x-h) +k; k and h should be find according by choosing two points.
		//here i find them according to (5, 0,005) , (10, 0,07)
		double h = 10.35708268;
		double k = -0.00028536;
		double prob = Math.exp(repetition-h) + k;
		return prob;
	}

	private double observationFunction(double repetition) {
		//linear function
		double prob = Constants.SLOP_OBSERVATION_PHASE * repetition;
		return prob;
	}


	private String getRepetitionGroupNorm(int neighborId,
			double donationAmount) {
		ArrayList<String> normativeActions = ifGroupDonationIsNormative(donationAmount, neighborId);
		ArrayList<String> notRepeatingNorms = getNotRepeatingNorms(neighborId, normativeActions);
		String returnedAction = "";		
		if (normativeActions != null && normativeActions.size() > 0)
			returnedAction = getMaxRepitiedNorm(normativeActions, neighborId);
		else if (notRepeatingNorms != null && notRepeatingNorms.size() > 0)
			returnedAction = getMinNotRepitiedNorm(notRepeatingNorms, neighborId);
		Logger.logDebug("H" + getId() + " updated norm repetition and returnedAction is : " + returnedAction);
		return returnedAction;
	}

	private void updateRepetitionGroupNorm(int neighborId,
			double donationAmount) {
		ArrayList<String> normativeActions = ifGroupDonationIsNormative(donationAmount, neighborId);
		ArrayList<String> notRepeatingNorms = getNotRepeatingNorms(neighborId, normativeActions);
		String returnedAction = "";
		Logger.logDebug("H"+ getId() + " : neighborId " + neighborId + " normativeAct of neighbors : " + normativeActions.size() + " notRepeated norms : " + notRepeatingNorms.size());
		Logger.logDebug("H"+ getId() + " getNormListByGroupId(neighborId) is " + (getNormListByGroupId(neighborId) ==null ? "null" : (getNormListByGroupId(neighborId).size())));
		if(normativeActions != null && normativeActions.size() > 0){
			for(String normTtl : normativeActions){
				if(getNormListByGroupId(neighborId) != null && getNormListByGroupId(neighborId).size() !=0){
					increaseNormRepetitionByTitle(neighborId, normTtl);
					Logger.logDebug("H" + getId() + " normativeAct not null; norm has member : returendAction : " + returnedAction);				
				}
				else
					Logger.logError("H" + getId() + " has no norm for group " + neighborId);
			}			
		}
		if(notRepeatingNorms != null && notRepeatingNorms.size() > 0){
			for(String normTtle : notRepeatingNorms){
				resetExistingNormRepetition(neighborId, normTtle);
				Logger.logDebug("H" + getId() + " has " + notRepeatingNorms.size() + " norms assgined to group G" + neighborId + " that that has not been repeated");
			}
		}
		
	}
	
	private void stepUpdateRepetitionNorms(double donationAmount, int neighborId){
		Logger.logDebug("H"+ getId() + " is updating repetition of norms.");
		updateRepetitionGroupNorm(neighborId, donationAmount);
		updateRepetitionOfPrviousNorms(donationAmount, neighborId);
		
	}
	private ArrayList<String> getNotRepeatingNorms(int neighborId,
			ArrayList<String> normativeActions) {
		ArrayList<String> returnedVal = new ArrayList<String>();
		if(getNormList().get(neighborId) != null)
		for(Norm norm: getNormList().get(neighborId)){
			if(normativeActions ==null || !normativeActions.contains(norm.getTitle()) )
				returnedVal.add(norm.getTitle());
		}
		return returnedVal;
	}

	private double calculateValueBasedDonationAmount() {
		double donationAmount = -1.0;
		if (this.getNettoIncome() >= this.getNecessaryCost()) {
			double amount = (this.getNettoIncome() - this.getNecessaryCost());
			donationAmount = amount;
			Logger.logAction("H" + this.getId() + " wants to donate " + amount + " money");
		}
		else if (this.getMoney() > Constants.DONATE_MONEY_MINIMUM_SAVINGS_WITHOUT_INCOME) {
			donationAmount = Constants.DONATE_MONEY_WITHOUT_INCOME;
			Logger.logAction("H" + this.getId() + " donated " + Constants.DONATE_MONEY_WITHOUT_INCOME + " money ");
		}
		else
		{
			donationAmount = 0.0;
			Logger.logError("H" + this.getId() + " netto income: " + this.getNettoIncome() + " not exceeding necessary cost: " + this.getNecessaryCost());
		}
		
		Log.printLog("H" + getId() + " preferred donation before value based calculation is : " + donationAmount);
		double valueBasedDonationAmount = decisionMaker.calculatePreferenceAccordingToValues(donationAmount);
		Log.printLog("H" + getId() + " value-based donation amount is : " + valueBasedDonationAmount);
		return valueBasedDonationAmount;
	}

	
/*	public void changeGroup(HouseType oldLivingPlace, HouseType newLivingPlace) {
		Logger.logDebug("H"+ getId() + " oldplace " + oldLivingPlace.name() + ", newplace " + newLivingPlace.name());
		if(oldLivingPlace.name().equals(newLivingPlace.name()))
			return;
		leaveGroup(oldLivingPlace);
		becomeGroupMemberByGroupName(newLivingPlace.name());
//		System.out.println("H"+ getId() + " changed his neighborhood from " + oldLivingPlace.name() + " to " + newLivingPlace.name());
	}*/


	
	public double getDonationDifferenceWithAvgNeighbors(){
//			return getAverageDonationAmountOfGroupMates(getLivingGroupId()) - getLastDonationAmount();
		return getAvgNeighborsDonationAmount() - getLastDonationAmount();
	}
	
	public double getAvgDonationNeighbors(){
//		return etAverageDonationAmountOfGroupMates(getLivingGroupId());
		return avgGroupDonationAmount;
	}

	public double getAvgNeighborsDonationAmount() {
		return avgGroupDonationAmount;
	}

	public void setAvgNeighborsDonationAmount(double avgNeighborsDonationAmount) {
		this.avgGroupDonationAmount = avgNeighborsDonationAmount;
	}
	
}