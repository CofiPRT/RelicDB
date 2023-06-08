package ro.cofi.relicdb;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RelicDBController {

    private static final Logger logger = LogManager.getLogger(RelicDBController.class);

    private final DBFileManager dbFileManager = new DBFileManager();
    private final DBScraper dbScraper = new DBScraper();
    private final ExecutorService executorService = new ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(1)
    );

    @FXML
    private AnchorPane dbVersionPane;
    @FXML
    private ChoiceBox<DBChoice> dbVersionChoice;
    @FXML
    private Button dbVersionUpdateButton;
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

    @FXML
    private void initialize() {
        // assure visibility
        updateNode(dbVersionPane, true, true);
        updateNode(dbVersionChoice, true, true);
        updateNode(dbVersionUpdateButton, true, true);
        updateNode(dbVersionProgress, false, false);

        updateNode(analysisPane, false, false);

        // populate the DB choices
        List<DBChoice> dbChoices = dbFileManager.discoverChoices();
        dbVersionChoice.setItems(FXCollections.observableList(dbChoices));
        dbVersionChoice.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {

        });
        dbVersionChoice.getSelectionModel().selectFirst();

        try {
            JsonObject data = dbScraper.scrape();
        } catch (IOException e) {
            logger.error("Could not scrape the DB", e);
        }
    }

    private void updateNode(Node node, boolean visible, boolean enabled) {
        node.setVisible(visible);
        node.setDisable(!enabled);
    }

    private void executeWithProgress(ProgressIndicator indicator, Runnable runnable) {
        updateNode(indicator, true, false);
        executeBackground(() -> {
            runnable.run();
            executeUI(() -> updateNode(indicator, false, true));
        });
    }

    private void executeBackground(Runnable runnable) {
        executorService.execute(runnable);
    }

    private void executeUI(Runnable runnable) {
        Platform.runLater(runnable);
    }

}