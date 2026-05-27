package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

import java.util.List;

import org.openmrs.module.ihmodule.api.patientexchange.api.dto.MpiDuplicateReviewCandidatesResponse;
import org.openmrs.module.ihmodule.api.patientexchange.api.dto.MpiDuplicateReviewCaseSummaryDto;
import org.openmrs.module.ihmodule.api.patientexchange.api.dto.MpiDuplicateReviewStatisticsDto;

/**
 * Read-only MPI duplicate-review queries. Implemented by {@link MpiDuplicateReviewQueryServiceImpl}.
 */
public interface MpiDuplicateReviewQueryService {
	
	List<MpiDuplicateReviewCaseSummaryDto> listPendingCases();
	
	MpiDuplicateReviewStatisticsDto getCaseStatistics();
	
	MpiDuplicateReviewCandidatesResponse listCandidatesForCaseUuid(String caseUuid);
}
