package com.blinktrade.api.websocket.client.sample;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.blinktrade.api.OrdStatus;
import com.blinktrade.api.TimeInForce;

public class MiniOMS 
{
	private Map<String, MiniOMS.Order> _orders = new LinkedHashMap<String, Order>();
	
	public void AddOrder(Order order)
    {
		if ( order == null ) 
			throw new NullPointerException();
		
		if ( _orders.containsKey(order) )
			throw new IllegalArgumentException();
		
		_orders.put(order.getClOrdID(), order);
    }

    public Order GetOrderByClOrdID(String clOrdID)
	{
		return _orders.get(clOrdID);
	}


	public Order GetOrderByOrderID(long orderId)
	{
		throw new java.lang.UnsupportedOperationException();
	}


	public boolean RemoveOrderByClOrdID(String clOrdID)
	{
		return (_orders.remove(clOrdID) != null ? true : false); 
	}
	
	@Override
	public String toString() 
	{
		StringBuilder sbuilder = new StringBuilder();
		sbuilder.append("ClOrdID;OrderID;OrdStatus;Symbol;Side;Price;OrderQty;LeavesQty;CumQty;AvgPx;OrderDate\n");
		// dump all orders
		Collection<Order> allOrders = _orders.values();
		for ( Order order : allOrders )
		{
			sbuilder.append(order.getClOrdID()).append(';');
			sbuilder.append(order.getOrderID()).append(';');
			sbuilder.append(order.getOrdStatus()).append(';');
			sbuilder.append(order.getSymbol()).append(';');
			sbuilder.append(order.getSide()).append(';');
			sbuilder.append(order.getPrice()).append(';');
			sbuilder.append(order.getOrderQty()).append(';');
			sbuilder.append(order.getLeavesQty()).append(';');
			sbuilder.append(order.getCumQty()).append(';');
			sbuilder.append(order.getAvgPx()).append(';');
			sbuilder.append(order.getOrderDate()).append(';');
			sbuilder.append('\n');
		}
		return sbuilder.toString();
	}
	
	
	public interface IOrder
	{
		String getClOrdID();
		char getOrdStatus();
		char getSide();
		long getOrderID();
		long getCumQty();
		long getLeavesQty();
		long getCxlQty();
		long getAvgPx();
		long getLastShares();
		long getLastPx();
		String getSymbol();
		char getOrdType();
		long getOrderQty();
		long getPrice();
		long getVolume();
		String getOrderDate();
		char getTimeInForce();
	}
	
	public class Order implements IOrder
	{
		private String _clOrdID;
		private long _orderID = 0;
		private long _cumQty = 0;
		private char _ordStatus = OrdStatus.PENDING_NEW;
		private long _leavesQty = 0;
		private long _cxlQty = 0;
		private long _avgPx = 0;
		private long _lastShares = 0;
		private long _lastPx = 0;
		private String _symbol;
		private char _side = '\0';
		private char _ordType;
		private long _orderQty = 0;
		private long _price = 0;
		private long _volume = 0;
		private String _orderDate = "";
		private char _timeInForce = TimeInForce.GOOD_TILL_CANCEL; // all blinktrade orders are GTC
		
		@Override
		public String getClOrdID() {
			return this._clOrdID;
		}

		@Override
		public char getOrdStatus() {
			return this._ordStatus;
		}

		@Override
		public char getSide() {
			return this._side;
		}

		@Override
		public long getOrderID() {
			return this._orderID;
		}

		@Override
		public long getCumQty() {
			return this._cumQty;
		}

		@Override
		public long getLeavesQty() {
			return this._leavesQty;
		}

		@Override
		public long getCxlQty() {
			return this._cxlQty;
		}

		@Override
		public long getAvgPx() {
			return this._avgPx;
		}

		@Override
		public long getLastShares() {
			return this._lastShares;
		}

		@Override
		public long getLastPx() {
			return this._lastPx;
		}

		@Override
		public String getSymbol() {
			return this._symbol;
		}

		@Override
		public char getOrdType() {
			return this._ordType;
		}

		@Override
		public long getOrderQty() {
			return this._orderQty;
		}

		@Override
		public long getPrice() {
			return this._price;
		}

		@Override
		public long getVolume() {
			return this._volume;
		}

		@Override
		public String getOrderDate() {
			return this._orderDate;
		}

		@Override
		public char getTimeInForce() {
			return this._timeInForce;
		}
		
		public void set_clOrdID(String clOrdID) {
			this._clOrdID = clOrdID;
		}

		public void set_orderID(long orderID) {
			this._orderID = orderID;
		}

		public void set_cumQty(long cumQty) {
			this._cumQty = cumQty;
		}

		public void set_ordStatus(char ordStatus) {
			this._ordStatus = ordStatus;
		}

		public void set_leavesQty(long leavesQty) {
			this._leavesQty = leavesQty;
		}

		public void set_cxlQty(long cxlQty) {
			this._cxlQty = cxlQty;
		}

		public void set_avgPx(long avgPx) {
			this._avgPx = avgPx;
		}

		public void set_lastShares(long lastShares) {
			this._lastShares = lastShares;
		}

		public void set_lastPx(long lastPx) {
			this._lastPx = lastPx;
		}

		public void set_symbol(String symbol) {
			this._symbol = symbol;
		}

		public void set_side(char side) {
			this._side = side;
		}

		public void set_ordType(char ordType) {
			this._ordType = ordType;
		}

		public void set_orderQty(long orderQty) {
			this._orderQty = orderQty;
		}

		public void set_price(long price) {
			this._price = price;
		}

		public void set_volume(long volume) {
			this._volume = volume;
		}

		public void set_orderDate(String orderDate) {
			this._orderDate = orderDate;
		}

		public void set_timeInForce(char timeInForce) {
			this._timeInForce = timeInForce;
		}
	}
}
