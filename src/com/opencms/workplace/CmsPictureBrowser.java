/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/workplace/Attic/CmsPictureBrowser.java,v $
 * Date   : $Date: 2000/04/20 08:11:55 $
 * Version: $Revision: 1.15 $
 *
 * Copyright (C) 2000  The OpenCms Group 
 * 
 * This File is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * For further information about OpenCms, please see the
 * OpenCms Website: http://www.opencms.com
 * 
 * You should have received a copy of the GNU General Public License
 * long with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.opencms.workplace;

import com.opencms.file.*;
import com.opencms.core.*;
import com.opencms.util.*;
import com.opencms.template.*;

import java.util.*;

import javax.servlet.http.*;

/**
 * Template class for displaying OpenCms picture browser.
 * <P>
 * Reads template files of the content type <code>CmsXmlWpTemplateFile</code>.
 * 
 * @author Alexander Lucas
 * @version $Revision: 1.15 $ $Date: 2000/04/20 08:11:55 $
 * @see com.opencms.workplace.CmsXmlWpTemplateFile
 */
public class CmsPictureBrowser extends CmsWorkplaceDefault {

    /**
     * Indicates if the results of this class are cacheable.
     * 
     * @param cms A_CmsObject Object for accessing system resources
     * @param templateFile Filename of the template file 
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     * @return <EM>true</EM> if cacheable, <EM>false</EM> otherwise.
     */
    public boolean isCacheable(A_CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
        return false;
    }    

    /**
     * Gets the content of a defined section in a given template file and its subtemplates
     * with the given parameters. 
     * 
     * @see getContent(A_CmsObject cms, String templateFile, String elementName, Hashtable parameters)
     * @param cms A_CmsObject Object for accessing system resources.
     * @param templateFile Filename of the template file.
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     */
    public byte[] getContent(A_CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
        if(C_DEBUG && A_OpenCms.isLogging()) {
            A_OpenCms.log(C_OPENCMS_DEBUG, getClassName() + "getting content of element " + ((elementName==null)?"<root>":elementName));
            A_OpenCms.log(C_OPENCMS_DEBUG, getClassName() + "template file is: " + templateFile);
            A_OpenCms.log(C_OPENCMS_DEBUG, getClassName() + "selected template section is: " + ((templateSelector==null)?"<default>":templateSelector));
        }

        String folder = (String)parameters.get(C_PARA_FOLDER);
        String pageText = (String)parameters.get(C_PARA_PAGE);
        String filter = (String)parameters.get(C_PARA_FILTER);
                
        // Check if the user requested a special folder
        if(folder == null || "".equals(folder)) {
            folder = getConfigFile(cms).getCommonPicturePath();
            parameters.put(C_PARA_FOLDER, folder);
        }

        // Check if the user requested a special page
        if(pageText == null ||"".equals(pageText))  {
            pageText = "1";
            parameters.put(C_PARA_PAGE, pageText);
        }
        
        // Check if the user requested a filter
        if(filter == null || "".equals(filter)) {
            filter = "*";
            parameters.put(C_PARA_FILTER, filter);
        }
        
        // Compute the maximum page number
        Vector filteredPics = getFilteredPicList(cms, folder, filter);
        int maxpage = ((filteredPics.size()-1)/C_PICBROWSER_MAXIMAGES)+1;
                        
        // Now load the template file and set the appropriate datablocks
        CmsXmlWpTemplateFile xmlTemplateDocument = (CmsXmlWpTemplateFile)getOwnTemplateFile(cms, templateFile, elementName, parameters, templateSelector);
        xmlTemplateDocument.setData(C_PARA_FOLDER, Encoder.escape(folder));
        xmlTemplateDocument.setData(C_PARA_PAGE, pageText);
        xmlTemplateDocument.setData(C_PARA_FILTER, filter);
        xmlTemplateDocument.setData(C_PARA_MAXPAGE, "" + maxpage);
        
        parameters.put("_PICLIST_", filteredPics);
        // Start the processing        
        return startProcessing(cms, xmlTemplateDocument, elementName, parameters, templateSelector);
    }                    


