package com.deitel.weatherviewer;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Class displaying 16-day weather forecast for the chosen city
 */
public class MainActivity extends AppCompatActivity {
    // list of Weather objects containing weather forecast
    private List<Weather> weatherList = new ArrayList<>();

    // adapter ArrayAdapter binding Weather objects with the ListView list
    private WeatherArrayAdapter weatherArrayAdapter;
    private ListView weatherListView;   // displays weather info

    // configure the Toolbar, ListView, and FloatingActionButton objects
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        weatherListView = (ListView) findViewById(R.id.weatherListView);
        weatherArrayAdapter = new WeatherArrayAdapter(this, weatherList);
        weatherListView.setAdapter(weatherArrayAdapter);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // read out text from the locationEditText object and create a URL object calling the web service
                EditText locationEditText = (EditText) findViewById(R.id.locationEditText);
                URL url = createURL(locationEditText.getText().toString());

                // hide the keyboard and launch GetWeatherTask, which downloads weather forecast data from OpenWeatherMap.org in a separate thread
                if (url != null) {
                    dismissKeyboard(locationEditText);
                    GetWeatherTask getLocalWeatherTask = new GetWeatherTask();
                    getLocalWeatherTask.execute(url);
                }
                else {
                    Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.invalid_url, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    // hides on-screen keyboard
    private void dismissKeyboard(View view){
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    // using the city name create a URL request for openweathermap.org
    private URL createURL (String city) {
        String apiKey = getString(R.string.api_key);
        String baseUrl = getString(R.string.web_service_url);

        try {
            // create a URL address for entered city and metric units (Celsius degrees)
            String urlString = baseUrl + URLEncoder.encode(city, "UTF-8") + "&units=metric&lang=en&cnt=16&APPID=" + apiKey;
            return new URL(urlString);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;    // error creating the URL address
    }

    // generates a REST request in order to obtain weather forecast data and store it in a local JSON object
    private class GetWeatherTask extends AsyncTask<URL, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(URL... urls) {
            HttpURLConnection connection = null;

            try {
                connection = (HttpURLConnection) urls[0].openConnection();
                int response = connection.getResponseCode();

                if (response == HttpURLConnection.HTTP_OK) {
                    StringBuilder builder = new StringBuilder();

                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()))) {
                        String line;

                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                    }
                    catch (IOException e) {
                        Snackbar.make(findViewById(R.id.coordinatorLayout),
                                R.string.read_error, Snackbar.LENGTH_LONG).show();

                        e.printStackTrace();
                    }

                    return new JSONObject(builder.toString());
                }
                else {
                    Snackbar.make(findViewById(R.id.coordinatorLayout),
                            R.string.connect_error, Snackbar.LENGTH_LONG).show();
                }
            }
            catch (Exception e) {
                Snackbar.make(findViewById(R.id.coordinatorLayout),
                        R.string.connect_error, Snackbar.LENGTH_LONG).show();
                e.printStackTrace();
            }
            finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            return null;
        }

        // process the response in form of a JSON object and update the ListView object
        @Override
        protected void onPostExecute(JSONObject weather) {
            convertJSONtoArrayList(weather);    // refill the weatherList
            weatherArrayAdapter.notifyDataSetChanged(); // refresh bindings of ListView elements
            weatherListView.smoothScrollToPosition(0); // scroll to top
        }
    }

    private void convertJSONtoArrayList(JSONObject forecast) {
        weatherList.clear();    // clear old weather data

        try {
            // read the "list" of weather forecast - JSONArray
            JSONArray list = forecast.getJSONArray("list");

            // transform each list element into a Weather object
            for (int i = 0; i < list.length(); ++i) {
                JSONObject day = list.getJSONObject(i); // read data concerning one day

                // read data concerning temperature in that day from JSONObject
                JSONObject temperatures = day.getJSONObject("main");

                // read description and weather icon from JSONObject
                JSONObject weather = day.getJSONArray("weather").getJSONObject(0);

                // add new Weather object to weatherList
                weatherList.add(new Weather(
                        day.getLong("dt"),  // timestamp
                        temperatures.getDouble("temp_min"),  // minimal temperature
                        temperatures.getDouble("temp_max"),  // maximal temperature
                        temperatures.getDouble("humidity"),  // humidity
                        weather.getString("description"), // weather conditions
                        weather.getString("icon")));    // icon name
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
