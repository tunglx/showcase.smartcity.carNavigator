package hmi.parkinglot.render;

import android.speech.tts.TextToSpeech;
import android.view.View;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.mapping.Map;

import java.util.List;

import hmi.parkinglot.ngsi.Entity;

/**
 *   This class holds data for the SmartCityHandler which will launch
 *   the rendering process
 *
 */
public class SmartCityRequest {
    public Map map;
    public TextToSpeech tts;
    public GeoCoordinate loc;
    public java.util.Map<String, List<Entity>> data;
    public View oascView;
}
