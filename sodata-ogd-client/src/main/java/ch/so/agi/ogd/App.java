package ch.so.agi.ogd;

import static elemental2.dom.DomGlobal.console;
import static elemental2.dom.DomGlobal.fetch;
import static org.jboss.elemento.Elements.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.dominokit.domino.ui.badges.Badge;
import org.dominokit.domino.ui.breadcrumbs.Breadcrumb;
import org.dominokit.domino.ui.button.Button;
import org.dominokit.domino.ui.forms.TextBox;
import org.dominokit.domino.ui.icons.Icons;
import org.dominokit.domino.ui.modals.ModalDialog;
import org.dominokit.domino.ui.style.Color;
import org.dominokit.domino.ui.style.ColorScheme;
import org.dominokit.domino.ui.themes.Theme;

import com.google.gwt.core.client.GWT;
import org.gwtproject.safehtml.shared.SafeHtmlUtils;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.i18n.client.DateTimeFormat;

import elemental2.core.Global;
import elemental2.core.JsArray;
import elemental2.core.JsString;
import elemental2.dom.AbortController;
import elemental2.dom.DomGlobal;
import elemental2.dom.Element;
import elemental2.dom.Event;
import elemental2.dom.EventListener;
import elemental2.dom.HTMLDivElement;
import elemental2.dom.HTMLDocument;
import elemental2.dom.HTMLElement;
import elemental2.dom.HTMLTableCellElement;
import elemental2.dom.HTMLTableElement;
import elemental2.dom.HTMLTableRowElement;
import elemental2.dom.HTMLTableSectionElement;
import elemental2.dom.KeyboardEvent;
import elemental2.dom.Location;
import elemental2.dom.RequestInit;
import elemental2.dom.URL;
import elemental2.dom.URLSearchParams;
import elemental2.dom.XMLHttpRequest;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;

public class App implements EntryPoint {
    // Internationalization
    private MyMessages messages = GWT.create(MyMessages.class);

    // Client application settings
    private String myVar;
    private String FILES_SERVER_URL;

    // Format settings
    private NumberFormat fmtDefault = NumberFormat.getDecimalFormat();
    private NumberFormat fmtPercent = NumberFormat.getFormat("#0.0");

    // Browser-URL components
    private Location location;
    private String pathname;
    private String filter = null;
    private String FILTER_PARAM_KEY = "filter";

    // Main HTML elements
    private HTMLElement container;
    private HTMLElement topLevelContent;
    private HTMLElement datasetContent;
    private HTMLTableElement rootTable = table().element();
    
    // Datasets vars
    private List<Object> datasets;
    private List<Object> fullDatasets;

    // Abort controller for fetching from server
    private AbortController abortController = null;

    public void onModuleLoad() {
        // Change Domino UI color scheme.
        Theme theme = new Theme(ColorScheme.RED);
        theme.apply();

        // Get url from browser (client) to find out the correct location of resources.
        location = DomGlobal.window.location;
        pathname = location.pathname;

        if (pathname.contains("index.html")) {
            pathname = pathname.replace("index.html", "");
        }
        
        // TODO: braucht es nicht mehr wenn files url in den Daten steckt.!?
        // Get settings with a synchronous request.
        XMLHttpRequest httpRequest = new XMLHttpRequest();
        httpRequest.open("GET", pathname + "settings", false);
        httpRequest.onload = event -> {
            if (Arrays.asList(200, 201, 204).contains(httpRequest.status)) {
                String responseText = httpRequest.responseText;
                try {
                    JsPropertyMap<Object> propertiesMap = Js.asPropertyMap(Global.JSON.parse(responseText));
                    FILES_SERVER_URL = propertiesMap.getAsAny("filesServerUrl").asString();

                } catch (Exception e) {
                    DomGlobal.window.alert("Error loading settings!");
                    DomGlobal.console.error("Error loading settings!", e);
                }
            } else {
                DomGlobal.window.alert("Error loading settings!" + httpRequest.status);
            }

        };

        httpRequest.addEventListener("error", event -> {
            DomGlobal.window
                    .alert("Error loading settings! Error: " + httpRequest.status + " " + httpRequest.statusText);
        });

        httpRequest.send();

        // Get themes publications json from server and initialize the site.
        DomGlobal.fetch("/datasets").then(response -> {
            if (!response.ok) {
                DomGlobal.window.alert(response.statusText + ": " + response.body);
                return null;
            }
            return response.text();
        }).then(json -> {
//            themePublications = mapper.read(json);
//            Collections.sort(themePublications, new ThemePublicationComparator());
            
            //List<SuggestItem<SearchResult>> suggestItems = new ArrayList<>();
            //JsPropertyMap<?> parsed = Js.cast(Global.JSON.parse(json));
            JsArray<?> datasetsArray = Js.cast(Global.JSON.parse(json));

            datasets = (List<Object>) datasetsArray.asList();
            Collections.sort(datasets, new DatasetComparator());
            fullDatasets = datasets;
            
            init();

            return null;
        }).catch_(error -> {
            console.log(error);
            DomGlobal.window.alert(error.toString());
            return null;
        });
    }
    
