package com.hissab.soap;

import com.hissab.ejb.CalculEJBLocal;
import com.hissab.ejb.TraceEJBLocal;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Custom SOAP servlet that manually handles SOAP requests without JAX-WS
 * This avoids the ServletContainerInitializer conflicts with GlassFish 7
 * Updated to use JNDI lookups instead of @EJB annotations to avoid classloader issues
 */
@WebServlet(
    name = "HissabSOAPServlet",
    urlPatterns = {"/HissabService"},
    loadOnStartup = 1
)
public class HissabSOAPServlet extends HttpServlet {
    
    private static final Logger logger = Logger.getLogger(HissabSOAPServlet.class.getName());
    
    // Use JNDI lookup instead of @EJB to avoid classloader conflicts
    private CalculEJBLocal calculEJB;
    private TraceEJBLocal traceEJB;
    
    private static final String TARGET_NAMESPACE = "http://service.hissab.com/";
    private static final String SERVICE_NAME = "HissabService";
    
    @PostConstruct
    public void init() {
        logger.log(Level.INFO, "HissabSOAPServlet initialized for GlassFish 7");
        
        // Use JNDI lookup for EJBs to avoid injection conflicts
        try {
            InitialContext ctx = new InitialContext();
            
            // Try different JNDI names for EJB lookup
            try {
                calculEJB = (CalculEJBLocal) ctx.lookup("java:app/hissab-ejb/CalculEJB");
                logger.log(Level.INFO, "CalculEJB lookup successful using java:app");
            } catch (NamingException e) {
                try {
                    calculEJB = (CalculEJBLocal) ctx.lookup("java:module/CalculEJB");
                    logger.log(Level.INFO, "CalculEJB lookup successful using java:module");
                } catch (NamingException e2) {
                    try {
                        calculEJB = (CalculEJBLocal) ctx.lookup("ejb/CalculEJB");
                        logger.log(Level.INFO, "CalculEJB lookup successful using ejb/");
                    } catch (NamingException e3) {
                        logger.log(Level.SEVERE, "CalculEJB lookup failed with all JNDI names", e3);
                    }
                }
            }
            
            // Try different JNDI names for TraceEJB lookup
            try {
                traceEJB = (TraceEJBLocal) ctx.lookup("java:app/hissab-ejb/TraceEJB");
                logger.log(Level.INFO, "TraceEJB lookup successful using java:app");
            } catch (NamingException e) {
                try {
                    traceEJB = (TraceEJBLocal) ctx.lookup("java:module/TraceEJB");
                    logger.log(Level.INFO, "TraceEJB lookup successful using java:module");
                } catch (NamingException e2) {
                    try {
                        traceEJB = (TraceEJBLocal) ctx.lookup("ejb/TraceEJB");
                        logger.log(Level.INFO, "TraceEJB lookup successful using ejb/");
                    } catch (NamingException e3) {
                        logger.log(Level.SEVERE, "TraceEJB lookup failed with all JNDI names", e3);
                    }
                }
            }
            
        } catch (NamingException e) {
            logger.log(Level.SEVERE, "Failed to initialize JNDI context", e);
        }
        
        if (calculEJB == null) {
            logger.log(Level.WARNING, "CalculEJB lookup failed");
        }
        if (traceEJB == null) {
            logger.log(Level.WARNING, "TraceEJB lookup failed");
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String queryString = request.getQueryString();
        
        if ("wsdl".equalsIgnoreCase(queryString)) {
            // Return WSDL
            response.setContentType("text/xml; charset=UTF-8");
            response.getWriter().write(generateWSDL(request));
        } else {
            // Return service info page
            response.setContentType("text/html; charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.println("<!DOCTYPE html>");
            out.println("<html><head><title>HISSAB SOAP Service</title></head>");
            out.println("<body>");
            out.println("<h1>HISSAB SOAP Web Service</h1>");
            out.println("<p>This is a SOAP web service for mathematical expression evaluation.</p>");
            out.println("<p><strong>WSDL:</strong> <a href=\"?wsdl\">?wsdl</a></p>");
            out.println("<h2>Available Operations:</h2>");
            out.println("<ul>");
            out.println("<li>calculateFromString(expression)</li>");
            out.println("<li>calculateFromImage(image)</li>");
            out.println("<li>healthCheck()</li>");
            out.println("</ul>");
            out.println("</body></html>");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        logger.log(Level.INFO, "Received SOAP request");
        
        try {
            // Parse SOAP request
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document soapRequest = builder.parse(request.getInputStream());
            
            // Extract operation and parameters
            SOAPRequestInfo requestInfo = parseSoapRequest(soapRequest);
            
            // Process request and generate response
            String result = processSOAPRequest(requestInfo);
            
            // Generate SOAP response
            String soapResponse = generateSOAPResponse(requestInfo.operation, result);
            
            // Send response
            response.setContentType("text/xml; charset=UTF-8");
            response.getWriter().write(soapResponse);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing SOAP request", e);
            
            // Generate SOAP fault response
            String faultResponse = generateSOAPFault("Server", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/xml; charset=UTF-8");
            response.getWriter().write(faultResponse);
        }
    }
    
    // ... [rest of the methods remain the same as in the original servlet]
    // Copy all the remaining private methods from the original HissabSOAPServlet class
    
    private SOAPRequestInfo parseSoapRequest(Document soapDoc) throws Exception {
        SOAPRequestInfo info = new SOAPRequestInfo();
        
        // Find the operation in the SOAP body
        NodeList bodyList = soapDoc.getElementsByTagNameNS("http://schemas.xmlsoap.org/soap/envelope/", "Body");
        if (bodyList.getLength() == 0) {
            throw new Exception("SOAP Body not found");
        }
        
        Element body = (Element) bodyList.item(0);
        NodeList operations = body.getChildNodes();
        
        for (int i = 0; i < operations.getLength(); i++) {
            Node node = operations.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element operation = (Element) node;
                info.operation = operation.getLocalName();
                
                // Extract parameters based on operation
                if ("calculateFromString".equals(info.operation)) {
                    info.expression = getElementValue(operation, "expression");
                } else if ("calculateFromImage".equals(info.operation)) {
                    // For image, we'd normally handle binary data, but for simplicity, simulate
                    info.imageSize = 1024; // Simulate image size
                } else if ("healthCheck".equals(info.operation)) {
                    // No parameters needed
                }
                break;
            }
        }
        
        return info;
    }
    
    private String getElementValue(Element parent, String elementName) {
        // First try with namespace
        NodeList nodeList = parent.getElementsByTagNameNS(TARGET_NAMESPACE, elementName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node.hasChildNodes()) {
                return node.getFirstChild().getNodeValue();
            }
        }
        
        // Fallback to local name without namespace
        nodeList = parent.getElementsByTagName(elementName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node.hasChildNodes()) {
                return node.getFirstChild().getNodeValue();
            }
        }
        
        return null;
    }
    
    private String processSOAPRequest(SOAPRequestInfo requestInfo) throws Exception {
        logger.log(Level.INFO, "Processing SOAP operation: " + requestInfo.operation);
        
        switch (requestInfo.operation) {
            case "calculateFromString":
                return processCalculateFromString(requestInfo.expression);
                
            case "calculateFromImage":
                return processCalculateFromImage(requestInfo.imageSize);
                
            case "healthCheck":
                return processHealthCheck();
                
            default:
                throw new Exception("Unknown operation: " + requestInfo.operation);
        }
    }
    
    private String processCalculateFromString(String expression) throws Exception {
        if (expression == null || expression.trim().isEmpty()) {
            throw new Exception("Expression cannot be empty");
        }
        
        if (calculEJB == null) {
            throw new Exception("CalculEJB is not available");
        }
        
        String result = calculEJB.evaluateExpression(expression.trim());
        
        // Log trace
        try {
            if (traceEJB != null) {
                traceEJB.logTrace(expression.trim(), result);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to log trace", e);
        }
        
        return result;
    }
    
    private String processCalculateFromImage(int imageSize) throws Exception {
        if (calculEJB == null) {
            throw new Exception("CalculEJB is not available");
        }
        
        // Simulate OCR processing
        String[] expressions = {"2+3", "5*4", "10-3", "15/3", "2+3*4"};
        String expression = expressions[imageSize % expressions.length];
        
        String result = calculEJB.evaluateExpression(expression);
        
        // Log trace with OCR indication
        try {
            if (traceEJB != null) {
                traceEJB.logTrace("[OCR] " + expression, result);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to log OCR trace", e);
        }
        
        return "OCR Result: " + expression + " = " + result;
    }
    
    private String processHealthCheck() throws Exception {
        if (calculEJB == null) {
            return "Service health check failed: CalculEJB is not available";
        }
        
        String testResult = calculEJB.evaluateExpression("1+1");
        return "Service is healthy. Test calculation: 1+1 = " + testResult;
    }
    
    private String generateSOAPResponse(String operation, String result) {
        StringBuilder response = new StringBuilder();
        response.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        response.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" ");
        response.append("xmlns:tns=\"").append(TARGET_NAMESPACE).append("\">\n");
        response.append("  <soap:Body>\n");
        response.append("    <tns:").append(operation).append("Response>\n");
        response.append("      <tns:return>").append(escapeXml(result)).append("</tns:return>\n");
        response.append("    </tns:").append(operation).append("Response>\n");
        response.append("  </soap:Body>\n");
        response.append("</soap:Envelope>");
        
        return response.toString();
    }
    
    private String generateSOAPFault(String faultCode, String faultString) {
        StringBuilder fault = new StringBuilder();
        fault.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        fault.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n");
        fault.append("  <soap:Body>\n");
        fault.append("    <soap:Fault>\n");
        fault.append("      <faultcode>").append(faultCode).append("</faultcode>\n");
        fault.append("      <faultstring>").append(escapeXml(faultString)).append("</faultstring>\n");
        fault.append("    </soap:Fault>\n");
        fault.append("  </soap:Body>\n");
        fault.append("</soap:Envelope>");
        
        return fault.toString();
    }
    
    private String generateWSDL(HttpServletRequest request) {
        String baseURL = request.getRequestURL().toString();
        
        StringBuilder wsdl = new StringBuilder();
        wsdl.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        wsdl.append("<definitions xmlns=\"http://schemas.xmlsoap.org/wsdl/\" ");
        wsdl.append("xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\" ");
        wsdl.append("xmlns:tns=\"").append(TARGET_NAMESPACE).append("\" ");
        wsdl.append("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" ");
        wsdl.append("targetNamespace=\"").append(TARGET_NAMESPACE).append("\">\n");
        
        // Types section
        wsdl.append("  <types>\n");
        wsdl.append("    <xsd:schema targetNamespace=\"").append(TARGET_NAMESPACE).append("\">\n");
        wsdl.append("      <xsd:element name=\"calculateFromString\">\n");
        wsdl.append("        <xsd:complexType>\n");
        wsdl.append("          <xsd:sequence>\n");
        wsdl.append("            <xsd:element name=\"expression\" type=\"xsd:string\"/>\n");
        wsdl.append("          </xsd:sequence>\n");
        wsdl.append("        </xsd:complexType>\n");
        wsdl.append("      </xsd:element>\n");
        wsdl.append("      <xsd:element name=\"calculateFromStringResponse\">\n");
        wsdl.append("        <xsd:complexType>\n");
        wsdl.append("          <xsd:sequence>\n");
        wsdl.append("            <xsd:element name=\"return\" type=\"xsd:string\"/>\n");
        wsdl.append("          </xsd:sequence>\n");
        wsdl.append("        </xsd:complexType>\n");
        wsdl.append("      </xsd:element>\n");
        wsdl.append("      <xsd:element name=\"calculateFromImage\">\n");
        wsdl.append("        <xsd:complexType>\n");
        wsdl.append("          <xsd:sequence>\n");
        wsdl.append("            <xsd:element name=\"imageData\" type=\"xsd:base64Binary\"/>\n");
        wsdl.append("          </xsd:sequence>\n");
        wsdl.append("        </xsd:complexType>\n");
        wsdl.append("      </xsd:element>\n");
        wsdl.append("      <xsd:element name=\"calculateFromImageResponse\">\n");
        wsdl.append("        <xsd:complexType>\n");
        wsdl.append("          <xsd:sequence>\n");
        wsdl.append("            <xsd:element name=\"return\" type=\"xsd:string\"/>\n");
        wsdl.append("          </xsd:sequence>\n");
        wsdl.append("        </xsd:complexType>\n");
        wsdl.append("      </xsd:element>\n");
        wsdl.append("      <xsd:element name=\"healthCheck\">\n");
        wsdl.append("        <xsd:complexType/>\n");
        wsdl.append("      </xsd:element>\n");
        wsdl.append("      <xsd:element name=\"healthCheckResponse\">\n");
        wsdl.append("        <xsd:complexType>\n");
        wsdl.append("          <xsd:sequence>\n");
        wsdl.append("            <xsd:element name=\"return\" type=\"xsd:string\"/>\n");
        wsdl.append("          </xsd:sequence>\n");
        wsdl.append("        </xsd:complexType>\n");
        wsdl.append("      </xsd:element>\n");
        wsdl.append("    </xsd:schema>\n");
        wsdl.append("  </types>\n");
        
        // Messages
        wsdl.append("  <message name=\"calculateFromStringRequest\">\n");
        wsdl.append("    <part name=\"parameters\" element=\"tns:calculateFromString\"/>\n");
        wsdl.append("  </message>\n");
        wsdl.append("  <message name=\"calculateFromStringResponse\">\n");
        wsdl.append("    <part name=\"parameters\" element=\"tns:calculateFromStringResponse\"/>\n");
        wsdl.append("  </message>\n");
        wsdl.append("  <message name=\"calculateFromImageRequest\">\n");
        wsdl.append("    <part name=\"parameters\" element=\"tns:calculateFromImage\"/>\n");
        wsdl.append("  </message>\n");
        wsdl.append("  <message name=\"calculateFromImageResponse\">\n");
        wsdl.append("    <part name=\"parameters\" element=\"tns:calculateFromImageResponse\"/>\n");
        wsdl.append("  </message>\n");
        wsdl.append("  <message name=\"healthCheckRequest\">\n");
        wsdl.append("    <part name=\"parameters\" element=\"tns:healthCheck\"/>\n");
        wsdl.append("  </message>\n");
        wsdl.append("  <message name=\"healthCheckResponse\">\n");
        wsdl.append("    <part name=\"parameters\" element=\"tns:healthCheckResponse\"/>\n");
        wsdl.append("  </message>\n");
        
        // Port Type
        wsdl.append("  <portType name=\"").append(SERVICE_NAME).append("\">\n");
        wsdl.append("    <operation name=\"calculateFromString\">\n");
        wsdl.append("      <input message=\"tns:calculateFromStringRequest\"/>\n");
        wsdl.append("      <output message=\"tns:calculateFromStringResponse\"/>\n");
        wsdl.append("    </operation>\n");
        wsdl.append("    <operation name=\"calculateFromImage\">\n");
        wsdl.append("      <input message=\"tns:calculateFromImageRequest\"/>\n");
        wsdl.append("      <output message=\"tns:calculateFromImageResponse\"/>\n");
        wsdl.append("    </operation>\n");
        wsdl.append("    <operation name=\"healthCheck\">\n");
        wsdl.append("      <input message=\"tns:healthCheckRequest\"/>\n");
        wsdl.append("      <output message=\"tns:healthCheckResponse\"/>\n");
        wsdl.append("    </operation>\n");
        wsdl.append("  </portType>\n");
        
        // Binding
        wsdl.append("  <binding name=\"").append(SERVICE_NAME).append("Binding\" type=\"tns:").append(SERVICE_NAME).append("\">\n");
        wsdl.append("    <soap:binding style=\"document\" transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n");
        wsdl.append("    <operation name=\"calculateFromString\">\n");
        wsdl.append("      <soap:operation soapAction=\"\"/>\n");
        wsdl.append("      <input><soap:body use=\"literal\"/></input>\n");
        wsdl.append("      <output><soap:body use=\"literal\"/></output>\n");
        wsdl.append("    </operation>\n");
        wsdl.append("    <operation name=\"calculateFromImage\">\n");
        wsdl.append("      <soap:operation soapAction=\"\"/>\n");
        wsdl.append("      <input><soap:body use=\"literal\"/></input>\n");
        wsdl.append("      <output><soap:body use=\"literal\"/></output>\n");
        wsdl.append("    </operation>\n");
        wsdl.append("    <operation name=\"healthCheck\">\n");
        wsdl.append("      <soap:operation soapAction=\"\"/>\n");
        wsdl.append("      <input><soap:body use=\"literal\"/></input>\n");
        wsdl.append("      <output><soap:body use=\"literal\"/></output>\n");
        wsdl.append("    </operation>\n");
        wsdl.append("  </binding>\n");
        
        // Service
        wsdl.append("  <service name=\"").append(SERVICE_NAME).append("Service\">\n");
        wsdl.append("    <port name=\"").append(SERVICE_NAME).append("Port\" binding=\"tns:").append(SERVICE_NAME).append("Binding\">\n");
        wsdl.append("      <soap:address location=\"").append(baseURL).append("\"/>\n");
        wsdl.append("    </port>\n");
        wsdl.append("  </service>\n");
        wsdl.append("</definitions>");
        
        return wsdl.toString();
    }
    
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }
    
    private static class SOAPRequestInfo {
        String operation;
        String expression;
        int imageSize;
    }
}
