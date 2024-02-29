package org.nprentza.dataaccess;

import org.emla.dbcomponent.Dataset;
import org.nprentza.DataToDrl;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.util.ArrayList;

public class DataaccessApp extends DataToDrl {

    public DataaccessApp(String appName, String drlContents, String drlPath, Dataset ds) throws IOException {
        super(appName, drlContents, drlPath,ds);
    }

    @Override
    protected void loadFactsFromData(String dataSplit){
        Table data = this.ds.getDataSplit(dataSplit);
        this.facts = new ArrayList<>();

        for (int i=0; i<data.rowCount(); i++) {
            this.facts.add(AgentDatapoint.fromRawData(
                    data.row(i).getInt("caseID"),
                    data.row(i).getString("role"),
                    data.row(i).getString("experience"),
                    data.row(i).getInt("age"),
                    data.row(i).getString("access")));
        }
    }

}
