package ch.so.agi.ogd.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.interlis.ili2c.Ili2c;
import ch.interlis.ili2c.Ili2cFailure;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.xtf.XtfReader;
import ch.interlis.iox.IoxEvent;
import ch.interlis.iox.IoxException;
import ch.interlis.iox_j.ObjectEvent;
import ch.so.agi.ogd.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ConfigService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
        
    @Value("${app.configDir}")
    private String CONFIG_DIR;   
    
    public IomObject iomObject;
    public Map<String,Object> iomObjMap;

    public void parseXtfFiles() throws IOException, IoxException, Ili2cFailure {
        System.out.println(Paths.get(CONFIG_DIR).toFile().getAbsolutePath());
                
        List<Path> xtfFiles = new ArrayList<Path>();
        try (Stream<Path> walk = Files.walk(Paths.get(CONFIG_DIR), 1)) {
            xtfFiles = walk
                    .filter(p -> !Files.isDirectory(p))   
                    .filter(f -> f.toString().endsWith("xtf"))
                    .collect(Collectors.toList());        
        }

        // Für Umwandlung iox2json
        TransferDescription td = getTransferdescription();
        
        for (Path xtfFile : xtfFiles) {
            XtfReader xtfReader = new XtfReader(xtfFile.toFile());
            
            IoxEvent event = xtfReader.read();
            while (event instanceof IoxEvent) {
                if (event instanceof ObjectEvent) {
                    Map<String,Object> iomObjMap = new HashMap<>();
                    ObjectEvent objectEvent = (ObjectEvent) event;
                    IomObject iomObj = objectEvent.getIomObject();
                    log.debug("TID <{}>", iomObj.getobjectoid());
                    
                    // Mit iox2json propieren.
                    
                    iomObjMap.put("Identifier", iomObj.getattrvalue("Identifier"));
                    iomObjMap.put("Title", iomObj.getattrvalue("Title"));
                    iomObjMap.put("Description", iomObj.getattrvalue("Description"));
                    
                    
                    this.iomObjMap = iomObjMap;
                    this.iomObject = iomObj;
                    

                }
                event = xtfReader.read();
            }
        }
    } 
    
    private TransferDescription getTransferdescription() throws IOException, Ili2cFailure {        
        File iliFile = Utils.copyResourceToTmpDir("ili/SO_OGD_Metadata_20230629.ili");

        ArrayList<String> filev = new ArrayList<String>() {{ add(iliFile.getAbsolutePath()); }};
        TransferDescription td = Ili2c.compileIliFiles(filev, null);

        if (td == null) {
            throw new IllegalArgumentException("INTERLIS compiler failed");
        }

        return td;
    }
}
