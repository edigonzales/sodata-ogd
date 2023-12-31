package ch.so.agi.ogd;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.interlis.iom.IomObject;
import ch.so.agi.ogd.repository.InvalidLuceneQueryException;
import ch.so.agi.ogd.repository.LuceneDatasetRepository;
import ch.so.agi.ogd.repository.LuceneSearcherException;
import ch.so.agi.ogd.service.ConfigService;

@RestController
public class MainController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${lucene.queryMaxRecords}")
    private Integer QUERY_MAX_RECORDS;   

    @Autowired
    ClientSettings settings;
    
    @Autowired
    ConfigService configService;
    
    @Autowired
    LuceneDatasetRepository luceneDatasetRepository;
    
    @Autowired
    ObjectMapper objectMapper;

    @PostConstruct
    public void init() throws Exception {
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        log.info("Hallo Welt");
        return new ResponseEntity<String>("sodata-ogd", HttpStatus.OK);
    }
    
//    @GetMapping("/foo")
//    public ResponseEntity<Map<String,Object>> foo() {
//        return new ResponseEntity<Map<String,Object>>(configService.getIomObjectJsonMap(), HttpStatus.OK);
//    }
    
    @RequestMapping(value = "/settings", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ClientSettings settings() {
        return settings;
    }

    @RequestMapping(value = "/datasets", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public String searchThemePublications(@RequestParam(value="query", required=false) String searchTerms, @RequestParam(value="theme", required=false) String searchThemes) {
        
        log.debug("searchThemes: " + searchThemes);
        
        if (searchTerms == null || searchTerms.trim().length() == 0) {
            if (searchThemes != null) {
                String[] searchThemesArray = searchThemes.toLowerCase().split(",");
                List<String> filteredList = configService.getIomObjectJsonList().stream()
                    .filter(r -> {
                        Map<String, Object> dataset;
                        try {
                            dataset = objectMapper.readValue(r, HashMap.class);
                            String datasetThemes = (String) dataset.get("Theme");
                            if (datasetThemes!=null) {
                                String[] datasetThemesArray = datasetThemes.toLowerCase().split(",");
                                String[] intersection = Arrays.stream(searchThemesArray)
                                        .distinct()
                                        .filter(x -> Arrays.asList(datasetThemesArray).contains(x))
                                        .toArray(String[]::new);
                                if (intersection.length > 0) {
                                    return true;
                                }                   
                            } else {
                                return false;
                            }
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                            throw new IllegalStateException(e);
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
                return filteredList.toString();
            } else {
                return configService.getIomObjectJsonList().toString();    
            }            
        } else {
            List<Map<String, String>> results = null;
            try {                
                results = luceneDatasetRepository.findByQuery(searchTerms, QUERY_MAX_RECORDS);
                log.debug("Search for '" + searchTerms +"' found " + results.size() + " records");            
            } catch (LuceneSearcherException | InvalidLuceneQueryException e) {
                throw new IllegalStateException(e);
            }

            List<String> resultList = results.stream()
                    .map(r -> {
                        return configService.getIomObjectJsonMap().get(r.get("identifier"));
                    })
                    .filter(r -> {
                        Map<String, Object> dataset;
                        if (searchThemes != null) {
                            String[] searchThemesArray = searchThemes.toLowerCase().split(",");
                            try {
                                dataset = objectMapper.readValue(r, HashMap.class);
                                String datasetThemes = (String) dataset.get("Theme");
                                if (datasetThemes!=null) {
                                    String[] datasetThemesArray = datasetThemes.toLowerCase().split(",");
                                    String[] intersection = Arrays.stream(searchThemesArray)
                                            .distinct()
                                            .filter(x -> Arrays.asList(datasetThemesArray).contains(x))
                                            .toArray(String[]::new);
                                    if (intersection.length > 0) {
                                        log.debug("Filter themes <{}> from search result", searchThemes);
                                        return true;
                                    }                   
                                } else {
                                    return false;
                                }
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                                throw new IllegalStateException(e);
                            }
                            return false;                            
                        } else {
                            return true;
                        }
                    }) 
                    .collect(Collectors.toList());
            return resultList.toString();
        }
    }
}




