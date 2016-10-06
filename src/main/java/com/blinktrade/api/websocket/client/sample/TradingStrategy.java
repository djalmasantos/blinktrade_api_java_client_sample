package com.blinktrade.api.websocket.client.sample;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import com.blinktrade.api.ExecInst;
import com.blinktrade.api.OrdStatus;
import com.blinktrade.api.OrdType;
import com.blinktrade.api.OrderBook;
import com.blinktrade.api.OrderSide;
import com.blinktrade.api.websocket.client.*;

public class TradingStrategy 
{
	private static final Logger LOG = Log.getLogger(TradingStrategy.class);
	private String _strategySellOrderClorid = null;
	private String _strategyBuyOrderClorid = null;
	private char _strategySide = 0; // 0 default: run both SELL AND BUY 
	private static final long _minTradeSize = (long)(0.0001 * 1e8); // 10,000 Satoshi
	private long _maxTradeSize = 0;
	private long _targetPrice = 0;
	private double _startTime;
	private AtomicLong _cloridSeqNum = new AtomicLong();
	private ITradeClientService _tradeclient;

	public TradingStrategy(long max_trade_size, long target_price, char side)
	{
		_maxTradeSize = max_trade_size;
		_targetPrice = target_price;
		_strategySide = side;
		_startTime = Util.ConvertToUnixTimestamp(new Date());
	}
	
	public void setTradeClient(ITradeClientService tradeclient) { _tradeclient = tradeclient; }

	
	public void runStrategy(IWebSocketClientConnection webSocketConnection, String symbol) throws IOException
	{
		// Run a dummy strategy to try to have an order on the top of the both sides of the book, 
	    // but never executing as a taker
		if (_maxTradeSize > 0)
		{
			if (_strategySide == OrderSide.BUY || _strategySide == 0) // buy or both
			{
				runBuyStrategy(webSocketConnection, symbol);  
			}

			if (_strategySide == OrderSide.SELL || _strategySide == 0) // sell or both
			{
				runSellStrategy(webSocketConnection, symbol);
			}
		}
	}

	private String MakeClOrdId()
	{
		return "BLKTRD-" + Double.toString(_startTime) + "-" + _cloridSeqNum.incrementAndGet();
	}
	
	private void runBuyStrategy(IWebSocketClientConnection webSocketConnection, String symbol) throws IOException
	{
		long qty = _maxTradeSize;
		OrderBook.IOrder bestBid = _tradeclient.getOrderBook(symbol).getBestBid();
		if (bestBid != null)
		{
			if (bestBid.getUserId() != _tradeclient.getUserId())
			{
				// buy @ 1 cent above the best price
				long buyPrice = bestBid.getPrice() + (long)(0.01 * 1e8);
				if (buyPrice < _targetPrice)
				{
					MiniOMS.Order strategyBuyOrder = _tradeclient.getMiniOMS().GetOrderByClOrdID(_strategyBuyOrderClorid);
					// cancel the previous sent order since it is not possible to modify the order
					if (strategyBuyOrder != null)
					{
						char ordStatus = strategyBuyOrder.getOrdStatus();
						switch (ordStatus)
						{
							case OrdStatus.PENDING_NEW: // client control - in the case no response was received
								return; // wait the confirmation
							case OrdStatus.NEW:
								qty = strategyBuyOrder.getOrderQty();
								_tradeclient.CancelOrderByClOrdID(webSocketConnection, strategyBuyOrder.getClOrdID());
								break;
							case OrdStatus.PARTIALLY_FILLED:
								if (strategyBuyOrder.getLeavesQty() > _minTradeSize)
								{
									LOG.info("Partially Filled condition with remaining qty : {} -> {}", 
											strategyBuyOrder.getLeavesQty(), 
											strategyBuyOrder.getClOrdID());
									
									qty = strategyBuyOrder.getLeavesQty();
									_tradeclient.CancelOrderByClOrdID(webSocketConnection, strategyBuyOrder.getClOrdID());
								}
								else
								{
								    // too small size, forget the order in the book for further execution
								    _strategySellOrderClorid = null;   
								}
								break;
							default:
								break;
						}
					}
					else
					{
						LOG.info("{}", "No previous BUY order to cancel...");
					}
					// send order (Participate don't initiate - aka book or cancel) 
					_strategyBuyOrderClorid = _tradeclient.SendOrder(
													webSocketConnection,
													symbol,
													qty,
													buyPrice,
													OrderSide.BUY,
													_tradeclient.getBrokerId(),
													MakeClOrdId(),
													OrdType.LIMIT,
													ExecInst.PARTICIPATE_DONT_INITIATE);
					// avoid too many messages per second
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		else
		{
			// TODO: empty book scenario
		}
	}

	private void runSellStrategy(IWebSocketClientConnection webSocketConnection, String symbol) throws IOException
	{
		long qty = _maxTradeSize;
		OrderBook.IOrder bestOffer = _tradeclient.getOrderBook(symbol).getBestOffer();
		if (bestOffer != null)
		{
			if (bestOffer.getUserId() != _tradeclient.getUserId())
			{
				// sell @ 1 cent bellow the best price
				long sellPrice = bestOffer.getPrice() - (long)(0.01 * 1e8);
				if (sellPrice >= _targetPrice)
				{
					// cancel the previous sent order since it is not possible to modify the order
					MiniOMS.Order strategySellOrder = _tradeclient.getMiniOMS().GetOrderByClOrdID(_strategySellOrderClorid);
					if (strategySellOrder != null)
					{
						char ordStatus = strategySellOrder.getOrdStatus();
						switch (ordStatus)
						{
							case OrdStatus.PENDING_NEW: // client control - in the case no response was received
								return; // wait the confirmation
							case OrdStatus.NEW:
								qty = strategySellOrder.getOrderQty();
								_tradeclient.CancelOrderByClOrdID(webSocketConnection, strategySellOrder.getClOrdID());
								break;
							case OrdStatus.PARTIALLY_FILLED:
								if (strategySellOrder.getLeavesQty() > _minTradeSize)
								{
									LOG.info("Partially Filled condition with remaining qty : {} -> {}", 
											strategySellOrder.getLeavesQty(), 
											strategySellOrder.getClOrdID());
									
									qty = strategySellOrder.getLeavesQty();
									_tradeclient.CancelOrderByClOrdID(webSocketConnection, strategySellOrder.getClOrdID());
								}
								else
								{
								    // too small size, forget the order in the book for further execution
								    _strategySellOrderClorid = null;   
								}
								break;
							default:
								break;
						}
					}
					else
					{
						LOG.info("{}", "No previous SELL order to cancel...");
					}
					// send the order (Participate don't initiate - aka book or cancel)
					_strategySellOrderClorid = _tradeclient.SendOrder(
													webSocketConnection,
													symbol,
													qty,
													sellPrice,
													OrderSide.SELL,
													_tradeclient.getBrokerId(),
													MakeClOrdId(),
													OrdType.LIMIT,
													ExecInst.PARTICIPATE_DONT_INITIATE);
					// avoid too many messages per second
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		else
		{
			// TODO: empty book scenario
		}
	}
}