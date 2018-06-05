package com.nellyoung.helloworld;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class HistoricalTab extends Fragment {
    public String symbol;
    public JSONObject historyData;
    //private TextView textView;
    private View rootView;
    private ChartWebView historyView;
    private FrameLayout historicalProgressBar;

    private RequestQueue requestQueue;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        System.out.println("Historical On Create!!!!");
        final View rootView = inflater.inflate(R.layout.historical_content, container, false);
        requestQueue = Volley.newRequestQueue(getActivity().getApplicationContext());
        //textView = (TextView) rootView.findViewById(R.id.section_label);
        DetailActivity detailActivity = (DetailActivity) getActivity();
        symbol = detailActivity.sendSymbol();

        historicalProgressBar = (FrameLayout) rootView.findViewById(R.id.historicalProgressBar);

        historyView = (ChartWebView) rootView.findViewById(R.id.historyView);
        historyView.getSettings().setJavaScriptEnabled(true);
        historyView.setWebChromeClient(new WebChromeClient());
        historyView.loadUrl("file:///android_asset/www/drawHistory.html");
        //historyView.addJavascriptInterface(new JavaScriptInterface(getActivity().getApplicationContext()), "Android");

        // retrive historical data
        String HistoryURL = "http://stocksearch-env-lidanyang.us-west-1.elasticbeanstalk.com/History?symbol=" + symbol;
        JsonObjectRequest historicalRequest = new JsonObjectRequest(
                Request.Method.GET, HistoryURL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //System.out.println(response.toString());
                        try {
                            //System.out.println(response.getString("JSONdata"));
                            if(response.getString("JSONdata").equals("APIError")){
                                System.out.println("Historical Fail");
                                historicalProgressBar.setVisibility(View.GONE);
                                historyView.setVisibility(View.GONE);
                                rootView.findViewById(R.id.historicalFail).setVisibility(View.VISIBLE);
                                return;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            historicalProgressBar.setVisibility(View.GONE);
                            historyView.setVisibility(View.GONE);
                            rootView.findViewById(R.id.historicalFail).setVisibility(View.VISIBLE);
                            return;
                        }
                        historyData = response;
                        historyView.setVisibility(View.VISIBLE);
                        historicalProgressBar.setVisibility(View.GONE);
                        //System.out.println(historyData.toString());
                        historyView.loadUrl("javascript:drawChart('" + symbol + "@" + historyData.toString() + "')");
                        /*
                        historyView.setWebViewClient(new WebViewClient(){
                            public void onPageFinished(WebView view, String url){
                                historyView.loadUrl("javascript:drawChart('" + symbol + "@" + historyData.toString() + "')");
                            }
                        });*/
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        historicalRequest.setRetryPolicy(new DefaultRetryPolicy(5000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(historicalRequest);

        return rootView;
    }

    public class JavaScriptInterface {
        private Context mContext;
        public JavaScriptInterface(Context c){
            mContext = c;
        }
        @android.webkit.JavascriptInterface
        public void showInfoFromJs(String toast){
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
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
