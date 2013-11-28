package negotiator.boaframework.acceptanceconditions.other;

import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import negotiator.boaframework.AcceptanceStrategy;
import negotiator.boaframework.Actions;
import negotiator.boaframework.NegotiationSession;
import negotiator.boaframework.OfferingStrategy;
import negotiator.boaframework.OpponentModel;
import negotiator.boaframework.offeringstrategy.other.NegoAgent_TDOffering;
import negotiator.boaframework.omstrategy.BidStrategy;
import negotiator.utility.UtilitySpace;

/**
 * This Acceptance Condition will accept an opponent bid if the utility is higher than the 
 * bid the agent is ready to present
 * Decoupling Negotiating Agents to Explore the Space of Negotiation Strategies
 * T. Baarslag, K. Hindriks, M. Hendrikx, A. Dirkzwager, C.M. Jonker
 * @author Alex Dirkzwager, Mark Hendrikx
 * @version 18/12/11
 */
public class AStrategy extends AcceptanceStrategy {
    
    private double ACCEPTANCE_THRESHOLD = 1;
    private double lamda;
	private double a;
	private double b;
    private double c;
    private double preassureThreshold;
    public NegoAgent_TDOffering nATDO;
    public BidStrategy  omBidStrat;

	/**
	 * Empty constructor for the BOA framework.
	 */
	public AStrategy() { }
	
	public AStrategy(NegotiationSession negoSession, OfferingStrategy strat, double alpha, double beta)
	{
		this.negotiationSession = negoSession;
		this.offeringStrategy = strat;
		this.a =  alpha;
		this.b = beta;
	}

	@Override
	public void init(NegotiationSession negoSession, OfferingStrategy strat, OpponentModel opponentModel, HashMap<String, Double> parameters) throws Exception 
	{
		this.negotiationSession = negoSession;
		this.offeringStrategy = strat;
		this.nATDO = new NegoAgent_TDOffering(negotiationSession, opponentModel, omBidStrat, 1, 0, .99, 0);
		
		if (parameters.get("a") != null || parameters.get("b") != null) 
		{
			a = parameters.get("a");
			b = parameters.get("b");
		} 
		else 
		{
			a = .7;
			b = .2;
            c = .2;
            lamda = .1 + .9* Math.pow(negotiationSession.getDiscountFactor()==1?.90:negotiationSession.getDiscountFactor(), b);
            
            preassureThreshold = .05;
		}
	}
	
	@Override
	public String printParameters() { return new String("[a: " + a + " b: " + b + "]"); }
        
	@Override
	public Actions determineAcceptability() 
	{
            System.out.println("-------------------------- ");
            
            try {
                double negTime = negotiationSession.getTime();
                double prediction = negotiationSession.getOpponentBidHistory().filterBetweenTime(negTime,  negTime + 0.001).size();
                double discountFactor = negotiationSession.getDiscountFactor();
                Random r = new Random();
                double randValue = r.nextDouble();
                UtilitySpace bH = negotiationSession.getUtilitySpace();
                
                if(negotiationSession.getTime() < randValue)
//                    if(prediction > 1)
                    {
                        c = 1/prediction;
//                        System.out.println(Math.pow(prediction,c));
                        
                        // TODO nATDO.getUniqueBidsCount() is returning zero instead of positive int
                        System.out.println("Unique bids size: " +  nATDO.getUniqueBidsCount() / negotiationSession.getOpponentBidHistory().size());
                        
                        lamda += (1 - lamda) * Math.pow(prediction, c); //1 / (nATDO.getUniqueBidsCount() / negotiationSession.getOpponentBidHistory().size()) * (1 - lamda) * Math.pow(prediction, c);
//                        lamda *= negTime;
                        System.out.println("lamda modified " + lamda);
                    }                
                                            
                if(negotiationSession.getTime() <= lamda)
                {
                    double maxUtility = bH.getUtility(bH.getMaxUtilityBid());
                    ACCEPTANCE_THRESHOLD = maxUtility * (1 - (1 -  Math.pow(discountFactor, 1 - lamda)) * Math.pow(negotiationSession.getTime()/lamda, a));
                }
                else
                    ACCEPTANCE_THRESHOLD = bH.getUtility(bH.getMaxUtilityBid()) * Math.pow(discountFactor, 1 - lamda);
                
                //System.out.println("negotiation Session and lamda " +negotiationSession.getTime() + " " + lamda);
                System.out.println("acceptance threshold " + ACCEPTANCE_THRESHOLD);
                
            } catch (Exception ex) { Logger.getLogger(AStrategy.class.getName()).log(Level.SEVERE, null, ex); }
            
            double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil();            
            double nextMyBidUtil = offeringStrategy.getNextBid().getMyUndiscountedUtil();
            double nextThres  = nextMyBidUtil * (1 - preassureThreshold) * Math.exp(1 - nextMyBidUtil); 
            
            System.out.println("Next Threshold " + nextThres);
            System.out.println("preassureThreshold " + preassureThreshold);            
            
            if(lastOpponentBidUtil >= ACCEPTANCE_THRESHOLD || lastOpponentBidUtil >= nextThres)
                    return Actions.Accept;
            
            return Actions.Reject;
	}
}