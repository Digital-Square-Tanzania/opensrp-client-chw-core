package org.smartregister.chw.core.dao;

import org.smartregister.dao.AbstractDao;

import java.util.List;

public class CoreHivDao extends AbstractDao {
    public static boolean isHivMember(String baseEntityId) {
        String sql = "select count(*) count from ec_hiv_register where base_entity_id = '" + baseEntityId + "'";

        DataMap<Integer> dataMap = cursor -> getCursorIntValue(cursor, "count");

        List<Integer> res = readData(sql, dataMap);

        if (res == null || res.size() < 1)
            return false;
        return res.get(0) == 1;
    }

    public static void cleanAncDataForClient(String baseEntityId) {
        //this deletes from ec_hiv_register, ec_cbhs_register, ec_hiv_outcome, ec_hts_register if the client was initially tested negative and now re-registering
        String cleanCBHSRegister = "DELETE FROM ec_cbhs_register WHERE base_entity_id = '" + baseEntityId + "'";
        updateDB(cleanCBHSRegister);

        String cleanHivRegister = "DELETE FROM ec_hiv_register WHERE base_entity_id = '" + baseEntityId + "'";
        updateDB(cleanHivRegister);

        String cleanHivOutcomeRegister = "DELETE FROM ec_hiv_outcome WHERE entity_id = '" + baseEntityId + "'";
        updateDB(cleanHivOutcomeRegister);

        String cleanHTSRegister = "DELETE FROM ec_hts_register WHERE base_entity_id = '" + baseEntityId + "'";
        updateDB(cleanHTSRegister);
    }


}
