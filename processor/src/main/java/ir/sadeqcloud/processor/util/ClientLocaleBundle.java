package ir.sadeqcloud.processor.util;

import java.util.Locale;
import java.util.ResourceBundle;

public class ClientLocaleBundle {
    /**
     * static nested classes are not target for ClassLoader
     * hence the resources in them are Lazy
     * best case for singleton design pattern
     */
    private static class LazyResourceBundle{
        public static final ResourceBundle englishBundle=ResourceBundle.getBundle("messages", Locale.ENGLISH);
        private static final Locale farsiLocale=new Locale("fs");
        public static final ResourceBundle farsiBundle=ResourceBundle.getBundle("messages",farsiLocale);
    }
    public static ResourceBundle getLocaleBasedResourceBundle(SupportedLocale supportedLocale){
    switch (supportedLocale){
        case FARSI:
            return LazyResourceBundle.farsiBundle;
        case ENGLISH:
            return LazyResourceBundle.englishBundle;
        default:
            return LazyResourceBundle.farsiBundle;
    }
    }
}
