package fiware.smartcity;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.mapping.MapMarker;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import fiware.smartcity.ambient.AmbientAreaRenderer;
import fiware.smartcity.navigation.SpeechMessage;
import fiware.smartcity.weather.WeatherAttributes;
import fiware.smartcity.weather.WeatherTypes;

/**
 *
 *  Utilities class
 *
 *
 */
public class Utilities {
    public static class AirQualityData {
        public String asString;
        public java.util.Map worstIndex;
    }

    public static class WeatherObservedData {
        public Double temperature = null;
        public Double humidity = null;
    }

    public static AirQualityData getAirQualityData(java.util.Map<String, java.util.Map> result) {
        AirQualityData out = new AirQualityData();

        int maxIndex = -1;
        // Lets see what is the greatest AQI and then paint accordingly
        java.util.Map<String,Object> targetAqi = null;

        StringBuffer markerOut = new StringBuffer();
        for (String aqiInfo : result.keySet()) {
            java.util.Map<String,Object> indexData = result.get(aqiInfo);

            int value = (Integer)indexData.get("value");
            String levelName = (String)indexData.get("description");

            markerOut.append(aqiInfo).append(": ").
                    append(value).append(". ").append(levelName).append("\n");

            if (value > maxIndex) {
                maxIndex = value;
                targetAqi = indexData;
            }
        }

        String aux = markerOut.toString();
        if (aux.length() > 0) {
            out.asString = markerOut.substring(0, aux.length() - 1);
        }
        out.worstIndex = targetAqi;

        return out;
    }

    public static MapMarker buildSensorMarker(GeoCoordinate coords, String title, String desc) {
        Image sensorImg = new Image();
        try {
            sensorImg.setImageResource(R.drawable.sensor2);
        }
        catch(IOException e) {
            Log.e(Application.TAG, "Cannot load image: " + e);
        }
        MapMarker marker = new MapMarker(coords, sensorImg);

        marker.setTitle(title);
        marker.setDescription(desc);

        return marker;
    }

    public static void updateWeather(Map<String, Object> data, View v) {
        v.setVisibility(RelativeLayout.VISIBLE);

        Map maximumValues = (Map)data.get(WeatherAttributes.MAXIMUM);

        if (maximumValues != null) {
            Double maxTemp = (Double)maximumValues.get(WeatherAttributes.TEMPERATURE);

            if (maxTemp != null) {
                TextView tv = (TextView)v.findViewById(R.id.maxTemperature);
                tv.setText(formatDouble(Math.floor(maxTemp)));
            }

            Double maxH = (Double)maximumValues.get(WeatherAttributes.R_HUMIDITY);

            if (maxH != null) {
                v.findViewById(R.id.forecastedHumidity).setVisibility(RelativeLayout.VISIBLE);
                TextView tv = (TextView)v.findViewById(R.id.maxHumidity);
                tv.setText(String.valueOf((long)(maxH * 100)));
            }
        }

        Map minimumValues = (Map)data.get(WeatherAttributes.MINIMUM);

        if (minimumValues != null) {
            Double minTemp = (Double)minimumValues.get(WeatherAttributes.TEMPERATURE);

            if (minTemp != null) {
                TextView tv = (TextView)v.findViewById(R.id.minTemperature);
                tv.setText(String.valueOf(formatDouble(Math.floor(minTemp))));
            }

            Double minH = (Double)minimumValues.get(WeatherAttributes.R_HUMIDITY);

            if (minH != null) {
                TextView tv = (TextView)v.findViewById(R.id.minHumidity);
                tv.setText(String.valueOf((long)(minH * 100)));
            }
        }

        Double temperature = (Double)data.get(WeatherAttributes.TEMPERATURE);

        if (temperature != null) {
            TextView tv = (TextView)v.findViewById(R.id.currentTemperature);
            tv.setText((long) temperature.doubleValue() + "ºC");
        }

        Double humidity = (Double)data.get(WeatherAttributes.R_HUMIDITY);

        if (humidity != null) {
            TextView tv = (TextView)v.findViewById(R.id.currentHumidity);
            if (humidity < 1) {
                humidity *= 100;
            }
            tv.setText((long)humidity.doubleValue() + "%");
        }

        Double windSpeed = (Double)data.get(WeatherAttributes.WIND_SPEED);

        if (windSpeed != null) {
            TextView tv = (TextView)v.findViewById(R.id.windSpeed);
            tv.setText((long)windSpeed.doubleValue() + "Km/h");
        }

        String windDirection = (String)data.get(WeatherAttributes.WIND_DIRECTION);
        if (windDirection != null) {
            TextView tv = (TextView)v.findViewById(R.id.windDirection);
            tv.setText(windDirection);
        }

        Double pop = (Double)data.get(WeatherAttributes.POP);
        if (pop != null) {
            v.findViewById(R.id.forecastedPrecipitation).setVisibility(RelativeLayout.VISIBLE);
            TextView tv = (TextView)v.findViewById(R.id.pop);
            tv.setText((long)(pop * 100) + "%");
        }

        String weatherType = (String)data.get(WeatherAttributes.WEATHER_TYPE);
        Log.d(Application.TAG, "Weather Type: " + weatherType);
        String icon = WeatherTypes.getIcon(weatherType);
        int id = Application.mainActivity.getResources().getIdentifier(icon, "drawable",
                Application.mainActivity.getPackageName());

        if ( id != 0) {
            ((ImageView)v.findViewById(R.id.forecastedWeatherType)).setImageResource(id);
        }
    }

