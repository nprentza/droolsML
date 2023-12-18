package org.nprentza;

public class DrlAssessment {
    private double coverage;
    private int errors;
    private int conflicts;

    public DrlAssessment(double coverage, int conflicts){
        this.coverage=coverage; errors=0; this.conflicts=conflicts;
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
}
