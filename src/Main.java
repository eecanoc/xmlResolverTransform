import net.sf.saxon.Configuration;
import net.sf.saxon.TransformerFactoryImpl;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlresolver.Resolver;
import org.xmlresolver.ResolverFeature;
import org.xmlresolver.XMLResolverConfiguration;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Main {

  public static void main(String[] args) {
    String sourcePath = "/home/elver/data/rsuite/rsuite-data/workflow-data/hotfolder-20240313-201743.126_96/APA_2022_70_4_20240313201808.xml";
    String xsltPath = "/home/elver/data/rsuite/xml/xslt/jrnl-ingest-config.xsl";
    String resultDir = "/home/elver/data/rsuite/rsuite-data/workflow-data/hotfolder-20240313-201743.126_96/ingest-config_20240313201808.xml";
    String catalogXml = "/home/elver/data/rsuite/xml/schema/catalog.xml";
    String zipFileName = "APA_2022_70_4.zip";

    Map<String, String> mapParameters = new HashMap<>();
    // Add any parameters if needed
    mapParameters.put("socrViewUrl", "http://localhost:8201");
    mapParameters.put("zipFileName", "APA_2022_70_4.zip");
    mapParameters.put("unzippedRootDirectory", "/home/elver/data/rsuite/rsuite-data/workflow-data/hotfolder-20240313-201743.126_96/unzipped");
    mapParameters.put("schematronName", "journalSchematron");
    mapParameters.put("ingestionId", "1.0.0_journal_issue");
    mapParameters.put("schematronPhase", "currentIssueContent");

    try {
      executeTransform(sourcePath, xsltPath, resultDir, catalogXml, mapParameters, zipFileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void executeTransform(String sourcePath, String xsltPath, String resultDir, String catalogXml,
                                      Map<String, String> mapParameters, String zipFileName) throws Exception {
    final ArrayList<TransformerException> errorList = new ArrayList<>();
    ErrorListener errorListener = new ErrorListener() {
      @Override
      public void warning(TransformerException e) {
        errorList.add(e);
      }

      @Override
      public void error(TransformerException e) {
        errorList.add(e);
      }

      @Override
      public void fatalError(TransformerException e) {
        errorList.add(e);
      }
    };
    TransformerFactory tFactory = TransformerFactory.newInstance();
    tFactory.setErrorListener(errorListener);
    TransformerFactoryImpl tf = (TransformerFactoryImpl) tFactory;
    Configuration conf = tf.getConfiguration();
    conf.setExpandAttributeDefaults(false);
    try {
      validateStyleSheet(xsltPath, zipFileName);
      Transformer transformer = tf.newTransformer(new StreamSource(new File(xsltPath)));
      transformer.setURIResolver(getResolver(catalogXml));
      transformer.setErrorListener(errorListener);
      for (Map.Entry<String, String> lmd : mapParameters.entrySet()) {
        transformer.setParameter(lmd.getKey(), lmd.getValue());
      }
      if (new File(sourcePath).exists()) {
        transformer.transform(new StreamSource(new File(sourcePath)), new StreamResult(new File(resultDir)));
      } else {
        Source source = new StreamSource(new StringReader(sourcePath));
        transformer.transform(source, new StreamResult(new File(resultDir)));
      }
    } catch (Exception ex) {
      handleException(errorList, ex);
    }
  }

  private static void handleException(final ArrayList<TransformerException> errorList, Exception ex)
    throws Exception {
    StringBuilder messageException = new StringBuilder();
    for (TransformerException exception : errorList) {
      messageException.append("com.sage.utils.executeTransform: -").append(exception.getMessage()).append("-")
        .append(exception.getLocationAsString());
      if (exception.getCause() != null) {
        messageException.append("- getCause:").append(exception.getCause().toString());
      }
    }
    System.out.println(messageException);
    throw ex;
  }

  private static URIResolver getResolver(String catalogXml) {
    XMLResolverConfiguration config = new XMLResolverConfiguration();
    URI caturi = URI.create(catalogXml);
    config.addCatalog(caturi.toString());
    config.setFeature(ResolverFeature.CATALOG_LOADER_CLASS, "org.xmlresolver.loaders.ValidatingXmlLoader");
    return new Resolver(config);
    //return new XMLResolver(config).getURIResolver();
  }

  private static void validateStyleSheet(String xsltFile, String zipFileName)
    throws ParserConfigurationException, SAXException, IOException {
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      dbFactory.setNamespaceAware(true);
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(xsltFile);
      doc.getDocumentElement().normalize();
    } catch (ParserConfigurationException | SAXException | IOException ex) {
      throw ex;
    }
  }
}