    public void init() {
        // HTMLDocument: used for creating html elements that are not
        // available in elemento (e.g. summary, details).
        HTMLDocument document = DomGlobal.document;

        // This cannot be done in index.html since href depends
        // on the real world url.
//        Element head = document.getElementsByTagName("head").getAt(0);
//        HTMLElement opensearchdescription = (HTMLElement) document.createElement("link");
//        opensearchdescription.setAttribute("rel", "search");
//        opensearchdescription.setAttribute("type", "application/opensearchdescription+xml");
//
//        String host = location.host;
//        String protocol = location.protocol;
//        opensearchdescription.setAttribute("href", protocol + "//" + host + pathname + "opensearchdescription.xml");
//        opensearchdescription.setAttribute("title", "Geodaten Kanton Solothurn");
//        head.appendChild(opensearchdescription);

        // Get search params to control the browser url
        URLSearchParams searchParams = new URLSearchParams(location.search);

        if (searchParams.has(FILTER_PARAM_KEY)) {
            filter = searchParams.get(FILTER_PARAM_KEY);
        }

        // Add invisible download anchor element. Used for downloading file when clicking into map.
        body().add(a().attr("download", "").id("download").element());
        
        // Add our "root" container
        container = div().id("container").element();
        body().add(container);

        // Add logo
        HTMLElement logoDiv = div().css("logo")
                .add(div().add(
                        img().attr("src", location.protocol + "//" + location.host + location.pathname + "Logo.png")
                                .attr("alt", "Logo Kanton"))
                        .element())
                .element();
        container.appendChild(logoDiv);

        // Create a top level content div for everything except the logo.
        // Not sure why this was done this way. Or if it is necessary.
        topLevelContent = div().id("top-level-content").element();
        container.appendChild(topLevelContent);

        // Add breadcrumb
        Breadcrumb breadcrumb = Breadcrumb.create().appendChild(Icons.ALL.home(), " Home ", (evt) -> {
            DomGlobal.window.open("https://data.so.ch/", "_self");
        }).appendChild(" Offene Daten ", (evt) -> {
        });
        topLevelContent.appendChild(breadcrumb.element());

        topLevelContent.appendChild(div().css("sodata-title").textContent("Offene Daten Kanton Fubar").element());

        String infoString = "Fake. Test. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor "
                + "finden Sie <a class='default-link' href='https://so.ch/verwaltung/bau-und-justizdepartement/amt-fuer-geoinformation/geoportal/geodaten-herunterladen/' target='_blank'>hier</a>.";

        topLevelContent.appendChild(div().css("info").innerHtml(SafeHtmlUtils.fromTrustedString(infoString)).element());

        TextBox textBox = TextBox.create().setLabel(messages.search_terms());
        textBox.addLeftAddOn(Icons.ALL.search());
        textBox.setFocusColor(Color.RED_DARKEN_3);
        textBox.getInputElement().setAttribute("autocomplete", "off");
        textBox.getInputElement().setAttribute("spellcheck", "false");

        textBox.focus();

        HTMLElement resetIcon = Icons.ALL.close().style().setCursor("pointer").get().element();
        resetIcon.addEventListener("click", new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                textBox.clear();
                // TODO
//                themePublicationListStore.setData(themePublications);
                removeResults();
                updatingResultTableElement(fullDatasets);

                removeQueryParam(FILTER_PARAM_KEY);
            }
        });
        textBox.addRightAddOn(resetIcon);

        textBox.addEventListener("keyup", event -> {
            if (textBox.getValue().trim().length() > 0 && textBox.getValue().trim().length() <= 2) {
                //themePublicationListStore.setData(themePublications);
                return;
            }

            if (textBox.getValue().trim().length() == 0) {
                //themePublicationListStore.setData(themePublications);
                removeResults();
                updatingResultTableElement(fullDatasets);

                removeQueryParam(FILTER_PARAM_KEY);
                return;
            }

            if (abortController != null) {
                abortController.abort();
            }

            abortController = new AbortController();
            final RequestInit init = RequestInit.create();
            init.setSignal(abortController.signal);

            DomGlobal.fetch("/datasets?query=" + textBox.getValue().toLowerCase(), init).then(response -> {
                if (!response.ok) {
                    return null;
                }
                return response.text();
            }).then(json -> {
                //List<ThemePublicationDTO> filteredThemePublications = mapper.read(json);
                //filteredThemePublications.sort(new ThemePublicationComparator());

                //themePublicationListStore.setData(filteredThemePublications);
                
                JsArray<?> datasetsArray = Js.cast(Global.JSON.parse(json));
                datasets = (List<Object>) datasetsArray.asList();
                Collections.sort(datasets, new DatasetComparator());

                removeResults();
                updatingResultTableElement(datasets);
                
                updateUrlLocation(FILTER_PARAM_KEY, textBox.getValue().trim());

                abortController = null;

                return null;
            }).catch_(error -> {
                console.log(error);
                return null;
            });
        });
        topLevelContent.appendChild(div().id("search-panel").add(div().id("suggestbox-div").add(textBox)).element());

        updatingResultTableElement(datasets);
        
        topLevelContent.appendChild(rootTable);
        
        if (filter != null && filter.trim().length() > 0) {
            textBox.setValue(filter);
            textBox.element().dispatchEvent(new KeyboardEvent("keyup"));
        }
    }    
    
    private void updatingResultTableElement(List<Object> datasets) {
        rootTable.id = "datasets-table";

        rootTable.appendChild(colgroup()
                .add(col().attr("span", "1").style("width: 2%"))
                .add(col().attr("span", "1").style("width: 2%"))
                .add(col().attr("span", "1").style("width: 46%"))
                .add(col().attr("span", "1").style("width: 18%"))
                .add(col().attr("span", "1").style("width: 12%"))
                .add(col().attr("span", "1").style("width: 20%"))
                .element());
        HTMLTableSectionElement mapsTableHead = thead()
                .add(tr()
                        .add(th().add(""))
                        .add(th().attr("colspan", "2").add("Titel"))
                        .add(th().add("Publikationsdatum")) 
                        .add(th().style("text-align: center;").add("Details"))
                        .add(th().add("Daten herunterladen")))
                .element();
        rootTable.appendChild(mapsTableHead);

        
        for (Object dataset : datasets) {
            HTMLTableSectionElement tbodyParent = tbody().element();
            
            HTMLTableRowElement tr = tr().element();
            HTMLTableCellElement tdSublayerIcon = null;
            
            JsPropertyMap<?> datasetObj = Js.cast(dataset);
                        
            JsArray<?> resources;
            try {
                resources = Js.cast(datasetObj.get("Resources"));  
                JsArray<?> foo = new JsArray();
                console.log("ich konnte zum Array casten");
            } catch (java.lang.ClassCastException e) {
                console.log("ich konnte nicht zum Array casten und muss es händisch machen.");
                JsPropertyMap<?> resourcesMap = Js.cast(datasetObj.get("Resources"));      
                resources = JsArray.of(resourcesMap);
            }
            
//            List<JsPropertyMap<?>> resourcesList
//            for (int i=0; i<resources.length; i++) {
//                
//            }
//            datasetObj.set("Resources", datasetObj.get("Resources"));
                        
            // Icon
            if (resources.length > 1) {
                //HTMLElement sublayersIcon = Icons.ALL.file_multiple_outline_mdi().style().setCursor("pointer").get().element();
                HTMLElement sublayersIcon = Icons.ALL.plus_mdi().style().setCursor("pointer").get().element();
                tdSublayerIcon = td().add(sublayersIcon).element();
                tr.appendChild(tdSublayerIcon);
            } else {
                //HTMLElement layerIcon = Icons.ALL.file_outline_mdi().style().setCursor("pointer").get().element();
                HTMLElement layerIcon = Icons.ALL.file_outline_mdi().element();
                tr.appendChild(td().add(layerIcon).element());
            }
            
            // Title
            String title = ((JsString) datasetObj.get("Title")).normalize();
            console.log(title);
            HTMLTableCellElement tdParentTitle = td().attr("colspan", "2").add(title).element();
            tr.appendChild(tdParentTitle);
            
            // Publication date
            JsPropertyMap<?> resourceTmp = Js.cast(resources.getAt(0));
            console.log("foo1");
            console.log(resources);
            console.log(resourceTmp);
            String dateString = ((JsString) resourceTmp.get("lastPublishingDate")).normalize();
            console.log("foo2");
            Date date = DateTimeFormat.getFormat("yyyy-MM-dd").parse(dateString);
            String formattedDateString = DateTimeFormat.getFormat("dd.MM.yyyy").format(date);
            tr.appendChild(td().add(formattedDateString).element()); 
            
            // Details / Metadata
            HTMLElement metadataLinkElement = null;
            if (resources.length == 1) {
                JsPropertyMap<?> resource = (JsPropertyMap<?>) resources.getAt(0);
                metadataLinkElement = div()
                        .add(Icons.ALL.information_outline_mdi().style().setCursor("pointer"))
                        .element();
                metadataLinkElement.addEventListener("click", new EventListener() {
                    @Override
                    public void handleEvent(Event evt) {
                        openMetadataDialog(datasetObj, resource);
                    }
                });
            } else {
                metadataLinkElement = div().add("").element();
            }
            tr.appendChild(td().attr("align", "center").add(metadataLinkElement).element());

            HTMLElement badgesElement = div().element();
            if (resources.length == 1) {
                JsPropertyMap<?> resource = (JsPropertyMap<?>) resources.getAt(0);
                JsArray<?> fileFormats = Js.cast(resource.get("FileFormats"));
                for (int i=0; i<fileFormats.length; i++) {
                    JsPropertyMap<?> fileFormat = Js.cast(fileFormats.getAt(i));
                    String ffName = ((JsString)fileFormat.get("Name")).normalize();
                    String ffExt = ((JsString)fileFormat.get("Extension")).normalize();
                    String ffAbbr = ((JsString)fileFormat.get("Abbreviation")).normalize();
                    String identifier = ((JsString) datasetObj.get("Identifier")).normalize();
                    String resourceIdentifier = ((JsString) resource.get("Identifier")).normalize();

                    String fileUrl = "https://s3.eu-central-1.amazonaws.com/ch.so.data-dev/" + identifier
                            + "/" + resourceIdentifier + "."
                            + ffExt;
                    
                    badgesElement.appendChild(a().css("badge-link")
                            .attr("href", fileUrl)
                            .attr("target", "_blank")
                            .add(Badge.create(ffName)
                                    .setBackground(Color.GREY_LIGHTEN_2)
                                    .style()
                                    .setMarginRight("10px")
                                    .setMarginTop("5px")
                                    .setMarginBottom("5px")
                                    .get()
                                    .element())
                            .element());
                }
            } else {
                
            }
            tr.appendChild(td().attr("align", "center").add(badgesElement).element());

            
            tbodyParent.appendChild(tr);
            rootTable.appendChild(tbodyParent);

            
            List<Object> resourcesList = (List<Object>) resources.asList();
            Collections.sort(resourcesList, new ResourceComparator());

            if (resources.length > 1) { 
                HTMLTableSectionElement tbodyChildren = tbody().css("hide").element();
                for (int j=0; j<resources.length; j++) {
                    JsPropertyMap<?> resource = Js.cast(resourcesList.get(j));
                    String resourceName = ((JsString) resource.get("Title")).normalize();
                    
                    console.log("foo3");
                    String resourceDateString = ((JsString) resource.get("lastPublishingDate")).normalize();
                    console.log("foo4");
                    Date resourceDate = DateTimeFormat.getFormat("yyyy-MM-dd").parse(resourceDateString);
                    String formattedResourceDateString = DateTimeFormat.getFormat("dd.MM.yyyy").format(resourceDate);
                    
                    //HTMLElement layerIcon = Icons.ALL.file_outline_mdi().style().setCursor("pointer").get().element();
                    HTMLElement layerIcon = Icons.ALL.file_outline_mdi().style().element();
                    HTMLTableRowElement trSublayer = tr().add(td().add("")).add(td().add(layerIcon)).add(td().add(resourceName)).add(td().add(formattedResourceDateString)).element();
                    
                    HTMLElement sublayerMetadataLinkElement = div()
                            .add(Icons.ALL.information_outline_mdi().style().setCursor("pointer"))
                            .element();
                    sublayerMetadataLinkElement.addEventListener("click", new EventListener() {
                        @Override
                        public void handleEvent(Event evt) {
                            openMetadataDialog(datasetObj, resource);
                        }
                    });

                    trSublayer.appendChild(td().attr("align", "center").add(sublayerMetadataLinkElement).element());
                    
                    HTMLElement subBadgesElement = div().element();
                    JsArray<?> fileFormats = Js.cast(resource.get("FileFormats"));
                    for (int i=0; i<fileFormats.length; i++) {
                        JsPropertyMap<?> fileFormat = Js.cast(fileFormats.getAt(i));
                        String ffName = ((JsString)fileFormat.get("Name")).normalize();
                        String ffExt = ((JsString)fileFormat.get("Extension")).normalize();
                        String ffAbbr = ((JsString)fileFormat.get("Abbreviation")).normalize();
                        String identifier = ((JsString) datasetObj.get("Identifier")).normalize();
                        String resourceIdentifier = ((JsString) resource.get("Identifier")).normalize();

                        String fileUrl = "https://s3.eu-central-1.amazonaws.com/ch.so.data-dev/" + identifier
                                + "/" + resourceIdentifier + "."
                                + ffExt;
                        
                        subBadgesElement.appendChild(a().css("badge-link")
                                .attr("href", fileUrl)
                                .attr("target", "_blank")
                                .add(Badge.create(ffName)
                                        .setBackground(Color.GREY_LIGHTEN_2)
                                        .style()
                                        .setMarginRight("10px")
                                        .setMarginTop("5px")
                                        .setMarginBottom("5px")
                                        .get()
                                        .element())
                                .element());
                    }
                    trSublayer.appendChild(td().attr("align", "center").add(subBadgesElement).element());

                    tbodyChildren.appendChild(trSublayer);
                }
                rootTable.appendChild(tbodyChildren);
                
                tdSublayerIcon.addEventListener("click", new EventListener() {
                    @Override
                    public void handleEvent(Event evt) {
                        tbodyChildren.classList.toggle("hide");
                    }
                });
                
                tdParentTitle.addEventListener("click", new EventListener() {
                    @Override
                    public void handleEvent(Event evt) {
                        tbodyChildren.classList.toggle("hide");
                    }
                });

            }
        }
    }
    
    private void openMetadataDialog(JsPropertyMap<?> datasetObj, JsPropertyMap<?> resource) {
        String title = ((JsString) resource.get("Title")).normalize();
        ModalDialog modal = ModalDialog.create(title).large().setAutoClose(true);
        modal.css("modal-object");
        
        MetadataElement metaDataElement = new MetadataElement(datasetObj, resource, messages);
        modal.appendChild(metaDataElement);

        
        
        
        //modal.add(div().innerHtml(SafeHtmlUtils.fromTrustedString(theAbstract)));
                
        Button closeButton = Button.create("SCHLIESSEN").linkify();
        closeButton.removeWaves();
        closeButton.setBackground(Color.RED_DARKEN_3);
        EventListener closeModalListener = (evt) -> modal.close();
        closeButton.addClickListener(closeModalListener);
        modal.appendFooterChild(closeButton);
        modal.open();

        closeButton.blur();
    }

    
    private void removeResults() {
        rootTable.getElementsByTagName("tbody");
        
        while(rootTable.firstChild != null) {
            rootTable.removeChild(rootTable.firstChild);
        }        
    }
    
    private void removeQueryParam(String key) {
        URL url = new URL(DomGlobal.location.href);
        String host = url.host;
        String protocol = url.protocol;
        String pathname = url.pathname;
        URLSearchParams params = url.searchParams;
        params.delete(key);

        String newUrl = protocol + "//" + host + pathname + "?" + params.toString();
        updateUrlWithoutReloading(newUrl);
    }

    private void updateUrlLocation(String key, String value) {
        URL url = new URL(DomGlobal.location.href);
        String host = url.host;
        String protocol = url.protocol;
        String pathname = url.pathname;
        URLSearchParams params = url.searchParams;
        params.set(key, value);

        String newUrl = protocol + "//" + host + pathname + "?" + params.toString();

        updateUrlWithoutReloading(newUrl);
    }

    // Update the URL in the browser without reloading the page.
    private static native void updateUrlWithoutReloading(String newUrl) /*-{
        $wnd.history.pushState(newUrl, "", newUrl);
    }-*/;
}