    /**
     * User method to generate an URL for the pics folder.
     * <P>
     * All pictures should reside in the docroot of the webserver for
     * performance reasons. This folder can be mounted into the OpenCms system to 
     * make it accessible for the OpenCms explorer.
     * <P>
     * The path to the docroot can be set in the workplace ini.
     * 
     * @param cms A_CmsObject Object for accessing system resources.
     * @param tagcontent Unused in this special case of a user method. Can be ignored.
     * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document <em>(not used here)</em>.  
     * @param userObj Hashtable with parameters <em>(not used here)</em>.
     * @return String with the pics URL.
     * @exception CmsException
     */    
    public Object pictureList(A_CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObj) 
            throws CmsException {

        Hashtable parameters = (Hashtable)userObj;
        CmsXmlWpTemplateFile xmlTemplateDocument = (CmsXmlWpTemplateFile)doc;

        StringBuffer result = new StringBuffer();

        String pageText = (String)parameters.get(C_PARA_PAGE);

        // Filter the pics
        Vector filteredPics = (Vector)parameters.get("_PICLIST_");
        int numPics = filteredPics.size();
        
        // Get limits for the requested page
        int page = new Integer(pageText).intValue();
        int from = (page-1) * C_PICBROWSER_MAXIMAGES;
        int to = ((from+C_PICBROWSER_MAXIMAGES)>numPics)?numPics:(from+C_PICBROWSER_MAXIMAGES);
        
        String picsUrl = getConfigFile(cms).getCommonPictureUrl();
        HttpServletRequest req = (HttpServletRequest)(cms.getRequestContext().getRequest().getOriginalRequest());
        String hostName = req.getScheme() + "://" + req.getHeader("HOST");
             
        // Generate the picture list for all pictures on the selected page
        for(int i=from; i<to; i++) {
            CmsFile file = (CmsFile)filteredPics.elementAt(i);
            String filename = file.getName();            
            String title = cms.readProperty(file.getAbsolutePath(), C_PROPERTY_TITLE);
            
            // If no "Title" property is given, the title will be set to the filename
            // without its postfix
            int dotIndex = filename.lastIndexOf(".");
            if(title == null) {                
                if(dotIndex > 0) {
                    title = filename.substring(0, dotIndex);
                } else {
                    title = filename;
                }                        
            }
                
            // The displayed type will be generated from the filename postfix
            String type;
            if(dotIndex > 0) {
                type = filename.substring(filename.lastIndexOf(".")+1).toUpperCase() + "-Bild";                
            } else {
                type = "unbekannt";
            }
                
            // Set all datablocks for the current picture list entry
            xmlTemplateDocument.setData("picsource", hostName + picsUrl + file.getName());
            xmlTemplateDocument.setData("title", title);                
            xmlTemplateDocument.setData("filename", filename);
            xmlTemplateDocument.setData("size", file.getLength() + " Byte");
            xmlTemplateDocument.setData("type", type);
            result.append(xmlTemplateDocument.getProcessedDataValue("piclistentry", this, userObj));
                
            // if this is not the last entry on the current page,
            // append a separator
            if(i<(to-1)) {
                result.append(xmlTemplateDocument.getProcessedDataValue("part", this, userObj));
            }
        }
        return result.toString();
    }

