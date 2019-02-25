package com.code.wordrank;

import java.util.*;
import java.util.StringTokenizer;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

public class WordRank {
	//Mapper
	public static class TextMapper extends Mapper<Object, Text, Text, IntWritable>
	{

		private final static IntWritable iw = new IntWritable(1);
		private Text word = new Text();

		String[] wordsearch = new String[] {"education", "politics", "sports", "agriculture"};	//words to search

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException 
		{
			StringTokenizer token = new StringTokenizer(value.toString());
			String filepath = ((FileSplit) context.getInputSplit()).getPath().toString();

			while (token.hasMoreTokens()) 
			{
				word.set(token.nextToken().toLowerCase());

				for(int i = 0; i < wordsearch.length; i++) 
				{
					while(word.toString().contains(wordsearch[i])) 
					{
						context.write(new Text(filepath + "/" + wordsearch[i]), iw);
						word.set(word.toString().replaceFirst(wordsearch[i], ""));
					}
				}
			}
		}
	}

	//Reducer
	public static class TextReducer extends Reducer<Text,IntWritable,Text,IntWritable> 
	{
		private IntWritable res = new IntWritable();

		public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException 
		{
			int total = 0;
			for (IntWritable val : values) 
			{
				total += val.get();
			}
			res.set(total);
			context.write(key, res);
		}
	}

	//Mapper
	public static class ChangeMapper extends Mapper<Object, Text, Text, Text>
	{
		String[] search = new String[] {"education", "politics", "sports", "agriculture"};

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException 
		{
			String[] values = new String[] {"", ""};
			int counter = 0;
			StringTokenizer token = new StringTokenizer(value.toString());
			
			while (token.hasMoreTokens()) 
			{
				values[counter] = token.nextToken();
				counter += 1;
			}
			
			counter = 0;
			int lastIndex = values[0].lastIndexOf("/");
			String line = values[0].substring(lastIndex + 1, values[0].length());
			String state = values[0].substring(0, lastIndex);

			StringBuilder sb = new StringBuilder();
			sb.append(line).append("-").append(values[1]);
			context.write(new Text(state), new Text(sb.toString()));
		}
	}

	//Reducer
	public static class ChangeReducer extends Reducer<Text,Text,Text,Text> 
	{
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException 
		{
			StringBuilder sb = new StringBuilder();

			for (Text val : values) 
			{
				sb.append(val.toString()).append(",");
			}

			context.write( key, new Text( sb.toString() ) );
		}
	}

	//Mapper
	public static class SortMapper extends Mapper<Object, Text, Text, IntWritable>
	{
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException 
		{
			String[] s = value.toString().split("\t");
			String list = s[1];
			String[] rank = list.split(",");

			Map<Integer, String> map = new HashMap<Integer, String>();

			for(int i = 0; i < rank.length; i++) 
			{
				String[] curr = rank[i].split("-");
				map.put( Integer.parseInt(curr[1] ) , curr[0] );
			}

			StringBuilder result = new StringBuilder();
			Map<Integer, String> treeMap = new TreeMap<Integer, String>(map);

			for(Integer val : treeMap.keySet()) 
			{
				result.append( map.get( val ) ).append(",");
				map.remove( val );
			}
			context.write(new Text(result.toString() ), new IntWritable(1));
		}
	}

	//Reducer
	public static class SortReducer extends Reducer<Text, IntWritable, Text, IntWritable>
	{
		public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException 
		{
			int sum = 0;
			for(IntWritable val : values) {
				sum += val.get();
			}
			context.write( key , new IntWritable(sum));
		}
	}

	public static void main(String[] args) throws Exception 
	{
		Configuration con = new Configuration();
		Job job1 = Job.getInstance(con, "Word Count Sum");
		job1.setJarByClass(WordRank.class);	//jar

		job1.setMapperClass(TextMapper.class);
		job1.setCombinerClass(TextReducer.class);
		job1.setReducerClass(TextReducer.class);

		job1.setOutputKeyClass(Text.class);
		job1.setOutputValueClass(IntWritable.class);

		FileInputFormat.addInputPath(job1, new Path(args[0]));
		FileOutputFormat.setOutputPath(job1, new Path( "/program/wordcount"));

		job1.waitForCompletion(true);
		

		Job job2 = Job.getInstance(con, "Change Value");
		job2.setJarByClass(WordRank.class);	//jar

		job2.setMapperClass(ChangeMapper.class);
		job2.setCombinerClass(ChangeReducer.class);
		job2.setReducerClass(ChangeReducer.class);

		job2.setOutputKeyClass(Text.class);
		job2.setOutputValueClass(Text.class);

		FileInputFormat.addInputPath(job2, new Path("/program/wordcount"));
		FileOutputFormat.setOutputPath(job2, new Path("/program/modifiedval"));

		job2.waitForCompletion(true);


		Job job3 = Job.getInstance(con, "Word Sorting");
		job3.setJarByClass(WordRank.class);	//jar

		job3.setMapperClass(SortMapper.class);
		job3.setCombinerClass(SortReducer.class);
		job3.setReducerClass(SortReducer.class);

		job3.setOutputKeyClass(Text.class);
		job3.setOutputValueClass(IntWritable.class);

		FileInputFormat.addInputPath(job3, new Path("/program/modifiedval"));
		FileOutputFormat.setOutputPath(job3, new Path(args[1]));

		System.exit(job3.waitForCompletion(true) ? 0 : 1);
	}
}