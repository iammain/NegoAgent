package negotiator.boaframework.acceptanceconditions.other;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import negotiator.Bid;
import negotiator.BidHistory;
import negotiator.boaframework.AcceptanceStrategy;
import negotiator.boaframework.Actions;
import negotiator.boaframework.NegotiationSession;
import negotiator.boaframework.OfferingStrategy;
import negotiator.boaframework.OpponentModel;
import negotiator.utility.UtilitySpace;

/**
 * This Acceptance Condition will accept an opponent bid if the utility is higher than the 
 * bid the agent is ready to present
 * 
 * Decoupling Negotiating Agents to Explore the Space of Negotiation Strategies
 * T. Baarslag, K. Hindriks, M. Hendrikx, A. Dirkzwager, C.M. Jonker
 * 
 * @author Alex Dirkzwager, Mark Hendrikx
 * @version 18/12/11
 */
public class AStrategy extends AcceptanceStrategy {
    
        private double ACCEPTANCE_THRESHOLD =  Integer.MIN_VALUE;
        private double lamda;
	private double a;
	private double b;
        private double c;
        private double preassureThreshold;

	/**
	 * Empty constructor for the BOA framework.
	 */
	public AStrategy() { }
	
	public AStrategy(NegotiationSession negoSession, OfferingStrategy strat, double alpha, double beta){
		this.negotiationSession = negoSession;
		this.offeringStrategy = strat;
		this.a =  alpha;
		this.b = beta;
                this.c = .3;
	}

	@Override
	public void init(NegotiationSession negoSession, OfferingStrategy strat, OpponentModel opponentModel, HashMap<String, Double> parameters) throws Exception {
		this.negotiationSession = negoSession;
		this.offeringStrategy = strat;

		if (parameters.get("a") != null || parameters.get("b") != null) {
			a = parameters.get("a");
			b = parameters.get("b");
		} else {
			a = .7;
			b = .2;
                        c = .2;
                        lamda =.1 + .9* Math.pow(negotiationSession.getDiscountFactor(), b);
                        preassureThreshold = .1;
		}
	}
	@Override
	public String printParameters() {
		String str = "[a: " + a + " b: " + b + "]";
		return str;
	}
        
	@Override
	public Actions determineAcceptability() {
            System.out.println("New Debug");
            
            try {
                double negTime    = negotiationSession.getTime();
                double prediction =negotiationSession.getOpponentBidHistory().filterBetweenTime(negTime-.001,  negTime).size();
                if(negotiationSession.getTime()!=0){
                    if(prediction!=1){
                        c = 1/prediction;
                        lamda = lamda+(1-lamda)*Math.pow(prediction, c );
                        lamda = lamda*negTime*2;
                    }
                }
                
                
                UtilitySpace bH = negotiationSession.getUtilitySpace();                
                if(negotiationSession.getTime()>lamda){
                    double maxUtility = bH.getUtility(bH.getMaxUtilityBid());
                    if(ACCEPTANCE_THRESHOLD== Integer.MIN_VALUE)
                        ACCEPTANCE_THRESHOLD = maxUtility*negotiationSession.getDiscountFactor();
                    else
                        ACCEPTANCE_THRESHOLD = ACCEPTANCE_THRESHOLD*negotiationSession.getDiscountFactor();
                }else{
                    double maxUtility = bH.getUtility(bH.getMaxUtilityBid());                    
                    double tempAcceptance = maxUtility-(maxUtility-maxUtility*negotiationSession.getDiscountFactor())*
                            Math.pow(negotiationSession.getTime()/lamda ,a);
                    ACCEPTANCE_THRESHOLD = tempAcceptance;
                }
                System.out.println("Acceptance threshold " +ACCEPTANCE_THRESHOLD);
                
            } catch (Exception ex) {
                Logger.getLogger(AStrategy.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil();
            double nextMyBidUtil = offeringStrategy.getNextBid().getMyUndiscountedUtil();
            System.out.println("Last oponents bid util " +lastOpponentBidUtil);
            System.out.println("Next bid util  " + nextMyBidUtil);
            System.out.println("Current time " + negotiationSession.getTime());
            double nextThres  = nextMyBidUtil + nextMyBidUtil*preassureThreshold; 
            System.out.println("Next Threshold " + nextThres);
            if(lastOpponentBidUtil > ACCEPTANCE_THRESHOLD ||lastOpponentBidUtil > nextThres)
                    return Actions.Accept;
            return Actions.Reject;
	}
}