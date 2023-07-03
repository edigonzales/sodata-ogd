package ch.so.agi.ogd.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ehi.ili2db.json.Iox2json;
import ch.ehi.ili2db.json.Iox2jsonUtility;
import ch.interlis.ili2c.Ili2c;
import ch.interlis.ili2c.Ili2cFailure;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.xtf.XtfReader;
import ch.interlis.iox.IoxEvent;
import ch.interlis.iox.IoxException;
import ch.interlis.iox_j.ObjectEvent;
import ch.so.agi.ogd.Utils;
import ch.so.agi.ogd.repository.LuceneDatasetRepository;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
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
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LuceneDatasetRepository luceneDatasetRepository;

    @Value("${app.configDir}")
    private String CONFIG_DIR;   
    
    private Map<String, String> iomObjectJsonMap = new HashMap<>();
    
    public Map<String, String> getIomObjectJsonMap() {
        return iomObjectJsonMap;
    }

    private List<String> iomObjectJsonList = new ArrayList<>();

    public List<String> getIomObjectJsonList() {
        return iomObjectJsonList;
    }
    
    public void parseXtfFiles() throws IOException, IoxException, Ili2cFailure {
        List<Path> xtfFiles = new ArrayList<Path>();
        try (Stream<Path> walk = Files.walk(Paths.get(CONFIG_DIR), 1)) {
            xtfFiles = walk
                    .filter(p -> !Files.isDirectory(p))   
                    .filter(f -> f.toString().endsWith("xtf"))
                    .collect(Collectors.toList());        
        }

        // Für Umwandlung iox2json
        TransferDescription td = getTransferdescription();
        
        List<IomObject> iomObjectList = new ArrayList<>();
        for (Path xtfFile : xtfFiles) {
            XtfReader xtfReader = new XtfReader(xtfFile.toFile());
            
            IoxEvent event = xtfReader.read();
            while (event instanceof IoxEvent) {
                if (event instanceof ObjectEvent) {
                    Map<String,Object> iomObjMap = new HashMap<>();
                    ObjectEvent objectEvent = (ObjectEvent) event;
                    IomObject iomObj = objectEvent.getIomObject();
                    log.debug("TID <{}>", iomObj.getobjectoid());

                    iomObjectList.add(iomObj);
                    
                    IomObject[] iomObjects = new IomObject[] {iomObj};                    
                    Writer writer = new StringWriter();
                    JsonGenerator jg = objectMapper.createGenerator(writer);
                    Iox2jsonUtility.write(jg, iomObjects, td);
                    jg.flush();
                    jg.close();
                    String jsonString = writer.toString();
                    iomObjectJsonList.add(jsonString);
                    iomObjectJsonMap.put(iomObj.getobjectoid(), jsonString);

//                    log.debug("***: "+writer.toString());
                }
                event = xtfReader.read();
            }
        }
        
        luceneDatasetRepository.saveAll(iomObjectList);        
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
