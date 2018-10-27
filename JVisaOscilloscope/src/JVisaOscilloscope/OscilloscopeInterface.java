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

/**
 * This class describes the interface for a VISA compliant oscilloscope driver.
 * @author Günter Fuchs (gfuchs@acousticmicroscopy.com)
 */
public interface OscilloscopeInterface {

  /** enumeration for status codes */
  public enum StatusCode {
    /** status for success */
    SUCCESS,
    /** busy status */
    BUSY,
    /** status for VISA error */
    VISA_ERROR,
    /** status when an exception was thrown */
    EXCEPTION,
    /** status when instrument has rebooted */
    REBOOTED,
    /** status when input parameter is invalid */
    PARAMETER_INVALID,
    /** status when trying to receive a waveform generated an error */
    WAVEFORM_ERROR;
  }
  
  /**
   * This method acquires a waveform.
   * @param waveForm waveform data and its parameters received from the instrument
   * @param averageCount number of acquisitions to average
   * @param channel channel to acquire
   * @return status of the operation
   */
  StatusCode acquire(JWaveForm waveForm, int averageCount, int channel);

  /**
   * This method acquires a number of waveforms.
   * @param waveForms waveform data and its parameters received from the instrument
   * @param averageCount number of acquisitions to average
   * @param channels channels to acquire
   * @return status of the operation
   */
  StatusCode acquire(JWaveForm waveForms[], int averageCount, int channels[]);

  /**
   * This method acquires a waveform.
   * @param waveform waveform data received from the instrument and its parameters
   * @param averageCount number of acquisitions to average
   * @return status of the operation
   */
  StatusCode acquire(JWaveForm waveform, int averageCount);

  /**
   * This method Acquires a waveform.
   * @param waveform waveform data received from the instrument and its parameters
   * @return status of the operation
   */
  StatusCode acquire(JWaveForm waveform);

  /**
   * This method reads the busy status from the instrument.
   * @return busy status
   */
  StatusCode getBusyStatus();

  /**
   * This method resets the instrument.
   * @param averageCount number of acquisitions for average waveform;
   * @param timeout Timeout after this many milliseconds.
   * @return status of the operation
   */
  StatusCode reset(int averageCount, int timeout);

  /**
   * This method resets the instrument with default values for average count 
   * and timeout.
   * @return reset status
   */
  StatusCode reset();

  /**
   * This method validates the value of the number of acquisitions to average.
   * @param count average count
   * @return success if count is a power of 2, otherwise error
   */
  StatusCode validateAverageCount(int count);

  /**
   * This method polls the busy status until the instrument indicates that it 
   * is ready or the timeout value is reached.
   * @param timeout stop waiting after this many milliseconds
   * @return busy status
   */
  StatusCode waitForReady(long timeout);
}
