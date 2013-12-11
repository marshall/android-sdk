/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.tools.lint.checks;

import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings("javadoc")
public class HardcodedDebugModeDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new HardcodedDebugModeDetector();
    }

    public void test() throws Exception {
        assertEquals(
                "AndroidManifest.xml:10: Warning: Avoid hardcoding the debug mode; leaving " +
                "it out allows debug and release builds to automatically assign one",
                lintProject("debuggable.xml=>AndroidManifest.xml"));
    }

    public void testOk() throws Exception {
        assertEquals(
                "No warnings.",
                lintProject("AndroidManifest.xml"));
    }
}