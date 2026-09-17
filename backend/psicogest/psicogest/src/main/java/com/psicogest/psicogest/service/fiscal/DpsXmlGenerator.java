package com.psicogest.psicogest.service.fiscal;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.stereotype.Service;
import com.psicogest.psicogest.exception.FiscalValidationException;

import lombok.extern.slf4j.Slf4j;

/**
 * Gerador de XML para DPS (Documento de Proposta de Serviço)
 * 
 * Gera XML conforme layout ABRASF/RPS padrão
 * Versões suportadas: 2.02, 2.03, etc
 */
@Slf4j
@Service
public class DpsXmlGenerator {

    /**
     * Gera DPS XML a partir de comando de emissão
     * 
     * @param command Comando com dados da NFS-e
     * @return XML em bytes
     */
    public byte[] generate(FiscalIssueCommand command) {
        if (command == null || command.issuerId() == null || command.invoiceId() == null
                || command.dpsNumber() <= 0 || command.competenceDate() == null
                || command.layoutVersion() == null || command.layoutVersion().isBlank()) {
            throw new FiscalValidationException("Dados mínimos da DPS não informados");
        }
        log.info(
                "Gerando DPS XML: dpsNumber={}, layoutVersion={}",
                command.dpsNumber(),
                command.layoutVersion()
        );
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream(2048);
            XMLStreamWriter writer = XMLOutputFactory.newFactory()
                    .createXMLStreamWriter(output, StandardCharsets.UTF_8.name());
            writer.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
            writer.writeStartElement("DPS");
            writer.writeDefaultNamespace("http://www.sped.fazenda.gov.br/nfse");
            writer.writeAttribute("versao", command.layoutVersion());
            writer.writeStartElement("infDPS");
            writer.writeAttribute("Id", "DPS" + command.invoiceId());
            element(writer, "dpsNumber", Long.toString(command.dpsNumber()));
            element(writer, "competenceDate", command.competenceDate().toString());
            element(writer, "issuerId", command.issuerId().toString());
            element(writer, "invoiceId", command.invoiceId().toString());
            writer.writeStartElement("values");
            element(writer, "grossAmount", decimal(command.grossAmount()));
            element(writer, "deductions", decimal(command.deductions()));
            element(writer, "netAmount", decimal(command.netAmount()));
            writer.writeEndElement();
            element(writer, "serviceDescription", command.serviceDescription());
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new FiscalValidationException("Não foi possível gerar a DPS", exception);
        }
    }

    private void element(XMLStreamWriter writer, String name, String value) throws Exception {
        writer.writeStartElement(name);
        writer.writeCharacters(value == null ? "" : value);
        writer.writeEndElement();
    }

    private String decimal(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
