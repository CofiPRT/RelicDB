package ro.cofi.relicdb;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;
import ro.cofi.relicdb.io.DBChoice;
import ro.cofi.relicdb.io.DBFileManager;
import ro.cofi.relicdb.io.DBScraper;
import ro.cofi.relicdb.logic.RelicPart;
import ro.cofi.relicdb.logic.RelicType;
import ro.cofi.relicdb.logic.Stat;
import ro.cofi.relicdb.scoring.AnalysisFilters;
import ro.cofi.relicdb.scoring.AnalysisRecipe;
import ro.cofi.relicdb.scoring.MainStatScoreType;
import ro.cofi.relicdb.scoring.RankScoreType;

import java.awt.Desktop;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

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
    private ChoiceBox<RelicType> inputRelicType;
    @FXML
    private ChoiceBox<RelicPart> inputRelicPart;
    @FXML
    private ChoiceBox<String> inputSetName;
    @FXML
    private ChoiceBox<Stat> inputMainStat;
    @FXML
    private ChoiceBox<Stat> inputSubstat1;
    @FXML
    private ChoiceBox<Stat> inputSubstat2;
    @FXML
    private ChoiceBox<Stat> inputSubstat3;
    @FXML
    private ChoiceBox<Stat> inputSubstat4;
    @FXML
    private ChoiceBox<RankScoreType> filterSetScore;
    @FXML
    private ChoiceBox<MainStatScoreType> filterMainStatScore;
    @FXML
    private ChoiceBox<Integer> filterSubStatCount;
    @FXML
    private Button analysisButton;
    @FXML
    private ProgressIndicator analysisProgress;
    @FXML
    private WebView analysisResult;

    private final Map<ChoiceBox<Stat>, ChangeListener<Stat>> inputSubstatListenerMap = new LinkedHashMap<>();

    private final Runnable directoryListener = () -> executeUI(() -> {
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
    });

    private final ChangeListener<DBChoice> choiceChangeListener = (observable, oldValue, newValue) -> {
        unloadChoice();

        updateNode(dbVersionPane, true, false);
        executeWithProgress(dbVersionProgress, () -> {
            try {
                LOGGER.info(
                    "Loading DB file \"{}\", at path {}",
                    newValue, newValue.getFile().getAbsolutePath()
                );
                loadedDB = dbFileManager.loadDBFile(newValue);

                executeUI(() -> {
                    if (loadedDB != null)
                        showAnalysis();

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

    private final EventHandler<ActionEvent> dbVersionUpdateButtonListener = event -> {
        updateNode(dbVersionPane, true, false);
        updateNode(analysisPane, false, false);
        executeWithProgress(dbVersionProgress, () -> {
            try {
                LOGGER.info("Pulling info from the web...");
                String newFilePath = dbFileManager.storeDBFile(dbScraper.scrape());
                LOGGER.info("Update complete. Written to file: {}", newFilePath);

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
    };

    private final EventHandler<ActionEvent> dbVersionExplorerButtonListener = event -> {
        try {
            dbFileManager.openExplorer(loadedDB.getDBChoice());
        } catch (Exception e) {
            errorAlert("Could not open explorer", e);
        }
    };

    private final ChangeListener<RelicType> inputRelicTypeListener = (observable, oldValue, newValue) -> {
        inputRelicPart.getSelectionModel().clearSelection();
        inputSetName.getSelectionModel().clearSelection();

        if (newValue == null) {
            updateNode(inputRelicPart, true, false);
            updateNode(inputSetName, true, false);
            return;
        }

        inputRelicPart.getItems().setAll(newValue.getParts());
        updateNode(inputRelicPart, true, true);

        inputSetName.getItems().setAll(loadedDB.getWeaponNames(newValue.getJsonKey()));
        updateNode(inputSetName, true, true);
    };

    private final ChangeListener<RelicPart> inputRelicPartListener = (observable, oldValue, newValue) -> {
        inputMainStat.getSelectionModel().clearSelection();

        if (newValue == null) {
            updateNode(inputMainStat, true, false);
            return;
        }

        List<Stat> mainStats = newValue.getAvailableStats();
        inputMainStat.getItems().setAll(newValue.getAvailableStats());

        if (mainStats.size() == 1)
            inputMainStat.getSelectionModel().selectFirst();
        else
            inputMainStat.getSelectionModel().clearSelection();

        updateNode(inputMainStat, true, inputMainStat.getItems().size() > 1);
    };

    private final ChangeListener<String> inputRelicNameListener = (observable, oldValue, newValue) ->
        testReadyForAnalysis();

    private final ChangeListener<Stat> inputMainStatListener = (observable, oldValue, newValue) -> {
        testReadyForAnalysis();

        List<Stat> subStatChoices = new ArrayList<>(Arrays.asList(Stat.valuesWithNull()));

        if (newValue != null)
            subStatChoices.remove(newValue);

        inputSubstatListenerMap.keySet().forEach(inputSubStat -> {
            Stat previousSelection = inputSubStat.getSelectionModel().getSelectedItem();

            clearSelectionFromSubstat(inputSubStat, newValue);

            // remove listener before replacing
            removeSubstatListener(inputSubStat);
            inputSubStat.getItems().setAll(subStatChoices);
            readdSubstatListener(inputSubStat);

            if (previousSelection != newValue)
                inputSubStat.getSelectionModel().select(previousSelection);
        });
    };

    private final EventHandler<ActionEvent> analysisButtonListener = event -> {
        updateNode(dbVersionPane, true, false);
        updateNode(analysisPane, true, false);
        analysisResult.getEngine().loadContent("");
        executeWithProgress(analysisProgress, () -> {
            try {
                RelicType relicType = inputRelicType.getSelectionModel().getSelectedItem();
                RelicPart relicPart = inputRelicPart.getSelectionModel().getSelectedItem();
                String relicName = inputSetName.getSelectionModel().getSelectedItem();
                Stat mainStat = inputMainStat.getSelectionModel().getSelectedItem();
                Stat substat1 = inputSubstat1.getSelectionModel().getSelectedItem();
                Stat substat2 = inputSubstat2.getSelectionModel().getSelectedItem();
                Stat substat3 = inputSubstat3.getSelectionModel().getSelectedItem();
                Stat substat4 = inputSubstat4.getSelectionModel().getSelectedItem();

                AnalysisRecipe recipe = new AnalysisRecipe(
                    relicType, relicPart, relicName,
                    mainStat, substat1, substat2, substat3, substat4
                );

                AnalysisFilters filters = new AnalysisFilters(
                    filterSetScore.getSelectionModel().getSelectedItem().getHigherScores(),
                    filterMainStatScore.getSelectionModel().getSelectedItem().getHigherScores(),
                    filterSubStatCount.getSelectionModel().getSelectedItem()
                );

                String analysisResultHTML = loadedDB.analyzeItem(recipe, filters);

                LOGGER.info("Analysis complete");

                executeUI(() -> analysisResult.getEngine().loadContent(analysisResultHTML));
            } catch (Exception e) {
                errorAlert("Could not perform analysis", e);
            } finally {
                executeUI(() -> {
                    updateNode(dbVersionPane, true, true);
                    updateNode(analysisPane, true, true);
                });
            }
        });
    };

    private final ChangeListener<Worker.State> analysisResultListener = (observable, oldState, newState) -> {
        if (newState != Worker.State.SUCCEEDED)
            return;

        // add listener to override the default hyperlink handling
        Document document = analysisResult.getEngine().getDocument();
        NodeList nodeList = document.getElementsByTagName("a");

        for (int i = 0; i < nodeList.getLength(); i++) {
            org.w3c.dom.Node node = nodeList.item(i);
            EventTarget eventTarget = (EventTarget) node;
            eventTarget.addEventListener("click", evt -> {
                evt.preventDefault();

                EventTarget target = evt.getCurrentTarget();
                HTMLAnchorElement anchorElement = (HTMLAnchorElement) target;
                String href = anchorElement.getHref();

                if (Desktop.isDesktopSupported() &&
                    Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        Desktop.getDesktop().browse(new URI(href));
                    } catch (IOException | URISyntaxException e) {
                        errorAlert("Could not open link", e);
                    }
                }

            }, false);
        }
    };

    @FXML
    private void initialize() {
        LOGGER.info("Initializing controller");

        inputSubstatListenerMap.put(inputSubstat1, null);
        inputSubstatListenerMap.put(inputSubstat2, null);
        inputSubstatListenerMap.put(inputSubstat3, null);
        inputSubstatListenerMap.put(inputSubstat4, null);

        // assure visibility
        updateNode(dbVersionPane, true, true);
        updateNode(dbVersionChoice, true, true);
        updateNode(dbVersionUpdateButton, true, true);
        updateNode(dbVersionExplorerButton, true, true);
        updateNode(dbVersionProgress, false, true);

        updateNode(analysisPane, false, false);
        updateNode(analysisProgress, false, true);
        updateNode(analysisResult, true, true);
        disableAnalysisChoices(false, false);

        // add listener for file changes
        dbFileManager.addDirectoryListener(directoryListener);

        try {
            LOGGER.info("Initializing directory monitor");
            dbFileManager.initDirectoryMonitor();
        } catch (Exception e) {
            errorAlert("Could not initialize directory monitor", e);
        }

        // set up DB version choices
        removeChoiceListener();
        clearChoiceSelection();
        setChoices(discoverChoices());
        addChoiceListener();
        selectFirstChoice();
        dbVersionUpdateButton.setOnAction(dbVersionUpdateButtonListener);
        dbVersionExplorerButton.setOnAction(dbVersionExplorerButtonListener);

        // set up analysis choices
        inputRelicType.getItems().setAll(RelicType.values());
        inputRelicType.getSelectionModel().selectedItemProperty().addListener(inputRelicTypeListener);
        inputRelicPart.getSelectionModel().selectedItemProperty().addListener(inputRelicPartListener);
        inputSetName.getSelectionModel().selectedItemProperty().addListener(inputRelicNameListener);
        inputMainStat.getSelectionModel().selectedItemProperty().addListener(inputMainStatListener);
        inputSubstatListenerMap.keySet().forEach(inputSubstat ->
            inputSubstat.getItems().setAll(Stat.valuesWithNull())
        );
        inputSubstatListenerMap.keySet().forEach(inputSubstat -> {
            ChangeListener<Stat> listener = getSubstatChangeListener(inputSubstat);
            inputSubstat.getSelectionModel().selectedItemProperty().addListener(listener);
            inputSubstatListenerMap.put(inputSubstat, listener);
        });
        filterSetScore.getItems().setAll(RankScoreType.values());
        filterSetScore.getSelectionModel().select(RankScoreType.ACCEPTABLE);
        filterMainStatScore.getItems().setAll(MainStatScoreType.values());
        filterMainStatScore.getSelectionModel().select(MainStatScoreType.IDEAL);
        setFilterSubStatMaxCount(0);
        filterSubStatCount.getSelectionModel().select(0);
        analysisButton.setOnAction(analysisButtonListener);
        analysisResult.getEngine().getLoadWorker().stateProperty().addListener(analysisResultListener);
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
        dbVersionChoice.getItems().setAll(dbChoices);
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

    private void disableAnalysisChoices(boolean enableRelicType, boolean enableSubstats) {
        updateNode(inputRelicType, true, enableRelicType);
        updateNode(inputRelicPart, true, false);
        updateNode(inputSetName, true, false);
        updateNode(inputMainStat, true, false);
        inputSubstatListenerMap.keySet().forEach(inputSubstat -> updateNode(inputSubstat, true, enableSubstats));
        updateNode(analysisButton, true, false);

        // deselect
        inputRelicType.getSelectionModel().clearSelection();
        inputRelicPart.getSelectionModel().clearSelection();
        inputSetName.getSelectionModel().clearSelection();
        inputMainStat.getSelectionModel().clearSelection();
        inputSubstatListenerMap.keySet().forEach(inputSubstat -> inputSubstat.getSelectionModel().clearSelection());
    }

    private void testReadyForAnalysis() {
        boolean ready = inputRelicType.getSelectionModel().getSelectedItem() != null
                        && inputRelicPart.getSelectionModel().getSelectedItem() != null
                        && inputSetName.getSelectionModel().getSelectedItem() != null
                        && inputMainStat.getSelectionModel().getSelectedItem() != null;

        updateNode(analysisButton, true, ready);
    }

    private void clearSelectionFromSubstat(ChoiceBox<Stat> inputSubStat, Stat mainStat) {
        if (inputSubStat.getSelectionModel().getSelectedItem() != mainStat)
            return;

        inputSubStat.getSelectionModel().clearSelection();
    }

    private void removeSubstatListener(ChoiceBox<Stat> inputSubstat) {
        ChangeListener<Stat> listener = inputSubstatListenerMap.get(inputSubstat);
        inputSubstat.getSelectionModel().selectedItemProperty().removeListener(listener);
    }

    private void readdSubstatListener(ChoiceBox<Stat> inputSubstat) {
        ChangeListener<Stat> listener = inputSubstatListenerMap.get(inputSubstat);
        inputSubstat.getSelectionModel().selectedItemProperty().addListener(listener);
    }

    private ChangeListener<Stat> getSubstatChangeListener(ChoiceBox<Stat> callingInputSubstat) {
        return (observable, oldValue, newValue) -> {
            if (newValue != null) {
                // clear selection from other substats
                inputSubstatListenerMap.keySet().stream()
                    .filter(inputSubstat -> inputSubstat != callingInputSubstat)
                    .forEach(inputSubstat -> clearSelectionFromSubstat(inputSubstat, newValue));
            }

            // assure that the filter cannot be set to more than the number of set substats
            long setSubstatCount = inputSubstatListenerMap.keySet().stream()
                .filter(inputSubstat -> inputSubstat.getSelectionModel().getSelectedItem() != null)
                .count();

            setFilterSubStatMaxCount((int) setSubstatCount);
        };
    }

    private void setFilterSubStatMaxCount(int maxCount) {
        // from 0 to maxCount, inclusive
        Integer previousSelection = filterSubStatCount.getSelectionModel().getSelectedItem();

        filterSubStatCount.getItems().setAll(IntStream.rangeClosed(0, maxCount).boxed().toList());

        Integer newSelection = previousSelection != null ? Math.min(previousSelection, maxCount) : 0;
        filterSubStatCount.getSelectionModel().select(newSelection);
    }

    private void showAnalysis() {
        updateNode(analysisPane, true, true);

        // disable all choices except relic type and substats
        disableAnalysisChoices(true, true);
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

    public void shutdown() {
        LOGGER.info("Shutting down...");
        executorService.shutdown();
        dbFileManager.shutdown();
    }

}