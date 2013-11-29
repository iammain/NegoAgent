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
import negotiator.boaframework.opponentmodel.OpponentsModel;
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
                omBidStrat = new BidStrategy(negotiationSession, opponentModel);
                this.nATDO = new NegoAgent_TDOffering(negoSession, opponentModel, omBidStrat, 1, 0, 0.99, 0);
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
            
            try {
                double negTime    = negotiationSession.getTime();
                double prediction = negotiationSession.getOpponentBidHistory().filterBetweenTime(negTime-.001,  negTime).size();
                double discountFactor = negotiationSession.getDiscountFactor()!=1?negotiationSession.getDiscountFactor():.95;
                double randValue = Math.random()*.2-.1;
                if(negotiationSession.getTime() <.80+randValue ){
                    if(prediction>1 ){
                        c = 1/prediction;
                        lamda = lamda+(1-lamda)*Math.pow(prediction, c );
                        lamda = lamda*negTime;
                    }
                }
                UtilitySpace bH = negotiationSession.getUtilitySpace();
                if(ACCEPTANCE_THRESHOLD== Integer.MIN_VALUE){
                        double maxUtility = bH.getUtility(bH.getMaxUtilityBid());
                        ACCEPTANCE_THRESHOLD = maxUtility*discountFactor;
                }
                if(negotiationSession.getTime()>lamda  && negotiationSession.getTime()>.5){  
                    //System.out.println("time bigger than lamda");
                        ACCEPTANCE_THRESHOLD = ACCEPTANCE_THRESHOLD;
                }else{
                    if(negotiationSession.getTime()<1 - discountFactor*(1-discountFactor)){
                        ACCEPTANCE_THRESHOLD = .95;
                    }
                    else{
                        double maxUtility = bH.getUtility(bH.getMaxUtilityBid());
                        double tempAcceptance = maxUtility-(maxUtility-maxUtility*discountFactor)
                                *Math.pow(negotiationSession.getTime()/lamda ,a);
                        ACCEPTANCE_THRESHOLD = tempAcceptance;
                    }
                    
                }
                
            } catch (Exception ex) {
                Logger.getLogger(AStrategy.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
            
            double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil();
            negotiationSession.getOwnBidHistory().getBestBidDetails().getMyUndiscountedUtil();
            double nextMyBidUtil = offeringStrategy.getNextBid().getMyUndiscountedUtil();
            
            double nextThres  = nextMyBidUtil * (1 - preassureThreshold) * Math.exp(1 - nextMyBidUtil);
            
            OpponentsModel oM = new OpponentsModel(negotiationSession);
            
            double weakThreshold = oM.getOpponentThreshold();
            
            double nextBidUtil = omBidStrat.getBid(negotiationSession.getOpponentBidHistory().getNBestBids(10)).getMyUndiscountedUtil();
            if(lastOpponentBidUtil> weakThreshold){
                if( lastOpponentBidUtil > ACCEPTANCE_THRESHOLD 
                && lastOpponentBidUtil<=nextBidUtil ){
                    boolean safd = lastOpponentBidUtil > ACCEPTANCE_THRESHOLD;
                    safd = lastOpponentBidUtil > nextThres;             
                    return Actions.Accept;
                }
            }
                return Actions.Reject;
	}
}