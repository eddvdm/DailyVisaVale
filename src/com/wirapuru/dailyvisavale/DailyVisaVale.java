package com.wirapuru.dailyvisavale;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.json.JSONArray;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

public class DailyVisaVale extends Activity
{
    ProgressDialog dialog = null;
    CardData card = new CardData();
    
    public static final String PREFS_CARD_NUMBERS = "card_numbers";
    public static final String PREF_CARD_NUMBERS_JSON = "json";
    public static final String PREFS_CURRENT_CARD_NUMBER = "current_card_number";
    public static final String PREF_CURRENT_CARD_NUMBER = "card_number";
    public static final String PREFS_LAST_CARD_NUMBER = "last_card_number";
    public static final String PREF_LAST_CARD_NUMBER = "card_number";

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
        rememberLastCard();
        buildCardNumberAutoComplete();
    }

    /**
     * Called onClick event on main button at main interface.
     * Validates, saves and sets card number.
     *
     * @param view
     */
    public void inputCardNumber(View view) {
        AutoCompleteTextView tx_card_number = (AutoCompleteTextView) findViewById(R.id.card_number);
        String str_card_number = tx_card_number.getText().toString();

        if (cardNumberIsValid(str_card_number)) {
            saveCardNumber(str_card_number);
            setCurrentCardNumber(str_card_number);
            buildCardNumberAutoComplete();
            logCardNumbers();

            try {
                card.setAndLoad(str_card_number);
                card.fetchRemoteData();
            } catch (Exception e) {
                e.printStackTrace();
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
    
    public boolean rememberLastCard() {
        final SharedPreferences prefs_last_card_number = getSharedPreferences(PREFS_LAST_CARD_NUMBER, 0);

        if (prefs_last_card_number.contains(PREF_LAST_CARD_NUMBER)) {
            final AutoCompleteTextView txt_card_number = (AutoCompleteTextView) findViewById(R.id.card_number);
            txt_card_number.setText(prefs_last_card_number.getString(PREF_LAST_CARD_NUMBER, null));
            return true;
        }

        return false;
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
                        throw new CardNumberExistsException();
                    for (String card_number : card_numbers)
                        json_array.put(card_number);
                }
            }
            json_array.put(str_card_number);

            SharedPreferences.Editor prefs_editor = prefs_card_numbers.edit();
            prefs_editor.clear();
            prefs_editor.putString(PREF_CARD_NUMBERS_JSON, json_array.toString());
            prefs_editor.commit();

            // set last card number
            saveLastCardNumber(str_card_number);

        } catch (CardNumberExistsException e) {
            saveLastCardNumber(str_card_number);    
        } catch (Exception e) {
            Toast toast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
            toast.show();
        }
    }
    
    public void saveLastCardNumber(String str_card_number) {
        final SharedPreferences prefs_last_card_number = getSharedPreferences(PREFS_LAST_CARD_NUMBER, 0);
        SharedPreferences.Editor prefs_editor = prefs_last_card_number.edit();
        prefs_editor.clear();
        prefs_editor.putString(PREF_LAST_CARD_NUMBER, str_card_number);
        prefs_editor.commit();        
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

    public void hideSoftKeyboard() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public class CardData {

        public static final String PREFS_CARD_PREFIX = "card__";
        public static final String PREF_CARD_JSON_DATA = "json_data";

        public static final String DATA_BALANCE = "balance";
        public static final String DATA_LAST_DATE = "last_date";

        protected String card_number;
        private final HashMap<String, Object> raw_data = new HashMap<String, Object>();

        public CardData() {}
        
        public void setAndLoad(String card_number) throws Exception{
            if (!numberIsValid(card_number))
                throw new Exception("Invalid card number: '"+card_number+"'.");

            this.setCardNumber(card_number);
//            this.loadData();            
        }

//        private void loadData() {
//            final SharedPreferences prefs_data = getSharedPreferences(PREFS_CARD_PREFIX+this.getCardNumber(), 0);
//
//            try {
//                JSONArray json_data = prefs_data.contains(PREF_CARD_JSON_DATA) ?
//                        new JSONArray(prefs_data.getString(PREF_CARD_NUMBERS_JSON, null)) :
//                            new JSONArray();
//
//            } catch (Exception e) {
//                Toast toast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
//                toast.show();
//            }
//        }

        public void fetchRemoteData() {

            try {
                dialog = new ProgressDialog(DailyVisaVale.this);
                dialog.setTitle("Coletando dados");
                dialog.setMessage("Por favor, aguarde...");
                dialog.show();

                HashMap<String, Object> to_fetch = new HashMap<String, Object>();

                HashMap<String, String> hashMap = new HashMap<String, String>();
                hashMap.put("//td[@class='corUm fontWeightDois'][2]", URL_TO_FETCH_CARD_DATA+this.getCardNumber());
                to_fetch.put(DATA_BALANCE, hashMap);

                hashMap = new HashMap<String, String>();
                hashMap.put("//table[@class='consulta'][1]/tbody/tr[@class='rowTable'][3]/td[@class='corUm'][1]", URL_TO_FETCH_CARD_DATA+this.getCardNumber());
                to_fetch.put(DATA_LAST_DATE, hashMap);

                RemoteParsedData task = new RemoteParsedData();
                task.execute(to_fetch);

            } catch (Exception e) {
                e.printStackTrace();
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

        final public Float extractMoneyValue(String value) {
            return Float.parseFloat(value.split(" ")[1].replace(".","").replace(",","."));
        }
    }

    private class RemoteParsedData extends AsyncTask<HashMap<String, Object>, Void, HashMap<String, StringBuilder>> {
        
        public final HashMap<URL, TagNode> tagnode_cache = new HashMap<URL, TagNode>();

        @Override
        protected HashMap<String, StringBuilder> doInBackground(HashMap<String, Object>... to_fetch) {

            // parser from htmlcleaner
            HtmlCleaner pageParser = new HtmlCleaner();
            CleanerProperties props = pageParser.getProperties();
            props.setAllowHtmlInsideAttributes(true);
            props.setAllowMultiWordAttributes(true);
            props.setRecognizeUnicodeChars(true);
            props.setOmitComments(true);

            // return values
            HashMap<String, StringBuilder> parsed_values = new HashMap<String, StringBuilder>();
            
            for (String data_name_key : to_fetch[0].keySet()) {
                HashMap<String, String> hashMap = (HashMap<String, String>)to_fetch[0].get(data_name_key);
                for (String key : hashMap.keySet()) {
                    String[] strings = {key, hashMap.get(key)};
                    parsed_values.put(data_name_key, new StringBuilder());

                    try {
                        URL url = new URL(strings[1]);

                        // process xpath_expression due a possible bug in API 7 (following-sibling)
                        String xpath_expression_modified = strings[0];
                        String[] xpath_expression_parts = xpath_expression_modified.split("---");
                        String xpath_expression = xpath_expression_parts[0];
                        Integer nodes_ahead = xpath_expression_parts.length > 1 ? Integer.parseInt(xpath_expression_parts[1]) : 0;

                        TagNode node = tagnode_cache.containsKey(url) ?
                                tagnode_cache.get(url) :
                                pageParser.clean(new InputStreamReader(url.openConnection().getInputStream()));

                        try {
                            Object[] td_nodes = node.evaluateXPath(xpath_expression);
                            for (int i =0; i < td_nodes.length; i++) {
                                if (parsed_values.get(data_name_key).length() == 0) {
                                    List children = ((TagNode)td_nodes[i]).getChildren();
                                    if (children.size() > 0) {
                                        parsed_values.get(data_name_key).append(((TagNode)td_nodes[i+nodes_ahead]).getChildren().get(0).toString());
                                        Log.v(LOG_TAG_ALL, "Found value: "+parsed_values);
                                    }
                                } else {
                                    break;
                                }
                            }
                        } catch (XPatherException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            return parsed_values;
        }

        @Override
        public void onPostExecute(HashMap<String, StringBuilder> result) {

            for (String key : result.keySet()) {
                card.raw_data.put(key, result.get(key));
            }              

            Toast toast = Toast.makeText(getApplicationContext(), card.raw_data.toString(), Toast.LENGTH_LONG);
            toast.show();

            dialog.dismiss();
        }
    }

    public class CardNumberExistsException extends Exception {
               
    }
}