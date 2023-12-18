package org.nprentza;

import org.apache.commons.lang3.tuple.Pair;
import org.emla.learning.LearningUtils;
import org.emla.learning.oner.Frequency;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public interface Predictor {

    String field();

    List<Pair<LearningUtils.Operator, Object>> conditions();

    //String operator();

    //Object value();

    String target();

    /**
     * Create a facade for the EMLA API.
     */
    static Predictor fromFrequency(Frequency frequency) {
        String field = frequency.getFeatureName(); //frequency.getPredictorValues().getKey();
        List<Pair<LearningUtils.Operator, Object>> conditions = new ArrayList<>();
        frequency.getOperatorValuePairs().forEach(condition -> {
            conditions.add(condition);
        });
        String target = frequency.getMajorityTargetClass(); // frequency.getBestTargetValue();
        return build(field, conditions, target);
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
        };
    }
}
