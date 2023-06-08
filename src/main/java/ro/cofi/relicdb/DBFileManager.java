package ro.cofi.relicdb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DBFileManager {

    private static final Logger logger = LogManager.getLogger(DBFileManager.class);

    private static final String STORAGE_DIR = "RelicDBVersions";

    public List<DBChoice> discoverChoices() {
        List<DBChoice> choices = internalDiscoverChoices();
        if (choices.isEmpty())
            return Collections.singletonList(DBChoice.DUMMY);

        return choices;
    }

    private List<DBChoice> internalDiscoverChoices() {
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
                    logger.error(String.format("Could not read attributes for file %s", file.getName()), e);
                    return null;
                }

                return new DBChoice(file, attr.lastModifiedTime().toMillis());
            })
            .filter(Objects::nonNull) // filter out null values
            .sorted() // sort by last modified date
            .toList();
    }
}
