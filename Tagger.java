package NLP_hsw268.Tagger_Training;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class Tagger {
    
	private static final int String = 0;
	private static final int HashMap = 0;
	static HashMap<String, Integer> label_Count = new HashMap<String, Integer>();
	static HashMap<String, Integer> word_Count = new HashMap<String, Integer>();
	static HashMap<String, HashMap<String, Integer>> label_label_Count = new HashMap<String, HashMap<String, Integer>>();
	static HashMap<String, HashMap<String, Integer>> word_label_Count = new HashMap<String, HashMap<String, Integer>>();

	static HashMap<String, HashMap<String, Double>> emmission_probs = new HashMap<String, HashMap<String, Double>>();
	static HashMap<String, HashMap<String, Double>> transition_probs = new HashMap<String, HashMap<String, Double>>();
	private static java.util.HashMap<java.lang.String, Integer> transmision_probs;
	
	@SuppressWarnings("unlikely-arg-type")
	public ArrayList<String> viterbi(String line, String[] labels)
	{
		ArrayList<String> predictedLabels = new ArrayList<String>();
		String predicted_label = "";
		String[] array = line.split("\t");
		
		for(String s:array)
		{
			System.out.print(s + " ");
		}
		System.out.println();
		
		double[][] dp = new double[label_Count.size()][array.length];
		
		double first_max_prob = Double.MIN_VALUE;
		for(int i=0;i<label_Count.size();i++)
		{
			String first_Token = array[0];
			String initial_label = labels[i];
			
			double initial_label_prob=0;
			HashMap<String, Double> m = transition_probs.get("Start");
				
			if(m.containsKey(labels[i]))
			{
			  initial_label_prob = m.get(labels[i]);// prob of start to NNp
			}
			
			if(emmission_probs.containsKey(first_Token))	
			{
			HashMap<String, Double> map = emmission_probs.get(first_Token);		
			
			if(map.containsKey(initial_label))
			{
			double prob_token_label = map.get(initial_label);	
			dp[i][0] = (initial_label_prob * prob_token_label);
			}
			}
			else
			{
				dp[i][0] = 0.0;
			}
			
			if(dp[i][0]>first_max_prob)
			{
				first_max_prob = dp[i][0];
				predicted_label = labels[i];
			}
		}
		
		predictedLabels.add(predicted_label);
		
		for(int j=1;j<array.length;j++)
		{
			
			for(int i=0;i<label_Count.size();i++)
			{
				double maxValue = Double.MIN_VALUE;
				maxValue = dp[i][j];
				for(int l=0;l<label_Count.size();l++)
				{
					
					HashMap<String, Double> tcheck = transition_probs.get(labels[l]);
					HashMap<String, Double> echeck = emmission_probs.getOrDefault(array[j], new HashMap<String, Double>());
					
					if(tcheck.containsKey(labels[i]) && echeck.containsKey(labels[i]))
					{
					double compare = dp[l][j-1] * tcheck.get(labels[i]) * 
							echeck.get(labels[i]);
					if(compare>maxValue)
					{
						maxValue = compare;
					}
					}
				}
				dp[i][j] = maxValue;
				
			}
			
			double max_prob = Double.MIN_VALUE;
			for(int c=0;c<dp.length;c++)
			{
				if(dp[c][j]>max_prob)
				{
					max_prob = dp[c][j];
					predicted_label = labels[c];
				}
			}
			
			predictedLabels.add(predicted_label);
		}
		return predictedLabels;
	}
	
	
	public void getCounts(File file) throws IOException
	{
		List<String> input = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
		String prevLabel = "Start";
		Scanner sc = null;
		try
		{
			sc = new Scanner(file);
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
		
		int num_sentences = 0;
		for(String line : input)
		{
			
			line = line.trim();
			if(line.length()==0)
			{
				prevLabel = "Start";
				num_sentences++;
				continue;
			}
			String[] word_labels = line.split("\t");
			String word = word_labels[0];
			String label = word_labels[1];
			
			// Storing labels Counts
			if(label_Count.containsKey(label))
			{
				label_Count.put(label, label_Count.get(label)+1);
			}
			else
			{
				label_Count.put(label, 1);
			}
			
            // Storing words Counts
			if(word_Count.containsKey(word))
			{
				word_Count.put(word, word_Count.get(word)+1);
			}
			else
			{
				word_Count.put(word, 1);
			}
			
			
			// Storing the counts for each word mapping to all the states(labels)
		
			if(word_label_Count.containsKey(word))
			{
				HashMap<String, Integer> map = word_label_Count.get(word);
				if(map.containsKey(label))
				{
				map.put(label, map.get(label)+1);
				}
				else
				{
					map.put(label, 1);
				}
				word_label_Count.put(word, map);			
			}
			else
			{
				HashMap<String, Integer> map = new HashMap<String, Integer>();
				map.put(label, 1);
				word_label_Count.put(word, map);
			}
			
			
			// Storing the counts of going from one state to an another state(label)
			if(label_label_Count.containsKey(prevLabel))
			{
				HashMap<String, Integer> map = label_label_Count.get(prevLabel);
				if(map.containsKey(label))
				{
				map.put(label, map.get(label)+1);
				}
				else
				{
					map.put(label, 1);
				}
				label_label_Count.put(prevLabel, map);
			}
			else
			{
				HashMap<String, Integer> map = new HashMap<String, Integer>();
				map.put(label, 1);
				label_label_Count.put(prevLabel, map);
			}
		
			prevLabel = label;
		}
		
		label_Count.put("Start", num_sentences);
	}
	
	public void getProbs(HashMap<String, Integer> label_Count, HashMap<String, Integer> word_Count, HashMap<String, HashMap<String, Integer>> label_label_Count, HashMap<String, HashMap<String, Integer>> word_label_Count)
	{
		for(String From: label_label_Count.keySet())
		{
			HashMap<String, Integer> map = label_label_Count.get(From);
			HashMap<String, Double> insert = new HashMap<String, Double>();
			for(String key:map.keySet())
			{
				String To = key;
				int count = map.get(To); 
				int denom = label_Count.get(From);
				double prob = ((double)count/(double)denom);
				insert.put(To, prob);
			}
			transition_probs.put(From, insert);
		}
		
		for(String word: word_label_Count.keySet())
		{
			HashMap<String, Integer> map = word_label_Count.get(word);
			HashMap<String, Double> insert = new HashMap<String, Double>();
			for(String label:map.keySet())
			{
				String state = label;
				int count = map.get(state); 
				int denom = label_Count.get(state);
				double prob = ((double)count/(double)denom);
				insert.put(state, prob);
			}
			emmission_probs.put(word, insert);
		}
			
		
	}
	
	@SuppressWarnings("static-access")
	public static void main(String[] args) throws IOException
	{
		Tagger tag = new Tagger();
		File file = new File("C:\\Users\\Admin\\Desktop\\NYU Courant(2nd sem)\\NLP\\NLP_A4\\WSJ_POS_CORPUS_FOR_STUDENTS\\WSJ_02-21.pos");
		tag.getCounts(file);
		tag.getProbs(label_Count, word_Count, label_label_Count, word_label_Count);
		
		String[] labels = new String[label_Count.size()];
		int i=0;
		for(String key:label_Count.keySet())
		{
			labels[i++] = key;
		}
		
		
		File test = new File("C:\\Users\\Admin\\Desktop\\NYU Courant(2nd sem)\\NLP\\NLP_A4\\WSJ_POS_CORPUS_FOR_STUDENTS\\WSJ_24.words");
		Scanner sc = null;
		
		try
		{
			sc = new Scanner(test);
		}
		catch(FileNotFoundException e)
		{
			e.getStackTrace();
		}
		

		
		FileWriter predictedLabels = new FileWriter("C:\\\\Users\\\\Admin\\\\Desktop\\\\NYU Courant(2nd sem)\\\\NLP\\\\NLP_A4\\\\predictedLabels.pos");
		while(sc.hasNextLine())
		{
			String line = "";
	
			String word = sc.nextLine();
			if(word.length()==0)
			{
				continue;
			}
			
			while(word.length()!=0)
			{
				
				line = line + word + "	";
				word = sc.nextLine();
				
			}
			
			
			ArrayList<String> predicted = tag.viterbi(line, labels);
			//System.out.println(predicted);
			String[] actual_words = line.split("\t");
			for(int p=0;p<predicted.size();p++)
			{
				predictedLabels.write(actual_words[p] + "\t" + predicted.get(p) + "\n");
				System.out.println(actual_words[p] + "\t" + predicted.get(p));
			}
			System.out.println();
			predictedLabels.write("\n");
		}
		predictedLabels.close();	
		
		}
				
	}

