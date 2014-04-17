


/*
 * Copyright (C) 2014 Francesco Azzola
 *  Surviving with Android (http://www.survivingwithandroid.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.survivingwithandroid.weather.lib.provider.wunderground;

import android.util.Log;

import com.survivingwithandroid.weather.lib.WeatherConfig;
import com.survivingwithandroid.weather.lib.exception.ApiKeyRequiredException;
import com.survivingwithandroid.weather.lib.exception.WeatherLibException;
import com.survivingwithandroid.weather.lib.model.City;
import com.survivingwithandroid.weather.lib.model.CurrentWeather;
import com.survivingwithandroid.weather.lib.model.DayForecast;
import com.survivingwithandroid.weather.lib.model.Location;
import com.survivingwithandroid.weather.lib.model.Weather;
import com.survivingwithandroid.weather.lib.model.WeatherForecast;
import com.survivingwithandroid.weather.lib.provider.IWeatherCodeProvider;
import com.survivingwithandroid.weather.lib.provider.IWeatherProvider;
import com.survivingwithandroid.weather.lib.util.WeatherUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class WeatherUndergroundProvider implements IWeatherProvider {


    private static String BASE_URL_ID = "http://api.wunderground.com/api";
    private static String IMG_URL = "http://openweathermap.org/img/w/";
    private static String SEARCH_URL = "http://autocomplete.wunderground.com/aq?query=";
    private static String SEARCH_URL_GEO = "http://api.openweathermap.org/data/2.5/find?mode=json&type=accurate";
    private static String BASE_FORECAST_URL_ID = "http://api.wunderground.com/api";


    private WeatherConfig config;
    private Weather.WeatherUnit units = new Weather.WeatherUnit();
    private IWeatherCodeProvider codeProvider;
    private WeatherForecast forecast = new WeatherForecast();

    public CurrentWeather getCurrentCondition(String data) throws WeatherLibException {
        //LogUtils.LOGD("JSON CurrentWeather ["+data+"]");
        CurrentWeather weather = new CurrentWeather();
        try {
        // We create out JSONObject from the data
         JSONObject rootObj = new JSONObject(data);
         JSONObject jObj = getObject("current_observation", rootObj);

         // We start extracting the info

         JSONObject dObj = getObject("display_location", jObj);

         Location loc = new Location();
         loc.setLatitude(getFloat("lat", dObj));
         loc.setLongitude(getFloat("lon", dObj));
         loc.setCountry(getString("state_name", dObj));
         loc.setCity(getString("city", dObj));
            /*
         loc.setSunrise(getInt("sunrise", sysObj));
         loc.setSunset(getInt("sunset", sysObj));
         */
          weather.location = loc;
         //weather.currentCondition.setWeatherId(getInt("id", JSONWeather));

         // Convert internal code
            /*
          if (codeProvider != null)
              weather.currentCondition.setWeatherCode(codeProvider.getWeatherCode(weather.currentCondition.getWeatherId()));
          */

         weather.currentCondition.setDescr(getString("weather", jObj));
         //weather.currentCondition.setCondition(getString("main", JSONWeather));
         weather.currentCondition.setIcon(getString("icon", jObj));

         //JSONObject mainObj = getObject("main", jObj);
         weather.currentCondition.setHumidity(getInt("relative_humidity", jObj));
         weather.wind.setDeg(getFloat("deg", jObj));
         weather.currentCondition.setPressureTrend(getInt("pressure_trend", jObj));

         if (WeatherUtility.isMetric(config.unitSystem)) {
             weather.currentCondition.setPressure(getInt("pressure_mb", jObj));
            // weather.temperature.setMaxTemp(getFloat("temp_max", mainObj));
            // weather.temperature.setMinTemp(getFloat("temp_min", mainObj));
             weather.temperature.setTemp(getFloat("temp_c", jObj));
             // Wind

             weather.wind.setGust(getFloat("wind_gust_kph", jObj));
             weather.wind.setSpeed(getFloat("wind_kph", jObj));
             weather.currentCondition.setVisibility(getFloat("visibility_km", jObj));
        }
        else {
             weather.currentCondition.setPressure(getInt("pressure_in", jObj));
             // weather.temperature.setMaxTemp(getFloat("temp_max", mainObj));
             // weather.temperature.setMinTemp(getFloat("temp_min", mainObj));
             weather.temperature.setTemp(getFloat("temp_f", jObj));
             // Wind

             weather.wind.setGust(getFloat("wind_gust_mph", jObj));
             weather.wind.setSpeed(getFloat("wind_mph", jObj));
             weather.currentCondition.setVisibility(getFloat("visibility_mi", jObj));

         }

            parseForecast(rootObj, weather);

        }
     catch(JSONException json) {
         json.printStackTrace();
         throw new WeatherLibException(json);
     }
       weather.setUnit(units);

       return weather;
	}

	
	public WeatherForecast getForecastWeather(String data) throws WeatherLibException  {
        if (forecast == null) {
            try {
                JSONObject rootObj = new JSONObject(data);
                parseForecast(rootObj, new CurrentWeather());
            }
            catch(JSONException json) {
                json.printStackTrace();
                throw new WeatherLibException(json);
            }
        }
		return forecast;
	}

    private void parseForecast(JSONObject root, CurrentWeather weather) throws JSONException {
        // Start parsing forecast
        JSONObject forecast1 = getObject("forecast", root);
        JSONObject simpleForecast = getObject("simpleforecast", forecast1);
        JSONArray jArr = simpleForecast.getJSONArray("forecastday");

        for (int i=0; i < jArr.length(); i++) {
            JSONObject dayForecast = jArr.getJSONObject(i);
            DayForecast df = new DayForecast();
            df.timestamp = dayForecast.getLong("epoch");
            df.weather.currentCondition.setDescr(dayForecast.getString("conditions"));
            df.weather.currentCondition.setIcon(dayForecast.getString("icon"));
            if (WeatherUtility.isMetric(config.unitSystem)) {
                df.forecastTemp.max = dayForecast.getJSONObject("high").getInt("celsius");
                df.forecastTemp.min = dayForecast.getJSONObject("low").getInt("celsius");
                df.weather.wind.setSpeed(dayForecast.getJSONObject("avewind").getInt("kph"));
                df.weather.snow.setTime("Day");
                df.weather.snow.setAmmount(dayForecast.getJSONObject("snow_allday").getInt("cm"));
                df.weather.rain.setTime("Day");
                df.weather.rain.setAmmount(dayForecast.getJSONObject("qpf_allday").getInt("cm"));
            }
            else {
                df.forecastTemp.max = dayForecast.getJSONObject("high").getInt("fahrenheit");
                df.forecastTemp.min = dayForecast.getJSONObject("low").getInt("fahrenheit");
                df.weather.wind.setSpeed(dayForecast.getJSONObject("avewind").getInt("mph"));
                df.weather.snow.setTime("Day");
                df.weather.snow.setAmmount(dayForecast.getJSONObject("snow_allday").getInt("in"));
                df.weather.rain.setTime("Day");
                df.weather.rain.setAmmount(dayForecast.getJSONObject("qpf_allday").getInt("in"));
            }

            df.weather.wind.setDeg(dayForecast.getJSONObject("avewind").getInt("degrees"));
            if (i == 0) {
                weather.temperature.setMinTemp(df.forecastTemp.min);
                weather.temperature.setMaxTemp(df.forecastTemp.max);
            }

            forecast.addForecast(df);
        }
    }
	
	
	public List<City> getCityResultList(String data) throws WeatherLibException {
        List<City> cityList = new ArrayList<City>();
       // Log.d("SwA", "Data ["+data+"]");
        try {

            JSONObject jObj = new JSONObject(data);
            JSONArray jArr = jObj.getJSONArray("RESULTS");


            for (int i = 0; i < jArr.length(); i++) {
                JSONObject obj = jArr.getJSONObject(i);

                String name = obj.getString("name");
                String id = obj.getString("l");
                String country = obj.getString("c");
                Log.d("SwA", "ID ["+id+"]");
                City c = new City(id, name, null, country);

                cityList.add(c);
            }
        }
        catch(JSONException json) {
            throw new WeatherLibException(json);
        }
		
		return cityList;
	}

    @Override
    public void setConfig(WeatherConfig config) {
        this.config = config;
        units = WeatherUtility.createWeatherUnit(config.unitSystem);
    }

    @Override
    public String getQueryCityURL(String cityNamePattern) {

        if (config.ApiKey == null)
            throw new ApiKeyRequiredException();

        cityNamePattern = cityNamePattern.replaceAll(" ", "%20");
        return SEARCH_URL + cityNamePattern; // + "&cnt=" + config.maxResult;
    }

    @Override
    public String getQueryCurrentWeatherURL(String cityId) {
        if (config.ApiKey == null)
            throw new ApiKeyRequiredException();

        return BASE_URL_ID + "/" + config.ApiKey + "/forecast/conditions/q/" + cityId + ".json";
    }

    @Override
    public String getQueryForecastWeatherURL(String cityId) {
        if (config.ApiKey == null)
            throw new ApiKeyRequiredException();
        return BASE_FORECAST_URL_ID  + "/" + config.ApiKey + "/forecast/conditions/q/" + cityId + ".json";
    }

    @Override
    public String getQueryImageURL(String icon) throws ApiKeyRequiredException {
        return IMG_URL + icon + ".png";
    }

    @Override
    public void setWeatherCodeProvider(IWeatherCodeProvider codeProvider) {
        this.codeProvider = codeProvider;
    }

    @Override
    public String getQueryCityURLByLocation(android.location.Location location) throws ApiKeyRequiredException {
        return SEARCH_URL_GEO + "&lat=" + location.getLatitude() + "&lon=" + location.getLongitude() + "&cnt=3";
    }

    private static JSONObject getObject(String tagName, JSONObject jObj)  throws JSONException {
		JSONObject subObj = jObj.getJSONObject(tagName);
		return subObj;
	}
	
	private static String getString(String tagName, JSONObject jObj) throws JSONException {
		return jObj.getString(tagName);
	}

	private static float  getFloat(String tagName, JSONObject jObj) throws JSONException {
		return (float) jObj.getDouble(tagName);
	}
	
	private static int  getInt(String tagName, JSONObject jObj) throws JSONException {
		return jObj.getInt(tagName);
	}
	
}
