package org.automonius.Actions.FileSystem;

import org.automonius.Annotations.ActionMeta;

import java.io.File;
import java.nio.file.Files;

public class FileReader {
    @ActionMeta(
            objectName = "FileSystem",
            description = "Verify that a file contains a given keyword",
            inputs = {"path", "keyword"}
    )
    public static boolean checkFileContainsKeyword(String path, String keyword) {
        File file = new File(path);
        if (!file.exists() || !file.canRead()) return false;
        try {
            return Files.readString(file.toPath()).contains(keyword);
        } catch (Exception e) {
            return false;
        }
    }
}
