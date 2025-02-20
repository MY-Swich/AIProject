package io.github.qifan777.knowledge.demo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.context.annotation.Description;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.function.Function;
@Component
@Description("文档解析函数")
public class DocumentAnalyzeFunction implements Function<DocumentAnalyzeFunction.Request, String> {
    @Override
    public String apply(Request request) {
        FileSystemResource resource = new FileSystemResource(request.path);
        return new TikaDocumentReader(resource).read().get(0).getContent();
    }

    public record Request(@JsonProperty(required = true)
                          @JsonPropertyDescription("需要解析的文档路径") String path){

    }
}
