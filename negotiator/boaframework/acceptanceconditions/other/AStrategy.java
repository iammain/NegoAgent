package negotiator.boaframework.acceptanceconditions.other;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import negotiator.BidHistory;
import negotiator.bidding.BidDetails;

import negotiator.boaframework.AcceptanceStrategy;
import negotiator.boaframework.Actions;
import negotiator.boaframework.NegotiationSession;
import negotiator.boaframework.OfferingStrategy;
import negotiator.boaframework.OpponentModel;
import negotiator.boaframework.offeringstrategy.other.NegoAgent_TDOffering;
import negotiator.boaframework.omstrategy.BidStrategy;
import negotiator.boaframework.opponentmodel.OpponentsModel;
//import negotiator.utility.UtilitySpace;

/**
 * Acceptance Strategy
 * 
 * @author Kirill Tumanov, Panagiotis Chatzichristodoulou
 * @version 30.11.2013
 */
public class AStrategy extends AcceptanceStrategy
{    
    private double ACCEPTANCE_THRESHOLD = 1;
	private double a;
	private double b;
    private double c;
    private double lambda = 0;
    private double initlambda = 0.000001;       
    private double pressure;
    public NegoAgent_TDOffering TDO;
    public BidStrategy omBidStrat;
    
	/**
	 * Empty constructor for the BOA framework.
	 */
	public AStrategy() { }
	
	public AStrategy(NegotiationSession negoSession, OfferingStrategy strat, OpponentModel opponentModel, double alpha, double pressure)
	{
            
		this.negotiationSession = negoSession;
		this.offeringStrategy = strat;		
		this.omBidStrat = new BidStrategy(negoSession, opponentModel);
		this.TDO = new NegoAgent_TDOffering(negotiationSession, opponentModel, omBidStrat, .99, 0);
		this.a = alpha;
		this.pressure = pressure;
	}

    public void init(NegotiationSession negoSession, HashMap<String, Double> parameters) throws Exception 
	{
		if (parameters.get("a") != null)
			a = parameters.get("a");
		else 
			a = .2;
		
		if (parameters.get("pressure") != null)
			pressure = parameters.get("pressure");
		else 
			pressure = .05;
	}
	
	@Override
	public String printParameters() { return new String("a: " + a); }
	
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
            //System.out.println("-------------------------- ");
            
            try {
                double negTime = negotiationSession.getTime();
                double dt = 0.1; // Time difference determining a window size
                double numbidInTime = countUniqueBidsAtPeriod(negTime - dt,  negTime); // Number of unique bids in a window
                double discountFactor = negotiationSession.getDiscountFactor();    
//                UtilitySpace uS = negotiationSession.getUtilitySpace();                

                b = negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil() 
                		* 10 * (negotiationSession.getOwnBidHistory().getFirstBidDetails().getMyUndiscountedUtil() - negotiationSession.getOpponentBidHistory().getFirstBidDetails().getMyUndiscountedUtil()) * (1 - negTime);
                c = negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil() 
                		* 10 * (negotiationSession.getOwnBidHistory().getFirstBidDetails().getMyUndiscountedUtil() - negotiationSession.getOpponentBidHistory().getFirstBidDetails().getMyUndiscountedUtil()) * (1 - negTime);
                                
        		b -= b * negotiationSession.getUtilitySpace().getUtility(negotiationSession.getUtilitySpace().getMinUtilityBid());
                c -= c * negotiationSession.getUtilitySpace().getUtility(negotiationSession.getUtilitySpace().getMinUtilityBid());        	
                
                if(lambda != 0)
                {
                    double n1 = TDO.getUniqueBidsCount();
                    double n2 = negotiationSession.getOpponentBidHistory().size();
                    
                    numbidInTime = numbidInTime * dt;
                    
                    lambda +=  (n1/n2) * (1 - lambda) * Math.pow(numbidInTime, c);
                    //System.out.println("lamda modified " + lambda);
                }         
                else
                	lambda = initlambda + (1 - initlambda) * Math.pow(discountFactor, b);
                                            
                double maxUtility = negotiationSession.getOwnBidHistory().getLastBidDetails().getMyUndiscountedUtil();//uS.getUtility(uS.getMaxUtilityBid());
                
                if(negTime < lambda)
                    ACCEPTANCE_THRESHOLD = maxUtility * (1 - (1 -  Math.pow(discountFactor, 1 - lambda)) * Math.pow(negTime/lambda, a));
                else
                    ACCEPTANCE_THRESHOLD = maxUtility * Math.pow(discountFactor, 1 - negTime);
                
                //System.out.println("acceptance threshold " + ACCEPTANCE_THRESHOLD);
                
            } catch (Exception ex) { Logger.getLogger(AStrategy.class.getName()).log(Level.SEVERE, null, ex); }
            
