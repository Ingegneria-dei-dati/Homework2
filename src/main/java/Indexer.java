import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;

public class Indexer {

    private static final String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();
    private static final String DOCS_PATH = "dataset" + FILE_SEPARATOR;
    private static final String INDEX_PATH = "lucene_index" + FILE_SEPARATOR;

    public static void main(String[] args) {
        // Avvio del cronometro globale
        long startTime = System.currentTimeMillis();

        try {
            indexDocs();
        } catch (IOException e) {
            System.err.println("Errore durante l'indicizzazione: " + e.getMessage());
            e.printStackTrace();
        }

        // Calcolo del tempo totale
        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);

        System.out.println("----------------------------------------");
        System.out.printf("Tempo Totale di Esecuzione: %.3f secondi%n", (double) duration / 1000.0);
        System.out.println("----------------------------------------");
    }

    private static void indexDocs() throws IOException {
        System.out.println("Inizio indicizzazione...");

        Directory dir = FSDirectory.open(Paths.get(INDEX_PATH));
        Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();

        // CONFIGURAZIONE DELL'ANALYZER
        perFieldAnalyzers.put("nome_file", new KeywordAnalyzer()); // Ricerca esatta con supporto PhraseQuery
        perFieldAnalyzers.put("contenuto", new EnglishAnalyzer()); // Ricerca full-text

        // Uso del PerFieldAnalyzerWrapper (EnglishAnalyzer come default)
        Analyzer perFieldAnalyzer = new PerFieldAnalyzerWrapper(new EnglishAnalyzer(), perFieldAnalyzers);

        IndexWriterConfig iwc = new IndexWriterConfig(perFieldAnalyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        IndexWriter writer = new IndexWriter(dir, iwc);

        File docsDir = new File(DOCS_PATH);
        if (!docsDir.exists() || !docsDir.isDirectory()) {
            System.err.println("La directory dei documenti non esiste o non è una directory valida: " + DOCS_PATH);
            return;
        }

        int indexedCount = 0;

        long indexingStartTime = System.currentTimeMillis();

        File[] files = docsDir.listFiles();
        if (files != null) {
            for (File file : files) {
                // Filtra solo i file .txt, SENZA FORZARE IL NOME A MINUSCOLO
                if (file.isFile() && file.getName().toLowerCase().endsWith(".txt")) {
                    Document doc = new Document();

                    // Uso del nome file originale.
                    String fileName = file.getName();

                    // nome_file (TextField + KeywordAnalyzer): memorizza i dati di posizione
                    doc.add(new TextField("nome_file", fileName, Field.Store.YES));

                    String content = readFile(file);
                    // contenuto (TextField + EnglishAnalyzer)
                    doc.add(new TextField("contenuto", content, Field.Store.YES));

                    writer.addDocument(doc);
                    indexedCount++;
                }
            }
        }

        long indexingEndTime = System.currentTimeMillis();
        long indexingDuration = (indexingEndTime - indexingStartTime);

        writer.commit();
        writer.close();
        dir.close();

        System.out.printf("\n Indicizzazione completata: %d file indicizzati.%n", indexedCount);
        System.out.printf("Tempo di I/O e Scrittura Documenti: %.3f secondi%n", (double) indexingDuration / 1000.0);
    }

    private static String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}
