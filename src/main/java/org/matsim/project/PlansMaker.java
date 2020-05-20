package org.matsim.project;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.Random;

public class PlansMaker {

    private static final Logger log = Logger.getLogger(PlansMaker.class);
    private Scenario scenario;
    private PopulationFactory pf;
    private Random r = new Random(15);
    private Coord i15nb;
    private Coord i15sb;
    private CoordinateTransformation ct;

    // mean and sd for Payson coordinates (normal distribution)
    Double randomNormal = 0.0;
    Coord randomCoord;

    Double latM = 40.03375;
    Double lonM = -111.7362;
    Double latSD = 0.0105619;
    Double lonSD = 0.0135612;

    public PlansMaker(String crs){
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        pf = scenario.getPopulation().getFactory();
        ct = TransformationFactory.getCoordinateTransformation(
                TransformationFactory.WGS84, crs);

        //i15nb = CoordUtils.createCoord(-111.721902, 40.066630);
        //i15sb = CoordUtils.createCoord(-111.738536, 40.049808);
        i15nb = CoordUtils.createCoord(482295.01165782614, 971960.5714109354);
        i15sb = CoordUtils.createCoord(478383.78144965466, 966952.9465414924);
    }


    public Double getRandomNormal(Double mean, Double sd) {
        randomNormal = mean + r.nextDouble()*sd;
        return randomNormal;


    }

    public Coord getRandomCoord(Double latM, Double lonM, Double latSD, Double lonSD) {
        randomCoord = CoordUtils.createCoord(
                getRandomNormal(lonM, lonSD),
                getRandomNormal(latM, latSD)
        );
        return randomCoord;
    }

