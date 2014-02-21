package edu.uga.ctegd.orthomcl.model;

/**
 * Created by mnural on 2/16/14.
 */
public class QueryTaxonPair {

    String queryId;
    String subjectTaxonId;

    public QueryTaxonPair(String queryId, String subjectTaxonId) {
        this.queryId = queryId;
        this.subjectTaxonId = subjectTaxonId;
    }

    public String toString(){
        return queryId + " " + subjectTaxonId;
    }

    public boolean equals(Object anotherPair){
        if (anotherPair == null  || getClass() != anotherPair.getClass()){
            return false;
        }else{
            QueryTaxonPair pair2 = (QueryTaxonPair) anotherPair;
            boolean result = this.queryId.equals(pair2.queryId) && this.subjectTaxonId.equals(pair2.subjectTaxonId);
            return result;
        }
    }

    public int hashCode(){
        return new String(queryId + subjectTaxonId).hashCode();
    }

}
