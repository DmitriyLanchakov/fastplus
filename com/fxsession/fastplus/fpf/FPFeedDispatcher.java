package com.fxsession.fastplus.fpf;


import java.util.HashMap;

import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;


import org.apache.log4j.Logger;
import org.openfast.session.FastConnectionException;

import com.fxsession.fastplus.handler.moex.MoexHandlerOBR;
import com.fxsession.fastplus.handler.moex.depreciated.MoexHandlerOLR;
import com.fxsession.fastplus.receiver.moex.MoexFeedOBR;
import com.fxsession.fastplus.receiver.moex.depreciated.MoexFeedOLR;
import com.fxsession.utils.FXPException;
import com.fxsession.utils.FXPXml;


/**
 * @author Dmitry Vulf
 * 
 * Main class dispatching all events comind from feeds.
 * Main functions:
 * - Create feeds that we are going to listen
 * - Register handlers (implementors of IFPFHandler). Handlers implement logic specific for each particular instrument i.e. EUR_RUB__TOD
 * - Dispatch every message coming from the created feed (a feed in a processMessage method calls <dispatcher.dispatch> 
 * sending key,msgSeqNum, message. See FPFeed) matching key parameter with getSiteID() and forwarding message to the certain handler.
 * Thus a key coming from feed.processMessage must coincide with getSiteID() of one of the registered handlers in order to be processed        
 *
 */

@SuppressWarnings("unused")
public class FPFeedDispatcher {
	
	private static Logger mylogger = Logger.getLogger(FPFeedDispatcher.class);
	
	/**
	* handlers - the map between instrument received from the feed and its handler
	* 
	*
	* key is a concatenation of the Feed.getSideID and Instrument.getInstrumentID
	* The best way to iterate through this map is :
	*
	*			for (Map.Entry<String,IFPFHandler> entry : handlers.entrySet()) {
	*			    String key = entry.getKey();			    }
	*
	*/
	private class MessageDispathcer{
		private IFPFHandler handler;
		private IFPFeed feed;
		private boolean stopped; 
		MessageDispathcer (IFPFHandler phandler, IFPFeed pfeed){
			handler = phandler;
			feed = pfeed;
			stopped = false;
		}
		IFPFeed getFeed() {return feed;}
		IFPFHandler getHandler() {return handler;}
		boolean isStopped() {return stopped;}
	} 
	
	private final Map <String, MessageDispathcer> handlers = new HashMap<String,MessageDispathcer>();  
		
	
	private void registerHandler(IFPFHandler handler,IFPFeed feed){
		if (handler!=null && feed!=null){
			String mapKey = composeKey(feed,handler.getInstrumentID());
			handlers.put(mapKey, new MessageDispathcer(handler,feed));
		}
		else 
			throw new NullPointerException();
	}
	
	private boolean monitorFeed(){
		boolean retVal = true;
     	try {		
			for (final Map.Entry<String, MessageDispathcer> entry : handlers.entrySet())
			if (!entry.getValue().getFeed().hasStarted()){
				mylogger.error("no response from  " + entry.getValue().getFeed().toString());
				//stopping feed
				if (!entry.getValue().isStopped()){
					entry.getValue().getFeed().stop();
					entry.getValue().stopped = true;
				}
				//restarting it
				new Thread(new Runnable() {
						public void run() {
							try {
								entry.getValue().stopped = false;
								entry.getValue().getFeed().restart();
								mylogger.info("exit thread");
							} catch (Exception e) {
		                    mylogger.error( e);
		                }
		            }
				}).start();
				Thread.sleep(20000);// parameter - millis
				retVal = false;
			}
         } catch (Exception e) {
             mylogger.error( e);
        }
		return retVal;
	}
	
	
	private String composeKey (IFPFeed ifeed, String key){
		
		String feedKey = ifeed.getSiteID();
		
		if (feedKey == null) {
			throw new IllegalArgumentException("define feed ID"); }

		if (key == null) {
			throw new IllegalArgumentException("define key"); }

		return feedKey+key; 
	}
	
	
	private void startFeeds(){
	//each feeds start sending messages in a separate thread
	 	for (final Map.Entry<String, MessageDispathcer> entry : handlers.entrySet()) 
        new Thread(new Runnable() {
            public void run() {
                try {
                	entry.getValue().getFeed().start();
                	mylogger.info("exit thread");
                } catch (Exception e) {
                    mylogger.error( e);
                }
            }
        }).start();
	}

	/**
	* Goes through the list of instrument codes in setting file 
	* and registers it one-by-one in the cycle 
	 * @throws FXPException 
	 * @throws ParserConfigurationException 
	*/
		
	private void registerInstruments() throws ParserConfigurationException, FXPException{
    	String instrString =  FXPXml.readCCYElement(FXPXml.SUBS_LIST);
    	//Parse the string, delimiters either "," or ";"
    	String delims = "[ ;,]+";
    	String[] instrList = instrString.split(delims);
    	for (int i = 0; i < instrList.length; i++)
    		registerHandler(new MoexHandlerOBR(instrList[i]),new MoexFeedOBR(this));

	}
	
	public void dispatch(IFPFeed ifeed, //pointer for feed which called this method ie dispatch(this...)
						FPFMessage message) throws FastConnectionException  // the message itself
	{
		try {
			MessageDispathcer dispatcher = handlers.get(composeKey(ifeed,message.getKeyFieldValue()));
			if (dispatcher!=null){
				IFPFHandler handle = dispatcher.getHandler();
				OnCommand command = handle.push(message);
				switch(command){
					case ON_STOP_FEED : ifeed.stopProcess(); break;

				default:
					break;
				}
			}
		}catch(Exception e) {
        	throw new FastConnectionException(e);
        }
	}
	
	
    public void run(){
    	try {
    		//register instruments 
		   registerInstruments();
		    //start feeds
		   startFeeds();
    		 
   		   new Thread(new Runnable() {
				public void run() {
    				try {
    					while (true){
    						Thread.sleep(10000);// parameter - millis
    					    monitorFeed();
    					}
    				} catch (Exception e) {
    					mylogger.error( e);
    				}
    			}
    		}).start();
   		   
		} catch (Exception e1) {
		  System.out.println(e1);
		  System.exit(-1);
		}
    		
    }
}
