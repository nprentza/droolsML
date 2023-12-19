package org.nprentza;

import org.emla.dbcomponent.Dataset;
import org.emla.learning.LearningSession;
import org.emla.learning.oner.Frequency;
import org.emla.learning.oner.FrequencyTable;
import tech.tablesaw.api.ColumnType;

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
        System.out.println("\n**** Rules evaluation on data ********************************************************************************");

        //  if coverage is less than 1 then we need to find additional rules for data (agent objects) not covered
        if (assessment.getCoverage()<1){
            LearningSession emlaSession = new LearningSession(ds,"agentsApp");
            List<Integer> casesNotCovered = agentApp.getRequestsNotCovered();
            System.out.println("Data cases " + casesNotCovered + " are not covered by the DRL."
                    + " \nWe will use the OneR algorithm to find additional rules for these cases.");
            List<FrequencyTable> frequencyTables = emlaSession.calculateFrequencyTables(ds, "train",casesNotCovered);
            frequencyTables.forEach(ft -> System.out.println(ft.toString()));
            //  assumption: update DRL with a rule on a numerical field type (if any)
            ColumnType fielType = ColumnType.INTEGER;
            Frequency fHighCovLowError_Numeric = emlaSession.calculateFrequencyHighCoverageLowError(frequencyTables, fielType);
            if (fHighCovLowError_Numeric!=null){
                System.out.println("\n** Update DRL with Selected condition on numerical feature **\n    " + fHighCovLowError_Numeric.toString());
                agentApp.updateDrl(fHighCovLowError_Numeric, fielType);
                // repeat evaluation
                System.out.println("\nRe-evaluate the DRL.");
                assessment = agentApp.evaluateAgentRequests();
                System.out.println("\n****\nThe revised DRL coverage is: " + assessment.getCoverage());
            }
        }else{
            System.out.println("All data cases are covered by the DRL.");
        }

    }
}
