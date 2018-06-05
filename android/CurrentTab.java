package com.nellyoung.helloworld;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
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
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.LoggingBehavior;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.facebook.FacebookSdk.getApplicationContext;

public class CurrentTab  extends Fragment {
    private String[] indicators = {"price", "SMA", "EMA", "MACD", "RSI", "ADX", "CCI"};
    private String symbol;
    private JSONObject detailData;
    private JSONObject priceData;
    private HashMap<String, JSONObject> indicatorData = new HashMap<String, JSONObject>();

    private ScrollDisabledListView detailListView;
    private ChartWebView indicatorView;
    private Button changeButton;
    private Spinner indicatorList;
    private ImageButton emptyStar;
    private ImageButton filledStar;

    private CallbackManager callbackManager;
    private ShareDialog shareDialog;
    private ImageButton fbShare;

    private String chartURL = "http://export.highcharts.com/charts/chart.86d654da00364a149faa8601e133f62e.png";

    private SharedPreferences favoriteData;
    private SharedPreferences.Editor editor;

    private FrameLayout detailProgressBar;
    private FrameLayout indicatorChartProgressBar;

    //private CallbackManager facebookCallbackManager;
    //private ShareDialog facebookShareDialog;

    private String currentIndicator = "price";

    private RequestQueue requestQueue;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //FacebookSdk.sdkInitialize(FacebookSdk.getApplicationContext());
        //System.out.println("On Create!!!!!");
        final View rootView = inflater.inflate(R.layout.current_content, container, false);
        requestQueue = Volley.newRequestQueue(getActivity().getApplicationContext());

