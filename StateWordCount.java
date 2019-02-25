package com.code.statewordcount;

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

public class StateWordCount {
	//Defining the Mapper method
	public static class TextMapper extends Mapper<Object, Text, Text, IntWritable>
	{
		private Text word = new Text();
		private final static IntWritable iw = new IntWritable(1);
		
		String[] wordsearch = new String[]{"education", "politics", "sports", "agriculture"};	//words which are to be searched

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

	//Defining Reducer Method
	public static class TextReducer extends Reducer<Text,IntWritable,Text,IntWritable> 
	{
		private IntWritable r = new IntWritable();
		
		public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException 
		{
			int sum = 0;
			
			for (IntWritable val : values) 
			{
				sum += val.get();
			}
			
			r.set(sum);
			context.write(key, r);
		}
	}

	// Defining the Mapper Method
	public static class ModifyMapper extends Mapper<Object, Text, Text, Text>
	{
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException 
		{
			String[] values = new String[] {"", ""};
			int counter = 0;

			StringTokenizer token = new StringTokenizer(value.toString());
			
			while (token.hasMoreTokens()) {
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

	//Defining Reducer Method
	public static class ModifyReducer extends Reducer<Text,Text,Text,Text> {

		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException
		{
			ArrayList<String> st = new ArrayList<String>();

			for (Text value : values) 
			{
				st.add(value.toString());
			}

			int count = -1;
			int index = 0;
			
			for(int i = 0; i < st.size(); i++) 
			{
				if(Integer.parseInt(st.get(i).split("-")[1]) > count) 
				{
					index = i;
					count = Integer.parseInt(st.get(i).split("-")[1]);
				}
			}
			context.write(new Text(st.get(index)), new Text(st.get(index)));
		}
	}

	//Mapper
	public static class TrimMapper extends Mapper<Object, Text, Text, IntWritable>
	{
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException 
		{
			String line = value.toString().trim().split("-")[0].trim();
			context.write(new Text(line), new IntWritable(1));
		}
	}

	//Reducer
	public static class TrimReducer extends Reducer<Text,IntWritable,Text,IntWritable>
	{
		public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException 
		{
			int total = 0;
			
			for (IntWritable val : values) 
			{
				total += val.get();
			}
			
			context.write(key, new IntWritable(total));
		}
	}

	//Main
	public static void main(String[] args) throws Exception
	{
		Configuration con = new Configuration();
		Job job1 = Job.getInstance(con, "Word Count Sum");
		job1.setJarByClass(StateWordCount.class);	//jar

		job1.setMapperClass(TextMapper.class);
		job1.setCombinerClass(TextReducer.class);
		job1.setReducerClass(TextReducer.class);

		job1.setOutputKeyClass(Text.class);
		job1.setOutputValueClass(IntWritable.class);

		FileInputFormat.addInputPath(job1, new Path(args[0]));
		FileOutputFormat.setOutputPath(job1, new Path( "/program/wordcount"));

		job1.waitForCompletion(true);


		Job job2 = Job.getInstance(con, "Change Value");
		job2.setJarByClass(StateWordCount.class);	//jar

		job2.setMapperClass(ModifyMapper.class);
		job2.setCombinerClass(ModifyReducer.class);
		job2.setReducerClass(ModifyReducer.class);

		job2.setOutputKeyClass(Text.class);
		job2.setOutputValueClass(Text.class);

		FileInputFormat.addInputPath(job2, new Path("/program/wordcount"));
		FileOutputFormat.setOutputPath(job2, new Path("/program/modifiedval"));

		job2.waitForCompletion(true);

		Job job3 = Job.getInstance(con, "Trimmer Job");
		job3.setJarByClass(StateWordCount.class);	//jar

		job3.setMapperClass(TrimMapper.class);
		job3.setCombinerClass(TrimReducer.class);
		job3.setReducerClass(TrimReducer.class);

		job3.setOutputKeyClass(Text.class);
		job3.setOutputValueClass(IntWritable.class);

		FileInputFormat.addInputPath(job3, new Path("/program/modifiedval"));
		FileOutputFormat.setOutputPath(job3, new Path(args[1]));

		System.exit(job3.waitForCompletion(true) ? 0 : 1);
	}
}