    public void makePlans(Integer numberofPeople){
        // make a plan for each person
        for(int i = 0; i < numberofPeople; i++){
            Person person = pf.createPerson(Id.createPersonId(i));
            Plan plan = pf.createPlan();

            // Get the plan figured out
            Double randomEmp = r.nextDouble();
            // make random
            if(randomEmp < 0.42) {
                person.getAttributes().putAttribute("employed", "worker");
            } else {
                person.getAttributes().putAttribute("employed", "non-worker");
            }

            // Figure out the daily activity pattern
            // "W", "N", "H"
            if(person.getAttributes().getAttribute("employed") == "worker"){
                Double randDAP = r.nextDouble();

                if(randDAP < 0.63){
                    person.getAttributes().putAttribute("DAP", "W");
                } else if(randDAP < 0.63 + 0.285){
                    person.getAttributes().putAttribute("DAP", "N");
                } else{
                    person.getAttributes().putAttribute("DAP", "H");
                }

            } else {
                Double randDAP2 = r.nextDouble();

                if(randDAP2 < 0.165){
                    person.getAttributes().putAttribute("DAP", "W");
                } else if(randDAP2 < 0.165 + 0.605){
                    person.getAttributes().putAttribute("DAP", "N");
                } else{
                    person.getAttributes().putAttribute("DAP", "H");
                }

            }



            // random home activity
            Coord homeCoord = ct.transform(getRandomCoord(latM, lonM, latSD, lonSD));
            Activity homeActivity = pf.createActivityFromCoord("home", homeCoord);





            if(person.getAttributes().getAttribute("DAP").equals("W")){
                // leave home around 7AM
                homeActivity.setEndTime(7 * 3600 + r.nextGaussian() * 30*60); // random!
                plan.addActivity(homeActivity);

                plan.addLeg(pf.createLeg("car"));

                // need to create a work activity
                // lets say 50% go to work on I15
                // 30% southbound
                // 20% stay in payson
                Double randWork = r.nextDouble();
                if(randWork < 0.5){
                    Coord workCoord = i15nb;
                    Activity workActivity = pf.createActivityFromCoord("work", workCoord);
                    workActivity.setEndTime(17 * 3600 + r.nextGaussian() * 30*60);
                    plan.addActivity(workActivity);
                } else if(randWork < 0.5 + 0.3){
                    Coord workCoord = i15sb;
                    Activity workActivity = pf.createActivityFromCoord("work", workCoord);
                    workActivity.setEndTime(17 * 3600 + r.nextGaussian() * 30*60);
                    plan.addActivity(workActivity);
                } else {
                    Coord workCoord = ct.transform(getRandomCoord(latM, lonM, latSD, lonSD));
                    Activity workActivity = pf.createActivityFromCoord("work", workCoord);
                    workActivity.setEndTime(17 * 3600 + r.nextGaussian() * 30*60);
                    plan.addActivity(workActivity);
                }


                plan.addLeg(pf.createLeg("car"));

                // need to create an optional other activity
                // probability of "other" trips will be 50%
                Double randOther = r.nextDouble();

                if(randOther < 0.5){
                    Coord otherCoord = ct.transform(getRandomCoord(latM, lonM, latSD, lonSD)); // make this random in payson
                    Activity otherActivity = pf.createActivityFromCoord("other", otherCoord);
                    otherActivity.setEndTime(18 * 3600 + r.nextGaussian() * 4*60); //
                    plan.addActivity(otherActivity);

                    plan.addLeg(pf.createLeg("car"));

                    Activity homeAgain = pf.createActivityFromCoord("home", homeCoord);
                    plan.addActivity(homeAgain);

                } else {
                    Activity homeAgain = pf.createActivityFromCoord("home", homeCoord);
                    plan.addActivity(homeAgain);
                }




            } else if(person.getAttributes().getAttribute("DAP").equals("N")) {
                // set time they leave home within 5 hours of 2:00
                homeActivity.setEndTime(8 * 3600 + r.nextGaussian() * 2*3600); // random!
                plan.addActivity(homeActivity);

                plan.addLeg(pf.createLeg("car"));


                // need to create between 1 and 3 discretionary activities
                // assume all one tour.


                Double randAct = r.nextDouble();
                // probability of first trip is 100%
                if(randAct < 1){
                    // probability of where the trip goes
                    Double rand1 = r.nextDouble();
                    if(rand1 < 0.6){
                        Coord otherCoord = ct.transform(getRandomCoord(latM, lonM, latSD, lonSD)); // make this random in payson
                        Activity otherActivity = pf.createActivityFromCoord("other", otherCoord);
                        otherActivity.setEndTime(12 * 3600 + r.nextGaussian() * 1*3600); //
                        plan.addActivity(otherActivity);
                    } else if(rand1 < 0.6 + 0.25){
                        Coord otherCoord = i15nb;
                        Activity otherActivity = pf.createActivityFromCoord("other", otherCoord);
                        otherActivity.setEndTime(17 * 3600 + r.nextGaussian() * 3*3600); //
                        plan.addActivity(otherActivity);
                    } else {
                        Coord otherCoord = i15sb;
                        Activity otherActivity = pf.createActivityFromCoord("other", otherCoord);
                        otherActivity.setEndTime(21 * 3600 + r.nextGaussian() * 1*3600); //
                        plan.addActivity(otherActivity);
                    }
                    plan.addLeg(pf.createLeg("car"));

                }
                // probability of second trip
                else if(randAct < 0.5){
                    // probability of where the trip goes
                    Double rand2 = r.nextDouble();
                    if(rand2 < 0.6){
                        Coord otherCoord = ct.transform(getRandomCoord(latM, lonM, latSD, lonSD)); // make this random in payson
                        Activity otherActivity = pf.createActivityFromCoord("other", otherCoord);
                        otherActivity.setEndTime(12 * 3600 + r.nextGaussian() * 1*3600); //
                        plan.addActivity(otherActivity);
                    } else if(rand2 < 0.6 + 0.25){
                        Coord otherCoord = i15nb;
                        Activity otherActivity = pf.createActivityFromCoord("other", otherCoord);
                        otherActivity.setEndTime(17 * 3600 + r.nextGaussian() * 3*3600); //
                        plan.addActivity(otherActivity);
                    } else {
                        Coord otherCoord = i15sb;
                        Activity otherActivity = pf.createActivityFromCoord("other", otherCoord);
                        otherActivity.setEndTime(21 * 3600 + r.nextGaussian() * 1*3600); //
                        plan.addActivity(otherActivity);
                    }
                    plan.addLeg(pf.createLeg("car"));

                }
                // probability of third trip
                else {
                    // probability of where the trip goes
                    Double rand3 = r.nextDouble();
                    if(rand3 < 0.6){
                        Coord otherCoord = ct.transform(getRandomCoord(latM, lonM, latSD, lonSD)); // make this random in payson
                        Activity otherActivity = pf.createActivityFromCoord("other", otherCoord);
                        otherActivity.setEndTime(12 * 3600 + r.nextGaussian() * 1*3600); //
                        plan.addActivity(otherActivity);
                    } else if(rand3 < 0.6 + 0.25){
                        Coord otherCoord = i15nb;
                        Activity otherActivity = pf.createActivityFromCoord("other", otherCoord);
                        otherActivity.setEndTime(17 * 3600 + r.nextGaussian() * 3*3600); //
                        plan.addActivity(otherActivity);
                    } else {
                        Coord otherCoord = i15sb;
                        Activity otherActivity = pf.createActivityFromCoord("other", otherCoord);
                        otherActivity.setEndTime(21 * 3600 + r.nextGaussian() * 1*3600); //
                        plan.addActivity(otherActivity);
                    }
                    plan.addLeg(pf.createLeg("car"));

                }

                Activity homeAgain = pf.createActivityFromCoord("home", homeCoord);
                plan.addActivity(homeAgain);





            } else if(person.getAttributes().getAttribute("DAP").equals("H")) {
                // No activities
            }



            person.addPlan(plan);
            scenario.getPopulation().addPerson(person);
        }
    }

    public void writePlans(String file){
        PopulationWriter writer = new PopulationWriter(scenario.getPopulation());
        writer.write(file);
    }


    public static void main(String[] args){
        PlansMaker pm = new PlansMaker("EPSG:2849");
        pm.makePlans(47446);
        pm.writePlans("scenarios/equil/2050_plans.xml.gz");
    }
}
