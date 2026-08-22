package com.backend.commonweb.converter;

import com.fasterxml.jackson.annotation.JsonValue;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

/**
 * query/path parameter enum 변환 시 {@link JsonValue} 직렬화 값과 enum 이름을 모두 허용한다.
 */
public class JsonValueEnumConverterFactory implements ConverterFactory<String, Enum> {

    private static final Map<Class<?>, Converter<String, ?>> CONVERTERS = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
        return (Converter<String, T>) CONVERTERS.computeIfAbsent(targetType, JsonValueEnumConverter::new);
    }

    private static final class JsonValueEnumConverter<T extends Enum<T>> implements Converter<String, T> {

        private final Class<T> enumType;
        private final Map<String, T> lookup;

        @SuppressWarnings("unchecked")
        private JsonValueEnumConverter(Class<?> enumType) {
            this.enumType = (Class<T>) enumType;
            this.lookup = buildLookup(this.enumType);
        }

        private static <T extends Enum<T>> Map<String, T> buildLookup(Class<T> enumType) {
            Map<String, T> map = new HashMap<>();
            for (T constant : enumType.getEnumConstants()) {
                for (String alias : aliases(constant)) {
                    map.putIfAbsent(alias.toLowerCase(), constant);
                }
            }
            return map;
        }

        private static Set<String> aliases(Enum<?> constant) {
            Set<String> aliases = new LinkedHashSet<>();
            aliases.add(constant.name());

            String jsonValue = resolveJsonValue(constant);
            if (jsonValue != null) {
                aliases.add(jsonValue);
            }
            return aliases;
        }

        private static String resolveJsonValue(Enum<?> constant) {
            for (Method method : constant.getClass().getMethods()) {
                if (method.isAnnotationPresent(JsonValue.class)
                        && method.getParameterCount() == 0
                        && method.getReturnType() != Void.TYPE) {
                    try {
                        return String.valueOf(method.invoke(constant));
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(
                                "Failed to read @JsonValue from " + constant.getClass().getSimpleName(), e);
                    }
                }
            }
            return null;
        }

        @Override
        public T convert(String source) {
            if (source == null || source.isBlank()) {
                return null;
            }

            T value = lookup.get(source.toLowerCase());
            if (value != null) {
                return value;
            }

            throw new IllegalArgumentException(
                    "Invalid value '" + source + "' for enum " + enumType.getSimpleName()
                            + ". Allowed values: " + String.join(", ", lookup.keySet().stream().sorted().toList()));
        }
    }
}
