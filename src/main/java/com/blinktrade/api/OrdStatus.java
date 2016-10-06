package com.blinktrade.api;

public final class OrdStatus 
{
	public final static char NEW = '0';
	public final static char PARTIALLY_FILLED = '1';
	public final static char FILLED = '2';
	public final static char CANCELED = '4';
	public final static char PENDING_CANCEL = '6'; // client control - in the case no response was received for a Cancel Request
	public final static char STOPPED = '7';
	public final static char REJECTED = '8';
	public final static char PENDING_NEW = 'A'; // client control - in the case no response was received
}
