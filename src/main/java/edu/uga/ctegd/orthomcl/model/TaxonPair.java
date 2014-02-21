package edu.uga.ctegd.orthomcl.model;

/**
 * Created by mnural on 2/17/14.
 */
public class TaxonPair {
    private final String taxonA;
    private final String taxonB;

    public TaxonPair(String taxonA, String taxonB) {
        this.taxonA = taxonA;
        this.taxonB = taxonB;
    }

    public boolean equals(Object anotherObject){
        if(anotherObject.getClass() != getClass()){
            return false;
        }else{
            TaxonPair anotherPair = (TaxonPair) anotherObject;
            return this.taxonA.equals(anotherPair.taxonA) && this.taxonB.equals(anotherPair.taxonB)
                    || this.taxonA.equals(anotherPair.taxonB) && this.taxonB.equals(anotherPair.taxonA);
        }
    }

    public int hashCode(){
        return taxonA.hashCode() + taxonB.hashCode();
    }
}
