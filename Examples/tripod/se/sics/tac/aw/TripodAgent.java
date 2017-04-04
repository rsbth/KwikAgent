/**
 * TAC AgentWare
 * http://www.sics.se/tac        tac-dev@sics.se
 *
 * Copyright (c) 2001-2005 SICS AB. All rights reserved.
 *
 * SICS grants you the right to use, modify, and redistribute this
 * software for noncommercial purposes, on the conditions that you:
 * (1) retain the original headers, including the copyright notice and
 * this text, (2) clearly document the difference between any derived
 * software and the original, and (3) acknowledge your use of this
 * software in pertaining publications and reports.  SICS provides
 * this software "as is", without any warranty of any kind.  IN NO
 * EVENT SHALL SICS BE LIABLE FOR ANY DIRECT, SPECIAL OR INDIRECT,
 * PUNITIVE, INCIDENTAL OR CONSEQUENTIAL LOSSES OR DAMAGES ARISING OUT
 * OF THE USE OF THE SOFTWARE.
 *
 * -----------------------------------------------------------------
 *
 * Author  : Joakim Eriksson, Niclas Finne, Sverker Janson
 * Created : 23 April, 2002
 * Updated : $Date: 2005/06/07 19:06:16 $
 *	     $Revision: 1.1 $
 * ---------------------------------------------------------
 * DummyAgent is a simplest possible agent for TAC. It uses
 * the TACAgent agent ware to interact with the TAC server.
 *
 * Important methods in TACAgent:
 *
 * Retrieving information about the current Game
 * ---------------------------------------------
 * int getGameID()
 *  - returns the id of current game or -1 if no game is currently plaing
 *
 * getServerTime()
 *  - returns the current server time in milliseconds
 *
 * getGameTime()
 *  - returns the time from start of game in milliseconds
 *
 * getGameTimeLeft()
 *  - returns the time left in the game in milliseconds
 *
 * getGameLength()
 *  - returns the game length in milliseconds
 *
 * int getAuctionNo()
 *  - returns the number of auctions in TAC
 *
 * int getClientPreference(int client, int type)
 *  - returns the clients preference for the specified type
 *   (types are TACAgent.{ARRIVAL, DEPARTURE, HOTEL_VALUE, E1, E2, E3}
 *
 * int getAuctionFor(int category, int type, int day)
 *  - returns the auction-id for the requested resource
 *   (categories are TACAgent.{CAT_FLIGHT, CAT_HOTEL, CAT_ENTERTAINMENT
 *    and types are TACAgent.TYPE_INFLIGHT, TACAgent.TYPE_OUTFLIGHT, etc)
 *
 * int getAuctionCategory(int auction)
 *  - returns the category for this auction (CAT_FLIGHT, CAT_HOTEL,
 *    CAT_ENTERTAINMENT)
 *
 * int getAuctionDay(int auction)
 *  - returns the day for this auction.
 *
 * int getAuctionType(int auction)
 *  - returns the type for this auction (TYPE_INFLIGHT, TYPE_OUTFLIGHT, etc).
 *
 * int getOwn(int auction)
 *  - returns the number of items that the agent own for this
 *    auction
 *
 * Submitting Bids
 * ---------------------------------------------
 * void submitBid(Bid)
 *  - submits a bid to the tac server
 *
 * void replaceBid(OldBid, Bid)
 *  - replaces the old bid (the current active bid) in the tac server
 *
 *   Bids have the following important methods:
 *    - create a bid with new Bid(AuctionID)
 *
 *   void addBidPoint(int quantity, float price)
 *    - adds a bid point in the bid
 *
 * Help methods for remembering what to buy for each auction:
 * ----------------------------------------------------------
 * int getAllocation(int auctionID)
 *   - returns the allocation set for this auction
 * void setAllocation(int auctionID, int quantity)
 *   - set the allocation for this auction
 *
 *
 * Callbacks from the TACAgent (caused via interaction with server)
 *
 * bidUpdated(Bid bid)
 *  - there are TACAgent have received an answer on a bid query/submission
 *   (new information about the bid is available)
 * bidRejected(Bid bid)
 *  - the bid has been rejected (reason is bid.getRejectReason())
 * bidError(Bid bid, int error)
 *  - the bid contained errors (error represent error status - commandStatus)
 *
 * quoteUpdated(Quote quote)
 *  - new information about the quotes on the auction (quote.getAuction())
 *    has arrived
 * quoteUpdated(int category)
 *  - new information about the quotes on all auctions for the auction
 *    category has arrived (quotes for a specific type of auctions are
 *    often requested at once).
 *    
 * auctionClosed(int auction)
 *  - the auction with id "auction" has closed
 *
 * transaction(Transaction transaction)
 *  - there has been a transaction
 *
 * gameStarted()
 *  - a TAC game has started, and all information about the
 *    game is available (preferences etc).
 *
 * gameStopped()
 *  - the current game has ended
 *
 */

