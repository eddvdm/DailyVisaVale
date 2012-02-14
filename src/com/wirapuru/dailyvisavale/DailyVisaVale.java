package com.wirapuru.dailyvisavale;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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
import java.util.HashMap;
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
    private UI ui = new UI();
    
    private static Integer active_dialog_id;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // autocomplete past typed card numbers
//        clearTypedCardNumbers();
        Integer wait_dialog_id = this.ui.startWaiting();
        buildCardNumberAutoComplete();
        ui.stopWaiting(wait_dialog_id);
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
        Integer wait_dialog_id = this.ui.startWaiting();

        if (wait_dialog_id > 0) {// && cardNumberIsValid(str_card_number)) {
            this.active_dialog_id = wait_dialog_id;
//            hideSoftKeyboard(); // todo make hiding keyboard to work

            saveCardNumber(str_card_number);
            setCurrentCardNumber(str_card_number);
            buildCardNumberAutoComplete();
            logCardNumbers();

            try {
//Thread.sleep(10000);
                CardData card = new CardData(str_card_number);
                card.fetchRemoteData();
//                this.ui.stopWaiting(wait_dialog_id);
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

    public void hideSoftKeyboard() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public class CardData {

        public static final String PREFS_CARD_PREFIX = "card__";
        public static final String PREF_CARD_JSON_DATA = "json_data";
        
        public static final String REQUIRED_SUBSTR_BALANCE = "Saldo dispon";
        public static final String REQUIRED_SUBSTR_LAST_DATE = "ltima disponibiliza";

        public static final String DATA_BALANCE = "balance";
        public static final String DATA_LAST_DATE = "last_date";

        protected String card_number;
        protected final HashMap<String,Object> raw_data = new HashMap<String, Object>();

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

//                this.raw_data = Util.JSONArrayToList(json_data);
                
            } catch (Exception e) {
                Toast toast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
                toast.show();
            }
        }

        public void fetchRemoteData() {

            try {
                ProgressDialog dialog = new ProgressDialog(DailyVisaVale.this);
                dialog.setTitle("Coletando dados");
                dialog.setMessage("Por favor, aguarde...");

                HashMap<String, String> to_fetch = new HashMap<String, String>();
                
                to_fetch.put("//td[@class='corUm fontWeightDois']", URL_TO_FETCH_CARD_DATA+this.getCardNumber());
                to_fetch.put("//td[@class='corUm fontWeightDois']---1", URL_TO_FETCH_CARD_DATA+this.getCardNumber());

                RemoteParsedData task = new RemoteParsedData();
                task.execute(to_fetch);


//                HtmlCleaner pageParser = new HtmlCleaner();
//                CleanerProperties props = pageParser.getProperties();
//                props.setAllowHtmlInsideAttributes(true);
//                props.setAllowMultiWordAttributes(true);
//                props.setRecognizeUnicodeChars(true);
//                props.setOmitComments(true);
//
//                URL url = new URL(URL_TO_FETCH_CARD_DATA+this.getCardNumber());
//                URLConnection conn = url.openConnection();
//                TagNode node = pageParser.clean(new InputStreamReader(conn.getInputStream()));
//
//            String xpath_expression = "//td"; //[@class='corUm fontWeightDois']";
//
//                try {
//                    Object[] td_nodes = node.evaluateXPath(xpath_expression);
//
//                    StringBuilder found_balance = new StringBuilder();
//                    StringBuilder found_last_date = new StringBuilder();
//
//                    for (int i =0; i < td_nodes.length; i++) {
//
//                        // balance
//                        if (found_balance.length() == 0) {
//                            List children = ((TagNode)td_nodes[i]).getChildren();
//                            if (children.size() > 0) {
//                                String str = children.get(0).toString();
//
//                                if (str.contains(REQUIRED_SUBSTR_BALANCE)) {
//                                    found_balance.append(((TagNode)td_nodes[i+1]).getChildren().get(0).toString());
//                                    Log.v(LOG_TAG_ALL, "Found balance next to td: "+str);
//                                }
//                            }
//                        } else {
//                            Float float_balance = extractMoneyValue(found_balance.toString());
//                            Log.v(LOG_TAG_ALL, "Found balance: "+float_balance.toString());
//                            Toast toast = Toast.makeText(getApplicationContext(), "Found balance: "+float_balance.toString(), Toast.LENGTH_LONG);
//                            toast.show();
//                            this.raw_data.put(RAW_DATA_BALANCE, float_balance.toString());
//                        }
//
//                        // last date
//                        if (found_last_date.length() == 0) {
//                            List children = ((TagNode)td_nodes[i]).getChildren();
//                            if (children.size() > 0) {
//                                String str = children.get(0).toString();
//
//                                if (str.contains(REQUIRED_SUBSTR_LAST_DATE)) {
//                                    found_last_date.append(((TagNode)td_nodes[i+1]).getChildren().get(0).toString());
//                                    Log.v(LOG_TAG_ALL, "Found last date next to td: "+str);
//                                }
//                            }
//                        } else {
//                            Log.v(LOG_TAG_ALL, "Found last date: "+found_last_date.toString());
//                            Toast toast = Toast.makeText(getApplicationContext(), "Found last date: "+found_last_date.toString(), Toast.LENGTH_LONG);
//                            toast.show();
//                            this.raw_data.put(RAW_DATA_LAST_DATE, found_last_date.toString());
//                        }
//                    }
//
//                } catch (XPatherException e) {
//                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//                }

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

    private class RemoteParsedData extends AsyncTask<HashMap<String, String>, Void, HashMap<String, StringBuilder>> {
        
        public final HashMap<URL, TagNode> tagnode_cache = new HashMap<URL, TagNode>();

        @Override
        protected HashMap<String, StringBuilder> doInBackground(HashMap<String, String>... to_fetch) {
            
            HashMap<String, StringBuilder> parsed_values = new HashMap<String, StringBuilder>();
            
            for (String key : to_fetch[0].keySet()) {
                String[] strings = {key, to_fetch[0].get(key)};
                parsed_values.put(key, new StringBuilder());

                try {
                    URL url = new URL(strings[1]);

                    // process xpath_expression due a possible bug in API 7 (following-sibling)
                    String xpath_expression_modified = strings[0];
                    String[] xpath_expression_parts = xpath_expression_modified.split("---");
                    String xpath_expression = xpath_expression_parts[0];
                    Integer nodes_ahead = xpath_expression_parts.length > 1 ? Integer.parseInt(xpath_expression_parts[1]) : 0;

                    // parse
                    HtmlCleaner pageParser = new HtmlCleaner();
                    CleanerProperties props = pageParser.getProperties();
                    props.setAllowHtmlInsideAttributes(true);
                    props.setAllowMultiWordAttributes(true);
                    props.setRecognizeUnicodeChars(true);
                    props.setOmitComments(true);

                    TagNode node = tagnode_cache.containsKey(url) ?
                            tagnode_cache.get(url) :
                            pageParser.clean(new InputStreamReader(url.openConnection().getInputStream()));

                    try {
                        Object[] td_nodes = node.evaluateXPath(xpath_expression);
                        for (int i =0; i < td_nodes.length; i++) {
                            if (parsed_values.get(key).length() == 0) {
                                List children = ((TagNode)td_nodes[i]).getChildren();
                                if (children.size() > 0) {
                                    parsed_values.get(key).append(((TagNode)td_nodes[i+nodes_ahead]).getChildren().get(0).toString());
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
            
            return parsed_values;
        }

        @Override
        public void onPostExecute(HashMap<String, StringBuilder> result) {

            for (String key : result.keySet()) {
                Toast toast = Toast.makeText(getApplicationContext(), result.get(key), Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }


    public class UI {
        
        private final static String MSG_DEFAULT_WAIT = "Please wait..";
        
        private final static String LAYER_DIALOG = "dialog";
        private final static String LAYER_TOAST = "toast";
        
        private Integer element_internal_id = 0;
        
        private HashMap<String, Object> layers = new HashMap<String, Object>();
        private HashMap<String, Object> layers_queue = new HashMap<String, Object>();

        private HashMap<Integer, Object> elements = new HashMap<Integer, Object>();        

        public UI() {
            setLayersIdle();
        }
        
        private void setLayersIdle() {
            this.layers.put(LAYER_DIALOG, null);
            this.layers.put(LAYER_TOAST, null);
        }

        public Integer startWaiting() {
            return startWaiting("", MSG_DEFAULT_WAIT);
        }

        public Integer startWaiting(String msg) {
            return startWaiting("", msg);
        }

        public Integer startWaiting(String dialog_title, String dialog_msg) {
            
            ProgressDialog dialog = new ProgressDialog(DailyVisaVale.this);
            dialog.setTitle(dialog_title);
            dialog.setMessage(dialog_msg);

            Integer element_internal_id = getNewElementId();
            this.elements.put(element_internal_id, dialog);
            
            if (throwAtLayer(LAYER_DIALOG, element_internal_id)) {
                dialog.show();
            }
            return element_internal_id;
        }
        
        public void stopWaiting(Integer element_id) {
            ProgressDialog dialog = (ProgressDialog) this.elements.get(element_id);
            
            if (this.layers.get(LAYER_DIALOG) == dialog) {
                if (dialog.isShowing())
                    dialog.dismiss();
                
                idleLayer(LAYER_DIALOG);
            } // todo implement queue management
            
            this.elements.remove(element_id);
        }
        
        private boolean layerIsBusy(String layer) {
            return this.layers.get(layer) != null;
        }
        
        private boolean throwAtLayer(String layer, Integer element_internal_id) {
            if (!layerIsBusy(layer)) {
                Object obj = this.elements.get(element_internal_id);
                this.layers.put(layer, obj);
                return true;
            } // todo implement queue management
            
            return false;
        }
        
        private void idleLayer(String layer) {
            if (this.layers.containsKey(layer)) {
                this.layers.remove(layer);
            }
            this.layers.put(layer, null);
        }
        
        private Integer getNewElementId() {
            return this.element_internal_id++;
        }
        
    }


    
    
}
