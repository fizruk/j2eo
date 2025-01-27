package eotree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static util.ListUtils.listOf;

import arrow.core.Some;
import org.junit.jupiter.api.Test;

/**
 * EO metas tests.
 */
public class TestEOMetas {

    @Test
    public void testGenerateEOZeroIndent() {
        var f = new EOMetas(
                new Some<>("org.eolang.package"),
                listOf(
                        new EOMeta("key1", "value1"),
                        new EOMeta("key2", "value2")
                )
        );
        assertEquals("""
                        +package org.eolang.package
                        +key1 value1
                        +key2 value2""",
                f.generateEO(0));
    }

    @Test
    public void testGenerateEONonZeroIndent() {
        var f = new EOMetas(
                new Some<>("org.eolang.package"),
                listOf(
                        new EOMeta("key1", "value1"),
                        new EOMeta("key2", "value2")
                )
        );
        assertEquals("""
                        +package org.eolang.package
                        +key1 value1
                        +key2 value2""",
                f.generateEO(1));
    }
}
