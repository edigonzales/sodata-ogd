package ch.so.agi.ogd.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import ch.interlis.iom.IomObject;

@Repository
public class LuceneDatasetRepository {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private Directory fsIndex;
    //private StandardAnalyzer analyzer;
    // Debugging: https://util.unicode.org/UnicodeJsps/breaks.jsp
    // https://www.baeldung.com/lucene-analyzers
    // Auch die Synonyme scheinen zu greifen.
    private WhitespaceAnalyzer analyzer;
    private QueryParser queryParser;
    private IndexWriter writer;

    // Index-Initialisierung muss vor dem Parsen der XTF-Dateien 
    // gemacht werden: PostConstruct wird vor CommandLineRunner ausgeführt.
    @PostConstruct
    public void init() throws IOException {
        log.info("Prepare index...");
        
        Path tempDirWithPrefix = Files.createTempDirectory(Paths.get(System.getProperty("java.io.tmpdir")), "sodataogdidx");
        log.info("Index folder: " + tempDirWithPrefix);
        
        fsIndex = new NIOFSDirectory(tempDirWithPrefix);
        //analyzer = new StandardAnalyzer();
        analyzer = new WhitespaceAnalyzer();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        writer = new IndexWriter(fsIndex, indexWriterConfig);
    }

    public void saveAll(List<IomObject> iomObjectList) throws IOException {
        for (IomObject iomObj : iomObjectList) {
            log.debug("Write document to Lucene index: <{}>", iomObj.getattrvalue("Identifier"));
            Document document = new Document();
            document.add(new TextField("identifier", iomObj.getattrvalue("Identifier").toLowerCase(), Store.YES));
            document.add(new TextField("title", iomObj.getattrvalue("Title").toLowerCase(), Store.YES));
            if (iomObj.getattrvalue("Description") != null) document.add(new TextField("description", iomObj.getattrvalue("Description").toLowerCase(), Store.YES));
            String agencyNameString = iomObj.getattrobj("Publisher", 0).getattrvalue("AgencyName").toLowerCase();
            String abbreviationString = iomObj.getattrobj("Publisher", 0).getattrvalue("Abbreviation").toLowerCase();
            document.add(new TextField("publisher", agencyNameString+","+abbreviationString, Store.YES));
            if (iomObj.getattrvalue("Theme") != null) document.add(new TextField("theme", String.join(", ", iomObj.getattrvalue("Theme")).toLowerCase(), Store.YES));
            if (iomObj.getattrvalue("Keywords") != null) document.add(new TextField("keywords", String.join(", ", iomObj.getattrvalue("Keywords")).toLowerCase(), Store.YES));
            
            // TODO
            // Eventuell müssen Titel und Description der einzelnen Ressourcen auch irgendwie indexiert werden.
            
            writer.addDocument(document);
        }
        IndexWriter.DocStats docStats = writer.getDocStats();
        writer.close();
        
        log.info("{} files indexed.", docStats.numDocs);
    }

    public List<Map<String, String>> findByQuery(String searchTerms, int numRecords) throws LuceneSearcherException, InvalidLuceneQueryException {
        IndexReader reader = null;
        IndexSearcher indexSearcher = null;
        Query query;
        TopDocs documents;
        
        try {
            reader = DirectoryReader.open(fsIndex);
            indexSearcher = new IndexSearcher(reader);
            queryParser = new QueryParser("title", analyzer); // 'title' is default field if we don't prefix search string
            queryParser.setAllowLeadingWildcard(true); 

            String luceneQueryString = "";
            String[] splitedQuery = searchTerms.split("\\s+");
            for (int i=0; i<splitedQuery.length; i++) {
                String token = QueryParser.escape(splitedQuery[i]);
                // Das Feld, welches bestimmend sein soll (also in der Suche zuoberst gelistet), bekommt
                // einen sehr hohen Boost. Wobei wir im GUI wieder alphabetisch sortieren. Es sorgt aber
                // auch dafür, dass Objekte gefunden werden, die wir für passender halten.      
                // Nachtrag: Weil wir aber alphabetisch sortieren, spielt es keine Rolle (ausser wenn wir
                // das Limit sehr tief setzen würden).
                luceneQueryString += "("
                        + "identifier:*" + token + "*^100 OR "
                        + "title:*" + token + "*^10 OR "
                        + "description:*" + token + "* OR "
                        + "publisher:*" + token + "* OR "
                        + "keywords:*" + token + "* OR "
                        + "theme:*" + token + "*";
                luceneQueryString += ")";
                if (i<splitedQuery.length-1) {
                    luceneQueryString += " AND ";
                }
            }
            
            query = queryParser.parse(luceneQueryString);
            log.debug("'" + luceneQueryString + "' ==> '" + query.toString() + "'");
            
            documents = indexSearcher.search(query, numRecords);
            List<Map<String, String>> mapList = new LinkedList<Map<String, String>>();
            for (ScoreDoc scoreDoc : documents.scoreDocs) {
                Document document = indexSearcher.doc(scoreDoc.doc);
                Map<String, String> docMap = new HashMap<String, String>();
                List<IndexableField> fields = document.getFields();
                for (IndexableField field : fields) {
                    docMap.put(field.name(), field.stringValue());
                }
                mapList.add(docMap);
            }
            
            return mapList;
            
        } catch (ParseException e) {
            e.printStackTrace();
            throw new InvalidLuceneQueryException(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new LuceneSearcherException(e.getMessage());
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ioe) {
                log.warn("Could not close IndexReader: " + ioe.getMessage());
            }
        }
    }

}
