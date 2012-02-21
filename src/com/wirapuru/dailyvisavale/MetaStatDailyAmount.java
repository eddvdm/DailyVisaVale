package com.wirapuru.dailyvisavale;

import hirondelle.date4j.DateTime;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: eduardo
 * Date: 2/21/12
 * Time: 1:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class MetaStatDailyAmount extends MetaStatAbstract {

    public MetaStatDailyAmount() throws Exception {
        super();
    }

    public MetaStatDailyAmount(Boolean force_regenerate) throws Exception {
        super(force_regenerate);
    }

    public MetaStatDailyAmount(DailyVisaVale.CardData base_data_obj) {
        super(base_data_obj);
    }

    @Override
    void processBaseData() {
        try {
            ArrayList<DateTime> remaining_days = base_data.getPeriodRemainingDays();
            Float remaining_balance = base_data.getRemainingBalance();
            setMetastatData(remaining_balance / remaining_days.size());
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
