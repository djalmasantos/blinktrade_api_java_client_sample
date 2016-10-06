package com.blinktrade.api.websocket.client;

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import com.blinktrade.api.EventSourceImpl;
import com.blinktrade.api.SystemEvent;
import com.blinktrade.api.SystemEventType;


public class WebSocketClientProtocolEngine 
	extends EventSourceImpl 
	implements IWebSocketClientProtocolEngine 
{
	private static final Logger LOG = Log.getLogger(WebSocketClientProtocolEngine.class);

	@Override
	public void OnOpen(IWebSocketClientConnection connection) 
	{
		assert (connection.IsConnected());
		assert (!connection.IsLoggedOn());

		LOG.info("Connection Succeeded [{}]", connection);

		// dispatch the connection opened event
		DispatchEvent(SystemEventType.OPENED, connection, null);

		// build the json Login Request Message
		JsonObjectBuilder login_request = Json.createObjectBuilder();
		login_request.add("MsgType", "BE");
		login_request.add("UserReqID", connection.NextOutgoingSeqNum());
		login_request.add("UserReqTyp", "1");
		login_request.add("Username", connection.getUserAccount().getUsername());
		login_request.add("Password", connection.getUserAccount().getPassword());
		login_request.add("BrokerID", connection.getUserAccount().getBrokerId());

		if (connection.getUserAccount().getSecondFactor() != null && !connection.getUserAccount().getSecondFactor().isEmpty()) 
		{
			login_request.add("SecondFactor", connection.getUserAccount().getSecondFactor());
		}

		login_request.add("UserAgent",
		"Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.76 Mobile Safari/537.36");
		login_request.add("UserAgentLanguage", "en-US");
		login_request.add("UserAgentTimezoneOffset", ":180,");
		login_request.add("UserAgentPlatform", "Linux x86_64");
		login_request.add("FingerPrint", connection.getDevice().getFingerPrint());
		login_request.add("STUNTIP", connection.getDevice().getStuntip().toString());

		try {
			// send the login request Message on wire
			connection.SendMessage(login_request.build().toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void OnClose(IWebSocketClientConnection connection) 
	{
		assert (!connection.IsConnected());
		connection.LoggedOn(false);
		LOG.warn("{}", "WebSocket closed.");
		DispatchEvent(SystemEventType.CLOSED, connection, null);

	}

	@Override
	public void OnMessage(String message, IWebSocketClientConnection connection) 
	{
		JsonObject msg = Json.createReader(new StringReader(message)).readObject();
		String msgType = msg.getString("MsgType");
		switch (msgType) {
		case "BF": // Login response:
		{

			if (msg.containsKey("UserReqTyp") && msg.getInt("UserReqTyp") == 3) 
			{
				assert (connection.IsLoggedOn());
				DispatchEvent(SystemEventType.CHANGE_PASSWORD_RESPONSE, connection, msg);
				break;
			}

			if (msg.getInt("UserStatus") == 1) {
				assert (!connection.IsLoggedOn());
				connection.LoggedOn(true);
				LOG.info("{}", "Received LOGIN_OK response");
				DispatchEvent(SystemEventType.LOGIN_OK, connection, msg);
			} else {
				connection.LoggedOn(false);
				LOG.info("{} {}", "Received LOGIN_ERROR response : ", msg.getString("UserStatusText"));
				DispatchEvent(SystemEventType.LOGIN_ERROR, connection, msg);
				try {
					connection.Shutdown();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			break;
		}
		case "W":
			assert (connection.IsLoggedOn());
			if (msg.getInt("MarketDepth") != 1) // Has Market Depth
			{
				DispatchEvent(SystemEventType.ORDER_BOOK_CLEAR, connection, msg);
				DispatchEvent(SystemEventType.TRADE_CLEAR, connection, msg);

				JsonArray jsonArray = msg.getJsonArray("MDFullGrp");
				for (int i = 0; i < jsonArray.size(); i++) {
					JsonObject entry = jsonArray.getJsonObject(i);
					JsonObjectBuilder jsonBuilder = Util.jsonObjectToBuilder(entry);
					jsonBuilder.add("MDReqID", msg.getInt("MDReqID"));
					switch (entry.getString("MDEntryType")) {
					case "0": // Bid
					case "1": // Offer
						jsonBuilder.add("Symbol", msg.getString("Symbol"));
						DispatchEvent(SystemEventType.ORDER_BOOK_NEW_ORDER, connection, jsonBuilder.build());
						break;
					case "2": // Trade
						DispatchEvent(SystemEventType.TRADE, connection, jsonBuilder.build());
						break;
					case "4": // Trading Session Status
						DispatchEvent(SystemEventType.TRADING_SESSION_STATUS, connection, jsonBuilder.build());
						break;
					}
				}
			}
			DispatchEvent(SystemEventType.MARKET_DATA_FULL_REFRESH, connection, msg);
			break;
		case "X":
			if (msg.getString("MDBkTyp").equals("3")) // Order Depth
			{
				JsonArray jsonArray = msg.getJsonArray("MDIncGrp");
				for (int i = 0; i < jsonArray.size(); i++) {
					JsonObject entry = jsonArray.getJsonObject(i);
					JsonObjectBuilder jsonBuilder = Util.jsonObjectToBuilder(entry);
					jsonBuilder.add("MDReqID", msg.getInt("MDReqID"));
					switch (entry.getString("MDEntryType")) {
					case "0": // Bid
					case "1": // Offer
						switch (entry.getString("MDUpdateAction")) {
						case "0":
							DispatchEvent(SystemEventType.ORDER_BOOK_NEW_ORDER, connection, jsonBuilder.build());
							break;
						case "1":
							DispatchEvent(SystemEventType.ORDER_BOOK_UPDATE_ORDER, connection, jsonBuilder.build());
							break;
						case "2":
							DispatchEvent(SystemEventType.ORDER_BOOK_DELETE_ORDER, connection, jsonBuilder.build());
							break;
						case "3":
							DispatchEvent(SystemEventType.ORDER_BOOK_DELETE_ORDERS_THRU, connection,
									jsonBuilder.build());
							break;
						}
						break;
					case "2": // Trade
						DispatchEvent(SystemEventType.TRADE, connection, entry);
						break;
					case "4": // Trading Session Status
						DispatchEvent(SystemEventType.TRADING_SESSION_STATUS, connection, entry);
						break;
					}
				}
			} else {
				// TODO: Top of the book handling.
			}
			DispatchEvent(SystemEventType.MARKET_DATA_INCREMENTAL_REFRESH, connection, msg);
			break;
		case "Y":
			DispatchEvent(SystemEventType.MARKET_DATA_REQUEST_REJECT, connection, msg);
			break;
		case "f":
			DispatchEvent(SystemEventType.SECURITY_STATUS, connection, msg);
			break;
		case "U3":
			DispatchEvent(SystemEventType.BALANCE_RESPONSE, connection, msg);
			break;
		case "U5":
			DispatchEvent(SystemEventType.ORDER_LIST_RESPONSE, connection, msg);
			break;
		case "8": // Execution Report
			if (!msg.containsKey("Volume")) {
				long volume = 0;
				if (msg.containsKey("AvgPx")) {
					long avgPx = msg.getJsonNumber("AvgPx").longValueExact();
					if (avgPx > 0)
						volume = (long) (avgPx * msg.getJsonNumber("CumQty").longValueExact() / 1e8);
				}
				// this workaround is necessary because JsonObject is immutable
				msg = Util.jsonObjectToBuilder(msg).add("Volume", volume).build();
			}
			DispatchEvent(SystemEventType.EXECUTION_REPORT, connection, msg);
			break;
		case "0":
			DispatchEvent(SystemEventType.HEARTBEAT, connection, msg);
			break;
		case "ERROR":
			LOG.warn("{}", msg.toString());
			DispatchEvent(SystemEventType.ERROR, connection, msg);
			try {
				connection.Shutdown();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		default: {
			assert (connection.IsLoggedOn());
			LOG.info("Unhandled message type : {}", msgType);
			break;
		}
		}
	}

	@Override
	public void OnError(Throwable cause, IWebSocketClientConnection connection) 
	{
		LOG.warn("{}", cause);
		DispatchEvent(SystemEventType.ERROR, connection, null);
	}

	@Override
	public void SendTestRequest(IWebSocketClientConnection connection) 
	{
		JsonObjectBuilder test_request_builder = Json.createObjectBuilder();
		test_request_builder.add("MsgType", "1");
		test_request_builder.add("TestReqID", connection.NextOutgoingSeqNum());
		test_request_builder.add("FingerPrint", connection.getDevice().getFingerPrint());
		test_request_builder.add("STUNTIP", connection.getDevice().getStuntip().toString());
		test_request_builder.add("SendTime", Util.ConvertToUnixTimestamp(new Date()));
		JsonObject test_request = test_request_builder.build();
		String test_request_msg = test_request.toString();
		if (connection.IsConnected()) {
			try {
				connection.SendMessage(test_request_msg);
				LOG.info("{}", test_request_msg);
			} catch (IOException ex) {
				LOG.warn(ex);
			}
		}
	}

	private void DispatchEvent(SystemEventType evtType, IWebSocketClientConnection connection, JsonObject json) 
	{
		SystemEvent evt = new SystemEvent(connection);
		evt.evtType = evtType;
		evt.json = json;
		super.fireActionPerformed(evt);
	}
}