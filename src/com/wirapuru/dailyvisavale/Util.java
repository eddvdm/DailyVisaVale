package com.wirapuru.dailyvisavale;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

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
}