    public static void updateWeatherObserved(WeatherObservedData data, View v) {
        if (data.temperature != null) {
            TextView tv = (TextView)v.findViewById(R.id.currentTemperature);
            tv.setText((long) data.temperature.doubleValue() + "ºC");
            showThunder(v, R.id.thunder_temperature);
        }

        if (data.humidity != null) {
            TextView tv = (TextView)v.findViewById(R.id.currentHumidity);
            if (data.humidity < 1) {
                data.humidity *= 100;
            }
            tv.setText((long)data.humidity.doubleValue() + "%");
            showThunder(v, R.id.thunder_humidity);
        }
    }

    public static void showThunder(View v, int id) {
        final ImageView thunder = (ImageView)v.findViewById(id);
        thunder.setVisibility(View.VISIBLE);
        final int interval2 = 3000; // 2 Second
        Handler handler2 = new Handler();
        Runnable runnable2 = new Runnable() {
            public void run() {
                thunder.setVisibility(View.GONE);
            }
        };
        handler2.postDelayed(runnable2, interval2);
    }

    public static void updateAirPollution(Map<String, Map> data, LinearLayout parent) {
        if (data.size() == 0) {
            return;
        }

        parent.removeAllViews();

        // Now all the views are re-created
        for (String pollutant: data.keySet()) {
            LinearLayout container = new LinearLayout(Application.mainActivity);

            container.setOrientation(LinearLayout.HORIZONTAL);
            container.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

            TextView label = new TextView(Application.mainActivity);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.25f);
            labelParams.setMargins(1, 1, 1, 1);
            label.setLayoutParams(labelParams);

            label.setPadding(1, 1, 1, 1);
            label.setText(pollutant);
            label.setTextColor(Application.mainActivity.getResources().getColor(R.color.blue_text_oasc));
            label.setTypeface(null, Typeface.BOLD);

            container.addView(label);

            TextView value = new TextView(Application.mainActivity);
            LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.75f);
            valueParams.setMargins(1,1,1,1);
            value.setLayoutParams(valueParams);
            value.setPadding(3, 3, 3, 3);
            value.setGravity(Gravity.CENTER_HORIZONTAL);

            container.addView(value);

            Map indexData = data.get(pollutant);
            value.setText((String) indexData.get("description"));
            value.setTextColor(Application.mainActivity.getResources().getColor(R.color.white));

            value.setBackgroundColor(Color.parseColor(
                    AmbientAreaRenderer.AREA_COLORS.get(indexData.get("name"))));
            value.setTypeface(null, Typeface.BOLD);

            parent.addView(container);
        }

        showThunder((View) parent.getParent(), R.id.thunder_air_quality);
    }

    public static String formatDouble(Double d) {
        double dd = d.doubleValue();

        String out;

        if (dd == (long) dd) {
            out = String.format("%d", (long) dd);
        }
        else {
            out = String.format("%s",dd);
        }

        return out;
    }

    // Ensures that speaking is not disturbing user
    public static void speak(TextToSpeech tts, List<SpeechMessage> text) {
        if (Application.isSpeaking) {
            return;
        }

        long now = new DateTime().getMillis();

        if (now - Application.lastTimeSpeak > Application.SPEAK_INTERVAL) {
            // Only an entity end will reset isSpeaking variable
            Application.isSpeaking = true;
            for (SpeechMessage msg : text) {
                if (msg.message != null) {
                    tts.speak(msg.message, TextToSpeech.QUEUE_ADD, null, msg.transactionId);
                }
                if (msg.silence > 0) {
                    tts.playSilentUtterance(msg.silence, TextToSpeech.QUEUE_ADD, msg.transactionId);
                }
            }

            // Now is time to mark the end of the transaction
            tts.playSilentUtterance(100, TextToSpeech.QUEUE_ADD, "Entity_End");
        }

    }

    public static void showBubble(MapMarker marker) {
        DateTime now = DateTime.now();
        if (now.getMillis() - Application.lastTimeBubble > 3 * 1000) {
            marker.showInfoBubble();
            Application.lastTimeBubble = now.getMillis();
        }
    }

    // Ensures that no zoom level change is done very frequently
    // so that user is not disturbed

}
