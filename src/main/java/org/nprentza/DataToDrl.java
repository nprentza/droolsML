package org.nprentza;

import org.drools.core.ClassObjectFilter;
import org.drools.io.ReaderResource;
import org.drools.model.codegen.ExecutableModelProject;
import org.drools.verifier.Verifier;
import org.drools.verifier.builder.VerifierBuilderFactory;
import org.drools.verifier.data.VerifierReport;
import org.drools.verifier.report.components.MissingRange;
import org.emla.dbcomponent.Dataset;
import org.emla.learning.LearningSession;
import org.emla.learning.oner.Frequency;
import org.emla.learning.oner.FrequencyTable;
import org.kie.api.KieBase;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;
import org.nprentza.dataaccess.AgentDatapoint;
import org.nprentza.gapanalysis.IntGap;
import tech.tablesaw.api.ColumnType;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class DataToDrl{

    protected KieBase kieBase;
    protected String DRL;
    protected final String appName;
    protected Dataset ds;
    protected final String trainSplit = "train";
    protected final String testSplit = "test";
    protected DrlAssessment trainAssessment=null; //  initially empty

    protected LearningSession emlaSession;
    protected Mode learningMode;

    //  internal structures
    protected Map<Integer, String> dataPredictors;
    protected Map<String, Predictor> predictors;
    protected List<BaseDatapoint> facts;

    public enum Mode {
        NEW_DRL,
        EXTEND_DRL
    }

    protected DataToDrl(String appName, String drlContents, String drlPath, Dataset ds) throws IOException {

        this.appName=appName;
        this.ds = ds;

        if (drlPath==null){     //  learning mode should be set to NEW_DRL
            DRL=drlContents;
            kieBase = new KieHelper().addContent(DRL, appName).build(ExecutableModelProject.class);
            learningMode = Mode.NEW_DRL;
        }
        else{       //  learning mode should be set to EXTEND_DRL
            kieBase = new KieHelper().addFromClassPath(drlPath).build(ExecutableModelProject.class);
            DRL = drlFileToString(drlPath);
            learningMode = Mode.EXTEND_DRL;
        }

    }

    /*
        getter, setters
     */
    public String getDRL(){return DRL;}
    public void setDRL(String drl){this.DRL=drl;}

    public DrlAssessment getTrainAssessment(){return trainAssessment;}

    //  each application (i.e. DatataccessAop) overwrites this method to load different types of records into memory
    protected void loadFactsFromData(String dataSplit){}

    /*
        learning methods
     */
    //  learn a new DRL from data
    public void learnDRL(String dataSplit){
        this.emlaSession = new LearningSession(ds,appName);
        predictors = new HashMap<>();
        loadFactsFromData(dataSplit);

        /*
            1. initial rules to cover all datapoints
         */
        learnForCoverage(dataSplit);
        System.out.println("learn for Coverage - assessment:\n" + trainAssessment.toString());

         /*
            2.  reduce errors (if any)
         */
        if (trainAssessment.getErrors() > 0) {
            learnReduceErrors(dataSplit);
            System.out.println("learn-ReduceErrors - assessment :\n" + trainAssessment.toString());
        }
    }

    protected void learnForCoverage(String dataSplit){
        while (trainAssessment==null || trainAssessment.getCoverage()<1){
            if (singleDrlUpdateRun(getDatapointsNotCovered(),dataSplit)>0){
                trainAssessment = assessDrl(dataSplit);
            }else {break;}
        }
    }

    protected void learnReduceErrors(String dataSplit){
        while (trainAssessment.getErrors()>0) {
            List<String> predictorsWithErrors = dataPredictors.entrySet().stream().filter(entry -> getWrongPredictions().contains(entry.getKey()))
                    .map(entry -> entry.getValue())
                    .distinct()
                    .collect(Collectors.toList());

            if (!predictorsWithErrors.isEmpty()) {
                final int[] updates = {0};
                predictorsWithErrors.forEach(ruleName -> {
                    List<Integer> dataIDs = dataPredictors.entrySet().stream().filter(entry -> entry.getValue().equals(ruleName)).map(entry -> entry.getKey()).collect(Collectors.toList());
                    List<FrequencyTable> frequencyTables = emlaSession.calculateFrequencyTables(ds, trainSplit, dataIDs);
                    Predictor predictorError = predictors.get(ruleName);
                    List<String> otherTargetValues = ds.getUniqueTargetValues().stream().filter(value -> !value.equals(predictorError.target())).collect(Collectors.toList());
                    for (String newTarget : otherTargetValues) {
                        Frequency newFrequency = emlaSession.calculateFrequencyHighCoverageLowError(frequencyTables, newTarget);
                        Predictor predictorToAdd = Predictor.fromFrequency(newFrequency, null);
                        updates[0] = updateDrl(predictorToAdd, true) ? updates[0] +1 : updates[0];
                    }
                });
                if (updates[0]>0){trainAssessment = assessDrl(dataSplit);}
                else {break;}
            }
        }
    }

    //  extend existing DRL from data
    public void extendDRL(String dataSplit){
        trainAssessment = drlEvaluation(dataSplit);
        System.out.println("DRL Initial assessment: " + trainAssessment.toString());
        if (trainAssessment.getCoverage()==1 && trainAssessment.getErrors()==0){
            //  DRL perfectly covers dataSplit
            return;
        }

        //  learn / extend existing DRL
        this.emlaSession = new LearningSession(ds,appName);
        predictorsFromDrl();
        //  no need to load facts, already loaded by drlEvaluation

        if (trainAssessment.getCoverage()<1){
            extendForCoverage(dataSplit);
        }

        if (trainAssessment.getErrors()>0){
            extendReduceErrors(dataSplit);
        }
     }

    //  drl updates for extendDRL use the feature/predictor type (ColumnType) to filter the frequencies returned from emla
    //      we do this to use gap-analysis results that currently can be used with numberical fields
    protected void extendForCoverage(String dataSplit){
        while (trainAssessment.getCoverage()<1){
            if (singleDrlUpdateRun(getDatapointsNotCovered(), ColumnType.INTEGER,dataSplit)>0){
                trainAssessment = assessDrl(dataSplit);
            }else {break;}
        }
    }

    //  drl updates for extendDRL use the feature/predictor type (ColumnType) to filter the frequencies returned from emla
    //      we do this to use gap-analysis results that currently can be used with numberical fields
    protected void extendReduceErrors(String dataSplit){
        while (trainAssessment.getErrors()>0) {
            if (singleDrlUpdateRun(getWrongPredictions(), ColumnType.INTEGER, dataSplit) > 0) {
                trainAssessment = assessDrl(dataSplit);
            } else {
                break;
            }
        }
    }

    //  DRL updates during learning process
    protected int singleDrlUpdateRun(List<Integer> caseIDs, String dataSpplit){
        return singleDrlUpdateRun(caseIDs,null,trainSplit);
    }

    protected int singleDrlUpdateRun(List<Integer> caseIDs, ColumnType filterByColumnType, String trainSplit){
        int updates=0;
        List<FrequencyTable> frequencyTables = emlaSession.calculateFrequencyTables(ds,trainSplit,caseIDs);
        for (String targetValue : ds.getUniqueTargetValues()){
            Frequency targetFrequency = filterByColumnType==null ?
                    emlaSession.calculateFrequencyHighCoverageLowError(frequencyTables,targetValue) :
                    emlaSession.calculateFrequencyHighCoverageLowError(frequencyTables,filterByColumnType,targetValue);
            updates += singleDrlUpdate(targetFrequency);
        }
        return updates;
    }

    protected int singleDrlUpdate(Frequency targetFrequency){
        if (targetFrequency==null){
            return 0;
        }else{
            return (learningMode == Mode.NEW_DRL) ? (updateDrl(targetFrequency) ? 1 : 0)
                    : (updateDrl(targetFrequency, ds.getDsTable().column(targetFrequency.getFeatureName()).type()) ? 1 : 0);
        }
    }

    //  for extendDRL we need to load rule-names from kieBase into the predictors list
    private void predictorsFromDrl(){
        predictors = new HashMap<>();
        for ( KiePackage kp : kieBase.getKiePackages() ) {
            for (Rule rule : kp.getRules()) {
                predictors.put(rule.getName(),null);
            }
        }
    }

    /*
        testing / evaluation
            drlEvaluation: evaluate current DRL against the dataSplit (train/test) of datapoints in ds
     */

    public DrlAssessment drlEvaluation(String dataSplit){
        this.loadFactsFromData(dataSplit);
        KieSession kieSession = kieBase.newKieSession();
        dataPredictors = new HashMap<>();
        kieSession.setGlobal("dataPredictors", dataPredictors);
        for (BaseDatapoint a : facts) {
            a.setPrediction(null);
            kieSession.insert(a);
        }

        kieSession.fireAllRules();

        List<AgentDatapoint> factsProcessed = new ArrayList(kieSession.getObjects(new ClassObjectFilter(AgentDatapoint.class)));

        int covered = factsProcessed.stream().filter(f -> f.getPrediction() != null).collect(Collectors.toList()).size();
        int errors = factsProcessed.stream().filter(f -> !f.isPredictionCorrect()).collect(Collectors.toList()).size();

        DrlAssessment assessment = new DrlAssessment((double) covered / facts.size(), 0, errors);
        if (learningMode==Mode.EXTEND_DRL) {assessment.setIntGaps(gapAnalysis(DRL));}
        return assessment;
    }

    public DrlAssessment validateDRL(String dataSplit){
        return assessDrl(dataSplit);
    }

    //  TODO: remove this method
    protected DrlAssessment testDRL(boolean gapAnalysis) {
        KieSession kieSession = kieBase.newKieSession();
        dataPredictors = new HashMap<>();
        kieSession.setGlobal("dataPredictors", dataPredictors);

        for (BaseDatapoint a : facts) {
            a.setPrediction(null);
            kieSession.insert(a);
        }

        kieSession.fireAllRules();

        List<AgentDatapoint> factsProcessed = new ArrayList(kieSession.getObjects(new ClassObjectFilter(AgentDatapoint.class)));

        int covered = factsProcessed.stream().filter(f -> f.getPrediction() != null).collect(Collectors.toList()).size();
        int errors = factsProcessed.stream().filter(f -> !f.isPredictionCorrect()).collect(Collectors.toList()).size();

        DrlAssessment assessment = new DrlAssessment((double) covered / facts.size(), 0, errors);
        if (gapAnalysis) {assessment.setIntGaps(gapAnalysis(DRL));}
        return assessment;
    }

    private DrlAssessment assessDrl(String dataSplit){
        kieBase = new KieHelper().addContent(DRL, ResourceType.DRL).build(ExecutableModelProject.class);
        return drlEvaluation(dataSplit);
    }


    /*
        predictors to Drl
     */
    private String addPredictor(Predictor predictor){
        String nextRuleName = "rule" + predictors.size();
        this.predictors.put(nextRuleName, predictor);
        return nextRuleName;
    }

    private boolean predictorExists(Predictor predictor){
        if (predictors.size()==0) {
            return false;
        }else {
            return (!predictors.values().stream().filter(p -> p.equals(predictor)).findAny().isEmpty());
        }
    }

    protected boolean updateDrl(Frequency ruleCondition){ // ColumnType fieldType
        Predictor predictorToAdd = Predictor.fromFrequency(ruleCondition, null);
        return updateDrl(predictorToAdd, true);

    }

    protected boolean updateDrl(Frequency ruleCondition, ColumnType fieldType){   // DrlAssessment assessment

        if (gapAnalysisCheck(ruleCondition,fieldType)){
            Predictor predictorToAdd = Predictor.fromFrequency(ruleCondition,null);
            return updateDrl(predictorToAdd, false);
        }else {
            return false;
        }
    }

    protected boolean updateDrl(Predictor predictorToAdd, boolean checkPredictorsList){
        if (checkPredictorsList && predictorExists(predictorToAdd)){
            return false;
        }
        String ruleName = addPredictor(predictorToAdd);
        DRL += "\n" + DrlConverter.predictorToDrlRule(predictorToAdd,ruleName);
        return true;
    }

    /*
        datapoints processing
     */
    public List<Integer> getDatapointsNotCovered(){
        return this.facts.stream().filter(f -> !f.covered()).collect(Collectors.toList()).stream().map(ff -> ff.getId()).collect(Collectors.toList());
    }

    public List<Integer> getWrongPredictions(){
        return this.facts.stream().filter(f -> !f.isPredictionCorrect()).collect(Collectors.toList()).stream().map(ff -> ff.getId()).collect(Collectors.toList());
    }

    public List<String> getClassesWrongPredictions(List<Integer> caseIDsWrongPredictions){
        return facts.stream().filter(f -> caseIDsWrongPredictions.contains(f.getId()))
                .map(fact -> fact.classLabel)
                .distinct()
                .collect(Collectors.toList());
    }

    /*
        I/O functions
     */
    //  TODO automatic way for drl absolute path
    protected String drlFileToString(String drlPath) throws IOException {

        Path filePath = Path.of("./src/main/resources" + drlPath);
        return Files.readString(filePath);
    }

    public void drlToFile(String fileName)
            throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        writer.write(DRL);

        writer.close();
    }

    /*
           gap-analysis, drools verifier
     */
    protected Map<String, List<IntGap>> gapAnalysis(String DRL){

        //  gap-analysis:
        Verifier verifier = VerifierBuilderFactory.newVerifierBuilder().newVerifier();
        verifier.addResourcesToVerify(new ReaderResource(new StringReader(DRL)), ResourceType.DRL);
        verifier.fireAnalysis();
        VerifierReport result = verifier.getResult();

        //  add gap analysis results for integer fields (currently gaps on string/enum fields are not returned as 'gaps')
        Map<String, List<IntGap>> intGaps = new HashMap<>();
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
        return intGaps;
    }

    protected boolean gapAnalysisCheck(Frequency ruleCondition, ColumnType fieldType){    // DrlAssessment assessment
        if (fieldType==ColumnType.INTEGER){
            List<Double> rangeValues = new ArrayList<>();
            ruleCondition.getOperatorValuePairs().forEach(p -> rangeValues.add((double)p.getRight()));
            Collections.sort(rangeValues);
            return !trainAssessment.intGapsContainFeatureRange(ruleCondition.getFeatureName(), rangeValues.get(0).intValue(), rangeValues.get(rangeValues.size()-1).intValue());
        }else{
            return false;
        }
    }

    /*
        old code - to be deleted
     */

    /*protected DrlAssessment drlFromData_Coverage(){

        // learn frequencies to initialize DRL
        if (singleDrlUpdateRun("train")==0){return null;}

        kieBase = new KieHelper().addContent(DRL, ResourceType.DRL).build(ExecutableModelProject.class);
        DrlAssessment drlAssessment = testDRL(false);

        //  repeat learning new rules for datapoints not already covered
        while (drlAssessment.getCoverage()<1){
            List<Integer> casesNotCovered = getDatapointsNotCovered();

            if (singleDrlUpdateRun(casesNotCovered)==0){
                break;
            } else {
                kieBase = new KieHelper().addContent(DRL, ResourceType.DRL).build(ExecutableModelProject.class);
                drlAssessment = testDRL(false);
            }
        }
        return drlAssessment;
    }*/

    /*
    protected DrlAssessment drlFromData_ReduceErrors(Dataset ds, List<Integer> dataIdsWithErrors){

        //  list with rule-names that produce errors
        List<String> predictorsWithErrors = dataPredictors.entrySet().stream().filter(entry -> dataIdsWithErrors.contains(entry.getKey()))
                .map(entry -> entry.getValue())
                .distinct()
                .collect(Collectors.toList());
        if (!predictorsWithErrors.isEmpty()){
            predictorsWithErrors.forEach(ruleName->{
                List<Integer> dataIDs = dataPredictors.entrySet().stream().filter(entry -> entry.getValue().equals(ruleName)).map(entry -> entry.getKey()).collect(Collectors.toList());
                List<FrequencyTable> frequencyTables = emlaSession.calculateFrequencyTables(ds, trainSplit,dataIDs);
                Predictor predictorError = predictors.get(ruleName);
                List<String> otherTargetValues = ds.getUniqueTargetValues().stream().filter(value -> !value.equals(predictorError.target())).collect(Collectors.toList());
                for (String newTarget : otherTargetValues){
                    Frequency newFrequency = emlaSession.calculateFrequencyHighCoverageLowError(frequencyTables,newTarget);
                    Predictor predictorToAdd = Predictor.fromFrequency(newFrequency,null);
                    updateDrl(predictorToAdd,true);
                }
            });
            return testDRL(false);
        }else{
            return null;
        }

    }*/

    //  TODO: remove this method
    /*
    public DrlAssessment drlFromData() {
        predictors = new HashMap<>();

        this.loadFactsFromData(testSplit);


        DrlAssessment drlAssessment = drlFromData_Coverage();
        System.out.println("drlFromData_Coverage assessment:\n" + drlAssessment.toString());

        if (drlAssessment.getErrors() > 0) {
            DrlAssessment drlAssessmentR = drlFromData_ReduceErrors(ds, getWrongPredictions());
            System.out.println("drlFromData_ReduceErrors errors reduced? :\n" + drlAssessment.toString());
            return (drlAssessmentR != null ? drlAssessmentR : drlAssessment);
        } else {
            return drlAssessment;
        }
    }
    */

}
