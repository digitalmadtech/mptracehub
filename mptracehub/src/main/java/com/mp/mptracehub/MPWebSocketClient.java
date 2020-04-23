package com.mp.mptracehub;


import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Map;

public class MPWebSocketClient extends WebSocketClient {
    public MPWebSocketClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void send(String text) {
        if(!isOpen()) {
            Log.println(Log.ERROR, "TRACEHUBLOG", "Socket not open, returning");
            return;
        }
        super.send(text);
        Log.println(Log.DEBUG, "TRACEHUBLOG", text);

    }

    public MPWebSocketClient(URI serverUri, Draft protocolDraft) {
        super(serverUri, protocolDraft);
    }

    public MPWebSocketClient(URI serverUri, Map<String, String> httpHeaders) {
        super(serverUri, httpHeaders);
    }

    public MPWebSocketClient(URI serverUri, Draft protocolDraft, Map<String, String> httpHeaders) {
        super(serverUri, protocolDraft, httpHeaders);
    }

    public MPWebSocketClient(URI serverUri, Draft protocolDraft, Map<String, String> httpHeaders, int connectTimeout) {
        super(serverUri, protocolDraft, httpHeaders, connectTimeout);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.println(Log.DEBUG, "TRACEHUBLOG", "WSSocket open");
    }

    @Override
    public void onMessage(String message) {
        Log.println(Log.DEBUG, "TRACEHUBLOG", "WSSocket received the message:"+message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.println(Log.DEBUG, "TRACEHUBLOG", "WSSocket closed: Reason:"+reason);
    }

    @Override
    public void onError(Exception ex) {
        Log.println(Log.ERROR, "TRACEHUBLOG", ex.getLocalizedMessage());
    }
}
