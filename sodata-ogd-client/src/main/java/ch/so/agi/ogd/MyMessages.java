package ch.so.agi.ogd;

import com.google.gwt.i18n.client.Messages;

public interface MyMessages extends Messages {
    @DefaultMessage("Fubar")
    String fubar();
    
    @DefaultMessage("Füü {0} bar {1}")
    String yinyang(String yin, String yang);
    
    // Search
    @DefaultMessage("Search terms")
    String search_terms();
}