        fbShare = (ImageButton) rootView.findViewById(R.id.facebookShare);
        callbackManager = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);
        shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {
                Toast.makeText(getActivity().getApplicationContext(), "FB share success", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel() {
                Toast.makeText(getActivity().getApplicationContext(), "FB share canceled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(getActivity().getApplicationContext(), "FB share error", Toast.LENGTH_SHORT).show();
            }
        });

        DetailActivity detailActivity = (DetailActivity) getActivity();
        symbol = detailActivity.sendSymbol();

        detailProgressBar = (FrameLayout) rootView.findViewById(R.id.detailProgressBar);
        indicatorChartProgressBar = (FrameLayout) rootView.findViewById(R.id.indicatorChartProgressBar);

        emptyStar = (ImageButton) rootView.findViewById(R.id.emptyStar);
        filledStar = (ImageButton) rootView.findViewById(R.id.filledStar);

        // open favorite sharedPreference
        favoriteData = getActivity().getSharedPreferences("favorite", Context.MODE_PRIVATE);
        editor = favoriteData.edit();
        if(favoriteData.contains(symbol)){
            emptyStar.setVisibility(View.GONE);
            filledStar.setVisibility(View.VISIBLE);
        } else {
            filledStar.setVisibility(View.GONE);
            emptyStar.setVisibility(View.VISIBLE);
        }

        detailListView = (ScrollDisabledListView) rootView.findViewById(R.id.detailListView);

        changeButton = (Button) rootView.findViewById(R.id.changeIndicator);

        indicatorList = (Spinner) rootView.findViewById(R.id.indicatorList);
        indicatorList.setOverScrollMode(View.OVER_SCROLL_NEVER);
        ArrayAdapter indicatorAdapter = new ArrayAdapter<String>(getActivity().getApplicationContext(), R.layout.indicator_spinner, indicators);
        indicatorList.setAdapter(indicatorAdapter);

        indicatorList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                try{
                    Field field = AdapterView.class.getDeclaredField("mOldSelectedPosition");
                    field.setAccessible(true);
                    field.setInt(indicatorList, AdapterView.INVALID_POSITION);
                } catch (Exception e){
                    e.printStackTrace();
                }

                String nowIndicator = adapterView.getItemAtPosition(i).toString();
                //System.out.println("now: " + nowIndicator + " cur: " + currentIndicator);
                if(nowIndicator == currentIndicator){
                    changeButton.setEnabled(false);
                } else {
                    changeButton.setEnabled(true);
                    currentIndicator = nowIndicator;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        indicatorView = (ChartWebView) rootView.findViewById(R.id.indicatorView);
        indicatorView.getSettings().setJavaScriptEnabled(true);
        indicatorView.setWebChromeClient(new WebChromeClient());
        indicatorView.addJavascriptInterface(new AndroidtoJs(), "Android");
        indicatorView.loadUrl("file:///android_asset/www/drawIndicator.html");

        // retrive time series data
        String TimeURL = "http://stocksearch-env-lidanyang.us-west-1.elasticbeanstalk.com/TimeSeries?symbol="  + symbol;
        JsonObjectRequest timeSeriesRequest = new JsonObjectRequest(
                Request.Method.GET, TimeURL, null,
                new Response.Listener<JSONObject>() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onResponse(JSONObject response) {
                        //System.out.println(response.toString());
                        detailListView.setVisibility(View.VISIBLE);
                        detailProgressBar.setVisibility(View.GONE);
                        try {
                            detailData = response.getJSONObject("Statistic");
                            priceData = response.getJSONObject("JSONdata");
                            // draw price chart
                            indicatorView.loadUrl("javascript:drawPriceChart('" + symbol + "@" + priceData.toString() + "')");
                            indicatorView.loadUrl("javascript:getImageUrl()");
                            //System.out.println(priceData.toString());

                            final List<Map<String, String>> data =  detailFormatter(detailData);
                            System.out.println(data);
                            System.out.println(R.layout.detail_row);

                            SimpleAdapter detailAdapter = new SimpleAdapter(getActivity().getApplicationContext(), data, R.layout.detail_row,
                                    new String[] {"name", "value"},
                                    new int[] {R.id.detailNameView, R.id.detailDataView}){
                                @Override
                                public View getView(int position, View convertView, ViewGroup parent) {
                                    View view = super.getView(position, convertView, parent);
                                    ImageView up = (ImageView) view.findViewById(R.id.arrowUpView);
                                    ImageView down = (ImageView) view.findViewById(R.id.arrowDownView);

                                    if(position == 2){ // change
                                        try {
                                            float change = Float.parseFloat(detailData.getString("Change"));
                                            if(change < 0){
                                                up.setVisibility(View.GONE);
                                                down.setVisibility(View.VISIBLE);
                                            } else {
                                                down.setVisibility(View.GONE);
                                                up.setVisibility(View.VISIBLE);
                                            }
                                        } catch (JSONException e){
                                            e.printStackTrace();
                                        }
                                    }
                                    return view;
                                }
                            };
                            detailListView.setAdapter(detailAdapter);
                            indicatorView.setVisibility(View.VISIBLE);
                            indicatorChartProgressBar.setVisibility(View.GONE);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            detailListView.setVisibility(View.GONE);
                            detailProgressBar.setVisibility(View.GONE);
                            rootView.findViewById(R.id.detailFail).setVisibility(View.VISIBLE);
                            indicatorView.setVisibility(View.GONE);
                            indicatorChartProgressBar.setVisibility(View.GONE);
                            rootView.findViewById(R.id.indicatorChartFail).setVisibility(View.VISIBLE);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        timeSeriesRequest.setRetryPolicy(new DefaultRetryPolicy(15000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(timeSeriesRequest);

        // retrive indicator data
        for(int i=1; i<indicators.length; i++){
            final String indi = indicators[i];
            String IndiURL = "http://stocksearch-env-lidanyang.us-west-1.elasticbeanstalk.com/Indicator?symbol="  + symbol + "&indicator=" + indi;
            JsonObjectRequest indicatorRequest = new JsonObjectRequest(
                    Request.Method.GET, IndiURL, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            //System.out.println(response.toString());
                            indicatorData.put(indi, response);
                            if(currentIndicator == indi){
                                indicatorChartProgressBar.setVisibility(View.GONE);
                                indicatorView.setVisibility(View.VISIBLE);
                                // draw indicator chart
                                indicatorView.loadUrl("javascript:drawChart('" + symbol + "@" + currentIndicator + "@" + indicatorData.get(currentIndicator).toString() + "')");
                                indicatorView.loadUrl("javascript:getImageUrl()");
                            }
                            //System.out.println(indi + " :" + response.toString());
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            });
            indicatorRequest.setRetryPolicy(new DefaultRetryPolicy(15000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            requestQueue.add(indicatorRequest);
        }

        changeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                indicatorChartProgressBar.setVisibility(View.VISIBLE);
                indicatorView.setVisibility(View.GONE);
                if(currentIndicator == "price"){
                    // draw price chart
                    if(priceData != null){
                        indicatorChartProgressBar.setVisibility(View.GONE);
                        indicatorView.setVisibility(View.VISIBLE);
                        indicatorView.loadUrl("javascript:drawPriceChart('" + symbol + "@" + priceData.toString() + "')");
                    }
                } else {
                    if(indicatorData.containsKey(currentIndicator)){
                        indicatorChartProgressBar.setVisibility(View.GONE);
                        indicatorView.setVisibility(View.VISIBLE);
                        // draw indicator chart
                        indicatorView.loadUrl("javascript:drawChart('" + symbol + "@" + currentIndicator + "@" + indicatorData.get(currentIndicator).toString() + "')");
                    }
                }
                indicatorView.loadUrl("javascript:getImageUrl()");
                changeButton.setEnabled(false);
            }
        });

        emptyStar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(detailData != null){
                    try {
                        filledStar.setVisibility(View.VISIBLE);
                        emptyStar.setVisibility(View.GONE);
                        editor.putString(symbol, symbol);
                        int cnt = favoriteData.getInt("cnt", -1);
                        JSONObject tmp = new JSONObject();
                        // symbol Id
                        tmp.put("id", String.valueOf(cnt + 1));
                        // symbol Price
                        tmp.put("price", detailData.getString("Last Price"));
                        // symbol Change
                        tmp.put("change", detailData.getString("Change") + " (" + detailData.getString("Change Percent") + ")");
                        editor.putString(symbol, tmp.toString());
                        // change cnt for default sort
                        if(favoriteData.contains("cnt")){
                            editor.remove("cnt");
                        }
                        editor.putInt("cnt", cnt + 1);
                        // change all symbol index list
                        Set<String> currentSet = favoriteData.getStringSet("all", new HashSet<String>());
                        currentSet.add(symbol);
                        if(favoriteData.contains("all")){
                            editor.remove("all");
                        }
                        editor.putStringSet("all", currentSet);
                        editor.apply();
                    } catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            }
        });

        filledStar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                filledStar.setVisibility(View.GONE);
                emptyStar.setVisibility(View.VISIBLE);
                editor.remove(symbol);
                Set<String> currentSet = favoriteData.getStringSet("all", new HashSet<String>());
                currentSet.remove(symbol);
                editor.remove("all");
                editor.putStringSet("all", currentSet);
                editor.apply();
            }
        });

        fbShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShareLinkContent content = new ShareLinkContent.Builder()
                        .setContentUrl(Uri.parse(chartURL))
                        .build();
                shareDialog.show(content, ShareDialog.Mode.AUTOMATIC);
            }
        });

        return rootView;
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static List<Map<String, String>> detailFormatter(JSONObject js){
        List<Map<String, String>> map = new ArrayList<Map<String, String>>();
        try {
            Map<String, String> datum = new HashMap<String, String>(2);
            datum.put("name", "Stock Symbol");
            datum.put("value", js.getString("Stock Ticker Symbol"));
            map.add(datum);
            datum = new HashMap<String, String>(2);
            datum.put("name", "Last Price");
            datum.put("value", js.getString("Last Price").substring(1));
            map.add(datum);
            datum = new HashMap<String, String>(2);
            datum.put("name", "Change");
            datum.put("value", js.getString("Change") + " (" + js.getString("Change Percent") + ")");
            map.add(datum);
            datum = new HashMap<String, String>(2);
            datum.put("name", "Timestamp");
            datum.put("value", js.getString("Timestamp"));
            map.add(datum);
            datum = new HashMap<String, String>(2);
            datum.put("name", "Open");
            datum.put("value", js.getString("Open"));
            map.add(datum);
            datum = new HashMap<String, String>(2);
            datum.put("name", "Close");
            datum.put("value", js.getString("Close"));
            map.add(datum);
            datum = new HashMap<String, String>(2);
            datum.put("name", "Day's Range");
            datum.put("value", js.getString("Day's Range"));
            map.add(datum);
            datum = new HashMap<String, String>(2);
            datum.put("name", "Volume");
            String volume = "";
            String[] fracs = js.getString("Volume").split(",");
            for(int i = 0; i < fracs.length; i++){
                volume += fracs[i];
            }
            datum.put("value", volume);
            map.add(datum);
        } catch (JSONException e){
            e.printStackTrace();
        }
        return map;
    }

    public class AndroidtoJs extends Object {
        @JavascriptInterface
        public void getChartURL(String msg) {
            chartURL = msg;
            System.out.println("得到chart URL " + msg);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }
    }
}
