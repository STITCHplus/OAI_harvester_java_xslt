package OAI_harvester;

import java.io.*;
import java.lang.management.*;
import java.net.*;
import java.util.*;
import java.text.*;

import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.*;
import org.apache.log4j.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

/*

todo: 

HARVEST_CONSOLE:

    if the programm fails or get's interruped, send a <commit> command to solr.

    if the harvester is finished, write the timestamp to disk,
    in case the harvester is resumed, start harvesting with OAI-from command (with timestamp as value).

    implement a FROM field in the web-interface.

    implement a STOP function in the web-interface (when resumed, resume with OAI-from command using the timestamp).

--
SRU
    Finish SRU / http interface (80% done)

--
SOLR
    Check how to implement name-spellings variants in SOLR (called Hiearchy synonym??)



*/


public final class OAI_harvester
{
    public static boolean quit = false;
    public static boolean exit = false;
    public static String exit_reason = "";

    public static boolean finished = false;

    public final static String sep = java.io.File.separator;

    static Map config = new HashMap<String, Map>();
    public static Logger logger = Logger.getLogger(OAI_harvester.class);
    public static String PATH_TO_TMP = sep+"tmp"+sep;

    static int my_pid = Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]); 

    public final static String config_path = "etc"+sep+"solr_harvester.xml";
    public final static String xsl_path = "etc"+sep;

    public final static String path_to_me = OAI_harvester.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    public final static String my_name = path_to_me.split(java.io.File.separator)[path_to_me.split(java.io.File.separator).length-1];
    public final static String [] config_format = {"oai_baseurl", "oai_setname", "oai_metadataprefix", "oai_resumption_token", "oai_from", "oai_to", "oai_optional", "solr_pre_xslt", "solr_target"};

    public final static String PATH_TO_PS = "/bin/ps";
    public final static String PS_ARGUMENTS = " -Ao pid,command";

    static String token = null;
    static String from = null;

    public static StringBuffer xml_in = new StringBuffer();
    //public static StreamSource xml_out = new StreamSource();
    
    // public static PrintWriter xml_out = new StringWriter();
    public static StringWriter xml_out = new StringWriter();



    public static void main( String[] args ) 
    {
        String harvester_name = null;

        System.out.println("OAI_harvester v.0.0.1 (xml sax) \n");
        parse_config();
        BasicConfigurator.configure();

        if (args.length == 0) {
            System.out.println("Please specify harvester name as argument.\n");
            System.out.println("Configured harvesters are : \n");

            Iterator config_iter = config.keySet().iterator();
            
            while(config_iter.hasNext()) {
                String name = (String) config_iter.next();
                System.out.println(" " + name);
            }
            System.out.println("\n");
            System.exit(-1);
        }

        try {
            if (config.containsKey(args[0])) harvester_name = args[0];
        } catch (Exception e) { };

        if (harvester_name == null) {
            System.out.println("Unknown harvester " + (String) args[0]+"\n");
            System.exit(-1);
        }

        if (args.length == 2) {
            token = args[1];
            System.out.println("Resuming with token : "+token);
        } else {
            if ((String)((HashMap)config.get(harvester_name)).get("oai_resumption_token") != null) {
                token = (String)((HashMap)config.get(harvester_name)).get("oai_resumption_token");
                System.out.println("Resuming with token : "+token+" (As defined in config file)");
            }
        }

        if (args.length == 3) {
            if (args[1].equals("-from")) {
                System.out.println("Starting from : "+args[2]);
                from = args[2];
            }
        }

        Integer pid = get_pid(harvester_name);

        if (pid == -1) {
            harvest(harvester_name);
        } else {
            System.out.println("Error, Harvester "+harvester_name+" is already running, and is using pid " + pid.toString());
        }
    }

    private static void harvest(final String name) 
    {
        Thread go = new Thread() {
            public void run() {
                shutdownHook(name);
            }
        };

        Runtime.getRuntime().addShutdownHook(go);


        while (!(quit || exit)) {
            if (!get_oai_data(name)) {
                token=find_next_token();
                int error=0;
                while (!get_oai_data(name)) {
                    error+=1;
                    if (error == 10) {
                        break;
                    }
                    try {
                        Thread.sleep(200000);
                    } catch (Exception e) {
                        error=10;
                    }
                }
                if (error == 10) {
                    exit=true;
                    token="Error, failed to transport data from OAI server. (10 retries failed.)";
                }
            }
            
            if (!exit) {
                token = find_next_token();
            }

            if (token.equals("Finished")) {
                quit=true; 
                finished=true; 
                break;
            }

            if (token.startsWith("Error,")) {
                exit=true;
                exit_reason=token;
                break;
            }

            try {
                transform_data(name);
            } catch (Exception e) {
                exit=true;
                exit_reason="Error,Xslt failed";
                break;
            }
           
            try {
                post_data(name);
            } catch (Exception e) { 
                exit=true;
                System.out.println(e.toString());
                exit_reason="Error,Posting data to SOLR failed";
                break;
            }
        }
    }

    private static void post_data(String harvester_name) throws Exception {
        String data = xml_out.toString();
        xml_out = new StringWriter();
        int len = data.length();
        //System.out.println(Integer.toString(len));

        HashMap<String,String> selected_harvester = ((HashMap)config.get(harvester_name));
        URL url = null;

        try {
            url = new URL(selected_harvester.get("solr_target"));
        } catch (Exception e) { throw(e); }

        System.out.println(url);

        try {
            URLConnection c = url.openConnection();
            c.setDoOutput(true);
            c.setDoInput(true);

            ((HttpURLConnection)c).setRequestMethod("POST");
            c.addRequestProperty("Content-Type","text/xml; charset=utf-8");
            c.addRequestProperty("Content-Length",Integer.toString(len));
            OutputStream out = c.getOutputStream();
            out.write(data.getBytes());
            System.out.println(((HttpURLConnection)c).getResponseCode());
        } catch (Exception e) { throw(e); } 
    }

    public static boolean transform_data(String harvester_name) throws Exception
    {
        HashMap selected_harvester = ((HashMap)config.get(harvester_name));
        File f = new File(path_to_me);
        String path = f.getParent()+sep+".."+sep+xsl_path;

        System.out.println(path+sep+selected_harvester.get("solr_pre_xslt"));
        System.out.println(selected_harvester);

        f = new File(path+sep+selected_harvester.get("solr_pre_xslt"));
    
        if (f.exists()) {
            StreamSource xsl = new StreamSource(f);
            StreamSource xml = new StreamSource(new StringReader(xml_in.toString()));
            TransformerFactory tFact = TransformerFactory.newInstance();
            try {
                Transformer transformer = tFact.newTransformer(xsl);
                //setTransformParameters(transformer, paramHash);
                PrintWriter pw = new PrintWriter(xml_out);
                transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.transform(xml,  new StreamResult(xml_out));
                pw.close();
            } catch (Exception e) {
                //log.debug(this.working_for + " : " + e.getCause());
                System.out.println(e.getMessage());
                return false;
            }
        } else return false;
        
        return true;
    }

    public static Integer get_pid(String harvester_name) // still messy but powerfull..
    {
        try {
            String s = null;
            Runtime r = Runtime.getRuntime();
            Process p = r.exec(PATH_TO_PS+" "+PS_ARGUMENTS);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            p.waitFor();
            StringBuffer buffer = new StringBuffer();

            while ((s = stdInput.readLine()) != null) {
                if ((s.indexOf(my_name)  > 0)  && (s.indexOf(harvester_name) > 0)) {
                    String a[] = s.split(" ");
                    for (int i=0;i<a.length;i++) {
                        try {
                            int pid = Integer.parseInt(a[i]);
                            if (pid != my_pid) {
                                if (from == null) {
                                    return pid;
                                } else {
                                    if ( pid != Integer.parseInt(from) ) {
                                        return pid;
                                    }
                                }
                            }
                        } catch (Exception e) { };
                    }
                }
            }
        } catch (Exception e) {};
        return -1;
    }

    private static boolean get_oai_data(String name)
    {
        HashMap <String,String> selected_harvester = ((HashMap)config.get(name));
        URL url = null;
        String xml = null;
        xml_in = new StringBuffer();

        if ((token == null) && (from == null)) {
            try {
                url = new URL(selected_harvester.get("oai_baseurl")+"?verb=ListRecords"+"&set="+selected_harvester.get("oai_setname")+"&metadataPrefix="+selected_harvester.get("oai_metadataprefix"));
            } catch (Exception e) { return false; }
        }

        if (token != null) {
            try {
                url = new URL(selected_harvester.get("oai_baseurl")+"?verb=ListRecords"+"&resumptionToken="+token);
            } catch (Exception e) { return false; }
        }

        if (from != null) {
            try {
                url = new URL(selected_harvester.get("oai_baseurl")+"?verb=ListRecords"+"&set="+selected_harvester.get("oai_setname")+"&metadataPrefix="+selected_harvester.get("oai_metadataprefix")+"&from="+from);
                from=null;
            } catch (Exception e) { return false; }
        }

        System.out.println(url.toString());

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), "UTF8"));
            while ((xml= in.readLine()) != null) {
                xml_in.append(xml);
            }
        } catch (Exception e) { return false; }
         
        return true;
    }

    private static void shutdownHook(String name)
    {
        long t0 = System.currentTimeMillis();

        while (true) {
            try {
                quit=true;
                Thread.sleep (500);
             } catch (Exception e) {
                System.err.println("Exception: "+e.toString());
                break;
            }
            if (System.currentTimeMillis() - t0 > 2*100) break;
        }

        if (exit) { 
            System.out.println(exit_reason);
            update_status_file(name, exit_reason);
        }

        if (finished) {
            System.out.println("Finished");
            update_status_file(name, "Finished,,"+token);
        }

        if (!exit && !finished) {
            System.out.println(token+",Pauzed");
            update_status_file(name, "Pauzed,,"+token);
        }

    }

    private static void parse_config() 
    {
        File f = new File(path_to_me);
        String path = f.getParent()+java.io.File.separator+".."+java.io.File.separator+config_path;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            Document doc = factory.newDocumentBuilder().parse(new File(path));
            NodeList nodeChildren = doc.getElementsByTagName ("harvester");

            for (int i=0; i<nodeChildren.getLength();i++) {
                Map hhash = new HashMap<String, String>();
                Element harvester = (Element) nodeChildren.item(i);
                for (int j=0; j<config_format.length;j++) {
                    try{
                        Node nd = harvester.getElementsByTagName(config_format[j]).item(0);
                        hhash.put(config_format[j], nd.getFirstChild().getNodeValue());
                    } catch (Exception e) { };
                }
                config.put(nodeChildren.item(i).getAttributes().getNamedItem("name").getTextContent(), hhash);
            }
        }  catch (Exception e) {
            //System.out.println("Error while parsing or reading : " + path);
            System.out.println(e.toString());
            System.exit(-1);
        }
    }

    private static String find_next_token()
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        StringReader reader = new StringReader(xml_in.toString());
        InputSource inputSource = new InputSource( reader );
        Document doc = null;

        System.out.println("Try to find next_token..");

        try {
            doc = factory.newDocumentBuilder().parse(inputSource);
        } catch(Exception e) { return "Error,Received invalid xml"; }

        XPathFactory getrestoken = null;
        String next_token = null;

        try {
            getrestoken = XPathFactory.newInstance();

            XPath xpath = getrestoken.newXPath();
            XPathExpression expr = xpath.compile("/OAI-PMH/ListRecords/resumptionToken/text()");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            next_token = nodes.item(0).getNodeValue();
        } catch (Exception e) { return "Finished"; }
        System.out.println("NEXT token : " + next_token); 
        return next_token;
    }

    public static void update_status_file(String name, String message) {
        try {
            FileOutputStream out = new FileOutputStream(PATH_TO_TMP+sep+name+"_status");
            PrintStream p = new PrintStream( out );
            p.println (message);
            p.close();
        } catch (Exception e ) { }
    }

    public static String stackTraceToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
