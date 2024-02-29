package org.nprentza;

import org.apache.commons.lang3.tuple.Pair;
import org.emla.learning.LearningUtils;
import org.emla.learning.oner.Frequency;

import java.util.List;
import java.util.stream.Collectors;

public interface Predictor {

    String field();

    List<Pair<LearningUtils.Operator, Object>> conditions();

    String target();

    /**
     * Create a facade for the EMLA API.
     */
    static Predictor fromFrequency(Frequency frequency, List<Pair<LearningUtils.Operator, Object>> otherConditions) {
        String field = frequency.getFeatureName(); //frequency.getPredictorValues().getKey();
        List<Pair<LearningUtils.Operator, Object>> conditions = frequency.getOperatorValuePairs(); //new ArrayList<>();

        if (otherConditions!=null){
            conditions.addAll(otherConditions);}
        String target = frequency.getMajorityTargetClass(); // frequency.getBestTargetValue();
        return build(field, conditions, target);    // build(field, conditions, target);
    }

    static Predictor build(String field, List<Pair<LearningUtils.Operator, Object>> conditions, String target) {
        return new Predictor() {
            @Override
            public String field() {
                return field;
            }

            @Override
            public List<Pair<LearningUtils.Operator, Object>> conditions() {
                return conditions;
            }

            @Override
            public String target() {
                return target;
            }

            @Override
            public String toString() {
                return conditions.stream().map(c -> field + c.getLeft().toString() + c.getRight().toString()).collect(Collectors.joining(", "))
                        + " => " + target;
            }
            @Override
            public boolean equals(Object p){
                if (this == p) {
                    return true;
                }
                if (p instanceof Predictor) {
                    Predictor anotherPredictor = (Predictor) p;
                    return (anotherPredictor.field().equals(this.field()) &&
                            anotherPredictor.target().equals(this.target()) &&
                            anotherPredictor.conditions().containsAll(this.conditions()) &&
                            anotherPredictor.conditions().size()==this.conditions().size() );
                }else {return false;}
            }
        };
    }
}
