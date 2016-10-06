package com.blinktrade.api;

import java.util.EventObject;
import javax.json.JsonObject;

public class SystemEvent extends EventObject
{
	private static final long serialVersionUID = 1L;
	public SystemEvent(Object source)
	{
		super( source );
	}
	public SystemEventType evtType;
	public JsonObject json;
}