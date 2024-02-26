package org.nprentza;

import org.nprentza.gapanalysis.IntGap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DrlAssessment {
    private double coverage;
    private int errors;
    private int conflicts;
    // gap-analysis
    private Map<String, List<IntGap>> intGaps;

    public DrlAssessment(double coverage, int conflicts){
        this.coverage=coverage; errors=0; this.conflicts=conflicts;
        intGaps = new HashMap<>();
    }

    public DrlAssessment(double coverage, int conflicts, int errors){
        this.coverage=coverage; this.conflicts=conflicts; this.errors=errors;
    }

    public double getCoverage() {
        return this.coverage;
    }

    public void setCoverage(double coverage) {
        this.coverage = coverage;
    }

    public int getErrors() {
        return errors;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }

    public int getConflicts() {
        return conflicts;
    }

    public void setConflicts(int conflicts) {
        this.conflicts = conflicts;
    }

    public String toString(){return "Coverage=" + (this.coverage*100) + "%, errors=" + this.errors + ", conflicts=" + this.conflicts;}

    public void setIntGaps(Map<String, List<IntGap>> gaps){this.intGaps=gaps;}
    public Map<String, List<IntGap>> getIntGaps(){return this.intGaps;}

    public boolean intGapsContainFeatureRange(String featureName, int lowValue, int highValue){
        if (!intGaps.containsKey(featureName)){
            return false;
        }else{
            return intGaps.get(featureName).stream().filter(gap -> gap.containsRange(lowValue,highValue)).findAny().isPresent();
        }
    }
}
