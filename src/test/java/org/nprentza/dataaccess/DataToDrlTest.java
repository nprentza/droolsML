package org.nprentza.dataaccess;

import org.emla.dbcomponent.Dataset;
import org.junit.jupiter.api.Test;
import org.nprentza.DrlAssessment;
import org.nprentza.DrlConverter;

import java.io.IOException;

public class DataToDrlTest {

    @Test
    void dataaccessToDrlFile() throws IOException {
        DrlConverter.drlPackage = "org.nprentza.dataaccess";
        DrlConverter.datapointClassName = "AgentDatapoint";
        DrlConverter.datapointClassCanonicalName = "org.nprentza.dataaccess.AgentDatapoint";
        drlFromData();
    }

    public static void drlFromData() throws IOException {
        Dataset ds = new Dataset("./src/test/resources/agentRequests.csv", "resourceAccess", "access", 1, 0);
        DataaccessApp app = new DataaccessApp("dataAccess", DrlConverter.preamble(), null,ds);
        app.learnDRL("train");
        DrlAssessment drlAssessment = app.getTrainAssessment();
        System.out.println("DRL Final assessment:" + drlAssessment.toString());
        app.drlToFile("./src/test/resources/agentRequests_out.drl");
    }

    @Test
    void reviseDrlFile() throws IOException {
        DrlConverter.drlPackage = "org.nprentza.dataaccess";
        DrlConverter.datapointClassName = "AgentDatapoint";
        DrlConverter.datapointClassCanonicalName = "org.nprentza.dataaccess.AgentDatapoint";
        validateExtendDrl();
    }

    public static void validateExtendDrl() throws IOException {
        Dataset ds = new Dataset("./src/test/resources/agentRequests.csv", "resourceAccess", "access", 1, 0);
        DataaccessApp app = new DataaccessApp("dataAccess", null, "/agentRequests.drl",ds);
        app.extendDRL("train");
        DrlAssessment drlAssessment = app.getTrainAssessment();
        System.out.println("DRL Final assessment: " + drlAssessment.toString());
        app.drlToFile("./src/test/resources/agentRequests_R_out.drl");
    }

}
