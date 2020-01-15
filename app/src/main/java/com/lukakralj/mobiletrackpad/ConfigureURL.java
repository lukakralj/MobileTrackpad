package com.lukakralj.mobiletrackpad;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.lukakralj.mobiletrackpad.backend.ServerConnection;

import java.util.HashSet;
import java.util.Set;

public class ConfigureURL extends AppCompatActivity {

    private EditText connectionName;
    private EditText ipInput;
    private EditText portInput;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_url);

        connectionName = (EditText) findViewById(R.id.connectionName);
        ipInput = (EditText) findViewById(R.id.ipInput);
        portInput = (EditText) findViewById(R.id.portInput);
        saveButton = (Button) findViewById(R.id.saveButton);

        connectionName.setText(ServerConnection.getInstance().getConnectionName());
        ipInput.setText(ServerConnection.getInstance().getIp());
        portInput.setText(ServerConnection.getInstance().getPort());
        saveButton.setOnClickListener(this::reconnect);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.history_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.history) {
            // Open URL configuration activity.
            showHistoryDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void showHistoryDialog() {
        Intent intent = new Intent(this, HistoryDialog.class);
        startActivity(intent);
    }

    private void reconnect(View v) {
        String name = connectionName.getText().toString().trim();
        String ip = ipInput.getText().toString().trim();
        String port = portInput.getText().toString().trim();
        boolean invalid = false;

        if (name.length() == 0 || name.contains(";")) {
            invalid = true;
            connectionName.setBackgroundColor(getResources().getColor(R.color.invalidInput));
        }
        if (ip.length() < 7 ||
                ip.length() - ip.replace(".", "").length() != 3) {
            // invalid ip
            invalid = true;
            ipInput.setBackgroundColor(getResources().getColor(R.color.invalidInput));
        }
        if (port.length() == 0 || port.length() > 5 || !port.matches("\\d+")) {
            invalid = true;
            portInput.setBackgroundColor(getResources().getColor(R.color.invalidInput));
        }

        if (!invalid) {
            updateHistory(name, ip, port);
            updateLastSelected(name);
            ServerConnection.reconnect(name, ip, port);
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }

    private void updateLastSelected(String name) {
        SharedPreferences history = getSharedPreferences("history", MODE_PRIVATE);
        SharedPreferences.Editor editor = history.edit();
        editor.putString("lastSelected", name);
        editor.apply();
    }

    private void updateHistory(String name, String ip, String port) {
        SharedPreferences history = getSharedPreferences("history", MODE_PRIVATE);
        SharedPreferences.Editor editor = history.edit();
        Set<String> allNames = new HashSet<>();
        if (history.contains("names")) {
            allNames = history.getStringSet("names", null);
        }
        allNames.add(name);
        editor.putStringSet("names", allNames);
        editor.putString(name + ";ip", ip);
        editor.putString(name + ";port", port);
        editor.apply();
    }

}