package se.sics.tac.aw;
import se.sics.tac.util.ArgEnumerator;

import java.util.logging.*;

public class TripodAgent extends AgentImpl {

  private static final Logger log =
    Logger.getLogger(TripodAgent.class.getName());

  private static final boolean DEBUG = false;

  private float[] prices;
  
  private float[] myCurrentFlightBid = new float[28];  
  private float[] myCurrentEnBid=new float[28];
  
  private float[] flightAskPrice_1 = new float[8];//flight price of 1st point
  private float[] flightAskPrice_2 = new float[8];//flight price of 2nd point
  private float[] flightAskPrice_3 = new float[8];//flight price of 3rd point
  
  private float AveOfClientPrefValue=0;//the average value of the entertainment preference of all clients.

  protected void init(ArgEnumerator args) {
    prices = new float[agent.getAuctionNo()];
  }

  public void quoteUpdated(Quote quote) {
	    int auction = quote.getAuction();
	    int auctionCategory = agent.getAuctionCategory(auction);
	    int i = auction;
	    int type;
	    int inFlight;
      	int outFlight;
	    
	    if (auctionCategory == TACAgent.CAT_HOTEL) {
	      int alloc = agent.getAllocation(auction) - agent.getOwn(i);
	      if (alloc > 0 && quote.hasHQW(agent.getBid(auction)) && quote.getHQW() < alloc) {
			
			// Can not own anything in hotel auctions.
			if(agent.getAuctionType(i)==TACAgent.TYPE_CHEAP_HOTEL){
	        	if(prices[auction] <= 600){
	        	//if(quote.getAskPrice() <= 500){
	          		prices[auction] = quote.getAskPrice() + 150.0f;
	          	}
	          	/*********
	        	else {
	        		type = TACAgent.TYPE_GOOD_HOTEL;
	        		inFlight = agent.getClientPreference(i, TACAgent.ARRIVAL);
	        		outFlight = agent.getClientPreference(i, TACAgent.DEPARTURE);
	        		for (int d = inFlight; d < outFlight; d++) {
						auction = agent.getAuctionFor(TACAgent.CAT_HOTEL, type, d);
						log.finer("Adding hotel for day: " + d + " on " + auction);
						agent.setAllocation(auction, agent.getAllocation(auction) + 1);
      				}
	     	   }********/
	     	}
	  	 	else if (agent.getAuctionType(i)==TACAgent.TYPE_GOOD_HOTEL){
	      		if(prices[auction] <= 600){
	      		//if(quote.getAskPrice() <= 500){
	        		prices[auction] = quote.getAskPrice() + 200.0f;
	   			}
	   			/********
	   			else {
	        		type = TACAgent.TYPE_CHEAP_HOTEL;
	        		inFlight = agent.getClientPreference(i, TACAgent.ARRIVAL);//i代表第几个client，有错，待查，否则删掉，恢复原来代码
	        		outFlight = agent.getClientPreference(i, TACAgent.DEPARTURE);
	        		for (int d = inFlight; d < outFlight; d++) {
						auction = agent.getAuctionFor(TACAgent.CAT_HOTEL, type, d);
						log.finer("Adding hotel for day: " + d + " on " + auction);
						agent.setAllocation(auction, agent.getAllocation(auction) + 1);
      				}
	        	}**********/
	        }
	        Bid bid = new Bid(auction);
			bid.addBidPoint(alloc, prices[auction]);
			if (DEBUG) {
		  		log.finest("submitting bid with alloc="
			     	+ agent.getAllocation(auction)
			     	+ " own=" + agent.getOwn(auction));
			}
			agent.submitBid(bid);
	      }
	    } else if(auctionCategory == TACAgent.CAT_FLIGHT){
	        //float[] flightAskPrice_1 = new float[8];//flight price of Day 1
	    	//float[] flightAskPrice_2 = new float[8];//flight price of Day 2
	    	//if (agent.getGameTime() >= 1*60*1000 && agent.getGameTime() < 1*60*1000 + 15*1000){
	    	//	flightAskPrice_1[i] = quote.getAskPrice();
	          //      log.fine("flightAskPrice_1[" + i + "] =" + flightAskPrice_1[i] + " AskPrice= " + quote.getAskPrice());
	    	//	}
	    	//else if (agent.getGameTime() >= 2*60*1000 && agent.getGameTime() < 2*60*1000 + 15*1000){
	    	//	flightAskPrice_2[i] = quote.getAskPrice();
	          //      log.fine("flightAskPrice_2[" + i + "] =" + flightAskPrice_2[i] + " AskPrice= " + quote.getAskPrice());
	    		//}
	    	/*****************	
	        if(agent.getGameTime() >= 3*60*1000 && agent.getGameTime() < 7*60*1000){
	        	int alloc = agent.getAllocation(i) - agent.getOwn(i);
	            if(alloc > 0 && quote.getAskPrice() > myCurrentFlightBid[i]){
	            	Bid bid = new Bid(i);
	            	bid.addBidPoint(alloc, 450);
	            	agent.submitBid(bid);
	            	myCurrentFlightBid[i] = 450;
	            }
	            *********************/
	            if (agent.getGameTime() >= 1*60*1000 && agent.getGameTime() < 1*60*1000 + 10*1000){
	    		flightAskPrice_1[i] = quote.getAskPrice();
	                log.fine("flightAskPrice_1[" + i + "] = " + flightAskPrice_1[i] + " flightAskPrice_2[" + i + "] = " 
	                	+ flightAskPrice_2[i] + " AskPrice = " + quote.getAskPrice());
	    		}
	    		else if (agent.getGameTime() >= 2*60*1000 && agent.getGameTime() < 2*60*1000 + 10*1000){
	    		flightAskPrice_2[i] = quote.getAskPrice();
	                log.fine("flightAskPrice_1[" + i + "] = " + flightAskPrice_1[i] + " flightAskPrice_2[" + i + "] = " 
	                	+ flightAskPrice_2[i] + " AskPrice = " + quote.getAskPrice());
	            } else if(agent.getGameTime() >= 2*60*1000 + 10*1000 && agent.getGameTime() < 2*60*1000 + 40*1000){
	            	flightAskPrice_3[i] = quote.getAskPrice();
	            } else if(agent.getGameTime() >= 3*60*1000 && agent.getGameTime() < 7*60*1000){
	        		if (flightAskPrice_1[i] - flightAskPrice_2[i] <= 0 && flightAskPrice_2[i] - flightAskPrice_3[i] <= 0){
	        		//flightAskPrice_1[i] = quote.getAskPrice();
	        		//flightAskPrice_2[i] = quote.getAskPrice();

	        			log.fine("<=0, flightAskPrice_1[" + i + "]  = " + flightAskPrice_1[i] + "flightAskPrice_2[" + i + "]  = " + flightAskPrice_2[i] + " Ask price = "
	        					+ quote.getAskPrice());
	        			int alloc = agent.getAllocation(i) - agent.getOwn(i);
	        			Bid bid = new Bid(i);
	        			myCurrentFlightBid[i] = flightAskPrice_3[i] + 40f;
	        			bid.addBidPoint(alloc, myCurrentFlightBid[i]);
	        			agent.submitBid(bid);
	        			} 
	        		else if ((flightAskPrice_1[i] - flightAskPrice_2[i])*(flightAskPrice_2[i] - flightAskPrice_3[i]) < 0){
	        			if (agent.getGameTime() >= 4*60*1000){
	        				int alloc = agent.getAllocation(i) - agent.getOwn(i);
	        				Bid bid = new Bid(i);
	        				myCurrentFlightBid[i] = flightAskPrice_3[i] + 30f;
	        				bid.addBidPoint(alloc, myCurrentFlightBid[i]);
	    					agent.submitBid(bid);
	    					}
	    				}
	        		else {
	        			log.fine(">0, flightAskPrice_1[" + i + "]  = " + flightAskPrice_1[i] + "flightAskPrice_2[" + i + "]  = " + flightAskPrice_2[i] + " Ask price = "
	        					+ quote.getAskPrice());
	        			if (agent.getGameTime() >= 5*60*1000){
	        				int alloc = agent.getAllocation(i) - agent.getOwn(i);
	        				Bid bid = new Bid(i);
	        				myCurrentFlightBid[i] = flightAskPrice_3[i] + 20f;
	        				bid.addBidPoint(alloc, myCurrentFlightBid[i]);
	        				agent.submitBid(bid);
	        				}
	        			}
	        		}
	        		
	        	 else if(agent.getGameTime() >= 7*60*1000){
	        		int alloc = agent.getAllocation(i) - agent.getOwn(i);
	        		if(alloc > 0 && quote.getAskPrice() > myCurrentFlightBid[i]){
	        			log.fine("myCurrentFlightBid[" + i + "]  = " + myCurrentFlightBid[i] + "Ask price = "
	        				+ quote.getAskPrice());
	        			Bid bid = new Bid(i);
	        			bid.addBidPoint(alloc, 800.0f);
	        			agent.submitBid(bid);
	        			myCurrentFlightBid[i] = 800.0f;
	        		}
	        	}
	    }else if (auctionCategory == TACAgent.CAT_ENTERTAINMENT) {
    	int alloc = agent.getAllocation(auction) - agent.getOwn(auction);
        if (alloc != 0) {
                     Bid bid = new Bid(auction);
                   if (alloc < 0 ){//sell tickets   
                      float sellPrice = 80 - (agent.getGameTime()*20f) /540000;
                         bid.addBidPoint(alloc, sellPrice);
                         agent.submitBid(bid);      

                   }else if(alloc>0){//buy tickets        
                        if(agent.getGameTime()<7*60*1000){                                                                                 
                                     prices[auction]= 35f+(agent.getGameTime()*45f)/420000;
                                     myCurrentEnBid[i]=prices[auction];                
                         }
                        else if(agent.getGameTime()>7*60*1000){
                        	prices[auction]=32f+(agent.getGameTime()*40f)/540000;
                        	 myCurrentEnBid[i]=prices[auction];
                        }
                         bid.addBidPoint(alloc, prices[auction]);
                         if (DEBUG) {
                                 log.finest("submitting bid with alloc=" + agent.getAllocation(auction)
                                 + " own=" + agent.getOwn(auction));
                                   }
                         agent.submitBid(bid);
                        
                         

                    }
                   
        
         }
 }
}

