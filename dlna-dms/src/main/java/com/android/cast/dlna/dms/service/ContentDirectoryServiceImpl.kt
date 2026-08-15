package com.android.cast.dlna.dms.service

import org.jupnp.support.contentdirectory.AbstractContentDirectoryService
import org.jupnp.support.model.BrowseFlag
import org.jupnp.support.model.BrowseResult
import org.jupnp.support.model.SortCriterion

internal class ContentDirectoryServiceImpl(private val control: ContentControl) : AbstractContentDirectoryService() {
    override fun browse(objectID: String, browseFlag: BrowseFlag, filter: String, firstResult: Long, maxResults: Long, orderBy: Array<SortCriterion>): BrowseResult =
        control.browse(objectID, browseFlag, filter, firstResult, maxResults)
    override fun search(containerId: String, searchCriteria: String, filter: String, firstResult: Long, maxResults: Long, orderBy: Array<SortCriterion>): BrowseResult =
        control.search(containerId, searchCriteria, filter, firstResult, maxResults)
}