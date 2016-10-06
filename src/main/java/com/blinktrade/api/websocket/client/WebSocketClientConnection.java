package com.blinktrade.api.websocket.client;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@WebSocket
public class WebSocketClientConnection
		implements IWebSocketClientConnection
{
	private WebSocketClient _wsclient = new WebSocketClient(new SslContextFactory()); //SSL is required
	private Session _session = null;
	private IWebSocketClientProtocolEngine _protocolEngine = null;
	private ScheduledExecutorService _scheduler = Executors.newScheduledThreadPool(1);
	private CountDownLatch _closeLatch = new CountDownLatch(1);
	private UserAccountCredentials _account = null;
	private UserDevice _device = null;
	private AtomicInteger _seqnum = new AtomicInteger();
	private AtomicLong _receivedMessageCounter = new AtomicLong();
	private boolean _loggedOn = false;
	private boolean _enableTestRequest = true;
	private static final int _testRequestDelayInSeconds = 1;
	private static final Logger LOG = Log.getLogger(WebSocketClientConnection.class);
	
	public WebSocketClientConnection(
	    UserAccountCredentials account, 
	    UserDevice device, 
	    WebSocketClientProtocolEngine protocolEngine)
	{
		_account = account;
		_device = device;
		_protocolEngine = protocolEngine;
	}	
	
	public void start(String serverUri) throws Exception
	{
	    // change the message size capacity before starting the connection
	    // this is required because the order book snapshot might be very large
	    _wsclient.getPolicy().setMaxTextMessageSize(512*1024);  
		_wsclient.start();
		_wsclient.connect(this,URI.create(serverUri));
	}
	
	public void waitClose() throws InterruptedException
	{
		_closeLatch.await();
	}

	public void stop() throws Exception
	{
		_scheduler.shutdown();
		_wsclient.stop();
	}

	@OnWebSocketConnect
	public void OnOpen(Session sess)
	{
		LOG.info("OnOpen({})",sess);
		this._session = sess;
		_protocolEngine.OnOpen(this);
		_scheduler.scheduleAtFixedRate(
		    new TestRequest(this), 
		    _testRequestDelayInSeconds, 
		    _testRequestDelayInSeconds, 
		    TimeUnit.SECONDS
		);
	}

	@OnWebSocketClose
	public void OnClose(Session session, int statusCode, String reason)
	{
		LOG.info("OnClose({}, {})", statusCode, reason);
		_protocolEngine.OnClose(this);
		_closeLatch.countDown();
	}

	@OnWebSocketError
	public void OnError(Session session, Throwable cause)
	{
		LOG.warn(cause);
		_protocolEngine.OnError(cause, this);
	}

	@OnWebSocketMessage
	public void OnMessage(Session session, String msg)
	{
		long count = _receivedMessageCounter.incrementAndGet();
		LOG.info("OnMessage() - {} {} {}", count, msg, session);
		this._protocolEngine.OnMessage(msg, this);
	}

	@Override
	public void SendMessage(String message) throws IOException 
	{
		this._session.getRemote().sendString(message);
	}
	
	@Override
	public void SendTestRequest() throws IOException
	{
		this._protocolEngine.SendTestRequest(this);
	}

	@Override
	public void Shutdown() throws IOException 
	{
		this._session.disconnect();
	}

	@Override
	public int NextOutgoingSeqNum() 
	{
		return this._seqnum.incrementAndGet();
	}

	@Override
	public boolean IsConnected() 
	{
		return this._session.isOpen();
	}

	@Override
	public boolean IsLoggedOn() 
	{
		return this._loggedOn;
	}

	@Override
	public void LoggedOn(boolean value)
	{
		this._loggedOn = value;
	}
	
	@Override
	public boolean IsTestRequestEnabled()
	{
		return this._enableTestRequest;
	}
	
	@Override
	public void EnableTestRequest(boolean value) 
	{
		this._enableTestRequest = value;
	}

	@Override
	public UserDevice getDevice() 
	{
		return this._device;
	}

	@Override
	public UserAccountCredentials getUserAccount() 
	{
		return this._account;
	}
	
	@Override
	public long receivedMessageCounter() 
	{ 
		return this._receivedMessageCounter.get(); 
	}
}