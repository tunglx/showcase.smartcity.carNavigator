package hmi.parkinglot.ambient;

import android.os.AsyncTask;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hmi.parkinglot.Application;
import hmi.parkinglot.ResultListener;


/**
 *   Calculates air quality according to the concentration values
 *   measured by sensors
 *
 */
public class AirQualityCalculator extends AsyncTask<Map, Integer, Map> {
    ResultListener<Map<String,Map>> listener;

    protected Map doInBackground(Map... request) {
        Map params = request[0];

        Map out = null;

        try {
            out = getAirQualityIndexes(params);
        }
        catch(Exception e) {
            Log.e(Application.TAG, "Error while calculating air quality index: " + e);
        }

        return out;
    }

    public void setListener(ResultListener<java.util.Map<String, java.util.Map>> listener) {
        this.listener = listener;
    }

    protected void onPostExecute(Map data) {
        listener.onResultReady(data);
    }



    public AirQualityCalculator() {

    }

    /**
     *   Obtains the air quality indexes for different pollutants
     *
     *   A map of values which represent an hourly-based value is passed as parameter
     *
     *   @param attributes
     *   @return
     *
     */
    private Map getAirQualityIndexes(Map<String, Double> attributes) throws Exception {
        AirQualityParameters.Values thresholds = AirQualityParameters.getInstance().getData();

        Map<String, Map> out = new HashMap<>();

        for (String pollutant : attributes.keySet()) {
            double measuredConcentration = attributes.get(pollutant);
            List<Map> pollutantData = thresholds.thresholdData.get(pollutant);

            if(pollutantData != null) {
                String clazz = null;
                double maxConcentration = -1;
                double minConcentration = -1;

                for (Map values : pollutantData) {
                    minConcentration = ((Integer)values.get("minConcentration")).doubleValue();
                    maxConcentration = ((Integer)values.get("maxConcentration")).doubleValue();
                    if (measuredConcentration >= minConcentration &&
                            measuredConcentration <= maxConcentration) {
                        clazz = (String)values.get("indexClass");
                        break;
                    }
                }

                if (clazz != null) {
                    // Now the air quality index can be calculated
                    Map indexCharacteristics = thresholds.indexClasses.get(clazz);

                    double iStart = ((Integer)indexCharacteristics.get("startValue")).doubleValue();
                    double iEnd = ((Integer)indexCharacteristics.get("endValue")).doubleValue();

                    double index = ((iEnd - iStart) / (maxConcentration - minConcentration)) *
                            (measuredConcentration - minConcentration) + iStart;

                    Map<String,Object> result = new HashMap<>();
                    result.put("value", new Integer((int)Math.round(index)));
                    result.put("name", clazz);
                    result.put("description", indexCharacteristics.get("description"));

                    out.put(pollutant, result);
                }

            }
        }

        return out;
    }
}
