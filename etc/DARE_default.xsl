<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:oai="http://www.openarchives.org/OAI/2.0/"
    xmlns:dcx="http://krait.kb.nl/coop/tel/handbook/telterms.html"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:dcterms="http://purl.org/dc/terms/"
    xmlns:mods="http://www.loc.gov/mods"
    xmlns:srw_dc="info:srw/schema/1/dc-v1.1"
    xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
    xmlns:dc="http://purl.org/dc/elements/1.1/">

<xsl:variable name="lcletters">abcdefghijklmnopqrstuvwxyz</xsl:variable>
<xsl:variable name="ucletters">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>

<xsl:variable name="oai_setname">DARE</xsl:variable>


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
     <xsl:text disable-output-escaping="yes">&lt;doc&gt;</xsl:text>
        <xsl:apply-templates select="oai:header"/>
        <xsl:apply-templates select="oai:metadata"/>
     <xsl:text disable-output-escaping="yes">&lt;/doc&gt;</xsl:text>
    </xsl:template>

    <xsl:template match="oai:header">
        <xsl:for-each select="*">
          <xsl:variable name="field_name" select="name(.)"/>

          <xsl:variable name="field_value" select="."/>

          <xsl:text disable-output-escaping="yes">&lt;field name="</xsl:text>
          <xsl:value-of select="$field_name"/>
          <xsl:text disable-output-escaping="yes">"&gt;</xsl:text>
          <xsl:value-of select="$field_value"/>
          <xsl:text disable-output-escaping="yes">&lt;/field&gt;</xsl:text>

          <xsl:choose>
            <xsl:when test="contains($field_name, 'date')">
              <xsl:text disable-output-escaping="yes">&lt;field name="</xsl:text>
              <xsl:value-of select="$field_name"/>_date<xsl:text disable-output-escaping="yes">"&gt;</xsl:text>
              <xsl:value-of select="$field_value"/>T00:00:00Z<xsl:text disable-output-escaping="yes">&lt;/field&gt;</xsl:text>
            </xsl:when>

            <xsl:when test="contains($field_name, 'identifier')">
              <xsl:text disable-output-escaping="yes">&lt;field name="id"&gt;</xsl:text>
              <xsl:value-of select="$field_value"/>
              <xsl:text disable-output-escaping="yes">&lt;/field&gt;</xsl:text>
            </xsl:when>

            <xsl:otherwise>
              <xsl:text disable-output-escaping="yes">&lt;field name="</xsl:text>
              <xsl:value-of select="$field_name"/><xsl:text disable-output-escaping="yes">_str"&gt;</xsl:text>
              <xsl:value-of select="$field_value"/>
              <xsl:text disable-output-escaping="yes">&lt;/field&gt;</xsl:text>
            </xsl:otherwise>
          </xsl:choose>

        </xsl:for-each>
        <xsl:text disable-output-escaping="yes">&lt;field name="</xsl:text>oai_setname_str<xsl:text disable-output-escaping="yes">"&gt;</xsl:text><xsl:value-of select="$oai_setname"/><xsl:text disable-output-escaping="yes">&lt;/field&gt;</xsl:text>
    </xsl:template>

    <xsl:template match="oai:metadata">
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="srw_dc:dc">
        <xsl:for-each select="*">
          <xsl:variable name="field_name" select="translate(substring-after(name(.), ':'), $ucletters, $lcletters)"/>
          <xsl:variable name="field_value" select="."/>

          <xsl:text disable-output-escaping="yes">&lt;field name="</xsl:text>
          <xsl:value-of select="$field_name"/>
          <xsl:text disable-output-escaping="yes">_str"&gt;</xsl:text>
          <xsl:value-of select="$field_value"/>
          <xsl:text disable-output-escaping="yes">&lt;/field&gt;</xsl:text>

 
          <xsl:text disable-output-escaping="yes">&lt;field name="</xsl:text>
          <xsl:value-of select="$field_name"/>
          <xsl:text disable-output-escaping="yes">"&gt;</xsl:text>
          <xsl:value-of select="$field_value"/>
          <xsl:text disable-output-escaping="yes">&lt;/field&gt;</xsl:text>

        </xsl:for-each>
    </xsl:template>

    <xsl:template match="*">
        <xsl:for-each select="*">
          <xsl:variable name="field_name" select="translate(name(.), $ucletters, $lcletters)"/>
          <xsl:variable name="field_value" select="."/>


          <xsl:text disable-output-escaping="yes">&lt;field name="</xsl:text>
          <xsl:value-of select="$field_name"/><xsl:text disable-output-escaping="yes">"&gt;</xsl:text>
          <xsl:value-of select="$field_value"/>
          <xsl:text disable-output-escaping="yes">&lt;/field&gt;</xsl:text>

          <xsl:choose>
            <xsl:when test="contains($field_name, 'date')">
              <xsl:choose>
                <xsl:when test="string-length($field_value) = 4 and number($field_value) != 'NaN'">
                    <xsl:text disable-output-escaping="yes">&lt;field name="</xsl:text>
                    <xsl:value-of select="$field_name"/><xsl:text>_date</xsl:text>
                    <xsl:text disable-output-escaping="yes">"&gt;</xsl:text>
                    <xsl:value-of select="$field_value"/><xsl:text>-01-01T00:00:00Z</xsl:text>
                    <xsl:text disable-output-escaping="yes">&lt;/field&gt;</xsl:text>
                </xsl:when>
              </xsl:choose>

            </xsl:when>
            <xsl:otherwise>
              <xsl:text disable-output-escaping="yes">&lt;field name="</xsl:text>
              <xsl:value-of select="$field_name"/>_str<xsl:text disable-output-escaping="yes">"&gt;</xsl:text>
              <xsl:value-of select="$field_value"/>
              <xsl:text disable-output-escaping="yes">&lt;/field&gt;</xsl:text>
            </xsl:otherwise>
          </xsl:choose>

        </xsl:for-each>
    </xsl:template>

</xsl:stylesheet>
