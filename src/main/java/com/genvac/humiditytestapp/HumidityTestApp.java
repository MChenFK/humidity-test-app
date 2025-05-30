                                                                                                                                                                                                                              /*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.genvac.humiditytestapp;

/**
 *
 * @author mebry
 */
import com.fazecast.jSerialComm.SerialPort;
import java.awt.BorderLayout;
import java.awt.Component;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class HumidityTestApp {

    static SerialPort chosenPort;
    static int x = 0;
    static float temp;
    static float hum;
    static byte first;

    public static void main(String[] args) {
        JFrame window = new JFrame();
        window.setTitle("24 Hour Humidity");
        window.setSize(1920, 1080);
        window.setLayout(new BorderLayout());
        window.setDefaultCloseOperation(3);
        JComboBox<String> portList = new JComboBox<>();
        JButton connectButton = new JButton("Connect");
        JPanel topPanel = new JPanel();
        topPanel.add(portList);
        topPanel.add(connectButton);
        window.add(topPanel, "North");
        SerialPort[] portNames = SerialPort.getCommPorts();
        for (SerialPort portName : portNames) {
            portList.addItem(portName.getSystemPortName());
        }
        final XYSeries tempSeries = new XYSeries("Temperature");
        final XYSeries humSeries = new XYSeries("Humidity");
        XYSeriesCollection tempHumData = new XYSeriesCollection();
        tempHumData.addSeries(tempSeries);
        tempHumData.addSeries(humSeries);
        JFreeChart chart = ChartFactory.createXYLineChart("Temperature Humidity Readings", "Time (sec)", "Data", (XYDataset) tempHumData, PlotOrientation.VERTICAL, true, true, true);
        window.add((Component) new ChartPanel(chart), "Center");
        connectButton.addActionListener(arg0 -> {
            if (connectButton.getText().equals("Connect")) {
                LocalDate today = LocalDate.now();
                chosenPort = SerialPort.getCommPort(portList.getSelectedItem().toString());
                chosenPort.setComPortTimeouts(16, 0, 0);
                final InputStream in = chosenPort.getInputStream();
                final Path pLog = Paths.get("G://AA Operations- Quality Control//Humidity_24Hour//24HourHumidityLog//" + today.toString() + ".CSV", new String[0]);
                if (chosenPort.openPort()) {
                    connectButton.setText("Disconnect");
                    portList.setEnabled(false);
                }
                Thread thread = new Thread() {
                    public void run() {
                        try {
                            FileWriter filewriter = new FileWriter(pLog.toString());
                            try {
                                PrintWriter out = new PrintWriter(filewriter);
                                try {
                                    while (HumidityTestApp.x < 86400) {
                                        LocalTime thisSec = LocalTime.now();
                                        byte[] bufferRead = new byte[9];
                                        byte startFlag = 0X3C;
                                        in.read(bufferRead);
                                        first = ByteBuffer.wrap(bufferRead).order(ByteOrder.LITTLE_ENDIAN).get(0);
                                        HumidityTestApp.temp = ByteBuffer.wrap(bufferRead).order(ByteOrder.LITTLE_ENDIAN).getFloat(1);
                                        HumidityTestApp.hum = ByteBuffer.wrap(bufferRead).order(ByteOrder.LITTLE_ENDIAN).getFloat(5);

                                        if (startFlag == first) {

                                            if (HumidityTestApp.x == 0) {
                                                out.println("Time (min.), Timer, Temperature, (Humidity (%)");
                                                tempSeries.add(HumidityTestApp.x, HumidityTestApp.temp);
                                                humSeries.add(HumidityTestApp.x, HumidityTestApp.hum);
                                                out.println("" + thisSec + "," + HumidityTestApp.x + ", " + HumidityTestApp.temp + ", " + HumidityTestApp.hum);
                                                HumidityTestApp.x++;
                                            } else {
                                                tempSeries.add(HumidityTestApp.x, HumidityTestApp.temp);
                                                humSeries.add(HumidityTestApp.x, HumidityTestApp.hum);
                                                out.println("" + thisSec + "," + HumidityTestApp.x + ", " + HumidityTestApp.temp + ", " + HumidityTestApp.hum);
                                                HumidityTestApp.x++;
                                            }
                                        }
                                    }
                                    out.close();
                                } catch (Throwable throwable) {
                                    try {
                                        out.close();
                                    } catch (Throwable throwable1) {
                                        throwable.addSuppressed(throwable1);
                                    }
                                    throw throwable;
                                }
                                filewriter.close();
                            } catch (Throwable throwable) {
                                try {
                                    filewriter.close();
                                } catch (Throwable throwable1) {
                                    throwable.addSuppressed(throwable1);
                                }
                                throw throwable;
                            }
                        } catch (IOException e) {
                            System.err.println("An error occurred while writing to the file: " + e.getMessage());
                            // } catch (InterruptedException ex) {
                            //     Logger.getLogger(HumidityTestApp.class.getName()).log(Level.SEVERE, (String) null, ex);
                        }
                    }
                };
                thread.start();
            } else {
                chosenPort.closePort();
                portList.setEnabled(true);
                connectButton.setText("Connect");
                x = 0;
            }
        });
        window.setVisible(true);
    }
}
