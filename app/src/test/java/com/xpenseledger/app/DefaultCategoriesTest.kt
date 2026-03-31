package com.xpenseledger.app

import com.xpenseledger.app.data.local.entity.DefaultCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DefaultCategories] verifying the new "Family Support" entry
 * and overall category list integrity.
 */
class DefaultCategoriesTest {

    private val all = DefaultCategories.all

    // ── Family Support entry ──────────────────────────────────────────────────

    @Test
    fun `Family Support category exists in DefaultCategories`() {
        val familySupport = all.firstOrNull { it.name == "Family Support" }
        assertNotNull("Family Support must exist in DefaultCategories", familySupport)
    }

    @Test
    fun `Family Support has id 78`() {
        val familySupport = all.first { it.name == "Family Support" }
        assertEquals(78L, familySupport.id)
    }

    @Test
    fun `Family Support is a SUB category`() {
        val familySupport = all.first { it.name == "Family Support" }
        assertEquals("SUB", familySupport.type)
    }

    @Test
    fun `Family Support has parentId 7 (Finance)`() {
        val familySupport = all.first { it.name == "Family Support" }
        assertEquals(7L, familySupport.parentId)
    }

    // ── No duplicate IDs ──────────────────────────────────────────────────────

    @Test
    fun `all category IDs are unique`() {
        val ids    = all.map { it.id }
        val unique = ids.toSet()
        assertEquals("Duplicate category IDs found: ${ids.groupBy { it }.filter { it.value.size > 1 }.keys}",
            unique.size, ids.size)
    }

    // ── Finance parent has Family Support as sub ──────────────────────────────

    @Test
    fun `Finance category (id=7) exists as MAIN`() {
        val finance = all.firstOrNull { it.id == 7L }
        assertNotNull(finance)
        assertEquals("Finance", finance!!.name)
        assertEquals("MAIN", finance.type)
    }

    @Test
    fun `Finance sub-categories include Family Support`() {
        val financeSubs = all.filter { it.type == "SUB" && it.parentId == 7L }
        assertTrue(financeSubs.any { it.name == "Family Support" })
    }

    // ── Total count sanity check ──────────────────────────────────────────────

    @Test
    fun `DefaultCategories contains at least 40 entries`() {
        assertTrue("Expected >= 40 categories, got ${all.size}", all.size >= 40)
    }

    @Test
    fun `all SUB categories have a non-null parentId`() {
        val subsWithoutParent = all.filter { it.type == "SUB" && it.parentId == null }
        assertTrue("SUB categories without parentId: $subsWithoutParent",
            subsWithoutParent.isEmpty())
    }

    @Test
    fun `all MAIN categories have null parentId`() {
        val mainsWithParent = all.filter { it.type == "MAIN" && it.parentId != null }
        assertTrue("MAIN categories with parentId: $mainsWithParent",
            mainsWithParent.isEmpty())
    }
}

