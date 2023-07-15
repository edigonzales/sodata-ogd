package ch.so.agi.ogd;

import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import ch.interlis.ili2c.Ili2c;
import ch.interlis.ili2c.Ili2cException;
import ch.interlis.ili2c.Ili2cFailure;
import ch.interlis.ili2c.metamodel.TransferDescription;

public class Utils {
    public static File copyResourceToTmpDir(String resource) {
        try {
            InputStream is = Utils.class.getClassLoader().getResourceAsStream(resource);
            if (is==null) return null;
            Path exportDir = Files.createTempDirectory("sodataogdws");
            Path exportedFile = exportDir.resolve(new File(resource).getName());
            Files.copy(is, exportedFile, StandardCopyOption.REPLACE_EXISTING);
            return exportedFile.toFile();            
        } catch (IOException e) {
            return null;
        }
    }
}
