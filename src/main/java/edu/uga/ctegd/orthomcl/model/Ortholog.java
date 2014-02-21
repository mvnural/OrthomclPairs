package edu.uga.ctegd.orthomcl.model;

/**
 * Created by mnural on 2/17/14.
 */
public class Ortholog {
    public final String queryId;
    public final String subjectId;
    public double score;

    public TaxonPair taxonPair;

    public Ortholog(String queryId, String subjectId,TaxonPair taxonPair, double score) {
        this.queryId = queryId;
        this.subjectId = subjectId;
        this.taxonPair = taxonPair;
        this.score = score;
    }

    @Override
    public boolean equals(Object anotherObject){
        if(anotherObject.getClass() != getClass()){
            return false;
        }else{
            Ortholog anotherOrtholog = (Ortholog) anotherObject;
            return (this.queryId.equals(anotherOrtholog.subjectId) && this.subjectId.equals(anotherOrtholog.queryId));
        }
    }

    @Override
    public int hashCode(){
        return this.queryId.hashCode()+this.subjectId.hashCode();
    }


}
