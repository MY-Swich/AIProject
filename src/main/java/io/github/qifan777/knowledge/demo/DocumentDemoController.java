package io.github.qifan777.knowledge.demo;

import org.springframework.ai.embedding.EmbeddingModel;
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
    private final EmbeddingModel embeddingModel;

    private final VectorStore vectorStore;

    /**
     * 读取上传的文档文件并提取文本内容
     *
     * @param file 前端上传的文件（PDF/DOCX/TXT 等 Tika 支持格式）
     * @return 文档全文
     */
    @SneakyThrows
    @PostMapping("etl/reader")
    public String readFormMultipart(@RequestParam MultipartFile file){
        Resource resource = new InputStreamResource(file.getInputStream());
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);

        return tikaDocumentReader.get().get(0).getText();
    }

    /**
     * 读取服务器本地文件并提取文本内容
     *
     * @param path 服务器本地文件绝对路径
     * @return 文档全文
     */
    @PostMapping("etl/reade/local-file")
    public String readFromLocalFile(@RequestParam String path){
        FileSystemResource fileSystemResource = new FileSystemResource(path);

        return new TikaDocumentReader(fileSystemResource).read().get(0).getText();

    }

    /**
     * 上传文档 → 提取文本 → Token 文本分片
     *
     * @param file 前端上传的文档文件
     * @return 分片后的文本列表
     */
    @SneakyThrows
    @PostMapping("etl/transfform/slit")
    public List<String> split(@RequestParam MultipartFile file){
        InputStreamResource inputStreamResource = new InputStreamResource(file.getInputStream());
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(inputStreamResource);
        List<Document> read = tikaDocumentReader.read();
        List<Document> split = TokenTextSplitter.builder().build().split(read);

        return split.stream().map(Document::getText).toList();

    }
    /**
     * 上传文档 → 提取文本 → 分片 → 向量化 → 写入向量库
     *
     * @param file 前端上传的文档文件
     */
    @SneakyThrows
    @PostMapping("etl/write/vector")
    public void writeVector(@RequestParam MultipartFile file){
        InputStreamResource inputStreamResource = new InputStreamResource(file.getInputStream());
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(inputStreamResource);
        List<Document> read = tikaDocumentReader.read();
        List<Document> split = TokenTextSplitter.builder().build().split(read);

        vectorStore.add(split);
    }

    /**
     * 基于向量相似度检索文档
     *
     * @param query 查询文本，自动向量化后与库中向量比对
     * @return 相似度最高的文档列表
     */
    @PostMapping("query")
    public List<Document> query(@RequestParam String query){
            return vectorStore.similaritySearch(query);
    }


    /**
     * 将文本转换为向量（不存储，仅返回 embedding 值）
     *
     * @param text 输入文本
     * @return float 数组转换为 List[Double] 后的向量值
     */
    @PostMapping("embedding")
    public List<Double> embedding (@RequestParam  String text){
        float[] embed = embeddingModel.embed(text);
        List<Double> doubleList = IntStream.range(0, embed.length)
                .mapToObj(i -> (double) embed[i])
                .collect(Collectors.toList());
        return doubleList;
    }
}
