package org.nprentza;

public final class DrlConverter {

    private DrlConverter() {
    }

    public static String predictorToDrlRule(Predictor predictor) {
        String ruleName = predictor.target() + predictor.value();
        return rule(ruleName, predictor.field(), predictor.operator(), predictor.value().toString(), predictor.target());
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

    private static String valueToDrl(String field, Object value) {
        return field.equals("role")
                ? AgentRole.class.getSimpleName() + "." + AgentRole.valueOf(value.toString().toUpperCase()).name()
                : value.toString();
    }

    private static boolean grantAccess(String decision) {
        return decision.equals("allow");
    }
}
