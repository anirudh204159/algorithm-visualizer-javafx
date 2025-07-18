import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.Screen;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main extends Application {
    private static final int NUM_BARS = 60;
    private static final int BAR_WIDTH = 16;
    private static final int BAR_GAP = 7;
    private static final int WINDOW_HEIGHT = 600;
    private static final int BAR_LEFT_PADDING = 80;      // wider padding!
    private static final int BAR_BOTTOM_PADDING = 50;    // wider bottom padding!

    private int[] array = new int[NUM_BARS];
    private Pane barPane = new Pane();
    private int highlight1 = -1, highlight2 = -1;
    private volatile boolean isSorting = false;
    private volatile boolean shouldStop = false;
    private volatile boolean isPaused = false;
    private volatile boolean stepRequested = false;
    private Slider speedSlider;
    private Label algoLabel, infoLabel;
    private Button sortBtn, stopBtn, pauseBtn, resumeBtn, nextStepBtn, exitBtn;
    private ComboBox<String> algorithmBox;

    @Override
    public void start(Stage stage) {
        // Controls Setup
        Button shuffleBtn = new Button("Shuffle");
        sortBtn = new Button("Sort");
        stopBtn = new Button("Stop");
        pauseBtn = new Button("Pause");
        resumeBtn = new Button("Resume");
        nextStepBtn = new Button("Next Step");
        exitBtn = new Button("Exit");

        String btnStyle = "-fx-font-size: 17px; -fx-background-radius: 9; -fx-padding: 7 25 7 25;";
        shuffleBtn.setStyle(btnStyle + "-fx-background-color: #2962FF; -fx-text-fill: white;");
        sortBtn.setStyle(btnStyle + "-fx-background-color: #43A047; -fx-text-fill: white;");
        stopBtn.setStyle(btnStyle + "-fx-background-color: #D32F2F; -fx-text-fill: white;");
        pauseBtn.setStyle(btnStyle + "-fx-background-color: #FFA000; -fx-text-fill: white;");
        resumeBtn.setStyle(btnStyle + "-fx-background-color: #388E3C; -fx-text-fill: white;");
        nextStepBtn.setStyle(btnStyle + "-fx-background-color: #455A64; -fx-text-fill: white;");
        exitBtn.setStyle(btnStyle + "-fx-background-color: #616161; -fx-text-fill: white;");

        sortBtn.setDisable(false);
        stopBtn.setDisable(true);
        pauseBtn.setDisable(true);
        resumeBtn.setDisable(true);
        nextStepBtn.setDisable(true);

        algorithmBox = new ComboBox<>();
        algorithmBox.getItems().addAll(
                "Bubble Sort", "Selection Sort", "Insertion Sort",
                "Quick Sort", "Merge Sort", "Heap Sort",
                "Linear Search", "Binary Search"
        );
        algorithmBox.setValue("Bubble Sort");
        algorithmBox.setStyle("-fx-font-size: 17px; -fx-background-radius: 9;");

        // Speed Slider: Higher = Faster!
        speedSlider = new Slider(1, 200, 30);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setMajorTickUnit(100);
        speedSlider.setMinorTickCount(5);
        speedSlider.setBlockIncrement(5);
        speedSlider.setStyle("-fx-font-size: 14px;");
        Label speedLbl = new Label("Speed:");
        speedLbl.setStyle("-fx-font-size: 17px; -fx-text-fill: #FFFFFF;");

        algoLabel = new Label("Algorithm: Bubble Sort");
        algoLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #E0E0E0; -fx-font-weight: bold;");
        infoLabel = new Label("Comparisons: 0 | Swaps: 0 | Delay: 30 ms");
        infoLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #FFFFFF;");
        HBox infoBar = new HBox(80, algoLabel, infoLabel);
        infoBar.setAlignment(Pos.CENTER);
        infoBar.setPadding(new Insets(15, 0, 15, 0));

        // Button Actions
        shuffleBtn.setOnAction(e -> {
            if (isSorting) return;
            generateRandomArray();
            drawBars(barPane.getWidth());
        });
        sortBtn.setOnAction(e -> {
            if (isSorting) return;
            shouldStop = false;
            isSorting = true;
            setUIOnSortStart();
            new Thread(() -> runSelectedAlgorithm(barPane.getWidth())).start();
        });
        stopBtn.setOnAction(e -> {
            shouldStop = true;
            isPaused = false;
            stepRequested = false;
            isSorting = false;
            highlight1 = highlight2 = -1;
            Platform.runLater(() -> {
                generateRandomArray();
                drawBars(barPane.getWidth());
                setUIOnSortEnd();
            });
        });
        pauseBtn.setOnAction(e -> {
            isPaused = true;
            pauseBtn.setDisable(true);
            resumeBtn.setDisable(false);
            nextStepBtn.setDisable(false);
        });
        resumeBtn.setOnAction(e -> {
            isPaused = false;
            resumeBtn.setDisable(true);
            pauseBtn.setDisable(false);
            nextStepBtn.setDisable(true);
        });
        nextStepBtn.setOnAction(e -> stepRequested = true);
        algorithmBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            algoLabel.setText("Algorithm: " + newVal);
        });
        exitBtn.setOnAction(e -> Platform.exit());

        HBox controls = new HBox(25, shuffleBtn, sortBtn, stopBtn, pauseBtn, resumeBtn, nextStepBtn, algorithmBox, speedLbl, speedSlider, exitBtn);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(23));
        controls.setSpacing(25);
        controls.setStyle("-fx-background-color: #232348;");
        controls.setPrefHeight(90);

        // Main Layout with BorderPane
        BorderPane root = new BorderPane();
        root.setTop(controls);

        VBox infoArea = new VBox(infoBar);
        infoArea.setAlignment(Pos.CENTER);
        infoArea.setStyle("-fx-background-color: #181828;");
        root.setCenter(infoArea);

        // Visualizer Area (bars)
        barPane.setStyle("-fx-background-color: #181828;");
        barPane.setPrefHeight(WINDOW_HEIGHT);

        ScrollPane scrollPane = new ScrollPane(barPane);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: #181828; -fx-border-color: #232348;");
        scrollPane.setPadding(new Insets(10));
        root.setBottom(scrollPane);

        // Responsive: redraw bars on pane resize
        barPane.widthProperty().addListener((obs, oldVal, newVal) -> drawBars(newVal.doubleValue()));

        generateRandomArray();
        // Draw once with a default width; will auto-adjust on resize/fullscreen
        drawBars(Screen.getPrimary().getBounds().getWidth());

        // Make window maximized (full screen usable, but with window controls)
        Scene scene = new Scene(root, 1280, 900); // starting size
        stage.setScene(scene);
        stage.setTitle("Algorithm Visualizer");
        stage.setMaximized(true);
        stage.show();
    }

    // --- Helper UI Methods ---
    private void setUIOnSortStart() {
        sortBtn.setDisable(true);
        stopBtn.setDisable(false);
        pauseBtn.setDisable(false);
        resumeBtn.setDisable(true);
        nextStepBtn.setDisable(true);
        algorithmBox.setDisable(true);
    }
    private void setUIOnSortEnd() {
        sortBtn.setDisable(false);
        stopBtn.setDisable(true);
        pauseBtn.setDisable(true);
        resumeBtn.setDisable(true);
        nextStepBtn.setDisable(true);
        algorithmBox.setDisable(false);
    }

    // --- Drawing and data management ---
    private void drawBars(double paneWidth) {
        barPane.getChildren().clear();

        // Responsive padding
        double availableWidth = paneWidth - 2 * BAR_LEFT_PADDING;
        double barSpace = BAR_WIDTH + BAR_GAP;
        double totalBarWidth = NUM_BARS * barSpace - BAR_GAP; // no gap after last bar
        double leftPad = BAR_LEFT_PADDING + Math.max(0, (availableWidth - totalBarWidth) / 2);

        for (int i = 0; i < NUM_BARS; i++) {
            int height = array[i];
            double x = leftPad + i * barSpace;
            double y = WINDOW_HEIGHT - height - BAR_BOTTOM_PADDING;
            Rectangle bar = new Rectangle(x, y, BAR_WIDTH, height);

            if (i == highlight1 || i == highlight2) {
                bar.setFill(Color.RED);
            } else {
                bar.setFill(Color.web("#2196F3"));
            }
            bar.setArcWidth(6);
            bar.setArcHeight(6);

            Label valueLabel = new Label(String.valueOf(height));
            valueLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #FFF; -fx-font-weight: bold;");
            valueLabel.setLayoutX(x + BAR_WIDTH / 2.0 - 10);
            valueLabel.setLayoutY(y - 18);

            barPane.getChildren().addAll(bar, valueLabel);
        }
    }

    private void generateRandomArray() {
        Random rand = new Random();
        for (int i = 0; i < NUM_BARS; i++) {
            array[i] = rand.nextInt(WINDOW_HEIGHT - 2 * BAR_BOTTOM_PADDING) + 40;
        }
        highlight1 = highlight2 = -1;
    }

    private int getDelay() {
        int min = (int) speedSlider.getMin();
        int max = (int) speedSlider.getMax();
        return max - (int) speedSlider.getValue() + min;
    }
    private void handlePause() throws InterruptedException {
        while (isPaused && !shouldStop) {
            if (stepRequested) {
                stepRequested = false;
                break;
            }
            Thread.sleep(10);
        }
    }
    private void updateInfo(int comparisons, int swaps) {
        Platform.runLater(() -> infoLabel.setText(
                String.format("Comparisons: %d | Swaps: %d | Delay: %d ms", comparisons, swaps, getDelay())
        ));
    }

    // --- Main sorting and searching algorithms ---
    private void runSelectedAlgorithm(double paneWidth) {
        String algo = algorithmBox.getValue();
        int comparisons = 0, swaps = 0;
        try {
            switch (algo) {
                case "Bubble Sort": {
                    for (int i = 0; i < NUM_BARS - 1 && !shouldStop; i++) {
                        for (int j = 0; j < NUM_BARS - i - 1 && !shouldStop; j++) {
                            highlight1 = j;
                            highlight2 = j + 1;
                            comparisons++;
                            Platform.runLater(() -> drawBars(paneWidth));
                            updateInfo(comparisons, swaps);
                            Thread.sleep(getDelay());
                            handlePause();
                            if (array[j] > array[j + 1]) {
                                int temp = array[j];
                                array[j] = array[j + 1];
                                array[j + 1] = temp;
                                swaps++;
                                Platform.runLater(() -> drawBars(paneWidth));
                                updateInfo(comparisons, swaps);
                                Thread.sleep(getDelay());
                                handlePause();
                            }
                        }
                    }
                } break;
                case "Selection Sort": {
                    for (int i = 0; i < NUM_BARS - 1 && !shouldStop; i++) {
                        int minIdx = i;
                        for (int j = i + 1; j < NUM_BARS && !shouldStop; j++) {
                            highlight1 = minIdx;
                            highlight2 = j;
                            comparisons++;
                            Platform.runLater(() -> drawBars(paneWidth));
                            updateInfo(comparisons, swaps);
                            Thread.sleep(getDelay());
                            handlePause();
                            if (array[j] < array[minIdx]) {
                                minIdx = j;
                            }
                        }
                        int temp = array[i];
                        array[i] = array[minIdx];
                        array[minIdx] = temp;
                        swaps++;
                        Platform.runLater(() -> drawBars(paneWidth));
                        updateInfo(comparisons, swaps);
                        Thread.sleep(getDelay());
                        handlePause();
                    }
                } break;
                case "Insertion Sort": {
                    for (int i = 1; i < NUM_BARS && !shouldStop; i++) {
                        int key = array[i];
                        int j = i - 1;
                        while (j >= 0 && array[j] > key && !shouldStop) {
                            highlight1 = j;
                            highlight2 = j + 1;
                            comparisons++;
                            array[j + 1] = array[j];
                            swaps++;
                            Platform.runLater(() -> drawBars(paneWidth));
                            updateInfo(comparisons, swaps);
                            Thread.sleep(getDelay());
                            handlePause();
                            j--;
                        }
                        array[j + 1] = key;
                        Platform.runLater(() -> drawBars(paneWidth));
                        updateInfo(comparisons, swaps);
                        Thread.sleep(getDelay());
                        handlePause();
                    }
                } break;
                case "Quick Sort": {
                    AtomicBoolean stopped = new AtomicBoolean(false);
                    quickSort(0, NUM_BARS - 1, comparisons, swaps, stopped, paneWidth);
                } break;
                case "Merge Sort": {
                    mergeSort(0, NUM_BARS - 1, new int[NUM_BARS], new int[]{0}, new int[]{0}, paneWidth);
                } break;
                case "Heap Sort": {
                    heapSort(paneWidth);
                } break;
                case "Linear Search": {
                    int target = array[new Random().nextInt(NUM_BARS)];
                    Platform.runLater(() -> infoLabel.setText("Searching for: " + target));
                    boolean found = false;
                    for (int i = 0; i < NUM_BARS && !shouldStop; i++) {
                        highlight1 = i;
                        highlight2 = -1;
                        Platform.runLater(() -> drawBars(paneWidth));
                        Thread.sleep(getDelay());
                        handlePause();
                        comparisons++;
                        updateInfo(comparisons, swaps);
                        if (array[i] == target) {
                            int foundIdx = i;
                            Platform.runLater(() -> infoLabel.setText("Found " + target + " at index " + foundIdx));
                            highlight1 = foundIdx;
                            Platform.runLater(() -> drawBars(paneWidth));
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        Platform.runLater(() -> infoLabel.setText("Target " + target + " not found."));
                    }
                } break;
                case "Binary Search": {
                    int target = array[new Random().nextInt(NUM_BARS)];
                    java.util.Arrays.sort(array);
                    Platform.runLater(() -> infoLabel.setText("Searching for: " + target + " (array sorted)"));
                    int left = 0, right = NUM_BARS - 1;
                    boolean found = false;
                    while (left <= right && !shouldStop) {
                        int mid = left + (right - left) / 2;
                        highlight1 = mid;
                        highlight2 = -1;
                        Platform.runLater(() -> drawBars(paneWidth));
                        Thread.sleep(getDelay());
                        handlePause();
                        comparisons++;
                        updateInfo(comparisons, swaps);
                        if (array[mid] == target) {
                            int foundIdx = mid;
                            Platform.runLater(() -> infoLabel.setText("Found " + target + " at index " + foundIdx));
                            highlight1 = foundIdx;
                            Platform.runLater(() -> drawBars(paneWidth));
                            found = true;
                            break;
                        } else if (array[mid] < target) {
                            left = mid + 1;
                        } else {
                            right = mid - 1;
                        }
                    }
                    if (!found) {
                        Platform.runLater(() -> infoLabel.setText("Target " + target + " not found."));
                    }
                } break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        isSorting = false;
        highlight1 = highlight2 = -1;
        Platform.runLater(() -> {
            drawBars(paneWidth);
            setUIOnSortEnd();
        });
    }

    // --- Sorting methods ---
    private void quickSort(int low, int high, int comparisons, int swaps, AtomicBoolean stopped, double paneWidth) throws InterruptedException {
        if (shouldStop || stopped.get()) return;
        if (low < high) {
            int pi = quickSortPartition(low, high, comparisons, swaps, stopped, paneWidth);
            quickSort(low, pi - 1, comparisons, swaps, stopped, paneWidth);
            quickSort(pi + 1, high, comparisons, swaps, stopped, paneWidth);
        }
    }
    private int quickSortPartition(int low, int high, int comparisons, int swaps, AtomicBoolean stopped, double paneWidth) throws InterruptedException {
        int pivot = array[high];
        int i = low - 1;
        for (int j = low; j < high && !shouldStop && !stopped.get(); j++) {
            highlight1 = j;
            highlight2 = high;
            Platform.runLater(() -> drawBars(paneWidth));
            Thread.sleep(getDelay());
            handlePause();
            if (array[j] < pivot) {
                i++;
                int temp = array[i];
                array[i] = array[j];
                array[j] = temp;
                Platform.runLater(() -> drawBars(paneWidth));
                Thread.sleep(getDelay());
                handlePause();
            }
        }
        int temp = array[i + 1];
        array[i + 1] = array[high];
        array[high] = temp;
        Platform.runLater(() -> drawBars(paneWidth));
        Thread.sleep(getDelay());
        handlePause();
        return i + 1;
    }

    private void mergeSort(int left, int right, int[] temp, int[] comparisons, int[] swaps, double paneWidth) throws InterruptedException {
        if (shouldStop) return;
        if (left < right) {
            int mid = (left + right) / 2;
            mergeSort(left, mid, temp, comparisons, swaps, paneWidth);
            mergeSort(mid + 1, right, temp, comparisons, swaps, paneWidth);
            merge(left, mid, right, temp, comparisons, swaps, paneWidth);
        }
    }
    private void merge(int left, int mid, int right, int[] temp, int[] comparisons, int[] swaps, double paneWidth) throws InterruptedException {
        for (int i = left; i <= right; i++) {
            temp[i] = array[i];
        }
        int i = left, j = mid + 1, k = left;
        while (i <= mid && j <= right && !shouldStop) {
            highlight1 = i;
            highlight2 = j;
            Platform.runLater(() -> drawBars(paneWidth));
            Thread.sleep(getDelay());
            handlePause();
            if (temp[i] <= temp[j]) {
                array[k++] = temp[i++];
            } else {
                array[k++] = temp[j++];
            }
        }
        while (i <= mid && !shouldStop) {
            highlight1 = i;
            Platform.runLater(() -> drawBars(paneWidth));
            Thread.sleep(getDelay());
            handlePause();
            array[k++] = temp[i++];
        }
        while (j <= right && !shouldStop) {
            highlight2 = j;
            Platform.runLater(() -> drawBars(paneWidth));
            Thread.sleep(getDelay());
            handlePause();
            array[k++] = temp[j++];
        }
    }

    private void heapSort(double paneWidth) throws InterruptedException {
        int n = NUM_BARS;
        for (int i = n / 2 - 1; i >= 0 && !shouldStop; i--) {
            heapify(n, i, paneWidth);
        }
        for (int i = n - 1; i >= 0 && !shouldStop; i--) {
            int temp = array[0];
            array[0] = array[i];
            array[i] = temp;
            highlight1 = 0;
            highlight2 = i;
            Platform.runLater(() -> drawBars(paneWidth));
            Thread.sleep(getDelay());
            handlePause();
            heapify(i, 0, paneWidth);
        }
    }
    private void heapify(int n, int i, double paneWidth) throws InterruptedException {
        if (shouldStop) return;
        int largest = i;
        int l = 2 * i + 1;
        int r = 2 * i + 2;

        if (l < n && array[l] > array[largest]) {
            largest = l;
        }
        if (r < n && array[r] > array[largest]) {
            largest = r;
        }

        if (largest != i) {
            int temp = array[i];
            array[i] = array[largest];
            array[largest] = temp;
            highlight1 = i;
            highlight2 = largest;
            Platform.runLater(() -> drawBars(paneWidth));
            Thread.sleep(getDelay());
            handlePause();
            heapify(n, largest, paneWidth);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