            double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails().getMyUndiscountedUtil();            
            double nextMyBidUtil = offeringStrategy.getNextBid().getMyUndiscountedUtil();
            double nextThres  = nextMyBidUtil * (1 - pressure) * Math.exp(1 - nextMyBidUtil); 
            
            //System.out.println("Next Threshold " + nextThres);
            
            OpponentsModel oM = new OpponentsModel(negotiationSession);            
            double weakThreshold = oM.getOpponentThreshold();
            
//            if(lastOpponentBidUtil <= weakThreshold * negotiationSession.getDiscountFactor())
//                return Actions.Reject;
            
            List<BidDetails> bH = negotiationSession.getOpponentBidHistory().getHistory();
            if(bH.size()>1){
                BidDetails oppBid = (BidDetails)bH.get(this.negotiationSession.getOpponentBidHistory().size() - 1);
                BidDetails prevOppBid = (BidDetails)bH.get(this.negotiationSession.getOpponentBidHistory().size() - 2);
                oppBid.getMyUndiscountedUtil();
                prevOppBid.getMyUndiscountedUtil();
                if(isLeapOfFaith(oppBid.getMyUndiscountedUtil(), prevOppBid.getMyUndiscountedUtil())){
                    System.out.println("LeapDetected");
                    double util = oppBid.getMyUndiscountedUtil();
                    double threshold = getLeapThreshold();
                    System.out.println(util);
                    System.out.println(threshold);
                    if(util >=threshold){
                        System.out.println("accepted");
                        return Actions.Accept;
                    }
                }
            }
            if(lastOpponentBidUtil >= ACCEPTANCE_THRESHOLD || lastOpponentBidUtil >= nextThres){
                System.out.println("lastOpponentBidUtil  ACCEPTANCE_THRESHOLD next thres" + " "
                        + "" + lastOpponentBidUtil+ " " +ACCEPTANCE_THRESHOLD  + " "+ nextThres);
                    return Actions.Accept;
            }
            return Actions.Reject;
	}
        
        public boolean isLeapOfFaith(double bidUtil , double prevBidUtil){
            double difference = bidUtil-prevBidUtil;
            return difference*getTimeMultiplier()>.10;
        }
        public double getSigmoidTime(){
            return negotiationSession.getTime()*10-5;
        }
        public double getTimeMultiplier(){
            double power = 1.0/3.33;
            return 1/(Math.pow( 1+ Math.exp(-getSigmoidTime()) , power));
        }
        public double getFinishTimeMultiplier(){
            double power = 1.0/3.33;
            return 1/(Math.pow( 1+ Math.exp(getSigmoidTime()) , power))+.66;
        }
        public double getLeapThreshold(){
            //plot 1/(1+e^(-x)) ^(1/3)   ,1/(1+e^x)^(1/3)+.66 from x=-5to5
            double timeInSigmDomain = getSigmoidTime();
            double thresh;
            if(timeInSigmDomain<2){
                thresh = .925;
            }else if(timeInSigmDomain>=2 && timeInSigmDomain<3.5){
                thresh = (.925)*getTimeMultiplier();
            }else{
                thresh = (.925)*getFinishTimeMultiplier();
            }
            return thresh;
        
        }
}