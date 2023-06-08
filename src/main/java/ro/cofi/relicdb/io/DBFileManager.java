package ro.cofi.relicdb.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ro.cofi.relicdb.LoadedDB;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DBFileManager {

    private static final Logger LOGGER = LogManager.getLogger(DBFileManager.class);
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String STORAGE_DIR = "RelicDBVersions";
    private static final long POLL_INTERVAL = 1000;

    private final FileAlterationObserver observer = new FileAlterationObserver(STORAGE_DIR);
    private final FileAlterationMonitor monitor = new FileAlterationMonitor(POLL_INTERVAL);
    private final List<Runnable> directoryListeners = Collections.synchronizedList(new ArrayList<>());

    private long lastDirectoryChangeTimestamp;

    public List<DBChoice> discoverChoices() {
        List<DBChoice> choices = createChoices();

        lastDirectoryChangeTimestamp = System.currentTimeMillis();

        if (choices.isEmpty())
            return Collections.singletonList(DBChoice.DUMMY);

        return choices;
    }

    private List<DBChoice> createChoices() {
        // if the storage directory does not exist, or if it is empty, return a dummy choice
        File storageDir = new File(STORAGE_DIR);
        if (!storageDir.exists())
            return Collections.emptyList();

        File[] files = storageDir.listFiles();
        if (files == null)
            return Collections.emptyList();

        // otherwise, return a list of choices
        return Arrays.stream(files)
            .filter(file -> file.getName().endsWith(".json")) // only search for .json files
            .map(file -> { // map each file to a DBChoice
                BasicFileAttributes attr;

                try {
                    attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                } catch (IOException e) {
                    LOGGER.error(String.format("Could not read attributes for file %s", file.getName()), e);
                    return null;
                }

                return new DBChoice(file, attr.lastModifiedTime().toMillis());
            })
            .filter(Objects::nonNull) // filter out null values
            .sorted() // sort by last modified date
            .toList();
    }

    public void storeDBFile(JsonObject data) throws IOException {
        // write the file to the storage directory - its name will be the current timestamp
        File storageDir = new File(STORAGE_DIR);
        if (!storageDir.exists() && !storageDir.mkdir())
            throw new IOException(String.format("Could not create storage directory %s", STORAGE_DIR));

        long timestamp = System.currentTimeMillis();

        File dbFile = new File(STORAGE_DIR, String.format("%d.json", timestamp));

        try {
            // write with pretty printing
            Files.writeString(dbFile.toPath(), PRETTY_GSON.toJson(data));
        } catch (IOException e) {
            throw new IOException("Could not write DB file", e);
        }

        lastDirectoryChangeTimestamp = System.currentTimeMillis();
    }

    public LoadedDB loadDBFile(DBChoice choice) throws IOException {
        try {
            File file = choice.getFile();
            if (file == null)
                return null;

            JsonObject data = PRETTY_GSON.fromJson(Files.readString(choice.getFile().toPath()), JsonObject.class);
            return new LoadedDB(data, choice);
        } catch (IOException e) {
            throw new IOException("Could not read DB file", e);
        }
    }

    public void openExplorer(DBChoice choice) throws IOException {
        Runtime.getRuntime().exec("explorer.exe /select," + choice.getFile().getAbsolutePath());
    }

    public void initDirectoryMonitor() throws Exception {
        FileAlterationListener listener = new FileAlterationListenerAdaptor() {
            @Override
            public void onFileCreate(File file) {
                fireListeners();
            }

            @Override
            public void onFileDelete(File file) {
                fireListeners();
            }

            @Override
            public void onFileChange(File file) {
                fireListeners();
            }
        };

        observer.addListener(listener);
        monitor.addObserver(observer);
        monitor.start();
    }

    public void addDirectoryListener(Runnable listener) {
        directoryListeners.add(listener);
    }

    private void fireListeners() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDirectoryChangeTimestamp < POLL_INTERVAL)
            return;

        lastDirectoryChangeTimestamp = currentTime;
        directoryListeners.forEach(Runnable::run);
    }
}
