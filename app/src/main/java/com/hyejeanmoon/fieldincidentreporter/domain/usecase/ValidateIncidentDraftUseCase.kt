package com.hyejeanmoon.fieldincidentreporter.domain.usecase

import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentDraft
import com.hyejeanmoon.fieldincidentreporter.domain.model.RequiredIncidentField
import javax.inject.Inject

class ValidateIncidentDraftUseCase @Inject constructor() {
    operator fun invoke(draft: IncidentDraft): List<RequiredIncidentField> = buildList {
        if (draft.title.isBlank()) add(RequiredIncidentField.TITLE)
        if (draft.location.isBlank()) add(RequiredIncidentField.LOCATION)
        if (draft.description.isBlank()) add(RequiredIncidentField.DESCRIPTION)
        if (draft.photoUris.size !in 1..20 || draft.photoUris.any { it.isBlank() }) add(RequiredIncidentField.PHOTOS)
    }
}
