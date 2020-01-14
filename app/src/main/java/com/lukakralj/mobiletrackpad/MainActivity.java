package com.lukakralj.mobiletrackpad;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.widget.TextView;
import com.lukakralj.mobiletrackpad.backend.RequestCode;
import com.lukakralj.mobiletrackpad.backend.ServerConnection;

import org.json.JSONException;
import org.json.JSONObject;



public class MainActivity extends AppCompatActivity {

    private TextView text;
    private Position prevPos;
    private Handler handler; // UI handler

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text = (TextView) findViewById(R.id.text);
        prevPos =  null;
        handler = new Handler(Looper.getMainLooper());

        // Initialise server connection.
        ServerConnection.getInstance();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int)event.getX();
        int y = (int)event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                prevPos = new Position(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                mouseMoved(x - prevPos.x, y - prevPos.y);
                prevPos.x = x;
                prevPos.y = y;
                break;
            case MotionEvent.ACTION_UP:
                prevPos = null;
        }


        return false;
    }

    private void mouseMoved(float deltaX, float deltaY) {
        text.setText(deltaX + " x " + deltaY);

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
        System.out.println(ServerConnection.getInstance().isConnected() + "-> " + ServerConnection.getInstance().getCurrentUrl());

        // Instantiate the RequestQueue.
        /*RequestQueue queue = Volley.newRequestQueue(this);
        String url ="http://10.171.17.77:3333/mousedelta/" + deltaX + "/" + deltaY + "";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                null, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("That didn't work!");
                System.out.println(error);
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);*/
    }

    private class Position {
        private float x;
        private float y;

        private Position(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
