/*
Copyright 2013 Twitter, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.twitter.hraven.etl;

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.io.IOException;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Test;

import com.google.common.io.Files;
import com.twitter.hraven.Constants;
import com.twitter.hraven.datasource.ProcessingException;

public class TestJobHistoryFileParserBase {

  @Test(expected=ProcessingException.class)
  public void testIncorrectGetXmxValue(){
    String xmxValue = "-XmxSOMETHINGWRONG!";
    @SuppressWarnings("unused")
    long val = JobHistoryFileParserBase.getXmxValue(xmxValue);
  }

  @Test
  public void testNullGetXmxValue(){
    String xmxValue = null;
    Long val = JobHistoryFileParserBase.getXmxValue(xmxValue);
    assertEquals(Constants.DEFAULT_XMX_SETTING, val);
  }

  @Test
  public void testGetXmxValue(){
    // check for megabyte value itself
    String xmxValue = "-Xmx500m";
    long expValue = 500;
    long actualValue = JobHistoryFileParserBase.getXmxValue(xmxValue);
    assertEquals(expValue, actualValue);
    long totalValue = JobHistoryFileParserBase.getXmxTotal(actualValue);
    assertEquals(666L, totalValue);

    // check if megabytes is returned for kilobytes
    xmxValue = "-Xmx2048K";
    actualValue = JobHistoryFileParserBase.getXmxValue(xmxValue);
    expValue = 2L;
    assertEquals(expValue, actualValue);
    totalValue = JobHistoryFileParserBase.getXmxTotal(actualValue);
    long expTotalVal = 2L;
    assertEquals(expTotalVal, totalValue);

    // check if megabytes is returned for gigabytes
    xmxValue = "-Xmx2G";
    actualValue = JobHistoryFileParserBase.getXmxValue(xmxValue);
    expValue = 2048;
    assertEquals(expValue, actualValue);
    totalValue = JobHistoryFileParserBase.getXmxTotal(actualValue);
    expTotalVal = 2730L;
    assertEquals(expTotalVal, totalValue);

    // what happens whene there are 2 Xmx settings,
    // picks the first one
    xmxValue = "-Xmx2G -Xms 1G -Xmx4G";
    actualValue = JobHistoryFileParserBase.getXmxValue(xmxValue);
    expValue = 2048;
    assertEquals(expValue, actualValue);
    totalValue = JobHistoryFileParserBase.getXmxTotal(actualValue);
    expTotalVal = 2730L;
    assertEquals(expTotalVal, totalValue);

    // check if megabytes is returned for bytes
    xmxValue = "-Xmx2097152";
    actualValue = JobHistoryFileParserBase.getXmxValue(xmxValue);
    expValue = 2L;
    assertEquals(expValue, actualValue);
    totalValue = JobHistoryFileParserBase.getXmxTotal(actualValue);
    expTotalVal = 2L;
    assertEquals(expTotalVal, totalValue);

    xmxValue = " -Xmx1024m -verbose:gc -Xloggc:/tmp/@taskid@.gc" ;
    actualValue = JobHistoryFileParserBase.getXmxValue(xmxValue);
    expValue = 1024L;
    assertEquals(expValue, actualValue);
    totalValue = JobHistoryFileParserBase.getXmxTotal(actualValue);
    expTotalVal = 1365L;
    assertEquals(expTotalVal, totalValue);
  }

  @Test
  public void testExtractXmxValue() {
    String jc = " -Xmx1024m -verbose:gc -Xloggc:/tmp/@taskid@.gc" ;
    String valStr = JobHistoryFileParserBase.extractXmxValueStr(jc);
    String expStr = "1024m";
    assertEquals(expStr, valStr);
  }

  @Test
  public void testExtractXmxValueIncorrectInput(){
    String jc = " -Xmx" ;
    String valStr = JobHistoryFileParserBase.extractXmxValueStr(jc);
    String expStr = Constants.DEFAULT_XMX_SETTING_STR;
    assertEquals(expStr, valStr);
  }

  @Test(expected=ProcessingException.class) 
  public void testGetXmxValueIncorrectInput2(){
    String jc = " -Xmx1024Q" ;
    @SuppressWarnings("unused")
    Long value = JobHistoryFileParserBase.getXmxValue(jc);
  }

  @Test
  public void testGetSubmitTimeMillisFromJobHistory2() throws IOException {
    String JOB_HISTORY_FILE_NAME =
        "src/test/resources/job_1329348432655_0001-1329348443227-user-Sleep+job-1329348468601-10-1-SUCCEEDED-default.jhist";

    // hadoop2 file
    File jobHistoryfile = new File(JOB_HISTORY_FILE_NAME);
    byte[] contents = Files.toByteArray(jobHistoryfile);
    long actualts = JobHistoryFileParserBase.getSubmitTimeMillisFromJobHistory(contents);
    long expts = 1329348443227L;
    assertEquals(expts, actualts);

    // another hadoop2 file
    JOB_HISTORY_FILE_NAME =
        "src/test/resources/job_1329348432999_0003-1329348443227-user-Sleep+job-1329348468601-10-1-SUCCEEDED-default.jhist";
    jobHistoryfile = new File(JOB_HISTORY_FILE_NAME);
    contents = Files.toByteArray(jobHistoryfile);
    actualts = JobHistoryFileParserBase.getSubmitTimeMillisFromJobHistory(contents);
    expts = 1328218696000L;
    assertEquals(expts, actualts);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testIncorrectSubmitTime() {
    // Now some cases where we should not be able to find any timestamp.
    byte[] jobHistoryBytes = Bytes.toBytes("");
    JobHistoryFileParserBase.getSubmitTimeMillisFromJobHistory(jobHistoryBytes);
  }

  @Test
  public void testCostDefault() {
    Double jobCost = JobHistoryFileParserBase.calculateJobCost(100L,
      0.0, 0L);
    assertEquals(0.0, jobCost, 0.0001);
    jobCost = JobHistoryFileParserBase.calculateJobCost(100L, 20.0, 512L);
    assertEquals(1.413850E-10, jobCost, 0.0001);
  }
}

