package edu.uga.ctegd.orthomcl;

import edu.uga.ctegd.orthomcl.model.HSP;
import edu.uga.ctegd.orthomcl.model.Ortholog;
import edu.uga.ctegd.orthomcl.model.QueryTaxonPair;
import edu.uga.ctegd.orthomcl.model.TaxonPair;

import java.io.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import edu.uga.ctegd.orthomcl.util.Scanner;

/**
 * Created by mnural on 2/16/14.
 */
public class OrthomclPairs {

    final boolean DEBUG;

    final static int EVALUE_CUTOFF_EXP = -5;
    final static double EVALUE_CUTOFF = 1e-5;
    final static double PERCENT_MATCH_THRESHOLD = 50.0;

    double minEvalue =1;

    /**
    * A Set of HashTables for HSP's separated by queryTaxonId and key'ed by queryId
    * */
    private HashMap<String,HashMap<String,List<HSP>>> interTaxonHits;
    private HashMap<QueryTaxonPair,Double> bestTaxonScore;

    private HashMap<String,List<HSP>> bestHits;
    private HashSet<Ortholog> orthologs;

    public OrthomclPairs(boolean debug){
        DEBUG = debug;
        interTaxonHits = new HashMap<String, HashMap<String, List<HSP>>>();
        bestTaxonScore = new HashMap<QueryTaxonPair,Double>();
        bestHits = new HashMap<String,List<HSP>>();
        orthologs = new HashSet<Ortholog>();
    }

