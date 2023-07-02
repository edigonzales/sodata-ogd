package ch.so.agi.ogd;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.interlis.iom.IomObject;
import ch.so.agi.ogd.service.ConfigService;

@RestController
public class MainController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${lucene.queryDefaultRecords}")
    private Integer QUERY_DEFAULT_RECORDS;

    @Value("${lucene.queryMaxRecords}")
    private Integer QUERY_MAX_RECORDS;   

    @Autowired
    Settings settings;
    
    @Autowired
    ConfigService configService;

    @PostConstruct
    public void init() throws Exception {
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        log.info("Hallo Welt");
        return new ResponseEntity<String>("sodata-ogd", HttpStatus.OK);
    }
    
    @GetMapping("/foo")
    public ResponseEntity<Map<String,Object>> foo() {
        return new ResponseEntity<Map<String,Object>>(configService.iomObjMap, HttpStatus.OK);
    }
}
