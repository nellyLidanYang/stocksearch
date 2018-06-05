package com.nellyoung.helloworld;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewsTab extends Fragment{
    public String symbol;
    public JSONObject newsData;
    private ListView newsList;

    private boolean isValidSymbol = true;

    private RequestQueue requestQueue;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.news_content, container, false);
        requestQueue = Volley.newRequestQueue(getActivity().getApplicationContext());
        newsList = (ListView) rootView.findViewById(R.id.newsListView);

        DetailActivity detailActivity = (DetailActivity) getActivity();
        symbol = detailActivity.sendSymbol();

        String AutoURL = "http://stocksearch-env-lidanyang.us-west-1.elasticbeanstalk.com/AutoComplete?symbol="  + symbol;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET, AutoURL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //System.out.println(response.toString());
                        try {
                            if(response.getJSONArray("JSONdata").length() == 0){
                                rootView.findViewById(R.id.newsFail).setVisibility(View.VISIBLE);
                                newsList.setVisibility(View.GONE);
                                isValidSymbol = false;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            rootView.findViewById(R.id.newsFail).setVisibility(View.VISIBLE);
                            newsList.setVisibility(View.GONE);
                            isValidSymbol = false;
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        requestQueue.add(jsonObjectRequest);

        if(isValidSymbol) {
            // retrive news data
            AutoURL = "http://stocksearch-env-lidanyang.us-west-1.elasticbeanstalk.com/NewsFeed?symbol=" + symbol;
            JsonObjectRequest newsRequest = new JsonObjectRequest(
                    Request.Method.GET, AutoURL, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            newsData = response;
                            rootView.findViewById(R.id.newsFail).setVisibility(View.GONE);
                            newsList.setVisibility(View.VISIBLE);
                            final List<Map<String, String>> data = newsFormatter(newsData);

                            System.out.println(getActivity().getApplicationContext());
                            System.out.println(data);
                            SimpleAdapter titleAdapter = new SimpleAdapter(getActivity().getApplicationContext(), data, R.layout.list_row,
                                    new String[]{"title", "author", "pubDate"},
                                    new int[]{R.id.titleView, R.id.authorView, R.id.pubDateView});
                            newsList.setAdapter(titleAdapter);
                            newsList.setVisibility(View.VISIBLE);
                            newsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                    String url = data.get(i).get("link");
                                    Uri uri = Uri.parse(url);
                                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                    startActivity(intent);
                                }
                            });
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            });
            newsRequest.setRetryPolicy(new DefaultRetryPolicy(5000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            requestQueue.add(newsRequest);
        }
        return rootView;
    }
    public static List<Map<String, String>> newsFormatter(JSONObject js){
        List<Map<String, String>> map = new ArrayList<Map<String, String>>();
        try {
            JSONArray ja = js.getJSONArray("JSONdata");
            for (int i = 0; i < ja.length(); i++) {
                JSONObject current = ja.getJSONObject(i);
                String t = current.getString("title");
                String a = "Author: " + current.getString("author");
                String d = "Data: " + current.getString("pubDate");
                Map<String, String> datum = new HashMap<String, String>(2);
                datum.put("title", t);
                datum.put("author", a);
                datum.put("pubDate", d);
                datum.put("link", current.getString("link"));
                map.add(datum);
            }
        } catch (JSONException e){
            e.printStackTrace();
        }
        return map;
    }
    @Override
    public void onStop() {
        super.onStop();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }
    }
}
