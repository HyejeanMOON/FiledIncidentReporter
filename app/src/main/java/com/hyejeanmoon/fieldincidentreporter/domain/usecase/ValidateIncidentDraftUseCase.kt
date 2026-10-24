package com.hyejeanmoon.fieldincidentreporter.domain.usecase

import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentDraft
import com.hyejeanmoon.fieldincidentreporter.domain.model.MAX_INCIDENT_PHOTOS
import com.hyejeanmoon.fieldincidentreporter.domain.model.RequiredIncidentField
import javax.inject.Inject

class ValidateIncidentDraftUseCase @Inject constructor() {
    operator fun invoke(draft: IncidentDraft): List<RequiredIncidentField> = buildList {
        if (draft.title.isBlank()) add(RequiredIncidentField.TITLE)
        if (draft.location.isBlank()) add(RequiredIncidentField.LOCATION)
        if (draft.description.isBlank()) add(RequiredIncidentField.DESCRIPTION)
        if (draft.photos.size !in 1..MAX_INCIDENT_PHOTOS) add(RequiredIncidentField.PHOTOS)
    }
}
