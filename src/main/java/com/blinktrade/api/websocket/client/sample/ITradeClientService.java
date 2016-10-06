package com.blinktrade.api.websocket.client.sample;

import com.blinktrade.api.OrderBook;
import com.blinktrade.api.websocket.client.IWebSocketClientConnection;

import java.io.IOException;

public interface ITradeClientService 
{
	MiniOMS getMiniOMS();
	OrderBook getOrderBook(String symbol);
	long getUserId();
	int getBrokerId();
	String SendOrder(
			IWebSocketClientConnection connection, 
			String symbol, 
			long qty, 
			long price, 
			char side, 
			int broker_id, 
			String client_order_id, 
			char order_type, 
			char execInst)  throws IOException;

	boolean CancelOrderByClOrdID(
			IWebSocketClientConnection connection, 
			String clOrdID) throws IOException;
}