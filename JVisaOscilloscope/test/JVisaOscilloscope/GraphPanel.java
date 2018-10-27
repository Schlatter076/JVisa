/*
 * Copyright 2018 Günter Fuchs (gfuchs@acousticmicroscopy.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package JVisaOscilloscope;

import java.awt.*;
import javax.swing.*;

/**
 * @author Günter Fuchs (gfuchs@acousticmicroscopy.com)
 */
public class GraphPanel extends JPanel {
  /** waveform object */
  protected JWaveForm graphData;
  /** bounds of panel */
  protected Rectangle bounds;
  /** 2D canvas */
  protected Graphics2D g2d;
  /** data points of waveform */
  protected double data[];
  /** time difference between adjacent data points */
  protected double xIncrement;
  /** vertical scale factor */
  protected double yScale;
  /** color of drawn waveform */
  protected Color waveFormColor = Color.black;
  /** number of data points */
  protected int dataSize;
  /** data index from where to start drawing the waveform */
  protected int dataStartIndex;
  /** data index at which to stop drawing the waveform */
  protected int dataEndIndex;
  /** minimum amplitude */
  protected double yMin;
  /** maximum amplitude */
  protected double yMax;


  /**
   * This getter method gets the time delta of the waveform.
   * @return time delta of waveform
   */
  public double getxIncrement() {
    return xIncrement;
  }


  /**
   * This getter method gets the vertical scale factor.
   * @return vertical scale factor 
   */
  public double getyScale() {
    return yScale;
  }
  
  
  /**
   * This method retrieves the amplitude at the current cursor position.
   * @param xGraph x position of cursor in graph panel
   * @return amplitude of waveform
   */
  public int getY(int xGraph)  {
    try {
      int index = (int) ((double) xGraph / xIncrement + 0.5);
      return (int) ((yMax - data[index]) * yScale + 0.5);
    }
    catch (Exception e) {
      System.err.println(e.toString());
    }
    return -1;
  }

  
  /** 
   * This method supplies information about the data to be displayed. 
   * @param waveform #FileData object
   * @return status of the operation
   */
  public JInstrument_TDS3000.StatusCode setData(JWaveForm waveform) {
    JInstrument_TDS3000.StatusCode status = JInstrument_TDS3000.StatusCode.SUCCESS;
    
    if (waveform == null) {
      return JInstrument_TDS3000.StatusCode.WAVEFORM_ERROR;
    }
    
    if (waveform.dataSize <= 0) {
      return JInstrument_TDS3000.StatusCode.WAVEFORM_ERROR;
    }
    // Arrange for background to be automatically drawn in the background color.
    setOpaque(true);
    graphData = waveform;
    yMin = graphData.min;
    yMax = graphData.max;
    dataSize = graphData.dataSize;
    dataStartIndex = 0;
    dataEndIndex = dataSize - 1;
    repaint();
    return status;
  }

  
  /** 
   * This method supplies information about the data to be displayed. 
   * @param data data array
   * @param xIncrement delta x
   * @return status of the operation
   */
  public JInstrument_TDS3000.StatusCode setData(double[] data, double xIncrement) {
    JInstrument_TDS3000.StatusCode status = JInstrument_TDS3000.StatusCode.SUCCESS;
    
    // Validate arguments.
    if (data == null || xIncrement <= 0.0) {
      return JInstrument_TDS3000.StatusCode.WAVEFORM_ERROR;
    }
    
    if (data.length == 0) {
      return JInstrument_TDS3000.StatusCode.WAVEFORM_ERROR;
    }
    
    // Initialize local variables.
    int lengthDivisor = 4;
    this.data = data;
    this.xIncrement = xIncrement;
    dataSize = data.length / lengthDivisor;
    dataStartIndex = 0;
    dataEndIndex = dataSize - 1;
    setMinMax();
    
    // Initialize graphData.
    graphData = new JWaveForm();
    graphData.dataSize = dataSize;
    graphData.data = data;
    graphData.max = yMax;
    
    // Arrange for background to be automatically drawn in the background color.
    setOpaque(true);
    repaint();
    return status;
  }
  
  
  /** 
   * This method supplies information about the data to be displayed. 
   * @param data data array
   * @return status of the operation
   * @todo Why to set xIncrement? It is calculated in paintWaveform.
   */
  public JInstrument_TDS3000.StatusCode setData(double[] data) {
    return setData(data, 1.0);
  }

  
  /**
   * This method obtains the minimum and maximum amplitude.
   */
  public void setMinMax() {
    yMin = yMax = data[dataStartIndex];
    for (int i = dataStartIndex + 1; i <= dataEndIndex; i++) {
      if (data[i] >= yMax) {
        yMax = data[i];
      }
      else if (data[i] < yMin) {
        yMin = data[i];
      }
    }
  }

  
  /** 
   * This method paints the wave form.
   */
  protected void paintWaveform() {
    try {
      if (bounds == null) {
        return;
      }

      int i, j = 0;
      int[] x = new int[dataSize];
      int[] y = new int[dataSize];
      xIncrement = (double) bounds.width / (double) dataSize;
      data = graphData.data;
      if (dataStartIndex > 0 && dataEndIndex < graphData.dataSize - 1) {
        setMinMax();
      }
      yScale =  (double) bounds.height / (Math.abs(yMax) + Math.abs(yMin));

      for (i = dataStartIndex; i <= dataEndIndex; i++, j++) {
        x[j] = (int) (xIncrement * (double) j + .5);
        y[j] = (int) ((yMax - data[i]) * yScale);
      }
      g2d.drawPolyline(x, y, dataSize);
    }
    catch (Exception exception) {
      System.err.println(exception.toString());
    }
  }


  /**
   * This method prepares the canvas.
   * @param g graphics object
   */
  protected void prepareGraphics(Graphics g) {
    if (graphData == null) {
      return;
    }
    super.paintComponent(g);
    g2d = (Graphics2D) g;
    bounds = getBounds();
  }


  /**
   * This method paints the graphics component.
   * @param g graphics object
   */
  @Override public void paintComponent(Graphics g)
  {
    prepareGraphics(g);
    paintWaveform();
  }
}