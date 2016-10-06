package com.blinktrade.api.websocket.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

public class Util 
{
	public static long ConvertToUnixTimestamp(Date date)
	{
		return date.getTime() / 1000;
	}
	
	public static String GetLocalIPAddress()
	{
		Enumeration<NetworkInterface> e;
		try {
			e = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e1) {
			e1.printStackTrace();
			return "";
		}
		while(e.hasMoreElements())
		{
			NetworkInterface n = (NetworkInterface) e.nextElement();
			Enumeration<InetAddress> ee = n.getInetAddresses();
			while (ee.hasMoreElements())
			{
				InetAddress i = (InetAddress) ee.nextElement();
				if ( i.isSiteLocalAddress() )
					return i.getHostAddress();
			}
		} 
		return "";
	}
 
	public static String GetExternalIpAddress()
	{
		try 
		{
			 URL whatismyip = new URL("http://checkip.amazonaws.com");
			 BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
			 String ip = in.readLine();
			 return ip;
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
		return "";
	}
	
	// awful workaround to get a JsonObjectBuilder with JsonObject content because JsonObject is immutable
	public static JsonObjectBuilder jsonObjectToBuilder(JsonObject jo, List<String> excludeKeys) 
	{
		JsonObjectBuilder builder = Json.createObjectBuilder();
		for (Entry<String, JsonValue> entry : jo.entrySet()) 
		{
			if ( excludeKeys == null || !excludeKeys.contains(entry.getKey()))
				builder.add(entry.getKey(), entry.getValue());
		}
		return builder;
	}
	
	public static JsonObjectBuilder jsonObjectToBuilder(JsonObject jo)
	{
		return jsonObjectToBuilder(jo, null);
	}
}
