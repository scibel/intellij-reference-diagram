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

package ch.docksnet.rgraph.toolwindow;

import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.IconUtil;
import com.intellij.util.PlatformIcons;

import javax.swing.*;
import java.awt.*;

import static com.intellij.openapi.fileChooser.FileElement.isFileHidden;
import static com.intellij.openapi.util.IconLoader.getTransparentIcon;

public class VirtualFileCellRenderer {
    private static final Color HIDDEN = SimpleTextAttributes.DARK_TEXT.getFgColor();

    public static void render(SimpleColoredComponent renderer, VirtualFile virtualFile) {
        int style = SimpleTextAttributes.STYLE_PLAIN;
        Color color = null;
        Icon icon = getIcon(virtualFile);
        String name = null;
        String comment = null;
        boolean hidden = false;
        boolean valid = true;
        name = virtualFile.getName();
        hidden = isFileHidden(virtualFile);
        valid = virtualFile.isValid();
        if (!valid) style |= SimpleTextAttributes.STYLE_STRIKEOUT;
        if (hidden) color = HIDDEN;
        renderer.setIcon(!hidden || icon == null ? icon : getTransparentIcon(icon));
        SimpleTextAttributes attributes = new SimpleTextAttributes(style, color);
        if (name != null) renderer.append(name, attributes);
        if (comment != null) renderer.append(comment, attributes);
    }

    private static Icon getIcon(final VirtualFile file) {
        return dressIcon(file, IconUtil.getIcon(file, Iconable.ICON_FLAG_READ_STATUS, null));
    }

    private static Icon dressIcon(final VirtualFile file, final Icon baseIcon) {
        return file.isValid() && file.is(VFileProperty.SYMLINK) ? new LayeredIcon(baseIcon, PlatformIcons.SYMLINK_ICON) : baseIcon;
    }
}
