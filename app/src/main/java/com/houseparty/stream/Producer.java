package com.houseparty.stream;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.BlockingQueue;

/**
 * continuously producing Items
 */
class Producer extends Thread {

    private static final String TAG = "Producer";
    private final String urlString;
    private final BlockingQueue<Item> queue;
    private boolean read = false;

    Producer(String urlString, BlockingQueue<Item> queue) {
        this.urlString = urlString;
        this.queue = queue;
    }

    @Override
    public void run() {
        BufferedReader reader;
        HttpURLConnection urlConnection;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream inStream = urlConnection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inStream));
            read = true;
        } catch (Exception ex) {
            Log.e(TAG, "Error ", ex);
            return;
        }
        while(read) {
            try {
                queue.put(parse(new JSONObject(reader.readLine())));
            } catch (Exception ex) {
                Log.e(TAG, "Error ", ex);
            }
        }
        try {
            reader.close();
            urlConnection.disconnect();
        } catch (Exception ex) {
            Log.e(TAG, "Error ", ex);
        }
        close();
    }

    void close() {
        read = false;
    }

    private static Item parse(JSONObject json) throws JSONException {
        Item item = new Item();
        item.to = json.getJSONObject("to").getString("name");
        item.from = json.getJSONObject("from").getString("name");
        item.timestamp = Long.parseLong(json.getString("timestamp"));
        item.areFriends = json.getBoolean("areFriends");
        return item;
    }
}
