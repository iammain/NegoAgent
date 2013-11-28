package negotiator.boaframework.omstrategy;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import negotiator.bidding.BidDetails;
import negotiator.boaframework.NegotiationSession;
import negotiator.boaframework.OMStrategy;
import negotiator.boaframework.OpponentModel;


/**
 * This class uses an opponent model to determine the next bid for the opponent, while taking
 * the opponent's preferences into account. The opponent model is used to select the best bid.
 *
 * 
 */
public class BidStrategy extends OMStrategy {
 
	/**  when to stop updating the opponentmodel. Note that this value
	 * 	 is not exactly one as a match sometimes lasts slightly longer. */
	double updateThreshold = 1.1;
	private ArrayDeque<Double> lastTen; // Last ten percent of opponent's bids
	Random r = new Random();
	
	/**
	 * Empty constructor for the BOA framework.
	 */	
	public BidStrategy() {}
	  
	public BidStrategy(NegotiationSession negotiationSession, OpponentModel model)
	{
		initializeAgent(negotiationSession, model);
	}
	  
	public void init(NegotiationSession nSession, OpponentModel oppModel, HashMap<String, Double> parameters) throws Exception
	{
		initializeAgent(nSession, oppModel);
	}
	  
	private void initializeAgent(NegotiationSession negotiationSession, OpponentModel model)
	{
		try
		{
			super.init(negotiationSession, model);
			this.lastTen = new ArrayDeque<Double>(negotiationSession.getOpponentBidHistory().size()/10 + 1);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
  	}
			
	private static final class BidComparator implements Comparator<BidDetails>
	{
		private final OpponentModel model;
	  
		protected BidComparator(OpponentModel paramOpponentModel)
		{
			this.model = paramOpponentModel;
		}
	  
		public int compare(BidDetails paramBidDetails1, BidDetails paramBidDetails2)
		{
			double d1 = this.model.getBidEvaluation(paramBidDetails1.getBid());
			double d2 = this.model.getBidEvaluation(paramBidDetails2.getBid());
			
			if (d1 < d2) return 1;	    
			else if (d1 > d2) return -1;				
			else return 0;
		}
	}
	
	public BidDetails getBid(List<BidDetails> allBids)
	{
		Collections.sort(allBids, new BidComparator(this.model));
	    BidDetails opponentBestBid = (BidDetails)allBids.get(0);
	    boolean allEqual = true;
	    
	    for (Iterator<Double> localIterator = this.lastTen.iterator(); localIterator.hasNext();)
	    {
	    	double bid = ((Double)localIterator.next()).doubleValue();
	      
	    	if (bid != opponentBestBid.getMyUndiscountedUtil()) 
	    		allEqual = false;
	    }	    
	    
	    if (allEqual)
	    	if (allBids.size() > 1) 
	    		opponentBestBid = (BidDetails)allBids.get(r.nextInt(allBids.size()));
	    
	    this.lastTen.addLast(Double.valueOf(opponentBestBid.getMyUndiscountedUtil()));
	    
	    if (this.lastTen.size() > negotiationSession.getOpponentBidHistory().size()/10)
	    	this.lastTen.removeFirst();
	    
	    return opponentBestBid;
	  }

	/**
	 * The opponent model may be updated, unless the time is higher
	 * than a given constant.
	 * 
	 * @return true if model may be updated.
	 */
	@Override
	public boolean canUpdateOM() { return negotiationSession.getTime() < updateThreshold; }
}