package com.blinktrade.api.websocket.client;

import java.io.IOException;

//interface to enable client interactivity with the Websocket connection Endpoint

public interface IWebSocketClientConnection
{
	void SendMessage(String message) throws IOException;
	void SendTestRequest() throws IOException;
	void Shutdown() throws IOException;
	int NextOutgoingSeqNum();
	boolean IsConnected();
	boolean IsLoggedOn();
	void LoggedOn(boolean value);
	boolean IsTestRequestEnabled();
	void EnableTestRequest(boolean value);
	UserDevice getDevice();
	UserAccountCredentials getUserAccount();
	long receivedMessageCounter();
}