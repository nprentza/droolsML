package org.nprentza;

public abstract class BaseDatapoint {

    protected final int id;
    protected final String classLabel;
    protected String prediction;

    protected BaseDatapoint(int id, String classLabel) {
        this.id = id;
        this.classLabel = classLabel;
    }

    public int getId() {
        return id;
    }

    public void setPrediction(String prediction){this.prediction=prediction;}
    public String getPrediction(){return this.prediction;}
    public boolean covered(){return this.prediction!=null;}
    public boolean isPredictionCorrect(){return this.classLabel.equals(this.prediction);}
}
