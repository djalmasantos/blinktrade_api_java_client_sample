package com.blinktrade.api.websocket.client;

public class UserAccountCredentials
{
	private int _brokerId;
	private String _username;
	private String _password;
	private String _secondFactor;
	

	public UserAccountCredentials(int broker_id, String username, String password)
	{
		_brokerId = broker_id;
		_username = username;
		_password = password;
	   
	}
	
	public UserAccountCredentials(int broker_id, String username, String password, String second_factor)
	{
		_brokerId = broker_id;
		_username = username;
		_password = password;
		_secondFactor = second_factor;
	}

	public int getBrokerId()
	{
		return _brokerId;
	}
	
	public String getUsername() 
	{
		return _username;
	}

	public String getPassword()
	{
		return _password;
	}
	
	public void set_secondFactor(String value) 
	{
		_secondFactor = value;
	}

	public String getSecondFactor()
	{
		return _secondFactor;
	}
	
	public void set_password(String value) 
	{
		_password = value;
	}
}