    private void run(String sequencesFilePath) {
        int totalTime = 0;
        java.util.Scanner keyboard = null;
        if(DEBUG){
            keyboard= new java.util.Scanner(System.in);
        }
        long start = System.currentTimeMillis();

        Scanner input = null;
        try {
            input = new Scanner (new FileInputStream(sequencesFilePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while(input.hasNext()){
            String queryId = input.next();
            String subjectId = input.next();
            String queryTaxonId = input.next();
            String subjectTaxonId = input.next();
            double evalue = input.nextDouble();
//            double percentIdentity = input.nextDouble();
            double percentMatch = input.nextDouble();
            String evalueMant = input.next();
            String evalueExp = input.next();
            evalue = Double.parseDouble(evalueMant+"e"+evalueExp);
            if(evalue < minEvalue && evalue > 0.0){
                minEvalue = evalue;
            }

            //This is an intra-taxon match
            if(queryTaxonId.equals(subjectTaxonId)){

            //This is an inter-taxon match
            }else{
                if (interTaxonHits.get(queryTaxonId) == null){
                    interTaxonHits.put(queryTaxonId, new HashMap<String, List<HSP>>());
                }

                List queryList = interTaxonHits.get(queryTaxonId).get(queryId);
                if (queryList == null){
                    queryList = new ArrayList();
                    interTaxonHits.get(queryTaxonId).put(queryId,queryList);
                }
                queryList.add(new HSP(queryId,subjectId,queryTaxonId,subjectTaxonId,evalue,percentMatch,evalueMant,evalueExp));


                QueryTaxonPair pair = new QueryTaxonPair(queryId,subjectTaxonId);
                if(bestTaxonScore.get(pair) != null ){
                    if (evalue < bestTaxonScore.get(pair) ){
                        bestTaxonScore.put(pair,evalue);
                    }
                }else{
                    bestTaxonScore.put(pair,evalue);
                }
            }
            input.nextLine();
        }

        minEvalue = minEvalue / 10;
        input.close();
        input = null;

        totalTime +=System.currentTimeMillis() - start;
        System.out.println("Loading the file and creating interTaxonHits table took:" + (System.currentTimeMillis() - start)) ;
        if(DEBUG){
            System.out.print("Hit enter to continue");
            keyboard.nextLine();
        }
        start = System.currentTimeMillis();

        /*
        * IDENTIFY BEST-HITS AND ADD TO THE bestHits TABLE
        * */
        for (String taxon : interTaxonHits.keySet()){
            for (String queryId : interTaxonHits.get(taxon).keySet()){
                for(HSP match  : interTaxonHits.get(taxon).get(queryId)){
                    double bestScore = bestTaxonScore.get(new QueryTaxonPair(queryId,match.getSubjectTaxonId()));
                    if (match.getEvalue() <= bestScore && (Integer.parseInt(match.getEvalueExp()) == EVALUE_CUTOFF_EXP || match.getEvalue() <= EVALUE_CUTOFF) && match.getPercentMatch() >= PERCENT_MATCH_THRESHOLD) { // QoH(Ax,By)
                        List queryMatches = bestHits.get(queryId);
                        if(queryMatches == null){
                            queryMatches = new ArrayList<HSP>();
                            bestHits.put(queryId, queryMatches);
                        }
                        queryMatches.add(match);
                    }
                }
            }
        }

        interTaxonHits = null;
        System.gc();

        totalTime +=System.currentTimeMillis() - start;
        System.out.println("Creating best hits table took:" + (System.currentTimeMillis() - start)) ;
        if(DEBUG){
            System.out.print("Hit enter to continue");
            keyboard.next();
        }
        start = System.currentTimeMillis();

        /*
         * FIND ORTHOLOGS
         */
        HashMap<TaxonPair, Double[]> orthologAvgScores = new HashMap<TaxonPair,Double[]>();
        for(List<HSP> hits : bestHits.values()){
            String queryId = hits.get(0).getQueryId();
            for(HSP singleBestHit : hits){
                List<HSP> subjectBestHits = bestHits.get(singleBestHit.getSubjectId());
                if (subjectBestHits == null) {
                    break;
                }
                for (HSP subjectSingleBestHit : subjectBestHits){
                    if(subjectSingleBestHit.getSubjectId().equals(singleBestHit.getQueryId())){
                        //Compute unnormalized score for this ortholog pair
                        double evalue1 = singleBestHit.getEvalue();
                        evalue1 = evalue1 == 0.0 ? minEvalue : evalue1;
                        double evalue2 = subjectSingleBestHit.getEvalue();
                        evalue2 = evalue2 == 0.0 ? minEvalue : evalue2;

                        double unnormalizedScore = (-Math.log10(evalue1) + -Math.log10(evalue2)) / 2;

                        //Update SUM and COUNT for the taxonPair
                        TaxonPair taxonPair = new TaxonPair(singleBestHit.getQueryTaxonId(),singleBestHit.getSubjectTaxonId());
                        //Add ortholog to the orthologs
                        Ortholog ortholog = new Ortholog(queryId,singleBestHit.getSubjectId(),taxonPair,unnormalizedScore);

                        if(orthologs.add(ortholog)){
                            Double[] taxonScore = orthologAvgScores.get(taxonPair);
                            if (taxonScore == null) {
                                taxonScore = new Double[]{0.0,0.0};
                                orthologAvgScores.put(taxonPair, taxonScore);
                            }
                            taxonScore[0] += 1;
                            taxonScore[1] += unnormalizedScore;
                        }
                        break;
                    }
                }
            }
        }

        totalTime +=System.currentTimeMillis() - start;
        System.out.println("Finding ortholog AVG Scores Took:" + (System.currentTimeMillis() - start)) ;
        if(DEBUG){
            System.out.print("Hit enter to continue");
            keyboard.next();
        }
        start = System.currentTimeMillis();


        //Calculate the Average taxon scores
        for(Double[] taxonScore :orthologAvgScores.values()){
            if(taxonScore[0] != 0){
                taxonScore[1] = taxonScore[1] / taxonScore[0];
            }
        }

        //Finally Normalize Ortholog scores
        for(Ortholog ortholog : orthologs){
            ortholog.score = ortholog.score / orthologAvgScores.get(ortholog.taxonPair)[1];
        }


        //compareBestTaxon();
        //compareBestHit();
        totalTime +=System.currentTimeMillis() - start;
        System.out.println("Ortholog took:" + (System.currentTimeMillis() - start)) ;
        System.out.println("**\nTotal Time :"+ totalTime);
        if(DEBUG){
            System.out.print("Hit enter to quit");
            keyboard.next();
        }

        //System.out.println("Ortholog Test Passed?:" + compareOrthologs());


    }

    private boolean compareOrthologs() {
        int localTotalCount =orthologs.size();
        int dbTotalCount =0;
        int dbHit = 0;

        try{
                Class.forName("com.mysql.jdbc.Driver");
                Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/orthomcl", "orthomcl", "orthomcl");

                String sql2 = "SELECT COUNT(*) FROM Ortholog";
                Statement stmt2 = conn.createStatement();
                ResultSet rs2 = stmt2.executeQuery(sql2);
                if (rs2.next()){
                    dbTotalCount = rs2.getInt(1);
                    if(dbTotalCount != localTotalCount){
                        return false;
                    }
                }else{
                    return false;
                }


                String sql = "SELECT COUNT(*) FROM Ortholog WHERE NORMALIZED_SCORE=? AND (SEQUENCE_ID_A=? AND SEQUENCE_ID_B=? OR SEQUENCE_ID_B=? AND SEQUENCE_ID_A=?) ";
                PreparedStatement stmt = conn.prepareStatement(sql);

                for(Ortholog local : orthologs){
                    stmt.setDouble(1,local.score);
                    stmt.setString(2,local.queryId);
                    stmt.setString(3,local.subjectId);
                    stmt.setString(4,local.queryId);
                    stmt.setString(5,local.subjectId);

                    ResultSet rs = stmt.executeQuery();
                        if (rs.next()){
                            dbHit ++;
                        }

                }
                if (dbHit == dbTotalCount){
                    return true;
                }else{
                    System.out.println("Missed "+dbHit+" items.");
                    return false;
                }

        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        return false;
    }

    private boolean compareBestHit() {
        int totalCount = 0;
        int uniqueCount = bestHits.size();
        for(String queryId : bestHits.keySet()){
            List<HSP> matches = bestHits.get(queryId);
            totalCount += matches.size();
            //System.out.println(queryId + "\t" + match.getSubjectId());
        }

        System.out.println("Total count of best hits:"+totalCount);
        System.out.println("Total number of unique queries:" + uniqueCount);

        try{
            int dbTotalCount = 0;
            int dbUniqueCount = 0;
            int missingTotalCount = 0;
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/orthomcl", "orthomcl", "orthomcl");
            String sql = "SELECT DISTINCT query_id FROM BestHit";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            List<String> missingQueryIds = new ArrayList<String>();

            while(rs.next()){
                dbUniqueCount ++;
                String queryId = rs.getString("query_id");
                if (bestHits.get(queryId) == null){
                    String sql2 = "SELECT COUNT(subject_id) FROM BestHit WHERE query_id='"+queryId+"'";
                    Statement stmt2 = conn.createStatement();
                    ResultSet rs2 = stmt2.executeQuery(sql2);
                    if (rs2.next()){
                        missingTotalCount += rs2.getInt(1);
                    }
                    missingQueryIds.add(queryId);
                }
            }
         /*   for (String queryId : bestHits.keySet()){
                for (HSP subject : bestHits.get(queryId)){
                    String sql = "SELECT * FROM BestHit WHERE query_id=? AND subject_id=?";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1,queryId);
                    stmt.setString(2,subject.getSubjectId());
                    ResultSet rs = stmt.executeQuery();
                    if (!rs.next()){
                        System.out.println(queryId +"\t" + subject);
                    }
                }
            }*/
            System.out.println("Missing elements:"+ missingQueryIds.size());
            System.out.println("Total size from the missing query_id's:" + missingTotalCount);
            System.out.println(missingQueryIds);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    private boolean compareBestTaxon()  {

        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/orthomcl", "orthomcl", "orthomcl");
            String sql = "SELECT * FROM BestQueryTaxonScore";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            int differentCount = 0;
            while(rs.next()){

                double localScore = bestTaxonScore.get(new QueryTaxonPair(rs.getString("query_id"),rs.getString("subject_taxon_id")));
                String evalueMant = "";
                DecimalFormat df = new DecimalFormat(".00");
                evalueMant = df.format(rs.getDouble("evalue_mant"));
                String evalueExp = rs.getString("evalue_exp");
                double dbScore = Double.parseDouble(evalueMant+ "e" + evalueExp);
                if (localScore != dbScore){
                    System.out.println(rs.getString("query_id") + " " +rs.getString("subject_taxon_id"));
                    differentCount++;
                }

            }
            System.out.println("Different Count:"+differentCount);

            conn.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        String seqFile;
        boolean debug = false;
        if (args.length == 2) {
            seqFile = args[0];
            if (args[1].equalsIgnoreCase("true")) debug = true;
        }else if (args.length == 1){
            seqFile = args[0];
        }else{
            seqFile = "similarSequencesModified18K.txt";
        }
        new OrthomclPairs(debug).run(seqFile);
        System.out.println("Processing Done. Exiting....");

    }


}
