import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Searcher {

    private static final String INDEX_PATH = "C:/Users/catal/OneDrive/Desktop/lucene_index";
    private static final int MAX_HITS = 10;

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            PerFieldAnalyzerWrapper analyzerWrapper = configureAnalyzers();

            Directory dir = FSDirectory.open(Paths.get(INDEX_PATH));
            IndexReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);

            System.out.println("Indice aperto con successo.");

            while (true) {
                System.out.println("\n-------------------------------------------");
                System.out.println("Inserisci la query (o 'exit' per uscire):");
                String rawQuery = scanner.nextLine();

                if (rawQuery.equalsIgnoreCase("exit")) {
                    break;
                }

                performSearch(rawQuery, searcher, analyzerWrapper);
            }

            reader.close();
            dir.close();

        } catch (Exception e) {
            System.err.println("Errore di ricerca: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static PerFieldAnalyzerWrapper configureAnalyzers() {
        Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();

        perFieldAnalyzers.put("nome_file", new KeywordAnalyzer());
        perFieldAnalyzers.put("contenuto", new EnglishAnalyzer());

        Analyzer defaultAnalyzer = new EnglishAnalyzer();
        return new PerFieldAnalyzerWrapper(defaultAnalyzer, perFieldAnalyzers);
    }

    private static void performSearch(String rawQuery, IndexSearcher searcher, PerFieldAnalyzerWrapper analyzerWrapper) throws Exception {

        //Definizione dei campi su cui cercare
        String[] fields = {"nome_file", "contenuto"};

        // Per ogni campo, usiamo il suo Analyzer specifico definito nel wrapper
        Map<String, Analyzer> analyzerMap = new HashMap<>();
        analyzerMap.put("nome_file", new KeywordAnalyzer()); // Ricerca esatta/Phrase
        analyzerMap.put("contenuto", new EnglishAnalyzer()); // Ricerca Full-Text

        // 1. Inizializzazione del MultiFieldQueryParser
        // Questo Parser cerca automaticamente in tutti i campi elencati in 'fields'
        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzerWrapper); //

        // Opzionale: Trattare l'input con OR logico per default
        // parser.setDefaultOperator(QueryParser.Operator.OR);

        // 2. Parsing della Query (Input grezzo)
        // Se l'utente scrive solo "love", il parser crea automaticamente:
        // (nome_file:love) OR (contenuto:love)
        Query query = parser.parse(rawQuery);

        System.out.println("\nQuery Eseguita (Finale): " + query.toString());

        // 3. Esecuzione della Ricerca
        TopDocs hits = searcher.search(query, MAX_HITS);

        System.out.println("Trovati " + hits.totalHits.value + " risultati.");

        // 4. Visualizzazione dei Risultati
        for (int i = 0; i < hits.scoreDocs.length; i++) {
            ScoreDoc scoreDoc = hits.scoreDocs[i];

            Document doc = searcher.doc(scoreDoc.doc);

            String titolo = doc.get("nome_file");
            String contenutoPreview = doc.get("contenuto").substring(0, Math.min(doc.get("contenuto").length(), 100)) + "...";

            System.out.printf("%d. Score: %.4f - File: %s\n", (i + 1), scoreDoc.score, titolo);
            System.out.printf("   Contenuto: %s\n", contenutoPreview);
        }
    }
}


