package com.diakogiannis.metropolitan.paper.alexiusruntracker;

import android.Manifest;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.diakogiannis.metropolitan.paper.alexiusruntracker.entity.Entry;
import com.diakogiannis.metropolitan.paper.alexiusruntracker.helpers.HttpUtils;
import com.diakogiannis.metropolitan.paper.alexiusruntracker.helpers.SQLiteDatabaseHandler;
import com.diakogiannis.metropolitan.paper.alexiusruntracker.timer.StartMeasuringPace;
import com.diakogiannis.metropolitan.paper.alexiusruntracker.tracker.LocationTrack;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int THRESHOLD = 600; //used to see whether a shake gesture has been detected or not.
    private final String TAG = MainActivity.class.getSimpleName();
    TextView coordinates;
    TextView address;
    private Timer mTimer1;
    private TimerTask mTt1;
    private Handler mTimerHandler = new Handler();
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private long lastTime = 0;
    private float lastX, lastY, lastZ;
    private SQLiteDatabaseHandler db;

    private boolean trackingStarted = false;

    final int DELAY_PERIOD = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION},
                255);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        db = new SQLiteDatabaseHandler(this);

        ((TextView) findViewById(R.id.pacetxt)).setText("--:--");
        ((TextView) findViewById(R.id.lineartxt)).setText("--:--");
        ((TextView) findViewById(R.id.distnum)).setText("--:--");

    }

    public void startPaceBgService(View v) {
        CharSequence toastText = "";
        if(trackingStarted){
            toastText = "Tracking Stopped";
            ((Button) findViewById(R.id.startTrackingButton)).setText("START!");
        } else {
            toastText = "Tracking Started";
            ((Button) findViewById(R.id.startTrackingButton)).setText("STOP :(");
        }

        trackingStarted = !trackingStarted;

        int duration = Toast.LENGTH_LONG;

        Toast toast = Toast.makeText(MainActivity.this, toastText, duration);
        toast.show();



        if(trackingStarted) {

            //cleab db
            db.deleteAll();

            StartMeasuringPace pace = new StartMeasuringPace();

            mTimer1 = new Timer();
            final HttpUtils httpUtils = new HttpUtils();


            mTt1 = new TimerTask() {
                final double[] oldLat = {0d};
                final double[] oldLon = {0d};
                final double[] lat = {0d};
                final double[] lon = {0d};
                final double[] distance = {0d};
                String[] urlParams = new String[2];

                int iteration = 0;

                public void run() {


                    mTimerHandler.post(new Runnable() {


                        public void run() {

                            iteration++;

                            LocationTrack ltrack = new LocationTrack(MainActivity.this);

                            String curPos = "Lat: " + ltrack.getLatitude() + " Lon:" + ltrack.getLongitude();

                            oldLat[0] = lat[0];
                            oldLon[0] = lon[0];
                            lat[0] = ltrack.getLatitude();
                            lon[0] = ltrack.getLongitude();

                            //calculate distance
                            distance[0] = distance(oldLat[0], oldLon[0], lat[0], lon[0], "K") * 1000;
                            String distanceFormated = new DecimalFormat("#.##").format(distance[0]);

                            if (distance[0] > 1000) {
                                distance[0] = 0;
                            }

                            //calculate linear acceleration
                            String linearAccelerationFormated = new DecimalFormat("#.##").format(linearAcceleration(lastX, lastY, lastZ));

                            //calculate pace
                            urlParams[0] = Double.toString(distance[0]);
                            urlParams[1] = "5000";
                            String pace = "0";
                            try {
                                pace = new JSONObject(httpUtils.doInBackground(urlParams)).getString("pace");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            //put everything to DB
                            db.addEntry(new Entry("Pace: "+pace+" Linear Acceleration: "+linearAccelerationFormated+"  Distance covered in "+DELAY_PERIOD+"ms: "+distanceFormated));


                            //log it
                            Log.i("Info", "Pace " + pace);
                            Log.i("Info", "Distance:" + distanceFormated);
                            Log.i("Info", "Linear Acceleration:" + linearAccelerationFormated);

                            //dispay on screen
                            ((TextView) findViewById(R.id.pacetxt)).setText(pace);
                            ((TextView) findViewById(R.id.lineartxt)).setText(linearAccelerationFormated);
                            ((TextView) findViewById(R.id.distnum)).setText(distanceFormated);

                            // list all players
                            List<Entry> entries = db.allEntries();

                            StringBuilder sb = new StringBuilder();

                            if (entries != null) {
                                for(Entry entry : entries){
                                    sb.append(entry.getRunEntry());
                                    sb.append("\n\n");
                                }
                                sb.append("\n");
                            }
                            ((TextView) findViewById(R.id.resultsTxt)).setText(sb.toString());
                            ((TextView) findViewById(R.id.resultsTxt)).setMovementMethod(new ScrollingMovementMethod());
                        }
                    });
                }
            };
            mTimer1.schedule(mTt1, 1, DELAY_PERIOD);
        } else {
            mTt1.cancel();
            //output DB results to view
            //reset display
            ((TextView) findViewById(R.id.pacetxt)).setText("--:--");
            ((TextView) findViewById(R.id.lineartxt)).setText("--:--");
            ((TextView) findViewById(R.id.distnum)).setText("--:--");

        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastTime) > 100) {
                long diffTime = (currentTime - lastTime);
                lastTime = currentTime;
                float speed = Math.abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000;

                lastX = x;
                lastY = y;
                lastZ = z;
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //default
    }

    /**
     *  Calculate distance
     * @param lat1
     * @param lon1
     * @param lat2
     * @param lon2
     * @param unit
     * @return
     */
    private static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        } else {
            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515;
            if (unit.equals("K")) {
                dist = dist * 1.609344;
            } else if (unit.equals("N")) {
                dist = dist * 0.8684;
            }
            return (dist);
        }
    }

    public void stopTimer() {
        if (mTimer1 != null) {
            mTimer1.cancel();
            mTimer1.purge();
        }
    }

    /**
     * The equation to calculate it is, √‾‾‾‾‾‾‾‾‾‾‾‾𝑥2+𝑦2+𝑧2
     * @param a
     * @param b
     * @param c
     * @return
     */
    private double linearAcceleration(float a, float b, float c) {
        return Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2) + Math.pow(c, 2));
    }

}


