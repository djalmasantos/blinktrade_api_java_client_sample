package com.blinktrade.api;

import java.util.EventObject;
import java.util.Vector;

public class EventSourceImpl
{
	private transient Vector<ActionListener> actionListeners;
	protected void fireActionPerformed(EventObject evt)
	{
		// make a copy of the listener list in case anyone adds/removes listeners
		Vector<?> targets = null;
		synchronized (this) 
		{
			if ( actionListeners != null && !actionListeners.isEmpty() )
				targets = (Vector<?>) actionListeners.clone();
		}

		if (targets != null)
		{
			int count = targets.size();
			for (int i=0; i<count; i++) 
			{
				((ActionListener)targets.elementAt(i)).actionPerformed(evt);
			}
		}
	}

	public synchronized void addActionListener(ActionListener l)
	{
		if (actionListeners == null) 
			actionListeners = new Vector<ActionListener>();
		
		if (!actionListeners.contains(l)) 
			actionListeners.addElement(l);
	}
		
	public synchronized void removeActionListener(ActionListener l)
	{
		if ((actionListeners != null) && (actionListeners.contains(l)))
			actionListeners.removeElement(l);
	}
}