/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.sdkuilib.internal.repository;

import com.android.sdklib.internal.repository.MockAddonPackage;
import com.android.sdklib.internal.repository.MockExtraPackage;
import com.android.sdklib.internal.repository.MockPlatformPackage;
import com.android.sdklib.internal.repository.MockPlatformToolPackage;
import com.android.sdklib.internal.repository.MockToolPackage;
import com.android.sdklib.internal.repository.Package;
import com.android.sdklib.internal.repository.SdkRepoSource;
import com.android.sdklib.internal.repository.SdkSource;
import com.android.sdkuilib.internal.repository.PackageLoader.PkgItem;
import com.android.sdkuilib.internal.repository.PackagesPage.PackagesDiffLogic;
import com.android.sdkuilib.internal.repository.PackagesPage.PkgCategory;
import com.android.sdkuilib.internal.repository.PackagesPage.PackagesDiffLogic.UpdateOp;

import junit.framework.TestCase;

public class PackagesDiffLogicTest extends TestCase {

    private PackagesDiffLogic m;
    private MockUpdaterData u;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        u = new MockUpdaterData();
        m = new PackagesDiffLogic(u);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    // ----
    //
    // Test Details Note: the way load is implemented in PackageLoader, the
    // loader processes each source and then for each source the packages are added
    // to a list and the sorting algorithm is called with that list. Thus for
    // one load, many calls to the sortByX/Y happen, with the list progressively
    // being populated.
    // However when the user switches sorting algorithm, the package list is not
    // reloaded and is processed at once.

    public void testSortByApi_Empty() {
        UpdateOp op = m.updateStart(true /*sortByApi*/);
        assertFalse(m.updateSourcePackages(op, null /*locals*/, new Package[0]));
        assertFalse(m.updateEnd(op));

        assertSame(m.mCurrentCategories, m.mApiCategories);

        // We also keep these 2 categories even if they contain nothing
        assertEquals(
                "PkgApiCategory <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgApiCategory <API=EXTRAS, label=Extras, #items=0>\n",
               getTree(m));
    }

