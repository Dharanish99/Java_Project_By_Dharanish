package com.ocrsearch.service;

import com.ocrsearch.model.Document;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport; 
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class SearchService {

    private final ElasticsearchClient esClient;
    private final String INDEX_NAME;

    public SearchService() {
        String host = "localhost";
        int port = 9200;
        String indexName = "documents";

        try (FileInputStream fis = new FileInputStream("src/main/resources/application.properties")) {
            Properties prop = new Properties();
            prop.load(fis);
            host = prop.getProperty("es.host");
            port = Integer.parseInt(prop.getProperty("es.port"));
            indexName = prop.getProperty("es.index.name");
        } catch (IOException | NumberFormatException e) {
            System.err.println("❌ ERROR: Could not load application.properties for ES setup. Using defaults. " + e.getMessage());
        }

        this.INDEX_NAME = indexName;
        
        RestClient restClient = RestClient.builder(new HttpHost(host, port, "http")).build();


        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());


        this.esClient = new ElasticsearchClient(transport);

        System.out.println("✅ SearchService initialized. ES Host: " + host + ":" + port);
        setupIndex();
    }
    
    
    private void setupIndex() {
        try {
            ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(INDEX_NAME));
            boolean exists = esClient.indices().exists(existsRequest).value();

            if (!exists) {
                System.out.println("⏳ Elasticsearch index not found. Creating index: " + INDEX_NAME);

                
                TypeMapping mapping = TypeMapping.of(m -> m
                    .properties("id", p -> p.keyword(k -> k)) 
                    .properties("filename", p -> p.keyword(k -> k))
                    .properties("text", p -> p.text(t -> t.analyzer("english"))) 
                    .properties("confidence", p -> p.double_(d -> d))
                    .properties("uploadTime", p -> p.date(d -> d))
                );

                CreateIndexResponse createResponse = esClient.indices().create(c -> c
                    .index(INDEX_NAME)
                    .mappings(mapping)
                );
                
                if (createResponse.acknowledged()) {
                    System.out.println("✅ Index created successfully.");
                } else {
                    System.err.println("❌ Index creation failed.");
                }
            }
        } catch (IOException e) {
            System.err.println("❌ ES Index Setup Error (Is Elasticsearch running?): " + e.getMessage());
        }
    }

    
    public void indexDocument(Document doc, int dbId) {
        if (dbId == -1) {
            System.err.println("❌ Cannot index document: Invalid DB ID.");
            return;
        }
        try {
            
            IndexResponse response = esClient.index(i -> i
                .index(INDEX_NAME)
                .id(String.valueOf(dbId))
                .document(doc)
            );
            
            if (response.result() == Result.Created || response.result() == Result.Updated) {
                 System.out.println("✅ ES Index successful. Document ID: " + response.id());
            } else {
                 System.err.println("❌ ES Index failed. Result: " + response.result());
            }

        } catch (IOException e) {
            System.err.println("❌ ES Index Error: " + e.getMessage());
        }
    }

    
    public void searchDocuments(String keyword) {
        System.out.println("⏳ Searching Elasticsearch for keyword: '" + keyword + "'...");
        try {
            SearchResponse<Document> response = esClient.search(s -> s
                .index(INDEX_NAME)
                .query(q -> q
                    .match(t -> t 
                        .field("text") 
                        .query(keyword)
                    )
                )
                .highlight(h -> h 
                    .fields("text", f -> f
                        .preTags(">>>") 
                        .postTags("<<<") 
                        .numberOfFragments(1)
                        .fragmentSize(150)
                    )
                )
                .size(10), 
                Document.class
            );

            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            System.out.println("\n--- Search Results --- (Total Hits: " + total + ")");

            if (total == 0) {
                System.out.println("No matching documents found.");
                return;
            }

            response.hits().hits().forEach(hit -> {
                String filename = hit.source().getFilename();
                String snippet = "N/A";
                
                
                if (hit.highlight() != null && hit.highlight().containsKey("text")) {
                    snippet = String.join("...", hit.highlight().get("text"));
                }

                System.out.println("------------------------------------");
                System.out.printf("📄 Filename: %s\n", filename);
                System.out.printf("⭐ Score: %.4f\n", hit.score());
                System.out.println("🔗 Snippet: " + snippet.replace(">>>", "**").replace("<<<", "**"));
            });
            System.out.println("------------------------------------");

        } catch (IOException e) {
            System.err.println("❌ ES Search Error: " + e.getMessage());
        }
    }
}
