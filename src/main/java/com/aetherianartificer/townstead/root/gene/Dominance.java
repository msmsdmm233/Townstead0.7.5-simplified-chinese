package com.aetherianartificer.townstead.root.gene;

import java.util.Locale;

/**
 * Whether a gene is expressed over the rival alleles at its locus at inheritance.
 * Resolution (dominant beats recessive, ties by weight then random) is a
 * deferred runtime phase; this iteration only carries/displays it.
 */
public enum Dominance {
    DOMINANT,
    RECESSIVE;

    public static Dominance fromString(String s) {
        if (s != null && "recessive".equals(s.toLowerCase(Locale.ROOT))) return RECESSIVE;
        return DOMINANT;
    }
}
