package org.cloudrobot.jloadtester;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class TestUrl {
  private static Logger logger = Logger.getLogger(LoadTester.class.getName());
  private String url;
  private String method;
  private String postData;
  
  TestUrl(String url, String method, String postData) {
    this.url = url;
    this.method = method;
    this.postData = postData;
  }
  
  public String url() {
    return url;
  }
  
  public String method() {
    return method;
  }
  
  public String postData() {
    return postData;
  }
}