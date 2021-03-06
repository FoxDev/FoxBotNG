/*
 * This file is part of FoxBotNG.
 *
 * FoxBotNG is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FoxBotNG is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FoxBotNG.  If not, see <http://www.gnu.org/licenses/>.
 */

package co.foxdev.foxbotng;

import co.foxdev.foxbotng.utils.UrlExtractor;
import org.junit.Test;

import static org.junit.Assert.*;

public class UrlExtractorTest {
    @Test
    public void shouldNotContainUrl() {
        assertFalse(UrlExtractor.hasUrl("google.com"));
        assertFalse(UrlExtractor.hasUrl("https:// google.com"));
        assertFalse(UrlExtractor.hasUrl("https://"));
        assertFalse(UrlExtractor.hasUrl("aaaa https:// google.com 1234"));
    }

    @Test
    public void shouldNotExtractUrl() {
        assertNull(UrlExtractor.getFrom("google.com"));
        assertNull(UrlExtractor.getFrom("https:// google.com"));
        assertNull(UrlExtractor.getFrom("https://"));
        assertNull(UrlExtractor.getFrom("aaaa https:// google.com 1234"));
    }

    @Test
    public void shouldContainUrl() {
        assertTrue(UrlExtractor.hasUrl("https://google.com"));
        assertTrue(UrlExtractor.hasUrl("aaaa https://google.com 1234"));
    }

    @Test
    public void shouldExtractUrl() {
        assertEquals("https://google.com", UrlExtractor.getFrom("https://google.com"));
        assertEquals("https://google.com", UrlExtractor.getFrom("aaaa https://google.com 1234"));
    }

    @Test
    public void shouldParseTitle() {
        assertEquals("Google", UrlExtractor.parseUrlForTitle("https://google.com"));
    }
}
