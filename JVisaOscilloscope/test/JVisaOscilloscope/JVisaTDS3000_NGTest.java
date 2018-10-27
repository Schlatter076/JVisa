/**
 * @license

Copyright 2014-2018 Günter Fuchs (gfuchs@acousticmicroscopy.com)

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
package JVisaOscilloscope;

import jvisa.*;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
//import static jvisa.JVisa.LOGGER;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.annotations.Parameters;


/**
 * This class provides TestNG functions for testing the JInstrument_TDS3000 class.
 * @author Günter Fuchs
 * todo Add data acquisition tests and remove tests that are already covered by
 *      JVisa tests. See JVisaTest.java in NetBeans project JTest_TDS3000.
 */
public class JVisaTDS3000_NGTest {
  /** JVisaOscilloscope class instance */
  static JInstrument_TDS3000 instance;
  /** session / resource manager handle */
  long viSession = 0;
  /** instrument handle */
  long viInstrument = 0;
  /** the name of this class used by its logger */
  protected static final String CLASS_NAME = JVisaTDS3000_NGTest.class.getName();
  /** logger of this class */
  public static final Logger LOGGER = Logger.getLogger(CLASS_NAME);
  /** path of log file */
  static final String LOG_PATH = "log/testlog.txt";
  /** expected number of bytes in response to "CURVE?" command for high resolution
   * and 2 bytes data width
   */
  static final int EXPECTED_RESPONSE_COUNT = 20008;
  /** log file */
  static File logFile; 
  
  /**
   * This method opens the VISA default resource manager.
   * @throws Exception 
   */
  @BeforeClass
  public static void setUpClass() throws Exception {
    instance = new JInstrument_TDS3000();
    assertNotEquals(JInstrument_TDS3000.visaLib, null);
    System.out.println("openDefaultResourceManager");
    long expResult = JVisaStatus.VISA_JAVA_SUCCESS;
    long result = instance.openDefaultResourceManager();
    assertEquals(result, expResult);
    logFile = new File(LOG_PATH);
  }

  
  /**
   * This method closes the VISA default resource manager.
   * @throws Exception 
   */
  @AfterClass
  public static void tearDownClass() throws Exception {
    System.out.println("closeResourceManager");
    long expResult = JVisaStatus.VISA_JAVA_SUCCESS;
    long result = instance.closeResourceManager();
    assertEquals(result, expResult);
    assertEquals(instance.getResourceManagerHandle(), 0);
    assertTrue(logFile.length() > 0, String.format("No entries in log file %s.", LOG_PATH));
  }

  
  /**
   * Test of setLogFile method, of class JInstrument_TDS3000.
   */
  @Test
  public void testSetLogFile() {
    System.out.println("setLogFile");
    if (logFile.exists()) {
      logFile.delete();
    }
    instance.setLogFile(LOG_PATH);
    JVisa.logFileHandler.setFormatter(new SimpleFormatter());
    JVisa.LOGGER.setLevel(Level.FINE);
  }


