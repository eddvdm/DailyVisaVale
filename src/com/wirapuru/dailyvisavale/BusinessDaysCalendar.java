package com.wirapuru.dailyvisavale;

import hirondelle.date4j.DateTime;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: eduardo
 * Date: 2/16/12
 * Time: 7:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class BusinessDaysCalendar {

    public static final int DEFAULT_TIMEZONE = -3;
    public static final String DEFAULT_LOCALE = "pt_BR";

    private static int cfg_timezone = DEFAULT_TIMEZONE;
    private static String cfg_locale = DEFAULT_LOCALE;
    private static boolean cfg_saturday_is_business_day = false;
    private static boolean cfg_sunday_is_business_day = false;
    private static int cfg_week_business_days = 5;

    private GregorianCalendar calendar = null;

    public BusinessDaysCalendar() {
        String[] ids = TimeZone.getAvailableIDs(getCfgTimezone() * 60 * 60 * 1000);
        SimpleTimeZone timezone = new SimpleTimeZone(getCfgTimezone() * 60 * 60 * 1000, ids[0]);

        this.calendar = new GregorianCalendar(timezone, new Locale(getCfgLocale()));
    }

    public ArrayList<Integer> getMonthBusinessDays(int month) {
        DateTime today = DateTime.today(TimeZone.getDefault());
        return getMonthBusinessDays(month, today.getYear());
    }

    public ArrayList<Integer> getMonthBusinessDays(int month, int year) {
        ArrayList<Integer> business_days = new ArrayList<Integer>();
        DateTime day_of_month = DateTime.forDateOnly(year, month, 1);
        int days_in_month = day_of_month.getNumDaysInMonth();
        
        // num of days to include each week starting from sunday
        int days_from_sunday = isSaturdayBusinessDay() ? getCgfWeekBusinessDays()+1 : getCgfWeekBusinessDays();
        while (day_of_month.getMonth() == month) {
            int week_day = day_of_month.getWeekDay();
            switch (week_day) {
                case 1: // sunday
                    if (isSundayBusinessDay())
                        business_days.add(day_of_month.getDay());

                    for (int j = 1; j <= days_from_sunday; j++) {
                        day_of_month = day_of_month.plusDays(1);
                        business_days.add(day_of_month.getDay());
                        if (day_of_month.getDay() >= days_in_month)
                            break;
                    }
                    break;
                case 7: // saturday
                    if (isSaturdayBusinessDay())
                        business_days.add(day_of_month.getDay());
                    break;
                default:
                    business_days.add(day_of_month.getDay());
                    break;
            }
            day_of_month = day_of_month.plusDays(1);
        }

        return business_days;
    }

    public ArrayList<DateTime> getBusinessDaysInRange(DateTime start, DateTime end) {
        ArrayList<DateTime> business_days = new ArrayList<DateTime>();
        int days_in_range = start.numDaysFrom(end) - 1; // last day doesn't count

        // num of days to include each week starting from sunday
        int days_from_sunday = isSaturdayBusinessDay() ? getCgfWeekBusinessDays()+1 : getCgfWeekBusinessDays();
        int i =0;
        while (i < days_in_range) {
            int week_day = start.getWeekDay();
            switch (week_day) {
                case 1: // sunday
                    if (isSundayBusinessDay())
                        business_days.add(start);

                    for (int j = 1; j <= days_from_sunday; j++) {
                        start = start.plusDays(1);
                        business_days.add(start);
                        if (++i >= days_in_range)
                            break;
                    }
                    break;
                case 7: // saturday
                    if (isSaturdayBusinessDay())
                        business_days.add(start);
                    break;
                default:
                    business_days.add(start);
                    break;
            }
            start = start.plusDays(1);
            i++;
        }

        return business_days;
    }

    public static int getCfgTimezone() {
        return cfg_timezone;
    }

    public static void setTimezone(int cfg_timezone) {
        BusinessDaysCalendar.cfg_timezone = cfg_timezone;
    }

    public static String getCfgLocale() {
        return cfg_locale;
    }

    public static void setLocale(String cfg_locale) {
        BusinessDaysCalendar.cfg_locale = cfg_locale;
    }

    private static boolean isSaturdayBusinessDay() {
        return cfg_saturday_is_business_day;
    }

    public static void setSaturdayIsBusinessDay(boolean option) {
        BusinessDaysCalendar.cfg_saturday_is_business_day = option;
    }

    private static boolean isSundayBusinessDay() {
        return cfg_sunday_is_business_day;
    }

    public static void setSundayIsBusinessDay(boolean option) {
        BusinessDaysCalendar.cfg_sunday_is_business_day = option;
    }

    private static int getCgfWeekBusinessDays() {
        return cfg_week_business_days;
    }

    public static void setNumberOfWeekBusinessDays(int cfg_week_business_days) {
        BusinessDaysCalendar.cfg_week_business_days = cfg_week_business_days;
    }

}
