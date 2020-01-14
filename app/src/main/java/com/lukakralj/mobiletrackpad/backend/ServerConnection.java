package com.lukakralj.mobiletrackpad.backend;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import io.socket.client.IO;
import io.socket.client.Socket;
import android.os.Process;
import org.json.JSONObject;
import static com.lukakralj.mobiletrackpad.backend.RequestCode.*;

public class ServerConnection extends Thread {
    private static String url = "http://10.171.17.77:3333"; // Server URL.
    private static ServerConnection instance;

    /** Queue of all the request that need to be send to the server. */
    private static List<ServerEvent> events = Collections.synchronizedList(new LinkedList<>());

    /* Each activity can subscribe to these three events to update the UI/trigger actions accordingly.*/
    private static SubscriberEvent onConnectEvent = null;
    private static SubscriberEvent onDisconnectEvent = null;

    private Socket io;
    private boolean stop; // Control variable to stop the thread.
    private boolean connected;

    private ServerConnection() {
        try {
            System.out.println("Connecting to: " + url);

            io = IO.socket(url);

            io.on(Socket.EVENT_CONNECT, (data) -> {
                connected = true;
                if (onConnectEvent != null) {
                    onConnectEvent.triggerEvent();
                }
            });

            io.on(Socket.EVENT_DISCONNECT, (data) -> {
                connected = false;
                if (onDisconnectEvent != null) {
                    onDisconnectEvent.triggerEvent();
                }
            });

            io.connect();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        stop = false;
    }

    /**
     * For the singleton pattern.
     *
     * @return Instance of ServerConnection.
     */
    public static ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
            instance.start();
        }
        return instance;
    }

    /**
     * Reconnect to the server using a new URL. Previously scheduled events will be preserved.
     *
     * @param newUrl New url of the server.
     */
    public static void reconnect(String newUrl) {
        System.out.println("Reconnecting with: " + newUrl);
        if (instance != null) {
            instance.stopThread();
            try {
                instance.join();
            }
            catch (InterruptedException e) {
                System.out.println(e);
            }
            instance.io.disconnect();
            instance.io.close();
            instance = null;
        }
        url = newUrl;
        getInstance();
    }

    /**
     * Start executing the events.
     */
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);

        while (!stop) {
            if (events.size() > 0) { // check if there are any new events
                System.out.println("Processing event: " + events.get(0).requestCode);
                processEvent(events.remove(0));
            }
            try {
                sleep(1);
            }
            catch (InterruptedException e) {
                System.out.println("Sleep interrupted: " + e.getMessage());
            }
        }
    }

    /**
     * Executes the request specified by the event and threats the response.
     *
     * @param event Event to be executed.
     */
    private void processEvent(ServerEvent event) {
        String code = getCodeString(event.requestCode);

        io.emit(code, event.extraData);

        io.once(code + "Res", args -> {
            try {
                JSONObject data = (JSONObject)args[0];
                event.listener.processResponse(data);
            }
            catch (Exception e) {
                System.out.println(e);
            }
        });
    }


    /**
     * Stop thread.
     */
    public void stopThread() {
        stop = true;
    }

    /**
     *
     * @return URL that the app is currently connected to.
     */
    public String getCurrentUrl() {
        return url;
    }

    /**
     * Schedule new request to be sent to the server. Requests are processed in
     * first-come-first-server manner.
     *
     * @param requestCode Request specific code.
     * @param extraData Specify additional information to be sent to the server. null if no
     *                  additional information needed.
     * @param listener Specifies what happens when the response is received.
     */
    public void scheduleRequest(RequestCode requestCode, JSONObject extraData, ResponseListener listener) {
        if (!connected) {
            // Prevent spamming.
            return;
        }
        events.add(new ServerEvent(requestCode, extraData, listener));
    }

    /**
     * Schedule new request to be sent to the server. Requests are processed in
     * first-come-first-server manner.
     * Use this method if the request does not require any extra data.
     *
     * @param requestCode Request specific code.
     * @param listener Specifies what happens when the response is received.
     */
    public void scheduleRequest(RequestCode requestCode, ResponseListener listener) {
        scheduleRequest(requestCode, null, listener);
    }

    /**
     * Set what happens when the connection is established.
     *
     * @param event Event to be triggered.
     */
    public void subscribeOnConnectEvent(SubscriberEvent event) {
        onConnectEvent = event;
    }

    /**
     * Set what happens when the connection is lost.
     *
     * @param event Event to be triggered.
     */
    public void subscribeOnDisconnectEvent(SubscriberEvent event) {
        onDisconnectEvent = event;
    }

    /**
     * @return True if the connection with the server is established, false when disconnected.
     */
    public boolean isConnected() {
        return connected;
    }


    /**
     * Converts the request code constant into a string to be used with socket.IO.
     *
     * @param code Request code.
     * @return String associated with the request code.
     */
    private String getCodeString(RequestCode code) {
        switch (code) {
            case MOUSE_DELTA: return "mouse_delta";
            default: throw new RuntimeException("Invalid server code: " + code);
        }
    }

    /**
     * Combines the details about each request (to be send to the server).
     */
    private class ServerEvent {

        private RequestCode requestCode;
        private ResponseListener listener;
        private JSONObject extraData;

        /**
         *
         * @param requestCode Request specific code.
         * @param extraData Specify additional information to be sent to the server. null if no
         *                  additional information needed.
         * @param listener Specifies what happens when the response is received.
         */
        private ServerEvent(RequestCode requestCode, JSONObject extraData, ResponseListener listener) {
            this.requestCode = requestCode;
            this.extraData = extraData;
            this.listener = listener;
        }
    }
}
