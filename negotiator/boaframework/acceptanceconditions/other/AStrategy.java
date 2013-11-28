package negotiator.boaframework.acceptanceconditions.other;

import java.util.HashMap;
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
    
        private double ACCEPTANCE_THRESHOLD =  Integer.MIN_VALUE;
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
                        lamda =.1 + .9* Math.pow(negotiationSession.getDiscountFactor()==1?.90:negotiationSession.getDiscountFactor(), b);
                        
                        preassureThreshold = .05;
		}
	}
	@Override
	public String printParameters() {
		String str = "[a: " + a + " b: " + b + "]";
		return str;
	}
        
	@Override
	public Actions determineAcceptability() {
            System.out.println("-------------------------- ");
            
            try {
                double negTime    = negotiationSession.getTime();
                double prediction = negotiationSession.getOpponentBidHistory().filterBetweenTime(negTime-.001,  negTime).size();
                double discountFactor = negotiationSession.getDiscountFactor()!=1?negotiationSession.getDiscountFactor():.95;
                double randValue = Math.random()*.2-.1;
                if(negotiationSession.getTime()>0 && negotiationSession.getTime() <.80+randValue ){
                    if(prediction>1 ){
                        c = 1/prediction;
                        System.out.println(Math.pow(prediction,c));
                        
                        lamda = lamda+(1-lamda)*Math.pow(prediction, c );
                        lamda = lamda*negTime;
                        System.out.println("lamda modified " + lamda);
                    }
                }
                UtilitySpace bH = negotiationSession.getUtilitySpace();
                if(ACCEPTANCE_THRESHOLD== Integer.MIN_VALUE){
                        double maxUtility = bH.getUtility(bH.getMaxUtilityBid());
                        ACCEPTANCE_THRESHOLD = maxUtility*discountFactor;
                }
                if(negotiationSession.getTime()>lamda){                    
                        ACCEPTANCE_THRESHOLD = ACCEPTANCE_THRESHOLD;
                }else{
                    double maxUtility = bH.getUtility(bH.getMaxUtilityBid());
                    double tempAcceptance = maxUtility-(maxUtility-maxUtility*discountFactor)
                            *Math.pow(negotiationSession.getTime()/lamda ,a);
                    ACCEPTANCE_THRESHOLD = tempAcceptance;
                }
                //System.out.println("negotiation Session and lamda " +negotiationSession.getTime() + " " + lamda);
                //System.out.println("acceptance threshold " + ACCEPTANCE_THRESHOLD);
                
            } catch (Exception ex) {
                Logger.getLogger(AStrategy.class.getName()).log(Level.SEVERE, null, ex);
            }
            double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil();
            negotiationSession.getOwnBidHistory().getBestBidDetails().getMyUndiscountedUtil();
            double nextMyBidUtil = offeringStrategy.getNextBid().getMyUndiscountedUtil();
            
            //System.out.println("is nash " + nATDO.isNash());
            double nextThres  = nextMyBidUtil + nextMyBidUtil*preassureThreshold; 
            System.out.println("Next Threshold " + nextThres);
            System.out.println("preassureThreshold " + preassureThreshold);
            if(lastOpponentBidUtil > ACCEPTANCE_THRESHOLD ||lastOpponentBidUtil > nextThres)
                    return Actions.Accept;
            return Actions.Reject;
	}
}