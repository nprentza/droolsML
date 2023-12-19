package org.nprentza;

import org.drools.io.ReaderResource;
import org.drools.verifier.Verifier;
import org.drools.verifier.builder.VerifierBuilderFactory;
import org.drools.verifier.data.VerifierReport;
import org.drools.verifier.report.components.MissingRange;
import org.drools.verifier.report.components.Severity;
import org.drools.verifier.report.components.VerifierMessageBase;
import org.emla.dbcomponent.Dataset;
import org.drools.model.codegen.ExecutableModelProject;
import org.emla.learning.oner.Frequency;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;
import tech.tablesaw.api.ColumnType;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DroolsAgentApp {

    private KieBase kieBase;
    private List<Agent> agentRequests;
    private List<Integer> allow;
    private List<Integer> deny;
    private Map<String, List<IntGap>> intGaps;

    private String DRL = DrlConverter.preamble()
            + DrlConverter.rule("AllowAdmin", "role", "==", "admin", "allow")
            + DrlConverter.rule("DenyGuest", "role", "==", "guest", "deny")
            // Uncomment this rule to create an upper bound for the gap:
            // + DrlConverter.rule("DenyDead", "age", ">",200, "deny")
            + DrlConverter.rule("DenyChildren", "age", "<",19, "deny");

    public DroolsAgentApp(){
        //kieBase = new KieHelper().addFromClassPath("/dataAccess.drl").build(ExecutableModelProject.class);
        kieBase = new KieHelper().addContent(DRL, "org/nprentza/dataAccess.drl").build(ExecutableModelProject.class);
    }

    //  assuming ruleConditions contains a rule on a numerical field type
    public void updateDrl(Frequency ruleCondition, ColumnType fieldType){
        if (fieldType==ColumnType.INTEGER){
            List<Double> rangeValues = new ArrayList<>();
            ruleCondition.getOperatorValuePairs().forEach(p -> rangeValues.add((double)p.getRight()));
            Collections.sort(rangeValues);
            if (this.intGapsContainFeatureRange(ruleCondition.getFeatureName(), rangeValues.get(0).intValue(), rangeValues.get(rangeValues.size()-1).intValue())){
                // add
                DRL += "\n" + DrlConverter.predictorToDrlRule(Predictor.fromFrequency(ruleCondition));
                kieBase = new KieHelper().addContent(DRL, ResourceType.DRL).build(ExecutableModelProject.class);
            }
        }
    }

    public void loadAgentsFromData(Dataset ds){
        this.agentRequests = new ArrayList<>();

        for (int i=0; i<ds.getRowCout(); i++) {
            this.agentRequests.add(Agent.fromRawData(
                    ds.getDsTable().row(i).getInt("caseID"),
                    ds.getDsTable().row(i).getString("role"),
                    ds.getDsTable().row(i).getString("experience"),
                    ds.getDsTable().row(i).getInt("age")));
        }
    }

    private boolean intGapsContainFeatureRange(String featureName, int lowValue, int highValue){
        if (!intGaps.containsKey(featureName)){
            return false;
        }else{
            return intGaps.get(featureName).stream().filter(gap -> gap.containsRange(lowValue,highValue)).findAny().isPresent();
        }
    }

    public DrlAssessment evaluateAgentRequests(){

        //  gap-analysis:
        Verifier verifier = VerifierBuilderFactory.newVerifierBuilder().newVerifier();
        verifier.addResourcesToVerify(new ReaderResource(new StringReader(DRL)), ResourceType.DRL);
        verifier.fireAnalysis();
        VerifierReport result = verifier.getResult();

        //  add gap analysis results for integer fields (currently gaps on string/enum fields are not returned as 'gaps')
        this.intGaps = new HashMap<>();
        for (MissingRange message : result.getRangeCheckCauses()) {
            IntGap gap = new IntGap();
            gap.addBound(message.getOperator(), message.getValueAsString());
            if (intGaps.containsKey(message.getField().getName())){
                intGaps.get(message.getField().getName()).add(gap);
            }else{
                List<IntGap> fieldGaps = new ArrayList<>(); fieldGaps.add(gap);
                intGaps.put(message.getField().getName(),fieldGaps);
            }
        }
        printGapAnalysisResults(result);

        //  evaluate rules on data:

        KieSession kieSession = kieBase.newKieSession();
        allow = new ArrayList<>();
        kieSession.setGlobal("allow", allow);
        deny = new ArrayList<>();
        kieSession.setGlobal("deny", deny);

        for (Agent a : agentRequests){
            kieSession.insert(a);
        }

        kieSession.fireAllRules();

        // return the results of DRL's assessment
        Set<Integer> conflicts = allow.stream().filter(deny::contains).collect(Collectors.toSet());
        Set<Integer> requestsProcessed = Stream.concat(allow.stream().distinct(),deny.stream().distinct()).distinct().collect(Collectors.toSet());
        DrlAssessment assessment = new DrlAssessment((double)(requestsProcessed.size()) / agentRequests.size(), conflicts.size());
        return assessment;
    }

    private void printGapAnalysisResults(VerifierReport result){
        System.out.println("\n**** Gap analysis results ******************************************************************************");
        System.out.println("===== NOTES =====");
        for (VerifierMessageBase message : result.getBySeverity(Severity.NOTE)) {
            System.out.println(message);
        }

        System.out.println("===== WARNS =====");
        for (VerifierMessageBase message : result.getBySeverity(Severity.WARNING)) {
            System.out.println(message);
        }

        System.out.println("===== ERRORS =====");
        for (VerifierMessageBase message : result.getBySeverity(Severity.ERROR)) {
            System.out.println(message);
        }

        System.out.println("===== GAPS =====");
        for (MissingRange message : result.getRangeCheckCauses()) {
            System.out.println(message);
            System.out.println("    >> MissingRange object analysis: [.field = " + message.getField().getName() + "] " +
                    "[.operator = '" + message.getOperator().getOperatorString() + "'] " +
                    "[.getValueAsString() = " + message.getValueAsString()+"]");
        }
    }

    public List<Integer> getRequestsNotCovered(){
        List<Integer> caseIdsNotCovered = new ArrayList<>();

        for (Agent a : agentRequests){
            if (!this.deny.contains(a.getId()) && !this.allow.contains(a.getId())){
                caseIdsNotCovered.add(a.getId());
            }
        }

        return caseIdsNotCovered;
    }
}