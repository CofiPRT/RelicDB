package ro.cofi.relicdb.io;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class DBChoice implements Comparable<DBChoice> {

    public static final DBChoice DUMMY = new DBChoice(null, 0);

    // not static as per rule java:S2885
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private final File file;
    private final long lastModifiedTimestamp;

    public DBChoice(File file, long lastModifiedTimestamp) {
        this.file = file;
        this.lastModifiedTimestamp = lastModifiedTimestamp;
    }

    public File getFile() {
        return file;
    }

    public long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    @Override
    public int compareTo(DBChoice o) {
        // descending
        return Long.compare(o.lastModifiedTimestamp, lastModifiedTimestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DBChoice other)
            return Objects.equals(file, other.file) && lastModifiedTimestamp == other.lastModifiedTimestamp;

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, lastModifiedTimestamp);
    }

    @Override
    public String toString() {
        return this != DUMMY ? dateFormat.format(new Date(lastModifiedTimestamp)) : "None";
    }

}
