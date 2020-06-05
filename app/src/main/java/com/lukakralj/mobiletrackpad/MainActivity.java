package com.lukakralj.mobiletrackpad;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;
import com.lukakralj.mobiletrackpad.backend.RequestCode;
import com.lukakralj.mobiletrackpad.backend.ServerConnection;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity {

    private static int clickDistanceThreshold = 5;
    private static int scrollDistanceThreshold = 5;
    private static float scrollFactor = 10; // controls speed of scrolling
    private static long longPressThreshold = 250; // ms

    private TextView text;

    private HashMap<Integer, PointerData> activePointers;
    private int mainPointerId;
    private ArrayList<Integer> otherPointers; // non main ones, in order

    private boolean mouseMoving;
    private boolean mouseDragged;
    private boolean isDoubleTouch;
    private boolean isLeftDown;
    private long startMillis;
    private Handler handler; // UI handler


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text = (TextView) findViewById(R.id.text);

        activePointers = new HashMap<>(3);
        mainPointerId = -1;
        otherPointers = new ArrayList<>();

        mouseMoving = false;
        mouseDragged = false;
        isDoubleTouch = false;

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
        int id = event.getPointerId(event.getActionIndex());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mainPointerId == -1) {
                    startMillis = System.currentTimeMillis();
                }

                initPointer(id, x, y);

                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (!mouseDragged) {
                    System.out.println("Down: " + id);
                    initPointer(id, x, y);
                    isDoubleTouch = true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (id == mainPointerId) {
                    PointerData main = activePointers.get(id);
                    int dxStart = x - main.moveStartPos.x;
                    int dyStart = y - main.moveStartPos.y;
                    int dx = x - main.prevPos.x;
                    int dy = y - main.prevPos.y;

                    if (isDoubleTouch) {
                        // Scroll
                        if (Math.abs(dxStart) >= scrollDistanceThreshold ||
                                Math.abs(dyStart) >= scrollDistanceThreshold) {
                            mouseMoving = true;

                            scrollX(dx);
                            //TODO: dy will be used for horizontal scroll eventually

                            main.prevPos.x = x;
                            main.prevPos.y = y;
                        }
                    }
                    else {
                        // Check if main pointer is ready to drag
                        if (Math.abs(dxStart) < clickDistanceThreshold &&
                                Math.abs(dyStart) < clickDistanceThreshold &&
                                System.currentTimeMillis() - startMillis >= longPressThreshold) {
                            leftDown();
                            mouseDragged = true;
                            isLeftDown = true;
                        }

                        // Move or drag mouse
                        if (Math.abs(dxStart) >= clickDistanceThreshold ||
                                Math.abs(dyStart) >= clickDistanceThreshold) {
                            mouseMoving = true;
                            mouseDelta(dx, dy);
                            main.prevPos.x = x;
                            main.prevPos.y = y;
                        }
                    }
                }

                break;
            case MotionEvent.ACTION_POINTER_UP:
                System.out.println("Up: " + id);
            case MotionEvent.ACTION_UP:
                if (id == mainPointerId && !mouseMoving && !mouseDragged && !isDoubleTouch
                        && Math.abs(x - activePointers.get(id).moveStartPos.x) < clickDistanceThreshold
                        && Math.abs(y - activePointers.get(id).moveStartPos.y) < clickDistanceThreshold) {
                    leftClick();
                }
                else if (otherPointers.size() == 1 && !mouseMoving && !mouseDragged && isDoubleTouch) {
                    PointerData main = activePointers.get(mainPointerId);
                    PointerData secondPointer = activePointers.get(otherPointers.get(0));

                    if (Math.abs(x - main.moveStartPos.x) < clickDistanceThreshold
                            && Math.abs(y - main.moveStartPos.y) < clickDistanceThreshold
                            && Math.abs(x - secondPointer.moveStartPos.x) < clickDistanceThreshold
                            && Math.abs(y - secondPointer.moveStartPos.y) < clickDistanceThreshold) {
                        rightClick();
                    }
                }

                deletePointer(id);
                break;

            case MotionEvent.ACTION_CANCEL:
                deletePointer(id);
                break;
        }

        return false;
    }

    private void initPointer(int id, int x, int y) {
        activePointers.put(id, new PointerData(new Position(x,y), new Position(x,y)));

        if (mainPointerId == -1) {
            mainPointerId = id;
        }
        else {
            otherPointers.add(id);
            if (!mouseDragged) {
                isDoubleTouch = true;
            }
        }
    }

    private void deletePointer(int id) {
        activePointers.remove(id);

        if (mainPointerId == id) {
            if (!otherPointers.isEmpty()) {
                mainPointerId = otherPointers.remove(0);
                startMillis = System.currentTimeMillis();
            }
            else {
                mainPointerId = -1;
            }
            mouseMoving = false;
            mouseDragged = false;

            if (otherPointers.isEmpty()) {
                isDoubleTouch = false;
            }
            else {
                isDoubleTouch = true;
            }

            if (isLeftDown) {
                leftUp();
            }
        }
        else {
            startMillis = System.currentTimeMillis();
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

    private void scrollX(int dx) {
        if (scrollFactor == 0) {
            scrollFactor = 1;
        }
        int amount = (int)(Math.ceil(Math.abs(dx) / scrollFactor));

        if (dx > 0) {
            scrollUp(amount);
        }
        else if (dx < 0) {
            scrollDown(amount);
        }
    }

    private void scrollUp(int amount) {
        JSONObject extraData = new JSONObject();
        try {
            extraData.put("amount", amount);
        }
        catch (JSONException e) {
            System.out.println(e);
            return;
        }

        ServerConnection.getInstance().scheduleRequest(RequestCode.SCROLL_UP, extraData, data -> {});
    }

    private void scrollDown(int amount) {
        JSONObject extraData = new JSONObject();
        try {
            extraData.put("amount", amount);
        }
        catch (JSONException e) {
            System.out.println(e);
            return;
        }

        ServerConnection.getInstance().scheduleRequest(RequestCode.SCROLL_DOWN, extraData, data -> {});
    }


    private class PointerData {
        private Position moveStartPos;
        private Position prevPos;

        private PointerData(Position moveStartPos, Position prevPos) {
            this.moveStartPos = moveStartPos;
            this.prevPos = prevPos;
        }
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
