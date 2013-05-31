package com.bt.pi.core;

import java.io.File;
import java.io.FileFilter;

public class ExcludeHiddenFilesFilter implements FileFilter {
    public ExcludeHiddenFilesFilter() {
    }

    @Override
    public boolean accept(File pathname) {
        return !pathname.isHidden();
    }
}