  public void quoteUpdated(int auctionCategory) {
    log.fine("All quotes for "
	     + agent.auctionCategoryToString(auctionCategory)
	     + " has been updated");
  }

  public void bidUpdated(Bid bid) {
    log.fine("Bid Updated: id=" + bid.getID() + " auction="
	     + bid.getAuction() + " state="
	     + bid.getProcessingStateAsString());
    log.fine("       Hash: " + bid.getBidHash());
  }

  public void bidRejected(Bid bid) {
    log.warning("Bid Rejected: " + bid.getID());
    log.warning("      Reason: " + bid.getRejectReason()
		+ " (" + bid.getRejectReasonAsString() + ')');
  }

  public void bidError(Bid bid, int status) {
    log.warning("Bid Error in auction " + bid.getAuction() + ": " + status
		+ " (" + agent.commandStatusToString(status) + ')');
  }

 public void gameStarted() {
	    log.fine("Game " + agent.getGameID() + " started!");
	    
	    
	    for(int i=0;i<28;i++){
	        myCurrentEnBid[i]=0;
	        myCurrentFlightBid[i]=0;
	    }
	    
	    calculateAllocation();
	    sendBids();

	  }

 public void gameStopped() {
	    log.fine("Game Stopped!");
	    

	  }

  public void auctionClosed(int auction) {
    log.fine("*** Auction " + auction + " closed!");
  }

