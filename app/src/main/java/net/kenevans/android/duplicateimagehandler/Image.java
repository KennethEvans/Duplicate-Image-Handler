package net.kenevans.android.duplicateimagehandler;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class Image {
    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("E MMM d, yyyy HH:mm:ss", Locale.US);
    private long id;
    private String path;
    private String name;
    private String mimetype;
    private long size;
    private long dateModified;
    private long dateTaken;
    //    private String fingerPrint;
    private long finger;
    private boolean valid = true;
    private boolean similar;
    private boolean checked;

    @androidx.annotation.NonNull
    @Override
    public String toString() {
        StringBuilder info = new StringBuilder();
        info.append(getPath()).append("\n");
        String readableSize =
                Utils.humanReadableByteCountBin(getSize());
        info.append(readableSize).append("\n");
        info.append("Date modified: ").append(getFormattedDateModified()).append("\n");
        info.append("Date taken: ").append(getFormattedDateTaken()).append(
                "\n");
        return info.toString();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getDateModified() {
        return dateModified;
    }

    public String getFormattedDateModified() {
        if (dateModified == 0) {
            return "NA";
        }
        return dateFormat.format(dateModified);
    }

    public void setDateModified(long dateModified) {
        this.dateModified = dateModified;
    }

    public long getDateTaken() {
        return dateTaken;
    }

    public String getFormattedDateTaken() {
        if (dateTaken == 0) {
            return "NA";
        }
        return dateFormat.format(dateTaken);
    }

    public void setDateTaken(long dateTaken) {
        this.dateTaken = dateTaken;
    }

    //    public String getFingerPrint() {
//        return fingerPrint;
//    }
//
//    public void setFingerPrint(String fingerPrint) {
//        this.fingerPrint = fingerPrint;
//    }

    public long getFinger() {
        return finger;
    }

    public void setFinger(long finger) {
        this.finger = finger;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isSimilar() {
        return similar;
    }

    public void setSimilar(boolean similar) {
        this.similar = similar;
    }
}
