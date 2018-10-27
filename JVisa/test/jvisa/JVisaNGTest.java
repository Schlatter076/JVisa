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
package jvisa;

import visatype.VisatypeLibrary;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.annotations.Parameters;
import visa.VisaLibrary;

/**
 * This class provides TestNG functions for testing the JVisa class.
 * @author Günter Fuchs (gfuchs@acousticmicroscopy.com)
 * @todo Confirm that all tests run successfully.
 */
public class JVisaNGTest {
  /** JVisa class instance */
  static JVisa instance;
  /** session / resource manager handle */
  long viSession = 0;
  /** instrument handle */
  long viInstrument = 0;
  /** instrument timeout in ms */
  long timeOut = 2000;
  /** IP address as string */
  String ipAddress;
  /** response string from *IDN? */
  String id;
  /** the name of this class used by its logger */
  protected static final String CLASS_NAME = JVisaNGTest.class.getName();
  /** logger of this class */
  public static final Logger LOGGER = Logger.getLogger(CLASS_NAME);
  /** path of log file */
  static final String LOG_PATH = "log/testlog.txt";
  /** log file */
  static File logFile; 

  
  /**
   * This method opens the VISA default resource manager.
   * @throws Exception 
   */
  @BeforeClass
  public static void setUpClass() throws Exception {
    instance = new JVisa();
    System.out.println("openDefaultResourceManager");
    assertEquals(instance.openDefaultResourceManager(), VisatypeLibrary.VI_SUCCESS);
    logFile = new File(LOG_PATH);
  }

  
  /**
   * This method closes the VISA default resource manager.
   * @throws Exception 
   */
  @AfterClass
  public static void tearDownClass() throws Exception {
    System.out.println("closeResourceManager");
    long expResult = VisatypeLibrary.VI_SUCCESS;
    long result = instance.closeResourceManager();
    assertEquals(result, expResult);
    assertEquals(instance.getResourceManagerHandle(), 0);
    assertTrue(logFile.length() > 0, String.format("No entries in log file %s.", LOG_PATH));
  }

  
  /**
   * Test of JVISA_VERSION constant, of class JVisa.
   */
  @Test
  public void testGetVersion() {
    System.out.println("getVersion");
    String result = jvisa.JVisa.JVISA_VERSION;
    System.out.println(result);
    assertTrue(result.startsWith("JVisa"));
  }

  
  /**
   * Test of setLogFile method, of class JVisa.
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
   * Test of getVisaResourceManagerHandle method, of class JVisa.
   */
  @Test
  public void testGetVisaResourceManagerHandle() {
    System.out.println("getVisaResourceManagerHandle");
    viSession = instance.getResourceManagerHandle();
    assertTrue(viSession > 0L, String.format("Resource manager handle is 0x%08X", viSession));
  }

  
  /**
   * Test of getResourceVersion method, of class JVisa.
   */
  @Test(dependsOnMethods = {"testGetVisaResourceManagerHandle"})
  public void testGetResourceVersion() {
    System.out.println("getResourceVersion");
    JVisaReturnNumber version = new JVisaReturnNumber(0L);
    long expResult = VisatypeLibrary.VI_SUCCESS;
    long result = instance.getResourceVersion(version);
    assertEquals(result, expResult);
    long versionLong = version.returnNumber.longValue();
    System.out.println(String.format("VISA specification version is 0x%08X", versionLong));
    // The latest Tektronix VISA library tkVisa64.dll is still only supporting version 4.0.
//    assertTrue(0x00500100L == versionLong);
    assertTrue(0x00400000L <= versionLong);
  }

  
  /**
   * Test of getAttribute method that retrieves a value of type int, of class JVisa.
   * It uses the attribute VI_ATTR_RSRC_MANF_ID.
   */
  @Test(dependsOnMethods = {"testGetVisaResourceManagerHandle"})
  public void testGetAttribute_Int() {
    System.out.println("getAttribute_Int");
    JVisaReturnNumber manufactureId = new JVisaReturnNumber(0);
    long expResult = VisatypeLibrary.VI_SUCCESS;
    long result = instance.getAttribute(VisaLibrary.VI_ATTR_RSRC_MANF_ID, manufactureId, viSession);
    if (JVisa.isLibreVisa == true && JVisa.isLibreVisaDevelop == false) {
      expResult = VisaLibrary.VI_ERROR_NSUP_ATTR;
      assertEquals(result, expResult);
      return;
    }
    assertEquals(result, expResult);
    System.out.println(String.format(
            "Manufacturer ID is 0x%04X.", manufactureId.returnNumber.intValue()));
  }

  
  /**
   * Test of getAttribute method that retrieves a value of type long, of class JVisa.
   * It uses the attribute VI_ATTR_RSRC_IMPL_VERSION.
   */
  @Test(dependsOnMethods = {"testGetVisaResourceManagerHandle"})
  public void testGetAttribute_Long() {
    System.out.println("getAttribute_Long");
    JVisaReturnNumber version = new JVisaReturnNumber(0L);
    long expResult = VisatypeLibrary.VI_SUCCESS;
    long result = instance.getAttribute(VisaLibrary.VI_ATTR_RSRC_IMPL_VERSION, version, viSession);
    if (JVisa.isLibreVisa == true && JVisa.isLibreVisaDevelop == false) {
      expResult = VisaLibrary.VI_ERROR_NSUP_ATTR;
      assertEquals(result, expResult);
      return;
    }
    assertEquals(result, expResult);
    System.out.print("Visa library implementation version is ");
    if (JVisa.isLibreVisa == false) {
      System.out.println(String.format("0x%08X.", version.returnNumber));
      assertTrue(0x400000L <= version.returnNumber.longValue());
    }
    else
      System.out.println(String.format("libreVisa (www.librevisa.org) %d.", version.returnNumber));
  }

  
  /**
   * Test of getAttribute method that retrieves a value of type String, of class JVisa.
   * It uses the attribute VI_ATTR_RSRC_MANF_NAME.
   */
  @Test(dependsOnMethods = {"testGetVisaResourceManagerHandle"})
  public void testGetAttribute_String() {
    System.out.println("getAttribute_String");
    JVisaReturnString manufacturer = new JVisaReturnString();
    long expResult = VisatypeLibrary.VI_SUCCESS;
    long result = instance.getAttribute(VisaLibrary.VI_ATTR_RSRC_MANF_NAME, manufacturer, viSession);
    if (JVisa.isLibreVisa == true) {
      expResult = VisaLibrary.VI_ERROR_NSUP_ATTR;
      assertEquals(result, expResult);
      return;
    }
    assertEquals(result, expResult);
    System.out.println(String.format("Manufacturer name is %s.", manufacturer.returnString));
  }

  
  /**
   * Test of openInstrument method, of class JVisa.
   * @param instrument VISA instrument string, e.g. "TCPIP::138.67.34.158::INSTR"
   * @todo This test fails when running Debian Mint in a VirtualBox.
   */
  @Parameters({"instrument"})
  @Test(dependsOnMethods = {"testGetResourceVersion"}, groups = {"test-group-instrument"})
  public void testOpenInstrument(String instrument) {
    try {
      System.out.println("openInstrument");
      long expResult = VisatypeLibrary.VI_SUCCESS;
      long result = instance.openInstrument(instrument);
      assertEquals(result, expResult);
      viInstrument = instance.getVisaInstrumentHandle();
      if (instrument.startsWith("USB0")) {
        ipAddress = instrument;
      }
      // todo Does not work, at least not with libreVisa.
 //     result = instance.flush(VisaLibrary.VI_IO_IN_BUF);
 //     assertEquals(result, expResult);
    } 
    catch (Exception ex) {
      Logger.getLogger(JVisaNGTest.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  
  /**
   * Test of clear method, of class JVisa.
   */
  @Test(dependsOnMethods = {"testOpenInstrument"}, groups = {"test-group-instrument"})
  public void testClear() {
    try {
      System.out.println("clear");
      long expResult = 0L;
      assertEquals(instance.clear(), expResult);
    }
    catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      fail(ex.getMessage());
    }
  }

  
  /**
   * Test of setTimeout method, of class JVisa.
   * It uses the attribute VI_ATTR_TMO_VALUE.
      The timeoutReturn variable can only be set when an instrument session is active.
   */
  @Test(dependsOnMethods = {"testOpenInstrument"}, groups = {"test-group-instrument-windows"})
  public void testSetTimeout() {
    System.out.println("setTimeout");
    JVisaReturnNumber timeoutReturn = new JVisaReturnNumber((int) 0);
    int timeoutOriginal, timeoutNew = 5123;
    long expResult = VisatypeLibrary.VI_SUCCESS;

    // Read original timeoutReturn. libreVisa library returns not-supported-attribute error.
    long result = instance.getAttribute(VisaLibrary.VI_ATTR_TMO_VALUE, timeoutReturn, viInstrument);
    assertEquals(result, expResult);
    timeoutOriginal = timeoutReturn.returnNumber.intValue();
    assertEquals(timeoutOriginal, timeOut);

    // Set timeoutReturn to new value.
    result = instance.setAttribute(VisaLibrary.VI_ATTR_TMO_VALUE, timeoutNew, viInstrument);
    assertEquals(result, expResult);

    // Read back and compare.
    result = instance.getAttribute(VisaLibrary.VI_ATTR_TMO_VALUE, timeoutReturn, viInstrument);
    assertEquals(result, expResult);
    assertEquals(timeoutReturn.returnNumber.intValue(), timeoutNew);

    // Restore original value.
    result = instance.setAttribute(VisaLibrary.VI_ATTR_TMO_VALUE, timeoutOriginal, viInstrument);
//    result = instance.setAttribute(VisaLibrary.VI_ATTR_TMO_VALUE, 10000, viInstrument);
    assertEquals(result, expResult);

    System.out.println(String.format("Communication timeout is %d ms.", timeoutOriginal));
  }

  
  /**
   * Test of getAttribute method that retrieves a value of type short, of class JVisa.
   * It uses the attribute VI_ATTR_TERMCHAR.
   */
  @Test(dependsOnMethods = {"testOpenInstrument"}, groups = {"test-group-instrument-windows"})
  public void testGetAttribute_Short() {
    System.out.println("getAttribute_Short");
    JVisaReturnNumber value = new JVisaReturnNumber((short) 0);
    long expResult = JVisa.isLibreVisa ? VisaLibrary.VI_ERROR_NSUP_ATTR : VisatypeLibrary.VI_SUCCESS;
    long result = instance.getAttribute(VisaLibrary.VI_ATTR_TERMCHAR, value, viInstrument);
    assertEquals(result, expResult);
    System.out.println(String.format("Value is 0x%02X", value.returnNumber));
  }

  
  /**
   * Test of closeInstrument method, of class JVisa.
   */
  @Test(dependsOnMethods = {"testRead_byteArr_int"}, groups = {"test-group-instrument"})
  public void testCloseInstrument() {
    System.out.println("closeInstrument");
    long expResult = VisatypeLibrary.VI_SUCCESS;
    long result = instance.closeInstrument();
    assertEquals(result, expResult, "Could not close the instrument session.");
  }

  
  /**
   * Test of getVisaInstrumentHandle method, of class JVisa.
   */
  @Test(dependsOnMethods = {"testOpenInstrument"}, groups = {"test-group-instrument"})
  public void testGetVisaInstrumentHandle() {
    System.out.println("getVisaInstrumentHandle");
    assertTrue(instance.getVisaInstrumentHandle() > 0L);
  }

  
  /**
   * Test of reset method, of class JVisa.
   * This test / function allows to reset an instrument.
   * I am excluding this test because it resets all signal settings (trigger,
   * Volts / Division, etc.) and such might cause the waveform test to fail.
   */
//  @Test(dependsOnMethods = {"testOpenInstrument"}, groups = {"test-group-instrument-reset"})
//  public void testReset() {
//    try {
//      System.out.println("write");
//      JVisaReturnString response = new JVisaReturnString();
//      long expResult = VisatypeLibrary.VI_SUCCESS;
//
////      long result;
////      long busyTimeout = System.currentTimeMillis() + timeOut;
////      boolean timedOut = false;
////      do {
////        if (busyTimeout < System.currentTimeMillis()) {
////          timedOut = true;
////          break;
////        }
////        result = instance.write("BUSY?");
////        assertEquals(result, expResult);
////        result = instance.read(response);
////        assertEquals(result, expResult);
////      } while (response.returnString.charAt(0) != '0');
////      assertFalse(timedOut, "Instrument remains busy. Do you have a signal?");
//
//      assertEquals(instance.write("*RST"), expResult);
//    } 
//    catch (JVisaException ex) {
//      Logger.getLogger(JVisaNGTest.class.getName()).log(Level.SEVERE, null, ex);
//      fail(ex.message);
//    }
//  }

  
  /**
   * Test of write method, of class JVisa.
   */
  @Test(dependsOnMethods = {"testOpenInstrument"}, groups = {"test-group-instrument"})
  public void testWrite() {
    System.out.println("write");
    long expResult = VisatypeLibrary.VI_SUCCESS;
    try {
      assertEquals(instance.write("*IDN?"), expResult);
    } 
    catch (JVisaException ex) {
      Logger.getLogger(JVisaNGTest.class.getName()).log(Level.SEVERE, null, ex);
      fail(ex.message);
    }
  }

  
  /**
   * Test of read method, of class JVisa.
   * Parameter to library read function is container for response string as input.
   */
  @Test(dependsOnMethods = {"testWrite"}, groups = {"test-group-instrument"})
  public void testRead_JVisaReturnString() {
    try {
      System.out.println("read");
      JVisaReturnString response = new JVisaReturnString();
      long expResult = VisatypeLibrary.VI_SUCCESS;
      long result = instance.read(response);
      assertEquals(result, expResult);
      id = response.returnString;
      System.out.println(id);
      assertTrue(id.length() > 0, "Response string is empty.");
    } 
    catch (JVisaException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      fail(ex.getMessage());
    }
  }

  
  /**
   * Test of read method, of class JVisa.
   * First parameter to library read function is container for response string as input, 
   * second parameter is buffer size as output.
   */
  @Test(dependsOnMethods = {"testRead_JVisaReturnString"}, groups = {"test-group-instrument"})
  public void testRead_JVisaReturnString_int() {
    try {
      System.out.println("read with buffer size");
      JVisaReturnString response = new JVisaReturnString();
      long expResult = VisatypeLibrary.VI_SUCCESS;
      long result = instance.write("ETHERNET:IPADDRESS?");
      assertEquals(result, expResult);
      result = instance.read(response, instance.bufferSizeDefault);
      assertEquals(result, expResult);
      String responseString = response.returnString;
      assertTrue(responseString.length() > 0, "Response string is empty.");
      System.out.println("Response string: " + responseString);
      if (ipAddress != null) {
        // The response string is enclosed in double quotes. Remove them.
        responseString = responseString.substring(1, responseString.length() - 2);
        assertTrue(ipAddress.contains(responseString));
      }
    } 
    catch (JVisaException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      fail(ex.getMessage());
    }
  }

  
  /**
   * Test of read binary method, of class JVisa.
   * First parameter to library read function is byte array as input, 
   * second parameter is buffer size as output.
   * The "CURVE?" command might be the only one for the Tektronix TDS3000
   * oscilloscope that returns binary data.
   */
  @Test(dependsOnMethods = {"testRead_JVisaReturnString"}, groups = {"test-group-instrument"})
  public void testRead_byteArr_int() {
    try {
      if (!id.contains("TDS")) {
        // Run only if instrument is a Tektronix TDS oscilloscope.
        return;
      }
      System.out.println("read binary");
      long expResult = VisatypeLibrary.VI_SUCCESS;
      boolean resolutionHigh = true;
      // Start acquisition.
      long result = instance.write(String.format("HORIZONTAL:RESOLUTION %s",
              resolutionHigh ? "HIGH" : "LOW"));
      assertEquals(result, expResult);
      result = instance.write("ACQUIRE:NUMAVG 1;STATE RUN");
      assertEquals(result, expResult);
      JVisaReturnString response = new JVisaReturnString();
      // Wait until acquisition has finished.
      long busyTimeout = System.currentTimeMillis() + timeOut;
      boolean timedOut = false;
      do {
        if (busyTimeout < System.currentTimeMillis()) {
          timedOut = true;
          break;
        }
        result = instance.write("BUSY?");
        assertEquals(result, expResult);
        result = instance.read(response);
        assertEquals(result, expResult);
      } while (response.returnString.charAt(0) != '0');
      assertFalse(timedOut, "Instrument remains busy. Do you have a signal?");

      // Get the wave form in binary format.
      result = instance.write("DATA:ENCDG RIBINARY;WIDTH 2");
      assertEquals(result, expResult);
      result = instance.write(String.format("DATA:START 1;STOP %s",
              resolutionHigh ? "10000" : "500"));
      assertEquals(result, expResult);
      instance.write("DATA:SOURCE CH1");
      
      // todo Occassionally, the oscilloscope remained busy after this command.
      instance.write("SAVE:WAVEFORM CH1,REF1");
      busyTimeout = System.currentTimeMillis() + timeOut;
      timedOut = false;
      do {
        if (busyTimeout < System.currentTimeMillis()) {
          timedOut = true;
          break;
        }
        result = instance.write("BUSY?");
        assertEquals(result, expResult);
        result = instance.read(response);
        assertEquals(result, expResult);
      } while (response.returnString.charAt(0) != '0');
      assertFalse(timedOut, "Instrument remains busy. Do you have a signal?");

      instance.write("SELECT:REF1 ON");
      instance.write("DATA:SOURCE REF1");
      result = instance.write("CURVE?");
      assertEquals(result, expResult);
      JVisaReturnBytes buffer = new JVisaReturnBytes();
      // libreVisa supports only a buffer size of up to 12 kB.
      result = instance.read(buffer, 32678, resolutionHigh ? 20008 : 1007);
      // The oscilloscope returns six header characters for low and seven for
      // high resolution. The first header byte is a hash character. It is 
      // followed by the header length - 2 (as ascii). The next characters 
      // indicate the number of data bytes. Such, for low resolution and two 
      // bytes for data width the header is "#41000". For high resolution it is 
      // "#520000". The binary data are terminated with a termination character
      // whose default is linefeed (0x0A).
      instance.write("SELECT:REF1 OFF; CH1 ON");
      assertEquals(result, expResult);
      // Make sure that there are not only zeroes in the buffer.
      byte[] byteArray = buffer.returnBytes;
      System.out.printf("Data header: %c%c%c%c%c%c%c\n", 
              (char) byteArray[0], (char) byteArray[1], (char) byteArray[2], 
              (char) byteArray[3], (char) byteArray[4], (char) byteArray[5],
              (char) byteArray[6]);
      long sum = 0;
      for (int i = 0; i < byteArray.length; i ++)
        sum += byteArray[i];
      assertTrue(sum != 0, "Wave form contains only zeroes.");
      assertEquals(result, expResult);
    } 
    catch (JVisaException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      fail(ex.getMessage());
    }
  }
  
  public static void main(String[] args) {
    try {
      setUpClass();
      //tearDownClass();
      
    } catch (Exception e) {
      // TODO 自动生成的 catch 块
      e.printStackTrace();
    }
    JVisaNGTest ng = new JVisaNGTest();
    JVisa jVisa = new JVisa();
    JVisaReturnString response = new JVisaReturnString();
    ng.testGetVersion();
    ng.testSetLogFile();
    //ng.testGetVisaResourceManagerHandle();
    ng.testGetResourceVersion();
    //ng.testGetAttribute_Int();
    //ng.testGetAttribute_Long();
    //ng.testGetAttribute_String();
    //ng.testOpenInstrument("?*");
    ng.testOpenInstrument("USB0::6833::2500::DM3R200200081::0::INSTR");
    ng.testClear();
    ng.testSetTimeout();
    ng.testGetAttribute_Short();
    //ng.testCloseInstrument();
    ng.testGetVisaInstrumentHandle();
    
    //ng.testWrite();
    try {
      long expResult = VisatypeLibrary.VI_SUCCESS;
      assertEquals(jVisa.write("*IDN?"), expResult);
      long result = jVisa.read(response);
      assertEquals(result, expResult);
      String s = response.returnString;     
      System.out.println(s);
    } catch (JVisaException e) {
      // TODO 自动生成的 catch 块
      e.printStackTrace();
    }
    
    //ng.testRead_JVisaReturnString();
    //ng.testRead_JVisaReturnString_int();
    
  }
  
}
