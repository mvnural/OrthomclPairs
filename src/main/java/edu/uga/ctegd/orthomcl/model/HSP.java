package edu.uga.ctegd.orthomcl.model;

/**
 * Created by mnural on 2/16/14.
 */
public class HSP {
    private String queryId;
    private String subjectId;
    private String queryTaxonId;
    private String subjectTaxonId;
    double evalue;
    double percentMatch;

    private String evalueExp;
    private String evalueMant;

    public HSP(String queryId,String subjectId, String queryTaxonId, String subjectTaxonId, double evalue, double percentMatch, String evalueMant, String evalueExp) {
        this.queryId = queryId;
        this.subjectId = subjectId;
        this.queryTaxonId = queryTaxonId;
        this.subjectTaxonId = subjectTaxonId;
        this.evalue = evalue;
        this.percentMatch = percentMatch;
        this.evalueExp = evalueExp;
        this.evalueMant = evalueMant;
    }

    public HSP(String queryId,String subjectId, String queryTaxonId, String subjectTaxonId, double evalue,double percentMatch) {
        this.queryId = queryId;
        this.subjectId = subjectId;
        this.queryTaxonId = queryTaxonId;
        this.subjectTaxonId = subjectTaxonId;
        this.evalue = evalue;
        this.percentMatch = percentMatch;
    }

    public String getEvalueExp() {
        return evalueExp;
    }

    public String getEvalueMant() {
        return evalueMant;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getSubjectTaxonId() {
        return subjectTaxonId;
    }

    public double getEvalue() {
        return evalue;
    }

    public double getPercentMatch() {
        return percentMatch;
    }

    public String getQueryId() {
        return queryId;
    }

    public String getQueryTaxonId() {
        return queryTaxonId;
    }

    public String toString(){
        return subjectId + "\t" + evalue + "\t" + evalueMant + "\t" + evalueExp;
    }

    public boolean equals(Object anotherHSP){
        if (anotherHSP.getClass() != getClass()){
            return false;
        }else{
            HSP other = (HSP) anotherHSP;
            return this.subjectId.equals(other.queryId) && this.queryId.equals(other.subjectId);
        }
    }
}
