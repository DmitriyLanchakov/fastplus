package com.fxsession.fastplus.handler.moex;

import org.apache.log4j.Logger;
import org.openfast.Message;
import org.openfast.SequenceValue;

import com.fxsession.fastplus.fpf.IFPFHandler;
import com.fxsession.fastplus.fpf.IFPFOrderBook;
import com.fxsession.fastplus.fpf.OnCommand;

/**
 * @author Dmitry Vulf
 * 
 * Incremenetal order book handler
 * 
 * It is possible, though not guaranteed, that a set of these book update messages can be used to construct the current, 
 * correct state of a book without prior book state knowledge. 
 * This process called Natural Refresh. 
 * Prior to beginning a natural refresh, the entire book should be emptied. 
 * Natural refresh assumes no prior knowledge of book state. 
 * Natural Refresh works best for aggregated orderbook feed and for highly liquid securities. 
 *
 */
public class MoexHandlerOLR implements IFPFHandler, IFPFOrderBook{
	
	private static Logger mylogger = Logger.getLogger(MoexHandlerOLR.class);

	static private final String GROUPMDENTRIES = "GroupMDEntries";



	
	
	
	static private final String RPTSEQ = "RptSeq";
	
	
	static private final String BID= "0";  //quote for buy
	
	@Override
	public String getInstrumentID() {
		return "EURUSD000TOM";
	}

	@Override
	public OnCommand push(Message message) {
		OnCommand retval = OnCommand.ON_PROCESS;
		SequenceValue secval =message.getSequence (GROUPMDENTRIES);
//		String msgSeqNum = message.getString(MSGSEQNUM);

		for (int i=0;i < secval.getValues().length;i++){ 
			String Symbol = secval.getValues()[i].getString("Symbol");
			if (Symbol == getInstrumentID()){ //due to up to 3 sequences in one entry I can get wrong instrument (see MoexFeed implementation)
				OrderBookRecord obr = new OrderBookRecord();
			    String key =  secval.getValues()[i].getString(MDENTRYID);
				obr.type = Boolean.getBoolean(secval.getValues()[i].getString(MDENTRYTYPE));
				obr.size = Integer.getInteger(secval.getValues()[i].getString(MDENTRYSIZE));
				obr.px = Double.valueOf(secval.getValues()[i].getString(MDENTRYPX));
				switch (secval.getValues()[i].getString(MDUPDATEACTION)){
				case ADD 		: addList.put(key, obr); mylogger.info("add " + key + " " + obr.toString()); break;
				case UPDATE 	: changeList.put(key, obr) ;mylogger.info("change " + key + " " + obr.toString()); break;
				case DELETE 	: deleteList.put(key, obr); mylogger.info("delete " + key + " " + obr.toString()); break;
				default : break; 
		       }
			}
			
		}
		return retval;
	}

}