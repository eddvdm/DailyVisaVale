package com.wirapuru.dailyvisavale;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class DailyVisaVale extends Activity
{
    public static final String PREFS_CARD_NUMBERS = "card_numbers";
    public static final String PREF_CARD_NUMBERS_JSON = "json";
    public static final String PREFS_CURRENT_CARD_NUMBER = "current_card_number";
    public static final String PREF_CURRENT_CARD_NUMBER = "card_number";
    
    public static final String LOG_TAG_ALL = "dvv";
    
    public static final String URL_TO_FETCH_CARD_DATA = "http://www.cbss.com.br/inst/convivencia/SaldoExtrato.jsp?numeroCartao="; 

    protected String current_card_number = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // autocomplete past typed card numbers
//        clearTypedCardNumbers();
        buildCardNumberAutoComplete();
    }

    /**
     * Called onClick event on main button at main interface.
     * Validates, saves and sets card number.
     *
     * @param view
     */
    public void inputCardNumber(View view) {
        final AutoCompleteTextView tx_card_number = (AutoCompleteTextView) findViewById(R.id.card_number);
        String str_card_number = tx_card_number.getText().toString();

        if (cardNumberIsValid(str_card_number)) {
            Toast toast = Toast.makeText(getApplicationContext(), "Saving card number: "+str_card_number, Toast.LENGTH_SHORT);
            toast.show();

            saveCardNumber(str_card_number);
            setCurrentCardNumber(str_card_number);
            buildCardNumberAutoComplete();
            logCardNumbers();

            try {
                CardData card = new CardData(str_card_number);
                card.fetchRemoteData();
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "Invalid card number: "+str_card_number, Toast.LENGTH_SHORT);
            toast.show();
        }
    }
    
    
    public void setCurrentCardNumber(String card_number) {
        this.current_card_number = card_number;
        saveCurrentCardNumber(card_number);
    }
    
    public boolean cardNumberIsValid(String card_number) {
        return card_number.length() == 16;
    }
    
    public void buildCardNumberAutoComplete() {
        List<String> card_numbers_list = Util.JSONArrayToList(getTypedCardNumbers());
        
        if (card_numbers_list.size() > 0) {
            final AutoCompleteTextView txt_card_number = (AutoCompleteTextView) findViewById(R.id.card_number);
            ArrayAdapter<String> adapter =  new ArrayAdapter<String>(this, R.layout.card_number_item, card_numbers_list);
            txt_card_number.setThreshold(0);

            txt_card_number.setAdapter(adapter);
        }
    }


    public void saveCardNumber(String str_card_number) {
        try {
            final SharedPreferences prefs_card_numbers = getSharedPreferences(PREFS_CARD_NUMBERS, 0);

            JSONArray json_array = new JSONArray();
            if (prefs_card_numbers.contains(PREF_CARD_NUMBERS_JSON)) {
                final String json_string = prefs_card_numbers.getString(PREF_CARD_NUMBERS_JSON, null);
                if (json_string != null) {
                    List<String> card_numbers = Util.JSONArrayToList(new JSONArray(json_string));
                    if (card_numbers.contains(str_card_number))
                        return;
                    for (String card_number : card_numbers)
                        json_array.put(card_number);
                }
            }
            json_array.put(str_card_number);

            SharedPreferences.Editor prefs_editor = prefs_card_numbers.edit();
            prefs_editor.clear();
            prefs_editor.putString(PREF_CARD_NUMBERS_JSON, json_array.toString());
            prefs_editor.commit();

            // set last card number todo
            //final SharedPreferences prefs_last = getSharedPreferences(PREFS_LAST_CARD_NUMBER, 0);
            //prefs_last.

        } catch (Exception e) {
            Toast toast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
            toast.show();
        }
    }


    public void saveCurrentCardNumber(String str_card_number) {
        try {
            final SharedPreferences prefs_current_card_number = getSharedPreferences(PREFS_CURRENT_CARD_NUMBER, 0);

            SharedPreferences.Editor prefs_editor = prefs_current_card_number.edit();
            prefs_editor.clear();
            prefs_editor.putString(PREF_CURRENT_CARD_NUMBER, str_card_number);
            prefs_editor.commit();

        } catch (Exception e) {
            Toast toast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
            toast.show();
        }
    }


    public void logCardNumbers() {
        try {
            String json_string = getTypedCardNumbers().toString();
            String to_log = json_string.length() > 0 ? "Stored card numbers: "+json_string : "Empty card numbers.";
            Log.i(LOG_TAG_ALL, to_log);
        } catch (Exception e) {
            Log.w(LOG_TAG_ALL, e.getMessage());
        }
    }

    public JSONArray getTypedCardNumbers() {
        try {
            SharedPreferences prefs_card_numbers = getSharedPreferences(PREFS_CARD_NUMBERS, 0);

            return prefs_card_numbers.contains(PREF_CARD_NUMBERS_JSON) ?
                new JSONArray(prefs_card_numbers.getString(PREF_CARD_NUMBERS_JSON, null)) :
                    new JSONArray();

        } catch (Exception e) {
            Log.w(LOG_TAG_ALL, "Cannot get card numbers: " + e.getMessage());
            return new JSONArray();
        }
    }

    public void clearTypedCardNumbers() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_CARD_NUMBERS,0).edit();
        editor.clear();
        editor.commit();
    }

    public class CardData {

        public static final String PREFS_CARD_PREFIX = "card__";
        public static final String PREF_CARD_JSON_DATA = "json_data";
        
        public static final String REQUIRED_SUBSTR_BALANCE = "Saldo dispon";
        public static final String MONEY_VALUE_SUBSTR = "R$";

        private String card_number;
        private List<String> raw_data;

        public CardData(String card_number) throws Exception{
            if (!numberIsValid(card_number))
                throw new Exception("Invalida card number: '"+card_number+"'.");

            this.setCardNumber(card_number);
            this.loadData();
        }

        private void loadData() {
            final SharedPreferences prefs_data = getSharedPreferences(PREFS_CARD_PREFIX+this.getCardNumber(), 0);

            try {
                JSONArray json_data = prefs_data.contains(PREF_CARD_JSON_DATA) ?
                        new JSONArray(prefs_data.getString(PREF_CARD_NUMBERS_JSON, null)) :
                            new JSONArray();

                this.raw_data = Util.JSONArrayToList(json_data);
                
            } catch (Exception e) {
                Toast toast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
                toast.show();
            }
        }

        public void fetchRemoteData() {

            try {
                HtmlCleaner pageParser = new HtmlCleaner();
                CleanerProperties props = pageParser.getProperties();
                props.setAllowHtmlInsideAttributes(true);
                props.setAllowMultiWordAttributes(true);
                props.setRecognizeUnicodeChars(true);
                props.setOmitComments(true);

                URL url = new URL(URL_TO_FETCH_CARD_DATA+this.getCardNumber());
                URLConnection conn = url.openConnection();
                TagNode node = pageParser.clean(new InputStreamReader(conn.getInputStream()));

                String xpath_expression = "//td[@class='corUm fontWeightDois']";

                try {
                    Object[] td_nodes = node.evaluateXPath(xpath_expression);

                    StringBuilder found_balance = new StringBuilder();
                    for (int i =0; i < td_nodes.length; i++) {
                        if (found_balance.length() == 0) {
                            List children = ((TagNode)td_nodes[i]).getChildren();
                            if (children.size() > 0) {
                                String str = children.get(0).toString();

                                if (str.contains(REQUIRED_SUBSTR_BALANCE)) {
                                    found_balance.append(((TagNode)td_nodes[i+1]).getChildren().get(0).toString());
                                    Log.v(LOG_TAG_ALL, "Found balance next to td: "+str);
                                } else {
                                    Log.v(LOG_TAG_ALL, "Not found balance in td: "+str);
                                }
                            }
                        } else {
                            Log.v(LOG_TAG_ALL, "Found balance: "+found_balance);
                            Toast toast = Toast.makeText(getApplicationContext(), "Found balance: "+found_balance, Toast.LENGTH_LONG);
                            toast.show();
                            break;
                        }
                    }

                } catch (XPatherException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }

            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }


        }
        
        public String getCardNumber() {
            return card_number;
        }

        public void setCardNumber(String card_number) {
            this.card_number = card_number;
        }
        
        final public boolean numberIsValid(String card_number) {
            return card_number.length() == 16;
        }

//        final public float extractMoneyValue(String value) {
//
//
//        }
    }

    
    
}
