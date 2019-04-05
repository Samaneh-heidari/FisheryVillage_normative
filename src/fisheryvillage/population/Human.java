package fisheryvillage.population;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.Setter;
import normFramework.Group;
import normFramework.Norm;
import normFramework.ParsNormLogic;
import fisheryvillage.batch.BatchRun;
import fisheryvillage.common.Constants;
import fisheryvillage.common.HumanUtils;
import fisheryvillage.common.Logger;
import fisheryvillage.common.SimUtils;
import fisheryvillage.property.Boat;
import fisheryvillage.property.House;
import fisheryvillage.property.HouseType;
import fisheryvillage.property.Property;
import fisheryvillage.property.Workplace;
import fisheryvillage.property.municipality.Council;
import fisheryvillage.property.municipality.School;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.random.RandomHelper;
import repast.simphony.relogo.ide.code.NetLogoRGWParser.prog_return;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import saf.v3d.scene.VSpatial;
import sun.util.logging.resources.logging;
import valueframework.common.Log;

/**
* The human class, without it the village would be a ghost town
*
* @author Maarten Jensen
* @since 2018-02-20
*/

public class Human {

	// Variable declaration (initialization in constructor)
	private final int id;
	private final boolean gender; // man = false; woman = true;
	private final boolean foreigner;
	private int age;
	private double money;
	private Status status;
	private boolean hasBeenFisher;
	
	// Variable initialization
	private ArrayList<Integer> childrenIds = new ArrayList<Integer>();
	private ArrayList<Integer> propertyIds = new ArrayList<Integer>();
	private ArrayList<GridPoint> ancestors = new ArrayList<GridPoint>();
	private HashMap<Status, VSpatial> spatialImages = new HashMap<Status, VSpatial>();
	private SchoolType schoolType = SchoolType.NO_SCHOOL;
	private double nettoIncome = 0;
	private double necessaryCost = 0;
	private double soloNetIncome = 0;
	private double salaryUntaxed = 0;
	private int partnerId = -1;
	private int workplaceId = -1;
	private int notHappyTick = 0;
	
	private double salaryTaxedData = 0;
	
	protected ArrayList<String> agentInfo = new ArrayList<>();
	
	/*
	 * Norm related
	 */
	private ArrayList<Group> groupList = new ArrayList<Group>();
	private Map<Integer, Collection<Norm>> normList = new HashMap<Integer, Collection<Norm>>();
//	private Multimap<Integer, Norm> normList = HashMultimap.create(); //key is the groupId that the norm belongs to it. 
	//Multimap allows to assign several values to one key. It works like Map<key, ArrayList<>>.
	private double lastDonationAmount;
	private double myValueBasedDonation;
	
	protected Human(int id, boolean gender, boolean foreigner, int age, double money) {

		this.id = id;
		this.gender = gender;
		this.foreigner = foreigner;
		this.age = age;
		this.money = money;
		this.schoolType = SchoolType.NO_SCHOOL;
		this.status = Status.UNEMPLOYED;
		this.hasBeenFisher = false;
		
		setStatusByAge();
		addToContext();
		
		agentInfo.add("Tick,id,gender,foreigner,hasBeenFisher,age,money,childrenWanted,nettoIncome,necessaryCost,jobTitle,status,workplaceId,notHappyTick,migrTickRequired,socialStatus" +
				 	  ",partnerId,salaryTaxed,hasEnoughMoney,childrenUnder18,house,P Thr.,P Lvl.,S Thr., S Lvl.,U Thr.,U Lvl.,T Thr.,T Lvl.,s_job,s_house,s_boat,s_ecol,s_econ,s_don,s_events,s_free_ev");

	}


	protected Human(int id, boolean gender, boolean foreigner, boolean hasBeenFisher, int age, double money,
				 double nettoIncome, double necessaryCost, Status status, int workplaceId, int notHappyTick) {
		
		this.id = id;
		this.gender = gender;
		this.foreigner = foreigner;
		this.hasBeenFisher = hasBeenFisher;
		this.age = age;
		this.money = money;
		this.schoolType = SchoolType.NO_SCHOOL;
		this.status = status;
		this.workplaceId = workplaceId;
		this.necessaryCost = necessaryCost;
		this.nettoIncome = nettoIncome;
		this.notHappyTick = notHappyTick;
		
		addToContext();
		
		agentInfo.add("Tick,id,gender,foreigner,hasBeenFisher,age,money,childrenWanted,nettoIncome,necessaryCost,jobTitle,status,workplaceId,notHappyTick,migrTickRequired,socialStatus" +
					  ",partnerId,salaryTaxed,hasEnoughMoney,childrenUnder18,house,P Thr.,P Lvl.,S Thr., S Lvl.,U Thr.,U Lvl.,T Thr.,T Lvl.,s_job,s_house,s_boat,s_ecol,s_econ,s_don,s_events,s_free_ev");
	}

	private void addToContext() {
		
		SimUtils.getContext().add(this);
		
		final NdPoint pt = SimUtils.getSpace().getLocation(this);
		if (!SimUtils.getGrid().moveTo(this, (int) pt.getX(), (int) pt.getY())) {
			Logger.logError("Human could not be placed, coordinate: " + pt.toString());
		}
	}
	
	/*=========================================
	 * Main functions
	 *=========================================
	 */
	protected void addAge() {
		age++;
		setStatusByAge();
	}

	protected void setPrimarySchool() {
		
		if (status == Status.CHILD) {
			School school = SimUtils.getSchool();
			if (school.getPupilVacancy())
				schoolType = SchoolType.INSIDE_VILLAGE;
			else 
				schoolType = SchoolType.OUTSIDE_VILLAGE;
		}
		else {
			schoolType = SchoolType.NO_SCHOOL;
		}
	}

	protected void equalizeMoneyWithPartner() {
		if (partnerId >= 0) {
			Human partner = getPartner();
			double dividedMoney = (money + partner.getMoney()) / 2;
			setMoney(dividedMoney);
			partner.setMoney(dividedMoney);
		}
	}
	
