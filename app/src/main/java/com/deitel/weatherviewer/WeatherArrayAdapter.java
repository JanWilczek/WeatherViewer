package com.deitel.weatherviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class used to display elements of <c>List<Weather></c> object in the <c>ListView</c> view.
 */
public class WeatherArrayAdapter extends ArrayAdapter<Weather>{

    /**
     * Class aiding in reuse of view removed from the screen when scrolling through list elements.
     */
    private static class ViewHolder {
        ImageView conditionImageView;
        TextView dayTextView;
        TextView lowTextView;
        TextView hiTextView;
        TextView humidityTextView;
    }

    // contains the downloaded bitmaps enabling their reuse
    private Map<String, Bitmap> bitmaps = new HashMap<>();

    public WeatherArrayAdapter(Context context, List<Weather> forecast){
        super(context, -1, forecast);
    }

    // creates modified views for ListView elements
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // get access to a Weather object proper to given element of ListView
        Weather day = getItem(position);

        ViewHolder viewHolder; // object relating to list elements' views

        // check if a reusable ViewHolder object exists attached to the element of the ListView
        // list removed from screen; if such an object does not exist, then create new ViewHolder object
        if (convertView == null) {
            // no reusable ViewHolder object
            // create new ViewHolder object
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_item, parent, false);
            viewHolder.conditionImageView = (ImageView) convertView.findViewById(R.id.conditionImageView);
            viewHolder.dayTextView = (TextView) convertView.findViewById(R.id.dayTextView);
            viewHolder.lowTextView = (TextView) convertView.findViewById(R.id.lowTextView);
            viewHolder.hiTextView = (TextView) convertView.findViewById(R.id.hiTextView);
            viewHolder.humidityTextView = (TextView) convertView.findViewById(R.id.humidityTextView);
            convertView.setTag(viewHolder);
        }
        else {
            // reuse an existing ViewHolder object contained as a tag of the list element
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // if an icon depicting given weather has already been downloaded then us it;
        // otherwise, download the icon on a separate thread
        if (bitmaps.containsKey(day.iconURL)) {
            viewHolder.conditionImageView.setImageBitmap(bitmaps.get(day.iconURL));
        }
        else {
            // download and display image depicting weather conditions
            new LoadImageTask(viewHolder.conditionImageView).execute(day.iconURL);
        }

        // read out the remaining data from the Weather object and place them in appropriate views
        Context context = getContext(); // for loading string resources
        viewHolder.dayTextView.setText(context.getString(R.string.day_description, day.dayOfWeek, day.description));
        viewHolder.lowTextView.setText(context.getString(R.string.low_temp, day.minTemp));
        viewHolder.hiTextView.setText(context.getString(R.string.high_temp, day.maxTemp));
        viewHolder.humidityTextView.setText(context.getString(R.string.humidity, day.humidity));

        return convertView; // return a complete list of elements to display
    }

    @Override
    public int getCount() {
        int count = super.getCount();
        return count;
    }

    // AsyncTask subclass loading the icon depicting weather conditions using a separate thread.
    private class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView imageView;    // displays icon

        // hold the ImageView object where the downloaded bitmap should be displayed
        public LoadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        // load the image; params[0] should be a string containing the URL address of the image
        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bitmap = null;
            HttpURLConnection connection = null;

            // open HttpURLConnection, gain access to the InputStream stream and download the image
            try {
                URL url = new URL(params[0]);

                connection = (HttpURLConnection) url.openConnection();

                try (InputStream inputStream = connection.getInputStream()) {
                    bitmap = BitmapFactory.decodeStream(inputStream);
                    bitmaps.put(params[0], bitmap); // save in cache
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            return bitmap;
        }

        // attach image to the list element
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            imageView.setImageBitmap(bitmap);
        }

    }
}
