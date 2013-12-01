package agents.anac.y2013.NegoAgent;

import negotiator.boaframework.acceptanceconditions.other.AStrategy;
import negotiator.boaframework.offeringstrategy.other.NegoAgent_TDOffering;
import negotiator.boaframework.omstrategy.BidStrategy;
import negotiator.boaframework.opponentmodel.OpponentsModel;
//import negotiator.bidding.BidDetails;	 	
import negotiator.boaframework.AcceptanceStrategy;
//import negotiator.boaframework.NegotiationSession;	 	
import negotiator.boaframework.OMStrategy;	 	
import negotiator.boaframework.OfferingStrategy;
import negotiator.boaframework.OpponentModel;
import negotiator.boaframework.agent.BOAagent;

/**
 * An ordinary agent comprised from BOA components
 * 
 * @author Kirill Tumanov, Panagiotis Chatzichristodoulou
 * @version 01.12.2013
 */

public class NegoAgent extends BOAagent {
    
    @Override
    public void agentSetup(){
        OpponentModel om = new OpponentsModel(negotiationSession);
        OMStrategy oms = new BidStrategy(negotiationSession, om);
        OfferingStrategy offering = new NegoAgent_TDOffering(negotiationSession, om, oms, .99D, 0D);
        AcceptanceStrategy ac =  new AStrategy(negotiationSession, offering, om, .2D , 15D, 10D);
        setDecoupledComponents (ac , offering , om , oms );
    }
    @Override
    public String getName () { return "NegoAgent"; }
}