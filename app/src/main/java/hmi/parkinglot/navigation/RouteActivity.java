package hmi.parkinglot.navigation;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Scene;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RoutingError;
import com.here.android.mpa.search.DiscoveryResult;
import com.here.android.mpa.search.DiscoveryResultPage;
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.GeocodeRequest;
import com.here.android.mpa.search.GeocodeResult;
import com.here.android.mpa.search.Location;
import com.here.android.mpa.search.PlaceLink;
import com.here.android.mpa.search.ResultListener;
import com.here.android.mpa.search.ReverseGeocodeRequest2;
import com.here.android.mpa.search.SearchRequest;
import com.here.android.mpa.search.TextSuggestionRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hmi.parkinglot.Alert;
import hmi.parkinglot.Application;
import hmi.parkinglot.MainActivity;
import hmi.parkinglot.R;

/**
 * Route Wizard
 */
public class RouteActivity implements LocationListener {
    public static Map<String, double[]> cityCoords = new HashMap<>();
    private static int GEOCODE_SEARCH_AREA = 6000;
    private static String[] CITIES = new String[]{
            "Ho Chi Minh City",
            "Hanoi",
            "Hai Phong",
            "Can Tho",
            "Da Nang",
            "Hue",
            "Quy Nhon",
            "Hiep Hoa",
            "Phu Vinh",
            "Thai Nguyen",
            "Nha Trang"
    };
    private static double[][] CITY_COORDS = new double[][]{
            {10.776577, 106.70085},
            {21.028167, 105.854152},
            {20.864807, 106.683449},
            {10.037105, 105.788249},
            {16.074806, 108.223958},
            {16.461901, 107.595458},
            {13.768898, 109.228563},
            {10.933333, 106.833333},
            {18.716667, 105.7},
            {21.59422, 105.848172},
            {12.248089, 109.19436}
    };
    private static Activity activity;

    static {
        for (int j = 0; j < CITIES.length; j++) {
            cityCoords.put(CITIES[j], CITY_COORDS[j]);
        }
    }

