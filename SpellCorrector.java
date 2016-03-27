import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class SpellCorrector {
    final private CorpusReader cr;
    final private ConfusionMatrixReader cmr;
    final private double lambda;
    final private double NO_ERROR;
    final private double LIKELY;
    
    final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz'".toCharArray();
    
    
    public SpellCorrector(CorpusReader cr, ConfusionMatrixReader cmr) 
    {
        this.cr = cr;
        this.cmr = cmr;
        lambda = 5;
        NO_ERROR = -13.125;
        LIKELY = -12.25;
    }
    
    public String correctPhrase(String phrase)
    {
        if(phrase == null || phrase.length() == 0)
        {
            throw new IllegalArgumentException("phrase must be non-empty.");
        }
            
        String[] words = phrase.split(" ");
        String finalSuggestion = "";
        HashMap<Integer,Double> p = new HashMap();
        for(int i = 0; i < words.length; i ++)
        {
            if(!cr.inVocabulary(words[i]))
                p.put(i, -50.0);
           
        }
        for(int i = 0; i < words.length; i ++)
        {
            double prob1, prob2;
            if(i == 0)
            {
                prob1 = cr.getSmoothedCount("SoS"+" "+words[i]);
                prob2 = cr.getSmoothedCount(words[i]+" "+words[i+1]);
                
            }
            else if(i == words.length-1 )
            {
                prob1 = cr.getSmoothedCount(words[i-1]+" "+words[i]);
                prob2 = cr.getSmoothedCount(words[i]+" "+"EoS");
                
            }
            else
            {
                prob1 = cr.getSmoothedCount(words[i-1]+" "+words[i]);
                prob2 = cr.getSmoothedCount(words[i]+" "+words[i+1]);
                //System.out.println(words[i-1]+" "+words[i] + " "+ prob);
                
            }
            if(!p.containsKey(i) && (prob1 < NO_ERROR && !p.keySet().contains(i-1)) && ( prob2 < NO_ERROR && 
                        !p.keySet().contains(i+1)))
            {
                if(prob1 < prob2)
                    p.put(i,prob1);
                else
                    p.put(i,prob2);
            }
                    
                
        }
        double min1 = 0, min2 = 0;
        int[] min_index = new int[2];
        if(p.size() > 2)
        {
            
            for(int i :p.keySet())
            {
                //System.out.println("    "+words[i]+ " "+ p.get(i));
                double val = p.get(i);
                if(val < min2 && val>= min1)
                {
                    min2 = val;
                    min_index[1] = i;
                }
                else if(val < min1)
                {
                    min2 = min1;
                    min_index[1]= min_index[0];
                    min1 = val;
                    min_index[0] = i;
                }
            }
            p.clear();
            p.put(min_index[0], min1);
            p.put(min_index[1], min2);
            
        }
            
        //System.out.println();
        for(int i : p.keySet())
        {
            //System.out.println("    "+words[i]);
            Map<String, Double> possibilities = this.getCandidateWords(words[i]);
            
            
            double maxP = -200;
            String best_candidate = words[i];
            for(String s : possibilities.keySet())
            {
                //System.out.println("        "+s);
                double P1, P2, totP;
                double P3 = possibilities.get(s);
                
                if(i > 0 && i < words.length-1)
                {
                    
                    P1 = cr.getSmoothedCount(words[i-1]+" "+s);
                    P2 = cr.getSmoothedCount(s+" "+words[i+1]);
                    
                    
                }
                else if(i == 0)
                {
                    P1 = cr.getSmoothedCount("SoS "+s);
                    P2 = cr.getSmoothedCount(s+" "+words[i+1]);
                }
                else
                {
                    P1 = cr.getSmoothedCount(words[i-1]+" "+s);
                    P2 = cr.getSmoothedCount(s+" EoS");
                } 
                //System.out.println("        "+s+" "+P1+" "+P2+" "+P3);
                if(P1 > LIKELY )
                    P1 = 0;
                    
                if(P2 > LIKELY)
                    P2 = 0;
                
                totP = 4*P1+4*P2+P3;
                
                               
                
                
                if(totP > maxP)
                {
                    best_candidate = s;
                    maxP = totP;
                }
                
                
            }
            words[i] = best_candidate;
            
        }
        
        for(String s : words)
            finalSuggestion = finalSuggestion + s + " ";
        
        return finalSuggestion.trim();
    }    
      
    /** returns a map with candidate words and their noisy channel probability. **/
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public Map<String,Double> getCandidateWords(String word)
    {
        Map<String,Double> mapOfWords = new HashMap<>();
        
                
        // insertions
        for(char a : ALPHABET)
        {
            String new_word = a+word;
                
            if(cr.inVocabulary(new_word))
            {
                double nbErr = (double)cmr.getConfusionCount("1", String.valueOf(a));
                if(nbErr == 0 )
                {
                    nbErr = 0.001;
                }
                double pErr = (Math.log(nbErr)-Math.log((double)cr.countFirstChar(a)))
                        +lambda * (cr.getSmoothedCount(new_word));


                mapOfWords.put(new_word, pErr);
            }
        }
        for(int i = 0; i < word.length(); i++)
        {
            char wi = word.charAt(i);
            char wim1 = ' ';

            if(i>0)
                wim1 = word.charAt(i-1);
            
                
            for(char a: ALPHABET)
            {
                //insertion
                String new_word = word.substring(0,i+1)+a
                        +word.substring(i+1, word.length());
                
                if(cr.inVocabulary(new_word))
                {
                    double nbErr = (double)cmr.getConfusionCount(String.valueOf(wi),String.valueOf(wi)+String.valueOf(a));
                    if(nbErr == 0 )
                    {
                        nbErr = 0.001;
                    }
                    double pErr = (Math.log(nbErr)-Math.log((double)cr.countChar(wi,a)))
                            +lambda * (cr.getSmoothedCount(new_word));
                            

                    mapOfWords.put(new_word, pErr);
                }
                
                
                //substition
                char[] word_tab = word.toCharArray();
                word_tab[i] = a;
                if(cr.inVocabulary(new String(word_tab)))
                {
                    double nbErr = (double)cmr.getConfusionCount(String.valueOf(wi), String.valueOf(a));
                    if(nbErr == 0 )
                    {
                        nbErr = 0.001;
                    }
                    double pErr = (Math.log(nbErr)-Math.log((double)cr.countChar(a)))
                            +lambda * ( cr.getSmoothedCount(new String(word_tab)));
                            
                   
                    mapOfWords.put(new String(word_tab), pErr);
                }
                
               
                
            }
            //deletion
            String del_word = word.substring(0, i)
                    +word.substring(i+1, word.length());

            if(cr.inVocabulary(del_word))
            {
                double nbErr = (double)cmr.getConfusionCount( String.valueOf(wim1)+String.valueOf(wi),String.valueOf(wim1));
                if(wim1 == ' ')
                    nbErr = (double)cmr.getConfusionCount( "1"+String.valueOf(wi),"1");
                if(nbErr == 0 )
                {
                    nbErr = 0.001;
                }
                
                double pErr = (Math.log(nbErr)-Math.log((double)cr.countChar(wim1)))
                        +lambda*(cr.getSmoothedCount(del_word));
                
                if(wim1 == ' ')      
                     pErr = (Math.log(nbErr)-Math.log((double)cr.getVocabularySize()))
                        +lambda*(cr.getSmoothedCount(del_word));
                mapOfWords.put(del_word, pErr);
            }
                
            //transposition
            char[] trans = word.toCharArray();
            trans[i] = wim1;
            if(i>0)
                trans[i-1] = wi;
            String trans_word = new String(trans);
            if(cr.inVocabulary(trans_word))
            {
                double nbErr = (double)cmr.getConfusionCount(String.valueOf(wim1)+String.valueOf(wi),
                        String.valueOf(wi)+String.valueOf(wim1) );
                if(nbErr == 0 )
                {
                    nbErr = 0.001;
                }
                double pErr = (Math.log(nbErr)-Math.log((double)cr.countChar(wi,wim1)))
                        +lambda*cr.getSmoothedCount(trans_word);
                mapOfWords.put(trans_word, pErr);
            }
           
        }
        
        return mapOfWords;
    }            
}