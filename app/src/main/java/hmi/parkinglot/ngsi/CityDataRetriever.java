package hmi.parkinglot.ngsi;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPolygon;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import hmi.parkinglot.Application;
import hmi.parkinglot.parking.ParkingAttributes;
import hmi.parkinglot.weather.WeatherAttributes;

/**
 * Retrieves data from the city by calling FIWARE-HERE Adaptor
 */
public class CityDataRetriever extends AsyncTask<CityDataRequest, Integer, Map<String, List<Entity>>> {
    private static String SERVICE_URL = "http://165.22.62.250:1027/v2/entities";
    private static String KEYROCK_URL = "http://165.22.62.250:3005";
    private static String AUTH_KEY_BEARER = "Basic dHV0b3JpYWwtZGNrci1zaXRlLTAwMDAteHByZXNzd2ViYXBwOnR1dG9yaWFsLWRja3Itc2l0ZS0wMDAwLWNsaWVudHNlY3JldA==";

    private CityDataListener listener;
    private final static double INTERPOLATION_UNIT = 0.000045;

    private String getAuthToken(String userName, String password) {
        StringBuffer output = new StringBuffer();
        String token = null;
        InputStream inputStream = null;
        DataOutputStream outputStream = null;
        BufferedReader rd = null;

        StringBuilder authData = new StringBuilder();
        authData.append("grant_type=password" + "&");
        authData.append("username=").append(userName).append("&");
        authData.append("password=").append(password);

        try {
            URL url = new URL(KEYROCK_URL + "/oauth2/token");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Authorization", AUTH_KEY_BEARER);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.connect();

            byte[] postData = authData.toString().getBytes(StandardCharsets.UTF_8);
            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.write(postData);

            inputStream = connection.getInputStream();
            rd = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = rd.readLine()) != null) {
                output.append(line);
            }
            JSONObject object = new JSONObject(output.toString());
            token = object.getString("access_token");

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rd != null) {
                    rd.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return token;
    }

    private HashMap<Integer, String> getOrionData(String urlString) {
        HashMap<Integer, String> result = new HashMap<>();
        InputStream inputStream = null;
        StringBuilder output = new StringBuilder("");
        BufferedReader rd = null;

        if (TextUtils.isEmpty(Application.ACCESS_TOKEN)) {
            result.put(401, "");
            return result;
        }

        try {
            URL url = new URL(urlString);

            Log.d(Application.TAG, "URL: " + urlString);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "FIWARE-HERE-Navigator");
            connection.setRequestProperty("X-Auth-Token", Application.ACCESS_TOKEN);
            connection.setRequestProperty("Connection", "close");
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();

            inputStream = connection.getInputStream();

            rd = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = rd.readLine()) != null) {
                output.append(line);
            }
            Log.d(Application.TAG, "Response: " + output.toString());
            result.put(connection.getResponseCode(), output.toString());
        } catch (IOException e) {
            e.printStackTrace();
            result.put(401, "");
        } finally {
            try {
                if (rd != null) {
                    rd.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    protected Map<String, List<Entity>> doInBackground(CityDataRequest... request) {
        String urlString = createRequestURL(request[0]);

        Map<String, List<Entity>> out = new HashMap<>();
        List<Entity> resultSet = new ArrayList<>();

        HashMap<Integer, String> result = getOrionData(urlString);
        if (result.keySet().size() > 0 && result.keySet().toArray() != null) {
            int responseCode = (int) result.keySet().toArray()[0];
            if (responseCode != 200) {
                Application.ACCESS_TOKEN = getAuthToken("hmi_lab@gmail.com", "hmi123");
                result = getOrionData(urlString);
            }
        }

        if (result.keySet().size() > 0) {
            try {
                int responseCode = (int) result.keySet().toArray()[0];
                JSONArray array = new JSONArray(result.get(responseCode));

                for (int j = 0; j < array.length(); j++) {
                    Entity ent = new Entity();
                    JSONObject obj = array.getJSONObject(j);

                    ent.id = obj.getString("id");
                    ent.type = obj.getString("type");
                    ent.attributes = new HashMap<String, Object>();

                    fillLocation(obj, ent);
                    fillAttributes(obj, ent.type, ent.attributes);

                    if (out.get(ent.type) == null) {
                        out.put(ent.type, new ArrayList<Entity>());
                    }
                    out.get(ent.type).add(ent);

                    resultSet.add(ent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        out.put(Application.RESULT_SET_KEY, resultSet);
        Log.d("tung", "retrieved data: " + out.toString());
        return out;
    }

    private void fillLocation(JSONObject obj, Entity ent) throws JSONException {
        JSONObject location = null;
        String locValue = null;
        JSONArray locArray = null;

        try {
            location = obj.getJSONObject("centroid");
        } catch (JSONException jse) {
            try {
                locValue = obj.getString("centroid");
            } catch (JSONException jse2) {
                try {
                    location = obj.getJSONObject("location");
                } catch (JSONException jse3) {
                    try {
                        locValue = obj.getString("location");
                    } catch (JSONException jsex) {
                        jsex.printStackTrace();
                    }

                    if (locValue != null && locValue.indexOf("[") == 0) {
                        locValue = null;
                    }
                }
            }
        }

        // There could be entities (namely weather entities) without location
        if (location != null) {
            location = location.getJSONObject("value");
            locValue = location.getString("coordinates");
        }
        if (locValue != null) {
            locValue = locValue.replace("[", "");
            locValue = locValue.replace("]", "");
            Log.d("tung", " locValue " + locValue);
            String[] coordinates = locValue.split(",");
            Log.d("tung", "coor " + coordinates[0] + " " + coordinates[1]);

            ent.location = new double[]{Double.parseDouble(coordinates[0]),
                    Double.parseDouble(coordinates[1])};
        }
    }

    private void fillAmbientObserved(JSONObject obj, String type, Map<String, Object> attrs) throws Exception {
        getDoubleJSONAttr(WeatherAttributes.TEMPERATURE, obj, WeatherAttributes.TEMPERATURE, attrs);
        getDoubleJSONAttr(WeatherAttributes.R_HUMIDITY, obj, WeatherAttributes.R_HUMIDITY, attrs);
        getDoubleJSONAttr("noiseLevel", obj, "noiseLevel", attrs);

        getStringJSONAttr("created", obj, "created", attrs);
        getStringJSONAttr("sensorType", obj, "sensorType", attrs);

        try {
            JSONObject pollutants = obj.getJSONObject("pollutants");
            for (String pollutant : Application.POLLUTANTS) {
                try {
                    double value =
                            pollutants.getJSONObject(pollutant).getDouble("concentration");
                    attrs.put(pollutant, value);
                } catch (JSONException jsoe) {
                    jsoe.printStackTrace();
                }
            }
        } catch (JSONException jsoe) {
            jsoe.printStackTrace();
        }
    }

    private void fillParkingRestriction(JSONObject obj, String type,
                                        Map<String, Object> attrs) throws Exception {
        // We obtain location
        attrs.put("polygon", getPolygon(obj.getString("location")));
    }

    private void fillParking(JSONObject obj, String type,
                             Map<String, Object> attrs) throws Exception {
        getIntegerJSONAttr(ParkingAttributes.AVAILABLE_SPOTS, obj,
                ParkingAttributes.AVAILABLE_SPOTS, attrs);
        getIntegerJSONAttr(ParkingAttributes.TOTAL_SPOTS, obj, ParkingAttributes.TOTAL_SPOTS, attrs);
//        getStringJSONAttr("name", obj, "name", attrs);
//        getStringJSONAttr("description", obj, "description", attrs);
        if (type.equals(Application.STREET_PARKING_TYPE)) {
            boolean isArray = true;
            JSONArray polygons = null;

            List<GeoPolygon> location = new ArrayList<GeoPolygon>();
            try {
                polygons = obj.getJSONArray("location").getJSONArray(0);
            } catch (JSONException jsoe) {
                isArray = false;
//                jsoe.printStackTrace();
            }
            if (isArray == true) {
                int total = polygons.length();
                for (int j = 0; j < total; j++) {
                    JSONArray polygon = polygons.getJSONArray(j);
                    List<GeoCoordinate> geoPolygon = new ArrayList<GeoCoordinate>();
                    for (int x = 0; x < polygon.length(); x++) {
                        double lat = Double.parseDouble(polygon.getJSONArray(x).getString(0));
                        double lon = Double.parseDouble(polygon.getJSONArray(x).getString(1));
                        geoPolygon.add(new GeoCoordinate(lat, lon));
                    }
                    location.add(new GeoPolygon(geoPolygon));
                }
            } else {
                JSONObject loc;
                loc = obj.getJSONObject("location");
                loc = loc.getJSONObject("value");
                String locValue = loc.getString("coordinates");
                locValue = locValue.replace("[", "");
                locValue = locValue.replace("]", "");
                location.add(getPolygonFromPoint(locValue));
            }
            attrs.put("polygon", location);
        }
    }

    private void fillAttributes(JSONObject obj, String type,
                                Map<String, Object> attrs) throws Exception {
        if (type.equals("TrafficEvent")) {

        } else if (type.equals(Application.AMBIENT_OBSERVED_TYPE)) {
            fillAmbientObserved(obj, type, attrs);
        } else if (type.equals(Application.PARKING_LOT_TYPE) ||
                type.equals(Application.PARKING_TYPE) ||
                type.equals(Application.STREET_PARKING_TYPE) ||
                type.equals((Application.PARKING_LOT_ZONE_TYPE))) {
            fillParking(obj, type, attrs);
        } else if (type.equals("CityEvent")) {

        } else if (type.equals(Application.AMBIENT_AREA_TYPE)) {
            fillAmbientArea(obj, type, attrs);
        } else if (type.equals(Application.WEATHER_FORECAST_TYPE)) {
            fillWeather(obj, type, attrs);
        } else if (type.equals(Application.GARAGE_TYPE)) {
            fillGarage(obj, type, attrs);
        } else if (type.equals(Application.GAS_STATION_TYPE)) {
            fillGasStation(obj, type, attrs);
        } else if (type.equals(Application.PARKING_RESTRICTION_TYPE)) {
            fillParkingRestriction(obj, type, attrs);
        }
    }

    private void fillGarage(JSONObject obj, String type,
                            Map<String, Object> attrs) throws Exception {
        getStringJSONAttr("name", obj, null, attrs);
    }

    private void fillGasStation(JSONObject obj, String type,
                                Map<String, Object> attrs) throws Exception {
        getStringJSONAttr("name", obj, null, attrs);
    }

    private void fillWeather(JSONObject obj, String type,
                             Map<String, Object> attrs) throws Exception {
        getDoubleJSONAttr(WeatherAttributes.TEMPERATURE, obj, null, attrs);
        getDoubleJSONAttr(WeatherAttributes.R_HUMIDITY, obj, null, attrs);
        getCompoundJSONAttr(WeatherAttributes.MAXIMUM, obj, null, attrs);
        getCompoundJSONAttr(WeatherAttributes.MINIMUM, obj, null, attrs);

        getCompoundJSONAttr(WeatherAttributes.VALIDITY, obj, null, attrs);

        getDoubleJSONAttr("windSpeed", obj, null, attrs);
        getStringJSONAttr("windDirection", obj, null, attrs);
        getStringJSONAttr("weatherType", obj, null, attrs);

        getDoubleJSONAttr(WeatherAttributes.POP, obj, null, attrs);
    }

    private void fillAmbientArea(JSONObject obj, String type,
                                 Map<String, Object> attrs) throws Exception {

        JSONArray array = obj.getJSONArray("location");
        List<GeoCoordinate> coordinates = new ArrayList<>();

        for (int j = 0; j < array.length(); j++) {
            String coordPair = array.getString(j);
            String[] list = coordPair.split(",");
            GeoCoordinate coord = new GeoCoordinate(Double.parseDouble(list[0]),
                    Double.parseDouble(list[1]));
            coordinates.add(coord);
        }
        attrs.put("polygon", new GeoPolygon(coordinates));
    }

    private GeoPolygon getPolygon(String coords) {
        String[] polygonCoords = coords.split(",");

        List<GeoCoordinate> geoPolygon = new ArrayList<GeoCoordinate>();
        for (int j = 0; j < polygonCoords.length; j += 2) {
            double lat = Double.parseDouble(polygonCoords[j]);
            double lon = Double.parseDouble(polygonCoords[j + 1]);
            geoPolygon.add(new GeoCoordinate(lat, lon));
        }

        if (geoPolygon.size() > 0) {
            GeoCoordinate last = geoPolygon.get(geoPolygon.size() - 1);
            GeoCoordinate first = geoPolygon.get(0);

            if (last.getLatitude() != first.getLatitude() ||
                    last.getLongitude() != first.getLongitude()) {
                geoPolygon.add(new GeoCoordinate(first));
            }
        }

        return new GeoPolygon(geoPolygon);
    }

    private GeoPolygon getPolygonFromPoint(String coords) {
        String[] polygonCoords = coords.split(",");
        double latCenter = Double.parseDouble(polygonCoords[1]);
        double lonCenter = Double.parseDouble(polygonCoords[0]);

        List<GeoCoordinate> geoPolygon = new ArrayList<GeoCoordinate>();
        geoPolygon.add(new GeoCoordinate((latCenter - INTERPOLATION_UNIT), (lonCenter + INTERPOLATION_UNIT)));
        geoPolygon.add(new GeoCoordinate((latCenter + INTERPOLATION_UNIT), (lonCenter + INTERPOLATION_UNIT)));
        geoPolygon.add(new GeoCoordinate((latCenter + INTERPOLATION_UNIT), (lonCenter - INTERPOLATION_UNIT)));
        geoPolygon.add(new GeoCoordinate((latCenter - INTERPOLATION_UNIT), (lonCenter - INTERPOLATION_UNIT)));

        if (geoPolygon.size() > 0) {
            GeoCoordinate last = geoPolygon.get(geoPolygon.size() - 1);
            GeoCoordinate first = geoPolygon.get(0);

            if (last.getLatitude() != first.getLatitude() ||
                    last.getLongitude() != first.getLongitude()) {
                geoPolygon.add(new GeoCoordinate(first));
            }
        }

        return new GeoPolygon(geoPolygon);
    }

    private String getTypes(List<String> types) {
        StringBuffer out = new StringBuffer();

        for (int j = 0; j < types.size(); j++) {
            out.append(types.get(j));
            if (j + 1 < types.size()) {
                out.append(",");
            }
        }

        return out.toString();
    }

    private String createRequestURL(CityDataRequest req) {
        String geometry = req.geometry;
        if (geometry == null) {
            geometry = "point";
        }

        String geoRelStr;
        if (req.radius != -1) {
            geoRelStr = "&georel=near;maxDistance:" + req.radius;
        } else if ("intersects".equals(req.georel)) {
            geoRelStr = "&georel=intersects";
        } else {
            geoRelStr = "&georel=coveredBy";
        }

        String coords = "";
        if (req.coordinates != null) {
            coords = req.coordinates[0] + "," + req.coordinates[1];
        } else if (req.polygon != null) {
            for (int j = 0; j < req.polygon.getNumberOfPoints(); j++) {
                GeoCoordinate point = req.polygon.getPoint(j);
                coords += point.getLatitude() + "," + point.getLongitude();
                coords += ",";
            }
            coords = coords.substring(0, coords.length() - 1);
        }

        String out = SERVICE_URL + "?" + "coords=" + coords
                + geoRelStr + "&type=" + getTypes(req.types) + "&geometry=" + geometry;

        if (req.token != null && req.token.length() > 0) {
            out += "&token=" + req.token;
        }

        return out;
    }


    private void getDoubleJSONAttr(String attr, JSONObject obj, String mAttr,
                                   Map<String, Object> attrs) {
        Double out = null;
        String mappedAttr = mAttr != null ? mAttr : attr;

        try {
            out = obj.getDouble(attr);
            attrs.put(mappedAttr, out);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getCompoundJSONAttr(String attr, JSONObject obj, String mAttr,
                                     Map<String, Object> attrs) {

        String mappedAttr = mAttr != null ? mAttr : attr;

        try {
            JSONObject data = obj.getJSONObject(attr);
            Iterator<String> keys = data.keys();
            Map<String, Object> values = new HashMap<>();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = data.get(key);
                if (value instanceof Integer) {
                    Integer ivalue = (Integer) value;
                    value = new Double(ivalue.doubleValue());
                }
                values.put(key, value);
            }

            attrs.put(mappedAttr, values);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getIntegerJSONAttr(String attr, JSONObject obj, String mAttr,
                                    Map<String, Object> attrs) {
        Integer out = null;
        String mappedAttr = mAttr != null ? mAttr : attr;
        JSONObject child;

        try {
            child = obj.getJSONObject(attr);
            out = child.getInt("value");
            attrs.put(mappedAttr, out);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d("tung", "IntegerOutput: " + attr + " : " + out);
    }

    private void getStringJSONAttr(String attr, JSONObject obj, String mAttr,
                                   Map<String, Object> attrs) {
        String out = null;
        String mappedAttr = mAttr != null ? mAttr : attr;
        JSONObject child;

        try {
            child = obj.getJSONObject(attr);
            out = child.getString("value");
            attrs.put(mappedAttr, out);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d("tung", "StringOutput: " + attr + " : " + out);
    }


    public void setListener(CityDataListener listener) {
        this.listener = listener;
    }

    protected void onPostExecute(Map<String, List<Entity>> data) {
        listener.onCityDataReady(data);
    }
}
