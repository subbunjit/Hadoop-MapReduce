To Run the Application use the following commands:

Package names for programs:
com.code.statewordcount
com.code.wordrank

Make a directory in HDFS:
namenode$ hdfs dfs -mkdir [directoryname]

First place the file in the HDFS by using the following command:
namenode$ hdfs dfs -put /home/ec2-user/[Files] [output directory of HDFS]

For executing StateWordCount:
namenode$ hadoop jar StateWordCount.jar com.code.statewordcount.StateWordCount [input directory] [output directory]

For executing WordRank:
namenode$ hadoop jar WordRank.jar com.code.wordrank.WordRank [input directory] [output directory]

For checking output, use the command:
namenode$ hadoop fs -cat /user/ec2-user/final/part-r-00000

