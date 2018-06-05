package com.nellyoung.helloworld;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.awt.font.TextAttribute;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String[] sortMethod = new String[]{
            "Sort by", "Default", "Symbol", "Price", "Change"
    };
    private static final String[] orderMethod = new String[]{
            "Order", "Ascending", "Descending"
    };
    public static final String EXTRA_MESSAGE = "com.nellyoung.helloworld.MESSAGE";

    private RequestQueue requestQueue;
    private Button btnQuote;
    private Button btnClear;
    private ListView favoriteListView;
    private SimpleAdapter favoriteAdapter;
    private SharedPreferences favoriteData;
    private SharedPreferences.Editor editor;
    private List<Map<String, String>> favoriteDataList;
    private Button manualRefresh;
    private SwitchCompat AutoSwitch;
    private Handler autoRefreshHandler = new Handler();
    private Runnable autoRefreshRunnable;
    private MenuItem deleteTitle;
    private FrameLayout autoCompleteProgressBar;

    private String sortWay = "Default";
    private String orderWay = "Ascending";

    private String prepareToDelete;

    public Comparator<Map<String, String>> mapComparator = new Comparator<Map<String, String>>() {
        @Override
        public int compare(Map<String, String> m1, Map<String, String> m2) {
            int compareResult = 0;
            if(sortWay == "Default"){
                int id1 = Integer.parseInt(m1.get("id"));
                int id2 = Integer.parseInt(m2.get("id"));
                if(id1 < id2) compareResult = -1;
                if(id1 > id2) compareResult = 1;
            }
            if(sortWay == "Symbol"){
                compareResult = m1.get("symbol").compareTo(m2.get("symbol"));
            }
            if(sortWay == "Price"){
                float p1 = Float.parseFloat(m1.get("price").substring(1));
                float p2 = Float.parseFloat(m2.get("price").substring(1));
                if(p1 < p2) compareResult = -1;
                if(p1 > p2) compareResult = 1;
            }
            if(sortWay == "Change"){
                float c1 = Float.parseFloat(m1.get("change").split(" \\(")[0]);
                float c2 = Float.parseFloat(m2.get("change").split(" \\(")[0]);
                if(c1 < c2) compareResult = -1;
                if(c1 > c2) compareResult = 1;
            }
            if(orderWay == "Ascending")
                return compareResult;
            else return 0 - compareResult;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestQueue = Volley.newRequestQueue(this);

        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.nellyoung.helloworld",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }

        System.out.println("Main activity on create called!!!");

        manualRefresh = (Button) findViewById(R.id.ManualRefresh);
        AutoSwitch = (SwitchCompat) findViewById(R.id.AutoSwitch);

        autoCompleteProgressBar = (FrameLayout) findViewById(R.id.AutoCompleteProgressBar);

        favoriteData = getSharedPreferences("favorite", Context.MODE_PRIVATE);
        editor = favoriteData.edit();
        favoriteListView = (ListView) findViewById(R.id.favoriteListView);
        favoriteDataList =  favoriteFormatter();
        //System.out.println(data);
        //System.out.println(favoriteData.getAll());
        if(favoriteDataList != null) {
             favoriteAdapter = new SimpleAdapter(this, favoriteDataList, R.layout.favorite_row,
                    new String[]{"symbol", "price", "change"},
                    new int[]{R.id.favoriteSymbol, R.id.favoritePrice, R.id.favoriteChange}){
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView favoriteChange = (TextView) view.findViewById(R.id.favoriteChange);
                    HashMap<String, String> tmp = (HashMap<String, String>) this.getItem(position);
                    String changeString = tmp.get("change").split(" \\(")[0];
                    float change = Float.parseFloat(changeString);

                    if(change > 0){ // change
                        favoriteChange.setTextColor(Color.parseColor("#6B8E23"));
                    } else {
                        favoriteChange.setTextColor(Color.parseColor("#A52A2A"));
                    }
                    favoriteChange.setGravity(Gravity.CENTER);
                    TextView favoritePrice = (TextView) view.findViewById(R.id.favoritePrice);
                    favoritePrice.setGravity(Gravity.CENTER);
                    return view;
                }
            };
            favoriteListView.setAdapter(favoriteAdapter);
            favoriteListView.setVisibility(View.VISIBLE);
            favoriteListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    HashMap<String, String> tmp = (HashMap<String, String>) adapterView.getItemAtPosition(i);
                    String clickedSymbol = tmp.get("symbol");
                    Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                    intent.putExtra(EXTRA_MESSAGE, clickedSymbol);
                    startActivity(intent);
                    //Toast.makeText(MainActivity.this, clickedSymbol,Toast.LENGTH_SHORT).show();
                }
            });
            favoriteListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                    HashMap<String, String> tmp = (HashMap<String, String>) adapterView.getItemAtPosition(position);
                    String clickedSymbol = tmp.get("symbol");
                    prepareToDelete = clickedSymbol;
                    //showMenu(findViewById(R.id.mainLinearLayout));
                    PopupMenu popup = new PopupMenu(MainActivity.this, view);
                    MenuInflater inflater = getMenuInflater();
                    inflater.inflate(R.menu.delete_menu, popup.getMenu());
                    MenuItem title = popup.getMenu().findItem(R.id.deleteTitle);
                    title.setEnabled(false);

                    // This activity implements OnMenuItemClickListener
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            int id = menuItem.getItemId();
                            if(id == R.id.deleteYes) {
                                System.out.println("prepare to delete: " + prepareToDelete);
                                editor.remove(prepareToDelete);
                                Set<String> all_symbol = favoriteData.getStringSet("all", new HashSet<String>());
                                all_symbol.remove(prepareToDelete);
                                editor.putStringSet("all", all_symbol);
                                editor.apply();
                                System.out.println(favoriteData);
                                favoriteDataList.clear();
                                List<Map<String, String>> newFavoriteDataList = favoriteFormatter();
                                favoriteDataList.addAll(newFavoriteDataList);
                                //favoriteDataList = favoriteFormatter();
                                System.out.print("Refreshed: ");
                                System.out.println(favoriteDataList);
                                favoriteAdapter.notifyDataSetChanged();
                            }
                            return false;
                        }
                    });
                    popup.show();
                    return true;
                }
            });
        }
        ManualRefreshFunction(findViewById(R.id.mainLinearLayout));

        /*
        *  For Volley
        */
        btnQuote = (Button) findViewById(R.id.Quote);
        btnClear = (Button) findViewById(R.id.Clear);

        SwitchCompat autoRefreshStatusSwitch = (SwitchCompat) findViewById(R.id.AutoSwitch);

        AutoCompleteTextView atv_content = (AutoCompleteTextView) findViewById(R.id.atv_content);

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_dropdown_item_1line);
        atv_content.setAdapter(adapter);

        atv_content.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(final CharSequence s, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(final Editable editable) {
                adapter.clear();
                if(editable.toString().length() != 0){
                    autoCompleteProgressBar.setVisibility(View.VISIBLE);
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String item = editable.toString();
                        String realSymbol = item.split(" - ")[0];
                        String AutoURL = "http://stocksearch-env-lidanyang.us-west-1.elasticbeanstalk.com/AutoComplete?symbol="  + realSymbol;
                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                                Request.Method.GET, AutoURL, null,
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        //System.out.println(response.toString());
                                        ArrayList<String> data = autoCompleteFormatter(response);
                                        for(int i = 0; i < data.size(); i++){
                                            adapter.add(data.get(i));
                                        }
                                        adapter.getFilter().filter(editable, null);
                                        autoCompleteProgressBar.setVisibility(View.GONE);
                                        //btnQuote.setEnabled(true);
                                    }
                                }, new Response.ErrorListener() {
                                @Override
                                    public void onErrorResponse(VolleyError error) {
                                    }
                        });
                        requestQueue.add(jsonObjectRequest);
                        //autoCompleteProgressBar.setVisibility(View.VISIBLE);
                        //btnQuote.setEnabled(false);
                    }
                }).start();
            }
        });

        Spinner sortList = (Spinner) findViewById(R.id.SortList);
        ArrayAdapter sortAdapter = new ArrayAdapter<String>(MainActivity.this, R.layout.support_simple_spinner_dropdown_item, sortMethod){
            @Override
            public boolean isEnabled(int position){
                if(position == 0){
                    return false;
                }
                return true;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent){
                View mView = super.getDropDownView(position, convertView, parent);
                TextView mTextView = (TextView) mView;
                if(position == 0){
                    mTextView.setTextColor(Color.GRAY);
                } else {
                    mTextView.setTextColor(Color.BLACK);
                }
                return mView;
            }
        };
        sortList.setAdapter(sortAdapter);
        sortList.setVisibility(View.VISIBLE);
        sortList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String sort = adapterView.getItemAtPosition(i).toString();
                if(sort == "Sort by"){
                    sort = "Default";
                    return;
                }
                //Toast.makeText(MainActivity.this, sort, Toast.LENGTH_SHORT).show();
                sortWay = sort;
                //System.out.println("Spinner. Sort: " + sortWay + " order: " + orderWay );
                Collections.sort(favoriteDataList, mapComparator);
                favoriteAdapter.notifyDataSetChanged();
                //System.out.println(favoriteDataList);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        Spinner orderList = (Spinner) findViewById(R.id.OrderList);
        ArrayAdapter orderAdapter = new ArrayAdapter<String>(MainActivity.this, R.layout.support_simple_spinner_dropdown_item, orderMethod){
            @Override
            public boolean isEnabled(int position){
                if(position == 0){
                    return false;
                }
                return true;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent){
                View mView = super.getDropDownView(position, convertView, parent);
                TextView mTextView = (TextView) mView;
                if(position == 0){
                    mTextView.setTextColor(Color.GRAY);
                } else {
                    mTextView.setTextColor(Color.BLACK);
                }
                return mView;
            }
        };
        orderList.setAdapter(orderAdapter);
        orderList.setVisibility(View.VISIBLE);
        orderList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String order = adapterView.getItemAtPosition(i).toString();
                if(order == "Order"){
                    order = "Ascending";
                    return;
                }
                orderWay = order;
                //Toast.makeText(MainActivity.this, order, Toast.LENGTH_SHORT).show();
                //System.out.println("Spinner. Sort: " + sortWay + " order: " + orderWay );
                Collections.sort(favoriteDataList, mapComparator);
                favoriteAdapter.notifyDataSetChanged();
                //System.out.println(favoriteDataList);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        AutoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){ // ON
                    System.out.println("is checked!! now " + isChecked);
                    autoRefreshRunnable = new Runnable() {
                        @Override
                        public void run() {
                            //Toast.makeText(MainActivity.this, "Auto", Toast.LENGTH_SHORT).show();
                            ManualRefreshFunction(findViewById(R.id.mainLinearLayout));
                            autoRefreshHandler.postDelayed(autoRefreshRunnable, 10000);
                        }
                    };
                    autoRefreshHandler.postDelayed(autoRefreshRunnable, 10000);
                } else { // OFF
                    autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
                }
            }
        });
    }

    public void getQuote(View view){
        AutoCompleteTextView atv_content = (AutoCompleteTextView) findViewById(R.id.atv_content);
        String item = atv_content.getText().toString();
        String symbol = item.split(" - ")[0];
        if(symbol.length() == 0){
            //Toast.makeText(MainActivity.this, "Please enter a stock name or symbol",Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(MainActivity.this, DetailActivity.class);
            intent.putExtra(EXTRA_MESSAGE, symbol);
            startActivity(intent);
            //Toast.makeText(MainActivity.this, symbol,Toast.LENGTH_SHORT).show();
        }
    }

    public void clearInput(View view){
        AutoCompleteTextView atv_content = (AutoCompleteTextView) findViewById(R.id.atv_content);
        atv_content.setText("");
    }

    public static ArrayList<String> autoCompleteFormatter(JSONObject js){
        ArrayList<String> data = new ArrayList<String>();
        try {
            JSONArray ja = js.getJSONArray("JSONdata");
            for (int i = 0; i < ja.length(); i++) {
                JSONObject current = ja.getJSONObject(i);
                String Symbol = current.getString("Symbol");
                String Name = current.getString("Name");
                String Exchange = current.getString("Exchange");
                data.add(Symbol + " - " + Name + " (" + Exchange + ")");
            }
            //System.out.println(data);
        } catch (JSONException e){
            e.printStackTrace();
            data.add("AutoComplete API Error, Please Wait. Do not click.");
        }
        return data;
    }
    public List<Map<String, String>> favoriteFormatter(){
        System.out.println("favorite formatter is called!!!!!");
        List<Map<String, String>> map = new ArrayList<Map<String, String>>();
        Set<String> all_symbol = favoriteData.getStringSet("all", null);
        // no any symbol yet
        if(all_symbol == null){
            return null;
        }
        //System.out.println("all");
        Iterator<String> it = all_symbol.iterator();
        while(it.hasNext()){
            String sym = it.next();
            System.out.println(sym);
            System.out.println(favoriteData.getString(sym, null));
            try {
                JSONObject tmp = new JSONObject(favoriteData.getString(sym, null));
                //System.out.println(tmp.toString());
                Map<String, String> datum = new HashMap<String, String>(2);
                datum.put("symbol", sym);
                datum.put("price", tmp.getString("price"));
                datum.put("change", tmp.getString("change"));
                datum.put("id", tmp.getString("id"));
                map.add(datum);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        //System.out.print("Before: ");
        //System.out.println(map);
        Collections.sort(map, mapComparator);
        //System.out.print("After: ");
        //System.out.println(map);
        return map;
    }

    public void ManualRefreshFunction(View view){
        //Toast.makeText(MainActivity.this, "Mannual", Toast.LENGTH_SHORT).show();
        Set<String> allSymbol = favoriteData.getStringSet("all", new HashSet<String>());
        for (final String sym : allSymbol) {
            String TimeURL = "http://stocksearch-env-lidanyang.us-west-1.elasticbeanstalk.com/TimeSeries?symbol="  + sym;
            JsonObjectRequest timeSeriesRequest = new JsonObjectRequest(
                    Request.Method.GET, TimeURL, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            //System.out.println(response.toString());
                            try {
                                JSONObject tmp = new JSONObject(favoriteData.getString(sym, ""));
                                String price = response.getJSONObject("Statistic").getString("Last Price");
                                String change = response.getJSONObject("Statistic").getString("Change") + " (" + response.getJSONObject("Statistic").getString("Change Percent") + ")";
                                String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                                tmp.put("price", price.substring(1));
                                tmp.put("change", change );
                                editor.putString(sym, tmp.toString());
                                editor.apply();
                                favoriteDataList.clear();
                                List<Map<String, String>> newFavoriteDataList = favoriteFormatter();
                                favoriteDataList.addAll(newFavoriteDataList);
                                //favoriteDataList = favoriteFormatter();
                                System.out.print("Refreshed: ");
                                System.out.println(favoriteDataList);
                                favoriteAdapter.notifyDataSetChanged();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            });
            timeSeriesRequest.setRetryPolicy(new DefaultRetryPolicy(5000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            requestQueue.add(timeSeriesRequest);
        }
    }
}
