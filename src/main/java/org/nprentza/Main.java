package org.nprentza;

import org.emla.dbcomponent.Dataset;
import org.emla.learning.LearningSession;
import org.emla.learning.Frequency;
import org.emla.learning.FrequencyTable;

import java.util.List;

public class Main {

    /*
        validate a DRL  against a set of data to identify any gaps
     */
    public static void main(String[] args) {

        DroolsAgentApp agentApp = new DroolsAgentApp();
        //  load data from a csv file
        Dataset ds = new Dataset("./src/main/resources/agentRequests.csv", "resourceAccess", "access", 1, 0);
        //  create Agent objects from data
        agentApp.loadAgentsFromData(ds);
        //  evaluate DRL
        DrlAssessment assessment = agentApp.evaluateAgentRequests();

        System.out.println("Validating a DRL against a set of data.");

        //  if coverage is less than 100% then we need to find additional rules for data (agent objects) not covered
        if (assessment.getCoverage()<1){
            LearningSession emlaSession = new LearningSession(ds,"agentsApp");
            List<Integer> casesNotCovered = agentApp.getRequestsNotCovered();
            System.out.println("Data cases " + casesNotCovered + " are not covered by the DRL."
                    + " \nWe will use the OneR algorithm to find additional rules for these cases.");
            List<FrequencyTable> frequencyTables = emlaSession.calculateFrequencyTables(ds, "train",casesNotCovered);
            frequencyTables.forEach(ft -> System.out.println(ft.toString()));
            Frequency fHighCovLowError = emlaSession.calculateFrequencyHighCoverageLowError(frequencyTables);
            System.out.println("\nUpdate DRL with the *best* frequency selected:" + fHighCovLowError.toString());
            Predictor bestPredictor = Predictor.fromFrequency(fHighCovLowError);
            // Uncomment the following line to override the best frequency produced by EMLA learning session.
            // Experiment with the age value to see whether it creates a conflict with the gap analysis from drools-verifier.
            // bestPredictor = Predictor.build("age", ">", 50, "allow");
            agentApp.updateDrl(frequencyTables, bestPredictor);
            //  repeat evaluation
            System.out.println("\nRe-evaluate the DRL.");
            assessment = agentApp.evaluateAgentRequests();
            System.out.println("The revised DRL covers all data cases.");
        }else{
            System.out.println("All data cases are covered by the DRL.");
        }

    }
}
