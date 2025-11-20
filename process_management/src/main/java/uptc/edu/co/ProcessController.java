package uptc.edu.co;

import uptc.edu.co.model.FileMutexManager;
import uptc.edu.co.model.Process;
import uptc.edu.co.scheduling.*;
import uptc.edu.co.memory.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ProcessController {

    @FXML private TextField cpuUseProcessText;
    @FXML private TextField durationProcessText;
    @FXML private TextField fileAccessProcessText;
    @FXML private VBox fileManagerVBox;
    @FXML private TextField memoryProcessText;
    @FXML private TextField nameProcessText;
    @FXML private ComboBox<String> pageReplacerComboBox;
    @FXML private VBox paginationVBox;
    @FXML private TextField priorityProcessText;
    @FXML private AnchorPane processConfigPanel;
    @FXML private Label processConfigTabLabel;
    @FXML private Line processConfigTabLine;
    @FXML private ComboBox<Process> processQueueComboBox;
    @FXML private VBox processSchedulerVBox;
    @FXML private Button resetSimulationButton;
    @FXML private ComboBox<String> schedulerComboBox;
    @FXML private AnchorPane simulationPanel;
    @FXML private Line simulationTabLine;
    @FXML private Label simulationTabText;
    @FXML private Button stopSimulationButton;

    @FXML private AnchorPane metricsPanel;
    @FXML private VBox metricsVBox;
    @FXML private ScrollPane metricsScrollPane;
    @FXML private Button showMetricsButton;
    @FXML private Label metricsTitleLabel;

    private List<Process> processQueue;
    private Map<Integer, Integer> processQuantumCounters = new HashMap<>();
    private ProcessScheduler currentScheduler;
    private MemoryManager memoryManager;
    private boolean simulationRunning;
    private int simulationTime;
    private Thread simulationThread;
    private ScrollPane memoryScrollPane;
    private VBox memoryContent;
    private double lastScrollPosition = 0.0;

    private FileMutexManager fileMutexManager;
    private Map<Process, Pane> processFilePanes;
    private VBox fileAccessPanel;
    private Label currentFileUserLabel;
    private Label mutexTimeLabel;

    @FXML
    public void initialize() {
        processQueue = new ArrayList<>();
        simulationRunning = false;
        simulationTime = 0;
        
        initializeComboBoxes();
        updateProcessQueueComboBox();
        initializeMemoryScrollPane();
        initializeStyles();
        metricsPanel.setVisible(false);
        showMetricsButton.setVisible(false);

        fileMutexManager = FileMutexManager.getInstance();
        processFilePanes = new ConcurrentHashMap<>();
        initializeFileManagerPanel();
    }

    private void initializeFileManagerPanel() {
        fileManagerVBox.getChildren().clear();
        
        Label filesTitle = createLabel("FILE MANAGER - MUTEX", "#f268ff", 16);
        fileManagerVBox.getChildren().add(filesTitle);
        
        VBox fileStatusPanel = createFileStatusPanel();
        fileManagerVBox.getChildren().add(fileStatusPanel);
        
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #f268ff;");
        fileManagerVBox.getChildren().add(separator);
        
        Label processesTitle = createLabel("PROCESSES WITH ACCESS:", "#ffff00", 14);
        fileManagerVBox.getChildren().add(processesTitle);
        
        fileAccessPanel = new VBox(5);
        fileAccessPanel.setStyle("-fx-padding: 10;");
        fileManagerVBox.getChildren().add(fileAccessPanel);
        
        updateFileManagerUI();
        
    }

    private VBox createFileStatusPanel() {
        VBox statusPanel = new VBox(5);
        statusPanel.setStyle("-fx-border-color: #00ff00; -fx-border-width: 2; -fx-padding: 10; -fx-background-color: #2d2d44;");
        
        Label statusTitle = createLabel("FILE STATE:", "#00ff00", 14);
        statusPanel.getChildren().add(statusTitle);
        
        currentFileUserLabel = createLabel("FREE", "white", 12);
        mutexTimeLabel = createLabel("", "#ffff00", 12);
        
        statusPanel.getChildren().addAll(currentFileUserLabel, mutexTimeLabel);
        
        return statusPanel;
    }


    private void updateFileManagerUI() {
        fileMutexManager.forceTimeUpdate();
        
        if (currentScheduler == null) {
            currentFileUserLabel.setText("File Free");
            currentFileUserLabel.setTextFill(Color.web("#00ff00"));
            mutexTimeLabel.setText("");
            
            fileAccessPanel.getChildren().clear();
            Label noProcessesLabel = createLabel("No Active Simulation", "#666666", 12);
            fileAccessPanel.getChildren().add(noProcessesLabel);
            return;
        }
        
        updateFileStatusPanel();
        
        updateProcessFilePanels();
    }

    private void updateFileStatusPanel() {
        fileMutexManager.forceTimeUpdate();
        
        if (fileMutexManager.isFileInUse()) {
            Process currentUser = fileMutexManager.getCurrentProcess();
            int remainingTime = fileMutexManager.getRemainingMutexTime();
            
            currentFileUserLabel.setText("Using by: " + currentUser.getName());
            currentFileUserLabel.setTextFill(Color.web("#ff4444"));
            mutexTimeLabel.setText("Remaining time: " + remainingTime + " s");
        } else {
            currentFileUserLabel.setText("File free");
            currentFileUserLabel.setTextFill(Color.web("#00ff00"));
            mutexTimeLabel.setText("");
        }
    }


    private void updateProcessFilePanels() {
        fileAccessPanel.getChildren().clear();
        processFilePanes.clear();
        
        
        if (currentScheduler == null) {
            Label noSimulationLabel = createLabel("No active simulation", "#666666", 12);
            fileAccessPanel.getChildren().add(noSimulationLabel);
            return;
        }
        
        List<Process> fileProcesses = getProcessesWithFileAccess();
                
        fileProcesses.sort((p1, p2) -> {
            Process current = currentScheduler.getCurrentProcess();
            if (current != null) {
                if (p1.equals(current)) return -1;
                if (p2.equals(current)) return 1;
            }
            return p1.getName().compareTo(p2.getName());
        });
        
        for (Process process : fileProcesses) {
            Pane processPane = createProcessFilePane(process);
            fileAccessPanel.getChildren().add(processPane);
            processFilePanes.put(process, processPane);
        }
        
        if (fileProcesses.isEmpty()) {
            Label noAccessLabel = createLabel("No processes with fie access", "#666666", 12);
            fileAccessPanel.getChildren().add(noAccessLabel);
        }
    }

    private void showFinalMetrics() {
        processConfigPanel.setVisible(false);
        simulationPanel.setVisible(false);
        
        stopSimulationButton.setVisible(false);
        resetSimulationButton.setVisible(false);
        
        metricsPanel.setVisible(true);
        showMetricsButton.setVisible(true);
        
        metricsScrollPane.setContent(new VBox());
        
        generateAndDisplayMetrics();
    }

    private Pane createProcessFilePane(Process process) {
        Pane pane = new Pane();
        pane.setStyle("-fx-background-color: #251F35; -fx-border-color: #444; -fx-border-radius: 5; -fx-padding: 10;");
        pane.setPrefHeight(100);
        pane.setPrefWidth(280);

        VBox content = new VBox(5);
        
        VBox infoBox = new VBox(5);
        Label nameLabel = createLabel(process.getName() + "-PID: " + process.getId(), "#f268ff", 12);
        
        String processType = "";
        if (currentScheduler != null) {
            if (currentScheduler.getCurrentProcess() != null && 
                currentScheduler.getCurrentProcess().equals(process)) {
                processType = " (Executing)";
            } else if (currentScheduler.getReadyQueue().contains(process)) {
                processType = " (ON QUEUE)";
            } else {
                processType = " (READY)";
            }
        }
        
        Label typeLabel = createLabel(processType, "#ffff00", 10);
        
        String accessType = process.needsFileAccess() ? 
            (process.getFileAccessType() != null && process.getFileAccessType().equals("w") ? "Write Only" : "Read/Write") : 
            "Read Only";
        
        Label accessLabel = createLabel(accessType, "#cccccc", 10);
        infoBox.getChildren().addAll(nameLabel, typeLabel, accessLabel);
        
        Label mutexStateLabel = createLabel(getMutexStateForProcess(process), getMutexStateColor(process), 11);
        
        Button accessButton = new Button("Request Access");
        accessButton.setStyle("-fx-background-color: #00aa00; -fx-text-fill: white; -fx-font-size: 10; -fx-padding: 3 8 3 8;");
        
        accessButton.setOnAction(e -> handleFileAccessRequest(process));
        
        updateAccessButtonState(accessButton, process);
        
        content.getChildren().addAll(infoBox, mutexStateLabel, accessButton);
        pane.getChildren().add(content);
        
        return pane;
    }

    private String getMutexStateForProcess(Process process) {
        if (process == null) {
            return "INVALID";
        }
        
        if (fileMutexManager.getCurrentProcess() != null && 
            fileMutexManager.getCurrentProcess().equals(process)) {
            return "USING FILE - " + fileMutexManager.getRemainingMutexTime() + "s remaining";
        } else if (fileMutexManager.getWaitingProcesses().containsKey(process)) {
            return "WAITING MUTEX - ON QUEUE";
        } else {
            return "READY TO REQUEST";
        }
    }

    private String getMutexStateColor(Process process) {
        if (fileMutexManager.getCurrentProcess() != null && 
            fileMutexManager.getCurrentProcess().equals(process)) {
            return "#ff4444";
        } else if (fileMutexManager.getWaitingProcesses().containsKey(process)) {
            return "#ffff00"; 
        } else {
            return "#00ff00"; 
        }
    }

    private void updateAccessButtonState(Button button, Process process) {
        if (process == null) {
            button.setText("Error");
            button.setDisable(true);
            button.setStyle("-fx-background-color: #666666; -fx-text-fill: #cccccc; -fx-font-size: 10; -fx-padding: 3 8 3 8;");
            return;
        }
        
        if (fileMutexManager.getCurrentProcess() != null && 
            fileMutexManager.getCurrentProcess().equals(process)) {
            button.setText("Extend (+5s)");
            button.setStyle("-fx-background-color: #ffff00; -fx-text-fill: black; -fx-font-size: 10; -fx-padding: 3 8 3 8;");
        } else if (fileMutexManager.getWaitingProcesses().containsKey(process)) {
            button.setText("Waiting...");
            button.setDisable(true);
            button.setStyle("-fx-background-color: #666666; -fx-text-fill: #cccccc; -fx-font-size: 10; -fx-padding: 3 8 3 8;");
        } else {
            button.setText("Access");
            button.setDisable(false);
            button.setStyle("-fx-background-color: #00aa00; -fx-text-fill: white; -fx-font-size: 10; -fx-padding: 3 8 3 8;");
        }
    }

    private void handleFileAccessRequest(Process process) {
        if (process == null) {
            showError("File", "Process invalid");
            return;
        }
        
        fileMutexManager.forceTimeUpdate();
        
        if (fileMutexManager.getCurrentProcess() != null && 
            fileMutexManager.getCurrentProcess().equals(process)) {
            fileMutexManager.extendAccess(process);
        } else {
            fileMutexManager.requestAccess(process);
        }
        
        updateFileManagerUI();
    }


    @FXML
    void resetFromMetrics(ActionEvent event) {
        metricsPanel.setVisible(false);
        showMetricsButton.setVisible(false);
        
        changeToSimulation(null);
        clearSimulationUI();
    }

    private void generateAndDisplayMetrics() {
        VBox metricsContent = new VBox(10);
        metricsContent.setStyle("-fx-padding: 20;");
        
        metricsTitleLabel.setText("SIMULATION METRICS - " + 
            (currentScheduler != null ? currentScheduler.getAlgorithmName() : "COMPLETED"));
        
        addGeneralMetrics(metricsContent);
        
        addProcessMetricsTable(metricsContent);
        
        addMemoryMetrics(metricsContent);
        
        metricsScrollPane.setContent(metricsContent);
    }

    private void addProcessMetricsTable(VBox container) {
        VBox tableBox = new VBox(5);
        tableBox.setStyle("-fx-border-color: #00ff00; -fx-border-width: 2; -fx-padding: 10; -fx-background-color: #2d2d44;");
        
        Label title = createLabel("METRICS BY PROCESS (ONLY COMPLETED)", "#00ff00", 14);
        tableBox.getChildren().add(title);
        
        GridPane metricsTable = createMetricsTable();
        tableBox.getChildren().add(metricsTable);
        
        container.getChildren().add(tableBox);
    }

    private GridPane createMetricsTable() {
        GridPane table = new GridPane();
        table.setHgap(10);
        table.setVgap(5);
        table.setStyle("-fx-padding: 10;");
        
        String[] headers = {"Process", "CPU Used", "Memory", "Files", "State", "T. Waiting", "T. Return", "RAM Pages"};
        
        for (int i = 0; i < headers.length; i++) {
            Label header = createLabel(headers[i], "#f268ff", 12);
            header.setStyle("-fx-font-weight: bold; -fx-border-color: #f268ff; -fx-border-width: 0 0 1 0; -fx-padding: 5;");
            table.add(header, i, 0);
        }
        
        List<Process> completedProcesses = getAllSimulationProcesses().stream()
                .filter(Process::isFinished)
                .sorted(Comparator.comparingInt(Process::getId))
                .collect(Collectors.toList());
        
        int row = 1;
        for (Process process : completedProcesses) {
            addProcessRow(table, process, row);
            row++;
        }
        
        if (completedProcesses.isEmpty()) {
            Label noData = createLabel("No processes completed", "white", 12);
            table.add(noData, 0, 1, headers.length, 1);
        }
        
        return table;
    }

   
    private void addProcessRow(GridPane table, Process process, int row) {
        String stateColor = process.isFinished() ? "#00ff00" : "#ffff00";
        String stateText = process.isFinished() ? "COMPLETED" : 
                        (process.getState() == Process.ProcessState.RUNNING ? "EXECUTING" : "READY");
        
        int cpuUsed = process.getCpuTime(); 
        int memoryUsed = process.getMemoryUsage();
        int maxMemory = process.getMaxMemory();
        String fileAccess = process.getFileAccessType().equals("r&w") ? "Read/Write" : 
                   process.getFileAccessType().equals("r") ? "Read Only" : 
                   process.getFileAccessType().equals("w") ? "Write Only" : 
                   "Unknown";
        
        int waitingTime = process.getWaitingTime(simulationTime);
        
        int turnaroundTime;
        if (process.isFinished()) {
            turnaroundTime = process.getTurnaroundTime();
        } else {
            turnaroundTime = simulationTime;
        }
        
        String pagesInfo = process.getPagesInMemory() + "/" + process.getMaxPages();
        
        Label[] cells = {
            createLabel(process.getName(), "white", 11),
            createLabel(cpuUsed + " cycles", "#cccccc", 11),
            createLabel(memoryUsed + "/" + maxMemory + "KB", "#cccccc", 11),
            createLabel(fileAccess, "#cccccc", 11),
            createLabel(stateText, stateColor, 11),
            createLabel(waitingTime + " cycles", "#cccccc", 11),
            createLabel(turnaroundTime + " cycles", "#cccccc", 11),
            createLabel(pagesInfo, process.getPagesInMemory() > 0 ? "#00ff00" : "#ff4444", 11)
        };
        
        for (int col = 0; col < cells.length; col++) {
            String bgColor = (row % 2 == 0) ? "#251F35" : "#2d2d44";
            cells[col].setStyle(cells[col].getStyle() + " -fx-background-color: " + bgColor + "; -fx-padding: 5;");
            table.add(cells[col], col, row);
        }
    }



    private void addGeneralMetrics(VBox container) {
        VBox generalBox = new VBox(5);
        generalBox.setStyle("-fx-border-color: #f268ff; -fx-border-width: 2; -fx-padding: 10; -fx-background-color: #2d2d44;");
        
        Label title = createLabel("SISTEM GENERAL METRICS", "#f268ff", 14);
        generalBox.getChildren().add(title);
        
        int totalProcesses = processQueue.size();
        int completedProcesses = getCompletedProcessesCount();
        double cpuUtilization = calculateCPUUtilization();
        double avgWaitingTime = calculateAverageWaitingTime();
        double avgTurnaroundTime = calculateAverageTurnaroundTime();
        double throughput = calculateThroughput();
        
        generalBox.getChildren().addAll(
            createMetricLabel("Total Processes: " + totalProcesses, "white"),
            createMetricLabel("Processes completed: " + completedProcesses, "white"),
            createMetricLabel("CPU utilization: " + String.format("%.2f%%", cpuUtilization), "white"),
            createMetricLabel("Average waiting time: " + String.format("%.2f cycles", avgWaitingTime), "white"),
            createMetricLabel("Average return time: " + String.format("%.2f cycles", avgTurnaroundTime), "white"),
            createMetricLabel("Throughput: " + String.format("%.3f process/cycle", throughput), "white"),
            createMetricLabel("Total Duration: " + simulationTime + " cycles", "white")
        );
        
        container.getChildren().add(generalBox);
    }

    


    private void addMemoryMetrics(VBox container) {
        if (memoryManager == null) return;
        
        VBox memoryBox = new VBox(5);
        memoryBox.setStyle("-fx-border-color: #00aaff; -fx-border-width: 2; -fx-padding: 10; -fx-background-color: #2d2d44;");
        
        Label title = createLabel("MEMORY METRICS", "#00aaff", 14);
        memoryBox.getChildren().add(title);
        
        int pageFaults = memoryManager.getPageFaults();
        int pageHits = memoryManager.getPageHits();
        double faultRate = memoryManager.getPageFaultRate();
        int occupiedFrames = getOccupiedFramesCount();
        
        memoryBox.getChildren().addAll(
            createMetricLabel("Page faults: " + pageFaults, "white"),
            createMetricLabel("Page hits: " + pageHits, "white"),
            createMetricLabel("Failure rate: " + String.format("%.2f%%", faultRate * 100), "white"),
            createMetricLabel("Occupied Frames: " + occupiedFrames + "/8 (" + (occupiedFrames * 100/8) + "%)", "white"),
            createMetricLabel("Replace algorithm: " + memoryManager.getReplacementAlgorithm().getName(), "white")
        );
        
        container.getChildren().add(memoryBox);
    }

    private Label createMetricLabel(String text, String color) {
        Label label = new Label(text);
        label.setTextFill(Color.web(color));
        label.setStyle("-fx-font-family: 'Cascadia Code Regular'; -fx-font-size: 11;");
        label.setWrapText(true);
        return label;
    }


    private int getCompletedProcessesCount() {
        int count = 0;
        for (Process process : getAllSimulationProcesses()) {
            if (process.isFinished()) {
                count++;
            }
        }
        return count;
    }

    private double calculateCPUUtilization() {
        if (simulationTime == 0) return 0.0;
        int totalCpuTime = 0;
        for (Process process : getAllSimulationProcesses()) {
            totalCpuTime += process.getCpuTime();
        }
        return (double) totalCpuTime / simulationTime * 100;
    }

    private double calculateAverageWaitingTime() {
        List<Process> processes = getAllSimulationProcesses();
        if (processes.isEmpty()) return 0.0;
        
        double total = 0;
        int count = 0;
        for (Process process : processes) {
            int waitingTime = process.getWaitingTime(simulationTime);
            total += waitingTime;
            count++;
        }
        return count > 0 ? total / count : 0.0;
    }

    private double calculateAverageTurnaroundTime() {
        List<Process> completedProcesses = getAllSimulationProcesses().stream()
                .filter(Process::isFinished)
                .toList();
        
        if (completedProcesses.isEmpty()) return 0.0;
        
        double total = 0;
        for (Process process : completedProcesses) {
            int turnaround = process.getTurnaroundTime();
            total += turnaround;
        }
        return total / completedProcesses.size();
    }

    private double calculateThroughput() {
        if (simulationTime == 0) return 0.0;
        int completed = getCompletedProcessesCount();
        return (double) completed / simulationTime;
    }


    private List<Process> getAllSimulationProcesses() {
        Set<Process> allProcesses = new HashSet<>();
        
        allProcesses.addAll(processQueue);
        
        if (currentScheduler != null) {
            allProcesses.addAll(currentScheduler.getReadyQueue());
            if (currentScheduler.getCurrentProcess() != null) {
                allProcesses.add(currentScheduler.getCurrentProcess());
            }
            
            if (currentScheduler instanceof BaseScheduler) {
                BaseScheduler baseScheduler = (BaseScheduler) currentScheduler;
                try {
                    java.lang.reflect.Field completedField = BaseScheduler.class.getDeclaredField("completedProcesses");
                    completedField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    List<Process> completed = (List<Process>) completedField.get(baseScheduler);
                    allProcesses.addAll(completed);
                } catch (Exception e) {
                    System.out.println("Could not get the processes by scheduler");
                }
            }
        }
        
        return new ArrayList<>(allProcesses);
    }

    private int getOccupiedFramesCount() {
        if (memoryManager == null) return 0;
        int count = 0;
        for (PageFrame frame : memoryManager.getPageFrames()) {
            if (frame.isOccupied()) {
                count++;
            }
        }
        return count;
    }


    private void initializeStyles() {
        applyWhiteTextStyle(nameProcessText);
        applyWhiteTextStyle(priorityProcessText);
        applyWhiteTextStyle(durationProcessText);
        applyWhiteTextStyle(cpuUseProcessText);
        applyWhiteTextStyle(memoryProcessText);
        applyWhiteTextStyle(fileAccessProcessText);
        
        applyProcessComboStyle(processQueueComboBox);
        applyWhiteComboStyle(schedulerComboBox);
        applyWhiteComboStyle(pageReplacerComboBox);
    }

    private void applyWhiteTextStyle(TextField textField) {
        if (textField != null) {
            String existingStyle = textField.getStyle();
            textField.setStyle(existingStyle + 
                " -fx-text-fill: white; " +
                " -fx-prompt-text-fill: #cccccc;");
        }
    }

    private void applyWhiteComboStyle(ComboBox<String> comboBox) {
        if (comboBox != null) {
            String existingStyle = comboBox.getStyle();
            comboBox.setStyle(existingStyle + 
                " -fx-text-fill: white; " +
                " -fx-prompt-text-fill: #cccccc;");
            
            comboBox.setCellFactory(lv -> new ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("-fx-background-color: #251F35;");
                    } else {
                        setText(item);
                        setTextFill(Color.WHITE);
                        setStyle("-fx-background-color: #251F35; -fx-border-color: #f268ff;");
                    }
                }
            });
            
            comboBox.setButtonCell(new ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(comboBox.getPromptText());
                        setTextFill(Color.web("#cccccc"));
                    } else {
                        setText(item);
                        setTextFill(Color.WHITE);
                    }
                    setStyle("-fx-background-color: #251F35;");
                }
            });
        }
    }

    private void applyProcessComboStyle(ComboBox<Process> comboBox) {
        if (comboBox != null) {
            String existingStyle = comboBox.getStyle();
            comboBox.setStyle(existingStyle + 
                " -fx-text-fill: white; " +
                " -fx-prompt-text-fill: #cccccc;");
            
            comboBox.setCellFactory(lv -> new ListCell<Process>() {
                @Override
                protected void updateItem(Process item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("-fx-background-color: #251F35;");
                    } else {
                        setText(processToString(item)); 
                        setTextFill(Color.WHITE);
                        setStyle("-fx-background-color: #251F35; -fx-border-color: #f268ff;");
                    }
                }
            });
            
            comboBox.setButtonCell(new ListCell<Process>() {
                @Override
                protected void updateItem(Process item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(comboBox.getPromptText());
                        setTextFill(Color.web("#cccccc"));
                    } else {
                        setText(processToString(item)); 
                        setTextFill(Color.WHITE);
                    }
                    setStyle("-fx-background-color: #251F35;");
                }
            });
        }
    }

    private void initializeMemoryScrollPane() {
        memoryScrollPane = new ScrollPane();
        memoryScrollPane.setFitToWidth(true);
        
        memoryScrollPane.setStyle("-fx-background: #1a1a2e; " +
                                "-fx-border-color: #f268ff; " +
                                "-fx-background-color: transparent;");
        
        memoryScrollPane.getStyleClass().add("scroll-pane");
        
        memoryContent = new VBox(10);
        memoryContent.setStyle("-fx-padding: 10;");
        memoryScrollPane.setContent(memoryContent);
        
        memoryScrollPane.prefHeightProperty().bind(paginationVBox.heightProperty());
        memoryScrollPane.prefWidthProperty().bind(paginationVBox.widthProperty().subtract(20));
        
        paginationVBox.setStyle("-fx-padding: 0;");
    }

    private void initializeComboBoxes() {
        schedulerComboBox.getItems().addAll(
            "Round Robin (Quantum: 20)",
            "Round Robin (Quantum: 10)", 
            "SJF No Preemptive",
            "Priority No Preemptive"
        );
        schedulerComboBox.setValue("Round Robin (Quantum: 20)");

        pageReplacerComboBox.getItems().addAll("FIFO", "LRU");
        pageReplacerComboBox.setValue("FIFO");

        processQueueComboBox.setCellFactory(lv -> new ListCell<Process>() {
            @Override
            protected void updateItem(Process process, boolean empty) {
                super.updateItem(process, empty);
                if (empty || process == null) {
                    setText(null);
                } else {
                    setText(processToString(process));
                }
            }
        });

        processQueueComboBox.setButtonCell(new ListCell<Process>() {
            @Override
            protected void updateItem(Process process, boolean empty) {
                super.updateItem(process, empty);
                if (empty || process == null) {
                    setText("Processes in queue: " + processQueue.size());
                } else {
                    setText(processToString(process));
                }
            }
        });

        processQueueComboBox.setOnAction(event -> {
            Process selectedProcess = processQueueComboBox.getValue();
            if (selectedProcess != null) {
                loadProcessToForm(selectedProcess);
            }
        });
    }

    private String processToString(Process process) {
        return String.format("PID: %d | %s | Pri: %d | Dur: %d | Mem: %dKB",
            process.getId(),
            process.getName(),
            process.getPriority(),
            process.getDuration(),
            process.getMemoryUsage()
        );
    }

    private void loadProcessToForm(Process process) {
        nameProcessText.setText(process.getName());
        priorityProcessText.setText(String.valueOf(process.getPriority()));
        durationProcessText.setText(String.valueOf(process.getDuration()));
        cpuUseProcessText.setText(String.valueOf(process.getCpuTime()));
        memoryProcessText.setText(String.valueOf(process.getMemoryUsage()));
        fileAccessProcessText.setText(process.needsFileAccess() ? "r&w" : "r");
    }

    @FXML
    void addProcess(ActionEvent event) {
        try {
            String name = nameProcessText.getText().trim();
            String priorityStr = priorityProcessText.getText().trim();
            String durationStr = durationProcessText.getText().trim();
            String cpuUseStr = cpuUseProcessText.getText().trim();
            String memoryStr = memoryProcessText.getText().trim();
            String fileAccess = fileAccessProcessText.getText().trim();

            if (name.isEmpty()) {
                showError("Error", "Name can't be empty");
                return;
            }

            int priority;
            try {
                priority = Integer.parseInt(priorityStr);
                if (priority < -20 || priority > 20) {
                    showError("Error", "Priority should be between -20 y 20");
                    return;
                }
            } catch (NumberFormatException e) {
                showError("Error", "Priority should be a valid number");
                return;
            }

            int duration;
            try {
                duration = Integer.parseInt(durationStr);
                if (duration < 1 || duration > 50) {
                    showError("Error", "Duration should be between 1 y 50 seconds");
                    return;
                }
            } catch (NumberFormatException e) {
                showError("Error", "Duration should be a valid number");
                return;
            }

            int cpuUse;
            try {
                cpuUse = Integer.parseInt(cpuUseStr);
                if (cpuUse < 1 || cpuUse > 100) {
                    showError("Error", "CPU use should be between 1% y 100%");
                    return;
                }
            } catch (NumberFormatException e) {
                showError("Error", "CPU use should be a valid number");
                return;
            }

            int memory;
            try {
                memory = Integer.parseInt(memoryStr);
                if (memory < 50 || memory > 1000) {
                    showError("Error", "Memory should be between 50 y 1000 KB");
                    return;
                }
            } catch (NumberFormatException e) {
                showError("Error", "Memory should be a valid number");
                return;
            }

            if (!fileAccess.equals("r&w") && !fileAccess.equals("r") && !fileAccess.equals("w")) {
                showError("Error", "Access shoul be specified by: 'r&w', 'r' o 'w'");
                return;
            }

            Process selectedProcess = processQueueComboBox.getValue();
            if (selectedProcess != null) {
                updateProcess(selectedProcess, name, priority, duration, cpuUse, memory, fileAccess);
            } else {
                Process process = new Process(
                    processQueue.size() + 1,
                    name,
                    priority,
                    duration,
                    cpuUse,
                    memory,
                    memory,
                    fileAccess, 
                    0
                );

                processQueue.add(process);
            }

            updateProcessQueueComboBox();
            clearProcessFields();
            processQueueComboBox.getSelectionModel().clearSelection();

        } catch (Exception e) {
            showError("Error", "Error processing: " + e.getMessage());
        }
    }

    private void updateProcess(Process process, String name, int priority, int duration, int cpuUse, int memory, String fileAccess) {
        process.setName(name);
        process.setPriority(priority);
        process.setDuration(duration);
        process.setCpuTime(cpuUse);
        process.setMemoryUsage(memory);
        process.setFileAccessType(fileAccess);
    }

    private void clearProcessFields() {
        nameProcessText.clear();
        //priorityProcessText.clear();
        //durationProcessText.clear();
        //cpuUseProcessText.clear();
        memoryProcessText.clear();
        //fileAccessProcessText.clear();
        processQueueComboBox.getSelectionModel().clearSelection();
    }

    private void updateProcessQueueComboBox() {
        processQueueComboBox.getItems().setAll(processQueue);
        
        if (!processQueue.isEmpty()) {
            processQueueComboBox.setPromptText("Processes in queue: " + processQueue.size());
        } else {
            processQueueComboBox.setPromptText("No processes in queue");
        }
    }

    @FXML
    void changeToProcessConfig(MouseEvent event) {
        showSimulationPanel();
    }

    @FXML
    void changeToSimulation(MouseEvent event) {
        showProcessConfigPanel();
    }

    @FXML
    void resetSimulation(ActionEvent event) {
        stopSimulation();
        processQueue.clear();
        simulationTime = 0;
        currentScheduler = null; 
        memoryManager = null;
        updateProcessQueueComboBox();
        initializeFileManagerPanel();
        clearSimulationUI();
        fileMutexManager.reset();
        fileMutexManager.reset();
    }

    @FXML
    void startSimulation(ActionEvent event) {
        if (processQueue.isEmpty()) {
            showError("Error", "No processes in queue to simulate");
            return;
        }

        showSimulationPanel();
        initializeSimulation();
    }

    @FXML
    void stopSimulation(ActionEvent event) {
        stopSimulation();
    }

    private void showProcessConfigPanel() {
        processConfigPanel.setVisible(true);
        simulationPanel.setVisible(false);
        
        processConfigTabLabel.setTextFill(Color.web("#f268ff"));
        processConfigTabLine.setStroke(Color.web("#f268ff"));
        
        simulationTabText.setTextFill(Color.web("#9d9d9d"));
        simulationTabLine.setStroke(Color.WHITE);
    }

    private void showSimulationPanel() {
        processConfigPanel.setVisible(false);
        simulationPanel.setVisible(true);
        
        simulationTabText.setTextFill(Color.web("#f268ff"));
        simulationTabLine.setStroke(Color.web("#f268ff"));
        
        processConfigTabLabel.setTextFill(Color.web("#9d9d9d"));
        processConfigTabLine.setStroke(Color.WHITE);
    }

    private ProcessScheduler createScheduler(String schedulerType) {
        switch (schedulerType) {
            case "Round Robin (Quantum: 20)":
                return new RoundRobinScheduler(20);
            case "Round Robin (Quantum: 10)":
                return new RoundRobinScheduler(10);
            case "SJF No Preemptive":
                return new SJFScheduler(false);
            case "Priority No Preemptive":
                return new PriorityScheduler(false); 
            default:
                return null;
        }
    }

    private void initializeSimulation() {
        String schedulerType = schedulerComboBox.getValue();
        currentScheduler = createScheduler(schedulerType);

        String pageReplacer = pageReplacerComboBox.getValue();
        PageReplacementAlgorithm algorithm = pageReplacer.equals("LRU") ? 
            new LRUAlgorithm() : new FIFOAlgorithm();
        memoryManager = new MemoryManager(8, 100, algorithm); 
        processQuantumCounters.clear();
        
        
        for (Process original : processQueue) {
            Process copy = createProcessCopy(original);
            currentScheduler.addProcess(copy);
        }

        stopSimulationButton.setVisible(true);
        resetSimulationButton.setVisible(true);

        startSimulationThread();
    }
    
    private Process createProcessCopy(Process original) {
        int maxMemory = getOriginalMemoryForProcess(original);
        
        Process copy = new Process(
            original.getId(),
            original.getName(),
            original.getPriority(),
            original.getDuration(),
            original.getCpuTime(),
            50,
            maxMemory, 
            original.getFileAccessType(),
            0
        );
    
        return copy;
    }

    private void startSimulationThread() {
        if (simulationRunning) {
            return;
        }
        
        simulationRunning = true;
        processQuantumCounters.clear(); 
        
        simulationThread = new Thread(() -> {
            try {
                while (simulationRunning && currentScheduler.hasPendingProcesses()) {
                    simulationTime++;
                                        
                    executeSchedulerCycle();
                    
                    simulateMemoryAccess();
                    
                    javafx.application.Platform.runLater(() -> {
                        updateSimulationUI();
                    });
                    
                    Thread.sleep(1000); 
                }
                
                javafx.application.Platform.runLater(() -> {
                    if (simulationRunning) {
                        showSuccess("Simulation", "Simulation completed in " + simulationTime + " cycles");
                    }
                    simulationRunning = false;
                    stopSimulationButton.setVisible(false);
                });
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        simulationThread.setDaemon(true);
        simulationThread.start();
    }


    private void executeSchedulerCycle() {
        if (currentScheduler != null) {
            Process nextProcess = currentScheduler.getNextProcess();
            
            fileMutexManager.updateMutexTime();
            
            updateAllWaitingTimes();            
            verifyAndFixMemoryState();
            
            if (nextProcess != null) {
                boolean wasFinished = nextProcess.isFinished();
                nextProcess.execute(1);
                
                if (!wasFinished && nextProcess.isFinished()) {
                    nextProcess.finish(simulationTime);
                }
                
            }
            
            currentScheduler.tick();
        }
    }

    private void updateAllWaitingTimes() {
        for (Process process : getAllSimulationProcesses()) {
            if (!process.isFinished()) {
                process.updateWaitingTime(simulationTime);
            }
        }
    }

    private void simulateMemoryAccess() {
        if (memoryManager != null && currentScheduler.getCurrentProcess() != null) {
            Process currentProcess = currentScheduler.getCurrentProcess();
            
            if (simulationTime % 20 == 0) { 
                memoryManager.checkAndCleanDuplicates();
            }
            
            verifyAndFixMemoryState();
            
            if (currentProcess.getTotalPages() > 0 && Math.random() < 0.3) {
                int pageToAccess = (int) (Math.random() * currentProcess.getTotalPages());
                Page page = currentProcess.getPages().get(pageToAccess);
                
                boolean pageHit = memoryManager.accessPage(page, simulationTime);

                forceMemoryConsistency();
                
                if (!pageHit) {
                    for (Process p : getAllProcessesInMemory()) {
                        forceMemorySync(p);
                    }
                }
            }
        }
    }


    private Set<Process> getAllProcessesInMemory() {
        Set<Process> processes = new HashSet<>();
        
        Process current = currentScheduler.getCurrentProcess();
        if (current != null) {
            processes.add(current);
        }
        
        processes.addAll(currentScheduler.getReadyQueue());
        
        if (memoryManager != null) {
            for (PageFrame frame : memoryManager.getPageFrames()) {
                if (frame.isOccupied() && frame.getPage() != null) {
                    processes.add(frame.getPage().getProcess());
                }
            }
        }
        
        return processes;
    }

    private void askForMemory(Process process) {
        if (process == null) return;
        
        int currentMemory = process.getMemoryUsage();
        int maxMemory = getOriginalMemoryForProcess(process);
        
        // Agregar 50KB
        int newMemory = currentMemory + 50;
        if (newMemory > maxMemory) {
            newMemory = maxMemory;
        }
        
        int newPages = (int) Math.ceil(newMemory / 100.0);
        
        List<Page> previouslyLoadedPages = new ArrayList<>();
        for (Page page : process.getPages()) {
            if (page.isLoaded()) {
                previouslyLoadedPages.add(page);
            }
        }
        
        process.setMemoryUsage(newMemory);
        
        if (memoryManager != null) {
            int pagesToLoad = newPages;
            
            for (int i = 0; i < process.getPages().size() && pagesToLoad > 0; i++) {
                Page page = process.getPages().get(i);
                
                if (!previouslyLoadedPages.contains(page)) {
                    memoryManager.accessPage(page, simulationTime);
                }
                pagesToLoad--;
            }
        }
        
        verifyAndFixMemoryState();
    }

    private void freeMemory(Process process) {
        if (process == null) return;
        
        int currentMemory = process.getMemoryUsage();
        
        int newMemory = currentMemory - 50;
        if (newMemory < 50) {
            newMemory = 50;
        }
        
        int newPages = (int) Math.ceil(newMemory / 100.0);
        
        List<Page> pagesToFree = new ArrayList<>();
        for (int i = newPages; i < process.getPages().size(); i++) {
            pagesToFree.add(process.getPages().get(i));
        }
        
        for (Page page : pagesToFree) {
            if (page.isLoaded() && memoryManager != null) {
                for (PageFrame frame : memoryManager.getPageFrames()) {
                    if (frame.isOccupied() && frame.getPage() != null && 
                        frame.getPage().equals(page)) {
                        frame.unloadPage();
                        break;
                    }
                }
                page.setLoaded(false);
            }
        }
        
        process.setMemoryUsage(newMemory);
        
        verifyAndFixMemoryState();
    }

    private int getOriginalMemoryForProcess(Process process) {
        for (Process original : processQueue) {
            if (original.getId() == process.getId()) {
                return original.getMemoryUsage();
            }
        }
        return process.getPages().size() * 100;
    }

    private void verifyAndFixMemoryState() {
        if (memoryManager == null) return;
        
        Set<Process> allProcesses = getAllProcessesInMemory();
        
        for (Process process : allProcesses) {
            int actuallyLoaded = 0;
            int believedLoaded = 0;
            
            for (PageFrame frame : memoryManager.getPageFrames()) {
                if (frame.isOccupied() && frame.getPage() != null && 
                    frame.getPage().getProcess().equals(process)) {
                    actuallyLoaded++;
                }
            }
            
            for (Page page : process.getPages()) {
                if (page.isLoaded()) {
                    believedLoaded++;
                }
            }
            
            if (actuallyLoaded != believedLoaded) {
                
                for (Page page : process.getPages()) {
                    boolean actuallyInMemory = false;
                    
                    for (PageFrame frame : memoryManager.getPageFrames()) {
                        if (frame.isOccupied() && frame.getPage() != null && 
                            frame.getPage().equals(page)) {
                            actuallyInMemory = true;
                            break;
                        }
                    }
                    
                    if (page.isLoaded() != actuallyInMemory) {
                        page.setLoaded(actuallyInMemory);
                    }
                }
                
            }
        }
    }

    private void stopSimulation() {
        simulationRunning = false;
        if (simulationThread != null) {
            simulationThread.interrupt();
            simulationThread = null;
        }
        fileMutexManager.reset();
        showFinalMetrics();
    }


    private void updateSimulationUI() {
        processSchedulerVBox.getChildren().clear();
        if (memoryManager != null) {
            verifyAndFixMemoryState(); 
        }
        
        if (!paginationVBox.getChildren().contains(memoryScrollPane)) {
            paginationVBox.getChildren().clear();
        } else {
            paginationVBox.getChildren().removeIf(node -> node != memoryScrollPane);
        }

        if (currentScheduler == null) {
            Label noSimulationLabel = createLabel("No active simulation", "#666666", 16);
            processSchedulerVBox.getChildren().add(noSimulationLabel);
            
            setupMemoryPanelWithScroll();
            
            updateFileManagerUI();
            return;
        }

        Label schedulerTitle = createLabel("SCHEDULER - " + currentScheduler.getAlgorithmName(), "#f268ff", 16);
        processSchedulerVBox.getChildren().add(schedulerTitle);

        String schedulerType = schedulerComboBox.getValue();
        if (schedulerType.contains("SJF No Preemptive")) {
            Label algoInfo = createLabel("Criterion: Less remaining time goes first", "#ffff00", 11);
            processSchedulerVBox.getChildren().add(algoInfo);
        } else if (schedulerType.contains("Priority No Preemptive")) {
            Label algoInfo = createLabel("Criterion: Major priority goes first", "#ffff00", 11);
            processSchedulerVBox.getChildren().add(algoInfo);
        } else if (schedulerType.contains("Round Robin") && currentScheduler instanceof RoundRobinScheduler) {
            RoundRobinScheduler rrScheduler = (RoundRobinScheduler) currentScheduler;
            Process currentProcess = rrScheduler.getCurrentProcess();

            if (currentProcess != null) {
                int currentQuantum = rrScheduler.getCurrentQuantum();
                int quantum = rrScheduler.getQuantumValue();
                String quantumColor = currentQuantum >= quantum ? "#ff4444" : "#ffff00";
                
                Label quantumLabel = createLabel(
                    "Quantum: " + currentQuantum + "/" + quantum + 
                    (currentQuantum >= quantum ? " (SWITCH IN NEXT CYCLE)" : ""), 
                    quantumColor, 11
                );
                processSchedulerVBox.getChildren().add(quantumLabel);
            }
        }

        Label timeLabel = createLabel("Time: " + simulationTime + " cycles", "white", 12);
        processSchedulerVBox.getChildren().add(timeLabel);

        Process currentProcess = currentScheduler.getCurrentProcess();
        if (currentProcess != null) {
            Pane currentProcessPane = createProcessPane(currentProcess, "EXECUTING", "#00ff00");
            processSchedulerVBox.getChildren().add(currentProcessPane);
            
            int maxMemory = getOriginalMemoryForProcess(currentProcess);
            
            Label execInfo = createLabel(
                "Remaining Time: " + currentProcess.getRemainingTime() + 
                " | Memory: " + currentProcess.getMemoryUsage() + "/" + maxMemory + "KB" +
                " | Priority: " + currentProcess.getPriority(),
                "#00ff00", 11
            );
            processSchedulerVBox.getChildren().add(execInfo);
        } else {
            Label idleLabel = createLabel("CPU INACTIVE", "#ff4444", 14);
            processSchedulerVBox.getChildren().add(idleLabel);
        }

        showReadyQueueAccordingToAlgorithm();

        setupMemoryPanelWithScroll();

        updateFileManagerUI();
    }

    private void showReadyQueueAccordingToAlgorithm() {
        Label readyQueueLabel = createLabel("READY QUEUE:", "#f268ff", 14);
        processSchedulerVBox.getChildren().add(readyQueueLabel);

        List<Process> sortedReadyQueue = getSortedReadyQueue();
        
        if (sortedReadyQueue.isEmpty()) {
            Label emptyLabel = createLabel("There is not processes in ready queue", "#666666", 12);
            processSchedulerVBox.getChildren().add(emptyLabel);
        } else {
            for (Process process : sortedReadyQueue) {
                Pane processPane = createProcessPane(process, "READY", "#ffff00");
                processSchedulerVBox.getChildren().add(processPane);
            }
        }
    }

    private List<Process> getSortedReadyQueue() {
        List<Process> readyQueue = currentScheduler.getReadyQueue();
        String algorithm = schedulerComboBox.getValue();
        
        List<Process> sortedQueue = new ArrayList<>(readyQueue);
        
        switch (algorithm) {
            case "Round Robin (Quantum: 20)":
            case "Round Robin (Quantum: 10)":
                break;
            case "SJF No Preemptive":
                sortedQueue.sort(Comparator.comparingInt(Process::getRemainingTime));
                break;
                
            case "Priority No Preemptive":
                sortedQueue.sort(Comparator.comparingInt(Process::getPriority));
                break;
        }
        
        return sortedQueue;
    }

    private Label createLabel(String text, String color, double fontSize) {
        Label label = new Label(text);
        label.setTextFill(Color.web(color));
        label.setStyle("-fx-font-family: 'Cascadia Code Regular'; -fx-font-size: " + fontSize + ";");
        label.setWrapText(true);
        label.setMinWidth(80); 
        return label;
    }


    private Pane createProcessPane(Process process, String state, String color) {
        Pane pane = new Pane();
        pane.setStyle("-fx-background-color: #251F35; -fx-border-color: " + color + "; -fx-border-radius: 5; -fx-padding: 8;");
        pane.setPrefHeight(100); 
        pane.setPrefWidth(280);

        VBox content = new VBox(3);
        
        HBox line1 = new HBox(10);
        Label nameLabel = createLabel(process.getName() + " (" + state + ")", color, 12);
        Label pidLabel = createLabel("PID: " + process.getId(), "white", 10);
        line1.getChildren().addAll(nameLabel, pidLabel);
        
        HBox line2 = new HBox(10);
        Label priorityLabel = createLabel("Pri: " + process.getPriority(), "#cccccc", 10);
        Label timeLabel = createLabel("Dur: " + process.getRemainingTime() + "/" + process.getDuration(), "#cccccc", 10);
        line2.getChildren().addAll(priorityLabel, timeLabel);
        
        HBox line3 = new HBox(10);
        int maxMemory = process.getMaxMemory();
        Label memoryLabel = createLabel("Mem: " + process.getMemoryUsage() + "/" + maxMemory + "KB", "#cccccc", 10);
        
        Label pagesLabel = createLabel(
            "Págs: " + process.getPagesInMemory() + "/" + process.getMaxPages(), 
            process.getPagesInMemory() > 0 ? "#00ff00" : "#ff4444", 10
        );
        line3.getChildren().addAll(memoryLabel, pagesLabel);

        content.getChildren().addAll(line1, line2, line3);
        
        if (state.equals("EXECUTING")) {
            HBox buttonBox = new HBox(5);
            buttonBox.setStyle("-fx-padding: 5 0 0 0;");
            
            Button askMemoryBtn = new Button("Ask Memory (+50KB)");
            Button freeMemoryBtn = new Button("Free Memory (-50KB)");
            
            askMemoryBtn.setStyle("-fx-background-color: #00aa00; -fx-text-fill: white; -fx-font-size: 9; -fx-padding: 3 5 3 5;");
            freeMemoryBtn.setStyle("-fx-background-color: #aa0000; -fx-text-fill: white; -fx-font-size: 9; -fx-padding: 3 5 3 5;");
            
            askMemoryBtn.setOnAction(e -> {
                askForMemory(process);
                forceMemoryUpdate();
            });
            
            freeMemoryBtn.setOnAction(e -> {
                freeMemory(process);
                forceMemoryUpdate();
            });
            
            askMemoryBtn.setDisable(process.getMemoryUsage() >= maxMemory);
            freeMemoryBtn.setDisable(process.getMemoryUsage() <= 50);
            
            buttonBox.getChildren().addAll(askMemoryBtn, freeMemoryBtn);
            content.getChildren().add(buttonBox);
        }
        
        pane.getChildren().add(content);
        
        return pane;
    }

    private void clearSimulationUI() {
        processSchedulerVBox.getChildren().clear();
        paginationVBox.getChildren().clear();
        
        stopSimulationButton.setVisible(false);
        resetSimulationButton.setVisible(false);
        updateFileManagerUI();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setupMemoryPanelWithScroll() {
        lastScrollPosition = memoryScrollPane.getVvalue();
        
        memoryContent.getChildren().clear();
        
        Label memoryTitle = createLabel("PAGINATION SYSTEM", "#f268ff", 16);
        memoryContent.getChildren().add(memoryTitle);
        
        Label memoryInfo = createLabel(
            "Algorithm: " + pageReplacerComboBox.getValue() + 
            " | Frames: 8 de 100KB c/u",
            "white", 12
        );
        memoryContent.getChildren().add(memoryInfo);
        
        showProcessesMemory(memoryContent);
        
        showPhysicalFrames(memoryContent);
        
        if (!paginationVBox.getChildren().contains(memoryScrollPane)) {
            paginationVBox.getChildren().add(memoryScrollPane);
        }
        
        javafx.application.Platform.runLater(() -> {
            memoryScrollPane.setVvalue(lastScrollPosition);
        });
    }

    private void showProcessesMemory(VBox memoryContent) {
        Label processesMemoryLabel = createLabel("MEMORY FOR PROCESS:", "#f268ff", 14);
        memoryContent.getChildren().add(processesMemoryLabel);
        Set<Process> displayedProcesses = new HashSet<>();

        Process current = currentScheduler.getCurrentProcess();
        if (current != null && !displayedProcesses.contains(current)) {
            createCompactProcessMemoryPanel(current, memoryContent);
            displayedProcesses.add(current);
        }
        
        for (Process process : getSortedReadyQueue()) {
            if (!displayedProcesses.contains(process)) {
                createCompactProcessMemoryPanel(process, memoryContent);
                displayedProcesses.add(process);
            }
        }
    }

    private void createCompactProcessMemoryPanel(Process process, VBox parent) {
        VBox processMemoryBox = new VBox(3);
        processMemoryBox.setStyle("-fx-border-color: #444; -fx-border-width: 1; -fx-padding: 8; -fx-background-color: #2d2d44;");
        
        HBox header = new HBox(10);
        Label processName = createLabel(process.getName(), "#f268ff", 12);
        
        int maxMemory = process.getMaxMemory();
        Label memoryInfo = createLabel(process.getMemoryUsage() + "/" + maxMemory + "KB", "white", 11);
        
        // CORREGIDO: Usar getMaxPages() para el denominador
        Label pagesInfo = createLabel(
            "Pages: " + process.getPagesInMemory() + "/" + process.getMaxPages() + " in RAM", 
            process.getPagesInMemory() > 0 ? "#00ff00" : "#ff4444", 16
        );
        
        header.getChildren().addAll(processName, memoryInfo, pagesInfo);
        processMemoryBox.getChildren().add(header);
        
        if (process.getMaxPages() > 0) {
            HBox memoryBar = createMemoryUsageBar(process);
            processMemoryBox.getChildren().add(memoryBar);
        }
        
        parent.getChildren().add(processMemoryBox);
    }

    private HBox createMemoryUsageBar(Process process) {
        HBox barContainer = new HBox();
        barContainer.setPrefHeight(12);
        barContainer.setMinWidth(200);
        barContainer.setMaxWidth(200);
        barContainer.setStyle("-fx-background-color: #444; -fx-border-color: #666; -fx-border-width: 1;");
        
        int maxPages = process.getMaxPages(); 
        int pagesInMemory = process.getPagesInMemory();
        
        if (maxPages == 0) {
            Pane emptyPane = new Pane();
            emptyPane.setPrefSize(200, 12);
            emptyPane.setStyle("-fx-background-color: #666;");
            barContainer.getChildren().add(emptyPane);
            return barContainer;
        }
        
        double usageRatio = (double) pagesInMemory / maxPages;
        int usedWidth = (int) Math.round(200 * usageRatio);
        
        usedWidth = Math.max(0, Math.min(usedWidth, 200));
        
        Pane usedPane = new Pane();
        usedPane.setPrefSize(usedWidth, 12);
        
        String usedColor;
        if (usageRatio >= 0.99) usedColor = "#00ff00"; 
        else if (usageRatio > 0.7) usedColor = "#ffff00"; 
        else usedColor = "#00aaff"; 
        
        usedPane.setStyle("-fx-background-color: " + usedColor + ";");
        
        if (usedWidth < 200) {
            Pane freePane = new Pane();
            freePane.setPrefSize(200 - usedWidth, 12);
            freePane.setStyle("-fx-background-color: #666;");
            barContainer.getChildren().addAll(usedPane, freePane);
        } else {
            barContainer.getChildren().add(usedPane);
        }
        
        return barContainer;
    }


    private void showPhysicalFrames(VBox memoryContent) {
        Label framesTitle = createLabel("PHYSICAL RAM FRAMES:", "#f268ff", 14);
        memoryContent.getChildren().add(framesTitle);
        
        if (memoryManager != null) {
            List<PageFrame> pageFrames = memoryManager.getPageFrames();
            int occupiedFrames = 0;
            
            GridPane framesGrid = new GridPane();
            framesGrid.setHgap(5);
            framesGrid.setVgap(5);
            framesGrid.setPrefWidth(280);
            
            int col = 0, row = 0;
            for (PageFrame frame : pageFrames) {
                Pane framePane = createCompactFramePane(frame);
                framesGrid.add(framePane, col, row);
                
                if (frame.isOccupied()) occupiedFrames++;
                
                col++;
                if (col >= 2) { 
                    col = 0;
                    row++;
                }
            }
            
            memoryContent.getChildren().add(framesGrid);
            
            Label statsLabel = createLabel(
                "RAM use: " + occupiedFrames + "/8 frames (" + (occupiedFrames * 100/8) + "%) | " +
                "Faults: " + memoryManager.getPageFaults() + 
                " | Rate: " + String.format("%.1f", memoryManager.getPageFaultRate() * 100) + "%",
                "white", 16
            );
            memoryContent.getChildren().add(statsLabel);
        }
    }

    private Pane createCompactFramePane(PageFrame frame) {
        Pane pane = new Pane();
        pane.setPrefHeight(60);
        pane.setPrefWidth(150);
        
        String frameColor = frame.isOccupied() ? "#00ff00" : "#666666";
        String bgColor = frame.isOccupied() ? "#1a1a2e" : "#251F35";
        
        pane.setStyle("-fx-background-color: " + bgColor + "; -fx-border-color: " + frameColor + "; -fx-border-radius: 3; -fx-padding: 3;");

        VBox content = new VBox(2);
        
        Label frameLabel = createLabel("Frame " + frame.getFrameId(), "white", 10);
        
        if (frame.isOccupied() && frame.getPage() != null) {
            Page page = frame.getPage();
            Label pageLabel = createLabel(
                page.getProcess().getName() + "-Pag" + page.getPageId(),
                "#00ff00", 16
            );
            content.getChildren().addAll(frameLabel, pageLabel);
            if (memoryManager != null && memoryManager.getReplacementAlgorithm() instanceof LRUAlgorithm) {
                // Información de último acceso
                Label lastAccessLabel = createLabel(
                    "Last Access:" + frame.getLastAccessTime(), 
                    "#00ff00", 16
                );
                
                content.getChildren().addAll(lastAccessLabel);
            }
        } else {
            Label freeLabel = createLabel("LIBRE", "#666666", 14);
            content.getChildren().addAll(frameLabel, freeLabel);
        }

        pane.getChildren().add(content);
        return pane;
    }

    private List<Process> getProcessesWithFileAccess() {
        List<Process> fileProcesses = new ArrayList<>();
        
        if (currentScheduler == null) {
            return fileProcesses;
        }
        
        Set<Integer> addedProcessIds = new HashSet<>();
        
        Process current = currentScheduler.getCurrentProcess();
        if (current != null) {
            if (current.needsFileAccess() && !addedProcessIds.contains(current.getId())) {
                fileProcesses.add(current);
                addedProcessIds.add(current.getId());
            }
        } else {
            System.out.println("No current process executing");
        }
        
        List<Process> readyQueue = currentScheduler.getReadyQueue();
        
        for (Process process : readyQueue) {
            if (process.needsFileAccess() && !addedProcessIds.contains(process.getId())) {
                fileProcesses.add(process);
                addedProcessIds.add(process.getId());
            }
        }
        
        for (Process process : processQueue) {
            if (!addedProcessIds.contains(process.getId()) && process.needsFileAccess()) {
                fileProcesses.add(process);
                addedProcessIds.add(process.getId());
            }
        }
        
        return fileProcesses;
    }


    private void forceMemoryUpdate() {
        verifyAndFixMemoryState();
        
        javafx.application.Platform.runLater(() -> {
            updateSimulationUI();
        });
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void forceMemorySync(Process process) {
        if (process != null) {
            int currentMemory = process.getMemoryUsage();
            process.setMemoryUsage(currentMemory); 
        }
    }

    private void forceMemoryConsistency() {
        if (memoryManager == null) return;
        
        for (Process process : getAllProcessesInMemory()) {
            int pagesActuallyLoaded = 0;
            
            for (PageFrame frame : memoryManager.getPageFrames()) {
                if (frame.isOccupied() && frame.getPage() != null && 
                    frame.getPage().getProcess().equals(process)) {
                    pagesActuallyLoaded++;
                }
            }
            
            int expectedMemory = pagesActuallyLoaded * 100; 
            int currentMemory = process.getMemoryUsage();
            
            if (expectedMemory != currentMemory) {
                    if (Math.abs(expectedMemory - currentMemory) >= 100) {
                    process.setMemoryUsage(expectedMemory);
                }
            }
        }
    }

}