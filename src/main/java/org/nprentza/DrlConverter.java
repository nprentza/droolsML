package org.nprentza;

import java.util.List;
import java.util.stream.Collectors;

public abstract class DrlConverter {

    public static String drlPackage;
    public static String datapointClassName;
    public static String datapointClassCanonicalName;

    public static String predictorToDrlRule(Predictor predictor, String ruleName){
        String conditions = predictor.conditions().stream()
                .map(c -> predictor.field() + " " + c.getLeft().getValue() + " " + addQuotes(c.getRight()))
                .collect(Collectors.joining(" && "));
        return rule(ruleName, conditions, predictor.target());
    }

    private static String addQuotes(Object value){
        if (value.getClass().equals(String.class)){
            return " '" + value.toString() + "' ";
        }else {return value.toString();}
    }

    public static String preamble() {
        return "package " + drlPackage + ";\n" +   //  "package org.nprentza.dataaccess;\n"
                "\n" +
                "import " + datapointClassCanonicalName + ";\n" +   // "import " + AgentDatapoint.class.getCanonicalName() + ";\n"
                "global java.util.Map dataPredictors;\n" +
                "\n";
    }

    public String rule(String ruleName, String field, String operator, Object value, String decision, String datapointClassName) {
        return rule(ruleName, condition(field, operator, value), decision);
    }

    public String rule(String ruleName, List<String> conditions, String decision, String datapointClassName){
        return "rule '" + ruleName + "' when\n" +
                conditionsToDrl(conditions) +
                "then\n" +
                " $a.setPrediction( '" + decision + "' ); \n" +
                " update( $a ); \n" +
                "end\n";
    }

    private String conditionsToDrl(List<String> conditions){
        if (conditions.size()>0){
            return conditions.stream().map(c -> "  $a: " + datapointClassName + "( " + c + " ) \n").collect(Collectors.joining());
        }else {return "";}
    }

    public static String rule(String ruleName, String condition, String decision) {
        return "rule '" + ruleName + "' when\n" +
                "  $a: " + datapointClassName + "( " + condition + " ) \n" +
                "then\n" +
                " $a.setPrediction( '" + decision + "' );\n" +
                " update( $a ); \n" +
                " dataPredictors.put( $a.getId(), '" + ruleName + "' ); \n" +
                "end\n";
    }

    private String condition(String field, String operator, Object value) {
        return field + " " + operator + valueToDrl(field, value);
    }

    private String quote(String s) {
        return new StringBuilder()
                .append('\'')
                .append(s)
                .append('\'')
                .toString();
    }

    private String valueToDrl(String field, Object value) {
        if (value instanceof Number){
            return value.toString();
        }else {
            return quote(value.toString());
        }
    }
}