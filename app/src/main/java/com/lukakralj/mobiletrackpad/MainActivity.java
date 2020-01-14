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
        int x = (int) event.getX();
        int y = (int) event.getY();

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

    private void mouseMoved(int deltaX, int deltaY) {
        text.setText(deltaX + " x " + deltaY);

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

    private class Position {
        private int x;
        private int y;

        private Position(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
