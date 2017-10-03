package com.houseparty.stream;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    private RecyclerView recyclerView;
    private LinearLayoutManager recyclerViewLayoutManager;
    private MainAdapter adapter;
    private BlockingQueue<Item> queue;// poor man's back pressure
    private static Producer producer;
    private final ScheduledThreadPoolExecutor consumer = new ScheduledThreadPoolExecutor(1);
    private boolean filtered;
    private long lastTimestamp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerViewLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(recyclerViewLayoutManager);
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        adapter = new MainAdapter();
        recyclerView.setAdapter(adapter);
        ((ToggleButton) findViewById(R.id.friends)).setOnCheckedChangeListener(this);

        RetainFragment retainFragment = RetainFragment.findOrCreateRetainFragment(getFragmentManager());
        queue = retainFragment.retainedCache;
        if (queue == null) {
            queue = new LinkedBlockingQueue<>(2000);
            retainFragment.retainedCache = queue;
        }
    }

    static public class RetainFragment extends Fragment {
        private static final String TAG = "RetainFragment";
        public BlockingQueue<Item> retainedCache;

        public RetainFragment() {}

        public static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
            RetainFragment fragment = (RetainFragment) fm.findFragmentByTag(TAG);
            if (fragment == null) {
                fragment = new MainActivity.RetainFragment();
                fm.beginTransaction().add(fragment, TAG).commit();
            }
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startProducing();
        final Runnable task = () -> {
            final int batchSize = 5;
            final List<Item> batch = new ArrayList<>(batchSize);
            Item item = null;
            for (int i = 0; i < batchSize; i++) {
                item = queue.poll();
                if (item == null) {
                    runOnUiThread(this::startProducing);
                    return;
                }
                else batch.add(item);
            }
            lastTimestamp = item.timestamp;
            if (!filtered || item.areFriends) {
                runOnUiThread(() -> {
                    adapter.addToTopOfList(batch);
                    if (recyclerViewLayoutManager.findFirstVisibleItemPosition() == 0)
                        recyclerView.smoothScrollToPosition(0);
                });
            }
        };
        consumer.scheduleAtFixedRate(task , 0 , 1, TimeUnit.SECONDS); // delay is to get a friendly UX
        registerReceiver(networkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onPause() {
        if (producer != null) producer.close();
        consumer.shutdownNow();
        unregisterReceiver(networkChangeReceiver);
        super.onPause();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        filtered = checked;
        adapter.clearNonFriends();
        adapter.notifyDataSetChanged();
    }

    private final BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (isOnline())
                startProducing();
            else if (producer != null)
                producer.close();
        }
    };

    private void startProducing() {
        String url = "https://hp-server-toy.herokuapp.com/?since=" +
                Long.toString(lastTimestamp > 0 ? lastTimestamp : System.currentTimeMillis());
        if (producer != null) producer.close();
        producer = new Producer(url, queue);
        producer.start();
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }
}
