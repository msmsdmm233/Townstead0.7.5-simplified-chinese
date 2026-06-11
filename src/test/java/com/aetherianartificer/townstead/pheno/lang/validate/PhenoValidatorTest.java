package com.aetherianartificer.townstead.pheno.lang.validate;

import com.aetherianartificer.townstead.pheno.lang.compile.Diagnostic;
import com.aetherianartificer.townstead.pheno.lang.compile.Diagnostics;
import com.aetherianartificer.townstead.pheno.lang.schema.PhenoSchemas;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhenoValidatorTest {

    @BeforeAll
    static void registerSchemas() {
        PhenoSchemas.registerAll();
    }

    private static JsonObject obj(String singleQuoted) {
        return JsonParser.parseString(singleQuoted.replace('\'', '"')).getAsJsonObject();
    }

    private static Diagnostics validate(String json) {
        Diagnostics diag = new Diagnostics();
        PhenoValidator.validateGene(ResourceLocation.tryParse("townstead_test:g"), obj(json), diag);
        return diag;
    }

    private static boolean has(Diagnostics diag, String jsonPath, String messageFragment) {
        for (Diagnostic d : diag.all()) {
            if (d.jsonPath().equals(jsonPath) && d.message().contains(messageFragment)) return true;
        }
        return false;
    }

    @Test
    void missingRequiredChildIsFlaggedWithExactPath() {
        Diagnostics diag = validate("{ 'type':'townstead_origins:active_ability', 'cooldown':40 }");
        assertTrue(has(diag, "$.action", "Missing required field"),
                "active_ability without 'action' must be flagged at $.action");
    }

    @Test
    void scalarTypeMismatchIsFlaggedWithExactPath() {
        Diagnostics diag = validate("{ 'type':'townstead_origins:attribute', 'attribute':5 }");
        assertTrue(has(diag, "$.attribute", "resource id"),
                "a numeric attribute id must be flagged at $.attribute");
    }

    @Test
    void variantMissingRequiredFieldUsesVariantPath() {
        Diagnostics diag = validate(
                "{ 'type':'townstead_origins:active_ability', 'variants':{ 'q':{ 'cooldown':40 } } }");
        assertTrue(has(diag, "$.variants.q.action", "Missing required field"),
                "a required field missing inside a variant must be located under that variant");
    }

    @Test
    void wellFormedGeneHasNoFieldDiagnostics() {
        Diagnostics diag = validate(
                "{ 'type':'townstead_origins:active_ability',"
                        + " 'action':{'type':'townstead_origins:heal','amount':1}, 'cooldown':40 }");
        for (Diagnostic d : diag.all()) {
            assertFalse(d.message().contains("Missing required field") || d.message().contains("Expected"),
                    "well-formed gene should produce no field diagnostics, got: " + d.render());
        }
    }
}
