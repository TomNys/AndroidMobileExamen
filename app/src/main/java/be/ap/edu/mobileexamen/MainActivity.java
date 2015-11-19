package be.ap.edu.mobileexamen;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.SimpleLocationOverlay;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;


public class MainActivity extends Activity {

    private TextView searchField;
    private Button searchButton;
    private MapView mapView;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private RequestQueue mRequestQueue;
    private String urlSearch = "http://nominatim.openstreetmap.org/search?q=";
    private String urlBib = "https://3ppo.cloudant.com/examen/json/bibliotheekoverzicht.json";
    ArrayList<Bibliotheek> allBibs = new ArrayList<Bibliotheek>();
    MapSQLiteHelper helper;




    private static String TAG = MainActivity.class.getSimpleName();
    private String jsonResponse;
    private SimpleLocationOverlay mBibLocationOverlay;

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        helper = new MapSQLiteHelper(this);

        // https://github.com/osmdroid/osmdroid/wiki/How-to-use-the-osmdroid-library
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(18);

        DefaultResourceProxyImpl mResourceProxy = new DefaultResourceProxyImpl(getApplicationContext());

        // http://code.tutsplus.com/tutorials/an-introduction-to-volley--cms-23800
        mRequestQueue = Volley.newRequestQueue(this);
        searchField = (TextView) findViewById(R.id.search_txtview);
        // disable text suggestions
        searchField.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        searchButton = (Button) findViewById(R.id.search_button);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchString = "";
                try {
                    searchString = URLEncoder.encode(searchField.getText().toString(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                JsonArrayRequest jr = new JsonArrayRequest(urlSearch + searchString + "&format=json", new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            hideSoftKeyBoard();
                            JSONObject obj = response.getJSONObject(0);
                            GeoPoint g = new GeoPoint(obj.getDouble("lat"), obj.getDouble("lon"));
                            mapView.getController().setCenter(g);
                        } catch (JSONException ex) {
                            Log.e("edu.ap.mapsaver", ex.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("edu.ap.mapsaver", error.getMessage());
                    }
                });

                mRequestQueue.add(jr);
            }
        });

        if (!getPreferences()) {
            // A JSONObject to post with the request. Null is allowed and indicates no parameters will be posted along with request.
            JSONObject obj = null;
            // haal alle parkeerzones op
            JsonObjectRequest jr = new JsonObjectRequest(Request.Method.GET, urlBib, obj, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    hideSoftKeyBoard();
                    try {
                        helper.saveBiblio(response.getJSONArray("data"));
                        setPreferences(true);
                        allBibs = helper.getAllBibs();
                        Log.d("edu.ap.mapsaver", "Libraries saved to DB");
                    } catch (JSONException e) {
                        Log.e("edu.ap.mapsaver", e.getMessage());
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("edu.ap.mapsaver", error.getMessage());
                }
            });
            mRequestQueue.add(jr);
        } else {
            allBibs = helper.getAllBibs();
            Log.d("edu.ap.mapsaver", "Libraries retrieved from DB");
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(getApplicationContext(), "GPS not enabled!", Toast.LENGTH_SHORT).show();
            // default = meistraat
            mapView.getController().setCenter(new GeoPoint(51.2244, 4.38566));
        }
        else {
            locationListener = new MyLocationListener();

            mapView.getController().setCenter(new GeoPoint(51.2244, 4.38566));
        }

        // Create an ArrayList with overlays to display objects on map
        ArrayList<OverlayItem> overlayItemArray = new ArrayList<OverlayItem>();

        // Add Library locations to the overlay
        for (Bibliotheek bib: helper.getAllBibs()){
            GeoPoint library = new GeoPoint(Double.parseDouble(bib.getPoint_lat()), Double.parseDouble(bib.getPoint_lng()));
            OverlayItem bibOverlayItem = new OverlayItem(bib.getNaam(),bib.getGrondOpp(),library);
            overlayItemArray.add(bibOverlayItem);
        }

        // Add the Array to the IconOverlay
        ItemizedIconOverlay<OverlayItem> itemizedIconOverlay = new ItemizedIconOverlay<OverlayItem>(this, overlayItemArray, null);
        MyOwnItemizedOverlay overlay = new MyOwnItemizedOverlay(this, overlayItemArray);
        // Add the overlay to the MapView
        mapView.getOverlays().add(overlay);


    }


    private void setPreferences(boolean b) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("db_filled", b);
        editor.commit();
    }

    private boolean getPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean("db_filled", false);
    }

    // http://codetheory.in/android-ontouchevent-ontouchlistener-motionevent-to-detect-common-gestures/


    private void hideSoftKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        if(imm.isAcceptingText()) { // verify if the soft keyboard is open
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
            mapView.getController().setCenter(new GeoPoint(loc.getLatitude(), loc.getLongitude()));
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider,
                                    int status, Bundle extras) {
        }
    }
}