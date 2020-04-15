package hmi.parkinglot.ambient;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hmi.parkinglot.Application;

/**
 * Holds AirQuality parameters
 */
public class AirQualityParameters {
    private static AirQualityParameters instance = new AirQualityParameters();

    private static String THRESHOLDS_URL = "";

    private boolean dataLoaded = false;


    private Values values = new Values();

    private AirQualityParameters() {

    }

    public static AirQualityParameters getInstance() {
        return instance;
    }

    private String getValue(JSONObject obj, String property) throws Exception {
        return obj.getJSONObject(property).getString("value");
    }

    private JSONArray getNgsiv2Data(String type) throws Exception {
        String urlString = THRESHOLDS_URL + "?" + "type" + "=" + type;

        URL url = new URL(urlString);

        Log.d(Application.TAG, "URL: " + urlString);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "FIWARE-HERE-Navigator");
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        connection.connect();

        InputStream inputStream = connection.getInputStream();

        StringBuffer output = new StringBuffer();
        BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = rd.readLine()) != null) {
            output.append(line);
        }

        Log.d(Application.TAG, "Response: " + output.toString());
        JSONArray array = new JSONArray(output.toString());

        return array;
    }

    public synchronized Values getData() throws Exception {
        if (!dataLoaded) {
            doLoadData();
        }
        dataLoaded = true;

        return this.values;
    }

    private void doLoadData() throws Exception {
        JSONArray data = getNgsiv2Data("AirQualityIndexClass");

        for (int j = 0; j < data.length(); j++) {
            JSONObject obj = data.getJSONObject(j);
            String className = getValue(obj, "name");
            Map<String, Object> classData = new HashMap<>();
            classData.put("description", getValue(obj, "description"));
            classData.put("startValue", new Integer(getValue(obj, "startValue")));
            classData.put("endValue", new Integer(getValue(obj, "endValue")));

            this.values.indexClasses.put(className, classData);
        }

        data = getNgsiv2Data("AirQualityThreshold");

        for (int j = 0; j < data.length(); j++) {
            JSONObject obj = data.getJSONObject(j);
            String pollutant = getValue(obj, "pollutant");

            List<Map> pollutantData = this.values.thresholdData.get(pollutant);
            if (pollutantData == null) {
                pollutantData = new ArrayList<Map>();
                this.values.thresholdData.put(pollutant, pollutantData);
            }

            Map<String, Object> newThreshold = new HashMap<>();
            newThreshold.put("indexClass", getValue(obj, "indexClass"));
            newThreshold.put("minConcentration", new Integer(getValue(obj, "minConcentration")));
            newThreshold.put("maxConcentration", new Integer(getValue(obj, "maxConcentration")));

            pollutantData.add(newThreshold);
        }
    }

    public class Values {
        // Cache to hold the data corresponding to thresholds
        public Map<String, List> thresholdData = new HashMap<>();
        public Map<String, Map> indexClasses = new HashMap<>();
    }
}