  /**
   * Test of getVisaResourceManagerHandle method, of class JInstrument_TDS3000.
   */
  @Test
  public void testGetVisaResourceManagerHandle() {
    System.out.println("getVisaResourceManagerHandle");
    viSession = instance.getResourceManagerHandle();
    assertTrue(viSession > 0L, String.format("Resource manager handle is %08X", viSession));
  }

  
  /**
   * Test of getResourceVersion method, of class JInstrument_TDS3000.
   */
  @Test(dependsOnMethods = {"testGetVisaResourceManagerHandle"})
  public void testGetResourceVersion() {
    System.out.println("getResourceVersion");
    JVisaReturnNumber version = new JVisaReturnNumber(0L);
    long expResult = JVisaStatus.VISA_JAVA_SUCCESS;
    long result = instance.getResourceVersion(version);
    assertEquals(result, expResult);
    long versionLong = version.returnNumber.longValue();
    assertTrue(0x00400000L <= versionLong);
    System.out.println(String.format("VISA library specification version is 0x%08X.", versionLong));
  }

  
  /**
   * Test of openInstrument method, of class JInstrument_TDS3000.
   * @param instrument VISA instrument string, e.g. "TCPIP::138.67.34.158::INSTR"
   */
  @Parameters({"instrument"})
  @Test(dependsOnMethods = {"testGetResourceVersion"}, groups = {"test-group-instrument"})
  public void testOpenInstrument(String instrument) {
    System.out.println("openInstrument");
    long expResult = JVisaStatus.VISA_JAVA_SUCCESS;
    long result = instance.openInstrument(instrument);
    assertEquals(result, expResult);
  }

  
  /**
   * Test of closeInstrument method, of class JInstrument_TDS3000.
   */
  @Test(dependsOnMethods = {"testRead_JVisaReturnString"}, groups = {"test-group-instrument"})
  public void testCloseInstrument() {
    System.out.println("closeInstrument");
    long expResult = JVisaStatus.VISA_JAVA_SUCCESS;
    long result = instance.closeInstrument();
    assertEquals(result, expResult, "Could not close the instrument session.");
  }

  
  /**
   * Test of getVisaInstrumentHandle method, of class JInstrument_TDS3000.
   */
  @Test(dependsOnMethods = {"testOpenInstrument"}, groups = {"test-group-instrument"})
  public void testGetVisaInstrumentHandle() {
    System.out.println("getVisaInstrumentHandle");
    long result = instance.getVisaInstrumentHandle();
    assertTrue(result > 0L);
  }

  
  /**
   * Test of write method, of class JInstrument_TDS3000.
   */
  @Test(dependsOnMethods = {"testOpenInstrument"}, groups = {"test-group-instrument"})
  public void testWrite() {
    System.out.println("write");
    long expResult = JVisaStatus.VISA_JAVA_SUCCESS;
    long result;
    try {
      result = instance.write("MESSAGE:BOX 0,0,639,24;STATE ON;SHOW \"Running unit test for JInstrument_TDS3000 class\"");
      assertEquals(result, expResult);
      Thread.sleep(1000);
      result = instance.write("MESSAGE:STATE OFF");
      assertEquals(result, expResult);
    } 
    catch (JVisaException | InterruptedException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      fail(ex.getMessage());
    }
  }

  
  /**
   * Test of read method for string, of class JInstrument_TDS3000.
   * Parameter is output string.
   */
  @Test(dependsOnMethods = {"testWrite"}, groups = {"test-group-instrument"})
  public void testRead_JVisaReturnString() {
    System.out.println("read string");
    JVisaReturnString response = new JVisaReturnString();
    long expResult = 0L;
    long result;
    try {
      instance.write("*IDN?");
      result = instance.read(response);
      assertEquals(result, expResult);
      assertTrue(response.returnString.length() > 0, "Response string is empty.");
    } 
    catch (JVisaException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      fail(ex.getMessage());
    }
  }

  
  /**
   * Test of read method, of class JInstrument_TDS3000.
   * First parameter is output byte array, second parameter is buffer size as input.
   */
  @Test(dependsOnMethods = {"testWrite"}, groups = {"test-group-instrument"})
  public void testRead_byteArr_int() {
    try {
      System.out.println("read int array");
      instance.write("HORIZONTAL:RESOLUTION HIGH");
      instance.write("ACQUIRE:STATE RUN");
      // Wait for acquisition(s) to finish.
      assertEquals(instance.waitForReady(3000), OscilloscopeInterface.StatusCode.SUCCESS);
      instance.write("DATA:SOURCE REF1");
      instance.write("SELECT:REF1 ON");
      instance.write("DATA:START 1; STOP 10000");
      instance.write(instance.CURVE_ENCODING);
      instance.write("CURVE?");
      JVisaReturnBytes buffer = new JVisaReturnBytes();
      long expResult = 0L;
      long result = instance.read(buffer, instance.BUFFER_SIZE, EXPECTED_RESPONSE_COUNT);
      assertEquals(result, expResult);
      assertEquals(buffer.returnBytes.length, EXPECTED_RESPONSE_COUNT);
      instance.write("SELECT:REF1 OFF; CH1 ON");
    } 
    catch (JVisaException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      fail(ex.getMessage());
    }
  }

  
  /**
   * Test of acquire method, of class JInstrument_TDS3000.
   * @param channelString string that contains the channels to test in the form
   *                      "123..". Maximum is four channels for the TDS3000.
   * @param showGraphString if "1", show waveform in graph dialog
   */
  @Parameters({"channels", "showGraph"})
  @Test(dependsOnMethods = {"testRead_JVisaReturnString"}, groups = {"test-group-instrument"})
  public void testAcquire(String channelString, String showGraphString) {
    System.out.println("acquire");
    JWaveForm waveForms[] = {new JWaveForm(), new JWaveForm(), new JWaveForm(), new JWaveForm()};
    int channels[] = new int[channelString.length()];
    for (int i = 0; i < channels.length; i++)
      channels[i] = channelString.charAt(i) - 0x30;
    JInstrument_TDS3000.StatusCode result = instance.acquire(waveForms, 1, channels);
    JInstrument_TDS3000.StatusCode expResult = JInstrument_TDS3000.StatusCode.SUCCESS;
    assertEquals(result, expResult);
    
    if (showGraphString.startsWith("1") == true) {
      // Show first waveform in graph dialog.
      System.out.println("Close graph dialog to finish test suite...");
      GraphDialog graphDialog = new GraphDialog(null, true);
      graphDialog.pack();
      GraphPanel panel = graphDialog.getGraph();
      panel.setData(waveForms[0]);
      graphDialog.setVisible(true);
    }
  }

  
  /**
   * Test of clear method, of class JInstrument_TDS3000.
   */
  @Test(dependsOnMethods = {"testOpenInstrument"}, groups = {"test-group-instrument"})
  public void testClear() {
    System.out.println("clear");
    long expResult = 0L;
    long result = instance.clear();
    assertEquals(result, expResult);
  }
}
