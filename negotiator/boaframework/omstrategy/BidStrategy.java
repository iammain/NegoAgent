package negotiator.boaframework.omstrategy;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import negotiator.bidding.BidDetails;
import negotiator.boaframework.NegotiationSession;
import negotiator.boaframework.OMStrategy;
import negotiator.boaframework.OpponentModel;

public final class BidStrategy extends OMStrategy{
  private ArrayDeque<Double> lastTen;
  private int secondBestCounter = 1;
  
  public BidStrategy() {}
  
  public BidStrategy(NegotiationSession negotiationSession, OpponentModel model){
    initializeAgent(negotiationSession, model);
  }
  
  public void init(NegotiationSession nSession, OpponentModel oppModel, HashMap<String, Double> parameters) throws Exception{
    initializeAgent(nSession, oppModel);
  }
  
  private void initializeAgent(NegotiationSession negotiationSession, OpponentModel model)
  {
    try
    {
      super.init(negotiationSession, model);
      this.lastTen = new ArrayDeque(11);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  public BidDetails getBid(List<BidDetails> list){
    Collections.sort(list, new Comparing(this.model));
    BidDetails opponentBestBid = (BidDetails)list.get(0);
    boolean allEqual = true;
    for (Iterator localIterator = this.lastTen.iterator(); localIterator.hasNext();){
      double bid = ((Double)localIterator.next()).doubleValue();
      if (bid != opponentBestBid.getMyUndiscountedUtil()) {
        allEqual = false;
      }
    }
    if (allEqual)
    {
      this.secondBestCounter += 1;
      if (list.size() > 1) {
        opponentBestBid = (BidDetails)list.get(1);
      }
    }
    this.lastTen.addLast(Double.valueOf(opponentBestBid.getMyUndiscountedUtil()));
    if (this.lastTen.size() > 10) {
      this.lastTen.removeFirst();
    }
    return opponentBestBid;
  }
  
  public int getSecondBestCount(){
    return this.secondBestCounter;
  }  
  public boolean canUpdateOM(){
    return true;
  }  
  private static final class Comparing
    implements Comparator<BidDetails>
  {
    private final OpponentModel model;
    
    protected Comparing(OpponentModel model)
    {
      this.model = model;
    }
    
    @Override
    public int compare(BidDetails a, BidDetails b)
    {
      double evalA = this.model.getBidEvaluation(a.getBid());
      double evalB = this.model.getBidEvaluation(b.getBid());
      if (evalA < evalB) {
        return 1;
      }
      if (evalA > evalB) {
        return -1;
      }
      return 0;
    }
  }
}