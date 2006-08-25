/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/relations/Attic/CmsRelationsValidator.java,v $
 * Date   : $Date: 2006/08/25 08:13:11 $
 * Version: $Revision: 1.1.2.3 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (c) 2005 Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.relations;

import org.opencms.db.CmsDbContext;
import org.opencms.db.CmsDriverManager;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.CmsVfsResourceNotFoundException;
import org.opencms.file.types.I_CmsResourceType;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.report.I_CmsReport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

/**
 * Validates relations of resources in the OpenCms VFS.<p>
 *  
 * Relations are, for instance, href attribs in anchor tags and src attribs in 
 * image tags, as well as OpenCmsVfsFileReference values in Xml Content.<p>
 * 
 * External links to targets outside the OpenCms VFS don't get validated.<p>
 * 
 * Objects using this class are responsible to handle detected broken links.<p>
 * 
 * @author Thomas Weckert
 * @author Michael Moossen
 *   
 * @version $Revision: 1.1.2.3 $ 
 * 
 * @since 6.3.0 
 */
public class CmsRelationsValidator {

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsRelationsValidator.class);

    /** The driver manager. */
    protected CmsDriverManager m_driverManager;

    /**
     * Default constructor.<p>
     * 
     * @param driverManager The Cms driver manager
     */
    public CmsRelationsValidator(CmsDriverManager driverManager) {

        m_driverManager = driverManager;
    }

    /**
     * Validates the relations.<p>
     * 
     * The result is printed to the given report.<p>
     * 
     * If the resources list is <code>null</code> or empty, the whole current project will be validated.<p>
     * 
     * If the resources list is not empty, Validating references means to answer the question, whether 
     * we would have broken links in the online project if a file or a list of files would get published.<p>
     * 
     * @param cms the current user's cms context
     * @param resources a list of offline resources, or <code>null</code>
     * @param report a report to print messages
     * 
     * @return a map with lists of invalid links keyed by resource names
     */
    public Map validateResources(CmsObject cms, List resources, I_CmsReport report) {

        boolean interProject = (resources != null);
        report.println(Messages.get().container(Messages.RPT_HTMLLINK_VALIDATOR_BEGIN_0), I_CmsReport.FORMAT_HEADLINE);
        if (resources == null) {
            resources = new ArrayList();
            CmsResourceFilter filter = CmsResourceFilter.IGNORE_EXPIRATION;
            Iterator itTypes = OpenCms.getResourceManager().getResourceTypes().iterator();
            while (itTypes.hasNext()) {
                I_CmsResourceType type = (I_CmsResourceType)itTypes.next();
                if (type instanceof I_CmsLinkParseable) {
                    filter = filter.addRequireType(type.getTypeId());
                    try {
                        resources.addAll(cms.readResources("/", filter, true));
                    } catch (CmsException e) {
                        LOG.error(
                            Messages.get().getBundle().key(Messages.LOG_RETRIEVAL_RESOURCES_1, type.getTypeName()),
                            e);
                    }
                }
            }
        }

        // populate a lookup map with the project resources that 
        // actually get published keyed by their resource names.
        // second, resources that don't get validated are ignored.
        Map offlineFilesLookup = new HashMap();
        List validatableResources = new ArrayList();
        Iterator itResources = resources.iterator();
        while (itResources.hasNext()) {
            CmsResource resource = (CmsResource)itResources.next();
            offlineFilesLookup.put(resource.getRootPath(), resource);
            try {
                I_CmsResourceType resourceType = OpenCms.getResourceManager().getResourceType(resource.getTypeId());
                if ((resourceType instanceof I_CmsLinkParseable) && (resource.getState() != CmsResource.STATE_DELETED)) {
                    // don't validate links on deleted resources
                    validatableResources.add(resource);
                }
            } catch (CmsException e) {
                LOG.error(
                    Messages.get().getBundle().key(Messages.LOG_RETRIEVAL_RESOURCETYPE_1, resource.getRootPath()),
                    e);
            }
        }

        Map invalidResources = new HashMap();
        boolean foundBrokenLinks = false;
        for (int index = 0, size = validatableResources.size(); index < size; index++) {
            CmsResource resource = (CmsResource)validatableResources.get(index);
            String resourceName = resource.getRootPath();

            report.print(org.opencms.report.Messages.get().container(
                org.opencms.report.Messages.RPT_SUCCESSION_2,
                new Integer(index + 1),
                new Integer(size)), I_CmsReport.FORMAT_NOTE);
            report.print(Messages.get().container(Messages.RPT_HTMLLINK_VALIDATING_0), I_CmsReport.FORMAT_NOTE);
            report.print(org.opencms.report.Messages.get().container(
                org.opencms.report.Messages.RPT_ARGUMENT_1,
                cms.getRequestContext().removeSiteRoot(resourceName)));
            report.print(org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_DOTS_0));
            List relations = null;
            try {
                relations = cms.getRelationsForResource(
                    cms.getRequestContext().removeSiteRoot(resourceName),
                    CmsRelationFilter.TARGETS);
            } catch (CmsException e) {
                LOG.error(Messages.get().getBundle().key(Messages.LOG_LINK_SEARCH_1, resourceName), e);
                report.println(
                    Messages.get().container(Messages.LOG_LINK_SEARCH_1, resourceName),
                    I_CmsReport.FORMAT_ERROR);
                continue;
            }
            int projectId = CmsProject.ONLINE_PROJECT_ID;
            if (!interProject) {
                projectId = cms.getRequestContext().currentProject().getId();
            }
            List brokenLinks = validateLinks(relations, offlineFilesLookup, projectId);
            if (brokenLinks.size() > 0) {
                // the resource contains broken links
                invalidResources.put(resourceName, brokenLinks);
                foundBrokenLinks = true;
                report.println(
                    Messages.get().container(Messages.RPT_HTMLLINK_FOUND_BROKEN_LINKS_0),
                    I_CmsReport.FORMAT_WARNING);
            } else {
                // the resource contains *NO* broken links
                report.println(
                    org.opencms.report.Messages.get().container(org.opencms.report.Messages.RPT_OK_0),
                    I_CmsReport.FORMAT_OK);
            }
        }

        if (foundBrokenLinks) {
            // print a summary if we found broken links in the validated resources
            report.println(
                Messages.get().container(Messages.RPT_BROKEN_LINKS_SUMMARY_BEGIN_0),
                I_CmsReport.FORMAT_HEADLINE);
            Iterator itInvalidResources = invalidResources.entrySet().iterator();
            while (itInvalidResources.hasNext()) {
                Map.Entry entry =(Map.Entry)itInvalidResources.next();
                String resourceName = (String)entry.getKey();
                List brokenLinks = (List)entry.getValue();
                report.println(
                    Messages.get().container(Messages.RPT_BROKEN_LINKS_IN_1, resourceName),
                    I_CmsReport.FORMAT_NOTE);
                Iterator itBrokenLinks = brokenLinks.iterator();
                while (itBrokenLinks.hasNext()) {
                    String brokenLink = (String)itBrokenLinks.next();
                    report.print(org.opencms.report.Messages.get().container(
                        org.opencms.report.Messages.RPT_ARGUMENT_1,
                        brokenLink), I_CmsReport.FORMAT_WARNING);
                }
                report.println();
            }
            report.println(
                Messages.get().container(Messages.RPT_BROKEN_LINKS_SUMMARY_END_0),
                I_CmsReport.FORMAT_HEADLINE);
            report.println(Messages.get().container(Messages.RPT_HTMLLINK_VALIDATOR_ERROR_0), I_CmsReport.FORMAT_ERROR);
        }
        report.println(Messages.get().container(Messages.RPT_HTMLLINK_VALIDATOR_END_0), I_CmsReport.FORMAT_HEADLINE);
        return invalidResources;
    }

    /**
     * Validates the URIs in the specified link list.<p>
     * 
     * @param relations the list of {@link CmsRelation} objects for a given resource
     * @param fileLookup a map for faster lookup with all resources keyed by their rootpath
     * @param projectId the project to validate
     * 
     * @return a list with the broken links in the specified link list, or an empty list if no broken links were found
     */
    protected List validateLinks(List relations, Map fileLookup, int projectId) {

        List brokenLinks = new ArrayList();
        List validatedLinks = new ArrayList();

        Iterator itRelations = relations.iterator();
        while (itRelations.hasNext()) {
            CmsRelation relation = (CmsRelation)itRelations.next();
            String link = relation.getTargetPath();
            boolean isValidLink = true;
            if (validatedLinks.contains(link) || "".equals(link)) {
                // skip links that are already validated or empty
                continue;
            }
            // the link is valid...
            try {
                // ... if the linked resource exists in the online project
                try {
                    link = m_driverManager.getVfsDriver().readResource(
                        new CmsDbContext(),
                        projectId,
                        relation.getTargetId(),
                        true).getRootPath();
                } catch (CmsVfsResourceNotFoundException e) {
                    m_driverManager.getVfsDriver().readResource(
                        new CmsDbContext(),
                        projectId,
                        relation.getTargetPath(),
                        true);
                }
                // ... and if the linked resource in the online project won't get deleted if it gets actually published
                if (fileLookup.containsKey(link)) {
                    CmsResource resource = (CmsResource)fileLookup.get(link);
                    if (resource.getState() == CmsResource.STATE_DELETED) {
                        isValidLink = false;
                    }
                }
            } catch (CmsException e) {
                // ... or if the linked resource is a resource that gets actually published
                if (!fileLookup.containsKey(link)) {
                    isValidLink = false;
                }
            }
            if (!isValidLink) {
                brokenLinks.add(link);
            }
            validatedLinks.add(link);
        }
        return brokenLinks;
    }
}