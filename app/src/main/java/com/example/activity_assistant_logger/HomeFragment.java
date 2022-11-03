package com.example.activity_assistant_logger;
import android.os.Bundle;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import com.example.activity_assistant_logger.actassistapi.ActAssistApi;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import static android.Manifest.permission.INTERNET;
import static com.example.activity_assistant_logger.Controller.SERVER_STATUS_ONLINE;




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



    Button qrcode_scan;
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


    }
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState){

        // Set controller
        controller = new ViewModelProvider(requireActivity()).get(Controller.class);
        controller.setHomeFragment(this);

        qrcode_scan = view.findViewById(R.id.btn_scan_qrcode);
        qrcode_text = view.findViewById(R.id.textView_qrcode);
        deviceStatus = view.findViewById(R.id.device_status);
        serverStatus = view.findViewById(R.id.server_status);

        switch_logging = view.findViewById(R.id.switch_logging);
        mySpinner_activity = view.findViewById(R.id.spinner_activity);
        mySpinner_activity.setOnItemSelectedListener(this);

        // Register all callbacks
        switch_logging.setOnClickListener(this);



        // INitialize view with values
        if (controller.isActAssistConfigured()){
            setReloadSpinnerActivity((ArrayList<String>) controller.getActivities());
            setServerStatus(SERVER_STATUS_OFFLINE);
            setDeviceStatus(DEVICE_STATUS_REGISTERED);
        }
        else{

        }


    }

    @Override
    public void onClick(View view){
        switch (view.getId()){
            case R.id.switch_logging:
                this.switchLogging(view);
                break;
            case R.id.btn_scan_qrcode:
                this.btnScanQrCode(view);
                break;
            case R.id.button2:
                this.btnSynchronize(view);
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }


        //__GETTER/SETTER----------------------------------------------------------------------------------
    public String getSelectedActivity(){
        return mySpinner_activity.getSelectedItem().toString();
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
        // TODO notification
        //if (val){
        //    createNotification();
        //}
        //else{
        //    removeNotification();
        //}
        switch_logging.setChecked(val);
        controller.setLogging(val);
    }

    public void setDeviceStatus(String new_status){
        device_status = new_status;
        deviceStatus.setText(new_status);
        // set the color
        if(new_status == Controller.DEVICE_STATUS_REGISTERED){
            // TODO refactor, set to primary color
            //deviceStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.primaryColor));
            qrcode_text.setText("");
            qrcode_scan.setText("decouple");
        }
        else if (new_status == Controller.DEVICE_STATUS_UNCONFIGURED){
            // set color to orange
            deviceStatus.setTextColor(Color.parseColor("#d3d3d3"));
            //deviceStatus.setTextColor(Color.parseColor("#ff5722"));
            qrcode_scan.setText("scan");
            qrcode_text.setText("qrcode");
        }
    }

    public boolean getSwitchChecked(){
        return switch_logging.isChecked();
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

    public void btnSynchronize(View view){
        controller.onBtnSynchronize();
    }

    public void btnScanQrCode(View view) {
        String btntext = (String) qrcode_scan.getText();
        if( btntext.equals("scan")){
            if (!DEBUG){
                controller.onBtnScanQRCode();
            }
            else {
                // DEBUG FROM HERE
                JSONObject obj = new JSONObject();
                try {
                    obj.put(ActAssistApi.URL_API,
                            "http://192.168.0.185:8000/api/");
                    obj.put(ActAssistApi.SMARTPHONE_ID,
                            1);
                    obj.put(ActAssistApi.URL_PERSON,
                            "persons/1/");
                    obj.put(ActAssistApi.USERNAME,
                            "admin");
                    obj.put(ActAssistApi.PASSWORD,
                            "asdf");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                controller.receivedDataFromQRCode(obj);
                // DEBUG END
            }
        }
        else{
            controller.onBtnDecouple();
        }
    }

    public void switchLogging(View view){
        controller.switchLoggingToggled(switch_logging.isChecked());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        /** The BarcodeCaptureActivity scanned sth. leads to calling this method
         * also the ActivityFile upon return leads to calling this but nothing is done as
         * the Intent data is null
         * */
         if (resultCode == CommonStatusCodes.SUCCESS && data!= null) {
             try {
                 // case of the activity with intent
                 Barcode barcode = data.getParcelableExtra("barcode");
                 String connectionInformation = barcode.displayValue;
                 try {
                     controller.receivedDataFromQRCode(
                             new JSONObject(connectionInformation)
                     );

                 } catch (JSONException e) {
                     e.printStackTrace();
                 }
             }catch (Exception e){
                String currentActivity = data.getStringExtra("currentActivity");
                controller.createToast("got forom notification: " + currentActivity);
             }
         }
         else {
            super.onActivityResult(requestCode, resultCode, data);
        }
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
