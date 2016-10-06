package com.blinktrade.api;

import java.util.EventListener;
import java.util.EventObject;

public interface ActionListener extends EventListener
{
	void actionPerformed(EventObject e);
}