import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class SpellCorrector {
    final private CorpusReader cr;
    final private ConfusionMatrixReader cmr;
    final private int lambda;
    final private double NO_ERROR;
    
    final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz'".toCharArray();
    
    
    public SpellCorrector(CorpusReader cr, ConfusionMatrixReader cmr) 
    {
        this.cr = cr;
        this.cmr = cmr;
        lambda = 3;
        NO_ERROR = -13;
    }
    
    public String correctPhrase(String phrase)
    {
        if(phrase == null || phrase.length() == 0)
        {
            throw new IllegalArgumentException("phrase must be non-empty.");
        }
            
        String[] words = phrase.split(" ");
        String finalSuggestion = "";
        ArrayList<Integer> p = new ArrayList();
        for(int i = 0; i < words.length; i ++)
        {
            if(!cr.inVocabulary(words[i]))
                p.add(i);
            else if (i == 0)
            {
                double prob = cr.getSmoothedCount(words[i]);
                if(prob < -15.0)
                    p.add(i);
            }
        }
        for(int i = 0; i < words.length; i ++)
        {
            if(i == 0)
            {
                double prob2 = cr.getSmoothedCount(words[i]+" "+words[i+1]);
                if(prob2 < NO_ERROR && !p.contains(i+1))
                    p.add(i);
            }
            else if(i == words.length-1 )
            {
                double prob1 = cr.getSmoothedCount(words[i-1]+" "+words[i]);
                if((prob1 < NO_ERROR && !p.contains(i-1)))
                    p.add(i);
            }
            else
            {
                double prob1 = cr.getSmoothedCount(words[i-1]+" "+words[i]);
                double prob2 = cr.getSmoothedCount(words[i]+" "+words[i+1]);
                //System.out.println(words[i-1]+" "+words[i] + " "+ prob);
                if((prob1 < NO_ERROR && !p.contains(i-1)) || ( prob2 < NO_ERROR && 
                        !p.contains(i+1)))
                    p.add(i);
            }
                
        }
        
        if(p.size() > 2)
            System.out.println("No error constant is not small enough");
        
        for(int i : p)
        {

            Map<String, Double> possibilities = this.getCandidateWords(words[i]);
            
            
            double maxP = -200;
            //double pErr1 = cr.getSmoothedCount(words[i-1]+" "+words[i]);
            //double pErr2 = cr.getSmoothedCount(words[i]+" "+words[i+1]);
            for(String s : possibilities.keySet())
            {
                double P1, P2, totP;
                double P3 = possibilities.get(s);
                
                if(i > 0 && i < words.length-1)
                {
                    
                    P1 = cr.getSmoothedCount(words[i-1]+" "+s);
                    P2 = cr.getSmoothedCount(s+" "+words[i+1]);
                    
                    if(P1 > NO_ERROR && P2 > NO_ERROR)
                    {
                        P1 *= 3;
                        P2 *= 3;
                    }
                      
                    //System.out.println(s+" "+P1+" "+P2+" "+P3);
                    totP = 0.5*P1+0.5*P2+0.25*P3;
                }
                else if(i == 0)
                {
                    P2 = cr.getSmoothedCount(s+" "+words[i+1]);
                    totP = 0.5*P2 + 0.5*P3;
                }
                else
                {
                    P1 = cr.getSmoothedCount(words[i-1]+" "+s);
                    totP = 0.5*P1 + 0.5*P3;
                } 
                
                if(totP > maxP)
                {
                    words[i] = s;
                    maxP = totP;
                }
                
                
            }
            
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
                    double nbErr = (double)cmr.getConfusionCount(String.valueOf(wi)+String.valueOf(a), String.valueOf(wi));
                    if(nbErr == 0 )
                    {
                        nbErr = 1;
                    }
                    double pErr = (Math.log(nbErr)-Math.log((double)cr.countChar(wi)))
                            +lambda * (cr.getSmoothedCount(new_word));
                            

                    mapOfWords.put(new_word, pErr);
                }
                
                
                //substition
                char[] word_tab = word.toCharArray();
                word_tab[i] = a;
                if(cr.inVocabulary(new String(word_tab)))
                {
                    double nbErr = (double)cmr.getConfusionCount(String.valueOf(a), String.valueOf(wi));
                    if(nbErr == 0 )
                    {
                        nbErr = 1;
                    }
                    double pErr = (Math.log(nbErr)-Math.log((double)cr.countChar(wi)))
                            +lambda * ( cr.getSmoothedCount(new String(word_tab)));
                            
                   
                    mapOfWords.put(new String(word_tab), pErr);
                }
                
               
                
            }
            //deletion
            String del_word = word.substring(0, i)
                    +word.substring(i+1, word.length());

            if(cr.inVocabulary(del_word))
            {
                double nbErr = (double)cmr.getConfusionCount(String.valueOf(wim1), String.valueOf(wim1)+String.valueOf(wi));
                if(wim1 == ' ')
                    nbErr = (double)cmr.getConfusionCount("1", String.valueOf(wi));
                if(nbErr == 0 )
                {
                    nbErr = 1;
                }
                
                double pErr = (Math.log(nbErr)-Math.log((double)cr.countChar(wim1,wi)))
                        +lambda*(cr.getSmoothedCount(del_word));
                if(wim1 == ' ')      
                     pErr = (Math.log(nbErr)-Math.log((double)cr.countChar(wi)))
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
                double nbErr = (double)cmr.getConfusionCount(String.valueOf(wi)
                        +String.valueOf(wim1), String.valueOf(wim1)+String.valueOf(wi));
                if(nbErr == 0 )
                {
                    nbErr = 1;
                }
                double pErr = (Math.log(nbErr)-Math.log((double)cr.countChar(wim1,wi)))
                        +lambda*cr.getSmoothedCount(trans_word);
                mapOfWords.put(trans_word, pErr);
            }
           
        }
        
        return mapOfWords;
    }            
}