	protected void resetCostIndicators() {
		
		necessaryCost = 0;
		nettoIncome = 0;
		soloNetIncome = 0;
		salaryUntaxed = 0;
		salaryTaxedData = 0;
	}

	public double getSalaryTaxedData() {
		return salaryTaxedData;
	}
	
	/**
	 * Get salary, pay tax and share it with partner
	 */
	protected void retrieveAndShareSalary() {
		
		this.salaryUntaxed = calculateSalary();
		double salary = payTax(salaryUntaxed);
		salaryTaxedData = salary;
		soloNetIncome = salary;
		double benefits = calculateBenefits();
		double bankrupt_benefits = calculateBankruptBenefits();
		Human partner = getPartner();
		if (partner != null) {
			salary /= 2;
			benefits /= 2;
			partner.giveIncomeToPartner(salary + benefits);
		}
		nettoIncome += salary + benefits;
		money += salary + benefits + bankrupt_benefits;
	}

	public void payStandardCosts() {
		
		// For children
		if (status == Status.CHILD) {
			money -= Constants.LIVING_COST_CHILD;
			necessaryCost += Constants.LIVING_COST_CHILD;
			return;
		}
		
		// For adults
		double partnerMultiplier = 1;
		Human partner = getPartner();
		if (partner != null)
			partnerMultiplier = 0.5;
		
		if (status != Status.ELDEST && status != Status.ELDER) {
			money -= Constants.LIVING_COST_ADULT;
			necessaryCost += Constants.LIVING_COST_ADULT;
		}
		else {
			money -= Constants.LIVING_COST_ELDERLY;
			necessaryCost += Constants.LIVING_COST_ELDERLY;
		}
		payChildren(partnerMultiplier);
		payPropertyMaintenance(partnerMultiplier);
		if (status == Status.ELDEST) {
			SimUtils.getElderlyCare().payElderlyCareCost(Constants.ELDERLY_CARE_COST);
			money -= Constants.ELDERLY_CARE_COST;
			necessaryCost += Constants.ELDERLY_CARE_COST;
		}
	}
	
	/**
	 * Pay costs for children this contains living cost and school cost
	 * The partnerMultiplier is 1 if the agent has no partner and
	 * 0.5 if he/she has a partner
	 * @param partnerMultiplier
	 */
	private void payChildren(double partnerMultiplier) {
		
		// Pay children
		double childPayment = Constants.LIVING_COST_CHILD * partnerMultiplier;
		for (Human child : HumanUtils.getChildrenUnder18(this)) {
			Logger.logProb("H" + id + " payed child " + child.getId() + " : " + childPayment, 0.05);
			money -= childPayment;
			necessaryCost += childPayment;
			child.addMoney(childPayment);
			// Pay school
			if (child.getSchoolType() == SchoolType.INSIDE_VILLAGE) {
				money -= Constants.COST_SCHOOL_INSIDE * partnerMultiplier;
				SimUtils.getSchool().addSavings(Constants.COST_SCHOOL_INSIDE * partnerMultiplier);
				necessaryCost += Constants.COST_SCHOOL_INSIDE * partnerMultiplier;
			}
			else if (child.getSchoolType() == SchoolType.OUTSIDE_VILLAGE) {
				money -= Constants.COST_SCHOOL_OUTSIDE * partnerMultiplier;
				necessaryCost += Constants.COST_SCHOOL_OUTSIDE * partnerMultiplier;
			}
		}
	}
	
	/**
	 * Pay maintenance cost for property
	 * The partnerMultiplier is 1 if the agent has no partner and
	 * 0.5 if he/she has a partner
	 * @param partnerMultiplier
	 */
	private void payPropertyMaintenance(double partnerMultiplier) {
		
		// Pay property TODO let husband/wife who doesn't own house pay their partner
		for (Property property : HumanUtils.getOwnedProperty(this)) {
			double maintenanceCost = property.getMaintenanceCost();
			money -= maintenanceCost * partnerMultiplier;
			necessaryCost += maintenanceCost * partnerMultiplier;
			if (partnerId >= 0) {
				getPartner().getNecessaryMoneyFromPartner(maintenanceCost * partnerMultiplier);
			}
		}
	}
	
	/**
	 * Return all job vacancies, can't become a fisher if you are a captain already
	 * @return
	 */
	protected ArrayList<String> getPossibleWorkActions(String currentJobTitle) {
		
		ArrayList<String> possibleActions = new ArrayList<String>();
		possibleActions.add("Job unemployed");

		ArrayList<Workplace> workplaces = SimUtils.getObjectsAllRandom(Workplace.class);
		for (final Workplace workplace : workplaces) {
			ArrayList<Status> vacancies = workplace.getVacancy(false, getMoney()); //TODO: has been fisher getHasBeenFisher()
			for (Status vacancy : vacancies) {
				if (!possibleActions.contains(vacancy.getJobActionName())) {
					possibleActions.add(vacancy.getJobActionName());
				}
			}
		}
		
		if (!possibleActions.contains(currentJobTitle) && !currentJobTitle.equals("none") && !currentJobTitle.equals("Job unemployed")) {
			possibleActions.add(currentJobTitle);
		}
		if ((currentJobTitle.equals("Job captain") && possibleActions.contains("Job fisher"))) {
			possibleActions.remove("Job fisher");
		}
		if (possibleActions.contains(currentJobTitle) && soloNetIncome <= Constants.BENEFIT_UNEMPLOYED && !currentJobTitle.equals("none") && !currentJobTitle.equals("Job unemployed")) {
			possibleActions.remove(currentJobTitle);
		}
		return possibleActions;
	}
	