  private void sendBids() {
    for (int i = 0, n = agent.getAuctionNo(); i < n; i++) {
      int alloc = agent.getAllocation(i) - agent.getOwn(i);
      float price = -1f;
      switch (agent.getAuctionCategory(i)) {
      case TACAgent.CAT_FLIGHT:
	if (alloc > 0) {
	  price = 230;
	}
	break;
      case TACAgent.CAT_HOTEL:
	if (alloc > 0) {
		if(agent.getAuctionType(i)==TACAgent.TYPE_CHEAP_HOTEL){
	  price = 300.0f;
	  prices[i] = 300.0f;
		}else{
			 price = 400.0f;
			  prices[i] = 400.0f;
		}
	}
	break;
      case TACAgent.CAT_ENTERTAINMENT:
    	  if (alloc < 0) {//need to sell tickets
              price = 90.0f;
              prices[i] = 90.0f;
       } else if (alloc > 0) {//need to buy tickets
            if(AveOfClientPrefValue<90){
                  price = AveOfClientPrefValue;
                  prices[i] = AveOfClientPrefValue;
                myCurrentEnBid[i]=AveOfClientPrefValue;
            }else{
                price=80.0f;
                prices[i]=80.0f;
                myCurrentEnBid[i]=80.0f;
            }
       }
       break;
  default:
       break;
}
if (price > 0) {
        Bid bid = new Bid(i);
        bid.addBidPoint(alloc, price);
        if (DEBUG) {
           log.finest("submitting bid with alloc=" + agent.getAllocation(i) + " own=" + agent.getOwn(i));
        }
        agent.submitBid(bid);
}
}
}
  private void calculateAllocation() {
	  
  	  int hotelPre=0;
      int hotelPreAve=0;
                     
      for(int j=0;j<8;j++){
         int n=agent.getClientPreference(j, TACAgent.HOTEL_VALUE);
         hotelPre=hotelPre+n;
      }
      hotelPreAve=hotelPre/8 - 10;
      //if(hotelPreAve>100){
      //hotelPreAve=100;
      //}
    for (int i = 0; i < 8; i++) {
      int inFlight = agent.getClientPreference(i, TACAgent.ARRIVAL);
      int outFlight = agent.getClientPreference(i, TACAgent.DEPARTURE);
      int hotel = agent.getClientPreference(i, TACAgent.HOTEL_VALUE);
      int type;

      // Get the flight preferences auction and remember that we are
      // going to buy tickets for these days. (inflight=1, outflight=0)
      int auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT,
					TACAgent.TYPE_INFLIGHT, inFlight);
      agent.setAllocation(auction, agent.getAllocation(auction) + 1);
      auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT,
				    TACAgent.TYPE_OUTFLIGHT, outFlight);
      agent.setAllocation(auction, agent.getAllocation(auction) + 1);

