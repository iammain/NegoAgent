package negotiator.boaframework.offeringstrategy.other;

import java.util.HashMap;
import java.lang.Math;
import java.util.List;
import java.util.ArrayList;

import negotiator.bidding.BidDetails;
import negotiator.boaframework.NegotiationSession;
import negotiator.boaframework.OMStrategy;
import negotiator.boaframework.OfferingStrategy;
import negotiator.boaframework.OpponentModel;
import negotiator.boaframework.SortedOutcomeSpace;
import negotiator.boaframework.opponentmodel.NoModel;

/**
 * This is an abstract class used to implement a TimeDependentAgent Strategy.
 * The default strategy was extended to enable the usage of opponent models.
 * 
 * @author Kirill Tumanov
 */
public class NegoAgent_TDOffering extends OfferingStrategy {

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
	public List<Double> uniquebids = new ArrayList<Double>();
	/** Outcome space */
	SortedOutcomeSpace outcomespace;
	
	/**
	 * Empty constructor used for reflexion. Note this constructor assumes that init is called next.
	 */
	public NegoAgent_TDOffering(){}
	
	public NegoAgent_TDOffering(NegotiationSession negoSession, OpponentModel model, OMStrategy oms, double e, double k, double max, double min){
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
	public void init(NegotiationSession negoSession, OpponentModel model, OMStrategy oms, HashMap<String, Double> parameters) throws Exception {
		if (parameters.get("e") != null) {
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
				this.Pmin = negoSession.getMinBidinDomain().getMyUndiscountedUtil();
		
			if (parameters.get("max") != null) {
				Pmax= parameters.get("max");
			} else {
				BidDetails maxBid = negoSession.getMaxBidinDomain();
				Pmax = maxBid.getMyUndiscountedUtil();
			}
			
			this.opponentModel = model;
			this.omStrategy = oms;
		} else {
			throw new Exception("Constant \"e\" for the concession speed was not set.");
		}
	}

	@Override
	public BidDetails determineOpeningBid() {
		return determineNextBid();
	}

	/**
	 * Simple offering strategy which retrieves the target utility and looks for the nearest bid if no opponent model is specified.
	 * If an opponent model is specified, then the agent return a bid according to the opponent model strategy.
	 */
	@Override
	public BidDetails determineNextBid() {
		double time = negotiationSession.getTime();
		double utilityGoal;
		utilityGoal = p(time);
		
//		System.out.println("[e=" + e + ", Pmin = " + BilateralAgent.round2(Pmin) + "] t = " + BilateralAgent.round2(time) + ". Aiming for " + utilityGoal);
		
		// if there is no opponent model available
		if (opponentModel instanceof NoModel) {
			nextBid = negotiationSession.getOutcomeSpace().getBidNearUtility(utilityGoal);
		} else {
			nextBid = omStrategy.getBid(outcomespace, utilityGoal);
		}
		return nextBid;
	}
	
	/**
	 * Method which returns true if the Nash is reached
	 */
	public boolean isNash()
	{
		double temp = negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil() 
				+ opponentModel.getBidEvaluation(negotiationSession.getOpponentBidHistory().getLastBid());
		
		if (temp < nashsum && opponentModel.getBidEvaluation(negotiationSession.getOpponentBidHistory().getLastBid()) < negotiationSession.getOpponentBidHistory().getBestBidDetails().getMyUndiscountedUtil()
				)
			return true;
		else
			nashsum = temp;
		
		return false;
	}
	
	/**
	 * Method which calculates a number of unique opponent bids 
	 */
	public void countUniqueBids()
	{
//		System.out.println("Size: " + negotiationSession.getOpponentBidHistory().getHistory().size());
		
		int count = 0;
		if (negotiationSession.getOpponentBidHistory().getHistory().size() == 1)
		{
			uniquebids.add(negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil());
			bidsum += negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil();
		}
		else if (negotiationSession.getOpponentBidHistory().getHistory().size() != 0)
		{
                        for (int j = 0; j < uniquebids.size(); j ++){
                            if (uniquebids.get(j) != negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil())
                                    count ++;
                        }
                        
			if (count == uniquebids.size())
			{
                            uniquebids.add(negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil());
//				System.out.println();
//				System.out.println("My utility: " + negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil());
//				System.out.println("Opponent utility: " + opponentModel.getBidEvaluation(negotiationSession.getOpponentBidHistory().getLastBid()));

                            bidsum += negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil();
                        }
		}
	}
        public int countUniqueBids(boolean adsf) throws InterruptedException
	{
            int count = 0;
            if (negotiationSession.getOpponentBidHistory().getHistory().size() == 1)
            {
                uniquebids.add(negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil());
                bidsum += negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil();
            }
            else if (negotiationSession.getOpponentBidHistory().getHistory().size() != 0)
            {
                if(uniquebids.size()==0){
                        uniquebids.add(negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil());
                        count++;
                }
                else{
                    for (int j = 0; j < uniquebids.size(); j ++){
                            if (uniquebids.get(j) != negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil()){
                                    count++;
                            }
                    }
                }
            }
            return count;
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
		int num = negotiationSession.getDomain().getIssues().size();
		double df = 0.1; // Division factor
				
		if (uniquebids.size() > 1 && !isNash())
		{
//			System.out.println("Average opponent's bid utility: " + bidsum/uniquebids.size());
			
			// Function which bids more active when opponent is tough
//			ft = (0.1*Math.sin(-1000 * Math.exp(t/10)) + 0.9) * Math.log(t/10 + 1) * (1 - t)/uniquebids.size() * (1 + uniquebids.get(uniquebids.size() - 1));
			ft = (df * Math.sin(-num * Math.exp(t * df)) + (1 - df)) * Math.log(t * df + 1) * bidsum/uniquebids.size() *  t;			
		}
		else if (uniquebids.size() > 1) // Should be something less agressive
			ft = (2 * df *Math.sin(-num * Math.exp(t * 2 * df)) + (1 - 2 * df)) * Math.log(t * 2 * df + 1) * bidsum/uniquebids.size() *  t;
		else
			ft = (df * Math.sin(-1000 * Math.exp(t * df)) + (1 - df)) * Math.log(t * df + 1);
		
		if (negotiationSession.getDomain().getNumberOfPossibleBids() < 10)
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
}