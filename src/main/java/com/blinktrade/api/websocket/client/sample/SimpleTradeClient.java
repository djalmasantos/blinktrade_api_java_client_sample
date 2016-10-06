package com.blinktrade.api.websocket.client.sample;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import com.blinktrade.api.ActionListener;
import com.blinktrade.api.OrdStatus;
import com.blinktrade.api.OrderBook;
import com.blinktrade.api.OrderSide;
import com.blinktrade.api.SystemEvent;
import com.blinktrade.api.websocket.client.*;

public class SimpleTradeClient 
	implements ActionListener, ITradeClientService
{
	private static final Logger LOG = Log.getLogger(SimpleTradeClient.class);
	private int _brokerId;
	private long _myUserID = 0;
	private Map<String, OrderBook> _allOrderBooks = new LinkedHashMap<String, OrderBook>();
	private Map<String, JsonObject> _securityStatus = new LinkedHashMap<String, JsonObject>();
	private MiniOMS _miniOMS = new MiniOMS();
	private TradingStrategy _tradingStrategy;
	private String _tradingSymbol;
	
	SimpleTradeClient(int broker_id, String symbol, TradingStrategy strategy)
	{
		_brokerId = broker_id;
		_tradingSymbol = symbol;
		_tradingStrategy = strategy;
		_tradingStrategy.setTradeClient(this);
	}
	
	
	@Override
 	public MiniOMS getMiniOMS() 
	{
		return this._miniOMS;
	}

	@Override
 	public OrderBook getOrderBook(String symbol) 
	{
		OrderBook result = _allOrderBooks.get(symbol);
		if ( result != null )
			return result;
		else
			throw new IllegalArgumentException("Symbol not found : " + symbol);
	}

	@Override
 	public long getUserId() 
	{
		if ( _myUserID > 0 )
			return _myUserID;
		else
			throw new java.lang.IllegalStateException();
	}

	@Override
 	public int getBrokerId() 
	{
		return this._brokerId;
	}

	@Override
 	public String SendOrder(
 			IWebSocketClientConnection connection, 
 			String symbol, 
 			long qty, 
 			long price, 
 			char side,
			int broker_id, 
			String client_order_id, 
			char order_type, 
			char execInst) throws IOException 
	{
		// add pending new order to the OMS
		MiniOMS.Order orderToSend = this._miniOMS.new Order();
		orderToSend.set_symbol(symbol);
		orderToSend.set_orderQty(qty);
		orderToSend.set_price(price);
		orderToSend.set_side(side);
		orderToSend.set_clOrdID(client_order_id);
		orderToSend.set_ordType(order_type);
		orderToSend.set_ordStatus('A'); // PENDING_NEW : according to FIX protocol standard
		try
		{
			_miniOMS.AddOrder(orderToSend);
		}
		catch(Throwable t)
		{
			LOG.warn("The MiniOMS Rejected the Order : {};{};{} ", 
			    orderToSend.getClOrdID(), 
			    orderToSend.getOrderQty(), 
			    orderToSend.getPrice()
			);
			LOG.warn("Error", t);
			return null;
		}

		// send the order to the broker
		JsonObjectBuilder new_order_single = Json.createObjectBuilder();
		new_order_single.add("MsgType", "D");
		new_order_single.add("ClOrdID", orderToSend.getClOrdID());
		new_order_single.add("Symbol", orderToSend.getSymbol());
		new_order_single.add("Side",   String.valueOf(orderToSend.getSide()));
		new_order_single.add("OrdType", String.valueOf(orderToSend.getOrdType()));
		new_order_single.add("Price", orderToSend.getPrice());
		new_order_single.add("OrderQty", orderToSend.getOrderQty());
		new_order_single.add("BrokerID", broker_id);
		if ( execInst != 0 )
		{
			new_order_single.add("ExecInst", String.valueOf(execInst));
		}
		new_order_single.add("FingerPrint", connection.getDevice().getFingerPrint());
		new_order_single.add("STUNTIP", connection.getDevice().getStuntip().toString());
		connection.SendMessage(new_order_single.build().toString()); 
		return orderToSend.getClOrdID();
	}

	@Override
 	public boolean CancelOrderByClOrdID(IWebSocketClientConnection connection, String clOrdID) throws IOException
	{
		MiniOMS.Order orderToCancel = _miniOMS.GetOrderByClOrdID(clOrdID);
		if (orderToCancel != null)
		{
			orderToCancel.set_ordStatus(OrdStatus.PENDING_CANCEL);
			JsonObjectBuilder order_cancel_request = Json.createObjectBuilder();
			order_cancel_request.add("MsgType", "F");
			order_cancel_request.add("ClOrdID", clOrdID);
			order_cancel_request.add("FingerPrint", connection.getDevice().getFingerPrint());
			order_cancel_request.add("STUNTIP", connection.getDevice().getStuntip().toString());
			connection.SendMessage(order_cancel_request.build().toString()); 
			return true;
		}
		else
		{
			LOG.info("Not sending OrderCancelRequest because order not found by ClOrdID = {}", clOrdID);
			return false;
		}
	}
	
	public void actionPerformed(EventObject e)
	{
		SystemEvent evt = (SystemEvent) e;
		IWebSocketClientConnection webSocketConnection = (IWebSocketClientConnection) evt.getSource();
		
		try
		{
			switch (evt.evtType)
			{
				case LOGIN_OK:
					LOG.info("{}", "Processing after succesful LOGON");
					this._myUserID = evt.json.getJsonNumber("UserID").longValueExact();
					// disable test request to avoid disconnection during the "slow" market data processing
					webSocketConnection.EnableTestRequest(false); 
					StartInitialRequestsAfterLogon(webSocketConnection);
					break;
				case MARKET_DATA_REQUEST_REJECT:
					LOG.warn("ERROR : {}", "Unexpected Marketdata Request Reject");
					webSocketConnection.Shutdown();
					break;

				case MARKET_DATA_FULL_REFRESH:
					{
						LOG.info("Processing {}", evt.evtType.toString());
						String symbol = evt.json.getString("Symbol");
						// dump the order book
						LOG.info("{}", _allOrderBooks.get(symbol).toString());
						// bring back the testrequest keep-alive mechanism after processing the book
						webSocketConnection.EnableTestRequest(true);
						// run the trading strategy to buy and sell orders based on the top of the book
						_tradingStrategy.runStrategy(webSocketConnection, symbol);
					}
					break;
				// --- Order Book Management Events ---
				case ORDER_BOOK_CLEAR:
					{
						LOG.info("Processing {}", evt.evtType.toString());
						String symbol = evt.json.getString("Symbol");
						OrderBook orderBook = _allOrderBooks.get(symbol);
						if (orderBook != null)
						{
							orderBook.Clear();
						}
						else
						{
							orderBook = new OrderBook(symbol);
							_allOrderBooks.put(symbol, orderBook);
						}

					}
					break;

				case ORDER_BOOK_NEW_ORDER:
					{
						LOG.info("Processing {}", evt.evtType.toString());
						String symbol = evt.json.getString("Symbol");
						OrderBook orderBook = _allOrderBooks.get(symbol);
						if (orderBook != null)
							orderBook.AddOrder(evt.json);
						else
							LOG.warn("ERROR : Order Book not found for Symbol {} @ {}", symbol, evt.evtType.toString());
					}
					break;

				case ORDER_BOOK_DELETE_ORDERS_THRU:
					{
						LOG.info("Processing {}", evt.evtType.toString());
						String symbol = evt.json.getString("Symbol");
						OrderBook orderBook = _allOrderBooks.get(symbol);
						if (orderBook != null)
							orderBook.DeleteOrdersThru(evt.json);
						else
							LOG.warn("ERROR : Order Book not found for Symbol {} @ {}", symbol, evt.evtType.toString());
					}
					break;

				case ORDER_BOOK_DELETE_ORDER:
					{
						LOG.info("Processing {}", evt.evtType.toString());
						String symbol = evt.json.getString("Symbol");
						OrderBook orderBook = _allOrderBooks.get(symbol);
						if (orderBook != null)
							orderBook.DeleteOrder(evt.json);
						else
							LOG.warn("ERROR : Order Book not found for Symbol {} @ {}", symbol, evt.evtType.toString());
					}
					break;

				case ORDER_BOOK_UPDATE_ORDER:
					{
						LOG.info("Processing {}", evt.evtType.toString());
						String symbol = evt.json.getString("Symbol");
						OrderBook orderBook = _allOrderBooks.get(symbol);
						if (orderBook != null)
							orderBook.UpdateOrder(evt.json);
						else
							LOG.warn("ERROR : Order Book not found for Symbol {} @ {}", symbol, evt.evtType.toString());
					}
					break;
				// ------------------------------------

				case TRADE_CLEAR:
					LOG.info("Receieved Market Data Event {}", evt.evtType.toString());
					break;

				case SECURITY_STATUS:
					{
						LOG.info("Receieved Market Data Event {} {}", 
						    evt.evtType.toString(), 
						    (evt.json != null ? evt.json.toString() : ".")
						);
						String securityKey = evt.json.getString("Market") + ":" + evt.json.getString("Symbol");
						// update the security status information
						ArrayList<String> excludeFields = new ArrayList<String>();
						excludeFields.add("MsgType");
						excludeFields.add("Market");
						excludeFields.add("Symbol");
						excludeFields.add("SecurityStatusReqID");
						// clone the json excluding the fields in the list
						_securityStatus.put(securityKey, Util.jsonObjectToBuilder(evt.json, excludeFields).build());
					}
					break;

				// --- Other marketdata events
				case TRADING_SESSION_STATUS:
				case TRADE:
					break;


				case MARKET_DATA_INCREMENTAL_REFRESH:
					LOG.info("Receieved Market Data Incremental Refresh : {}", evt.evtType.toString());
					_tradingStrategy.runStrategy(webSocketConnection, _tradingSymbol);
					break;

				// --- Order Entry Replies ---
				case EXECUTION_REPORT:
					LOG.info( "Receieved {}\n{}", evt.evtType.toString(), evt.json.toString());
					ProcessExecutionReport(evt.json);
					break;

				case ORDER_LIST_RESPONSE:
					{
						// process the requested list of orders
						JsonObject msg = evt.json;
						LOG.info("Received {} : Page={}", evt.evtType.toString(), msg.getInt("Page"));
						JsonArray ordersLst = msg.getJsonArray("OrdListGrp");
						
						if (ordersLst != null && ordersLst.size() > 0)
						{
							JsonArray columns = msg.getJsonArray("Columns");
							Map<String, Integer> indexOf = new HashMap<String, Integer>();
							for (int i=0; i < columns.size(); i++)
							{
								indexOf.put(columns.getString(i), i);
							}

							for (int i=0; i < ordersLst.size(); i++)
							{
								JsonArray data = ordersLst.getJsonArray(i);
								MiniOMS.Order order = this._miniOMS.new Order();
								order.set_clOrdID(data.getString(indexOf.get("ClOrdID")));
								order.set_orderID(data.getJsonNumber(indexOf.get("OrderID")).longValueExact());
								order.set_symbol(data.getString(indexOf.get("Symbol")));
								order.set_side(data.getString(indexOf.get("Side")).charAt(0));
								order.set_ordType(data.getString(indexOf.get("OrdType")).charAt(0));
								order.set_ordStatus(data.getString(indexOf.get("OrdStatus")).charAt(0));
								order.set_avgPx(data.getJsonNumber(indexOf.get("AvgPx")).longValueExact());
								order.set_price(data.getJsonNumber(indexOf.get("Price")).longValueExact());
								order.set_orderQty(data.getJsonNumber(indexOf.get("OrderQty")).longValueExact());
								order.set_leavesQty(data.getJsonNumber(indexOf.get("LeavesQty")).longValueExact());
								order.set_cumQty(data.getJsonNumber(indexOf.get("CumQty")).longValueExact());
								order.set_cxlQty(data.getJsonNumber(indexOf.get("CxlQty")).longValueExact());
								order.set_volume(data.getJsonNumber(indexOf.get("Volume")).longValue());
								order.set_orderDate(data.getString(indexOf.get("OrderDate")));
								order.set_timeInForce(data.getString(indexOf.get("TimeInForce")).charAt(0));
								LOG.info("Adding Order to MiniOMS -> ClOrdID = {} OrdStatus =  {}", 
								    order.getClOrdID(), 
								    order.getOrdStatus()
								);
								try
								{
									_miniOMS.AddOrder(order);
								}
								catch (IllegalArgumentException ex)
								{
								}
							}

							// check and request the next page
							if (ordersLst.size() >= msg.getInt("PageSize"))
							{
								LOG.info("Requesting Page {}", msg.getInt("Page") + 1);
								SendRequestForOpenOrders(webSocketConnection, msg.getInt("Page") + 1);
							}
							else 
							{
								LOG.info("{}", "EOF - no more Security List pages to process");
							}
						}
							
					}
					break;

				// Following events are ignored because inheritted behaviour is sufficient for this prototype
				case OPENED:
				case CLOSED:
				case ERROR:
				case LOGIN_ERROR:
				case HEARTBEAT:
				//case BALANCE_RESPONSE:
				break;
				default:
					LOG.warn("Unhandled Broker Notification Event : {}", evt.evtType.toString());
					break;
			}
	
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
		
	}
	
	private void ProcessExecutionReport(JsonObject msg) 
	{
		assert(msg.getString("MsgType").equals("8"));
		MiniOMS.Order order = _miniOMS.GetOrderByClOrdID(msg.getString("ClOrdID"));
		if ( order != null )
		{
			switch (msg.getString("OrdStatus").charAt(0)) 
			{
				// -- If the order is still "alive", update the order data in the MiniOMS
				case OrdStatus.NEW:
				case OrdStatus.PARTIALLY_FILLED: 
				case OrdStatus.STOPPED:  // Coming soon / work in progress
					order.set_orderID(msg.getJsonNumber("OrderID").longValue());
					order.set_ordStatus(msg.getString("OrdStatus").charAt(0));
					order.set_cumQty(msg.getJsonNumber("CumQty").longValue());
					order.set_cxlQty(msg.getJsonNumber("CxlQty").longValue());
					order.set_lastPx(msg.getJsonNumber("LastPx").longValue());
					order.set_lastShares(msg.getJsonNumber("LastShares").longValue());
					order.set_leavesQty(msg.getJsonNumber("LeavesQty").longValue());
					order.set_price(msg.getJsonNumber("Price").longValue());
					order.set_avgPx(msg.getJsonNumber("AvgPx").longValue());
					break;
				// -- If the order is "dead", remove the order from the MiniOMS
				case OrdStatus.CANCELED: 
				case OrdStatus.REJECTED:
				case OrdStatus.FILLED: 
					boolean retVal = _miniOMS.RemoveOrderByClOrdID(msg.getString("ClOrdID"));
					assert(retVal);
					break;
				
				default:
					LOG.warn("ERROR : unexpected ExecutionReport.OrdStatus : " + msg.getString("OrdStatus").charAt(0));
					break;
			}
		}
		else 
		{
			LOG.warn("ERROR : Order not found by ClOrdID = " + msg.getString("ClOrdID"));
		}
		
	}


	private void StartInitialRequestsAfterLogon(IWebSocketClientConnection connection) throws IOException
	{
		// 1. send market data request
		JsonObjectBuilder marketdata_request = Json.createObjectBuilder();
		//JObject marketdata_request = new JObject();
		marketdata_request.add("MsgType", "V");
		marketdata_request.add("MDReqID", connection.NextOutgoingSeqNum());
		marketdata_request.add("SubscriptionRequestType", "1");
		marketdata_request.add("MarketDepth", 0);
		marketdata_request.add("MDUpdateType", "1");
		marketdata_request.add("MDEntryTypes", Json.createArrayBuilder().add("0").add("1").build());
		marketdata_request.add("Instruments",  Json.createArrayBuilder().add(_tradingSymbol).build());
		marketdata_request.add("STUNTIP", connection.getDevice().getStuntip().toString());
		marketdata_request.add("FingerPrint", connection.getDevice().getFingerPrint());
		connection.SendMessage(marketdata_request.build().toString());

		// 2. send security status request
		JsonObjectBuilder securitystatus_request = Json.createObjectBuilder();
		securitystatus_request.add("MsgType", "e");
		securitystatus_request.add("SecurityStatusReqID", connection.NextOutgoingSeqNum());
		securitystatus_request.add("SubscriptionRequestType", "1");
		JsonArrayBuilder instruments = Json.createArrayBuilder();
		instruments.add("BLINK:BTCVEF");
		instruments.add("BLINK:BTCBRL");
		instruments.add("BLINK:BTCPKR");
		instruments.add("BLINK:BTCCLP");
		instruments.add("BITSTAMP:BTCUSD");
		instruments.add("ITBIT:BTCUSD");
		instruments.add("BITFINEX:BTCUSD");
		instruments.add("BTRADE:BTCUSD");
		instruments.add("MBT:BTCBRL");
		instruments.add("KRAKEN:BTCEUR");
		instruments.add("COINFLOOR:BTCGBP");
		instruments.add("BLINK:BTCVND");
		instruments.add("UOL:USDBRL");
		instruments.add("UOL:USDBRT");
		instruments.add("OKCOIN:BTCCNY");
		securitystatus_request.add("Instruments",instruments.build());
		securitystatus_request.add("STUNTIP", connection.getDevice().getStuntip().toString());
		securitystatus_request.add("FingerPrint", connection.getDevice().getFingerPrint());
		connection.SendMessage(securitystatus_request.build().toString());

		// 3. send request for open orders
		SendRequestForOpenOrders(connection, 0);

		// 4. send the balance request
		JsonObjectBuilder balance_request = Json.createObjectBuilder();;
		balance_request.add("MsgType", "U2");
		balance_request.add("BalanceReqID", connection.NextOutgoingSeqNum());
		balance_request.add("STUNTIP", connection.getDevice().getStuntip().toString());
		balance_request.add("FingerPrint", connection.getDevice().getFingerPrint());
		connection.SendMessage(balance_request.build().toString());		
	}
	
	private void SendRequestForOpenOrders(IWebSocketClientConnection connection, int page) throws IOException
	{
		JsonObjectBuilder job = Json.createObjectBuilder();
		job.add("MsgType", "U4");
		job.add("OrdersReqID", connection.NextOutgoingSeqNum() );
		job.add("Page", page );
		job.add("PageSize", 20 );
		job.add("has_cum_qty eq 1", Json.createArrayBuilder().add("has_cum_qty eq 1").build());
		job.add("STUNTIP", connection.getDevice().getStuntip().toString());
		job.add("FingerPrint", connection.getDevice().getFingerPrint());
		connection.SendMessage(job.build().toString());
	}
	
	private static void show_usage(String program_name)
	{
		System.out.println("Blinktrade websocket client Java sample");
		System.out.println("\nusage:\n\t java "+program_name+" <URL> <BROKER-ID> <SYMBOL> <BUY|SELL|BOTH> <MAX-BTC-TRADE-SIZE> <TARGET-PRICE> <USERNAME> <PASSWORD> [<SECOND-FACTOR>]");
		System.out.println("\nexample:\n\t java "+program_name+" \"wss://api.testnet.blinktrade.com/trade/\" 5 BTCUSD BOTH 0.1 1900.01 user abc12345");
	}
	
	public static void main(String[] args)
	{
		final StackTraceElement[] stack = Thread.currentThread ().getStackTrace();
		final String program_name = stack[stack.length - 1].getClassName();
		
		if (args.length < 8 || args.length > 9)
		{
			show_usage(program_name);
			return;
		}
		
		String url = args[0];
		int broker_id;
		try 
		{
			broker_id = Integer.parseInt(args[1]);
		}
		catch (NumberFormatException ex)
		{
			System.out.println(ex.getMessage());
			show_usage(program_name);
			return;
		}
		String symbol = args[2];
		
		char side;
		switch(args[3].toUpperCase())
		{ 
			case "BUY":
				side = OrderSide.BUY;
				break;
			case "SELL":
				side = OrderSide.SELL;
				break;
			case "BOTH":
				side = 0;
				break;
			default:
				show_usage(program_name);
				return;
		}
		
		long maxTradeSize;
		try
		{
			maxTradeSize = (long)(Double.parseDouble(args[4]) * 1e8);
  
		}
		catch (NumberFormatException ex)
		{
			System.out.println(ex.getMessage());
			show_usage(program_name);
			return;
		}
		
		long targetPrice;
		try
		{
			targetPrice = (long)(Double.parseDouble(args[5]) * 1e8);
			if (targetPrice <= 0)
				throw new IllegalArgumentException("Invalid Target Price");
		}
		catch (Throwable t)
		{
			System.out.println(t.getMessage());
			show_usage(program_name);
			return;
		}
		
		String user = args[6];
		String password = args[7];
		String second_factor = args.length == 9 ? args[8] : null;
		
		WebSocketClientConnection connection = null;
		
		try
		{
			// start the program
			SimpleTradeClient tradeclient = new SimpleTradeClient(
													broker_id, 
													symbol, 
													new TradingStrategy( maxTradeSize, targetPrice, side));

			// instantiate the protocol engine object to handle the Blinktrade messaging stuff
			WebSocketClientProtocolEngine protocolEngine = new WebSocketClientProtocolEngine();
			
			//subscribe to receive notification events from the broker
			protocolEngine.addActionListener(tradeclient);
			
			// instantiate the connection object
			connection = new WebSocketClientConnection(
												new UserAccountCredentials(broker_id, user, password, second_factor),
												new UserDevice(), 
												protocolEngine);
			
			// start the connection to handle the websocket connectivity and initiate the whole process
			connection.start(url);
			connection.waitClose();
		}
		catch (Throwable t)
		{
			LOG.warn(t);
		}
		finally
		{
			try
			{
				connection.stop();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}	
}