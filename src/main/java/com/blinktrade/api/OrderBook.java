package com.blinktrade.api;

import java.util.LinkedList;
import java.util.List;
import javax.json.JsonObject;

public class OrderBook 
{
	private List<Order> _buyside = new LinkedList<Order>();
	private List<Order> _sellside = new LinkedList<Order>();
	private String _symbol;

	public OrderBook(String symbol)
	{
		_symbol = symbol;
	}
	
	public String getSymbol()
	{
		return _symbol;
	}
	
	public IOrder getBestOffer()
	{
		return _sellside.size() > 0 ? _sellside.get(0) : null;
	}

	public IOrder getBestBid()
	{
		return _buyside.size() > 0 ? _buyside.get(0) : null;
	}
	
	public void Clear()
	{
		_buyside.clear();
		_sellside.clear();
	}
	
	public void AddOrder(JsonObject entry)
	{
		int index = entry.getInt("MDEntryPositionNo") - 1;
		Order order = new Order();
		order.setPrice(entry.getJsonNumber("MDEntryPx").longValueExact());
		order.setQty(entry.getJsonNumber("MDEntrySize").longValueExact());
		order.setUserId(entry.getJsonNumber("UserID").longValueExact());
		order.setBroker(entry.getString("Broker"));
		order.setOrderId(entry.getJsonNumber("OrderID").longValueExact());
		order.setSide(entry.getString("MDEntryType").charAt(0));
		order.setOrderDate(entry.getString("MDEntryDate"));
		order.setOrderTime(entry.getString("MDEntryTime"));

		if (order.getSide() == OrderBook.OrdSide.BUY)
			_buyside.add(index, order);
		else if (order.getSide() == OrderBook.OrdSide.SELL)
			_sellside.add(index, order);
		else
			throw new IllegalArgumentException("Invalid OrderBook Side : " + order.getSide() );
	}
	
	public void UpdateOrder(JsonObject entry)
	{
		int index = entry.getInt("MDEntryPositionNo") - 1;
		Order order = new Order();
		order.setPrice(entry.getJsonNumber("MDEntryPx").longValueExact());
		order.setQty(entry.getJsonNumber("MDEntrySize").longValueExact());
		order.setUserId(entry.getJsonNumber("UserID").longValueExact());
		order.setBroker(entry.getString("Broker"));
		order.setOrderId(entry.getJsonNumber("OrderID").longValueExact());
		order.setSide(entry.getString("MDEntryType").charAt(0));
		order.setOrderDate(entry.getString("MDEntryDate"));
		order.setOrderTime(entry.getString("MDEntryTime"));

		if (order.getSide() == OrderBook.OrdSide.BUY)  
			_buyside.set(index, order);
		else if (order.getSide() == OrderBook.OrdSide.SELL)
			_sellside.set(index, order);
		else
			throw new IllegalArgumentException("Invalid OrderBook Side : " + order.getSide() );
	}
	
	public void DeleteOrder(JsonObject entry)
	{
		int index = entry.getInt("MDEntryPositionNo") - 1;
		char side = entry.getString("MDEntryType").charAt(0);

		if (side == OrderBook.OrdSide.BUY) 
			_buyside.remove(index);
		else if (side == OrderBook.OrdSide.SELL)
			_sellside.remove(index);
		else
			throw new IllegalArgumentException("Invalid OrderBook Side : " + side );
	}
	
	public void DeleteOrdersThru(JsonObject entry)
	{
		int count = entry.getInt("MDEntryPositionNo");
		char side = entry.getString("MDEntryType").charAt(0);

		if (side == OrderBook.OrdSide.BUY)
			_buyside.subList(0, count).clear();
		else if (side == OrderBook.OrdSide.SELL)
			_sellside.subList(0, count).clear();
		else
			throw new IllegalArgumentException("Invalid OrderBook Side : " + side );
	}
	
	@Override
	public String toString()
	{
		int max_count = (_buyside.size() > _sellside.size() ? _buyside.size() : _sellside.size());
		StringBuilder result = new StringBuilder(); 
		result.append("*** SYMBOL --> " + this._symbol + " ***\nBUYER;QUANTITY;PRICE;PRICE;QUANTITY;SELLER\n");
		for (int i = 0; i < max_count; i++)
		{
			String left = "";
			if (i < _buyside.size())
			{
				Order order = _buyside.get(i);
				left = Long.toString(order.getUserId())+
				       ';' + 
				       Long.toString(order.getQty())+
				       ';' + 
				       Long.toString(order.getPrice())+
				       ';';
			}
			else
			{
				left += ";;;";
			}

			String right = "";
			if (i < _sellside.size())
			{
				Order order = _sellside.get(i);
				right = Long.toString(order.getPrice()) + 
				        ';' + 
				        Long.toString(order.getQty()) + 
				        ';' + 
				        Long.toString(order.getUserId());
			}
			else
			{
				right += ";;";
			}

			// append a full line
			result.append(left);
			result.append(right);
			result.append('\n');
		}

		return result.toString();
	}
	
	public interface IOrder
	{
		long getPrice();
		long getQty();
		long getUserId();
		String getBroker();
		long getOrderId();
		char getSide();
		String getOrderTime();
		String getOrderDate();
	}
   
	public class Order implements IOrder
	{
		private long _price = 0;
		private long _qty = 0;
		private long _user_id = 0;
		private String _broker = "";
		private long _order_id = 0;
		private char _side;
		private String _order_time = "";
		private String _order_date = "";
		
		public long getPrice() {
			return _price;
		}
		public long getQty() {
			return _qty;
		}
		public long getUserId() {
			return _user_id;
		}
		public String getBroker() {
			return _broker;
		}
		public long getOrderId() {
			return _order_id;
		}
		public char getSide() {
			return _side;
		}
		public String getOrderTime() {
			return _order_time;
		}
		public String getOrderDate() {
			return _order_date;
		}
		public void setPrice(long price) {
			this._price = price;
		}
		public void setQty(long qty) {
			this._qty = qty;
		}
		public void setUserId(long user_id) {
			this._user_id = user_id;
		}
		public void setBroker(String broker) {
			this._broker = broker;
		}
		public void setOrderId(long order_id) {
			this._order_id = order_id;
		}
		public void setSide(char side) {
			this._side = side;
		}
		public void setOrderTime(String order_time) {
			this._order_time = order_time;
		}
		public void setOrderDate(String order_date) {
			this._order_date = order_date;
		}
	}
	
	public class OrdSide
	{
		public final static char BUY = '0';
		public final static char SELL = '1';
	}
}
