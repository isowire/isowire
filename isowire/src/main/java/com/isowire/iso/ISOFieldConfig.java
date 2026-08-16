package com.isowire.iso;

import com.isowire.iso.packager.ISOFieldPackager;
import org.yaml.snakeyaml.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

public record ISOFieldConfig(
    int id,
    String name,
    String type,
    int length,
    String description,
    PaddingConfig padding,
    ValidationConfig validation,
    String format,
    Integer scale,
    List<ISOFieldConfig> subFields,
    boolean isSubField,
    boolean hasSubFields
) {
    private static final Logger logger = LoggerFactory.getLogger(ISOFieldConfig.class);

    // Runtime-only mutable associations stored externally to keep record compact
    private static final Map<ISOFieldConfig, Object> VALUE_MAP = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<ISOFieldConfig, ISOFieldPackager> PACKAGER_MAP = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Padding configuration for field formatting.
     */
    public static record PaddingConfig(String align, String padChar) {
        public PaddingConfig {
            if (align != null && !align.equals("left") && !align.equals("right")) {
                throw new IllegalArgumentException("Align must be 'left' or 'right'");
            }
        }
    }

    /**
     * Validation configuration for field validation rules.
     */
    public static record ValidationConfig(String pattern, Boolean luhn, Boolean required) {
        public ValidationConfig {
            if (pattern != null && pattern.isEmpty()) {
                throw new IllegalArgumentException("Pattern cannot be empty");
            }
        }
    }

    // Builder for creating immutable instances
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int id;
        private String name;
        private String type;
        private int length;
        private String description;
        private PaddingConfig padding;
        private ValidationConfig validation;
        private String format;
        private Integer scale;
        private List<ISOFieldConfig> subFields;
        private boolean isSubField = false;
        private boolean hasSubFields = false;

        public Builder id(int id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder length(int length) { this.length = length; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder padding(PaddingConfig padding) { this.padding = padding; return this; }
        public Builder validation(ValidationConfig validation) { this.validation = validation; return this; }
        public Builder format(String format) { this.format = format; return this; }
        public Builder scale(Integer scale) { this.scale = scale; return this; }
        public Builder subFields(List<ISOFieldConfig> subFields) { this.subFields = subFields; return this; }
        public Builder isSubField(boolean isSubField) { this.isSubField = isSubField; return this; }
        public Builder hasSubFields(boolean hasSubFields) { this.hasSubFields = hasSubFields; return this; }

        public ISOFieldConfig build() {
            List<ISOFieldConfig> sf = subFields == null ? Collections.<ISOFieldConfig>emptyList() : List.copyOf(subFields);
            return new ISOFieldConfig(id, name, type, length, description, padding, validation, format, scale, sf, isSubField, hasSubFields);
        }
    }

    // Backward-compatible getters/setters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public int getLength() { return length; }
    public String getDescription() { return description; }
    public PaddingConfig getPadding() { return padding; }
    public ValidationConfig getValidation() { return validation; }
    public String getFormat() { return format; }
    public Integer getScale() { return scale; }
    public List<ISOFieldConfig> getSubFields() { return subFields; }
    public boolean isSubField() { return isSubField; }
    public boolean hasSubFields() { return hasSubFields; }

    public Object getValue() { return VALUE_MAP.get(this); }
    public void setValue(Object value) { VALUE_MAP.put(this, value); }

    public ISOFieldPackager getPackager() { return PACKAGER_MAP.get(this); }
    public void setPackager(ISOFieldPackager packager) { PACKAGER_MAP.put(this, packager); }

    @Override
    public String toString() {
        return String.format(
            "FieldConfig{id=%d, name='%s', type='%s', length=%d, padding=%s, validation=%s, format='%s', scale=%s, description='%s'}",
            id, name, type, length, padding, validation, format, scale, description
        );
    }

    public static class Loader {
        public static Map<Integer, ISOFieldConfig> load() {
            var configs = new HashMap<Integer, ISOFieldConfig>();

            try {
                var yaml = new Yaml();
                var inputStream = ISOFieldConfig.class.getClassLoader().getResourceAsStream("iso8583-fields.yaml");

                if (inputStream == null) {
                    logger.warn("iso8583-fields.yaml not found, trying old format");
                    return loadOldFormat();
                }

                @SuppressWarnings("unchecked")
                var data = (Map<String, Object>) yaml.load(inputStream);
                var fieldsObj = data.get("fields");

                if (!(fieldsObj instanceof List<?> fieldsList)) {
                    logger.warn("No 'fields' list found in iso8583-fields.yaml");
                    return configs;
                }

                for (var fieldObj : fieldsList) {
                    if (!(fieldObj instanceof Map<?, ?> fieldMapRaw)) continue;
                    @SuppressWarnings("unchecked")
                    var fieldData = (Map<String, Object>) fieldMapRaw;

                    var builder = ISOFieldConfig.builder();

                    var idObj = fieldData.get("id");
                    if (idObj instanceof Number n) builder.id(n.intValue());

                    var nameObj = fieldData.get("name");
                    if (nameObj instanceof String s) builder.name(s);

                    var typeObj = fieldData.get("type");
                    if (typeObj instanceof String t) builder.type(t);

                    var lengthObj = fieldData.get("length");
                    if (lengthObj instanceof Number ln) builder.length(ln.intValue());

                    // Load padding configuration
                    var paddingObj = fieldData.get("padding");
                    if (paddingObj instanceof Map<?, ?> paddingRaw) {
                        @SuppressWarnings("unchecked")
                        var paddingData = (Map<String, Object>) paddingRaw;
                        var align = paddingData.get("align") instanceof String ? (String) paddingData.get("align") : null;
                        var padChar = paddingData.get("padChar") instanceof String ? (String) paddingData.get("padChar") : null;
                        builder.padding(new PaddingConfig(align, padChar));
                    }

                    // Load subfield definitions
                    var subFieldsObj = fieldData.get("subFields");
                    if (subFieldsObj instanceof List<?> subFieldListRaw) {
                        var subFieldConfigs = new ArrayList<ISOFieldConfig>();

                        for (var sfObj : subFieldListRaw) {
                            if (!(sfObj instanceof Map<?, ?> sfRaw)) continue;
                            @SuppressWarnings("unchecked")
                            var sfData = (Map<String, Object>) sfRaw;

                            var sfBuilder = ISOFieldConfig.builder();

                            var sfId = sfData.get("id");
                            if (sfId instanceof Number sn) sfBuilder.id(sn.intValue());

                            var sfName = sfData.get("name");
                            if (sfName instanceof String sns) sfBuilder.name(sns);

                            var sfType = sfData.get("type");
                            if (sfType instanceof String sft) sfBuilder.type(sft);

                            var sfLength = sfData.get("length");
                            if (sfLength instanceof Number sln) sfBuilder.length(sln.intValue());

                            var sfDesc = sfData.get("description");
                            if (sfDesc instanceof String sdesc) sfBuilder.description(sdesc);

                            var sfPadding = sfData.get("padding");
                            if (sfPadding instanceof Map<?, ?> spRaw) {
                                @SuppressWarnings("unchecked")
                                var spData = (Map<String, Object>) spRaw;
                                var align = spData.get("align") instanceof String ? (String) spData.get("align") : null;
                                var padChar = spData.get("padChar") instanceof String ? (String) spData.get("padChar") : null;
                                sfBuilder.padding(new PaddingConfig(align, padChar));
                            }

                            sfBuilder.isSubField(true);

                            var subFieldConfig = sfBuilder.build();
                            if (subFieldConfig.getType() != null) {
                                subFieldConfig.setPackager(instantiatePackager(subFieldConfig.getType()));
                            }

                            subFieldConfigs.add(subFieldConfig);
                            logger.trace("Loaded subfield: {}", subFieldConfig);
                        }

                        builder.subFields(subFieldConfigs);
                        builder.hasSubFields(true);
                    }

                    var formatObj = fieldData.get("format");
                    if (formatObj instanceof String fmt) builder.format(fmt);

                    var scaleObj = fieldData.get("scale");
                    if (scaleObj instanceof Number sc) builder.scale(sc.intValue());

                    var descObj = fieldData.get("description");
                    if (descObj instanceof String desc) builder.description(desc);

                    var config = builder.build();

                    if (config.getType() != null) {
                        config.setPackager(instantiatePackager(config.getType()));
                    }

                    configs.put(config.getId(), config);
                }

                logger.info("Loaded {} field configurations from YAML", configs.size());

            } catch (Exception e) {
                logger.error("Error loading field configuration from YAML", e);
                return new HashMap<>();
            }

            return configs;
        }

        private static Map<Integer, ISOFieldConfig> loadOldFormat() {
            // Fallback to old format if needed
            return new HashMap<>();
        }

        private static ISOFieldPackager instantiatePackager(String className) {
            try {
                Class<?> clazz = Class.forName(className);
                Object instance = clazz.getDeclaredConstructor().newInstance();

                // Use pattern matching for instanceof with type checking
                if (instance instanceof ISOFieldPackager packager) {
                    return packager;
                }

                throw new IllegalArgumentException("Class " + className + " does not implement ISOFieldPackager");
            } catch (Exception e) {
                logger.error("Failed to instantiate packager: {}", className, e);
                throw new RuntimeException("Invalid field type: " + className, e);
            }
        }
    }
}
