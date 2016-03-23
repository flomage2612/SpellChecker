import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class CorpusReader 
{
    final static String CNTFILE_LOC = "samplecnt.txt";
    final static String VOCFILE_LOC = "samplevoc.txt";
    
    private HashMap<String,Integer> ngrams;
    private Set<String> vocabulary;
    private int nbWords;
    private int k;
    private int l;
        
    public CorpusReader() throws IOException
    {  
        nbWords = 0;
        readNGrams();
        readVocabulary();
        l = 5;
        k = 5;
    }
    
    /**
     * Returns the n-gram count of <NGram> in the file
     * 
     * 
     * @param nGram : space-separated list of words, e.g. "adopted by him"
     * @return 0 if <NGram> cannot be found, 
     * otherwise count of <NGram> in file
     */
     public int getNGramCount(String nGram) throws  NumberFormatException
    {
        if(nGram == null || nGram.length() == 0)
        {
            throw new IllegalArgumentException("NGram must be non-empty.");
        }
        Integer value = ngrams.get(nGram);
        return value==null?0:value;
    }
    
    private void readNGrams() throws 
            FileNotFoundException, IOException, NumberFormatException
    {
        ngrams = new HashMap<>();

        FileInputStream fis;
        fis = new FileInputStream(CNTFILE_LOC);
        BufferedReader in = new BufferedReader(new InputStreamReader(fis));

        while (in.ready()) {
            String phrase = in.readLine().trim();
            String s1, s2;
            int j = phrase.indexOf(" ");

            s1 = phrase.substring(0, j);
            s2 = phrase.substring(j + 1, phrase.length());

            int count = 0;
            try {
                count = Integer.parseInt(s1);
                if(!s2.contains(" "))
                    nbWords += count;
                ngrams.put(s2, count);
            } catch (NumberFormatException nfe) {
                throw new NumberFormatException("NumberformatError: " + s1);
            }
        }
    }
    
    
    private void readVocabulary() throws FileNotFoundException, IOException {
        vocabulary = new HashSet<>();
        
        FileInputStream fis = new FileInputStream(VOCFILE_LOC);
        BufferedReader in = new BufferedReader(new InputStreamReader(fis));
        
        while(in.ready())
        {
            String line = in.readLine();
            vocabulary.add(line);
        }
    }
    
    /**
     * Returns the size of the number of unique words in the dataset
     * 
     * @return the size of the number of unique words in the dataset
     */
    public int getVocabularySize() 
    {
        return vocabulary.size();
    }
    
    /**
     * Returns the subset of words in set that are in the vocabulary
     * 
     * @param set
     * @return 
     */
    public HashSet<String> inVocabulary(Set<String> set) 
    {
        HashSet<String> h = new HashSet<>(set);
        h.retainAll(vocabulary);
        return h;
    }
    
    public boolean inVocabulary(String word) 
    {
       return vocabulary.contains(word);
    }    
    
    public double getSmoothedCount(String NGram)
    {
        if(NGram == null || NGram.length() == 0)
        {
            throw new IllegalArgumentException("NGram must be non-empty.");
        }
        
        double smoothedCount = 0.0;
        
        if(!NGram.contains(" "))
        {
            // add-k smoothing for unigram
            return -Math.log((this.getNGramCount(NGram) + k))+Math.log(nbWords+k*this.getVocabularySize());
        }
        
        String[] words = NGram.split(" ");
        if(words.length != 2) System.err.println("only bigrams are handled");
        
        smoothedCount = -Math.log(this.getNGramCount(NGram) + l * this.getSmoothedCount(words[1]))
                +Math.log(this.getNGramCount(words[0])+l);
        
        
        return smoothedCount;        
    }
    
    public int countChar(char x, char y)
    {
        int result = 0;
        String xy = new String(new char[]{x,y});
        for(String p : vocabulary)
        {   
            if(p.contains(xy))
                result += this.getNGramCount(p);
        }
        
        return result;
    }

    public int countChar(char x)
    {
        int result = 0;
        String xy = String.valueOf(x);
        for(String p : vocabulary)
        {   
            if(p.contains(xy))
                result += this.getNGramCount(p);
        }
        
        return result;
    }
    
    public int setSize()
    {
        return nbWords;
    }
    
}