      // if the hotel value is greater than 70 we will select the
      // expensive hotel (type = 1)
      
      if (hotel > hotelPreAve) {
	type = TACAgent.TYPE_GOOD_HOTEL;
      } else {
	type = TACAgent.TYPE_CHEAP_HOTEL;
      }
      // allocate a hotel night for each day that the agent stays
      for (int d = inFlight; d < outFlight; d++) {
	auction = agent.getAuctionFor(TACAgent.CAT_HOTEL, type, d);
	log.finer("Adding hotel for day: " + d + " on " + auction);
	agent.setAllocation(auction, agent.getAllocation(auction) + 1);
      }

    //****************************************************
//    range entertainment
//****************************************************
     int EntType;
     int d = outFlight - inFlight;
     if(d>=3){
       d=3;
     }
   for(int ord=1;ord<=d;ord++){
	   EntType = nextEntType(i, ord);
       auction = agent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT,
    		   EntType, inFlight+ord-1);
       agent.setAllocation(auction, agent.getAllocation(auction) + 1);
     }

   }
   
 }
  private int bestEntDay(int inFlight, int outFlight, int type) {
	    for (int i = inFlight; i < outFlight; i++) {
	      int auction = agent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT,
						type, i);
	      if (agent.getAllocation(auction) < agent.getOwn(auction)) {
		       return auction;
	      }
	    }
	    // If no left, just take the first...
	    return agent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT,
				       type, inFlight);
	  }
  
  private int nextEntType(int client, int order) {
      int e1 = agent.getClientPreference(client, TACAgent.E1);
      int e2 = agent.getClientPreference(client, TACAgent.E2);
      int e3 = agent.getClientPreference(client, TACAgent.E3);
      switch(order){
      case 1:
        if(e1 > e2 && e1 > e3)
          return TACAgent.TYPE_ALLIGATOR_WRESTLING;
        else if (e2 > e1 && e2 > e3)
          return TACAgent.TYPE_AMUSEMENT;
        else if (e3 > e1 && e3 > e2)
          return TACAgent.TYPE_MUSEUM;
      case 2:
        if((e1 > e2 && e1 < e3)||(e1 < e2 &&e1 > e3))
          return TACAgent.TYPE_ALLIGATOR_WRESTLING;
        else if ((e2 > e1 && e2 < e3)||(e2 < e1 && e2 > e3))
          return TACAgent.TYPE_AMUSEMENT;
        else if ((e3 > e1 && e3 < e2)||(e3 < e1 && e3 > e2))
          return TACAgent.TYPE_MUSEUM;
      case 3:
        if(e1 < e2 && e1 < e3)
          return TACAgent.TYPE_ALLIGATOR_WRESTLING;
        else if (e2 < e1 && e2 < e3)
          return TACAgent.TYPE_AMUSEMENT;
        else if (e3 < e1 && e3 < e2)
          return TACAgent.TYPE_MUSEUM;
      default:
        return -1;
      }
      
  }


  // -------------------------------------------------------------------
  // Only for backward compability
  // -------------------------------------------------------------------

  public static void main (String[] args) {
    TACAgent.main(args);
  }

} // TripodAgent
