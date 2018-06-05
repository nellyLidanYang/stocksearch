package com.nellyoung.helloworld;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.Spinner;

/**
 * Created by sss on 2017/11/28.
 */

public class SpinnerTrigger extends android.support.v7.widget.AppCompatSpinner {
    OnItemSelectedListener listener;
    int prevPos = -1;
    public SpinnerTrigger(Context context){
        super(context);
    }
    public SpinnerTrigger(Context context, AttributeSet attrs){
        super(context, attrs);
    }
    public SpinnerTrigger(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
    }

    @Override
    public void setSelection(int position){
        super.setSelection(position);
        if(position == getSelectedItemPosition() && prevPos == position){
            getOnItemSelectedListener().onItemSelected(null, null, position, 0);
        }
        prevPos = position;
    }
}
