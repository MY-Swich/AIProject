package io.github.qifan777.knowledge.infrastructure.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;

@JacksonComponent
@Slf4j
public class LocalDateTimeConvert {

  public static class Serializer extends ValueSerializer<LocalDateTime> {


    @Override
    public void serialize(LocalDateTime localDateTime, JsonGenerator jsonGenerator,
        SerializationContext serializationContext) {
      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(
          "yyyy-MM-dd HH:mm:ss");
      String format = dateTimeFormatter.format(localDateTime);
      jsonGenerator.writeString(format);
    }
  }

  public static class Deserializer extends ValueDeserializer<LocalDateTime> {


    @Override
    public LocalDateTime deserialize(JsonParser jsonParser,
        DeserializationContext deserializationContext) {
      String text = jsonParser.getText();
      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(
          "yyyy-MM-dd HH:mm:ss");
      return LocalDateTime.parse(text, dateTimeFormatter);
    }
  }
}
