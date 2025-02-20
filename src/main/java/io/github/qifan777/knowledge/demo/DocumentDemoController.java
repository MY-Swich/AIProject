package io.github.qifan777.knowledge.demo;

import io.qifan.ai.dashscope.DashScopeAiEmbeddingModel;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RequestMapping("demo/document")
@RestController
@AllArgsConstructor
public class DocumentDemoController {
    private final DashScopeAiEmbeddingModel embeddingModel;

    private final VectorStore vectorStore;

    @SneakyThrows
    @PostMapping("etl/reader")
    public String readFormMultipart(@RequestParam MultipartFile file){
        Resource resource = new InputStreamResource(file.getInputStream());
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);

        return tikaDocumentReader.get().get(0).getContent();
    }

    @PostMapping("etl/reade/local-file")
    public String readFromLocalFile(@RequestParam String path){
        FileSystemResource fileSystemResource = new FileSystemResource(path);

        return new TikaDocumentReader(fileSystemResource).read().get(0).getContent();

    }

    @SneakyThrows
    @PostMapping("etl/transfform/slit")
    public List<String> split(@RequestParam MultipartFile file){
        InputStreamResource inputStreamResource = new InputStreamResource(file.getInputStream());
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(inputStreamResource);
        List<Document> read = tikaDocumentReader.read();
        List<Document> split = new TokenTextSplitter().split(read);

        return split.stream().map(Document::getContent).toList();

    }
    @SneakyThrows
    @PostMapping("etl/write/vector")
    public void writeVector(@RequestParam MultipartFile file){
        InputStreamResource inputStreamResource = new InputStreamResource(file.getInputStream());
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(inputStreamResource);
        List<Document> read = tikaDocumentReader.read();
        List<Document> split = new TokenTextSplitter().split(read);

        vectorStore.add(split);
    }

    @PostMapping("query")
    public List<Document> query(@RequestParam String query){
            return vectorStore.similaritySearch(query);
    }


    @PostMapping("embedding")
    public List<Double> embedding (@RequestParam  String text){
        float[] embed = embeddingModel.embed(text);
        List<Double> doubleList = IntStream.range(0, embed.length)
                .mapToObj(i -> (double) embed[i])
                .collect(Collectors.toList());
        return doubleList;
    }
}
