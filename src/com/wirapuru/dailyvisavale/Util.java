package com.wirapuru.dailyvisavale;

import android.util.Log;
import hirondelle.date4j.DateTime;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by IntelliJ IDEA.
 * User: eduardo
 * Date: 2/8/12
 * Time: 11:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class Util {
    
    public static List<String> JSONArrayToList(JSONArray json_array) {
        List<String> list = new ArrayList<String>();
        try {
            for (int i =0; i < json_array.length(); i++)
                list.add(json_array.getString(i));
        } catch (JSONException e) {
            Log.w(DailyVisaVale.LOG_TAG_ALL, e.getMessage());
        }
        return list;
    }

    public static Float extractMoneyValue(String value) {
        return Float.parseFloat(value.split(" ")[1].replace(".","").replace(",","."));
    }

    public static DateTime extractDate(String value) {
        String[] parts = value.split("/", 3);
        int day = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        // todo handle default year when value is from different than from today (i.e. 31/12)
        int year = parts.length > 2 ? Integer.parseInt(parts[2]) : DateTime.today(TimeZone.getDefault()).getYear();

        return DateTime.forDateOnly(year, month, day);
    }
}