	/**
	 * TODO for now this function makes sure Humans are moved when they are on top of
	 * each other
	 */
	public void updateLocation() {
		
		final Grid<Object> grid = SimUtils.getGrid();
		GridPoint newLocation = grid.getLocation(this);

		double currentTick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		if (currentTick % 2 == 0 | currentTick < 0) {
			Property livingPlace = HumanUtils.getLivingPlace(this);
			if (livingPlace != null) {
				newLocation = livingPlace.getFreeLocationExcluded(this);
				if (newLocation == null)  {
					livingPlace.getLocation();
					Log.printLog("H" + id + " living place is full: " + livingPlace.getName());
				}
			}
			else {
				Logger.logError("H" + id + " has no living place");
			}
		}
		else {
			Property workingPlace = HumanUtils.getWorkingPlace(workplaceId, status, schoolType);
			if (workingPlace != null){
				if (workingPlace.getFreeLocationExcluded(this) != null) {
					newLocation = workingPlace.getFreeLocationExcluded(this);
				}
				else {
					Logger.logError("H" + id + " status " + status + " no room in working place to put agent");
				}
			}
		}
		grid.moveTo(this, newLocation.getX(), newLocation.getY());
	}
	
	/**
	 * Remove person because it died
	 */
	public void die() {
		Logger.logAction("H" + id + " died at age : " + age);
		removeSelf();
	}
	
