package negotiator.boaframework.acceptanceconditions.other;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import negotiator.boaframework.AcceptanceStrategy;
import negotiator.boaframework.Actions;
import negotiator.boaframework.NegotiationSession;
import negotiator.boaframework.OfferingStrategy;
import negotiator.boaframework.OpponentModel;
import negotiator.boaframework.offeringstrategy.other.NegoAgent_TDOffering;
import negotiator.boaframework.omstrategy.BidStrategy;
import negotiator.boaframework.opponentmodel.OpponentsModel;
import negotiator.utility.UtilitySpace;

/**
 * Acceptance Strategy
 * 
 * @author Kirill Tumanov, Panagiotis Chatzichristodoulou
 * @version 30.11.2013
 */
public class AStrategy extends AcceptanceStrategy {
    
    private double ACCEPTANCE_THRESHOLD = 1;
    private double lambda;
    private double initlambda;
	private double a;
	private double b;
    private double c;
    private double preassureThreshold;
    public NegoAgent_TDOffering TDO;
    public BidStrategy omBidStrat;
	

	/**
	 * Empty constructor for the BOA framework.
	 */
	public AStrategy() { }
	
	public AStrategy(NegotiationSession negoSession, OfferingStrategy strat, OpponentModel opponentModel, double alpha, double beta)
	{
		this.negotiationSession = negoSession;
		this.offeringStrategy = strat;		
		this.omBidStrat = new BidStrategy(negoSession, opponentModel);
		this.TDO = new NegoAgent_TDOffering(negotiationSession, opponentModel, omBidStrat, .99, 0);
		
		this.a =  alpha;
		this.b = beta;
	}

        public void init(NegotiationSession negoSession, HashMap<String, Double> parameters) throws Exception 
	{
		if (parameters.get("a") != null || parameters.get("b") != null) 
		{
			a = parameters.get("a");
			b = parameters.get("b");
		} 
		else 
		{
			a = .2;
			b = 15;
            c = 10;
            lambda = 0;
            initlambda = 0.000001;            
            preassureThreshold = .05;
         
            b -= b * negoSession.getUtilitySpace().getUtility(negoSession.getUtilitySpace().getMinUtilityBid());
            c -= c * negoSession.getUtilitySpace().getUtility(negoSession.getUtilitySpace().getMinUtilityBid());
		}
	}
	
	@Override
	public String printParameters() { return new String("[a: " + a + " b: " + b + "]"); }
	
	/**
	 * Method which calculates a number of unique opponent bids given time boundaries
	 */
	public int countUniqueBidsAtPeriod(double t1, double t2)
	{
		if (negotiationSession.getOpponentBidHistory().filterBetweenTime(t1, t2).size() != 0)
		{
			List<Double> ub = new ArrayList<Double>();
			
			for (int i = 0; i < negotiationSession.getOpponentBidHistory().filterBetweenTime(t1, t2).size(); i ++)
				ub.add(negotiationSession.getOpponentBidHistory().filterBetweenTime(t1, t2).getHistory().get(i).getMyUndiscountedUtil());
			
			// Remove duplicate obstacles
		  	Set<Double> s = new LinkedHashSet<Double>(ub);
		  	ub.clear();
		  	ub.addAll(s);
		  	
		  	return ub.size();
		}
		
		return 0;
	}
        
	@Override
	public Actions determineAcceptability() 
	{
            System.out.println("-------------------------- ");
            
            try {
                double negTime = negotiationSession.getTime();
                double dt = 0.1; // Time difference determining a window size
                double numbidInTime = countUniqueBidsAtPeriod(negTime - dt,  negTime); // Number of unique bids in a window
                double discountFactor = negotiationSession.getDiscountFactor();    
                UtilitySpace uS = negotiationSession.getUtilitySpace();
                               
                if(lambda != 0)
                {
                    double n1 = TDO.getUniqueBidsCount();
                    double n2 = negotiationSession.getOpponentBidHistory().size();
                    
                    numbidInTime = numbidInTime * dt;
                    
                    lambda +=  (n1/n2) * (1 - lambda) * Math.pow(numbidInTime, c);
                    System.out.println("lamda modified " + lambda);
                }         
                else
                	lambda = initlambda + (1 - initlambda) * Math.pow(discountFactor, b);
                                            
                if(negTime < lambda)
                {
                    double maxUtility = uS.getUtility(uS.getMaxUtilityBid());
                    ACCEPTANCE_THRESHOLD = maxUtility * (1 - (1 -  Math.pow(discountFactor, 1 - lambda)) * Math.pow(negTime/lambda, a));
                }
                else
                    ACCEPTANCE_THRESHOLD = uS.getUtility(uS.getMaxUtilityBid()) * Math.pow(discountFactor, 1 - negTime);
                
                System.out.println("acceptance threshold " + ACCEPTANCE_THRESHOLD);
                
            } catch (Exception ex) { Logger.getLogger(AStrategy.class.getName()).log(Level.SEVERE, null, ex); }
            
            double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil();            
            double nextMyBidUtil = offeringStrategy.getNextBid().getMyUndiscountedUtil();
            double nextThres  = nextMyBidUtil * (1 - preassureThreshold) * Math.exp(1 - nextMyBidUtil); 
            
            System.out.println("Next Threshold " + nextThres);
            //System.out.println("preassureThreshold " + preassureThreshold);
            
            OpponentsModel oM = new OpponentsModel(negotiationSession);            
            double weakThreshold = oM.getOpponentThreshold();
//            System.out.println("Weak discounted thresh "  + weakThreshold * negotiationSession.getDiscountFactor());
            
            if(lastOpponentBidUtil <= weakThreshold * negotiationSession.getDiscountFactor())
                return Actions.Reject;
//            System.out.println("Passed weak threshold");
            
            if(lastOpponentBidUtil >= ACCEPTANCE_THRESHOLD || lastOpponentBidUtil >= nextThres)
                    return Actions.Accept;
            
            return Actions.Reject;
	}
}