package com.nellyoung.helloworld;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.webkit.WebView;

/**
 * Created by sss on 2017/11/26.
 */

public class ChartWebView extends WebView {
    public ChartWebView(Context context) {
        this(context, null);
    }

    public ChartWebView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        /* any initialisation work here */
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN){
            onScrollChanged(getScrollX(), getScrollY(), getScrollX(), getScrollY());
        }
        return super.onTouchEvent(event);
    }

}
