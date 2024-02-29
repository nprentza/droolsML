package org.nprentza;

import java.util.stream.Collectors;

public abstract class DrlConverter {

    public static String drlPackage;
    public static String datapointClassName;
    public static String datapointClassCanonicalName;

    public static String predictorToDrlRule(Predictor predictor, String ruleName, DataToDrl.Mode learningMode){
        String conditions = predictor.conditions().stream()
                .map(c -> predictor.field() + " " + c.getLeft().getValue() + " " + addQuotes(c.getRight()))
                .collect(Collectors.joining(" && "));
        return rule(ruleName, conditions, predictor.target(), learningMode);
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

    public static String rule(String ruleName, String condition, String decision, DataToDrl.Mode learningMode) {
        return "rule '" + ruleName + "' when\n" +
                "  $a: " + datapointClassName + "( " + condition + " ) \n" +
                "then\n" +
                " $a.setPrediction( '" + decision + "' );\n" +
                " update( $a ); \n" +
                (learningMode== DataToDrl.Mode.NEW_DRL ? " dataPredictors.put( $a.getId(), '" + ruleName + "' ); \n" : "") +
                "end\n";
    }

    private String quote(String s) {
        return new StringBuilder()
                .append('\'')
                .append(s)
                .append('\'')
                .toString();
    }
}