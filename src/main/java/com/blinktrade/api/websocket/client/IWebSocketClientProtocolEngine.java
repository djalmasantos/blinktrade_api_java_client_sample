package com.blinktrade.api.websocket.client;

import com.blinktrade.api.ActionListener;

//interface of the protocol engine to provide websocket connection access and callback events
public interface IWebSocketClientProtocolEngine
{
	void OnOpen(IWebSocketClientConnection connection);
	void OnClose(IWebSocketClientConnection connection);
	void OnMessage(String message, IWebSocketClientConnection connection);
	void OnError(Throwable cause, IWebSocketClientConnection connection);
	void SendTestRequest(IWebSocketClientConnection connection);
	void addActionListener(ActionListener l);
	void removeActionListener(ActionListener l);
}