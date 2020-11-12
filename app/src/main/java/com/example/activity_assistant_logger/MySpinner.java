package com.example.activity_assistant_logger;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import androidx.appcompat.widget.AppCompatSpinner;

import java.util.ArrayList;

/**
 * Used this to differentiate between user selected and prorammatically selected
 * Call {@link MySpinner#programmaticallySetPosition} to use this feature.
 * Created by vedant on 6/1/15.
 */
public class MySpinner extends AppCompatSpinner implements AdapterView.OnItemSelectedListener {

    OnItemSelectedListener mListener;

    /**
     * used to ascertain whether the user selected an item on spinner (and not programmatically)
     */
    private boolean mUserActionOnSpinner = true;

    public ArrayList<String> getItemList(){
        Adapter adp = this.getAdapter();
        ArrayList<String> result = new ArrayList<>();
        for(int i=0; i <adp.getCount(); i++){
            result.add(adp.getItem(i).toString());
        }
        return result;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        if (mListener != null) {

            mListener.onItemSelected(parent, view, position, id, mUserActionOnSpinner);
        }
        // reset variable, so that it will always be true unless tampered with
        mUserActionOnSpinner = true;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        if (mListener != null)
            mListener.onNothingSelected(parent);
    }

    public interface OnItemSelectedListener {
        /**
         * <p>Callback method to be invoked when an item in this view has been
         * selected. This callback is invoked only when the newly selected
         * position is different from the previously selected position or if
         * there was no selected item.</p>
         *
         * Impelmenters can call getItemAtPosition(position) if they need to access the
         * data associated with the selected item.
         *
         * @param parent The AdapterView where the selection happened
         * @param view The view within the AdapterView that was clicked
         * @param position The position of the view in the adapter
         * @param id The row id of the item that is selected
         */
        void onItemSelected(AdapterView<?> parent, View view, int position, long id, boolean userSelected);

        /**
         * Callback method to be invoked when the selection disappears from this
         * view. The selection can disappear for instance when touch is activated
         * or when the adapter becomes empty.
         *
         * @param parent The AdapterView that now contains no selected item.
         */
        void onNothingSelected(AdapterView<?> parent);
    }

    public void programmaticallySetPosition(int pos) {
        mUserActionOnSpinner = false;
        setSelection(pos);
    }

    public void setOnItemSelectedListener (OnItemSelectedListener listener) {
        mListener = listener;
    }

    public MySpinner(Context context) {
        super(context);
        super.setOnItemSelectedListener(this);
    }

    public MySpinner(Context context, int mode) {
        super(context, mode);
        super.setOnItemSelectedListener(this);
    }

    public MySpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setOnItemSelectedListener(this);
    }

    public MySpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        super.setOnItemSelectedListener(this);
    }

    public MySpinner(Context context, AttributeSet attrs, int defStyle, int mode) {
        super(context, attrs, defStyle, mode);
        super.setOnItemSelectedListener(this);
    }
}