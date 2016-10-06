package com.blinktrade.api.websocket.client;

import java.io.IOException;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class TestRequest implements Runnable 
{
	private IWebSocketClientConnection _connection;
	private long _nextExpectedCounter = 0;
	private boolean _disconnect = false;
	private boolean _shutdown = false;
	private static final Logger LOG = Log.getLogger(TestRequest.class);

	public TestRequest(IWebSocketClientConnection connection)
	{
		_connection = connection;
	}

	public void run() 
	{
		if ( _connection.IsConnected() && _connection.IsTestRequestEnabled() ) 
		{
			try 
			{
				if (_nextExpectedCounter > _connection.receivedMessageCounter()) 
				{
					if (!_disconnect) 
					{
						_connection.SendTestRequest();
						_disconnect = true;
						_shutdown = false;
					}
					else 
					{
						// second chance before disconnecting
						if (!_shutdown)
						{
							_shutdown = true;
							return;
						}
						
						if (_nextExpectedCounter > _connection.receivedMessageCounter())
						{
							LOG.warn("{}", "Websocket connection not responding");
							_connection.Shutdown();
							return;
						}
						_disconnect = false;
					}
				}
				else 
				{
					_disconnect = false;
				}
				// update expectation for next iteration
				_nextExpectedCounter = _connection.receivedMessageCounter() + 1;
			}
			catch(IOException ex)
			{
				LOG.warn(ex);
			}
		}
	}
}