    public void testSortByApi_AddSamePackage() {
        SdkSource src1 = new SdkRepoSource("http://repo.com/url", "repo1");

        UpdateOp op = m.updateStart(true /*sortByApi*/);
        // First insert local packages
        assertTrue(m.updateSourcePackages(op, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "some pkg", 1)
        }));

        assertEquals(
                "PkgApiCategory <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgApiCategory <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'some pkg' rev=1>\n",
               getTree(m));

        // Insert the next source
        // Same package as the one installed, so we don't display it
        assertFalse(m.updateSourcePackages(op, src1, new Package[] {
                new MockEmptyPackage(src1, "some pkg", 1)
        }));

        assertFalse(m.updateEnd(op));

        assertEquals(
                "PkgApiCategory <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgApiCategory <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'some pkg' rev=1>\n",
               getTree(m));
    }

    public void testSortByApi_AddOtherPackage() {
        SdkSource src1 = new SdkRepoSource("http://repo.com/url", "repo1");

        UpdateOp op = m.updateStart(true /*sortByApi*/);
        // First insert local packages
        assertTrue(m.updateSourcePackages(op, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "some pkg", 1)
        }));

        assertEquals(
                "PkgApiCategory <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgApiCategory <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'some pkg' rev=1>\n",
               getTree(m));

        // Insert the next source
        // Not the same package as the one installed, so we'll display it
        assertTrue(m.updateSourcePackages(op, src1, new Package[] {
                new MockEmptyPackage(src1, "other pkg", 1)
        }));

        assertFalse(m.updateEnd(op));

        assertEquals(
                "PkgApiCategory <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgApiCategory <API=EXTRAS, label=Extras, #items=2>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'some pkg' rev=1>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'other pkg' rev=1>\n",
               getTree(m));
    }

    public void testSortByApi_Update1() {
        SdkSource src1 = new SdkRepoSource("http://repo.com/url", "repo1");

        // Typical case: user has a locally installed package in revision 1
        // The display list after sort should show that installed package.
        UpdateOp op = m.updateStart(true /*sortByApi*/);
        // First insert local packages
        assertTrue(m.updateSourcePackages(op, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1)
        }));

        assertEquals(
                "PkgApiCategory <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgApiCategory <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1>\n",
               getTree(m));

        assertTrue(m.updateSourcePackages(op, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 4),
                new MockEmptyPackage(src1, "type1", 2)
        }));

        assertFalse(m.updateEnd(op));

        assertEquals(
                "PkgApiCategory <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgApiCategory <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=4>\n",
               getTree(m));
    }

    public void testSortByApi_Reload() {
        SdkSource src1 = new SdkRepoSource("http://repo.com/url", "repo1");

        // First load reveals a package local package and its update
        UpdateOp op1 = m.updateStart(true /*sortByApi*/);
        // First insert local packages
        assertTrue(m.updateSourcePackages(op1, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1)
        }));
        assertTrue(m.updateSourcePackages(op1, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 2)
        }));

        assertFalse(m.updateEnd(op1));

        assertEquals(
                "PkgApiCategory <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgApiCategory <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n",
               getTree(m));

        // Now simulate a reload that clears the package list and create similar
        // objects but not the same references. The only difference is that updateXyz
        // returns false since they don't change anything.

        UpdateOp op2 = m.updateStart(true /*sortByApi*/);
        // First insert local packages
        assertFalse(m.updateSourcePackages(op2, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1)
        }));
        assertFalse(m.updateSourcePackages(op2, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 2)
        }));

        assertFalse(m.updateEnd(op2));

        assertEquals(
                "PkgApiCategory <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgApiCategory <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n",
               getTree(m));
    }

    public void testSortByApi_InstallPackage() {
        SdkSource src1 = new SdkRepoSource("http://repo.com/url", "repo1");

        // First load reveals a new package
        UpdateOp op1 = m.updateStart(true /*sortByApi*/);
        // No local packages at first
        assertFalse(m.updateSourcePackages(op1, null /*locals*/, new Package[0]));
        assertTrue(m.updateSourcePackages(op1, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 1)
        }));

        assertFalse(m.updateEnd(op1));

        assertEquals(
                "PkgApiCategory <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgApiCategory <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'type1' rev=1>\n",
               getTree(m));

        // Install it.
        UpdateOp op2 = m.updateStart(true /*sortByApi*/);
        // local packages
        assertTrue(m.updateSourcePackages(op2, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1)
        }));
        assertFalse(m.updateSourcePackages(op2, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 1)
        }));

        assertFalse(m.updateEnd(op2));

        assertEquals(
                "PkgApiCategory <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgApiCategory <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1>\n",
               getTree(m));

        // Load reveals an update
        UpdateOp op3 = m.updateStart(true /*sortByApi*/);
        // local packages
        assertFalse(m.updateSourcePackages(op3, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1)
        }));
        assertTrue(m.updateSourcePackages(op3, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 2)
        }));

        assertFalse(m.updateEnd(op3));

        assertEquals(
                "PkgApiCategory <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgApiCategory <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n",
               getTree(m));
    }

    public void testSortByApi_DeletePackage() {
        SdkSource src1 = new SdkRepoSource("http://repo.com/url", "repo1");

        // We have an installed package
        UpdateOp op2 = m.updateStart(true /*sortByApi*/);
        // local packages
        assertTrue(m.updateSourcePackages(op2, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1)
        }));
        assertTrue(m.updateSourcePackages(op2, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 2)
        }));

        assertFalse(m.updateEnd(op2));

        assertEquals(
                "PkgApiCategory <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgApiCategory <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n",
               getTree(m));

        // User now deletes the installed package.
        UpdateOp op1 = m.updateStart(true /*sortByApi*/);
        // No local packages
        assertTrue(m.updateSourcePackages(op1, null /*locals*/, new Package[0]));
        assertTrue(m.updateSourcePackages(op1, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 1)
        }));

        assertFalse(m.updateEnd(op1));

        assertEquals(
                "PkgApiCategory <API=TOOLS, label=Tools, #items=0>\n" +
                "PkgApiCategory <API=EXTRAS, label=Extras, #items=1>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'type1' rev=1>\n",
               getTree(m));
    }

    public void testSortByApi_CompleteUpdate() {
        SdkSource src1 = new SdkRepoSource("http://repo.com/url1", "repo1");
        SdkSource src2 = new SdkRepoSource("http://repo.com/url2", "repo2");

        // Resulting categories are sorted by Tools, descending platform API and finally Extras.
        // Addons are sorted by name within their API.
        // Extras are sorted by vendor name.
        // The order packages are added to the mAllPkgItems list is purposedly different from
        // the final order we get.

        // First update has the typical tools and a couple extras
        UpdateOp op1 = m.updateStart(true /*sortByApi*/);

        assertTrue(m.updateSourcePackages(op1, null /*locals*/, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "android", "usb_driver", 4, 3),
        }));
        assertTrue(m.updateSourcePackages(op1, src1, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "carrier", "custom_rom", 1, 0),
                new MockExtraPackage(src1, "android", "usb_driver", 5, 3),
        }));
        assertFalse(m.updateEnd(op1));

        assertEquals(
                "PkgApiCategory <API=TOOLS, label=Tools, #items=2>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 10>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3>\n" +
                "PkgApiCategory <API=EXTRAS, label=Extras, #items=2>\n" +
                "-- <INSTALLED, pkg:Android USB Driver package, revision 4, updated by:Android USB Driver package, revision 5>\n" +
                "-- <NEW, pkg:Carrier Custom Rom package, revision 1>\n",
               getTree(m));

        // Next update adds platforms and addon, sorted in a category based on their API level
        UpdateOp op2 = m.updateStart(true /*sortByApi*/);
        MockPlatformPackage p1;
        MockPlatformPackage p2;

        assertTrue(m.updateSourcePackages(op2, null /*locals*/, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "android", "usb_driver", 4, 3),
                // second update
                p1 = new MockPlatformPackage(src1, 1, 2, 3),  // API 1
                new MockPlatformPackage(src1, 3, 6, 3),
                new MockAddonPackage(src2, "addon A", p1, 5),
        }));
        assertTrue(m.updateSourcePackages(op2, src1, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "carrier", "custom_rom", 1, 0),
                new MockExtraPackage(src1, "android", "usb_driver", 5, 3),
                // second update
                p2 = new MockPlatformPackage(src1, 2, 4, 3),    // API 2
        }));
        assertTrue(m.updateSourcePackages(op2, src2, new Package[] {
                new MockAddonPackage(src2, "addon C", p2, 9),
                new MockAddonPackage(src2, "addon A", p1, 6),
                new MockAddonPackage(src2, "addon B", p2, 7),
                // the rev 8 update will be ignored since there's a rev 9 coming after
                new MockAddonPackage(src2, "addon B", p2, 8),
                new MockAddonPackage(src2, "addon B", p2, 9),
        }));
        assertFalse(m.updateEnd(op2));

        assertEquals(
                "PkgApiCategory <API=TOOLS, label=Tools, #items=2>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 10>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3>\n" +
                "PkgApiCategory <API=API 3, label=Android android-3 (API 3), #items=1>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-3, API 3, revision 6>\n" +
                "PkgApiCategory <API=API 2, label=Android android-2 (API 2), #items=3>\n" +
                "-- <NEW, pkg:SDK Platform Android android-2, API 2, revision 4>\n" +
                "-- <NEW, pkg:addon B by vendor 2, Android API 2, revision 7, updated by:addon B by vendor 2, Android API 2, revision 9>\n" +
                "-- <NEW, pkg:addon C by vendor 2, Android API 2, revision 9>\n" +
                "PkgApiCategory <API=API 1, label=Android android-1 (API 1), #items=2>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "-- <INSTALLED, pkg:addon A by vendor 1, Android API 1, revision 5, updated by:addon A by vendor 1, Android API 1, revision 6>\n" +
                "PkgApiCategory <API=EXTRAS, label=Extras, #items=2>\n" +
                "-- <INSTALLED, pkg:Android USB Driver package, revision 4, updated by:Android USB Driver package, revision 5>\n" +
                "-- <NEW, pkg:Carrier Custom Rom package, revision 1>\n",
               getTree(m));

        // Reloading the same thing should have no impact except for the update methods
        // returning false when they don't change the current list.
        UpdateOp op3 = m.updateStart(true /*sortByApi*/);

        assertFalse(m.updateSourcePackages(op3, null /*locals*/, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "android", "usb_driver", 4, 3),
                // second update
                p1 = new MockPlatformPackage(src1, 1, 2, 3),
                new MockPlatformPackage(src1, 3, 6, 3),
                new MockAddonPackage(src2, "addon A", p1, 5),
        }));
        assertFalse(m.updateSourcePackages(op3, src1, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "carrier", "custom_rom", 1, 0),
                new MockExtraPackage(src1, "android", "usb_driver", 5, 3),
                // second update
                p2 = new MockPlatformPackage(src1, 2, 4, 3),
        }));
        assertTrue(m.updateSourcePackages(op3, src2, new Package[] {
                new MockAddonPackage(src2, "addon C", p2, 9),
                new MockAddonPackage(src2, "addon A", p1, 6),
                new MockAddonPackage(src2, "addon B", p2, 7),
                // the rev 8 update will be ignored since there's a rev 9 coming after
                // however as a side effect it makes the update method return true as it
                // incorporated the update.
                new MockAddonPackage(src2, "addon B", p2, 8),
                new MockAddonPackage(src2, "addon B", p2, 9),
        }));
        assertFalse(m.updateEnd(op3));

        assertEquals(
                "PkgApiCategory <API=TOOLS, label=Tools, #items=2>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 10>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3>\n" +
                "PkgApiCategory <API=API 3, label=Android android-3 (API 3), #items=1>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-3, API 3, revision 6>\n" +
                "PkgApiCategory <API=API 2, label=Android android-2 (API 2), #items=3>\n" +
                "-- <NEW, pkg:SDK Platform Android android-2, API 2, revision 4>\n" +
                "-- <NEW, pkg:addon B by vendor 2, Android API 2, revision 7, updated by:addon B by vendor 2, Android API 2, revision 9>\n" +
                "-- <NEW, pkg:addon C by vendor 2, Android API 2, revision 9>\n" +
                "PkgApiCategory <API=API 1, label=Android android-1 (API 1), #items=2>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "-- <INSTALLED, pkg:addon A by vendor 1, Android API 1, revision 5, updated by:addon A by vendor 1, Android API 1, revision 6>\n" +
                "PkgApiCategory <API=EXTRAS, label=Extras, #items=2>\n" +
                "-- <INSTALLED, pkg:Android USB Driver package, revision 4, updated by:Android USB Driver package, revision 5>\n" +
                "-- <NEW, pkg:Carrier Custom Rom package, revision 1>\n",
               getTree(m));
    }

    // ----

    public void testSortBySource_Empty() {
        UpdateOp op = m.updateStart(false /*sortByApi*/);
        assertFalse(m.updateSourcePackages(op, null /*locals*/, new Package[0]));
        // UpdateEnd returns true since it removed the synthetic "unknown source" category
        assertTrue(m.updateEnd(op));

        assertSame(m.mCurrentCategories, m.mSourceCategories);
        assertTrue(m.mApiCategories.isEmpty());

        assertEquals(
                "",
               getTree(m));
    }

    public void testSortBySource_AddPackages() {
        // Since we're sorting by source, items are grouped under their source
        // even if installed. The 'local' source is only for installed items for
        // which we don't know the source.
        SdkSource src1 = new SdkRepoSource("http://repo.com/url", "repo1");

        UpdateOp op = m.updateStart(false /*sortByApi*/);
        assertTrue(m.updateSourcePackages(op, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "known source", 2),
                new MockEmptyPackage(null, "unknown source", 3),
        }));

        assertEquals(
                "PkgSourceCategory <source=Local Packages (no.source), #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'unknown source' rev=3>\n" +
                "PkgSourceCategory <source=repo1 (repo.com), #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'known source' rev=2>\n",
               getTree(m));

        assertTrue(m.updateSourcePackages(op, src1, new Package[] {
                new MockEmptyPackage(src1, "new", 1),
        }));

        assertFalse(m.updateEnd(op));

        assertEquals(
                "PkgSourceCategory <source=Local Packages (no.source), #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'unknown source' rev=3>\n" +
                "PkgSourceCategory <source=repo1 (repo.com), #items=2>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'new' rev=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'known source' rev=2>\n",
               getTree(m));
    }

    public void testSortBySource_Update1() {

        // Typical case: user has a locally installed package in revision 1
        // The display list after sort should show that instaled package.
        SdkSource src1 = new SdkRepoSource("http://repo.com/url", "repo1");
        UpdateOp op = m.updateStart(false /*sortByApi*/);
        assertTrue(m.updateSourcePackages(op, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1),
        }));

        assertEquals(
                "PkgSourceCategory <source=Local Packages (no.source), #items=0>\n" +
                "PkgSourceCategory <source=repo1 (repo.com), #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1>\n",
               getTree(m));

        // Edge case: the source reveals an update in revision 2. It is ignored since
        // we already have a package in rev 4.

        assertTrue(m.updateSourcePackages(op, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 4),
                new MockEmptyPackage(src1, "type1", 2),
        }));

        assertTrue(m.updateEnd(op));

        assertEquals(
                "PkgSourceCategory <source=repo1 (repo.com), #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=4>\n",
               getTree(m));
    }

    public void testSortBySource_Reload() {

        // First load reveals a package local package and its update
        SdkSource src1 = new SdkRepoSource("http://repo.com/url", "repo1");
        UpdateOp op1 = m.updateStart(false /*sortByApi*/);
        assertTrue(m.updateSourcePackages(op1, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1),
        }));
        assertTrue(m.updateSourcePackages(op1, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 2),
        }));
        assertTrue(m.updateEnd(op1));

        assertEquals(
                "PkgSourceCategory <source=repo1 (repo.com), #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n",
               getTree(m));

        // Now simulate a reload that clears the package list and creates similar
        // objects but not the same references. Update methods return false since
        // they don't change anything.
        UpdateOp op2 = m.updateStart(false /*sortByApi*/);
        assertFalse(m.updateSourcePackages(op2, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1),
        }));
        assertFalse(m.updateSourcePackages(op2, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 2),
        }));
        assertTrue(m.updateEnd(op2));

        assertEquals(
                "PkgSourceCategory <source=repo1 (repo.com), #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n",
               getTree(m));
    }

    public void testSortBySource_InstallPackage() {

        // First load reveals a new package
        SdkSource src1 = new SdkRepoSource("http://repo.com/url", "repo1");
        UpdateOp op1 = m.updateStart(false /*sortByApi*/);
        // no local package
        assertFalse(m.updateSourcePackages(op1, null /*locals*/, new Package[0]));
        assertTrue(m.updateSourcePackages(op1, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 1),
        }));
        assertTrue(m.updateEnd(op1));

        assertEquals(
                "PkgSourceCategory <source=repo1 (repo.com), #items=1>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'type1' rev=1>\n",
               getTree(m));


        // Install it. The display only shows the installed one, 'hiding' the remote package
        UpdateOp op2 = m.updateStart(false /*sortByApi*/);
        assertTrue(m.updateSourcePackages(op2, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1),
        }));
        assertFalse(m.updateSourcePackages(op2, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 1),
        }));
        assertTrue(m.updateEnd(op2));

        assertEquals(
                "PkgSourceCategory <source=repo1 (repo.com), #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1>\n",
               getTree(m));

        // Now we have an update
        UpdateOp op3 = m.updateStart(false /*sortByApi*/);
        assertFalse(m.updateSourcePackages(op3, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1),
        }));
        assertTrue(m.updateSourcePackages(op3, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 2),
        }));
        assertTrue(m.updateEnd(op3));

        assertEquals(
                "PkgSourceCategory <source=repo1 (repo.com), #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1, updated by:MockEmptyPackage 'type1' rev=2>\n",
               getTree(m));
    }

    public void testSortBySource_DeletePackage() {
        SdkSource src1 = new SdkRepoSource("http://repo.com/url", "repo1");

        // Start with an installed package and its matching remote package
        UpdateOp op2 = m.updateStart(false /*sortByApi*/);
        assertTrue(m.updateSourcePackages(op2, null /*locals*/, new Package[] {
                new MockEmptyPackage(src1, "type1", 1),
        }));
        assertFalse(m.updateSourcePackages(op2, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 1),
        }));
        assertTrue(m.updateEnd(op2));

        assertEquals(
                "PkgSourceCategory <source=repo1 (repo.com), #items=1>\n" +
                "-- <INSTALLED, pkg:MockEmptyPackage 'type1' rev=1>\n",
               getTree(m));

        // User now deletes the installed package.
        UpdateOp op1 = m.updateStart(false /*sortByApi*/);
        // no local package
        assertTrue(m.updateSourcePackages(op1, null /*locals*/, new Package[0]));
        assertFalse(m.updateSourcePackages(op1, src1, new Package[] {
                new MockEmptyPackage(src1, "type1", 1),
        }));
        assertTrue(m.updateEnd(op1));

        assertEquals(
                "PkgSourceCategory <source=repo1 (repo.com), #items=1>\n" +
                "-- <NEW, pkg:MockEmptyPackage 'type1' rev=1>\n",
               getTree(m));
    }

    public void testSortBySource_CompleteUpdate() {
        SdkSource src1 = new SdkRepoSource("http://repo.com/url1", "repo1");
        SdkSource src2 = new SdkRepoSource("http://repo.com/url2", "repo2");

        // First update has the typical tools and a couple extras
        UpdateOp op1 = m.updateStart(false /*sortByApi*/);

        assertTrue(m.updateSourcePackages(op1, null /*locals*/, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "android", "usb_driver", 4, 3),
        }));
        assertTrue(m.updateSourcePackages(op1, src1, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "carrier", "custom_rom", 1, 0),
                new MockExtraPackage(src1, "android", "usb_driver", 5, 3),
        }));
        assertTrue(m.updateEnd(op1));

        assertEquals(
                "PkgSourceCategory <source=repo1 (repo.com), #items=4>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 10>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3>\n" +
                "-- <INSTALLED, pkg:Android USB Driver package, revision 4, updated by:Android USB Driver package, revision 5>\n" +
                "-- <NEW, pkg:Carrier Custom Rom package, revision 1>\n",
               getTree(m));

        // Next update adds platforms and addon, sorted in a category based on their API level
        UpdateOp op2 = m.updateStart(false /*sortByApi*/);
        MockPlatformPackage p1;
        MockPlatformPackage p2;

        assertTrue(m.updateSourcePackages(op2, null /*locals*/, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "android", "usb_driver", 4, 3),
                // second update
                p1 = new MockPlatformPackage(src1, 1, 2, 3),  // API 1
                new MockPlatformPackage(src1, 3, 6, 3),       // API 3
                new MockAddonPackage(src2, "addon A", p1, 5),
        }));
        assertTrue(m.updateSourcePackages(op2, src1, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "carrier", "custom_rom", 1, 0),
                new MockExtraPackage(src1, "android", "usb_driver", 5, 3),
                // second update
                p2 = new MockPlatformPackage(src1, 2, 4, 3),    // API 2
        }));
        assertTrue(m.updateSourcePackages(op2, src2, new Package[] {
                new MockAddonPackage(src2, "addon C", p2, 9),
                new MockAddonPackage(src2, "addon A", p1, 6),
                new MockAddonPackage(src2, "addon B", p2, 7),
                // the rev 8 update will be ignored since there's a rev 9 coming after
                new MockAddonPackage(src2, "addon B", p2, 8),
                new MockAddonPackage(src2, "addon B", p2, 9),
        }));
        assertTrue(m.updateEnd(op2));

        assertEquals(
                "PkgSourceCategory <source=repo1 (repo.com), #items=7>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 10>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-3, API 3, revision 6>\n" +
                "-- <NEW, pkg:SDK Platform Android android-2, API 2, revision 4>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "-- <INSTALLED, pkg:Android USB Driver package, revision 4, updated by:Android USB Driver package, revision 5>\n" +
                "-- <NEW, pkg:Carrier Custom Rom package, revision 1>\n" +
                "PkgSourceCategory <source=repo2 (repo.com), #items=3>\n" +
                "-- <NEW, pkg:addon B by vendor 2, Android API 2, revision 7, updated by:addon B by vendor 2, Android API 2, revision 9>\n" +
                "-- <NEW, pkg:addon C by vendor 2, Android API 2, revision 9>\n" +
                "-- <INSTALLED, pkg:addon A by vendor 1, Android API 1, revision 5, updated by:addon A by vendor 1, Android API 1, revision 6>\n",
               getTree(m));

        // Reloading the same thing should have no impact except for the update methods
        // returning false when they don't change the current list.
        UpdateOp op3 = m.updateStart(false /*sortByApi*/);

        assertFalse(m.updateSourcePackages(op3, null /*locals*/, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "android", "usb_driver", 4, 3),
                // second update
                p1 = new MockPlatformPackage(src1, 1, 2, 3),
                new MockPlatformPackage(src1, 3, 6, 3),
                new MockAddonPackage(src2, "addon A", p1, 5),
        }));
        assertFalse(m.updateSourcePackages(op3, src1, new Package[] {
                new MockToolPackage(src1, 10, 3),
                new MockPlatformToolPackage(src1, 3),
                new MockExtraPackage(src1, "carrier", "custom_rom", 1, 0),
                new MockExtraPackage(src1, "android", "usb_driver", 5, 3),
                // second update
                p2 = new MockPlatformPackage(src1, 2, 4, 3),
        }));
        assertTrue(m.updateSourcePackages(op3, src2, new Package[] {
                new MockAddonPackage(src2, "addon C", p2, 9),
                new MockAddonPackage(src2, "addon A", p1, 6),
                new MockAddonPackage(src2, "addon B", p2, 7),
                // the rev 8 update will be ignored since there's a rev 9 coming after
                // however as a side effect it makes the update method return true as it
                // incorporated the update.
                new MockAddonPackage(src2, "addon B", p2, 8),
                new MockAddonPackage(src2, "addon B", p2, 9),
        }));
        assertTrue(m.updateEnd(op3));

        assertEquals(
                "PkgSourceCategory <source=repo1 (repo.com), #items=7>\n" +
                "-- <INSTALLED, pkg:Android SDK Tools, revision 10>\n" +
                "-- <INSTALLED, pkg:Android SDK Platform-tools, revision 3>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-3, API 3, revision 6>\n" +
                "-- <NEW, pkg:SDK Platform Android android-2, API 2, revision 4>\n" +
                "-- <INSTALLED, pkg:SDK Platform Android android-1, API 1, revision 2>\n" +
                "-- <INSTALLED, pkg:Android USB Driver package, revision 4, updated by:Android USB Driver package, revision 5>\n" +
                "-- <NEW, pkg:Carrier Custom Rom package, revision 1>\n" +
                "PkgSourceCategory <source=repo2 (repo.com), #items=3>\n" +
                "-- <NEW, pkg:addon B by vendor 2, Android API 2, revision 7, updated by:addon B by vendor 2, Android API 2, revision 9>\n" +
                "-- <NEW, pkg:addon C by vendor 2, Android API 2, revision 9>\n" +
                "-- <INSTALLED, pkg:addon A by vendor 1, Android API 1, revision 5, updated by:addon A by vendor 1, Android API 1, revision 6>\n",
               getTree(m));
    }

    // ----

    /**
     * Simulates the display we would have in the Packages Tree.
     * This always depends on mCurrentCategories like the tree does.
     * The display format is something like:
     * <pre>
     *   PkgCategory &lt;description&gt;
     *   -- &lt;PkgItem description&gt;
     * </pre>
     */
    public String getTree(PackagesDiffLogic l) {
        StringBuilder sb = new StringBuilder();

        for (PkgCategory cat : l.mCurrentCategories) {
            sb.append(cat.toString()).append('\n');
            for (PkgItem item : cat.getItems()) {
                sb.append("-- ").append(item.toString()).append('\n');
            }
        }

        return sb.toString();
    }
}
