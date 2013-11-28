package negotiator.boaframework.opponentmodel;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import negotiator.Bid;
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
  private double weightCoef = 0.4D;
  private double weightDecr = 0.2D;
  private double initCoef = 0.4D;
  private int learnValueAddition = 1;
  private int amountOfIssues;
  private ArrayList<Bid> smallHistory;
  private double lastTime = 0.0D;
  private ArrayList<Double> timeList = new ArrayList<Double>();
  private int roundsEst = 15000;
  
  public OpponentsModel() {}
  
  public OpponentsModel(NegotiationSession negotiationSession)
  {
    this.negotiationSession = negotiationSession;
    this.smallHistory = new ArrayList<Bid>();
    initializeModel();
  }
  
  public void init(NegotiationSession negotiationSession, HashMap<String, Double> parameters)
    throws Exception
  {
    this.negotiationSession = negotiationSession;
    this.smallHistory = new ArrayList<Bid>();
    initializeModel();
  }
  
  private void initializeModel()
  {
    this.opponentUtilitySpace = new UtilitySpace(this.negotiationSession.getUtilitySpace());
    this.amountOfIssues = this.opponentUtilitySpace.getDomain().getIssues().size();
    double commonWeight = 1.0D / this.amountOfIssues;
    for (Map.Entry<Objective, Evaluator> e : this.opponentUtilitySpace.getEvaluators())
    {
      this.opponentUtilitySpace.unlock((Objective)e.getKey());
      ((Evaluator)e.getValue()).setWeight(commonWeight);
      try
      {
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
    try
    {
      for (Issue i : this.opponentUtilitySpace.getDomain().getIssues()) {
        diff.put(Integer.valueOf(i.getNumber()), Integer.valueOf(
          ((ValueDiscrete)first.getBid().getValue(i.getNumber())).equals((ValueDiscrete)second.getBid().getValue(i.getNumber())) ? 0 : 1));
      }
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
    return diff;
  }
  
  public void updateModel(Bid opponentBid, double time)
  {
    if (this.negotiationSession.getOpponentBidHistory().size() < 2)
    {
      this.smallHistory.add(opponentBid);
      return;
    }
    updateRoundEst(time);
    

    int numberOfUnchanged = 0;
    BidDetails oppBid = (BidDetails)this.negotiationSession.getOpponentBidHistory().getHistory().get(this.negotiationSession.getOpponentBidHistory().size() - 1);
    BidDetails prevOppBid = (BidDetails)this.negotiationSession.getOpponentBidHistory().getHistory().get(this.negotiationSession.getOpponentBidHistory().size() - 2);
    HashMap<Integer, Integer> lastDiffSet = determineDifference(prevOppBid, oppBid);
    for (Integer i : lastDiffSet.keySet()) {
      if (((Integer)lastDiffSet.get(i)).intValue() == 0) {
        numberOfUnchanged++;
      }
    }
    if (numberOfUnchanged == this.amountOfIssues)
    {
      this.weightCoef -= this.weightDecr;
      if (this.weightCoef < 0.0D) {
        this.weightCoef = 0.0D;
      }
    }
    else
    {
      this.weightCoef = this.initCoef;
      if (!this.smallHistory.contains(opponentBid)) {
        this.smallHistory.add(opponentBid);
      }
    }
    double goldenValue = this.weightCoef / this.amountOfIssues;
    
    double totalSum = 1.0D + goldenValue * numberOfUnchanged;
    
    double maximumWeight = 1.0D - this.amountOfIssues * goldenValue / totalSum;
    for (Integer i : lastDiffSet.keySet()) {
      if ((((Integer)lastDiffSet.get(i)).intValue() == 0) && (this.opponentUtilitySpace.getWeight(i.intValue()) < maximumWeight)) {
        this.opponentUtilitySpace.setWeight(this.opponentUtilitySpace.getDomain().getObjective(i.intValue()), (this.opponentUtilitySpace.getWeight(i.intValue()) + goldenValue) / totalSum);
      } else {
        this.opponentUtilitySpace.setWeight(this.opponentUtilitySpace.getDomain().getObjective(i.intValue()), this.opponentUtilitySpace.getWeight(i.intValue()) / totalSum);
      }
    }
    try
    {
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
  
  public double getBidEvaluation(Bid bid)
  {
    double result = 0.0D;
    try
    {
      if (this.smallHistory.size() > 0.2D * this.negotiationSession.getOpponentBidHistory().size())
      {
        result = this.opponentUtilitySpace.getUtility(bid);
      }
      else
      {
        Objective root = this.opponentUtilitySpace.getDomain().getObjectivesRoot();
        Enumeration<Objective> issueEnum = root.getPreorderIssueEnumeration();
        while (issueEnum.hasMoreElements())
        {
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
}
