package ro.cofi.relicdb;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RelicDBController {

    private static final Logger LOGGER = LogManager.getLogger(RelicDBController.class);

    private final DBFileManager dbFileManager = new DBFileManager();
    private final DBScraper dbScraper = new DBScraper();
    private final ExecutorService executorService = new ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(1)
    );

    private LoadedDB loadedDB;

    @FXML
    private AnchorPane dbVersionPane;
    @FXML
    private ChoiceBox<DBChoice> dbVersionChoice;
    @FXML
    private Button dbVersionUpdateButton;
    @FXML
    private Button dbVersionExplorerButton;
    @FXML
    private ProgressIndicator dbVersionProgress;

    @FXML
    private AnchorPane analysisPane;
    @FXML
    private ChoiceBox<Object> inputRelicName;
    @FXML
    private ChoiceBox<Object> inputMainStat;
    @FXML
    private ChoiceBox<Object> inputSubstat1;
    @FXML
    private ChoiceBox<Object> inputSubstat2;
    @FXML
    private ChoiceBox<Object> inputSubstat3;
    @FXML
    private ChoiceBox<Object> inputSubstat4;
    @FXML
    private Button analysisButton;
    @FXML
    private ProgressIndicator analysisProgress;
    @FXML
    private TextArea analysisResult;

    private final ChangeListener<DBChoice> choiceChangeListener = (observable, oldValue, newValue) -> {
        unloadChoice();

        updateNode(dbVersionPane, true, false);
        executeWithProgress(dbVersionProgress, () -> {
            try {
                loadedDB = dbFileManager.loadDBFile(newValue);
                executeUI(() -> {
                    if (loadedDB != null)
                        updateNode(analysisPane, true, true);

                    updateNode(dbVersionExplorerButton, true, loadedDB != null);
                });
            } catch (Exception e) {
                loadedDB = null;
                executeUI(() -> dbVersionChoice.getSelectionModel().clearSelection());
                errorAlert("Could not load DB", e);
            } finally {
                executeUI(() -> updateNode(dbVersionPane, true, true));
            }
        });
    };

    @FXML
    private void initialize() {
        // assure visibility
        updateNode(dbVersionPane, true, true);
        updateNode(dbVersionChoice, true, true);
        updateNode(dbVersionUpdateButton, true, true);
        updateNode(dbVersionExplorerButton, true, true);
        updateNode(dbVersionProgress, false, true);

        updateNode(analysisPane, false, false);
        updateNode(inputRelicName, true, true);
        updateNode(inputMainStat, true, true);
        updateNode(inputSubstat1, true, true);
        updateNode(inputSubstat2, true, true);
        updateNode(inputSubstat3, true, true);
        updateNode(inputSubstat4, true, true);
        updateNode(analysisButton, true, true);
        updateNode(analysisProgress, false, true);
        updateNode(analysisResult, true, true);

        // set up DB version choices
        removeChoiceListener();
        clearChoiceSelection();
        setChoices(discoverChoices());
        addChoiceListener();
        selectFirstChoice();

        // set the update button
        dbVersionUpdateButton.setOnAction(event -> {
            updateNode(dbVersionPane, true, false);
            updateNode(analysisPane, false, false);
            executeWithProgress(dbVersionProgress, () -> {
                try {
                    dbFileManager.storeDBFile(dbScraper.scrape());
                    executeUI(() -> {
                        removeChoiceListener();
                        clearChoiceSelection();
                        setChoices(discoverChoices());
                        addChoiceListener();
                        selectFirstChoice();
                    });
                } catch (Exception e) {
                    errorAlert("Could not perform DB update", e);
                    executeUI(this::clearChoiceSelection);
                } finally {
                    executeUI(() -> updateNode(dbVersionPane, true, true));
                }
            });
        });

        // set the explorer button
        dbVersionExplorerButton.setOnAction(event -> {
            try {
                dbFileManager.openExplorer(loadedDB.getDBChoice());
            } catch (Exception e) {
                errorAlert("Could not open explorer", e);
            }
        });

        // add listener for file changes
        dbFileManager.addDirectoryListener(() -> executeUI(() -> {
            // save current selection
            DBChoice selectedChoice = dbVersionChoice.getSelectionModel().getSelectedItem();

            removeChoiceListener();
            clearChoiceSelection();
            setChoices(discoverChoices());

            // if the previously selected choice is still available, select it
            if (selectedChoice != null && dbVersionChoice.getItems().contains(selectedChoice))
                dbVersionChoice.getSelectionModel().select(selectedChoice);
            else
                unloadChoice();

            // add the listener after selecting - this way, no DB reload is triggered
            addChoiceListener();
        }));

        try {
            dbFileManager.initDirectoryMonitor();
        } catch (Exception e) {
            errorAlert("Could not initialize directory monitor", e);
        }
    }

    private void errorAlert(String message, Throwable throwable) {
        executeUI(() -> {
            LOGGER.error(message, throwable);

            Alert alert = new Alert(Alert.AlertType.ERROR);
            Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
            alertStage.getIcons().add(RelicDBApplication.ICON);
            alert.setTitle("Error");
            alert.setHeaderText(String.format("%s%n%s", message, "Consider sending the text below to the developer"));

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);

            TextArea textArea = new TextArea(String.format("%s%n%s", throwable, sw));
            textArea.setWrapText(true);
            textArea.setEditable(false);

            alert.getDialogPane().setContent(textArea);
            alert.showAndWait();
        });
    }

    private void unloadChoice() {
        loadedDB = null;
        updateNode(analysisPane, false, false);
        updateNode(dbVersionExplorerButton, true, false);
    }

    private List<DBChoice> discoverChoices() {
        return dbFileManager.discoverChoices();
    }

    private void clearChoiceSelection() {
        dbVersionChoice.getSelectionModel().clearSelection();
    }

    private void setChoices(List<DBChoice> dbChoices) {
        dbVersionChoice.setItems(FXCollections.observableList(dbChoices));
    }

    private void selectFirstChoice() {
        dbVersionChoice.getSelectionModel().selectFirst();
    }

    private void removeChoiceListener() {
        dbVersionChoice.getSelectionModel().selectedItemProperty().removeListener(choiceChangeListener);
    }

    private void addChoiceListener() {
        dbVersionChoice.getSelectionModel().selectedItemProperty().addListener(choiceChangeListener);
    }

    private void updateNode(Node node, boolean visible, boolean enabled) {
        node.setVisible(visible);
        node.setDisable(!enabled);
    }

    private void executeWithProgress(ProgressIndicator indicator, Runnable runnable) {
        updateNode(indicator, true, true);
        executeBackground(() -> {
            try {
                runnable.run();
            } finally {
                executeUI(() -> updateNode(indicator, false, true));
            }
        });
    }

    private void executeBackground(Runnable runnable) {
        executorService.execute(runnable);
    }

    private void executeUI(Runnable runnable) {
        Platform.runLater(runnable);
    }

}