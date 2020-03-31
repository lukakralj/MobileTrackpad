package com.lukakralj.mobiletrackpad;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import com.lukakralj.mobiletrackpad.backend.RequestCode;
import com.lukakralj.mobiletrackpad.backend.ServerConnection;

import org.json.JSONException;
import org.json.JSONObject;



public class MainActivity extends AppCompatActivity {

    private static int clickDistanceThreshold = 5;
    private static long longPressThreshold = 300; // ms

    private TextView text;
    private Position movedStartPos;
    private Position prevPos;
    private boolean mouseMoving;
    private boolean mouseDragged;
    private boolean isLeftDown;
    private long startMillis;
    private Handler handler; // UI handler


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text = (TextView) findViewById(R.id.text);
        prevPos =  null;
        movedStartPos = null;
        mouseMoving = false;
        mouseDragged = false;
        isLeftDown = false;
        startMillis = 0;
        handler = new Handler(Looper.getMainLooper());

        text.setText(R.string.waitConnection);
        // Initialise server connection.
        ServerConnection.getInstance();
        ServerConnection.getInstance().subscribeOnConnectEvent(() -> handler.post(() -> text.setText(R.string.moveAround)));
        ServerConnection.getInstance().subscribeOnDisconnectEvent(() -> handler.post(() -> text.setText(R.string.waitConnection)));

        SharedPreferences history = getSharedPreferences("history", MODE_PRIVATE);
        if (history.contains("lastSelected")) {
            String name = history.getString("lastSelected", "");
            String ip = history.getString(name + ";ip", "");
            String port = history.getString(name + ";port", "");
            ServerConnection.reconnect(name, ip, port);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.configureURL) {
            // Open URL configuration activity.
            showUrlDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void showUrlDialog() {
        Intent intent = new Intent(this, ConfigureURL.class);
        startActivity(intent);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                movedStartPos = new Position(x, y);
                prevPos = new Position(x, y);
                mouseMoving = false;
                startMillis = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(x - movedStartPos.x) < clickDistanceThreshold &&
                        Math.abs(y - movedStartPos.y) < clickDistanceThreshold &&
                        System.currentTimeMillis() - startMillis >= longPressThreshold) {
                    leftDown();
                    mouseDragged = true;
                    isLeftDown = true;
                }

                if (Math.abs(x - movedStartPos.x) >= clickDistanceThreshold ||
                        Math.abs(y - movedStartPos.y) >= clickDistanceThreshold) {
                    mouseMoving = true;
                    mouseDelta(x - prevPos.x, y - prevPos.y);
                    prevPos.x = x;
                    prevPos.y = y;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!mouseMoving && !mouseDragged
                        && Math.abs(x - movedStartPos.x) < clickDistanceThreshold
                        && Math.abs(y - movedStartPos.y) < clickDistanceThreshold) {
                    leftClick();
                }
                resetMouse();
                break;
            case MotionEvent.ACTION_CANCEL:
                resetMouse();
                break;
        }

        return false;
    }

    private void resetMouse() {
        mouseMoving = false;
        movedStartPos = null;
        prevPos = null;
        startMillis = 0;
        mouseDragged = false;
        if (isLeftDown) {
            leftUp();
        }
    }

    private void mouseDelta(int deltaX, int deltaY) {
        if ((deltaX == 0) && deltaY == 0) {
            return;
        }
        JSONObject extraData = new JSONObject();
        try {
            extraData.put("dx", deltaX);
            extraData.put("dy", deltaY);
        }
        catch (JSONException e) {
            System.out.println(e);
            return;
        }

        ServerConnection.getInstance().scheduleRequest(RequestCode.MOUSE_DELTA, extraData, data -> {});
    }

    private void leftClick() {
        ServerConnection.getInstance().scheduleRequest(RequestCode.LEFT_CLICK, data -> {});
    }

    private void rightClick() {
        ServerConnection.getInstance().scheduleRequest(RequestCode.RIGHT_CLICK, data -> {});
    }

    private void leftDown() {
        ServerConnection.getInstance().scheduleRequest(RequestCode.LEFT_DOWN, data -> {});
    }

    private void leftUp() {
        ServerConnection.getInstance().scheduleRequest(RequestCode.LEFT_UP, data -> {
        });
    }

    private void scrollUp() {
        ServerConnection.getInstance().scheduleRequest(RequestCode.SCROLL_UP, data -> {});
    }

    private void scrollDown() {
        ServerConnection.getInstance().scheduleRequest(RequestCode.SCROLL_DOWN, data -> {});
    }

    private class Position {
        private int x;
        private int y;

        private Position(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
