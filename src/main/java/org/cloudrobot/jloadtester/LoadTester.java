package org.cloudrobot.jloadtester;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.util.Stack;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URLConnection;
import java.net.URL;
import java.io.InputStreamReader;
import java.util.Random;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LoadTester {
  private static Logger logger = Logger.getLogger(LoadTester.class.getName());
  private Stack<TestUrl> testUrls;
  private AtomicInteger processedUrlsCount;
  private ExecutorService executor;
  
  public static void main(String[] args) throws Exception {
    new LoadTester(args[0], Integer.parseInt(args[1]));
  }
  
  LoadTester(String inputFile, int numConcurrent) throws Exception {
    PropertyConfigurator.configure("log4j.configuration");
    
    executor = Executors.newFixedThreadPool(numConcurrent);
    processedUrlsCount = new AtomicInteger(0);
    Random r = new Random();
    
    BufferedReader in = null;
    try {
      in = new BufferedReader(new FileReader(inputFile));
      String str;
      while ((str = in.readLine()) != null) {
        String[] parts = str.split("\t");
        
        if (parts.length == 4) {
          Integer count = Integer.parseInt(parts[1]);
          for (int i = 0; i < count; i++) {
            String newPostBody = parts[3];
            
            while (newPostBody.indexOf("<<rand_int>>") >= 0) {
              newPostBody = newPostBody.replace("<<rand_int>>", r.nextInt()+"");
            }
            
            queueUrl(parts[2], parts[0], newPostBody);
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    finally {
      in.close();
    }
    
    start(numConcurrent);
  }
  
  private void queueUrl(String url, String method, String postData) {
    if (testUrls == null) {
      testUrls = new Stack<TestUrl>();
    }
    
    synchronized(testUrls) {
      testUrls.push(new TestUrl(url, method, postData));
    }
  }
  
  private void start(int numConcurrent) {
    System.out.println("Running...");
    long startAt = System.nanoTime();
    
    for (int i = 0; i < numConcurrent; i++) {
      executor.execute(new QueueProcessor());
    }
    executor.shutdown();
    try {
      executor.awaitTermination(60*60*24, TimeUnit.SECONDS); // wait 1 day
    }
    catch (java.lang.InterruptedException e) {e.printStackTrace();}
    
    long endAt = System.nanoTime();
    System.out.println("Done!");
    
    long totalTime = endAt - startAt;
    double perSec = processedUrlsCount.get() / ((double)totalTime/1000000000l);
    
    
    System.out.println("########################################");
    System.out.format("%-25s%15d%n",    "# total urls", processedUrlsCount.get());
    System.out.format("%-25s%15s%n",    "# total time", (totalTime/1000000000.0) + " sec");
    System.out.format("%-25s%15.3f%n",  "# req/sec", perSec);
    System.out.println("########################################");
  }
  
  class QueueProcessor implements Runnable {
    public QueueProcessor() {
    }
    
    public void run() {
      try {
        while (true) {
          TestUrl nextUrl = null;
          synchronized(testUrls) {
            nextUrl = testUrls.pop();
          }
          
          execTestUrl(nextUrl);
          
          processedUrlsCount.addAndGet(1);
        }
      }
      catch (java.util.EmptyStackException e) {
        // we're done.
      }
    }
    
    private void execTestUrl(TestUrl testUrl) {
      OutputStreamWriter wr = null;
      BufferedReader rd = null;
      
      try {
        URL url = new URL(testUrl.url());
        URLConnection conn = url.openConnection();
        
        if (testUrl.method().equals("POST")) {
          conn.setDoOutput(true);
          wr = new OutputStreamWriter(conn.getOutputStream());
          wr.write(testUrl.postData());
          wr.flush();
        }
        
        // Get the response
        rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
          // ignore for now
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      finally {
        try {
          if (wr != null)
            wr.close();
          if (rd != null)
            rd.close();
        }
        catch (java.io.IOException e) {}
      }
    }
  }
}