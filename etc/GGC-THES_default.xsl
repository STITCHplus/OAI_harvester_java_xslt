<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:oai="http://www.openarchives.org/OAI/2.0/"
    xmlns:srw_dc="info:srw/schema/1/dc-schema"
    xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:skos="http://www.w3.org/2004/02/skos/core#">

<xsl:variable name="lcletters">abcdefghijklmnopqrstuvwxyz</xsl:variable>
<xsl:variable name="ucletters">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>


<xsl:output method="xml" omit-xml-declaration="yes" standalone="yes" indent="yes"/>

    <xsl:template match="/oai:OAI-PMH">
     <xsl:text disable-output-escaping="yes"> &lt;add&gt;</xsl:text>
        <xsl:apply-templates select="oai:ListRecords"/>
     <xsl:text disable-output-escaping="yes"> &lt;/add&gt;</xsl:text>
    </xsl:template>

    <xsl:template match="oai:ListRecords">
        <xsl:apply-templates select="oai:record"/>
    </xsl:template>

    <xsl:template match="oai:record">
     <xsl:text disable-output-escaping="yes"> &lt;doc&gt;</xsl:text>
        <xsl:apply-templates select="oai:header"/>
        <xsl:apply-templates select="oai:metadata"/>
     <xsl:text disable-output-escaping="yes"> &lt;/doc&gt;</xsl:text>
    </xsl:template>

    <xsl:template match="oai:header">


        <xsl:for-each select="*">
          <xsl:choose>
            <xsl:when test="contains(name(.), 'identifier')">
                  <xsl:text disable-output-escaping="yes">&lt;field name="</xsl:text>
                  <xsl:text>id</xsl:text>
                  <xsl:text disable-output-escaping="yes">"&gt;</xsl:text>
                  <xsl:value-of select="."/>
                  <xsl:text disable-output-escaping="yes">&lt;/field&gt;</xsl:text>

                  <xsl:text disable-output-escaping="yes">&lt;field name="</xsl:text>
                  <xsl:text>fullrecord</xsl:text>
                  <xsl:text disable-output-escaping="yes">"&gt;</xsl:text>
                  <xsl:value-of select="."/>
                  <xsl:text disable-output-escaping="yes">&lt;/field&gt;</xsl:text>


            </xsl:when>
            <xsl:otherwise>
                  <xsl:text disable-output-escaping="yes">&lt;field name="</xsl:text>
                  <xsl:value-of select="name(.)"/>
                  <xsl:text disable-output-escaping="yes">"&gt;</xsl:text>
                  <xsl:value-of select="."/>
                  <xsl:text disable-output-escaping="yes">&lt;/field&gt;</xsl:text>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="oai:metadata">
        <xsl:apply-templates select="rdf:RDF"/>
    </xsl:template>

    <xsl:template match="rdf:RDF">
        <xsl:apply-templates select="skos:Concept"/>
    </xsl:template>

    <xsl:template match="skos:Concept">
        <xsl:for-each select="*">
         <xsl:choose>
             <xsl:when test="contains(name(.), 'inScheme')">
                
             </xsl:when>
             <xsl:otherwise>
                <xsl:text disable-output-escaping="yes">&lt;field name="</xsl:text>
                    <xsl:value-of select="substring-after(name(.), ':')"/>
                <xsl:text disable-output-escaping="yes">_str"&gt;</xsl:text>
                <xsl:value-of select="."/>
                <xsl:text disable-output-escaping="yes">&lt;/field&gt;</xsl:text>

             </xsl:otherwise>
         </xsl:choose>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>
