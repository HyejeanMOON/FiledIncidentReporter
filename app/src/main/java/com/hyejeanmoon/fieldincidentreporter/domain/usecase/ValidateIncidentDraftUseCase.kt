package com.hyejeanmoon.fieldincidentreporter.domain.usecase

import com.hyejeanmoon.fieldincidentreporter.domain.model.IncidentDraft
import com.hyejeanmoon.fieldincidentreporter.domain.model.MAX_INCIDENT_PHOTOS
import com.hyejeanmoon.fieldincidentreporter.domain.model.RequiredIncidentField
import javax.inject.Inject

class ValidateIncidentDraftUseCase @Inject constructor() {
    operator fun invoke(draft: IncidentDraft): List<RequiredIncidentField> = buildList {
        if (!draft.title.fits(RequiredIncidentField.TITLE)) add(RequiredIncidentField.TITLE)
        if (!draft.location.fits(RequiredIncidentField.LOCATION)) add(RequiredIncidentField.LOCATION)
        if (!draft.description.fits(RequiredIncidentField.DESCRIPTION)) add(RequiredIncidentField.DESCRIPTION)
        if (draft.photos.size !in 1..MAX_INCIDENT_PHOTOS) add(RequiredIncidentField.PHOTOS)
    }
}

/** Like the server, checks the trimmed text: it must not be blank or longer than the field allows. */
private fun String.fits(field: RequiredIncidentField) = trim().length in 1..checkNotNull(field.maxLength)
