package com.lukakralj.mobiletrackpad;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.lukakralj.mobiletrackpad.backend.ServerConnection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HistoryDialog extends ListActivity {

    private HistoryAdapter curAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_dialog);

        curAdapter = new HistoryAdapter(this, getHistoryList());
        setListAdapter(curAdapter);
    }

    @Override
    protected void onListItemClick(ListView list, View vi, int position, long id) {
        super.onListItemClick(list, vi, position, id);
    }

    private List<HistoryEntry> getHistoryList() {
        SharedPreferences history = getSharedPreferences("history", MODE_PRIVATE);
        if (!history.contains("names")) {
            return new ArrayList<>();
        }

        List<HistoryEntry> historyEntries = new ArrayList<>();
        for (String name : history.getStringSet("names", new HashSet<>())) {
            String ip = history.getString(name + ";ip", "");
            String port = history.getString(name + ";port", "");
            historyEntries.add(new HistoryEntry(name, ip, port));
        }

        return historyEntries;
    }

    private void deleteHistoryEntry(HistoryEntry e) {
        SharedPreferences history = getSharedPreferences("history", MODE_PRIVATE);
        SharedPreferences.Editor editor = history.edit();
        Set<String> allNames = history.getStringSet("names", null);
        allNames.remove(e.name);
        editor.putStringSet("names", allNames);
        editor.remove(e.name + ";ip");
        editor.remove(e.name + ";port");

        // Remove last selected if deleted.
        if (history.getString("lastSelected", "").equals(e.name)) {
            editor.remove("lastSelected");
        }
        editor.apply();
    }

    private void reconnectWith(HistoryEntry e) {
        updateLastSelected(e.name);
        ServerConnection.reconnect(e.name, e.ip, e.port);
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void updateLastSelected(String name) {
        SharedPreferences history = getSharedPreferences("history", MODE_PRIVATE);
        SharedPreferences.Editor editor = history.edit();
        editor.putString("lastSelected", name);
        editor.apply();
    }

    private class HistoryEntry {
        private String name;
        private String ip;
        private String port;

        private HistoryEntry(String name, String ip, String port) {
            this.name = name;
            this.ip = ip;
            this.port = port;
        }
    }

    /**
     * Custom adapter for the ListView.
     */
    private class HistoryAdapter extends BaseAdapter {
        Context context;
        List<HistoryEntry> data;
        private LayoutInflater inflater;

        private HistoryAdapter(Context context, List<HistoryEntry> data) {
            this.context = context;
            this.data = data;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return data.get(position).name.hashCode();
        }

        @Override
        public boolean isEnabled(int position) {
            return true; // If true, the whole row is clickable.
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View vi;
            vi = inflater.inflate(R.layout.list_item, null);
            ((TextView) vi.findViewById(R.id.connectionName)).setText(data.get(position).name);
            String description = "IP: " + data.get(position).ip + ", port: " + data.get(position).port;
            ((TextView) vi.findViewById(R.id.data)).setText(description);

            ((Button) vi.findViewById(R.id.delete)).setOnClickListener(v -> {
                deleteHistoryEntry(data.get(position));
                data.remove(position);
                notifyDataSetChanged();
            });

            vi.setOnClickListener(v -> reconnectWith(data.get(position)));

            // Needed for correct rendering.
            vi.setEnabled(true);

            return vi;
        }
    }
}
