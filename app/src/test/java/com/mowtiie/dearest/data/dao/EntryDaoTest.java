package com.mowtiie.dearest.data.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class EntryDaoTest {

    @Test
    public void wordsBecomeQuotedPrefixTerms() {
        assertEquals("\"hello\"* \"world\"*", EntryDao.toFtsQuery("  hello   world "));
    }

    @Test
    public void ftsOperatorCharactersAreStripped() {
        assertEquals("\"foo\"* \"bar\"*", EntryDao.toFtsQuery("\"foo\" (bar)*"));
        assertEquals("\"nearmiss\"*", EntryDao.toFtsQuery("near:miss^"));
    }

    @Test
    public void inputWithNoSearchableTermsYieldsNull() {
        assertNull(EntryDao.toFtsQuery(null));
        assertNull(EntryDao.toFtsQuery("   "));
        assertNull(EntryDao.toFtsQuery("- * ()"));
    }
}
