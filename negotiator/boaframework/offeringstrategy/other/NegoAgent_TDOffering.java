package negotiator.boaframework.offeringstrategy.other;

import java.util.HashMap;
import java.lang.Math;
import java.util.List;
import java.util.ArrayList;

import misc.Range;
import negotiator.BidHistory;
import negotiator.bidding.BidDetails;
import negotiator.boaframework.NegotiationSession;
import negotiator.boaframework.OMStrategy;
import negotiator.boaframework.OfferingStrategy;
import negotiator.boaframework.OpponentModel;
import negotiator.boaframework.SortedOutcomeSpace;

/**
 * Time-dependent Offering Strategy
 * 
 * @author Kirill Tumanov
 * @version 30.11.2013
 */
public class NegoAgent_TDOffering extends OfferingStrategy
{
	/** k \in [0, 1]. For k = 0 the agent starts with a bid of maximum utility */
	private double k;
	/** Maximum target utility */
	private double Pmax;
	/** Minimum target utility */
	private double Pmin;
	/** Concession factor */
	private double e;
	/** Sum calculated to find a Nash */
	private double nashsum;
	/** Sum of opponent's bids */
	private double bidsum;
	/** Number of unique opponent bids */
	private List<Double> uniquebids = new ArrayList<Double>();
	/** Outcome space */
	SortedOutcomeSpace outcomespace;
	
	/**
	 * Empty constructor used for reflexion. Note this constructor assumes that init is called next.
	 */
	public NegoAgent_TDOffering(){}
	
	public NegoAgent_TDOffering(NegotiationSession negoSession, OpponentModel model, OMStrategy oms, double e, double k, double max, double min)
	{
		this.e = e;
		this.k = k;
		this.Pmax = max;
		this.Pmin = min;
		this.negotiationSession = negoSession;
		outcomespace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
		negotiationSession.setOutcomeSpace(outcomespace);
		this.opponentModel = model;
		this.omStrategy = oms;	
	}
	
	/**
	 * Method which initializes the agent by setting all parameters.
	 * The parameter "e" is the only parameter which is required.
	 */
	public void init(NegotiationSession negoSession, OpponentModel model, OMStrategy oms, HashMap<String, Double> parameters) throws Exception
	{
		if (parameters.get("e") != null)
		{
			this.negotiationSession = negoSession;
			
			outcomespace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
			negotiationSession.setOutcomeSpace(outcomespace);
			
			this.e = parameters.get("e");
			
			if (parameters.get("k") != null)
				this.k = parameters.get("k");
			else
				this.k = 0;
			
			if (parameters.get("min") != null)
				this.Pmin = parameters.get("min");
			else
				this.Pmin = negoSession.getOutcomeSpace().getMinBidPossible().getMyUndiscountedUtil();
                        
			if (parameters.get("max") != null) 
				Pmax= parameters.get("max");
			else 
				Pmax = negoSession.getMaxBidinDomain().getMyUndiscountedUtil();
			
			this.opponentModel = model;
			this.omStrategy = oms;
		} 
		else
			throw new Exception("Constant \"e\" for the concession speed was not set.");		
	}

	@Override
	public BidDetails determineOpeningBid() { return determineNextBid(); }

	/**
	 * Simple offering strategy which retrieves the target utility and looks for the nearest bid if no opponent model is specified.
	 * If an opponent model is specified, then the agent return a bid according to the opponent model strategy.
	 */
	@Override
	public BidDetails determineNextBid()
	{
		double time = negotiationSession.getTime();
		double utilityGoal;
		utilityGoal = p(time);

		// if there is no opponent model available
//		if (opponentModel instanceof NoModel) 
//			nextBid = negotiationSession.getOutcomeSpace().getBidNearUtility(utilityGoal);
//		else
			nextBid = omStrategy.getBid(outcomespace, utilityGoal);
		
		return nextBid;
	}
	
	/**
	 * Method which returns true if the Nash is reached
	 * @throws Exception 
	 */
	public boolean isNash()
	{
		BidHistory bH = negotiationSession.getOpponentBidHistory();
		
		if (bH.getLastBidDetails().getBid() != null)
		{
			double temp = bH.getLastBidDetails().getMyUndiscountedUtil() + opponentModel.getBidEvaluation(bH.getLastBidDetails().getBid());
			
			if (temp < nashsum && opponentModel.getBidEvaluation(bH.getLastBidDetails().getBid()) < bH.getBestBidDetails().getMyUndiscountedUtil())
				return true;
			else
				nashsum = temp;
		}
		
		return false;
	}
	
	/**
	 * Method which calculates a number of unique opponent bids 
	 */
	public void countUniqueBids()
	{
		int count = 0;
		
		if (negotiationSession.getOpponentBidHistory().getHistory().size() == 1)
		{
			uniquebids.add(negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil());
			bidsum += negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil();
		}
		else if (negotiationSession.getOpponentBidHistory().getHistory().size() != 0)
		{
            for (int j = 0; j < uniquebids.size(); j ++)
                if (uniquebids.get(j) != negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil())
                        count ++;
                        
			if (count == uniquebids.size())
			{
                uniquebids.add(negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil());
                bidsum += negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil();
            }
		}
	}       
	
	/**
	 * Time-dependent function for bid-offering
	 */
	public double f(double t)
	{
		countUniqueBids();
		
		if (e == 0)
			return k;
		
		double ft = 0;
		int num = negotiationSession.getIssues().size();
		double df = 0.1; // Division factor
				
		if (uniquebids.size() > 1 && !isNash()) // Function which bids more active when opponent is tough
			ft = (df * Math.sin(-num * Math.exp(t * df)) + (1 - df)) * Math.log(t * df + 1) * bidsum/uniquebids.size() *  t;	
		else if (uniquebids.size() > 1) // Less aggressive offering
			ft = (2 * df * Math.sin(-num * Math.exp(t * 2 * df)) + (1 - 2 * df)) * Math.log(t * 2 * df + 1) * bidsum/uniquebids.size() *  t;
		else
			ft = (df * Math.sin(-num * Math.exp(t * df)) + (1 - df)) * Math.log(t * df + 1);
                Range r = new Range(0D, 1D);
                
		if (outcomespace.getBidsinRange(r).size() < 10)
			if (negotiationSession.getTime() > 0.8)
				ft += negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil()/(1 - negotiationSession.getTime());
		
		return ft;
	}

	/**
	 * Adjusts a time-dependent offering function the acceptable range according to the domain (from Pmax to Pmin).
	 * @param t - time
	 * @return double
	 */
	public double p(double t) {	return Pmin + (Pmax - Pmin) * (1 - f(t)); }

	public NegotiationSession getNegotiationSession() {	return negotiationSession; }
	
	public int getUniqueBidsCount() { countUniqueBids(); return uniquebids.size(); }
}