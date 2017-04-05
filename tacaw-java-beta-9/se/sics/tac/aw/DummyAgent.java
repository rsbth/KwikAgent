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
import com.mongodb.*;
import java.util.Arrays;

public class DummyAgent extends AgentImpl {
  private double avg_ent_Client_pref = 0;
    static MongoCredential credential = MongoCredential.createCredential("scott","tacLog","tiger".toCharArray());
    static MongoClient client = new MongoClient(new ServerAddress("127.0.0.1",27017), Arrays.asList(credential));
    static DB db = client.getDB("tacLog");
    static DBCollection collection  = db.getCollection("infoj");

  private static final Logger log =
    Logger.getLogger(DummyAgent.class.getName());

  private static final boolean DEBUG = false;

  private float[] prices;

  protected void init(ArgEnumerator args) {
      prices = new float[agent.getAuctionNo()];
  }

  public void quoteUpdated(Quote quote) {
      //BasicDBObject insertData =new BasicDBObject();
      int auction = quote.getAuction();
      //insertData.put("auction",auction);
    int auctionCategory = agent.getAuctionCategory(auction);
       /*insertData.put("auction_categ",auctionCategory);
       insertData.put("Categ_hotel_is",TACAgent.CAT_HOTEL);
       insertData.put("Categ_hotel_is",TACAgent.CAT_FLIGHT);
       insertData.put("Categ_hotel_is",TACAgent.CAT_ENTERTAINMENT);
    */
       if (auctionCategory == TACAgent.CAT_HOTEL) {
      int alloc = agent.getAllocation(auction);
     /* insertData.put("Allocation_Hotel",alloc);
      insertData.put("Hotel_bef_all_hasqw_get_quote_getAskPrice",quote.getAskPrice());
      insertData.put("Hotel_bef_all_hasqw_get_quote_hasHQW_getBid_auction",quote.hasHQW(agent.getBid(auction)));
         insertData.put("Hotel_bef_all_hasqw_get_quote_getHQW",quote.getHQW());
         insertData.put("Hotel_bef_prices_auction",prices[auction]);
      insertData.put("Hotel_bef_all_GetOwn_auction", agent.getOwn(auction));
       insertData.put("bef_all_Agent_getTime",agent.getGameTime());
    */
      if (alloc > 0 && quote.hasHQW(agent.getBid(auction)) &&
	  quote.getHQW() < alloc) {

	  /*   insertData.put("Hotel_eft_all_hasqw_get_getBid_auction",agent.getBid(auction));
	     insertData.put("Hotel_eft_all_hasqw_get_quote_hasHQW_getBid_auction",quote.hasHQW(agent.getBid(auction)));
         insertData.put("Hotel_eft_all_hasqw_get_quote_getHQW",quote.getHQW());
      */    Bid bid = new Bid(auction);
	// Can not own anything in hotel auctions...
	prices[auction] = quote.getAskPrice() + 50;
	/*insertData.put("Hotel_eft_getAskPrice",quote.getAskPrice());
	insertData.put("Hotel eft_prices_auction",prices[auction]);
	insertData.put("Hotel eft_all_GetOwn_auction", agent.getOwn(auction));
	insertData.put("eft_all_Agent_getTime",agent.getGameTime()); */
	bid.addBidPoint(alloc, prices[auction]);
	if (DEBUG) {
	  log.finest("submitting bid with alloc="
		     + agent.getAllocation(auction)
		     + " own=" + agent.getOwn(auction));
	}
	agent.submitBid(bid);
      }
    } else if (auctionCategory == TACAgent.CAT_ENTERTAINMENT) {
      int alloc = agent.getAllocation(auction) - agent.getOwn(auction);
    /*  insertData.put("Ent_alloc", alloc);
      insertData.put("Ent_agent_get_alloc_auc",agent.getAllocation(auction));
      insertData.put("Ent_agent_get_own_auc",agent.getOwn(auction));
      insertData.put("Auction",auction);*/
      if (alloc != 0) {
	Bid bid = new Bid(auction);
	if (alloc < 0){
	  prices[auction] = 200f - (agent.getGameTime() * 120f) / 720000;
    //insertData.put("Ent_all_lt0_prices_AUCT",prices[auction]);}
	else{
	  prices[auction] = 50f + (agent.getGameTime() * 100f) / 720000;
    //insertData.put("Ent_all_ifntlt0_prices_AUCT",prices[auction]);}
	bid.addBidPoint(alloc, prices[auction]);
	if (DEBUG) {
	  log.finest("submitting bid with alloc="
		     + agent.getAllocation(auction)
		     + " own=" + agent.getOwn(auction));
	}
	agent.submitBid(bid);
      }
    }else if(auctionCategory == TACAgent.CAT_FLIGHT){

    }
    //collection.insert(insertData);
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

    calculateAllocation();
    sendBids();
  }

  public void gameStopped() {
    log.fine("Game Stopped!");
  }

  public void auctionClosed(int auction) {
    /*BasicDBObject gameStats = new BasicDBObject();
    gameStats.put("message_type","auctionClosed");
    gameStats.put("auction", auction);
    gameStats.put("Elapsed_Gametime", agent.getGameTime());
    gameStats.put("auction_no", agent.getAuctionNo());*/
    String pice_Str = "";
    for(int i=0;i<prices.length;i++){
      pice_Str += i+"="+prices[i] + " ";
    }
   // gameStats.put("prices", pice_Str);
    log.fine("*** Auction " + auction + " closed!");
     //collection.insert(gameStats);
  }

  private void sendBids() {
    BasicDBObject bidsenderdb = new BasicDBObject();
    for (int i = 0, n = agent.getAuctionNo(); i < n; i++) {
      //bidsenderdb.put("message_type","fromSendBids");
      double time_left_for_completion = agent.getGameTimeLeft();
      //bidsenderdb.put("time_left_for_completion",time_left_for_completion);
      int alloc = agent.getAllocation(i) - agent.getOwn(i);
      /*bidsenderdb.put("agent_getallocationi",agent.getAllocation(i));
      bidsenderdb.put("agent_getOwni",agent.getOwn(i));
      bidsenderdb.put("alloc",alloc);
      collection.insert(bidsenderdb);*/

      //BasicDBObject priceTracker = new BasicDBObject();
      float price = -1f;
      switch (agent.getAuctionCategory(i)) {
      case TACAgent.CAT_FLIGHT:
	if (alloc > 0) {
	  price = 1000;
    //priceTracker.put("message_from","Flight");
    //priceTracker.put("time_left_for_completion_f", agent.getGameTimeLeft());
	}
	break;
      case TACAgent.CAT_HOTEL:
	if (alloc > 0) {
	  price = 200;
	  prices[i] = 200f;
 //	priceTracker.put("message_from","Hotel");
  //  priceTracker.put("time_left_for_completion_h", agent.getGameTimeLeft());
  }
	break;
      case TACAgent.CAT_ENTERTAINMENT:
	if (alloc < 0) {
	  price = 120;
	  prices[i] = 120f;
 //   priceTracker.put("time_left_for_completion_e", agent.getGameTimeLeft());
	} else if (alloc > 0) {
	  price = 50;
	  prices[i] = 50f;
  // priceTracker.put("time_left_for_completion_e", agent.getGameTimeLeft());
	}
	break;
      default:
	break;
      }
  //    collection.insert(priceTracker);
      if (price > 0) {
	Bid bid = new Bid(i);
	bid.addBidPoint(alloc, price);
  /*BasicDBObject bidPointRecord = new BasicDBObject();
  bidPointRecord.put("message_from", "BidPointAdd");
  bidPointRecord.put("alloc", alloc);
  bidPointRecord.put("price",price);
  bidPointRecord.put("allocation", agent.getAllocation(i));
  bidPointRecord.put("own",agent.getOwn(i));
  collection.insert(bidPointRecord); */
	if (DEBUG) {
	  log.finest("submitting bid with alloc=" + agent.getAllocation(i)
		     + " own=" + agent.getOwn(i));
	}
	agent.submitBid(bid);
      }
    }
  }

  private void calculateAllocation() {
    for (int i = 0; i < 8; i++) {
      //BasicDBObject allocCalc = new BasicDBObject();
      int inFlight = agent.getClientPreference(i, TACAgent.ARRIVAL);
      //allocCalc.put("InFlightPref_Of_"+i, inFlight );
      int outFlight = agent.getClientPreference(i, TACAgent.DEPARTURE);
      //allocCalc.put("outFlightPref_of_"+ i, outFlight);
      int hotel = agent.getClientPreference(i, TACAgent.HOTEL_VALUE);
      //allocCalc.put("hotelPrefValue_of_"+i,hotel);
      int type;

      // Get the flight preferences auction and remember that we are
      // going to buy tickets for these days. (inflight=1, outflight=0)
      int auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT,
					TACAgent.TYPE_INFLIGHT, inFlight);
        // allocCalc.put("inFlight_auction", auction);
      agent.setAllocation(auction, agent.getAllocation(auction) + 1);

      auction = agent.getAuctionFor(TACAgent.CAT_FLIGHT,
				    TACAgent.TYPE_OUTFLIGHT, outFlight);
               //allocCalc.put("OutFlight_auction", auction);

      agent.setAllocation(auction, agent.getAllocation(auction) + 1);

      // if the hotel value is greater than 70 we will select the
      // expensive hotel (type = 1)
      if (hotel > 70) {
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

      int eType = -1;
      while((eType = nextEntType(i, eType)) > 0) {
	auction = bestEntDay(inFlight, outFlight, eType);
	log.finer("Adding entertainment " + eType + " on " + auction);
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

  private int nextEntType(int client, int lastType) {
    //BasicDBObject clientPrefEnt = new BasicDBObject();
    int e1 = agent.getClientPreference(client, TACAgent.E1);
    //clientPrefEnt.put("e1", e1);
    int e2 = agent.getClientPreference(client, TACAgent.E2);
    //clientPrefEnt.put("e2",e2);
    int e3 = agent.getClientPreference(client, TACAgent.E3);
    //clientPrefEnt.put("e3",e3);
    //collection.insert(clientPrefEnt);
    //Store in DB 

    // At least buy what each agent wants the most!!!
    if ((e1 > e2) && (e1 > e3) && lastType == -1)
      return TACAgent.TYPE_ALLIGATOR_WRESTLING;
    else-if((e1<e2)&&(e1>e3))
      return TACAgent.TYPE_MUSEUM;
    else-if((e1<e2)&&(e1<e3))
      return TACAgent.TYPE_ALLIGATOR_WRESTLING; 
    if ((e2 > e1) && (e2 > e3) && lastType == -1)
      return TACAgent.TYPE_AMUSEMENT;
    else-if((e2<e1) && (e2>e3))
      return TACAgent.
    else-if(())
    if ((e3 > e1) && (e3 > e2) && lastType == -1)
      return TACAgent.TYPE_MUSEUM;
    return -1;
  }



  // -------------------------------------------------------------------
  // Only for backward compability
  // -------------------------------------------------------------------

  public static void main (String[] args) {
    TACAgent.main(args);
  }

} // DummyAgent
