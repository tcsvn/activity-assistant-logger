package com.example.activity_assistant_logger;
import android.os.Bundle;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
*/
public class HomeFragment extends Fragment implements MySpinner.OnItemSelectedListener, View.OnClickListener {
    final static String SERVER_STATUS_ONLINE = "online";
    final static String SERVER_STATUS_UNCONFIGURED = "unconfigured";
    final static String SERVER_STATUS_OFFLINE = "not reachable";
    final static String DEVICE_STATUS_REGISTERED = "registered";
    final static String DEVICE_STATUS_UNCONFIGURED = "unconfigured";
    final String STATE_INITIAL = "initial";


    Button mBtnScan;
    Button mBtnSync;
    TextView qrcode_text;
    TextView deviceStatus;
    TextView serverStatus;
    MySpinner mySpinner_activity;
    private final Boolean DEBUG = false;
    private SwitchCompat switch_logging;
    private String device_status;
    private String CHANNEL_ID = "0";
    private String server_status;
    private Runnable runnable_check_server_status;
    private static final int PERMISSION_REQUEST_CODE=200;
    public static final String SPINNER_INITIAL_VAL = "unconfigured";
    private Controller controller;
    private static final int TYPE_DAY_VIEW = 1;
    private static final int TYPE_THREE_DAY_VIEW = 2;


   //controller = new Controller(MainActivity.this, getIntent());

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        //args.putString(ARG_PARAM1, param1);
        //args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
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
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState){
        /* https://developer.android.com/guide/fragments/lifecycle
           instantiate callbacks
        * */
        controller.setHomeFragment(this);

        // Get
        qrcode_text = view.findViewById(R.id.textView_qrcode);
        deviceStatus = view.findViewById(R.id.device_status);
        serverStatus = view.findViewById(R.id.server_status);

        // Get active components
        switch_logging = view.findViewById(R.id.switch_logging);
        mySpinner_activity = view.findViewById(R.id.spinner_activity);
        mySpinner_activity.setOnItemSelectedListener(this);
        mBtnScan = view.findViewById(R.id.btn_scan_qrcode);
        mBtnSync = view.findViewById(R.id.btn_sync);

        // Register all callbacks
        switch_logging.setOnClickListener(this);
        mBtnScan.setOnClickListener(this);
        mBtnSync.setOnClickListener(this);

        populateValuesFromController();

    }

    public void populateValuesFromController(){
        try {
            setReloadSpinnerActivity((ArrayList<String>) controller.getActivities());
        }catch (NullPointerException e){
            // The case when device is not connected to a server
        };
        // If new controller than this is false, else the previous state
        switch_logging.setChecked(controller.getLogging());
        setServerStatus(controller.getServerState());
        setDeviceStatus(controller.getDeviceState());
    }

    @Override
    public void onClick(View view){
        switch (view.getId()){
            case R.id.switch_logging:
                this.onSwitchLogging(view);
                break;
            case R.id.btn_scan_qrcode:
                this.onBtnScan(view);
                break;
            case R.id.btn_sync:
                this.onBtnSync(view);
                break;
        }
    }



    //__GETTER/SETTER----------------------------------------------------------------------------------
    public String getSelectedActivity(){
        return mySpinner_activity.getSelectedItem().toString();
    }

    public boolean getSwitchChecked(){
        return switch_logging.isChecked();
    }

    public void setServerStatus(String new_status){
        /* sets the text and color of the server entry in status
         * */
        serverStatus.setText(new_status);

        // set the according color
        if(new_status == SERVER_STATUS_ONLINE){
            // TODO
            //serverStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.attr.colorPrimary));
        }
        else {
            serverStatus.setTextColor(Color.parseColor("#d3d3d3"));
        }
    }

    public void setSwitchLogging(Boolean val){
        switch_logging.setChecked(val);
    }

    public void setDeviceStatus(String new_status){
        device_status = new_status;
        deviceStatus.setText(new_status);
        // set the color
        if(new_status == Controller.DEVICE_STATUS_REGISTERED){
            // TODO refactor, set to primary color
            //deviceStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.primaryColor));
            qrcode_text.setText("");
            mBtnScan.setText("decouple");
        }
        else if (new_status == Controller.DEVICE_STATUS_UNCONFIGURED){
            // set color to orange
            deviceStatus.setTextColor(Color.parseColor("#d3d3d3"));
            //deviceStatus.setTextColor(Color.parseColor("#ff5722"));
            mBtnScan.setText("scan");
            qrcode_text.setText("qrcode");
        }
    }

//__Spinner__--------------------------------------------------------------------------------------
    public void reloadSpinners(){
        setReloadSpinnerActivity(mySpinner_activity.getItemList());
    }

    public void setReloadSpinnerActivity(ArrayList<String> spinnerArray){
         ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this.getActivity(),
                android.R.layout.simple_spinner_item,
                spinnerArray
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mySpinner_activity.setAdapter(adapter);
        // TODO check what this does
        //      I think this was to set the activity to from the server. Is this still needed though?
        //if(controller.isActAssistConfigured()
        //        && controller.deviceHasActivity()
        //        && containsElement(adapter, controller.getDeviceActivityName())){
        //    int pos = adapter.getPosition(controller.getDeviceActivityName());
        //    mySpinner_activity.programmaticallySetPosition(pos);
        //}
    }

    public void setSpinnerActivity(ArrayList<String> activityArray, String currentActivity){
          ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this.getActivity(),
                android.R.layout.simple_spinner_item,
                  activityArray
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mySpinner_activity.setAdapter(adapter);
        int pos = adapter.getPosition(currentActivity);
        mySpinner_activity.programmaticallySetPosition(pos);
    }

    public boolean containsElement(ArrayAdapter<String> adapter, String element){
        for(int i =0; i < adapter.getCount(); i++){
            if (adapter.getItem(i).equals(element)){
                return true;
            }
        }
        return false;
    }

    public void resetSpinnerLists(){
        /** deletes all items in the spinner and sets it to unconfigured
        * */
        setReloadSpinnerActivity(new ArrayList<String>(){{ add(SPINNER_INITIAL_VAL); }});
    }


//__Callbacks__--------------------------------------------------------------------------------
    // TODO flag for removal Menu activity file callback
    //@Override
    //public boolean onOptionsItemSelected(MenuItem item) {
    //    // TODO, navigation Handle item selection
    //    switch (item.getItemId()) {
    //        case R.id.action_display_actfile:
    //            controller.onBtnShowActFileView();
    //            return true;
    //        default:
    //            return super.onOptionsItemSelected(item);
    //    }
    //}

    public void onBtnSync(View view){
        controller.onBtnSynchronize();
    }

    public void onBtnScan(View view) {
        switch ((String) mBtnScan.getText()){
            case "scan":
                controller.onScan();
                break;
            case "decouple":
                controller.onDecouple();
                break;
        }
    }

    public void onSwitchLogging(View view){
        controller.onSwitchToggled(switch_logging.isChecked());
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id, boolean userSelected) {
        // only update the model if the user selected the stuff
        String selectedActivity = mySpinner_activity.getSelectedItem().toString();
        if (userSelected && selectedActivity != SPINNER_INITIAL_VAL) {
            controller.onActivitySelected(selectedActivity);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        System.out.println("");
    }

}
