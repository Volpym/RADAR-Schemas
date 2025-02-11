package org.radarbase.schema.validation.rules;

import java.util.function.Function;
import org.apache.avro.Schema;

public interface SchemaMetadataRules {
    SchemaRules getSchemaRules();

    /** Checks the location of a schema with its internal data. */
    Validator<SchemaMetadata> validateSchemaLocation();

    /**
     * Validates any schema file. It will choose the correct validation method based on the scope
     * and type of the schema.
     */
    default Validator<SchemaMetadata> getValidator(final boolean validateScopeSpecific) {
        return metadata -> {
            SchemaRules schemaRules = getSchemaRules();
            Validator<SchemaMetadata> validator = validateSchemaLocation();

            if (metadata.getSchema().getType().equals(Schema.Type.ENUM)) {
                validator = validator.and(schema(schemaRules.validateEnum()));
            } else if (validateScopeSpecific) {
                switch (metadata.getScope()) {
                    case ACTIVE:
                        validator = validator.and(schema(schemaRules.validateActiveSource()));
                        break;
                    case MONITOR:
                        validator = validator.and(schema(schemaRules.validateMonitor()));
                        break;
                    case PASSIVE:
                        validator = validator.and(schema(schemaRules.validatePassive()));
                        break;
                    default:
                        validator = validator.and(schema(schemaRules.validateRecord()));
                        break;
                }
            } else {
                validator = validator.and(schema(schemaRules.validateRecord()));
            }
            return validator.apply(metadata);
        };
    }

    /** Validates schemas without their metadata. */
    default Validator<SchemaMetadata> schema(Validator<Schema> validator) {
        return metadata -> validator.apply(metadata.getSchema());
    }

    default Function<SchemaMetadata, String> message(String text) {
        return metadata -> "Schema " + metadata.getSchema().getFullName()
                + " at " + metadata.getPath() + " is invalid. " + text;
    }
}
