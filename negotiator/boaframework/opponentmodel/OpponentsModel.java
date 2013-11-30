package negotiator.boaframework.opponentmodel;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import negotiator.Bid;
import negotiator.BidHistory;
import negotiator.bidding.BidDetails;
import negotiator.boaframework.NegotiationSession;
import negotiator.boaframework.OpponentModel;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.Objective;
import negotiator.issue.ValueDiscrete;
import negotiator.utility.Evaluator;
import negotiator.utility.EvaluatorDiscrete;
import negotiator.utility.UtilitySpace;

public class OpponentsModel
  extends OpponentModel
{
  private double changeCoef = 0.4D;
  private double decrFactor = 0.1D;
  private double startCoef = 0.4D;
  private int learnValueAddition = 1;
  private int issues;
  private ArrayList<Bid> latestHistory;
  private double lastTime = 0.0D;
  private ArrayList<Double> timeList = new ArrayList<Double>();
  private int roundsEst = 15000;
  
  public OpponentsModel() {}
  
  public OpponentsModel(NegotiationSession negotiationSession)
  {
    this.negotiationSession = negotiationSession;
    this.latestHistory = new ArrayList<Bid>();
    initializeModel();
  }
  
  public void init(NegotiationSession negotiationSession, HashMap<String, Double> parameters)
    throws Exception
  {
    this.negotiationSession = negotiationSession;
    this.latestHistory = new ArrayList<Bid>();
    initializeModel();
  }
  
  private void initializeModel()
  {
    this.opponentUtilitySpace = new UtilitySpace(this.negotiationSession.getUtilitySpace());
    this.issues = this.opponentUtilitySpace.getDomain().getIssues().size();
    double mosCommonWt = 1.0D / this.issues;
    for (Map.Entry<Objective, Evaluator> e : this.opponentUtilitySpace.getEvaluators()){
      this.opponentUtilitySpace.unlock((Objective)e.getKey());
      ((Evaluator)e.getValue()).setWeight(mosCommonWt);
      try{
        for (ValueDiscrete vd : ((IssueDiscrete)e.getKey()).getValues()) {
          ((EvaluatorDiscrete)e.getValue()).setEvaluation(vd, 1);
        }
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
      }
    }
  }
  
  private HashMap<Integer, Integer> determineDifference(BidDetails first, BidDetails second)
  {
    HashMap<Integer, Integer> diff = new HashMap<Integer, Integer>();
    try{
      for (Issue i : this.opponentUtilitySpace.getDomain().getIssues()) {
        diff.put(Integer.valueOf(i.getNumber()), Integer.valueOf(
          ((ValueDiscrete)first.getBid().getValue(i.getNumber())).equals((ValueDiscrete)second.getBid().getValue(i.getNumber())) ? 0 : 1));
      }
    }
    catch (Exception ex){
      ex.printStackTrace();
    }
    return diff;
  }
  
  public void updateModel(Bid opponentBid, double time)
  {
    if (this.negotiationSession.getOpponentBidHistory().size() < 2)
    {
      this.latestHistory.add(opponentBid);
      return;
    }
        
    double negTime    = negotiationSession.getTime();
    double consetionDegree = negotiationSession.getOpponentBidHistory().filterBetweenTime(negTime-.001,  negTime).size();
    
    //Simple frequency stuff
    if(consetionDegree<3){
        //System.out.println("returned due to frequency thingy oponent proposes less than 3  ");
        return;
    }
    updateRoundEst(time);

    int numberOfUnchanged = 0;
    BidDetails oppBid = (BidDetails)this.negotiationSession.getOpponentBidHistory().
            getHistory().get(this.negotiationSession.getOpponentBidHistory().size() - 1);
    BidDetails prevOppBid = (BidDetails)this.negotiationSession.getOpponentBidHistory().
            getHistory().get(this.negotiationSession.getOpponentBidHistory().size() - 2);
    HashMap<Integer, Integer> lastDiffSet = determineDifference(prevOppBid, oppBid);
    for (Integer i : lastDiffSet.keySet()) {
      if (((Integer)lastDiffSet.get(i)).intValue() == 0) {
        numberOfUnchanged++;
      }
    }
    if (numberOfUnchanged == this.issues)
    {
      this.changeCoef -= this.decrFactor;
      if (this.changeCoef < 0.0D) {
        this.changeCoef = 0.0D;
      }
    }
    else
    {
      this.changeCoef = this.startCoef;
      if (!this.latestHistory.contains(opponentBid)) {
        this.latestHistory.add(opponentBid);
      }
    }
    double goldenValue = this.changeCoef / this.issues;
    
    double totalSum = 1.0D + goldenValue * numberOfUnchanged;
    
    double maximumWeight = 1.0D - this.issues * goldenValue / totalSum;
    for (Integer i : lastDiffSet.keySet()) {
      if ((((Integer)lastDiffSet.get(i)).intValue() == 0) && (this.opponentUtilitySpace.getWeight(i.intValue()) < maximumWeight)) {
        this.opponentUtilitySpace.setWeight(this.opponentUtilitySpace.getDomain().getObjective(i.intValue()), 
                (this.opponentUtilitySpace.getWeight(i.intValue()) + goldenValue) / totalSum);
      } else {
        this.opponentUtilitySpace.setWeight(this.opponentUtilitySpace.getDomain().getObjective(i.intValue()), 
                this.opponentUtilitySpace.getWeight(i.intValue()) / totalSum);
      }
    }
    try{
      for (Iterator<Map.Entry<Objective, Evaluator>> localIterator = this.opponentUtilitySpace.getEvaluators().iterator(); localIterator.hasNext();)
      {
        Map.Entry<Objective, Evaluator> e = (Entry<Objective, Evaluator>)localIterator.next();
        ((EvaluatorDiscrete)e.getValue()).setEvaluation(oppBid.getBid().getValue(((IssueDiscrete)e.getKey()).getNumber()), 
          this.learnValueAddition + 
          ((EvaluatorDiscrete)e.getValue()).getEvaluationNotNormalized(
          (ValueDiscrete)oppBid.getBid().getValue(((IssueDiscrete)e.getKey()).getNumber())).intValue());
      }
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }
  
  public double getBidEvaluation(Bid bid){
    double result = 0.0D;
    try{
      if (this.latestHistory.size() > 0.1D * this.negotiationSession.getOpponentBidHistory().size()){
        result = this.opponentUtilitySpace.getUtility(bid);
      }else{
        Objective root = this.opponentUtilitySpace.getDomain().getObjectivesRoot();
        Enumeration<Objective> issueEnum = root.getPreorderIssueEnumeration();
        while (issueEnum.hasMoreElements()){
          Objective is = (Objective)issueEnum.nextElement();
          Evaluator eval = this.opponentUtilitySpace.getEvaluator(is.getNumber());
          result += eval.getWeight() * valueEval(is, bid);
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return result;
  }
  
  private double mapToEval(int freq)
  {
    return 70.0D - 1.0D / (10000.0D / this.roundsEst * freq / 15000.0D + 0.01428571428571429D);
  }
  
  public double valueEval(Objective obj, Bid bid)
    throws Exception
  {
    EvaluatorDiscrete lEval = (EvaluatorDiscrete)this.opponentUtilitySpace.getEvaluator(obj.getNumber());
    int variable = 0;
    for (ValueDiscrete vd : lEval.getValues()) {
      if (variable < lEval.getEvaluationNotNormalized(vd).intValue()) {
        variable = lEval.getEvaluationNotNormalized(vd).intValue();
      }
    }
    int idc = lEval.getEvaluationNotNormalized(bid, obj.getNumber()).intValue();
    return mapToEval(idc) / mapToEval(variable);
  }
  
  private void updateRoundEst(double t)
  {
    this.timeList.add(Double.valueOf(t - this.lastTime));
    this.lastTime = t;
    if (this.timeList.size() >= 10)
    {
      if (this.timeList.size() > 10) {
        this.timeList.remove(0);
      }
      double sum = 0.0D;
      for (int i = 0; i < this.timeList.size(); i++) {
        sum += ((Double)this.timeList.get(i)).doubleValue();
      }
      this.roundsEst = ((int)(this.timeList.size() / sum));
    }
  }
  public double getOpponentThreshold(){
    double NegotiatedTooLong = .45;
    double oponentFirstUtil = negotiationSession.getOpponentBidHistory().getFirstBidDetails().getMyUndiscountedUtil();
    double myUtility        = negotiationSession.getOwnBidHistory().getFirstBidDetails().getMyUndiscountedUtil();
    
    double dous = myUtility - oponentFirstUtil;
    
    double threshold=1;
    
    BidHistory bH = negotiationSession.getOpponentBidHistory();
    bH.getLastBidDetails().getMyUndiscountedUtil();
    int horizon = 20;
    if(bH.getHistory().size()>horizon){
        double mean = 0;
        for(int i = bH.getHistory().size()-1;i>bH.getHistory().size()-horizon;i--){
            BidDetails prevOppBid = (BidDetails)this.negotiationSession.getOpponentBidHistory().
            getHistory().get(i);
            
            mean += prevOppBid.getMyUndiscountedUtil();
            
        }
        mean = mean/horizon;
        double variance  = 0;
        for(int i = bH.getHistory().size()-1;i>bH.getHistory().size()-horizon;i--){
            BidDetails prevOppBid = (BidDetails)this.negotiationSession.getOpponentBidHistory().
            getHistory().get(i);
            
            variance += (prevOppBid.getMyUndiscountedUtil() - mean)*(prevOppBid.getMyUndiscountedUtil() - mean);
        }
        variance /= horizon;
        double asdfasd = (mean + dous)/2;
        double time = negotiationSession.getTime();
        
        if(time>.80 && time<=.90){
            double lowertime = .80;
            for(int i=1;i<11;i++){
                double uppertime = lowertime +.01;
                if(uppertime>time && lowertime<time){
                    threshold = asdfasd- (1-i)*variance;
                    break;
                }
                lowertime +=.01;
            }
        }else if(time>.90){
            double lowertime = .90;
            for(int i=1;i<11;i++){
                double uppertime = lowertime +.01;
                if(uppertime>time && lowertime<time){
                    threshold = asdfasd+ (1-i)*variance;
                    break;
                }
                lowertime +=.01;
            }
        }else 
            threshold = asdfasd+ variance;
        
        if(threshold>=1)
            threshold = (mean + dous)/2;
        
        if(negotiationSession.getTime()>NegotiatedTooLong || threshold<0)
            return 0;
    }    
    return threshold;
  }
}
