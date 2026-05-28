package io.github.qifan777.knowledge.infrastructure.config;

import io.qifan.infrastructure.common.model.PageResult;
import java.util.List;
import org.springframework.boot.jackson.JacksonComponent;
import org.springframework.data.domain.Page;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;

@JacksonComponent
public class PageableConvert {

  public static class Serializer extends ValueSerializer<Page<?>> {

    @Override
    public void serialize(Page<?> page, JsonGenerator jsonGenerator,
        SerializationContext serializationContext) {
      PageResult<?> pageResult = new PageResult<>()
          .setNumber(page.getNumber())
          .setSize(page.getSize())
          .setTotalElements(page.getTotalElements())
          .setTotalPages(page.getTotalPages())
          .setContent((List<Object>) page.getContent());
      serializationContext.writeValue(jsonGenerator, pageResult);
    }
  }

  public static class Deserializer extends ValueDeserializer<Page<?>> {


    @Override
    public Page<?> deserialize(JsonParser jsonParser,
        DeserializationContext deserializationContext) {
      return jsonParser.readValueAs(Page.class);
    }
  }
}
