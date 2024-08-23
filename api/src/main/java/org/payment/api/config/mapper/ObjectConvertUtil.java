package org.payment.api.config.mapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class ObjectConvertUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setPropertyNamingStrategy(new PropertyNamingStrategies.SnakeCaseStrategy());
    }

    public static <T> T copyVO(Object fromValue, Class<T> toValueType) {
        try {
            // 새로운 인스턴스를 생성
            T targetInstance = toValueType.getDeclaredConstructor().newInstance();
            // fromValue를 targetInstance로 복사
            objectMapper.updateValue(targetInstance, fromValue);

            return targetInstance;
        } catch (Exception e) {
            throw new RuntimeException("VO copy failed", e);
        }
    }
}