    /**
     * Used by the workplace "back" button to decide whether the icon should 
     * be activated or not. A button will use this method if the attribute <code>method="showBackButton"</code>
     * is defined in the <code>&lt;BUTTON&gt;</code> tag.
     * <P>
     * This method returns <code>false</code> if the currently displayed page is
     * the first page.
     * 
     * @param cms A_CmsObject Object for accessing system resources <em>(not used here)</em>.
     * @param lang reference to the currently valid language file <em>(not used here)</em>.
     * @param parameters Hashtable containing all user parameters <em>(not used here)</em>.
     * @return <code>true</code> if the button should be enabled, <code>false</code> otherwise.
     */
    public Boolean showBackButton(A_CmsObject cms, CmsXmlLanguageFile lang, Hashtable parameters) {
        
        // Get the current page number
        String pageText = (String)parameters.get(C_PARA_PAGE);
        int page = new Integer(pageText).intValue();
        return new Boolean(page > 1);
    }    


    /**
     * Used by the workplace "next" button to decide whether the icon should 
     * be activated or not. A button will use this method if the attribute <code>method="showNextButton"</code>
     * is defined in the <code>&lt;BUTTON&gt;</code> tag.
     * <P>
     * This method returns <code>false</code> if the currently displayed page is
     * the last page.
     * 
     * @param cms A_CmsObject Object for accessing system resources <em>(not used here)</em>.
     * @param lang reference to the currently valid language file <em>(not used here)</em>.
     * @param parameters Hashtable containing all user parameters <em>(not used here)</em>.
     * @return <code>true</code> if the button should be enabled, <code>false</code> otherwise.
     */
    public Boolean showNextButton(A_CmsObject cms, CmsXmlLanguageFile lang, Hashtable parameters) {
        
        // Get the current page number
        String pageText = (String)parameters.get(C_PARA_PAGE);
        int page = new Integer(pageText).intValue();

        // get the number of pics
        Vector filteredPics = (Vector)parameters.get("_PICLIST_");
        int numPics = filteredPics.size();
        
        // Get the maximum page number
        int maxpage = ((numPics - 1) / C_PICBROWSER_MAXIMAGES) + 1;        
        return new Boolean(numPics == 0 || (page < maxpage));
    }    
    
    /**
     * Internal method for getting a vector of all pictures using
     * a given filter.
     * @param cms Cms object for accessing system resources.
     * @param folder Folder to look for pictures.
     * @param filter Search pattern that should be used.
     * @return Vector of CmsFile objects.
     */
    private Vector getFilteredPicList(A_CmsObject cms, String folder, String filter)  throws CmsException {
        // Get all pictures in the given folder using the cms object
        Vector allPics = cms.getFilesInFolder(folder);
        
        // Filter the pics
        Vector filteredPics = new Vector();
        for(int i=0; i< allPics.size(); i++) {
            CmsFile file = (CmsFile)allPics.elementAt(i);
            String filename = file.getName();
            String title = cms.readProperty(file.getAbsolutePath(), C_PROPERTY_TITLE); 
            boolean filenameFilter = inFilter(filename, filter);
            boolean titleFilter = ((title == null) || ("".equals(title)))? false : inFilter(title, filter);
            if((filenameFilter || titleFilter) && isImage(filename)) {
                filteredPics.addElement(file);
            }
        }
        return filteredPics;
    }
    
    /**
     * Checks, if the given filename matches the filter.
     * @param filename filename to be checked.
     * @param filter filter to be checked.
     * @return <code>true</code> if the filename matches the filter, <code>false</code> otherwise.
     */
    private boolean inFilter(String name, String filter) {
        String compareName = name.toLowerCase();
        String compareFilter = filter.toLowerCase();
        if("*".equals(compareFilter) || "".equals(compareFilter) || (compareName.indexOf(compareFilter) != -1)) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Checks, if the given filename ends with a typical picture suffix.
     * @param filename filename to be checked.
     * @return <code>true</code> if the is an image file, <code>false</code> otherwise.
     */
    private boolean isImage(String filename) {
        if(filename.toLowerCase().endsWith("gif") 
                || filename.toLowerCase().endsWith("jpg") 
                || filename.toLowerCase().endsWith("jpeg")) {
            return true;
        } else {
            return false;
        }
    }        
}
