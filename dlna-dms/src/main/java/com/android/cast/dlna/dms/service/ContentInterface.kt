package com.android.cast.dlna.dms.service

import org.jupnp.support.model.BrowseFlag
import org.jupnp.support.model.BrowseResult

interface ContentControl {
    fun browse(objectID: String, browseFlag: BrowseFlag, filter: String? = null, firstResult: Long = 0, maxResults: Long = 9999): BrowseResult
    fun search(containerId: String, searchCriteria: String, filter: String? = null, firstResult: Long = 0, maxResults: Long = 9999): BrowseResult
}