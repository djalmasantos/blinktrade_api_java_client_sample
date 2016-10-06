package com.blinktrade.api.websocket.client;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class UserDevice 
{
	private final String _fingerPrint = "1730142891";
	private JsonObject _stuntip;
	public UserDevice()
	{
		JsonObjectBuilder jsonObjBuilder = Json.createObjectBuilder();
		jsonObjBuilder.add("local", Util.GetLocalIPAddress());
		jsonObjBuilder.add("public", Json.createArrayBuilder().add(Util.GetExternalIpAddress()).build());
		_stuntip = jsonObjBuilder.build();
	}

	public String getFingerPrint()
	{ 
		return _fingerPrint; 
	}

	public JsonObject getStuntip()
	{
		return _stuntip;
	}
}