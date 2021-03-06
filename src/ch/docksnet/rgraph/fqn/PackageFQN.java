/*
 * Copyright (C) 2019 Stefan Zeller
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.docksnet.rgraph.fqn;

import com.intellij.psi.impl.file.PsiJavaDirectoryImpl;

/**
 * @author Stefan Zeller
 */
public class PackageFQN extends FQN implements Hierarchically {
    private final String fqn;

    private PackageFQN(String fqn) {
        this.fqn = fqn;
    }

    public static PackageFQN create(String string) {
        return new PackageFQN(string);
    }

    public static PackageFQN create(PsiJavaDirectoryImpl psiJavaDirectory) {
        String className = psiJavaDirectory.getPresentation().getLocationString();
        return new PackageFQN(className);
    }

    public static boolean isPackage(String string) {
        String[] split = string.split("\\.");
        if (Character.isLowerCase(split[split.length - 1].charAt(0))) {
            return true;
        }
        return false;
    }

    public String getFQN() {
        return this.fqn;
    }

    @Override
    public String getHierarchie() {
        return this.fqn;
    }
}