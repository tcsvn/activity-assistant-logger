package com.example.activity_assistant_logger;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;




import android.content.Context;
import android.content.DialogInterface;
import android.graphics.RectF;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.util.TypedValue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


import com.example.activity_assistant_logger.actassistapi.Activity;
import com.example.activity_assistant_logger.weekview.MonthLoader;
import com.example.activity_assistant_logger.weekview.WeekViewEvent;
import com.example.activity_assistant_logger.weekview.WeekView;
import com.example.activity_assistant_logger.weekview.DateTimeInterpreter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


import android.widget.Toast;
import java.text.SimpleDateFormat;


public class WeekFragment extends Fragment implements MonthLoader.MonthChangeListener,  WeekView.EventLongPressListener, View.OnClickListener {
/*
    Implements https://github.com/alamkanak/Android-Week-View
 */
    TextView text;

    private static final int TYPE_DAY_VIEW = 1;
    private static final int TYPE_THREE_DAY_VIEW = 2;
    private static final int TYPE_WEEK_VIEW = 3;
    private int mWeekViewType = TYPE_THREE_DAY_VIEW;
    private WeekView mWeekView;
    private FloatingActionButton mButtonAddActivity;

    private Context mContext;
    private Controller controller;

    public WeekFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext=context;
    }

    public static WeekFragment newInstance(String param1, String param2) {
        WeekFragment fragment = new WeekFragment();
        Bundle args = new Bundle();
        args.putString("test1", param1);
        args.putString("test2", param2);
        fragment.setArguments(args);
        return fragment;

    }


    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            //do when hidden
        } else {
            // When activities are logged and the fragments are switched newly created
            // activities should appear in the view
            mWeekView.notifyDatasetChanged();
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //mParam1 = getArguments().getString(ARG_PARAM1);
            //mParam2 = getArguments().getString(ARG_PARAM2);
        }
        controller = new ViewModelProvider(requireActivity()).get(Controller.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Provide layout view for fragment
        return inflater.inflate(R.layout.fragment_week, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState){
        /* https://developer.android.com/guide/fragments/lifecycle
          Set up the initial state of, instantiate callbacks
        * */

        // Get a reference for the week view in the layout.
        mWeekView = (WeekView) view.findViewById(R.id.weekView);

        // weekview Loader
        mButtonAddActivity = view.findViewById(R.id.btn_weekview_add_activity);
        mButtonAddActivity.setOnClickListener(this);
        ///event.setColor(getResources().getColor(R.color.event_color_01));

        // The week view has infinite scrolling horizontally. We have to provide the events of a
        // month every time the month changes on the week view.
        mWeekView.setMonthChangeListener(this);

        // Set long press listener for events.
        mWeekView.setEventLongPressListener(this);

        // Set up a date time interpreter to interpret how the date and time will be formatted in
        // the week view. This is optional.
        setupDateTimeInterpreter(false);

        mWeekViewType = TYPE_DAY_VIEW;
        mWeekView.setNumberOfVisibleDays(1);

        // Lets change some dimensions to best fit the view.
        mWeekView.setColumnGap((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
        mWeekView.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
        mWeekView.setEventTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));

        controller.setWeekFragment(this);

    }


    /**
     * Set up a date time interpreter which will show short date values when in week view and long
     * date values otherwise.
     *
     * @param shortDate True if the date values should be short.
     */
    private void setupDateTimeInterpreter(final boolean shortDate) {
        mWeekView.setDateTimeInterpreter(new DateTimeInterpreter() {
            @Override
            public String interpretDate(Calendar date) {
                SimpleDateFormat weekdayNameFormat = new SimpleDateFormat("EEE", Locale.getDefault());
                String weekday = weekdayNameFormat.format(date.getTime());
                SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

                // All android api level do not have a standard way of getting the first letter of
                // the week day name. Hence we get the first char programmatically.
                // Details: http://stackoverflow.com/questions/16959502/get-one-letter-abbreviation-of-week-day-of-a-date-in-java#answer-16959657
                if (shortDate)
                    weekday = String.valueOf(weekday.charAt(0));
                return weekday.toUpperCase() + " - " + format.format(date.getTime());
            }

            @Override
            public String interpretTime(int hour, int minutes) {
                /* Example: h=10, min=35 --> 10:35 */

                return String.format("%02d", hour) + ":" + String.format("%02d", minutes);
            }
        });
    }


    protected String getEventTitle(Calendar time) {
        return String.format("Event of %02d:%02d %s/%d", time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE), time.get(Calendar.MONTH)+1, time.get(Calendar.DAY_OF_MONTH));
    }

    private void onBtnAddActivity(){
        Context appContext = this.getActivity();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(appContext);//,  R.style.AlertDialogTheme);

        View viewInflated = LayoutInflater.from(appContext).inflate(R.layout.create_activity, null);
        ActivityFileHandler activityFile = new ActivityFileHandler(appContext);

        // Create dropdown with current activities
        ArrayList<String> activities = (ArrayList<String>) controller.getActivities();
        Spinner spinner = (Spinner) viewInflated.findViewById(R.id.spinner_create_activity);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_spinner_item, activities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Create initial event
        WeekViewEvent event = new WeekViewEvent(1234, activities.get(0), Calendar.getInstance(),
                Calendar.getInstance());
        EditText inputStarttime = (EditText) viewInflated.findViewById(R.id.ca_start_time);
        EditText inputEndtime = (EditText) viewInflated.findViewById(R.id.ca_end_time);
        inputStarttime.setText(ActivityFileHandler.cal2Str(event.getStartTime()));
        inputEndtime.setText(ActivityFileHandler.cal2Str(event.getEndTime()));

        builder.setView(viewInflated)
               .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // set activity
                        String starttime = inputStarttime.getText().toString() ;
                        String endtime = inputEndtime.getText().toString();
                        Calendar st = null;
                        Calendar et = null;
                        try {
                            st = ActivityFileHandler.str2Cal(starttime);
                            et = ActivityFileHandler.str2Cal(endtime);
                        } catch (ParseException e){
                            Toast.makeText(appContext, "No valid Timestamp given...", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (st.compareTo(et) >=0){
                            Toast.makeText(appContext, "Start time can not be after End time...", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        WeekViewEvent newEvent = new WeekViewEvent(st.getTimeInMillis(),
                                spinner.getSelectedItem().toString(), st, et);
                        try {
                            activityFile.insertActivity(appContext, newEvent);
                        }catch (FileNotFoundException  e){
                            Toast.makeText(appContext, "Could not find activity file...", Toast.LENGTH_SHORT).show();
                        }catch (IOException e){
                            Toast.makeText(appContext, "Could not write to activity file...", Toast.LENGTH_SHORT).show();
                        }
                        mWeekView.notifyDatasetChanged();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        dialog.cancel();
                    }
                });
        AlertDialog dialog  = builder.create();
        dialog.show();
    }

    @Override
    public void onEventLongPress(WeekViewEvent event, RectF eventRect) {
        Context appContext = this.getActivity();
        // Get text representation of date from ActivityFileHandler
        ActivityFileHandler activityFile = new ActivityFileHandler(appContext);
        if (! activityFile.isActivityInFile(appContext, event)){
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(appContext);
        View viewInflated = LayoutInflater.from(appContext).inflate(R.layout.edit_activity, null);

        EditText inputStarttime = (EditText) viewInflated.findViewById(R.id.start_time);
        EditText inputEndtime = (EditText) viewInflated.findViewById(R.id.end_time);
        TextView title = (TextView) viewInflated.findViewById(R.id.title_weekview_dialog);
        Button btnDelete = (Button) viewInflated.findViewById(R.id.btn_weekview_delete);

        String tmp1 = ActivityFileHandler.cal2Str(event.getStartTime());
        inputStarttime.setText(tmp1);
        inputEndtime.setText(ActivityFileHandler.cal2Str(event.getEndTime()));
        title.setText("Edit :" + event.getName());



        builder.setView(viewInflated)
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // set activity
                        String starttime = inputStarttime.getText().toString() ;
                        String endtime = inputEndtime.getText().toString();
                        Calendar st = null;
                        Calendar et = null;
                        try {
                            st = ActivityFileHandler.str2Cal(starttime);
                            et = ActivityFileHandler.str2Cal(endtime);
                        } catch (ParseException e){
                            Toast.makeText(appContext, "No valid Timestamp given...", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (st.compareTo(et) >=0){
                            Toast.makeText(appContext, "Start time can not be after End time...", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        WeekViewEvent newEvent = new WeekViewEvent(st.getTimeInMillis(), event.getName(), st, et);
                        try {
                            activityFile.overwriteActivity(appContext, event, newEvent);
                        }catch (FileNotFoundException  e){
                            Toast.makeText(appContext, "Could not find activity file...", Toast.LENGTH_SHORT).show();
                        }catch (IOException e){
                            Toast.makeText(appContext, "Could not write to activity file...", Toast.LENGTH_SHORT).show();
                        }
                        mWeekView.notifyDatasetChanged();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog

                        dialog.cancel();
                    }
                });
        AlertDialog dialog  = builder.create();

        btnDelete.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                try {
                    activityFile.deleteActivity(appContext, event);
                } catch (Exception IOException){}
                mWeekView.notifyDatasetChanged();
                dialog.dismiss();
            }
        });

        dialog.show();
    }


    @Override
    public List<? extends WeekViewEvent> onMonthChange(int newYear, int newMonth) {
        ArrayList<WeekViewEvent> events = (ArrayList<WeekViewEvent>) controller.getActivitiesAsEvents();

        // Filter the events for correct year and month
        List<WeekViewEvent> notInMonth = new ArrayList<WeekViewEvent>();
        for (int i =0; i < events.size(); i++){
             System.out.println(ActivityFileHandler.cal2Str(events.get(i).getStartTime()));
             int year = events.get(i).getStartTime().get(Calendar.YEAR);
             int month = events.get(i).getStartTime().get(Calendar.MONTH)+1; // WTF calendar
             if (year != newYear || month != newMonth){
                 notInMonth.add(events.get(i));
             }
        }
        events.removeAll(notInMonth);
        return events;
    }

    public WeekView getWeekView() {
        return mWeekView;
    }



    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_weekview_add_activity:
                this.onBtnAddActivity();
                break;
        }
    }
}
