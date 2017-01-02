package edu.spbau.master.java.bemchmark.servers.ui;

import edu.spbau.master.java.bemchmark.servers.app.ServerArchitecture;
import edu.spbau.master.java.bemchmark.servers.benchmark.BenchmarkResult;
import edu.spbau.master.java.bemchmark.servers.benchmark.BenchmarkSessionRunner;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.event.ItemEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ClientGUI extends JFrame {
    private final static int DEFAULT_CHART_WIDTH = 560;
    private final static int DEFAULT_CHART_HEIGHT = 300;

    private final static int MAIN_SERVER_PORT = 27527;
    private final static int TEST_SERVER_PORT = 27528;

    private JPanel rootPane;

    private JRadioButton clientCountRadioButton;
    private JRadioButton elementCountRadioButton;
    private JRadioButton delayTimeRadioButton;

    private JComboBox serverArchitectureComboBox;

    private JFormattedTextField clientCountTextField;
    private JFormattedTextField requestPerClientTextField;
    private JFormattedTextField elementCountTextField;
    private JFormattedTextField delayTimeTextField;
    private JFormattedTextField startValueTextField;
    private JFormattedTextField finishValueTextField;
    private JFormattedTextField stepValueTextField;


    private JPanel clientProcessTimeChartPane;
    private JPanel requestProcessTimeChartPane;
    private JPanel clientWorkTimeChartPane;

    private volatile CompletableFuture<Void> curFuture;

    private JButton startButton;
    private JButton cancelButton;

    private DefaultCategoryDataset clientProcesingTimeDataset;
    private DefaultCategoryDataset requestProcesingTimeDataset;
    private DefaultCategoryDataset workTimeDataset;

    @NotNull
    private final AtomicBoolean isInProgress = new AtomicBoolean();


    private ClientGUI() throws InterruptedException {
        super("Server benchmark");
        setContentPane(rootPane);
        pack();

        ButtonGroup radioButtonGroup = new ButtonGroup();

        addRadioListener(clientCountRadioButton, clientCountTextField);
        addRadioListener(elementCountRadioButton, elementCountTextField);
        addRadioListener(delayTimeRadioButton, delayTimeTextField);

        clientCountRadioButton.setSelected(true);
        cancelButton.setEnabled(false);

        radioButtonGroup.add(clientCountRadioButton);
        radioButtonGroup.add(elementCountRadioButton);
        radioButtonGroup.add(delayTimeRadioButton);


        startButton.addActionListener(e -> {
            CompletableFuture.runAsync(() -> {
                if (isInProgress.compareAndSet(false, true)) {
                    try {
                        startButton.setEnabled(false);
                        clientProcesingTimeDataset.clear();
                        requestProcesingTimeDataset.clear();
                        workTimeDataset.clear();

                        int requestCount = parseIntParameter(
                                requestPerClientTextField,
                                "Request per client",
                                1,
                                Integer.MAX_VALUE);
                        if (requestCount == -1) {
                            return;
                        }


                        BenchmarkSessionRunner sessionRunner = BenchmarkSessionRunner.builder()
                                .mainServerPort(MAIN_SERVER_PORT)
                                .testServerPort(TEST_SERVER_PORT)
                                .requestCount(requestCount)
                                .serverArchitecture((ServerArchitecture) serverArchitectureComboBox.getSelectedItem())
                                .serverAddress(getServerAddress())
                                .onResult(result -> {
                                    BenchmarkResult benchmarkResult = result.getBenchmarkResult();
                                    clientProcesingTimeDataset.addValue(benchmarkResult.getAverageClientProcessingTime(), "time", String.valueOf(result.getVariableValue()));
                                    requestProcesingTimeDataset.addValue(benchmarkResult.getAverageRequestProcessingTime(), "time", String.valueOf(result.getVariableValue()));
                                    workTimeDataset.addValue(benchmarkResult.getAverageWorkTime(), "time", String.valueOf(result.getVariableValue()));
                                })
                                .build();

                        int startValue = parseIntParameter(startValueTextField, "Start value", 1, Integer.MAX_VALUE);
                        if (startValue == -1) {
                            return;
                        }

                        int finishValue = parseIntParameter(finishValueTextField, "Finish value", startValue, Integer.MAX_VALUE);
                        if (finishValue == 1) {
                            return;
                        }

                        int stepValue = parseIntParameter(stepValueTextField, "Step value", 1, Integer.MAX_VALUE);
                        if (stepValue == -1) {
                            return;
                        }


                        if (clientCountRadioButton.isSelected()) {
                            int elementPerRequest = parseIntParameter(elementCountTextField, "Element per request", 1, Integer.MAX_VALUE);
                            if (elementPerRequest == 1) {
                                return;
                            }

                            int delayBetweenRequest = parseIntParameter(delayTimeTextField, "Delay between request", 1, Integer.MAX_VALUE);
                            if (delayBetweenRequest == -1) {
                                return;
                            }

                            curFuture = sessionRunner.runWithVariableClientCount(startValue, finishValue, stepValue, elementPerRequest, delayBetweenRequest);
                        } else if (delayTimeRadioButton.isSelected()) {
                            int clientCount = parseIntParameter(clientCountTextField, "Client count", 1, Integer.MAX_VALUE);
                            if (clientCount == -1) {
                                return;
                            }

                            int elementPerRequest = parseIntParameter(elementCountTextField, "Element per request", 1, Integer.MAX_VALUE);
                            if (elementPerRequest == 1) {
                                return;
                            }

                            curFuture = sessionRunner.runWithVariableDelayBetweenRequest(startValue, finishValue, stepValue, clientCount, elementPerRequest);
                        } else if (elementCountRadioButton.isSelected()) {
                            int elementPerRequest = parseIntParameter(elementCountTextField, "Element per request", 1, Integer.MAX_VALUE);
                            if (elementPerRequest == -1) {
                                return;
                            }

                            int delayBetweenRequest = parseIntParameter(delayTimeTextField, "Delay between request", 1, Integer.MAX_VALUE);
                            if (delayBetweenRequest == -1) {
                                return;
                            }

                            curFuture = sessionRunner.runWithVariableClientCount(startValue, finishValue, stepValue, elementPerRequest, delayBetweenRequest);
                        }
                        cancelButton.setEnabled(true);

                        curFuture.thenRunAsync(() -> {
                            isInProgress.set(false);
                            cancelButton.setEnabled(false);
                            startButton.setEnabled(true);
                        });
                    } catch (UnknownHostException e1) {
                        log.error("Exception during benchmark session start");
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Benchmark already in progress!");
                }
            });
        });

        cancelButton.addActionListener(e -> {
            if (isInProgress.get() && curFuture != null) {
                curFuture.complete(null);
            }
        });

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        setVisible(true);
    }

    private int parseIntParameter(@NotNull
                                  final JFormattedTextField textField,
                                  @NotNull
                                  final String parameterName,
                                  final int minValue,
                                  final int maxValue) {
        int value;
        try {
            value = (int) textField.getValue();
        } catch (Exception ex) {
            log.error("Failed to parse request count", ex);
            JOptionPane.showMessageDialog(this, String.format("Invalid %s format.", parameterName));
            return -1;
        }

        if (value < minValue) {
            JOptionPane.showMessageDialog(this, String.format("%s count must be more than %d.", parameterName, minValue));
            return -1;
        }
        if (value > maxValue) {
            JOptionPane.showMessageDialog(this, String.format("%s count must be less than %d.", parameterName, maxValue));
            return -1;
        }

        return value;
    }


    @NotNull
    private InetAddress getServerAddress() throws UnknownHostException {
        return InetAddress.getLocalHost();
    }

    private void addRadioListener(@NotNull final JRadioButton radioButton, @NotNull final JFormattedTextField textField) {
        radioButton.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                textField.setEnabled(false);
            }

            if (e.getStateChange() == ItemEvent.DESELECTED) {
                textField.setEnabled(true);
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        new ClientGUI();
    }

    private void createUIComponents() {
        NumberFormat format = NumberFormat.getInstance();
        NumberFormatter formatter = new NumberFormatter(format);
        formatter.setValueClass(Integer.class);
        formatter.setMinimum(0);
        formatter.setMaximum(Integer.MAX_VALUE);
        formatter.setAllowsInvalid(false);


        clientCountTextField = new JFormattedTextField(formatter);
        clientCountTextField.setValue(10);
        elementCountTextField = new JFormattedTextField(formatter);
        elementCountTextField.setValue(3000);
        requestPerClientTextField = new JFormattedTextField(formatter);
        requestPerClientTextField.setValue(10);
        delayTimeTextField = new JFormattedTextField(formatter);
        delayTimeTextField.setValue(100);
        startValueTextField = new JFormattedTextField(formatter);
        startValueTextField.setValue(10);
        finishValueTextField = new JFormattedTextField(formatter);
        finishValueTextField.setValue(100);
        stepValueTextField = new JFormattedTextField(formatter);
        stepValueTextField.setValue(10);

        serverArchitectureComboBox = new JComboBox();
        serverArchitectureComboBox.setModel(new DefaultComboBoxModel(ServerArchitecture.values()));

        ChartInfo clientChartInfo = addChart("Client processing time");
        clientProcesingTimeDataset = clientChartInfo.dataset;
        clientProcessTimeChartPane = clientChartInfo.chartPanel;

        ChartInfo requestChartInfo = addChart("Request processing time");
        requestProcesingTimeDataset = requestChartInfo.dataset;
        requestProcessTimeChartPane = requestChartInfo.chartPanel;

        ChartInfo wokTimeChartInfo = addChart("Client work time");
        workTimeDataset = wokTimeChartInfo.dataset;
        clientWorkTimeChartPane = wokTimeChartInfo.chartPanel;
    }

    @NotNull
    private ChartInfo addChart(
            @NotNull
            final String chartTitle) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        JFreeChart lineChart = ChartFactory.createLineChart(
                chartTitle,
                "", "Time",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);
        ChartPanel chartPanel = new ChartPanel(lineChart);
        chartPanel.setPreferredSize(new java.awt.Dimension(DEFAULT_CHART_WIDTH, DEFAULT_CHART_HEIGHT));
        return new ChartInfo(chartPanel, dataset);
    }

    @Data
    private static final class ChartInfo {
        @NotNull
        private final ChartPanel chartPanel;
        @NotNull
        private final DefaultCategoryDataset dataset;
    }
}