    private ProgressDialog progress, locationProgress;
    private AutoCompleteTextView origin, destination, city, originCity;
    private Button nextButton;
    private ArrayAdapter<String> originAdapter;
    private ArrayAdapter<String> destinationAdapter;
    private List<String> optionList1 = new ArrayList<String>();
    private List<String> optionList2 = new ArrayList<String>();
    private List<String> cityList = Arrays.asList(CITIES);
    private Drawable x, y;
    private String currentStep = "Origin";
    private RouteData routeData = new RouteData();
    private Context context;
    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            InputMethodManager mgr = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            mgr.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    };
    private Handler UIHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message inputMessage) {
            if (inputMessage.what == -1) {
                if (progress != null) {
                    progress.dismiss();
                }

                Toast.makeText(Application.mainActivity.getApplicationContext(),
                        "Error while geocoding location", Toast.LENGTH_SHORT).show();
            }
        }
    };

    public RouteActivity(Context ctx) {
        context = ctx;
        activity = Application.mainActivity;

        x = activity.getResources().getDrawable(R.drawable.clear);
        x.setBounds(0, 0, 50, 50);

        y = activity.getResources().getDrawable(R.drawable.search);
        y.setBounds(0, 0, 50, 50);
    }

    public void start() {
        ViewGroup rootContainer = activity.findViewById(R.id.mainFrame);
        activity.getLayoutInflater().inflate(R.layout.route, rootContainer);

        goToOriginStep();
    }

    private void goToOriginStep() {
        ViewGroup routeContainer = activity.findViewById(R.id.frameRoute);
        Scene scene1 = Scene.getSceneForLayout(routeContainer, R.layout.activity_route, activity);

        TransitionManager.go(scene1);

        setupNextEventHandler();
        setupHeader(1);
        setAutoCompleteHandlerOrigin();

        SharedPreferences prefs = Application.mainActivity.getPreferences(Context.MODE_PRIVATE);

        originCity.setText(prefs.getString(Application.LAST_CITY_VISITED, "Hanoi"));
        origin.setText(prefs.getString(Application.LAST_ORIGIN, Application.EMPTY_STR));

        if (routeData.originCity.length() > 0) {
            originCity.setText(routeData.originCity);
        }
        if (routeData.origin.length() > 0) {
            origin.setText(routeData.origin);
        }

        if (origin.getText().length() == 0) {
            origin.requestFocus();
        }
    }

    private void goToDestinationStep() {
        ViewGroup routeContainer = activity.findViewById(R.id.frameRoute);
        Scene scene1 = Scene.getSceneForLayout(routeContainer, R.layout.activity_route_2, activity);
        TransitionManager.go(scene1);

        setupNextEventHandler();
        setupHeader(2);
        setAutoCompleteHandlerDestination();

        // By default dest city equal to the origin city
        if (city.getText().length() == 0) {
            city.setText(routeData.originCity);
            routeData.city = routeData.originCity;
        }

        SharedPreferences prefs = Application.mainActivity.getPreferences(Context.MODE_PRIVATE);

        if (routeData.originCity.equals(prefs.getString(Application.LAST_CITY_VISITED,
                Application.EMPTY_STR))) {
            destination.setText(prefs.getString(Application.LAST_DESTINATION,
                    Application.EMPTY_STR));
        }
    }

    private void goToParkingStep() {
        ViewGroup routeContainer = activity.findViewById(R.id.frameRoute);
        Scene scene1 = Scene.getSceneForLayout(routeContainer, R.layout.activity_route_3, activity);
        TransitionManager.go(scene1);

        setupNextEventHandler();
        setupHeader(3);
    }

    private void setupHeader(int step) {
        Toolbar myToolbar = activity.findViewById(R.id.my_toolbar);
        ((AppCompatActivity) activity).setSupportActionBar(myToolbar);

        ((AppCompatActivity) activity).getSupportActionBar().setTitle("Route Planning " + step + "/3");
        ((AppCompatActivity) activity).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setupNextEventHandler() {
        nextButton = activity.findViewById(R.id.nextButton1);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextStep(v);
            }
        });
    }

    private void setAutoCompleteHandlerOrigin() {
        origin = activity.findViewById(R.id.editText);
        originCity = activity.findViewById(R.id.originCityInput);

        originAdapter = new ArrayAdapter<String>(activity,
                android.R.layout.simple_dropdown_item_1line, optionList1);
        origin.setAdapter(originAdapter);
        origin.addTextChangedListener(new MyTextWatcher(origin, originAdapter));

        origin.setCompoundDrawables(y, null, null, null);
        origin.setOnTouchListener(new MyTouchListener(origin));

        origin.setText(routeData.origin);

        originCity.setCompoundDrawables(y, null, null, null);
        ArrayAdapter originCityAdapter = new ArrayAdapter<String>(activity,
                android.R.layout.simple_dropdown_item_1line, cityList);
        originCity.setAdapter(originCityAdapter);
        originCity.setOnTouchListener(new MyTouchListener(originCity));
        originCity.addTextChangedListener(new MyTextWatcher(originCity, originCityAdapter));

        originCity.setText(routeData.originCity);

        origin.setOnItemClickListener(itemClickListener);

        if (routeData.originCity.equals("")) {
            // originCity.requestFocus();
        }

        originCity.setOnItemClickListener(itemClickListener);

        activity.findViewById(R.id.currentLocButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateCurrentposition();
            }
        });

        checkNextButton(null);
    }

    private void setAutoCompleteHandlerDestination() {
        destination = activity.findViewById(R.id.editText2);
        destinationAdapter = new ArrayAdapter<String>(activity,
                android.R.layout.simple_dropdown_item_1line, optionList2);
        destination.setAdapter(destinationAdapter);
        destination.addTextChangedListener(new MyTextWatcher(destination, destinationAdapter));

        destination.setCompoundDrawables(y, null, null, null);
        destination.setOnTouchListener(new MyTouchListener(destination));

        destination.setOnItemClickListener(itemClickListener);

        city = activity.findViewById(R.id.cityInput);
        ArrayAdapter cityAdapter = new ArrayAdapter<String>(activity,
                android.R.layout.simple_dropdown_item_1line, cityList);
        city.setAdapter(cityAdapter);

        city.setCompoundDrawables(y, null, null, null);
        city.setOnTouchListener(new MyTouchListener(city));
        city.addTextChangedListener(new MyTextWatcher(city, cityAdapter));

        city.setOnItemClickListener(itemClickListener);

        city.setText(routeData.city);
        destination.setText(routeData.destination);

        /*
        if (routeData.city.equals("")) {
            city.requestFocus();
        } */

        checkNextButton(null);
    }

    public void back() {
        if (currentStep.equals("Origin")) {
            currentStep = "";
            ((MainActivity) activity).onRouteCanceled();
        } else if (currentStep.equals("Destination")) {
            routeData.city = city.getText().toString();
            routeData.destination = destination.getText().toString();
            currentStep = "Origin";
            goToOriginStep();
        } else if (currentStep.equals("Parking")) {
            currentStep = "Destination";

            fillParkingPreferences();
            goToDestinationStep();
        }
    }

    private void nextStep(View v) {
        if (currentStep.equals("Origin")) {
            routeData.origin = origin.getText().toString();
            routeData.originCity = originCity.getText().toString();
            currentStep = "Destination";
            goToDestinationStep();
        } else if (currentStep.equals("Destination")) {
            routeData.destination = destination.getText().toString();
            routeData.city = city.getText().toString();
            currentStep = "Parking";
            goToParkingStep();
        } else if (currentStep.equals("Parking")) {
            fillParkingPreferences();

            progress = ProgressDialog.show(activity, "Route Calculation",
                    "We are calculating a route", true);
            calculateRoute();
        }
    }

    private void fillParkingPreferences() {
        routeData.parkingCategory.clear();
        routeData.parkingDistance = 500;
        routeData.vehicle = "Car";
    }

    private void calculateCurrentposition() {
        locationProgress = ProgressDialog.show(Application.mainActivity, "FIWARE-HERE",
                "Obtaining current location", true);

        Application.mainActivity.calculateCurrentPosition(this);
    }

    public void onLocationReady(GeoCoordinate coords) {
        locationProgress.dismiss();

        if (coords != null) {
            ReverseGeocodeRequest2 req = new ReverseGeocodeRequest2(coords);
            req.execute(new ResultListener<Location>() {
                @Override
                public void onCompleted(Location location, ErrorCode errorCode) {
                    originCity.setText(location.getAddress().getCity());
                    origin.setText(location.getAddress().getText());
                    origin.setEnabled(true);
                }
            });
        }
    }

    private void doCalculateRoute(GeoCoordinate start, GeoCoordinate end) {
        // Initialize RouteManager
        CoreRouter routeManager = new CoreRouter();

        // 3. Select routing options via RoutingMode
        final RoutePlan routePlan = new RoutePlan();
        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);
        routePlan.setRouteOptions(routeOptions);

        routePlan.addWaypoint(new RouteWaypoint(start));
        routePlan.addWaypoint(new RouteWaypoint(end));

        // Retrieve Routing information via RouteManagerListener
        routeManager.calculateRoute(routePlan, new CoreRouter.Listener() {
            @Override
            public void onCalculateRouteFinished(List<RouteResult> list, RoutingError routingError) {
                if (routingError == RoutingError.NONE && list.get(0).getRoute() != null) {
                    routeData.route = list.get(0).getRoute();
                    progress.dismiss();
                    Log.d("tung", "route data city " + routeData.city);
                    ((MainActivity) activity).onRouteReady(routeData);
                } else {
                    Alert.show(activity.getApplicationContext(), "Error while obtaining route");
                    Log.e("FIWARE-HERE", "Error while obtaining route: " + routingError);
                }
            }

            @Override
            public void onProgress(int i) {

            }
        });
    }

    private void geoCodeLocation(String locationStr, GeoCoordinate center,
                                 final ResultListener<GeoCoordinate> listener) {
        if (locationStr.indexOf(",") == -1) {
            // It seems to be a POI. issuing search request
            searchLocation(locationStr, center, listener);
        } else {
            getCoordinatesFor(locationStr, center, listener);
        }
    }

    private void searchLocation(String locationStr, GeoCoordinate center,
                                final ResultListener<GeoCoordinate> listener) {
        SearchRequest sr = new SearchRequest(locationStr);
        sr.setSearchCenter(center);
        sr.setCollectionSize(1);
        sr.execute(new ResultListener<DiscoveryResultPage>() {
            @Override
            public void onCompleted(DiscoveryResultPage data, ErrorCode errorCode) {
                if (errorCode == ErrorCode.NONE && data != null) {
                    List<DiscoveryResult> results = data.getItems();
                    if (results != null && results.size() > 0) {
                        DiscoveryResult result = results.get(0);
                        if (result.getResultType() == DiscoveryResult.ResultType.PLACE) {
                            PlaceLink placeLink = (PlaceLink) result;
                            listener.onCompleted(placeLink.getPosition(), errorCode);
                        }
                    }
                } else {
                    listener.onCompleted(null, errorCode);
                }
            }
        });
    }

    private void getCoordinatesFor(String locationStr, GeoCoordinate center,
                                   final ResultListener<GeoCoordinate> listener) {
        GeocodeRequest req1 = new GeocodeRequest(locationStr);
        req1.setSearchArea(center, GEOCODE_SEARCH_AREA);
        req1.execute(new ResultListener<List<GeocodeResult>>() {
            @Override
            public void onCompleted(List<GeocodeResult> geocodeResults, ErrorCode errorCode) {
                if (errorCode == ErrorCode.NONE && geocodeResults != null && geocodeResults.size() > 0) {
                    Location location = geocodeResults.get(0).getLocation();
                    listener.onCompleted(location.getCoordinate(), errorCode);
                } else {
                    listener.onCompleted(null, errorCode);
                }
            }
        });
    }

    private void notifyErrorToUI() {
        Message msg = UIHandler.obtainMessage(-1);
        msg.sendToTarget();
    }

    private void calculateRoute() {
        SharedPreferences.Editor edit = Application.mainActivity.
                getPreferences(Activity.MODE_PRIVATE).edit();
        edit.putString(Application.LAST_CITY_VISITED, routeData.city);
        edit.putString(Application.LAST_ORIGIN, routeData.origin);
        edit.putString(Application.LAST_DESTINATION, routeData.destination);

        edit.commit();

        GeoCoordinate originCoordinates = getCoordForCity(routeData.originCity);
        final GeoCoordinate destCoordinates = getCoordForCity(routeData.city);

        String originStr = routeData.origin;

        geoCodeLocation(originStr, originCoordinates, new ResultListener<GeoCoordinate>() {
            @Override
            public void onCompleted(GeoCoordinate coords, ErrorCode errorCode) {
                if (errorCode == ErrorCode.NONE) {
                    routeData.originCoordinates = coords;
                    String destinationStr = routeData.destination;
                    geoCodeLocation(destinationStr, destCoordinates, new ResultListener<GeoCoordinate>() {
                        @Override
                        public void onCompleted(GeoCoordinate geoCoordinate, ErrorCode errorCode) {
                            if (errorCode == ErrorCode.NONE) {
                                routeData.destinationCoordinates = geoCoordinate;
                                if (routeData.originCoordinates != null && routeData.destinationCoordinates != null) {
                                    doCalculateRoute(routeData.originCoordinates, routeData.destinationCoordinates);
                                } else {
                                    notifyErrorToUI();
                                }
                            } else {
                                notifyErrorToUI();
                            }
                        }
                    });
                } else {
                    notifyErrorToUI();
                }
            }
        });
    }

    private void checkNextButton(AutoCompleteTextView view) {
        if (currentStep.equals("Origin")) {
            if (origin.getText().length() > 0 && originCity.getText().length() > 0) {
                nextButton.setEnabled(true);
            } else {
                nextButton.setEnabled(false);
            }

            // If city changes then address is reset
            if (originCity.getText().length() == 0 && origin.getText().length() > 0) {
                origin.setText("");
            }
            if (view != null && view.getId() == R.id.originCityInput) {
                origin.setText("");
            }
        } else if (currentStep.equals("Destination")) {
            if (city.getText().length() > 0 && destination.getText().length() > 0) {
                nextButton.setEnabled(true);
            } else {
                nextButton.setEnabled(false);
            }

            if (city.getText().length() == 0 && destination.getText().length() > 0) {
                destination.setText("");
            }
            if (view != null && view.getId() == R.id.cityInput) {
                destination.setText("");
            }
        }
    }

    private GeoCoordinate getCoordForCity(String city) {
        // If city is not known, for the moment we return default coords
        double[] coords = cityCoords.get(city);
        GeoCoordinate geoCoordinates = MainActivity.DEFAULT_COORDS;

        if (coords != null) {
            geoCoordinates = new GeoCoordinate(coords[0], coords[1]);
        }

        return geoCoordinates;
    }

    private class MyTouchListener implements View.OnTouchListener {
        private AutoCompleteTextView field;

        public MyTouchListener(AutoCompleteTextView f) {
            field = f;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (field.getCompoundDrawables()[2] == null) {
                return false;
            }
            if (event.getAction() != MotionEvent.ACTION_UP) {
                return false;
            }
            if (event.getX() > field.getWidth() - field.getPaddingRight() - x.getIntrinsicWidth()) {
                field.setText("");
                field.setCompoundDrawables(y, null, null, null);
            }
            return false;
        }
    }

    private class MyTextWatcher implements TextWatcher, ResultListener<List<String>> {
        private AutoCompleteTextView view;
        private ArrayAdapter<String> adapter;
        // Previous text
        private String lastKnownInput = "";
        private String lastRequestQuery = "";
        private boolean pendingRequest = false;

        public MyTextWatcher(AutoCompleteTextView v, ArrayAdapter<String> a) {
            view = v;
            adapter = a;
        }

        private void checkRemoveButton() {
            view.setCompoundDrawables(y, null,
                    view.getText().toString().equals("") ? null : x, null);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        private void executeSearch(String query) {
            pendingRequest = true;

            String scity = "";
            GeoCoordinate searchCenter = MainActivity.DEFAULT_COORDS;
            if (view.getTag() != null && view.getTag().equals("originAddress")) {
                scity = originCity.getText().toString();

            } else {
                if (view.getTag() != null && view.getTag().equals("destAddress")) {
                    scity = city.getText().toString();
                }
            }

            lastRequestQuery = query;

            searchCenter = getCoordForCity(scity);
            if (query.endsWith(" ")) {
                query += scity;
            }
            TextSuggestionRequest req = new TextSuggestionRequest(query);
            req.setSearchCenter(searchCenter);
            req.setCollectionSize(10);
            req.execute(this);
        }

        @Override
        public void onCompleted(List<String> strings, ErrorCode errorCode) {
            if (lastKnownInput.equals(lastRequestQuery)) {
                if (strings != null) {
                    adapter = new ArrayAdapter<>(activity,
                            android.R.layout.simple_dropdown_item_1line, strings);
                    view.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    pendingRequest = false;
                }
            } else {
                executeSearch(lastKnownInput);
            }
        }


        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            checkRemoveButton();

            checkNextButton(view);

            String tag = (String) view.getTag();
            if (tag == null || tag.indexOf("Address") == -1) {
                return;
            }

            lastKnownInput = s.toString();

            if (pendingRequest) {
                return;
            }

            // Nothing is done if text refines previous text
            if (s.length() >= 4) {
                executeSearch(s.toString());
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

}
