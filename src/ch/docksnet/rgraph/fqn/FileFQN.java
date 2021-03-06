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

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.Objects;

public class FileFQN extends FQN implements Hierarchically {
    private final String packageName;
    private final String fileName;
    private final PsiJavaFile psiJavaFile;

    private FileFQN(String packageName, String fileName, PsiJavaFile psiJavaFile) {
        this.psiJavaFile = psiJavaFile;
        if (packageName == null) {
            throw new IllegalArgumentException("packageName: null not allowed");
        }
        if (fileName == null) {
            throw new IllegalArgumentException("fileName: null not allowed");
        }
        this.packageName = packageName;
        this.fileName = fileName;
    }

    public static FileFQN from(PsiJavaFile psiJavaFile) {
        return new FileFQN(psiJavaFile.getPackageName(), psiJavaFile.getName(), psiJavaFile);
    }

    public static FileFQN resolveHierarchically(PsiElement psiElement) {
        PsiJavaFile psiJavaFile = PsiTreeUtil.getParentOfType(psiElement, PsiJavaFile.class, true);
        if (psiJavaFile == null) {
            return null;
        }
        return from((PsiJavaFile) psiJavaFile);
    }

    public static FileFQN create(PsiJavaFile psiJavaFile) {
        return from(psiJavaFile);
    }

    @Override
    public String getHierarchie() {
        return this.packageName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileFQN fileFQN = (FileFQN) o;
        return this.packageName.equals(fileFQN.packageName) &&
                this.fileName.equals(fileFQN.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.packageName, this.fileName);
    }

    @Override
    public String toString() {
        return this.packageName + "." + this.fileName;
    }

    public PsiJavaFile getPsiJavaFile() {
        return this.psiJavaFile;
    }

    public String getFQN() {
        return this.toString();
    }
}