	/**
	 * Removes the agent from the context, the house is sold or given to the partner (if he/she has
	 * no house) after that the money is given to the partner. If the person has no partner then it 
	 * is shared among the children. The person is also removed from the childrenIds in his/her parents
	 */
	public void removeSelf() {
		
		stopWorkingAtWorkplace();
		status = Status.DEAD;
		
		// Remove this person from parent id
		for (Human parent : HumanUtils.getParents(this)) {
			parent.removeChild(id);
		}
		
		Logger.logInfo("H" + id + " is removed");
		Human partner = getPartner();
		if (partner != null && HumanUtils.getOwnedHouse(this) != null) {
			if (HumanUtils.getOwnedHouse(partner) == null) {
				removeAllPropertyExceptHouse();
				int houseId = HumanUtils.getOwnedHouse(this).getId();
				removeProperty(houseId, true);
				partner.connectProperty(houseId);
				Logger.logInfo("H" + id + " gave house " + houseId + " to partner H" + partner.getId());
			}
		}
		
		removeAndSellAllProperty();
		shareMoney();
		
		breakUpWithPartner();
		Logger.logDebug("ContextUtils.remove" + getId());
		SimUtils.getContext().remove(this);
	}
	
	
	public void writeToFile(String filePathAndName, List<String> data) {
		PrintWriter writer;
		try {
			writer = new PrintWriter(filePathAndName, "UTF-8");
			for (String datum : data) {
				writer.println(datum);
			}
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getLastMigrData() {
		return agentInfo.get(agentInfo.size() - 1);
	}
	
	protected void migrateOutOfTown() {
	
		SimUtils.getGrid().moveTo(this, RandomHelper.nextIntFromTo(1, Constants.GRID_VILLAGE_START - 2), RandomHelper.nextIntFromTo(0, Constants.GRID_HEIGHT - 1));
		status = Status.DEAD;
		// Remove partner
		Human partner = getPartner();
		if (partner != null) {
			Logger.logInfo("and takes partner H" + partner.getId() + " with her/him");
			SimUtils.getDataCollector().addMigratorOut(false, partner.getId());
			SimUtils.getDataCollector().addMigratedPersonsExt(false, partner.getLastMigrData());
			if (!BatchRun.getEnable()) {
				partner.writeToFile("D:\\UniversiteitUtrecht\\7MasterThesis\\Repast-filesink\\fisheryvillage\\MigrWith" + partner.getId() + ".txt", partner.getAgentInfo());
			}
			partner.removeSelf();
		}
		// Also remove children (in removeSelf the child removes the childrenIds id in the parent.
		for (Human child : HumanUtils.getChildrenUnder18(this)) {
			Logger.logInfo("and also child H" + child.getId());
			SimUtils.getDataCollector().addMigratorOut(false, child.getId());
			SimUtils.getDataCollector().addMigratedPersonsExt(false, child.getLastMigrData());
			if (!BatchRun.getEnable()) {
				child.writeToFile("D:\\UniversiteitUtrecht\\7MasterThesis\\Repast-filesink\\fisheryvillage\\MigrChild" + child.getId() + ".txt", child.getAgentInfo());
			}
			child.removeSelf();
		}
		SimUtils.getDataCollector().addMigratedPersonsExt(true, getLastMigrData());
		if (!BatchRun.getEnable()) {
			writeToFile("D:\\UniversiteitUtrecht\\7MasterThesis\\Repast-filesink\\fisheryvillage\\MigrSelf" + id + ".txt", agentInfo);
		}
		removeSelf();
	}
	
	public void breakUpWithPartner() {
		if (partnerId >= 0) {
			getPartner().resetPartnerId();
			resetPartnerId();
		}
	}

	public ArrayList<String> getAgentInfo() {
		return agentInfo;
	}
	
	/**
	 * Remove all property but share the house with husband or wife
	 */
	protected void goToElderlyCare() {
		
		Human partner = getPartner();
		if (partner != null && HumanUtils.getOwnedHouse(this) != null) {
			if (HumanUtils.getOwnedHouse(partner) == null) {
				removeAllPropertyExceptHouse();
				int houseId = HumanUtils.getOwnedHouse(this).getId();
				removeProperty(houseId, true);
				partner.connectProperty(houseId);
				Logger.logInfo("H" + id + " gave house " + houseId + " to partner H" + partner.getId());
			}
		}
		removeAndSellAllProperty();
	}
	
	private void shareMoney() {
		
		Human partner = getPartner();
		if (partner != null) {
			partner.addMoney(money);
			money = 0;
		}
		else if (childrenIds.size() > 0) {
			double sharedMoney = money / childrenIds.size();
			for (int childId : childrenIds) {
				HumanUtils.getHumanById(childId).addMoney(sharedMoney);
			}
			money = 0;
		}
	}
	
	/*=========================================
	 * Getters and setters with logic 
	 *========================================
	 */
	public boolean isSingle() {
		
		if (partnerId >= 0) {
			return false;
		}
		return true;
	}
	
	/**
	 * Calculates the number of ancestors to take into account, dependent on the
	 * Constants.HUMAN_ANCESTORS_LAYERS 0:none, 1:parents, 2:grandparents, 3:great-grandparents, 4:great-great-grandparents, 5:etc
	 * @param motherId
	 * @param fatherId
	 * @param motherAncestors
	 * @param fatherAncestors
	 */
	@SuppressWarnings("unused")
	public void setAncestors(int motherId, int fatherId, final ArrayList<GridPoint> motherAncestors,
														 final ArrayList<GridPoint> fatherAncestors) {
		ancestors = new ArrayList<GridPoint>();
		if (Constants.HUMAN_ANCESTORS_LAYERS == 0)
			return ;
		
		ancestors.add(new GridPoint(1, motherId));
		ancestors.add(new GridPoint(1, fatherId));
		if (Constants.HUMAN_ANCESTORS_LAYERS == 1)
			return ;

		for (GridPoint ancestor : motherAncestors) {
			if (ancestor.getX() + 1 <= Constants.HUMAN_ANCESTORS_LAYERS) {
				ancestors.add(new GridPoint(ancestor.getX() + 1, ancestor.getY()));
			}
		}
		for (GridPoint ancestor : fatherAncestors) {
			if (ancestor.getX() + 1 <= Constants.HUMAN_ANCESTORS_LAYERS) {
				ancestors.add(new GridPoint(ancestor.getX() + 1, ancestor.getY()));
			}
		}
		Logger.logDebug("H" + id + " ancestors : " + ancestors.toString());
	}

	public boolean getAncestorsMatch(final ArrayList<GridPoint> ancestors1, final ArrayList<GridPoint> ancestors2) {
		
		Logger.logDebug("An1:"+ancestors1.toString() + ", An2:"+ancestors2.toString());
		ArrayList<Integer> ancestorsId = new ArrayList<Integer>();
		for (GridPoint ancestor : ancestors1) {
			ancestorsId.add(ancestor.getY());
		}
		for (GridPoint ancestor : ancestors2) {
			ancestorsId.add(ancestor.getY());
		}
		Set<Integer> ancestorsIdSet = new HashSet<Integer>(ancestorsId);
		if (ancestorsIdSet.size() != ancestorsId.size()) {
			Logger.logDebug("#Matching ancestors");
			return true;
		}
		Logger.logDebug("#No matching ancestors");
		return false;
	}
	
	protected double calculateSalary() {
		
		switch(status) {
		case FACTORY_WORKER:
			return SimUtils.getFactory().getFactoryWorkerPayment();
		case FACTORY_BOSS:
			return SimUtils.getFactory().getFactoryBossPayment();
		case TEACHER:
			return SimUtils.getSchool().getTeacherPayment();
		case WORK_OUT_OF_TOWN:
			return Constants.SALARY_OUTSIDE_WORK;
		case FISHER:
			if (SimUtils.getBoatByHumanId(id) != null) {
				return SimUtils.getBoatByHumanId(id).getFisherPayment(id);
			}
			Logger.logError("Human.calculateSalary: H" + id + " no boat for fisher");
			return 0;
		case CAPTAIN:
			if (SimUtils.getBoatByHumanId(id) != null) {
				return SimUtils.getBoatByHumanId(id).getCaptainPayment(id);
			}
			Logger.logError("Human.calculateSalary: H" + id + " no boat for captain");
			return 0;
		case MAYOR:
			return SimUtils.getCouncil().getMayorPayment();
		case ELDERLY_CARETAKER:
			return Math.round(SimUtils.getElderlyCare().getCaretakerPayment());
		default: // You get nothing
			return 0;
		}
	}
	
	protected double calculateBenefits() {
		switch(status) {
		case UNEMPLOYED:
			return Math.round(SimUtils.getSocialCare().getUnemployedBenefit());
		case ELDER:
			return Math.round(SimUtils.getElderlyCare().getPension());
		case ELDEST:
			return Math.round(SimUtils.getElderlyCare().getPension());
		default:
			return 0;
		}
	}
	
	protected double calculateBankruptBenefits() {
		
		if (money < 0) {
			if (SimUtils.getSocialCare().getSavings() > 0) {
				
				double amountOfMoney = Math.max(0, Math.min(Constants.BENEFIT_UNEMPLOYED, SimUtils.getSocialCare().getSavings()));
				SimUtils.getSocialCare().addSavings(-1 * amountOfMoney);
				return amountOfMoney;
			}
		}
		return 0;
	}

	//TODO in this function change the parameter to something that is defined before hand
	protected double payTax(double salary) {
		double payedAsTax = salary * ((Constants.TAX_PERCENTAGE) / 100);
		salary -= payedAsTax;
		if (status != Status.WORK_OUT_OF_TOWN) {
			
			Council council = SimUtils.getObjectsAll(Council.class).get(0);
			council.addSavings(payedAsTax * (Constants.PERC_FROM_TAX_TO_COUNCIL / 100));
			
			return salary;
		}
		return salary;
	}

	public void connectProperty(int propertyId) {
		
		if (!propertyIds.contains(propertyId)) {
			SimUtils.getPropertyById(propertyId).setOwner(id);
			propertyIds.add(propertyId);
			Logger.logDebug("H" + getId() + " connects to property"+ propertyId);
			return;
		}
		Logger.logError("Property " + propertyId + " already contained in H" + id);
	}
	
	public void stopWorkingAtWorkplace() {
		
		Logger.logInfo("H" + id + ", status: " + status + " stops working at: " + workplaceId);
		if (workplaceId >= 0) {
			Workplace workplace = (Workplace) SimUtils.getPropertyById(workplaceId);
			if (propertyIds.contains(workplaceId)) {
				removeAndSellProperty(workplaceId, true);
			}
			else if (workplace instanceof Boat) {
				((Boat) workplace).removeFisher(getId());
			}
			status = Status.UNEMPLOYED;
			workplaceId = -1;
		}
	}
	
	/**
	 * Remove and sells property
	 * @param propertyId
	 */
	public void removeAndSellProperty(int propertyId, boolean removeFromArray) {
		
		Logger.logDebug("H" + id + " remove property: " + propertyId);
		if (removeFromArray) 
			propertyIds.remove(propertyIds.indexOf(propertyId));
		Property property = SimUtils.getPropertyById(propertyId);
		money += property.getPrice() * ((double) Constants.PROPERTY_SELL_PERCENTAGE / 100);
		property.removeOwner(id);
	}
	
	/**
	 * Remove without selling
	 * @param propertyId
	 */
	public void removeProperty(int propertyId, boolean removeFromArray) {
		
		Logger.logDebug("H" + id + " remove property: " + propertyId);
		if (removeFromArray)
			propertyIds.remove(propertyIds.indexOf(propertyId));
		Property property = SimUtils.getPropertyById(propertyId);
		property.removeOwner(id);
	}
	
	
	protected void removeAndSellAllProperty() {
		
		for (int propertyId : propertyIds) {
			removeAndSellProperty(propertyId, false);
		}
		propertyIds.clear();
	}
	
	protected void removeAllPropertyExceptHouse() {
		
		int houseId = -1;
		for (int propertyId : propertyIds) {
			if (!(SimUtils.getPropertyById(propertyId) instanceof House)) {
				removeAndSellProperty(propertyId, false);
			}
			else {
				houseId = propertyId;
			}
		}
		if (houseId >= 0) {
			propertyIds.clear();
			propertyIds.add(houseId);
		}
	}
	
	/*=========================================
	 * Standard getters and setters
	 *=========================================
	 */
	private void resetPartnerId() {
		partnerId = -2;
	}
	
	public void setPartner(Human newPartner) {
		partnerId = newPartner.getId();
	}

	protected void setStatusByAge() {
		
		if (age < Constants.HUMAN_ADULT_AGE) {
			if (status != Status.CHILD)
				status = Status.CHILD;
		}
		else if (age >= Constants.HUMAN_ELDERLY_CARE_AGE) {
			if (status != Status.ELDEST) {
				status = Status.ELDEST;
				schoolType = SchoolType.NO_SCHOOL;
				goToElderlyCare();
			}
		}
		else if (age >= Constants.HUMAN_ELDERLY_AGE) {
			if (status != Status.ELDER) {
				stopWorkingAtWorkplace();
				status = Status.ELDER;
				schoolType = SchoolType.NO_SCHOOL;
			}
		}
		else if (status == Status.CHILD) {
			status = Status.UNEMPLOYED;
			schoolType = SchoolType.NO_SCHOOL;
		}
	}
	
	public boolean doesHumanDie(int age) {
		double prob = Math.pow((1.0/125) * age, 5) * (1.0/24);
		if (prob > RandomHelper.nextDouble())
			return true;
		return false;
	}
	
	public double getMoney() {
		return money;
	}
	
	public void getNecessaryMoneyFromPartner(double cost) {
		money -= cost;
		necessaryCost += cost;
	}
	
	public void addMoney(double money) {
		this.money += money;
	}
	
	public void setMoney(double money) {
		this.money = money;
	}

	public void addChild(int childId) {
		if (!childrenIds.contains(childId)) {
			childrenIds.add(childId);
		}
		else {
			Logger.logError("H" + id + " child already in childrenIds : " + childId);
		}
	}
	
	public void addParent(int parentId) {
		
		GridPoint parentAncestor = new GridPoint(1, parentId);
		if (!ancestors.contains(parentAncestor)) {
			ancestors.add(parentAncestor);
		}
		else {
			Logger.logError("H" + id + " parent already in ancestors : " + parentId);
		}
	}
	
	public void removeChild(int childId) {
		if (childrenIds.contains(childId)) {
			Logger.logDebug("H" + id + " remove child " + childId + " from parent");
			childrenIds.remove(childrenIds.indexOf(childId));
		}
		else {
			Logger.logError("H" + id + " child not in childrenIds : " + childId);
		}
	}
	
	public void giveIncomeToPartner(double income) {
		money += income;
		nettoIncome += income;
	}
	
	public boolean isAdult() {
		if (age >= Constants.HUMAN_ADULT_AGE) {
			return true;
		}
		return false;
	}
	
	public int getAge() {
		return age;
	}

	public int getId() {
		return id;
	}

	public boolean getMigrated() {
		return foreigner;
	}
	
	public boolean getForeigner() {
		return foreigner;
	}
	
	public double getNettoIncome() {
		return nettoIncome;
	}

	public double getNecessaryCost() {
		return necessaryCost;
	}
	
	public double getLeftoverMoney() {
		return nettoIncome - necessaryCost;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}
	
	public SchoolType getSchoolType() {
		return schoolType;
	}
	
	public void setSchoolType(SchoolType schoolType) {
		this.schoolType = schoolType;
	}
	
	public boolean isMan() {
		return (!gender);
	}

	public ArrayList<GridPoint> getAncestors() {
		return ancestors;
	}
	
	public Human getPartner() {
		if (partnerId <= -1)
			return null;
		Human partner = HumanUtils.getHumanById(partnerId);
		if (partner != null)
			return partner;
		Logger.logError("H"+ id + " has no partner:" + partnerId);
		return null;
	}
	
	public boolean getHasBeenFisher() {
		return hasBeenFisher;
	}
	
	public int getPartnerId() {
		return partnerId;
	}
	
	public int getWorkplaceId() {
		return workplaceId;
	}
	
	public void setWorkplaceId(int workplaceId) {
		this.workplaceId = workplaceId;
	}
	
	public void setHasBeenFisher() {
		this.hasBeenFisher = true;
	}
	
	protected void setNotHappyTick(boolean isHappy) {
		
		if (isHappy)
			resetNotHappyTick();
		else 
			addNotHappyTick();
	}
	
	private void resetNotHappyTick() {
		notHappyTick = 0;
	}
	
	private void addNotHappyTick() {
		notHappyTick ++;
	}
	
	public int getNotHappyTick() {
		return notHappyTick;
	}
	
	public ArrayList<Integer> getParentsIds() {
		ArrayList<Integer> parentsIds = new ArrayList<Integer>();
		for (GridPoint ancestor : ancestors) {
			if (ancestor.getX() == 1) {
				parentsIds.add(ancestor.getY());
			}
		}
		return parentsIds;
	}
	
	public String getChildrenIdsString() {
		
		String datum = "";
		for (Integer childId : childrenIds) {
			if (datum.equals("")) {
				datum += Integer.toString(childId);
			}
			else {
				datum += "," + Integer.toString(childId);
			}
		}		
		return datum;
	}
	
	public String getPropertyIdsString() {
		
		String datum = "";
		for (Integer propertyId : propertyIds) {
			if (datum.equals("")) {
				datum += Integer.toString(propertyId);
			}
			else {
				datum += "," + Integer.toString(propertyId);
			}
		}		
		return datum;
	}
	
	public ArrayList<Integer> getChildrenIds() {
		return childrenIds;
	}
	
	public ArrayList<Integer> getPropertyIds() {
//		Logger.logDebug("H" + getId() + " is calling getPropertyIds");
		return propertyIds;
	}
	
	/*=========================================
	 * Graphics and information
	 *========================================
	 */
	public String getStatusString() {
		return status.toString();
	}
	
	public String getSchoolTypeString() {
		return schoolType.toString();
	}
	
	@Override
	public String toString() {
		return String.format("Human (" + id + "), location %s", SimUtils.getGrid().getLocation(this));
	}
	
	public String getLabel() {

		return Integer.toString(id) + "|" + age;
	}

	public VSpatial getSpatialImage() {

		return spatialImages.get(status);
	}

	public void setSpatialImages(HashMap<Status, VSpatial> spatialImages) {
		this.spatialImages = spatialImages;	
	}

	/*
	 * Norm related
	 */
	protected boolean hasGroup(int gId){
		for (Group myGr : groupList) {
			if(myGr.getId() == gId)
				return true;			
		}
		return false;
	}

	public double getLastDonationAmount() {
		return lastDonationAmount;
	}

	public void setLastDonationAmount(double lastDonationAmount) {
		this.lastDonationAmount = lastDonationAmount;
	}
	
	public void becomeGroupMember(Group gr){
		boolean alreadyAmember = false;
		for (Group myGrp : groupList) {			
			if(myGrp.getId() == gr.getId()){
				alreadyAmember = true;
				break;
			}
		}
		if(!alreadyAmember){
			Log.printLog("H" + getId() + " became a member of G" + gr.getId() + " entitled " + gr.getTitle());
			groupList.add(gr);
			addNeighborhoodNorms(gr.getId());
		}else
			Log.printLog("H" + getId() + " is already a member of group " + gr.getId()+ " entitled " + gr.getTitle()) ;
	}
	
	
	private void addNeighborhoodNorms(int groupID) {
		Logger.logDebug("H" + getId() + " is becoming group member G" + groupID + " and trying to add norms. normList " + ((normList != null) ? normList.size() : "null"));
		Collection<Norm> norms = new ArrayList<Norm>();
		String livingGroupName = getLivingGroupName();
		Norm groupNorm = null;
		if(livingGroupName.equals(HouseType.CHEAP.name())){
			groupNorm = new Norm((String) Constants.NORM_TYPE_LIST.get(0), Constants.CHEAP_DONATION_DEFAULT_NORM_TITLE, groupID);
			groupNorm.setRepetition(1);			
		}else if(livingGroupName.equals(HouseType.STANDARD.name())){
			groupNorm = new Norm((String) Constants.NORM_TYPE_LIST.get(0), Constants.STANDARD_DONATION_DEFAULT_NORM_TITLE, groupID);
			groupNorm.setRepetition(1);
		}else if(livingGroupName.equals(HouseType.EXPENSIVE.name())){
			groupNorm = new Norm((String) Constants.NORM_TYPE_LIST.get(0), Constants.EXPENSIVE_DONATION_DEFAULT_NORM_TITLE, groupID);
			groupNorm.setRepetition(1);
		}
//		normList.clear();//TODO: here when the agent change the living group, he will forget all the norms he had in the 
		//previous group. But, it can take the norms that has been internalized b.
		//This might be how a norm will change in a group
//		norms.add(cheapGroup); norms.add(standardGroup); norms.add(expensiveGroup);
		if(groupNorm != null){
			norms.add(groupNorm);
			normList.put(groupID, norms);
			Logger.logDebug("H" + getId() + " is becoming group member G" + groupID + " and added norms. normList " + ((normList != null) ? normList.size() : "null"));
		}
		else
			Log.printLog("H" + getId() + " joined group G"+ groupID + " which doesn't have any norm.");
	}

	public void leaveGroup(Group gr){
		
		for (int idx = 0; idx < groupList.size(); idx ++){			
			if(groupList.get(idx).getId() == gr.getId()){
				groupList.remove(idx);
				Log.printDebug("H" + getId() + " eliminated its membership of group G" + gr.getId()+ " entitled " + gr.getTitle()) ;
				return;
			}
		}
		Log.printLog("H" + id + " is not a member of group " + gr.getId()+ " entitled " + gr.getTitle()) ;
	}

	public void leaveGroup(int groupID){
		for (int idx = 0; idx < groupList.size(); idx ++){			
			if(groupList.get(idx).getId() == groupID){
				groupList.remove(idx);
				Log.printLog("H" + getId() + " eliminated its membership of group G" + groupID) ;
				return;
			}
		}
		Log.printLog("H" + id + " is not a member of group " + groupID ) ;
	}
	
	public int getLivingGroupId() {
		int gId = -1;
		int numOfGroups = 0;
		String groupTtl = "";
		for (Group myGrp : groupList) {
			groupTtl = myGrp.getTitle();
			if(groupTtl.equals(HouseType.CHEAP.name()) | 
					groupTtl.equals(HouseType.EXPENSIVE.name()) |
					groupTtl.equals(HouseType.HOMELESS.name()) |
					groupTtl.equals(HouseType.STANDARD.name()) |
					groupTtl.equals(HouseType.WITH_OTHERS.name())){
				numOfGroups ++;
				gId = myGrp.getId();
			}				
		}
		if(numOfGroups <=1 )
			return gId;
//		else
			Log.printError("H" + getId() + " is a member of " + numOfGroups + " neighborhoods!" );
			return -1;
	}
	
	public String getLivingGroupName() {
		int numOfGroups = 0;
		String groupTtl = "";
		for (Group myGrp : groupList) {
			groupTtl = myGrp.getTitle();
			if(groupTtl.equals(HouseType.CHEAP.name()) | 
					groupTtl.equals(HouseType.EXPENSIVE.name()) |
					groupTtl.equals(HouseType.HOMELESS.name()) |
					groupTtl.equals(HouseType.STANDARD.name()) |
					groupTtl.equals(HouseType.WITH_OTHERS.name())){
				numOfGroups ++;
			}				
		}
		if(numOfGroups <=1 )
			return groupTtl;
//		else
			Log.printError("H" + getId() + " is a member of " + numOfGroups + " neighborhoods!" );
			return null;
	}

	public  Map<Integer, Collection<Norm>> getNormList() {
		return normList;
	}
	
	public Collection<Norm> getNormListByGroupId(int groupID){
		System.out.println("H" + getId() + " get norm list " + normList.get(groupID));
		return normList.get(groupID);
	}

	public void addToNormList(int groupID, Norm newNorm) {
		if(this.normList.get(groupID) != null){
			if(getNormListByGroupId(groupID).size() >0){
				if(!getNormListByGroupId(groupID).iterator().next().getTitle().equals(newNorm.getTitle()))
					this.normList.get(groupID).add(newNorm);
				else
					Log.printDebug("H" + getId() + " already has norm " + newNorm.getTitle() + " for his Group G"+groupID);
			}else
				this.normList.get(groupID).add(newNorm);
		}
		else{
			Collection<Norm> norms = new ArrayList<Norm>();
			this.normList.put(groupID, norms);
		}
			
	}
	
	protected void resetNormRepetition(int groupID, String normTitle) {
		for (Norm nr : this.normList.get(groupID)) {
			Log.printDebug("before resetting repetition : " + this.normList.get(groupID));
			if(nr.getTitle().equals(normTitle))
				nr.resetNorm();
			Log.printDebug("after resetting repetition : " + this.normList.get(groupID));
		}
	}

	protected String resetExistingNormRepetition(int groupID, String normTtl) {
		String returnedNormTitle = null;
		System.out.println("H" + getId() + " in resetExistingNormRepetition " + this.normList.get(groupID).size());
		Norm norm = null;
		for (Norm nr : this.normList.get(groupID)) {
			if (nr.getTitle().equals(normTtl))
				norm = nr;
		}
		if(norm == null) return null;
		
		returnedNormTitle = norm.getTitle();
		System.out.println("H" + getId() + " normTitle " + returnedNormTitle + " type " + norm.getType() + " noRepetition time " + norm.getNoRepetition());
		if(norm.getNoRepetition() > Constants.T_DISAPPEARING){
			//remove the norm 
//				normList.get(groupID).remove(nr);
			norm.setRepetition(0);
			norm.setNoRepetition(0);
		}else{
			norm.setRepetition(0);
			norm.incraseNoRepetition();
		}				
		return returnedNormTitle;
	}
	
	protected int getRepetition(int groupID, String normTitle) {
		if(normList != null){
			for (Norm nr : this.normList.get(groupID)) {
				if(nr.getTitle().equals(normTitle))
					return nr.getRepetition();
			}
		}
		return -1;
	}
	
	protected int getNoRepetition(int groupID, String normTitle) {
		for (Norm nr : this.normList.get(groupID)) {
			if(nr.getTitle().equals(normTitle))
				return nr.getNoRepetition();
		}
		return -1;
	}
	
	public void increaseNormRepetitionByTitle(int groupID, String normTitle) {
		for (Norm nr : this.normList.get(groupID)) {
			Log.printDebug("before increasing repetition : " + this.normList.get(groupID));
			if(nr.getTitle().equals(normTitle))
				nr.increaseRepeatingNorm();
			Log.printDebug("after increasing repetition : " + this.normList.get(groupID));
		}
	}
	
	public void increaseNormRepetitionByType(int groupID, String normType) {
		for (Norm nr : this.normList.get(groupID)) {
			Log.printDebug("before increasing repetition : " + this.normList.get(groupID));
			if(nr.getType().equals(normType))
				nr.increaseRepeatingNorm();
			Log.printDebug("after increasing repetition : " + this.normList.get(groupID));
		}
	}
	
/*	public String getDefaultNormOfNeighborhood(int groupId) {
		String normTitle = "";
		String livingGroupName = getLivingGroupName();
		if(livingGroupName.equals(HouseType.CHEAP.name()))
			normTitle = Constants.CHEAP_DONATION_DEFAULT_NORM_TITLE;			
		else if(livingGroupName.equals(HouseType.STANDARD.name()))
			normTitle = Constants.STANDARD_DONATION_DEFAULT_NORM_TITLE;
		else if(livingGroupName.equals(HouseType.EXPENSIVE.name()))
			normTitle = Constants.EXPENSIVE_DONATION_DEFAULT_NORM_TITLE;
		return normTitle;		
	}*/

	protected ArrayList<String> ifGroupDonationIsNormative(
			double donationAmount, int groupID) {
		//TODO: here the norm, which is saved in logical format, should be parsed
		//TODO: for now we look for the comparative sign and the number after that as we only have donation amount as a norm
		// we implemented a simple sample of it.
		Logger.logDebug("H" + getId() + " is checking normative action of his neighbors G" + groupID +", avgNeighbors " + donationAmount);
		Logger.logDebug("H" + getId() + " is checking normative action of his neighbors, normlist " + (normList != null ? normList.size() : "null"));
		ArrayList<String> normativeActions  = null;
		normativeActions = findNormActionOfGroup(donationAmount, groupID);
		if(normativeActions.size() <= 0)			
			Log.printLog("H" + getId() + "; There is no norm in group G" +groupID + " that matches the action");
		return normativeActions;
	}

	protected String getMaxRepitiedNorm(ArrayList<String> normativeActions, int groupID) {
		double maxRepetition = -1;
		String returnedNormTitle = "";
		for (Norm norm : normList.get(groupID)) {
			for (String normTtl : normativeActions) {
				if(norm.getTitle().equals(normTtl)){
					if(norm.getRepetition() > maxRepetition){
						maxRepetition = norm.getRepetition();
						returnedNormTitle = normTtl;
					}
				}						
			}
		}
		return returnedNormTitle;
	}


	private ArrayList<String> findNormActionOfGroup(double avgNeighborsDonationAmount, int groupID) {
		ArrayList<String> normativeActions = new ArrayList<String>();		
		for (Norm norm : normList.get(groupID)) {
			if(norm == null | norm.equals("")) //he deons't have this norm for group groupID
				continue;
			Logger.logDebug("H" + getId() + " has norm " + norm.getTitle() + " repeated " + norm.getRepetition() + "; notRepited " + norm.getNoRepetition());
			String myNorm = norm.getTitle();
			double[] minMaxNormativeAmount = ParsNormLogic.getDonationAmount(myNorm);
//			String operator = ParsNormLogic.getDonationOperation(myNorm);			
			boolean minCondition = (minMaxNormativeAmount[0] >=0) ? avgNeighborsDonationAmount >= minMaxNormativeAmount[0] : true;
			boolean maxCondition = (minMaxNormativeAmount[1] >=0) ? avgNeighborsDonationAmount <= minMaxNormativeAmount[1] : true;
			Logger.logDebug("H" + getId() + " min " + minCondition + ", max " + maxCondition + ", minx " + minMaxNormativeAmount[0] + ", max " + minMaxNormativeAmount[1]) ;
			if(minCondition & maxCondition){
				normativeActions.add(myNorm);
				break;
			}
		}
		return normativeActions;
	}


	public void leaveGroup(HouseType house) {
		Log.printDebug("H" + getId() + " is calling leaveGroup(HouseType house)");
		int groupId = -1;
		if(house!=null){
			groupId = getGroupIdByName(house.name());
		}
		
		if(groupId != -1){
			for(int idx = 0; idx < groupList.size(); idx ++){
				if(groupList.get(idx).getId() == groupId){
					groupList.remove(idx);
					break;
				}
			}
			Log.printDebug("H"+ getId() + " left group G" + groupId + " and left house " + house.name());
		}
		else
			Log.printDebug("H"+ getId() + " couldn't find a group for previouse housing " + house.name());
	}

	protected int getGroupIdByName(String houseName) {
		int groupId = -1;
		if (equals(HouseType.CHEAP.name()))
			groupId = Constants.CHEAP_GROUP_ID;
		else if(houseName.equals(HouseType.EXPENSIVE.name()) )
			groupId = Constants.EXPENSIVE_GROUP_ID;
		else if(houseName.equals(HouseType.STANDARD.name()))
			groupId = Constants.STANDARD_GROUP_ID;
		else if(houseName.equals(HouseType.HOMELESS.name()) )
			groupId = Constants.HOMELESS_GROUP_ID;
		else if(houseName.equals(HouseType.WITH_OTHERS.name()))
			groupId = Constants.WITH_OTHERS_GROUP_ID;
		return groupId;
	}

	public void becomeGroupMemberByGroupName(String groupName) {
		leaveGroup(getLivingGroupId());
		Group group = null; 
		Logger.logDebug("H" + getId() + " is calling becomeGroupMemberByGroupName, gname " + groupName);
	
		if(groupName!=null){
			if (groupName.equals(HouseType.CHEAP.name()))
				group =new Group(Constants.CHEAP_GROUP_ID,HouseType.CHEAP.name());
			else if(groupName.equals(HouseType.EXPENSIVE.name()) )
				group = new Group(Constants.EXPENSIVE_GROUP_ID,HouseType.EXPENSIVE.name());
			else if(groupName.equals(HouseType.STANDARD.name()))
				group = new Group(Constants.STANDARD_GROUP_ID,HouseType.STANDARD.name());
			else if(groupName.equals(HouseType.HOMELESS.name()) )
				group= new Group(Constants.HOMELESS_GROUP_ID,HouseType.HOMELESS.name());
			else if(groupName.equals(HouseType.WITH_OTHERS.name()))
				group= new Group(Constants.WITH_OTHERS_GROUP_ID,HouseType.WITH_OTHERS.name());
		}
		
		if(group != null)
			becomeGroupMember(group);
		else
			Log.printError("H" + getId() + " couldn't find the correct houseType " + groupName);
	}


	public boolean isMember(String groupName) {
		for(Group gr : groupList){
			if(gr.getTitle().equals(groupName))
				return true;
		}
		return false;
	}
	
	public boolean isMember(int groupId) {
		for(Group gr : groupList){
			if(gr.getId() == groupId)
				return true;
		}
		return false;
	}


	public double getMyValueBasedDonation() {
		return myValueBasedDonation;
	}


	public void setMyValueBasedDonation(double myValueBasedDonation) {
		this.myValueBasedDonation = myValueBasedDonation;
	}
}