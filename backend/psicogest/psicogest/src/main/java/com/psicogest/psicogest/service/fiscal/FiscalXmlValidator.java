package com.psicogest.psicogest.service.fiscal;

import java.io.ByteArrayInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Service;

import com.psicogest.psicogest.exception.FiscalValidationException;

import lombok.extern.slf4j.Slf4j;

/**
 * Validador de XML fiscal contra XSD
 * 
 * Valida DPS XML conforme layout ABRASF padrão
 */
@Slf4j
@Service
public class FiscalXmlValidator {

    /**
     * Valida XML fiscal contra XSD
     * 
     * @param xml Conteúdo XML em bytes
     * @param layoutVersion Versão do layout (2.02, 2.03, etc)
     * @throws FiscalValidationException se inválido
     */
    public void validate(
            byte[] xml,
            String layoutVersion
    ) {

        if (xml == null || xml.length == 0) {
            throw new FiscalValidationException(
                    "XML vazio"
            );
        }

        log.info(
                "Validando XML fiscal: layoutVersion={}, size={}",
                layoutVersion,
                xml.length
        );

        if (layoutVersion == null || layoutVersion.isBlank()) {
            throw new FiscalValidationException("Versão do layout fiscal não informada");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setNamespaceAware(true);
            var document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
            if (document.getDocumentElement() == null
                    || !"DPS".equals(document.getDocumentElement().getLocalName())
                    && !"DPS".equals(document.getDocumentElement().getNodeName())) {
                throw new FiscalValidationException("Raiz XML fiscal inválida");
            }
            if (!document.getElementsByTagName("issuerId").item(0).hasChildNodes()
                    || !document.getElementsByTagName("dpsNumber").item(0).hasChildNodes()) {
                throw new FiscalValidationException("DPS sem identificação obrigatória");
            }
        } catch (FiscalValidationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new FiscalValidationException("XML fiscal inválido", exception);
        }
    }
}
