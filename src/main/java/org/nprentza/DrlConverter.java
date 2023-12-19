package org.nprentza;

import org.emla.learning.LearningUtils;

import java.util.List;
import java.util.stream.Collectors;

public final class DrlConverter {

    private DrlConverter() {
    }

    public static String predictorToDrlRule(Predictor predictor) {
        String ruleName = predictor.target() + "_" + //predictor.value();
                predictor.conditions().stream().map(c->predictor.field() + c.getRight().toString()).collect(Collectors.joining("_"));

        String conditions = predictor.conditions().stream().map(c -> predictor.field() + " " + c.getLeft().getValue() + " " + c.getRight().toString())
                .collect(Collectors.joining(" && "));
        return rule(ruleName, conditions, predictor.target());
    }

    public static String preamble() {
        return "package org.nprentza;\n" +
                "\n" +
                "import " + Agent.class.getCanonicalName() + ";\n" +
                "import " + AgentRole.class.getCanonicalName() + ";\n" +
                "\n" +
                "global java.util.List allow;\n" +
                "global java.util.List deny;\n" +
                "\n";
    }

    public static String rule(String ruleName, String field, String operator, Object value, String decision) {
        return rule(ruleName, condition(field, operator, value), decision);
    }

    public static String rule(String ruleName, List<String> conditions, String decision){
        return "rule '" + ruleName + "' when\n" +
                conditionsToDrl(conditions) +
                "then\n" +
                "  $a.setGrantAccess( " + grantAccess(decision) + " );\n" +
                "  " + decision + ".add( $a.getId() );\n" +
                "end\n";
    }

    private static String conditionsToDrl(List<String> conditions){
        if (conditions.size()>0){
            return conditions.stream().map(c -> "  $a: Agent( " + c + " ) \n").collect(Collectors.joining());
        }else {return "";}
    }

    public static String rule(String ruleName, String condition, String decision) {
        return "rule '" + ruleName + "' when\n" +
                "  $a: Agent( " + condition + " ) \n" +
                "then\n" +
                "  $a.setGrantAccess( " + grantAccess(decision) + " );\n" +
                "  " + decision + ".add( $a.getId() );\n" +
                "end\n";
    }

    private static String condition(String field, String operator, Object value) {
        return field + " " + operator + " " + valueToDrl(field, value);
    }

    private static String condition(String field, LearningUtils.Operator operator, Object value){
        return field + " " + operator.toString() + " " + valueToDrl(field, value);
    }

    private static String valueToDrl(String field, Object value) {
        return field.equals("role")
                ? AgentRole.class.getSimpleName() + "." + AgentRole.valueOf(value.toString().toUpperCase()).name()
                : value.toString();
    }

    private static boolean grantAccess(String decision) {
        return decision.equals("allow");
    }
}