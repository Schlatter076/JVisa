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

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.logging.Level;
import jvisa.*;

/**
 * VISA instrument driver for the Tektronix TDS3000 oscilloscope series.
 * @author Günter Fuchs (gfuchs@acousticmicroscopy.com)
 * @todo If we have to send a REBOOT command, would we not have to re-open
 *       the instrument session?
 */
public class JInstrument_TDS3000 extends JVisaInstrument implements OscilloscopeInterface {
  /** number of acquisitions to average */
  public int averageCount = 64;
  /** command string that requests the busy flag */
  protected final String IS_BUSY = "BUSY?";
  /** command string for acquisition mode */
  protected final String ACQUIRE_MODE = 
          String.format("ACQUIRE:MODE AVERAGE;STOPAFTER SEQUENCE;NUMAVG %d; STATE STOP", averageCount);
  /** command string for encoding of waveform data */
  protected final String CURVE_ENCODING = "DATA:ENCDG RIBINARY;WIDTH 2";
  /** info string when instrument has finished resetting */
  protected final String RESETTING_FINISHED = "Finished resetting instrument.";
  //protected final String responseEncoding = "DATA:ENCDG ASCII";
  /** 
   * valid values for number of acquisitions to average. 512 might only work for certain models. 
   * @todo Change the type from int to enum.
   */
  protected final int[] VALID_COUNT = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512};
  /** minimum channel value */
  protected final int CHANNEL_MIN = 1;
  /** maximum channel value */
  protected final int CHANNEL_MAX = 4;
  /** default channel */
  protected final int CHANNEL_DEFAULT = CHANNEL_MIN;
  /** size of receive buffer */
  // libreVisa supports only a buffer size of 12 kB.
  public final int BUFFER_SIZE = 0x20000;
  /** default timeout */
  public final int TIMEOUT = 5000;
  /** ordinal of enumerated status code */
  public final long SUCCESS = OscilloscopeInterface.StatusCode.SUCCESS.ordinal();
  /** number of data points / amplitude values in waveform */
  protected int dataPointCount;
  /** return status of VISA function */ 
  protected long visaStatus;
  /**
   * If resolution is set to low, the number of acquired data points is 500,
   * otherwise it is 10,000.
   */
  public boolean isHighResolution = true;

  
  /**
   * This method validates the average count.
   * The number of acquisitions to average has to be a power of 2.
   * @param count average count
   * @return success if count is a power of 2, otherwise error 
   */
  @Override
  public StatusCode validateAverageCount(int count) {
    return (Arrays.binarySearch(VALID_COUNT, count) < 0 
            ? StatusCode.PARAMETER_INVALID : StatusCode.SUCCESS);
  }
  
  /**
   * This method reads the busy status from the instrument.
   * @return busy status
   */
  @Override
  public StatusCode getBusyStatus() {
    try {
      JVisaReturnString response = new JVisaReturnString();
      sendAndReceive(IS_BUSY, response);
      return (Integer.parseInt(response.returnString) == 0 ? StatusCode.SUCCESS : StatusCode.BUSY);
    }
    catch (JVisaException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
      return StatusCode.VISA_ERROR;
    }
  }
  
  
  /**
   * This method polls the busy status until the instrument indicates that it is
   * ready or the timeout value is reached.
   * @param timeout stop waiting after this many milliseconds
   * @return busy status
   */
  @Override
  public StatusCode waitForReady(long timeout) {
    StatusCode status = StatusCode.BUSY;
    try {
      long pollingStart = System.currentTimeMillis();
      do {
        status = getBusyStatus();
        if (status != StatusCode.BUSY) {
          return status;
        }
      } while(System.currentTimeMillis() - pollingStart < timeout);
      return status;
    }
    catch (Exception e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
      status = StatusCode.EXCEPTION;
      visaStatus = VISA_JAVA_ERROR;
      return status;
    }
  }
  
  /**
   * This method resets the instrument.
   * @param averageCount number of acquisitions for average waveform;
   * @param timeout communication timeout after this many milliseconds 
   *                (VI_ATTR_TMO_VALUE attribute)
   * @return status of the operation
   */
  @Override
  public StatusCode reset(int averageCount, int timeout) {
    visaStatus = VISA_JAVA_ERROR;
    StatusCode status = validateAverageCount(averageCount);
    if (status != StatusCode.SUCCESS) {
      return status;
    }
    LOGGER.severe("Resetting instrument...");
    try {
      write("*RST");
      // The command above should have reset the busy status.
      // The timeout used here is not the communication timeout handed to this
      // function in a parameter, but hard-coded here.
      status = waitForReady(6000);
      if (status != StatusCode.SUCCESS) {
        throw new JVisaException(statusObject.getVisaStatus());
      }
//      write("*CLS");
      visaStatus = clear();
      if (visaStatus != SUCCESS) {
        throw new JVisaException(statusObject.getVisaStatus());
      }
      visaStatus = setTimeout(timeout);
      if (visaStatus != SUCCESS) {
        throw new JVisaException(statusObject.getVisaStatus());
      }
      write(CURVE_ENCODING);
      write("HORIZONTAL:TRIGGER:POSITION 0");
      write("TRIGGER:A:SETLEVEL");
      this.averageCount = averageCount;
      
      write(ACQUIRE_MODE);
      status = waitForReady(10000);
      if (status != StatusCode.BUSY) {
        LOGGER.severe(RESETTING_FINISHED);
        return status;
      }
      // Instrument polling timed out.
      write("*OPC");
      write("ACQUIRE:STATE STOP");
      status = getBusyStatus();
      if (status == StatusCode.SUCCESS) {
        LOGGER.severe(RESETTING_FINISHED);
        return status;
      }
      // Instrument is still not happy.
      LOGGER.severe(RESETTING_FINISHED);
      LOGGER.severe("Rebooting instrument...");
      write("REBOOT");
      return StatusCode.REBOOTED;
    }
    catch (JVisaException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
      return StatusCode.VISA_ERROR;
    }
  }

  
  /**
   * This method resets the instrument with default values for average count and
   * timeout.
   * @return reset status
   */
  @Override
  public StatusCode reset() {
    return reset(averageCount, TIMEOUT);
  }
  
  
  /**
   * This method acquires a waveform.
   * @param waveForms waveform data received from the instrument and its 
   *                 parameters
   * @param averageCount number of acquisitions to average
   * @param channels channels to acquire (1 to 4)
   * @return status of the operation
   */
  @Override
  public StatusCode acquire(JWaveForm waveForms[], int averageCount, int channels[]) {
    StatusCode status = validateAverageCount(averageCount);
    if (status != StatusCode.SUCCESS) {
      return status;
    }
    if (channels == null)
      return StatusCode.PARAMETER_INVALID;
    for (int channel : channels) {
      if (channel < CHANNEL_MIN || channel > CHANNEL_MAX) {
        return StatusCode.PARAMETER_INVALID;
      }
    }
    this.averageCount = averageCount;
    visaStatus = VISA_JAVA_ERROR;
    JVisaReturnString response = new JVisaReturnString();
    String responseString;
    JWaveForm waveForm = waveForms[0];
    try {
      // ------- Set the acquisition parameters. ---------------  
      status = waitForReady(TIMEOUT);
      if (status != StatusCode.SUCCESS) {
        if (status == StatusCode.BUSY) {
          LOGGER.severe("Instrument is busy.");
          return status;
        }
        throw new JVisaException(statusObject.getVisaStatus());
      }
      write(ACQUIRE_MODE);
      write(CURVE_ENCODING);
      write(String.format("ACQUIRE:NUMAVG %d", averageCount));
      String resolution;
      if (isHighResolution) {
        resolution = "HIGH";
        dataPointCount = 10000;
      }
      else {
        resolution = "LOW";
        dataPointCount = 500;
      }
      write(String.format("HORIZONTAL:RESOLUTION %s", resolution));
      if (waveForm.triggerDelay != null) {
        write(String.format("HORIZONTAL:DELAY:TIME %E", waveForm.triggerDelay));
        write("HORIZONTAL:DELAY:STATE ON");
      }
      write(String.format("DATA:START 1;STOP %d", dataPointCount));
      write("HEADER OFF");

      // ------- Start the acquisition. ------------------------------
      write("ACQUIRE:STATE RUN");
      LOGGER.info("Acquiring waveform...");
      
      // ------- Wait for acquisition(s) to finish. ----------------------
//      long timeout = 300 * averageCount * channels.length; // in ms
      long timeout = 1200 * averageCount * channels.length; // in ms
      status = waitForReady(timeout);
      if (status == StatusCode.BUSY) {
        // This usually happens if "Single Seq" is on or there is no trigger.
        return status;
      }
      
      // ------- Obtain settings for waveforms. ----------------------
      for (int channel: channels) {
        write(String.format("DATA:SOURCE CH%d", channel));
        write(String.format("SAVE:WAVEFORM CH%d,REF%d", channel, channel));
        write(String.format("SELECT:REF%d ON", channel));
        write(String.format("DATA:SOURCE REF%d", channel));
        sendAndReceive("DATA:WIDTH?", response);
        int dataWidth = Integer.parseInt(response.returnString);

        sendAndReceive(String.format("CH%d:SCALE?", channel), response);
        waveForm = waveForms[channel - 1];
        waveForm.gain = Double.parseDouble(response.returnString);

        sendAndReceive("HORIZONTAL:SCALE?", response);
        //double mainScale = Double.parseDouble(response.getReturnString());

        sendAndReceive("WFMPRE:XINCR?", response);
        waveForm.rate = Double.parseDouble(response.returnString);

        sendAndReceive("WFMPRE:NR_PT?", response);
        waveForm.dataSize = Integer.parseInt(response.returnString);
        if (waveForm.dataSize != dataPointCount) {
          return StatusCode.WAVEFORM_ERROR;
        }

        sendAndReceive("HORIZONTAL:DELAY:TIME?", response);
        //responseString = response.returnString;
        // DELAY:TIME is the time the trigger is off the center of the screen
        // which is on the time scale half the total time of the acquisition trace.
        // Therefore, the absolute trigger point in time = total delay time / 2.
        waveForm.triggerDelay = Double.parseDouble(response.returnString) 
                - waveForm.dataSize.doubleValue() * waveForm.rate / 2.0;

        sendAndReceive("WFMPRE:YMULT?", response);
        responseString = response.returnString;
        double yScale = Double.parseDouble(responseString);

        sendAndReceive("WFMPRE:YZERO?", response);
        double yZero = Double.parseDouble(response.returnString);

        sendAndReceive("WFMPRE:YOFF?", response);
        double yOffset = Double.parseDouble(response.returnString);

        sendAndReceive("DATA:ENCDG?", response);
        responseString = response.returnString;
        boolean isAsciiEncoding;
        isAsciiEncoding = responseString.startsWith("ASCI");

        // This command is necessary. Otherwise "CURVE?" returns an unknown
        // system error.
        // todo 2016/04/13: not found to be needed (TDS3014B)
        //write("*OPC");

        int i = 0;
        int expectedResponseCount;
        if (isAsciiEncoding) {
          sendAndReceive("CURVE?", response, BUFFER_SIZE);
          LOGGER.info("Acquisition finished successfully.");
          // Values are comma separated. Not used.
          String dataPoint;
          waveForm.data = new double[waveForm.dataSize];
          StringTokenizer st = new StringTokenizer(response.returnString, ",");
          dataPoint = (String) st.nextElement();
          waveForm.data[i++] = (Double.parseDouble(dataPoint) - yOffset) * yScale + yZero;
          waveForm.min = waveForm.data[0];
          waveForm.max = waveForm.min;
          while (st.hasMoreElements()) {
            dataPoint = (String) st.nextElement();
            waveForm.data[i] = (Double.parseDouble(dataPoint) - yOffset) * yScale + yZero;
            if (waveForm.data[i] < waveForm.min) {
              waveForm.min = waveForm.data[i];
            }
            else if (waveForm.data[i] > waveForm.max) {
              waveForm.max = waveForm.data[i];
            }
            i++;
          }
        }
        else {
          // The Tektronix scope returns the data in the following format:
          // #<x><yy..><block of bytes or integers><termination character>
          // <x> indicates in ASCII the number of y's
          // <yy..> indicates in ASCII the number of data bytes
          JVisaReturnBytes buffer = new JVisaReturnBytes();
          if (dataWidth == 1)
            expectedResponseCount = isHighResolution ? 10008 : 5006;
          else
            expectedResponseCount = isHighResolution ? 20008 : 1007;
          sendAndReceive("CURVE?", buffer, BUFFER_SIZE, expectedResponseCount);

          // Check validity of first byte.
          if (buffer.returnBytes[0] != '#') {
            return StatusCode.WAVEFORM_ERROR;
          }
          // Check validity of <yy..>. It has to match dataSize.
          int yLength = buffer.returnBytes[1] & 0x0F;
          byte[] y = new byte[yLength];
          System.arraycopy(buffer.returnBytes, 2, y, 0, yLength);
          String countString = new String(y);
          int count = Integer.parseInt(countString);
          if (count != waveForm.dataSize * dataWidth) {
            return StatusCode.WAVEFORM_ERROR;
          }
          // Now it should be safe to convert the byte data into the double array.
          waveForm.data = new double[waveForm.dataSize];
          // 2: '#' + <x>
          int sourceIndex = 2 + yLength, destinationIndex;
          if (dataWidth == 1) {
            waveForm.data[0] = (((double) buffer.returnBytes[sourceIndex++]) - yOffset) * yScale + yZero;
            waveForm.min = waveForm.data[0];
            waveForm.max = waveForm.min;
            for (destinationIndex = 1; destinationIndex < waveForm.dataSize; destinationIndex++) {
              waveForm.data[destinationIndex] = (((double) buffer.returnBytes[sourceIndex++]) - yOffset) * yScale + yZero;
              if (waveForm.data[destinationIndex] < waveForm.min) {
                waveForm.min = waveForm.data[destinationIndex];
              }
              if (waveForm.data[destinationIndex] > waveForm.max) {
                waveForm.max = waveForm.data[destinationIndex];
              }
            }
          }
          else {
            // dataWidth == 2
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer.returnBytes, sourceIndex, waveForm.dataSize * 2);
            ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
            int dataPoint = (int) shortBuffer.get(0);
            waveForm.data[0] = ((double) dataPoint - yOffset) * yScale + yZero;
            waveForm.min = waveForm.data[0];
            waveForm.max = waveForm.min;

            for (destinationIndex = 1; destinationIndex < waveForm.dataSize; destinationIndex++) {
              dataPoint = (int) shortBuffer.get(destinationIndex);
              waveForm.data[destinationIndex] = ((double) dataPoint - yOffset) * yScale + yZero;
              if (waveForm.data[destinationIndex] < waveForm.min) {
                waveForm.min = waveForm.data[destinationIndex];
              }
              if (waveForm.data[destinationIndex] > waveForm.max) {
                waveForm.max = waveForm.data[destinationIndex];
              }
            }
          }
        }
        write(String.format("SELECT:REF%d OFF", channel));
      }
      return StatusCode.SUCCESS;
    }
    catch (JVisaException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
      return StatusCode.VISA_ERROR;
    }
    catch (NumberFormatException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
      return StatusCode.EXCEPTION;
    }
  }
  
  
  /**
   * This method acquires a waveform.
   * @param waveForm data for one wave form received from the instrument and its 
   *                 parameters
   * @param averageCount number of acquisitions to average
   * @param channel channel to acquire (1 to 4)
   * @return status of the operation
   */
  @Override
  public StatusCode acquire(JWaveForm waveForm, int averageCount, int channel) {
    JWaveForm waveForms[] = {waveForm};
    int channels[] = {channel};
    return acquire(waveForms, averageCount, channels);
  }

  
  /**
   * This method acquires a waveform.
   * @param waveform waveform data received from the instrument and its parameters
   * @return status of the operation
   * todo Make sure that the buffer for receiving the waveform is big enough.
   */
  @Override
  public StatusCode acquire(JWaveForm waveform) {
    return acquire(waveform, this.averageCount, CHANNEL_DEFAULT);
  }

  
  /**
   * This method Acquires a waveform.
   * @param waveform waveform data received from the instrument and its parameters
   * @param averageCount number of acquisitions to average
   * @return status of the operation
   */
  @Override
  public StatusCode acquire(JWaveForm waveform, int averageCount) {
    return acquire(waveform, averageCount, CHANNEL_DEFAULT);
  }
}
