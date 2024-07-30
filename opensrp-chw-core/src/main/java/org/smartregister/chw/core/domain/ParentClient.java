package org.smartregister.chw.core.domain;

import org.smartregister.domain.Client;

public class ParentClient extends Client {
    public String motherBaseEntityId;

    public ParentClient(String baseEntityId) {
        super(baseEntityId);
    }

    public void setMotherBaseEntityId(String motherBaseEntityId) {
        this.motherBaseEntityId = motherBaseEntityId;
    }

    public String getMotherBaseEntityId() {
        return motherBaseEntityId;
    }
}
