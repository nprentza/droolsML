package org.nprentza;

import org.drools.drl.parser.impl.Operator;

public class IntGap {
    // Initially, the gap is infinite.
    private int lowerBound = Integer.MIN_VALUE;
    private int upperBound = Integer.MAX_VALUE;

    /**
     * drools-verifier friendly.
     */
    public void addBound(Operator operator, String valueAsString) {
        addBound(operator.getOperatorString(), ((Double) Double.parseDouble(valueAsString)).intValue());
    }

    /**
     * Testing friendly.
     */
    public void addBound(String operator, int value) {
        switch (operator) {
            case ">":
                lowerBound = value;
                break;
            case ">=":
                lowerBound = value - 1;
                break;
            case "<":
                upperBound = value;
                break;
            case "<=":
                upperBound = value + 1;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + operator);
        }
    }

    boolean contains(int number) {
        return number > lowerBound && number < upperBound;
    }

    boolean containsRange (int lowValue, int highValue){
            for (int i = lowValue; i<=highValue; i++ ){
                if ( !(i > lowValue && i < upperBound) ){
                    return true;
                }
            }
        return false;
    }

    @Override
    public String toString() {
        if ((long) upperBound - lowerBound < 2) {
            return "no-gap";
        }
        return String.format("(%s, %s)",
                lowerBound == Integer.MIN_VALUE ? ".." : lowerBound,
                upperBound == Integer.MAX_VALUE ? ".." : upperBound);
    }
}
