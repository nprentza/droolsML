package org.nprentza;

import org.drools.io.ReaderResource;
import org.drools.verifier.Verifier;
import org.drools.verifier.builder.VerifierBuilderFactory;
import org.drools.verifier.data.VerifierReport;
import org.drools.verifier.report.components.MissingRange;
import org.drools.verifier.report.components.Severity;
import org.drools.verifier.report.components.VerifierMessageBase;
import org.emla.dbcomponent.Dataset;
import org.emla.learning.FrequencyTable;
import org.drools.model.codegen.ExecutableModelProject;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class DroolsAgentApp {

    private KieBase kieBase;
    private List<Agent> agentRequests;
    private List<Integer> allow;
    private List<Integer> deny;

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

    public void updateDrl(List<FrequencyTable> frequencyTables, Predictor predictor) {
        Verifier verifier = VerifierBuilderFactory.newVerifierBuilder().newVerifier();
        verifier.addResourcesToVerify(new ReaderResource(new StringReader(DRL)), ResourceType.DRL);
        verifier.fireAnalysis();
        VerifierReport result = verifier.getResult();

        IntGap gap = new IntGap();
        for (MissingRange message : result.getRangeCheckCauses()) {
            gap.addBound(message.getOperator(), message.getValueAsString());
        }

        /*
            if bestFrequency condition is not already in the drl then add it
            else    process frequencyTables to find the next best frequency and repeat the process
         */
        String field = predictor.field();
        if (!field.equals("age") || gap.contains((Integer) predictor.value())) {
            DRL += "\n" + DrlConverter.predictorToDrlRule(predictor);
            kieBase = new KieHelper().addContent(DRL, ResourceType.DRL).build(ExecutableModelProject.class);
        } else {
            // TODO take the next best frequency and repeat the gap analysis.
            throw new IllegalArgumentException("The proposed predictor (" + predictor
                    + ") does not fit in the '" + field
                    + "' gap (" + gap + ").");
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

    public DrlAssessment evaluateAgentRequests(){
        Verifier verifier = VerifierBuilderFactory.newVerifierBuilder().newVerifier();
        verifier.addResourcesToVerify(new ReaderResource(new StringReader(DRL)), ResourceType.DRL);
        verifier.fireAnalysis();
        VerifierReport result = verifier.getResult();

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
        DrlAssessment assessment = new DrlAssessment((double)(allow.size() + deny.size()) / agentRequests.size());
        return assessment;
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
