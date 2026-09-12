package com.psicogest.psicogest.service.privacy;

import com.psicogest.psicogest.domain.privacy.DataSubjectContext;
import com.psicogest.psicogest.domain.privacy.DataSubjectExportSection;

public interface DataSubjectExportContributor {

    String domain();

    DataSubjectExportSection collect(
            DataSubjectContext subject
    );
}
