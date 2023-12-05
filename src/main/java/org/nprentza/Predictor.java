package org.nprentza;

import org.emla.learning.Frequency;

public interface Predictor {

    String field();

    String operator();

    Object value();

    String target();

    /**
     * Create a facade for the EMLA API.
     */
    static Predictor fromFrequency(Frequency frequency) {
        String field = frequency.getPredictorValues().getKey();
        Object value = frequency.getPredictorValues().getValue();
        String target = frequency.getBestTargetValue();
        return build(field, "==", value, target);
    }

    static Predictor build(String field, String operator, Object value, String target) {
        return new Predictor() {
            @Override
            public String field() {
                return field;
            }

            @Override
            public String operator() {
                return operator;
            }

            @Override
            public Object value() {
                return value;
            }

            @Override
            public String target() {
                return target;
            }

            @Override
            public String toString() {
                return field + operator + value + " => " + target;
            }
        };
    }
}
